package io.github.wycst.wast.socket.tcp;

import io.github.wycst.wast.socket.handler.ChannelHandler;
import io.github.wycst.wast.socket.handler.IdleStateHandler;
import io.github.wycst.wast.socket.handler.IdleStateHandlerTrigger;
import io.github.wycst.wast.socket.util.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.*;
public class ChannelContext {

    final String id;
    final SocketChannel channel;
    ScheduledExecutorService scheduledExecutorService;
    SelectionKey readKey;
    IdleStateHandlerTrigger idleStateHandlerTrigger;
    private ChannelHandler channelHandler;

    private Object attachment;

    private Map<String, Object> attributes;
    private Object lock = new Object();

    public ChannelContext(SocketChannel channel) throws IOException {
        this.id = Utils.hex();
        this.channel = channel;
    }

    public void close() {
        if (!isClosed()) {
            try {
                readKey.cancel();
                channel.close();
                channelHandler.onClosed(this);
                if (idleStateHandlerTrigger != null) {
                    idleStateHandlerTrigger.release();
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            } finally {
                attachment = null;
                if (scheduledExecutorService != null) {
                    Utils.shutdownExecutorService(scheduledExecutorService);
                }
            }
        }
    }

    // async trigger selector close
    public void awaitClose() {
        try {
            Thread.sleep(500);
            close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public int write(ByteBuffer buf) throws IOException {
        return channelWrite(buf);
    }

    protected final int channelWrite(ByteBuffer buf) throws IOException {
        try {
            int len = buf.remaining();
            while (buf.hasRemaining()) {
                channel.write(buf);
            }
            return len;
        } finally {
            // triggered as long as a write action is performed, regardless of whether data is write or not
            if (idleStateHandlerTrigger != null) {
                idleStateHandlerTrigger.onWriteTriggered();
            }
        }
    }

    protected final int channelRead(ByteBuffer buf) throws IOException {
        try {
            if (isClosed()) return -1;
            return channel.read(buf);
        } finally {
            // Triggered as long as a read action is performed, regardless of whether data is read or not
            if (idleStateHandlerTrigger != null) {
                idleStateHandlerTrigger.onReadTriggered();
            }
        }
    }

    /**
     * read bytes
     *
     * @param b
     * @return b.length
     * @throws IOException
     */
    public final int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * <p> This method will block until the read is completed and returns the arg len</p>
     * <p> If the channel is closed, it will return -1 </p>
     *
     * @param b
     * @param off
     * @param len
     * @return -1 if the channel is closed, otherwise return the len
     * @throws IOException
     */
    public int read(byte[] b, int off, int len) throws IOException {
        // no copy
        ByteBuffer byteBuffer = ByteBuffer.wrap(b, off, len);
        try {
            while (true) {
                int size = channelRead(byteBuffer); // channel.read(byteBuffer);
                if (size == -1) return -1;
                if (byteBuffer.hasRemaining()) {
                    awaitRead();
                } else {
                    break;
                }
            }
            return len;
        } finally {
            // if print message
        }
    }

    protected void awaitRead() {
        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected void wakeup() {
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    public boolean isSSL() {
        return false;
    }

    public boolean isClosed() {
        return !channel.isOpen();
    }

    public SocketChannel channel() {
        return channel;
    }

    void setIdleTrigger(IdleStateHandler idleStateHandler) {
        idleStateHandlerTrigger = new IdleStateHandlerTrigger(idleStateHandler, this);
    }

    public final ScheduledFuture schedule(Runnable runnable, long delay, TimeUnit timeUnit) {
        if (scheduledExecutorService == null) {
            synchronized (this) {
                if (scheduledExecutorService == null) {
                    this.scheduledExecutorService = Executors.newScheduledThreadPool(100);
                }
            }
        }
        return scheduledExecutorService.schedule(runnable, delay, timeUnit);
    }

    public String getId() {
        return id;
    }

    void setChannelHandler(ChannelHandler channelHandler) {
        this.channelHandler = channelHandler;
    }

    public void setReadKey(SelectionKey readKey) {
        this.readKey = readKey;
    }

    public Object attachment() {
        return attachment;
    }

    public void attachment(Object attachment) {
        this.attachment = attachment;
    }

    public Object getAttribute(String key) {
        Map<String, Object> attributes = getAttributes();
        return attributes.get(key);
    }

    public void setAttribute(String key, Object value) {
        Map<String, Object> attributes = getAttributes();
        attributes.put(key, value);
    }

    Map<String, Object> getAttributes() {
        if (attributes == null) {
            attributes = new ConcurrentHashMap<String, Object>();
        }
        return attributes;
    }

    public String getHandShakedApplicationProtocol() {
        return null;
    }
}
