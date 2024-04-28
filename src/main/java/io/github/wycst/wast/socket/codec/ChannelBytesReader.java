package io.github.wycst.wast.socket.codec;

import io.github.wycst.wast.socket.tcp.ChannelContext;

import java.nio.ByteBuffer;

/**
 * <p> Not accustomed to operating ByteBuffer objects, inheritable ChannelBytesReader traverses arrays
 * <p></p>
 * <p> If switching between BIG-ENDIAN and LITTLE-EDIAN is involved, it is recommended to use ByteBuffer directly.
 * <p> Otherwise, the encoder needs to manually handle the conversion issue between BIG-ENDIAN and LITTLE-EDIAN
 *
 * @Date 2024/1/31 14:24
 * @Created by wangyc
 */
public abstract class ChannelBytesReader<E> extends ChannelDecoder<E> {

    /**
     * Operation to read byte array (zero copy)
     *
     * @param channelContext
     * @param buf
     * @param offset
     * @param len
     * @return
     */
    public abstract void read(ChannelContext channelContext, byte[] buf, int offset, int len, ChannelHandlerDelegation<E> delegation);

    @Override
    public final void read(ChannelContext channelContext, ByteBuffer buf, ChannelHandlerDelegation<E> delegation) {
        // buf.arrayOffset();
        int len = buf.remaining();
        int offset = buf.position();
        buf.clear();
        read(channelContext, buf.array(), offset, len, delegation);
    }

    @Override
    public void wakeup() {

    }
}
