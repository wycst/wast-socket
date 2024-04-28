package com.wycst.tcp.codec.string;

import io.github.wycst.wast.socket.codec.ChannelStringCodec;
import io.github.wycst.wast.socket.handler.ChannelHandler;
import io.github.wycst.wast.socket.tcp.ChannelContext;
import io.github.wycst.wast.socket.tcp.TCPServer;

import java.io.IOException;

/**
 * @Date 2024/2/21 15:33
 * @Created by wangyc
 */
public class TcpServerTest {

    public static void main(String[] args) throws IOException, InterruptedException {
        ChannelStringCodec codec = new ChannelStringCodec();
        TCPServer tcpServer = new TCPServer(8081);
        tcpServer.workerNum(5).channelReader(codec).channelHandler(new ChannelHandler<String>() {
            @Override
            public void onHandle(ChannelContext channelContext, String message) throws IOException {
                System.out.println("revice message " + message);
            }
        }).start();
        // tcpServer.shutdown();
    }

}
