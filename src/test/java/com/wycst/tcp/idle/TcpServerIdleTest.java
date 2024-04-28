package com.wycst.tcp.idle;

import io.github.wycst.wast.socket.codec.ChannelStringCodec;
import io.github.wycst.wast.socket.handler.ChannelHandler;
import io.github.wycst.wast.socket.handler.IdleStateHandler;
import io.github.wycst.wast.socket.tcp.ChannelContext;
import io.github.wycst.wast.socket.tcp.TCPServer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @Date 2024/2/21 15:33
 * @Created by wangyc
 */
public class TcpServerIdleTest {

    public static void main(String[] args) throws IOException, InterruptedException {
        ChannelStringCodec codec = new ChannelStringCodec();
        TCPServer tcpServer = new TCPServer(8081);
        tcpServer.channelReader(codec).channelHandler(new ChannelHandler<String>() {
            @Override
            public void onHandle(ChannelContext channelContext, String message) throws IOException {
                // System.out.println("message " + message);
            }
        }).idleStateHandler(new IdleStateHandler(30, 30, TimeUnit.SECONDS) {
            @Override
            public void onIdleTriggered(ChannelContext ctx, IdleType idleType, long triggerTotalCount, long triggerConsecutiveCount) throws Throwable {
                System.out.println("idle trigger " + idleType);
            }
        }).start();
        // tcpServer.shutdown();
    }

}
