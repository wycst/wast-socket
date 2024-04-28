package io.github.wycst.wast.socket.codec;

import io.github.wycst.wast.socket.tcp.ChannelContext;

import java.io.IOException;

/**
 * @Date 2024/2/25 16:28
 * @Created by wangyc
 */
public abstract class ChannelDecoder<T> implements ChannelReader<T> {

    @Override
    public void init(ChannelContext channelContext) {
    }

    @Override
    public void wakeup() {
    }

    /**
     * 读取指定字节
     *
     * @param channelContext
     * @param buf
     * @param offset
     * @param len
     * @throws IOException
     */
    public final void read(ChannelContext channelContext, byte[] buf, int offset, int len) throws IOException {
        if (channelContext.read(buf, offset, len) == -1) {
            throw new IOException("channel is closed");
        }
    }

    /**
     * 读取指定字节
     *
     * @param channelContext
     * @param buf
     * @throws IOException
     */
    public final void read(ChannelContext channelContext, byte[] buf) throws IOException {
        read(channelContext, buf, 0, buf.length);
    }
}
