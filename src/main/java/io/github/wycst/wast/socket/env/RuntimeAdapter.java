package io.github.wycst.wast.socket.env;

import sun.misc.Unsafe;

import javax.net.ssl.SSLEngine;
import java.lang.reflect.Field;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

/**
 * Adaptation to different versions of JDK
 *
 * @Date 2024/2/19 14:17
 * @Created by wangyc
 */
public abstract class RuntimeAdapter {

    public static final RuntimeAdapter INSTANCE;

    public static final float JDK_VERSION;
    static final Unsafe UNSAFE;
    public static final boolean BIG_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
    public static final long STRING_VALUE_OFFSET;

    static {
        Field theUnsafeField;
        try {
            theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
        } catch (NoSuchFieldException exception) {
            theUnsafeField = null;
        }

        Unsafe instance = null;
        if (theUnsafeField != null) {
            try {
                instance = (Unsafe) theUnsafeField.get((Object) null);
            } catch (IllegalAccessException exception) {
                throw new RuntimeException(exception);
            }
        }
        UNSAFE = instance;

        Field valueField;
        long valueOffset = -1;
        try {
            valueField = String.class.getDeclaredField("value");
            valueOffset = UNSAFE.objectFieldOffset(valueField);
        } catch (Exception e) {
        }
        STRING_VALUE_OFFSET = valueOffset;
    }

    public static final long BYTE_ARRAY_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);

    public RuntimeAdapter() {
    }

    static {

        float jdkVersion = 1.8f;
        try {
            // 规范版本号
            String version = System.getProperty("java.specification.version");
            if (version != null) {
                jdkVersion = Float.parseFloat(version);
            }
        } catch (Throwable throwable) {
        }
        JDK_VERSION = jdkVersion;
        Class<? extends RuntimeAdapter> adapterClass;
        RuntimeAdapter adapterInstance;
        try {
            if (JDK_VERSION >= 9) {
                adapterClass = (Class<? extends RuntimeAdapter>) Class.forName("io.github.wycst.wast.socket.env.RuntimeAdapterJDK9Plus");
                adapterInstance = adapterClass.newInstance();
            } else {
                adapterClass = (Class<? extends RuntimeAdapter>) Class.forName("io.github.wycst.wast.socket.env.RuntimeAdapterJDK9Below");
                adapterInstance = adapterClass.newInstance();
            }
            INSTANCE = adapterInstance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public final static int getInt(byte[] array, int offset) {
        return UNSAFE.getInt(array, BYTE_ARRAY_OFFSET + offset);
    }

    /**
     * get value
     *
     * @param source
     * @return
     */
    public final static Object getStringValue(String source) {
        source.getClass();
        return UNSAFE.getObject(source, STRING_VALUE_OFFSET);
    }

    public byte[] getStringBytes(String message, Charset charset) {
        return message.getBytes(charset);
    }

    /**
     * app protocols set
     *
     * @param sslEngine            engine
     * @param applicationProtocols app protocols
     */
    public void setApplicationProtocols(SSLEngine sslEngine, String[] applicationProtocols) {
    }


    public String getSSLApplicationProtocol(SSLEngine sslEngine) {
        return null;
    }
}
