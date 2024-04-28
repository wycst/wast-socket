package io.github.wycst.wast.socket.factory;

import io.github.wycst.wast.socket.handler.ChannelHandler;

/**
 * @Date 2024/1/21 10:55
 * @Created by wangyc
 */
public interface ChannelHandlerFactory {

    /**
     * create handler
     *
     * @return
     */
    ChannelHandler getChannelHandler();
}
