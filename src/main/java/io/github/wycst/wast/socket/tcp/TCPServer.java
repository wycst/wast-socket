package io.github.wycst.wast.socket.tcp;

import io.github.wycst.wast.socket.codec.ChannelHandlerDelegation;
import io.github.wycst.wast.socket.codec.ChannelReader;
import io.github.wycst.wast.socket.env.RuntimeAdapter;
import io.github.wycst.wast.socket.exception.SocketException;
import io.github.wycst.wast.socket.factory.ChannelHandlerFactory;
import io.github.wycst.wast.socket.factory.ChannelReaderFactory;
import io.github.wycst.wast.socket.handler.ChannelHandler;
import io.github.wycst.wast.socket.handler.IdleStateHandler;
import io.github.wycst.wast.socket.log.ConsoleLog;
import io.github.wycst.wast.socket.util.Utils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * High performance TCP service based on nio selector implementation
 *
 * @Date 2024/1/14 19:27
 * @Created by wangyc
 */
public class TCPServer {

    public static final ConsoleLog CONSOLE_LOG = ConsoleLog.getLog(TCPServer.class);
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    protected final int port;
    volatile boolean serverRunFlag = false;
    volatile Selector selector;
    ServerSocketChannel serverChannel;
    boolean shutdowned = false;

    private int workerNum = 1;

    protected ChannelConfig channelConfig = new ChannelConfig();
    // ssl
    private boolean ssl;
    private SSLContext sslContext;
    private String[] sslCipherSuites;
    private String[] applicationProtocols;
    protected SSLContextWrapper sslContextWrapper;

    public TCPServer(int port) {
        if (port <= 0) {
            throw new IllegalArgumentException("port out of range: " + port);
        }
        this.port = port;
    }

    public final int getPort() {
        return port;
    }

    /**
     * min 1
     *
     * @param workerNum
     */
    public TCPServer workerNum(int workerNum) {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        if (workerNum > availableProcessors) {
            workerNum = availableProcessors;
        }
        this.workerNum = Math.max(workerNum, 1);
        return this;
    }

    public TCPServer ssl(boolean ssl) {
        this.ssl = ssl;
        return this;
    }

    public TCPServer idleStateHandler(IdleStateHandler idleStateHandler) {
        channelConfig.setIdleStateHandler(idleStateHandler);
        return this;
    }

    // Singleton mode
    public TCPServer channelHandler(ChannelHandler channelHandler) {
        channelConfig.setChannelHandler(channelHandler);
        return this;
    }

    // Factory mode
    public TCPServer channelHandlerFactory(ChannelHandlerFactory channelHandlerFactory) {
        channelConfig.setChannelHandlerFactory(channelHandlerFactory);
        return this;
    }

    public TCPServer channelReader(ChannelReader channelReader) {
        channelConfig.setChannelReader(channelReader);
        return this;
    }

    public TCPServer channelReaderFactory(ChannelReaderFactory channelReaderFactory) {
        channelConfig.setChannelReaderFactory(channelReaderFactory);
        return this;
    }

    public TCPServer config(ChannelConfig channelConfig) {
        this.channelConfig = channelConfig.self();
        return this;
    }

    public final ChannelConfig config() {
        return channelConfig;
    }

    public TCPServer bufferSize(int buffSize) {
        channelConfig.setReadBufferSize(buffSize);
        return this;
    }

    public TCPServer sslContext(SSLContext sslContext) {
        sslContext.getClass();
        this.sslContext = sslContext;
        this.ssl = true;
        return this;
    }

    public TCPServer sslCipherSuites(String... sslCipherSuites) {
        sslCipherSuites.getClass();
        this.sslCipherSuites = sslCipherSuites;
        return this;
    }

    public TCPServer applicationProtocols(String... applicationProtocols) {
        applicationProtocols.getClass();
        this.applicationProtocols = applicationProtocols;
        return this;
    }

    public TCPServer printSSLErrorLog(boolean bl) {
        channelConfig.setPrintSSLErrorLog(bl);
        return this;
    }

    public TCPServer printReadErrorLog(boolean bl) {
        channelConfig.setPrintReadErrorLog(bl);
        return this;
    }

