package io.github.wycst.wast.socket.codec;

import io.github.wycst.wast.socket.tcp.ChannelContext;

/**
 * codec for reader and writer
 *
 * @Date 2024/2/24
 * @Created by wangyc
 */
public abstract class ChannelCodec<T> extends ChannelDecoder<T> implements ChannelReader<T>, ChannelWriter<T> {

    @Override
    public void init(ChannelContext channelContext) {
    }

    @Override
    public void wakeup() {
    }
}
