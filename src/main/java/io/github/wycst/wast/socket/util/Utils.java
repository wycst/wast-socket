package io.github.wycst.wast.socket.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Date 2024/4/28 12:28
 * @Created by wangyc
 */
public final class Utils {

    final static char[] HEX_DIGITS = "0123456789abcdef".toCharArray();
    final static AtomicLong atomicLong = new AtomicLong();

    public static void shutdownExecutorService(ExecutorService executorService) {
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(5000L, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (Throwable var4) {
            try {
                executorService.shutdownNow();
            } catch (Throwable var3) {
            }
        }

    }

    /**
     * 将long类型的value转为长度为16的16进制字符串,缺省补字符0
     *
     * @param value
     * @return
     * @see Long#toHexString(long)
     */
    public static String toHexString16(long value) {
        char[] chars = new char[16];
        for (int i = 15; i > -1; --i) {
            int val = (int) (value & 0xf);
            chars[i] = HEX_DIGITS[val];
            value >>= 4L;
        }
        return new String(chars);
    }

    public static String hex() {
        return toHexString16(atomicLong.incrementAndGet());
    }

    public static String printHexString(byte[] b, char splitChar) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < b.length; ++i) {
            String hex = Integer.toHexString(b[i] & 255);
            if (hex.length() == 1) {
                builder.append('0');
            }
            builder.append(hex.toUpperCase());
            if (splitChar > 0) {
                builder.append(splitChar);
            }
        }
        return builder.toString();
    }
}
