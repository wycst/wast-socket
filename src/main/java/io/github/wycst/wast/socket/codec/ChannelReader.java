package io.github.wycst.wast.socket.codec;

import io.github.wycst.wast.socket.tcp.ChannelContext;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * <p>Strictly speaking, it is a ByteBuffer decoder that can encode ByteBuffers directly read by channels into objects that the handler can manipulate</p>
 * <br/>
 * <p>It is not necessary. If the default UNDO is not specified, all decoding work will be handed over to the handler.</p>
 *
 * @Date 2024/1/21 10:38
 * @Created by wangyc
 */
public interface ChannelReader<T> {

    ChannelReader<ByteBuffer> UNDO = new ChannelReader<ByteBuffer>() {
        @Override
        public void init(ChannelContext channelContext) {

        }

        @Override
        public void read(ChannelContext channelContext, ByteBuffer buf, ChannelHandlerDelegation<ByteBuffer> delegation) throws IOException {
            delegation.call(buf);
        }

        @Override
        public void wakeup() {
        }
    };

    /**
     * before ready
     *
     * @param channelContext
     * @throws IOException
     */
    void init(ChannelContext channelContext);

    /**
     * 读取管道数据，并返回编码完成的数据结构
     *
     * @param channelContext
     * @param buf
     * @return
     */
    void read(ChannelContext channelContext, ByteBuffer buf, ChannelHandlerDelegation<T> delegation) throws IOException;

    /**
     * 唤醒
     */
    void wakeup();
}
