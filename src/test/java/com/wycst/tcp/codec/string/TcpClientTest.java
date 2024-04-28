package com.wycst.tcp.codec.string;

import io.github.wycst.wast.socket.codec.ChannelStringCodec;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @Date 2024/2/25 10:03
 * @Created by wangyc
 */
public class TcpClientTest {

    public static void main(String[] args) throws IOException, InterruptedException {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(new InetSocketAddress("localhost", 8081));
        while (!channel.finishConnect()) ;

        final ChannelStringCodec codec = new ChannelStringCodec();

        // 发送消息给服务端
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        String randomMessage = String.valueOf(Math.random());
                        ByteBuffer buffer = codec.write(randomMessage);
                        channel.write(buffer);
                        Thread.sleep(1000);
                    } catch (Throwable throwable) {

                    }
                }
            }
        }).start();
    }

}
