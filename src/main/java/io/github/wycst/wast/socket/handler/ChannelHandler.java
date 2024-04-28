package io.github.wycst.wast.socket.handler;

import io.github.wycst.wast.socket.tcp.ChannelContext;

import java.io.IOException;

public abstract class ChannelHandler<T> {

    /**
     * <p> called when channel is connected (accept)</p>
     *
     * @param channelContext
     * @throws Exception
     */
    public void onConnected(ChannelContext channelContext) throws IOException {
    }

    /**
     * <p> call when data reading is completed</p>
     *
     * @param channelContext context
     * @param message        Encoded or aggregated object
     * @throws Exception
     */
    public abstract void onHandle(ChannelContext channelContext, T message) throws IOException;

    /**
     * <p> called when channel is closed </p>
     *
     * @param ctx
     * @throws Exception
     */
    public void onClosed(ChannelContext ctx) throws IOException {
    }

    /**
     * <p> on exception catched </p>
     *
     * @param context
     * @param cause
     * @throws Exception
     */
    public void onException(ChannelContext context, Throwable cause) throws IOException {
    }
}
