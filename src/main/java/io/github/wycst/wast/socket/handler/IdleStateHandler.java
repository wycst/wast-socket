package io.github.wycst.wast.socket.handler;

import io.github.wycst.wast.socket.tcp.ChannelContext;

import java.util.concurrent.TimeUnit;

/**
 * Global singleton
 */
public abstract class IdleStateHandler {

    public enum IdleType {
        IdleTypeRead, IdleTypeWrite
    }

    private final long readerIdleTimeNanos;
    private final long writerIdleTimeNanos;

    public IdleStateHandler(long readerIdleTime, long writerIdleTime, TimeUnit unit) {
        this.readerIdleTimeNanos = readerIdleTime > 0 ? unit.toNanos(readerIdleTime) : 0l;
        this.writerIdleTimeNanos = readerIdleTime > 0 ? unit.toNanos(writerIdleTime) : 0l;
    }

    public long getReaderIdleTimeNanos() {
        return readerIdleTimeNanos;
    }

    public long getWriterIdleTimeNanos() {
        return writerIdleTimeNanos;
    }

    /**
     * Trigger hook when idle
     *
     * @param ctx                     context
     * @param idleType                IdleTypeRead | IdleTypeWrite
     * @param triggerTotalCount       current triggered total count
     * @param triggerConsecutiveCount current triggered consecutive count
     * @throws Throwable
     */
    public abstract void onIdleTriggered(ChannelContext ctx, IdleType idleType, long triggerTotalCount, long triggerConsecutiveCount) throws Throwable;

}
