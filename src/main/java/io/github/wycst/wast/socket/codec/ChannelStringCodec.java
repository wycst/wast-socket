package io.github.wycst.wast.socket.codec;
import io.github.wycst.wast.socket.env.RuntimeAdapter;
import io.github.wycst.wast.socket.exception.SocketException;
import io.github.wycst.wast.socket.tcp.ChannelContext;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

/**
 * <p>
 * Encoding and decoding of string messages. <br>
 * Use Protocol: content-length(len) + body. <br>
 * Use BIG_ENDIAN for integers to compatible with single byte length, When the length is less than 128, only one byte needs to be written. When reading, check whether the first byte is greater than 0. If it is greater than 0, it indicates the length
 * </p>
 *
 * @Date 2024/2/23 21:10
 * @Created wangyc
 */
public class ChannelStringCodec extends ChannelCodec<String> {
    final Charset charset;
    final int limitPackLength;
    final static ByteBuffer EMPTY = ByteBuffer.allocate(0);

    public ChannelStringCodec() {
        this(Charset.defaultCharset());
    }

    public ChannelStringCodec(String charsetName) {
        this(Charset.forName(charsetName));
    }

    public ChannelStringCodec(String charsetName, int limit) {
        this(Charset.forName(charsetName), limit);
    }

    public ChannelStringCodec(Charset charset) {
        this(charset, -1);
    }

    public ChannelStringCodec(Charset charset, int limit) {
        this.charset = charset;
        this.limitPackLength = limit <= 0 ? Integer.MAX_VALUE : limit;
    }

    @Override
    public void init(ChannelContext channelContext) {
    }

    /**
     * ByteBuffer -> String
     *
     * <p> First read length: Check if the first byte b is a non-negative number. If so, set it to the byte length L of the string. Otherwise, read 4 bytes as an int value and set the first bit to 0 to obtain the length L of the string</p>
     * <p> Read content: Read L bytes backwards and call the construction method of string</p>
     *
     * @param channelContext
     * @param buf
     * @param delegation
     * @throws IOException
     */
    @Override
    public final void read(ChannelContext channelContext, ByteBuffer buf, ChannelHandlerDelegation<String> delegation) throws IOException {
        if (buf.hasRemaining()) {
            byte[] array = buf.array();
            int len = buf.remaining(), offset = 0, limit = len;
            while (len > 0) {
                byte firstByte = array[offset];
                int contentLength;
                if (firstByte >= 0) {
                    contentLength = firstByte;
                    ++offset;
                } else {
                    if (len < 4) {
                        read(channelContext, array, len, 4 - len);
                        limit = 4;
                    }
                    if(RuntimeAdapter.BIG_ENDIAN) {
                        // use unsafe read 4 bytes
                        contentLength = RuntimeAdapter.getInt(array, offset) & 0x7fff;
                    } else {
                        // use bits
                        contentLength = (((array[offset] & 0xFF) << 24) | ((array[offset + 1] & 0xFF) << 16) | ((array[offset + 2] & 0xFF) << 8) | (array[offset + 3] & 0xFF)) & 0x7fff;
                    }
                    offset += 4;
                }
                if (contentLength > limitPackLength) {
                    throw new SocketException("message length " + contentLength + " exceeds max limit " + limitPackLength);
                }
                int len0 = limit - offset, rem = contentLength - len0;
                if (len0 >= contentLength) {
                    delegation.call(new String(array, offset, contentLength, charset));
                    offset += contentLength;
                    len = limit - offset;
                    continue;
                }
                byte[] bytes = new byte[contentLength];
                if (len0 > 0) {
                    System.arraycopy(array, offset, bytes, 0, len0);
                }
                read(channelContext, bytes, len0, rem);
                delegation.call(new String(bytes, charset));
                break;
            }
        }
    }

    /**
     * <p> String -> ByteBuffer </p>
     * <p> Length+Content </p>
     * <p> Length: Output one byte if less than 0x80, otherwise output four bytes, with the first bit set to 1</p>
     * <p> Content: Encode according to charset</p>
     *
     * @param message
     * @throws IOException
     */
    @Override
    public final ByteBuffer write(String message) throws IOException {
        if (message.isEmpty()) return EMPTY;
        byte[] bytes = RuntimeAdapter.INSTANCE.getStringBytes(message, charset); // message.getBytes(charset);
        int len = bytes.length;
        if (len > limitPackLength) {
            throw new SocketException("message length exceeds max limit " + limitPackLength);
        }
        ByteBuffer buf = ByteBuffer.allocate(len + 4);
        if (len < 0x80) {
            buf.put((byte) len);
        } else {
            buf.order(ByteOrder.BIG_ENDIAN);
            buf.putInt(len | 0x8000);
        }
        buf.put(bytes);
        buf.flip();
        return buf;
    }
}
