package io.github.wycst.wast.socket.tcp;

import io.github.wycst.wast.socket.codec.ChannelReader;
import io.github.wycst.wast.socket.factory.ChannelHandlerFactory;
import io.github.wycst.wast.socket.factory.ChannelReaderFactory;
import io.github.wycst.wast.socket.handler.ChannelHandler;
import io.github.wycst.wast.socket.handler.IdleStateHandler;

/**
 * tcp server config
 *
 * @Date 2024/1/20 9:49
 * @Created by wangyc
 */
public final class ChannelConfig {

    private static int defaultBufferSize = 1024;
    private int readBufferSize = defaultBufferSize;
    private int writeBufferSize = defaultBufferSize;
    private boolean printSSLErrorLog;
    private boolean printReadErrorLog;
    private boolean printApplicationMessage;
    private ChannelReader channelReader = ChannelReader.UNDO;
    private ChannelHandler channelHandler;
    private IdleStateHandler idleStateHandler;
    private ChannelHandlerFactory channelHandlerFactory = singletonChannelHandlerFactory();
    private ChannelReaderFactory channelReaderFactory = singletonChannelReaderFactory();

    public void setReadBufferSize(int readBufferSize) {
        this.readBufferSize = Math.max(readBufferSize, 512);
    }

    public int getReadBufferSize() {
        return readBufferSize;
    }

    public int getWriteBufferSize() {
        return writeBufferSize;
    }

    public void setWriteBufferSize(int writeBufferSize) {
        this.writeBufferSize = Math.max(writeBufferSize, 512);
    }

    public ChannelConfig self() {
        return this;
    }

    public static void setDefaultBufferSize(int defaultBufferSize) {
        ChannelConfig.defaultBufferSize = defaultBufferSize;
    }

    public boolean isPrintSSLErrorLog() {
        return printSSLErrorLog;
    }

    public void setPrintSSLErrorLog(boolean printSSLErrorLog) {
        this.printSSLErrorLog = printSSLErrorLog;
    }

    public boolean isPrintReadErrorLog() {
        return printReadErrorLog;
    }

    public void setPrintReadErrorLog(boolean printReadErrorLog) {
        this.printReadErrorLog = printReadErrorLog;
    }

    public boolean isPrintApplicationMessage() {
        return printApplicationMessage;
    }

    public void setPrintApplicationMessage(boolean printApplicationMessage) {
        this.printApplicationMessage = printApplicationMessage;
    }

    public void setIdleStateHandler(IdleStateHandler idleStateHandler) {
        this.idleStateHandler = idleStateHandler;
    }

    public IdleStateHandler getIdleStateHandler() {
        return idleStateHandler;
    }

    public void setChannelHandler(ChannelHandler channelHandler) {
        this.channelHandler = channelHandler;
    }

    public ChannelHandler getChannelHandler() {
        return channelHandlerFactory.getChannelHandler();
    }

    public ChannelReader getChannelReader() {
        ChannelReader channelReader = channelReaderFactory.getChannelReader();
        return channelReader == null ? ChannelReader.UNDO : channelReader;
    }

    public void setChannelReader(ChannelReader channelReader) {
        channelReader.getClass();
        this.channelReader = channelReader;
    }


    public void setChannelHandlerFactory(ChannelHandlerFactory channelHandlerFactory) {
        channelHandlerFactory.getClass();
        this.channelHandlerFactory = channelHandlerFactory;
    }

    public void setChannelReaderFactory(ChannelReaderFactory channelReaderFactory) {
        channelReaderFactory.getClass();
        this.channelReaderFactory = channelReaderFactory;
    }

    public void resetChannelHandlerFactory() {
        this.channelHandlerFactory = singletonChannelHandlerFactory();
    }

    ChannelHandlerFactory singletonChannelHandlerFactory() {
        return new ChannelHandlerFactory() {
            @Override
            public ChannelHandler getChannelHandler() {
                return channelHandler;
            }
        };
    }

    ChannelReaderFactory singletonChannelReaderFactory() {
        return new ChannelReaderFactory() {
            @Override
            public ChannelReader getChannelReader() {
                return channelReader;
            }
        };
    }
}
