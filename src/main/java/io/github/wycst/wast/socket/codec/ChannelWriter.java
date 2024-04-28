package io.github.wycst.wast.socket.codec;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * <p>channel writer </p>
 *
 * @Date 2024/1/21 10:38
 * @Created by wangyc
 */
public interface ChannelWriter<T> {

    /**
     * write to ByteBuffer
     *
     * @param message
     * @return
     */
    ByteBuffer write(T message) throws IOException;

}
