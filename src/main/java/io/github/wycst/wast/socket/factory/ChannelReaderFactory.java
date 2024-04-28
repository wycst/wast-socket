package io.github.wycst.wast.socket.factory;

import io.github.wycst.wast.socket.codec.ChannelReader;

/**
 * @Date 2024/1/21 10:55
 * @Created by wangyc
 */
public interface ChannelReaderFactory {

    /**
     * create reader
     *
     * @return
     */
    ChannelReader getChannelReader();
}