    public TCPServer printApplicationMessage(boolean bl) {
        channelConfig.setPrintApplicationMessage(bl);
        return this;
    }

    class ChannelAcceptDispatcher extends Thread {

        private final ChannelReaderWorker[] workers;
        private final int workerNumMask;

        public ChannelAcceptDispatcher(ChannelReaderWorker[] workers) {
            this.workers = workers;
            this.workerNumMask = workers.length - 1;
        }

        void handleAccept() throws Throwable {

            int clientCnt = 0;
            while (serverRunFlag) {
                int num = selector.select();
                if (num == 0) continue;
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();
                    if (!key.isValid()) {
                        CONSOLE_LOG.info("handleAccept: isValid false");
                        continue;
                    }
                    if (key.isAcceptable()) {
                        SocketChannel client = serverChannel.accept();
                        client.configureBlocking(false);
                        SocketChannelRunner channelRunner = createSocketRunner(client);
                        ChannelReaderWorker channelReaderWorker = workers[++clientCnt & workerNumMask];
                        channelReaderWorker.register(client, channelRunner);
                    }
                }
            }
            for (ChannelReaderWorker worker : workers) {
                worker.wakeup();
            }
        }

        @Override
        public void run() {
            try {
                handleAccept();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    class ChannelReaderWorker extends Thread {
        final String workId;
        final Selector selector;
        boolean registering;

        public ChannelReaderWorker() throws IOException {
            this.workId = Utils.hex();
            this.selector = Selector.open();
        }

        public void register(SocketChannel client, SocketChannelRunner channelRunner) throws ClosedChannelException {
            registering = true;
            selector.wakeup();
            try {
                SelectionKey selectionKey = client.register(selector, SelectionKey.OP_READ, channelRunner);
                channelRunner.setReadKey(selectionKey);
            } finally {
                registering = false;
                synchronized (this) {
                    notify();
                }
            }
        }

        public void wakeup() {
            selector.wakeup();
        }

        void handleRead() throws Throwable {
            while (serverRunFlag) {
                // can also use selector.selectNow()
                int num = selector.select();
                if (registering) {
                    synchronized (this) {
                        wait();
                    }
                }
                // maybe zero if wakeup
                if (num == 0) continue;
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();
                    // get binded runner
                    SocketChannelRunner channelRunner = (SocketChannelRunner) key.attachment();
                    try {
                        if (key.isValid()/*key.isReadable()*/) {
                            if (!channelRunner.isRunFlag()) {
                                channelRunner.runFlag = true;
                                executorService.execute(channelRunner);
                            } else {
                                channelRunner.channelContext.wakeup();
                                channelRunner.channelReader.wakeup();
                            }
                        }
                    } catch (Throwable throwable) {
                        if (channelConfig.isPrintReadErrorLog()) {
                            throwable.printStackTrace();
                        }
                        channelRunner.close();
                    }
                }
            }
            selector.close();
        }

        @Override
        public void run() {
            try {
                handleRead();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    ChannelReaderWorker[] workers(int workNum) throws IOException {
        int cap = cap(workNum);
        ChannelReaderWorker[] selectorWorks = new ChannelReaderWorker[cap];
        for (int i = 0; i < cap; ++i) {
            selectorWorks[i] = new ChannelReaderWorker();
        }
        return selectorWorks;
    }

    static int cap(int n) {
        int cap = 1 << (31 - Integer.numberOfLeadingZeros(n));
        if (n != cap) {
            cap = cap << 1;
        }
        return cap;
    }

    public synchronized TCPServer start() {
        checkServerAvailable();
        try {
            serverRunFlag = true;
            selector = Selector.open();
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);

            ServerSocket serverSocket = serverChannel.socket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(port));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            this.initSslContext();
            final ChannelReaderWorker[] readerWorkers = workers(workerNum);
            submitTasks(new ChannelAcceptDispatcher(readerWorkers));
            submitTasks(readerWorkers);
        } catch (Throwable e) {
            throw e instanceof RuntimeException ? (RuntimeException) e : new SocketException(e.getMessage(), e);
        }
        CONSOLE_LOG.info("server startup on {}", port);
        return this;
    }

    void submitTasks(Runnable... tasks) {
        for (Runnable task : tasks) {
            executorService.submit(task);
        }
    }

    private SocketChannelRunner createSocketRunner(SocketChannel socketChannel) throws IOException {
        if (sslContextWrapper != null) {
            return new SocketChannelSSLRunner(socketChannel, channelConfig, sslContextWrapper);
        } else {
            return new SocketChannelRunner(socketChannel, channelConfig);
        }
    }

    private void initSslContext() {
        if (ssl) {
            if (sslContext == null) {
                // create default
            }
            sslContextWrapper = new SSLContextWrapper(sslContext, sslCipherSuites, applicationProtocols);
        }
    }

    private void checkServerAvailable() {
        if (shutdowned) {
            throw new SocketException("Sever is shutdowned");
        }
    }

    /**
     * 停止服务(可重新启动)
     */
    public synchronized void stop() {
        try {
            if (serverRunFlag) {
                serverRunFlag = false;
                selector.wakeup();
                selector.close();
                serverChannel.close();
                CONSOLE_LOG.info("server is stoped");
            } else {
                CONSOLE_LOG.info("server is not start");
            }
        } catch (Throwable e) {
            throw e instanceof RuntimeException ? (RuntimeException) e : new SocketException(e.getMessage(), e);
        }
    }

    /**
     * 重启服务
     */
    public synchronized void restart() {
        CONSOLE_LOG.info("restart ...");
        stop();
        start();
    }

    /**
     * 关闭服务（注意服务句柄已经不可用）
     */
    public synchronized void shutdown() {
        if (serverRunFlag) {
            stop();
        }
        if (!shutdowned) {
            Utils.shutdownExecutorService(executorService);
            shutdowned = true;
        }
    }

    static class SSLContextWrapper {
        public final SSLContext sslContext;
        public final String[] sslCipherSuites;
        public final String[] applicationProtocols;

        public SSLContextWrapper(SSLContext sslContext, String[] sslCipherSuites, String[] applicationProtocols) {
            this.sslContext = sslContext;
            this.sslCipherSuites = sslCipherSuites;
            this.applicationProtocols = applicationProtocols;
        }
    }

    static class SSLEngineContext {
        final SSLEngine sslEngine;
        final ByteBuffer packetInBuf;
        final ByteBuffer applicationInBuf;
        final ByteBuffer packetOutBuf;
        final ByteBuffer applicationOutBuf;

        boolean disabled;

        public SSLEngineContext(SSLContextWrapper sslContextWrapper) {
            sslEngine = sslContextWrapper.sslContext.createSSLEngine();
            String[] cipherSuites = sslContextWrapper.sslCipherSuites;
            String[] applicationProtocols = sslContextWrapper.applicationProtocols;
            if (cipherSuites != null && cipherSuites.length > 0) {
                sslEngine.setEnabledCipherSuites(cipherSuites);
            }
            RuntimeAdapter.INSTANCE.setApplicationProtocols(sslEngine, applicationProtocols);
            sslEngine.setUseClientMode(false);
            SSLSession session = sslEngine.getSession();
            packetInBuf = ByteBuffer.allocate(session.getPacketBufferSize());
            applicationInBuf = ByteBuffer.allocate(session.getApplicationBufferSize());
            packetOutBuf = ByteBuffer.allocate(session.getPacketBufferSize());
            applicationOutBuf = ByteBuffer.allocate(session.getApplicationBufferSize());
            session.invalidate();
        }

        public void setDisabled(boolean disabled) {
            this.disabled = disabled;
        }

        public boolean isDisabled() {
            return disabled;
        }
    }

    static class SocketChannelRunner extends Thread {
        protected final ChannelContext channelContext;
        protected final ChannelConfig channelConfig;
        protected final ChannelReader channelReader;
        protected final ChannelHandlerDelegation channelHandlerDelegation;
        protected final ChannelHandler channelHandler;

        protected boolean ready;
        protected boolean closed;
        volatile boolean runFlag;

        public boolean isRunFlag() {
            return runFlag;
        }

        SocketChannelRunner(SocketChannel channel, ChannelConfig channelConfig) throws IOException {
            this(new ChannelContext(channel), channelConfig);
        }

        SocketChannelRunner(final ChannelContext channelContext, ChannelConfig channelConfig) throws IOException {
            this.channelContext = channelContext;
            this.channelConfig = channelConfig;
            this.channelReader = channelConfig.getChannelReader();
            if ((this.channelHandler = channelConfig.getChannelHandler()) == null) {
                throw new SocketException("channel handler not set");
            }
            channelHandler.onConnected(channelContext);
            channelHandlerDelegation = createChannelHandlerDelegation();
            channelContext.setChannelHandler(channelHandler);
            IdleStateHandler idleStateHandler = channelConfig.getIdleStateHandler();
            if (idleStateHandler != null) {
                channelContext.setIdleTrigger(idleStateHandler);
            }
        }

        private ChannelHandlerDelegation createChannelHandlerDelegation() {
            return new ChannelHandlerDelegation() {
                @Override
                public void call(Object target) throws IOException {
                    channelHandler.onHandle(channelContext, target);
                }
            };
        }

        public void run() {
            try {
                runFlag = true;
                before();
                if (this.closed) return;
                try {
                    if (channelContext.isClosed()) {
                        release();
                        return;
                    }
                    try {
                        if (handleChannelRead() == -1) {
                            CONSOLE_LOG.info("channel close by client {}", channelContext.getId());
                            release();
                        }
                    } catch (Throwable throwable) {
                        if (channelConfig.isPrintReadErrorLog()) {
                            throwable.printStackTrace();
                        }
                        release();
                    }
                } catch (Exception e) {
                    if (e instanceof ClosedSelectorException) {
                        CONSOLE_LOG.info("channel close");
                        try {
                            release();
                        } catch (IOException ex) {
                        }
                    } else {
                        throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
                    }
                }
            } finally {
                runFlag = false;
            }
        }

        final void before() {
            if (!ready) {
                try {
                    beforeReady();
                } finally {
                    channelReader.init(channelContext);
                    ready = true;
                }
            }
        }

        protected void beforeReady() {
        }

        protected int handleChannelRead() throws IOException {
            // size
            int buffsize = channelConfig.getReadBufferSize();
            // if cache buf ?
            ByteBuffer buf = ByteBuffer.allocate(buffsize);
            int size;
            do {
                size = channelContext.channelRead(buf);
                if (size == -1) {
                    return -1;
                }
                buf.flip();
                read(buf);
                buf.clear();
            } while (size == buffsize);
            return 0;
        }

        // close
        public final void release() throws IOException {
            try {
                // close
                channelContext.close();
            } finally {
                this.closed = true;
            }
        }

        /**
         * plain application buf that is read already
         *
         * @param buf
         */
        public final void read(ByteBuffer buf) throws IOException {
            if (buf.hasRemaining()) {
                if (channelConfig.isPrintApplicationMessage()) {
                    byte[] data = Arrays.copyOf(buf.array(), buf.limit());
                    CONSOLE_LOG.info("hex \n{}", Utils.printHexString(data, ' '));
                }
                try {
                    channelReader.read(channelContext, buf, channelHandlerDelegation);
                } catch (Throwable throwable) {
                    channelHandler.onException(channelContext, throwable);
                    if (throwable instanceof RuntimeException) {
                        throw (RuntimeException) throwable;
                    }
                    throw (IOException) throwable;
                }
            }
        }

        public void setReadKey(SelectionKey readKey) {
            channelContext.setReadKey(readKey);
        }

        public void close() throws IOException {
            channelContext.close();
        }
    }

    static final class SSLChannelContext extends ChannelContext {
        final SSLEngineContext sslEngineContext;

        public SSLChannelContext(SocketChannel channel, SSLEngineContext sslEngineContext) throws IOException {
            super(channel);
            this.sslEngineContext = sslEngineContext;
        }

        @Override
        public int write(ByteBuffer buf) throws IOException {
            if (sslEngineContext.isDisabled()) {
                channelWrite(buf);
                buf.clear();
            } else {
                SSLEngine sslEngine = sslEngineContext.sslEngine;
                ByteBuffer packetOutBuf = sslEngineContext.packetOutBuf;
                packetOutBuf.clear();
                SSLEngineResult res = sslEngine.wrap(buf, packetOutBuf);
                if (res.getStatus() != SSLEngineResult.Status.OK) {
                    throw new SocketException("Unexpected exception, SSL encryption failed");
                }
                packetOutBuf.flip();
                channelWrite(packetOutBuf);
            }
            return 0;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (sslEngineContext.isDisabled()) {
                return super.read(b, off, len);
            } else {
                ByteBuffer applicationInBuf = sslEngineContext.applicationInBuf;
                int limit = applicationInBuf.limit();
                int position = applicationInBuf.position();
                int tlen = len;
                if (position > 0) {
                    // just check if not the first and read
                    int rem = limit - position;   // applicationInBuf.remaining()
                    if (rem >= len) {
                        applicationInBuf.get(b, off, len);
                        return len;
                    }
                    applicationInBuf.get(b, off, rem);
                    applicationInBuf.clear();
                    tlen -= rem;
                }
                SSLEngine sslEngine = sslEngineContext.sslEngine;
                ByteBuffer packetInBuf = sslEngineContext.packetInBuf;
                // packetInBuf.clear(); // do not call clear()
                while (true) {
                    int bytesNum = channelRead(packetInBuf);
                    if (bytesNum == -1) {
                        return -1;
                    } else {
                        packetInBuf.flip();
                        SSLEngineResult res;
                        do {
                            res = sslEngine.unwrap(packetInBuf, applicationInBuf);
                        } while (res.getStatus() == SSLEngineResult.Status.OK);
                        packetInBuf.compact();
                        applicationInBuf.flip();
                        // position is zero after flip
                        limit = applicationInBuf.limit();
                        if (limit >= tlen) {
                            applicationInBuf.get(b, off, tlen);
                            return len;
                        } else {
                            applicationInBuf.get(b, off, limit);
                            off += limit;
                            tlen -= limit;
                            // 切换为写模式
                            applicationInBuf.clear();
                        }
                    }
                }
            }
        }

        @Override
        public boolean isSSL() {
            return !sslEngineContext.isDisabled();
        }

        @Override
        public String getHandShakedApplicationProtocol() {
            if (isSSL()) {
                return RuntimeAdapter.INSTANCE.getSSLApplicationProtocol(sslEngineContext.sslEngine);
            } else {
                return null;
            }
        }
    }

    static class SocketChannelSSLRunner extends SocketChannelRunner {

        private boolean isSSL;
        private boolean finishHandShake;
        SSLEngineContext sslEngineContext;

        SocketChannelSSLRunner(SocketChannel channel, ChannelConfig channelConfig, SSLContextWrapper sslContextWrapper) throws IOException {
            super(new SSLChannelContext(channel, new SSLEngineContext(sslContextWrapper)), channelConfig);
        }

        @Override
        protected void beforeReady() {
            if (!finishHandShake) {
                boolean isSSL = true;
                SSLChannelContext sslChannelContext = (SSLChannelContext) channelContext;
                this.sslEngineContext = sslChannelContext.sslEngineContext;
                try {
                    int handShakeFlag = sslHandShake(sslEngineContext);
                    if (handShakeFlag == -1) {
                        CONSOLE_LOG.info("channel close by client");
                        release();
                        return;
                    }
                    isSSL = handShakeFlag == 1;
                } catch (Throwable throwable) {
                    if (channelConfig.isPrintSSLErrorLog()) {
                        throwable.printStackTrace();
                    }
                } finally {
                    this.finishHandShake = true;
                }
                this.isSSL = isSSL;
            }
        }

        protected int handleChannelRead() throws IOException {
            if (isSSL) {
                SSLEngine sslEngine = sslEngineContext.sslEngine;
                ByteBuffer packetInBuf = sslEngineContext.packetInBuf;
                ByteBuffer applicationInBuf = sslEngineContext.applicationInBuf;
                // packetInBuf.clear(); // do not call clear()
                int bytesNum = channelContext.channelRead(packetInBuf);
                if (bytesNum == -1) {
                    return -1;
                } else {
                    packetInBuf.flip();
                    SSLEngineResult res;
                    int position = applicationInBuf.position();
                    if (position > 0) {
                        applicationInBuf.compact();
                    }
                    do {
                        res = sslEngine.unwrap(packetInBuf, applicationInBuf);
                    } while (res.getStatus() == SSLEngineResult.Status.OK);
                    packetInBuf.compact();
                    applicationInBuf.flip();
                    read(applicationInBuf);
                    applicationInBuf.clear();
                    return 1;
                }
            } else {
                return super.handleChannelRead();
            }
        }

        /**
         * Check if it may be a plaintext message
         *
         * <p>
         * Each message of SSL handshake starts with a type attribute of one byte,
         * The length attribute of 3 bytes indicates the length of the message.
         * </p>
         *
         * <p>
         * The first five bytes of SSL records are headers.
         * The first byte of the header is 22, indicating the handshake protocol.
         * The next two bytes are the version, and the last two bytes are the length of the remaining part of the entire SSL record
         * </p>
         *
         * <p>
         * Here, we only check if the message starting with 22 can basically filter out all non SSL data
         * </p>
         *
         * @param buf
         * @return
         */
        boolean isMaybePlaintext(ByteBuffer buf) {
            byte firstByte = buf.array()[0];
            if (firstByte != 22) {
                return true;
            }
            return false;
        }

        private int sslHandShake(SSLEngineContext sslEngineContext) throws Exception {
            ByteBuffer packetInBuf = sslEngineContext.packetInBuf;
            int size = channelContext.channelRead(packetInBuf);
            while (size == 0) {
                Thread.sleep(5);
                size = channelContext.channelRead(packetInBuf);
            }
            if (size == -1) {
                return -1;
            }
            if (isMaybePlaintext(packetInBuf)) {
                sslEngineContext.setDisabled(true);
                int capacity = packetInBuf.capacity();
                do {
                    packetInBuf.flip();
                    read(packetInBuf);
                    if (size < capacity) {
                        return 0;
                    }
                    packetInBuf.clear();
                    size = channelContext.channelRead(packetInBuf);
                    if (size == -1) return -1;
                } while (true);
            }
            boolean firstFlag = true;
            SSLEngine sslEngine = sslEngineContext.sslEngine;
            ByteBuffer applicationInBuf = sslEngineContext.applicationInBuf;
            ByteBuffer packetOutBuf = sslEngineContext.packetOutBuf;
            ByteBuffer applicationOutBuf = sslEngineContext.applicationOutBuf;

            sslEngine.beginHandshake();
            SSLEngineResult.HandshakeStatus handshakeStatus = sslEngine.getHandshakeStatus();
            SSLEngineResult res;
            while (handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED && handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
                switch (handshakeStatus) {
                    case NEED_UNWRAP:
                        do {
                            if (firstFlag) {
                                firstFlag = false;
                            } else {
                                channelContext.channelRead(packetInBuf);
                            }
                            packetInBuf.flip();
                            res = sslEngine.unwrap(packetInBuf, applicationInBuf);
                            packetInBuf.compact();
                            handshakeStatus = res.getHandshakeStatus();
                        } while (handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_UNWRAP);
                        break;
                    case NEED_WRAP:
                        packetOutBuf.clear();
                        res = sslEngine.wrap(applicationOutBuf, packetOutBuf);
                        handshakeStatus = res.getHandshakeStatus();
                        if (res.getStatus() == SSLEngineResult.Status.OK) {
                            packetOutBuf.flip();
                            channelContext.channelWrite(packetOutBuf);
                        }
                        break;
                    case NEED_TASK:
                        Runnable task;
                        while ((task = sslEngine.getDelegatedTask()) != null) {
                            new Thread(task).start();
                        }
                        handshakeStatus = sslEngine.getHandshakeStatus();
                        break;
                }
            }
            return 1;
        }
    }

    public final void closeLogLevel() {
        CONSOLE_LOG.setLevel(Level.OFF);
    }

}
