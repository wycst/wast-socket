package io.github.wycst.wast.socket.codec;

import java.io.IOException;

/**
 *
 * @Date 2024/2/23 17:09
 * @Created by wangyc
 */
public interface ChannelHandlerDelegation<T> {
    void call(T t) throws IOException;
}
