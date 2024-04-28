package io.github.wycst.wast.socket.handler;

import io.github.wycst.wast.socket.tcp.ChannelContext;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * todo
 * Need to solve the idle detection task caused by initiating a connection but not sending data (not sending at all)
 *
 * @Date 2024/1/25 15:01
 * @Created by wangyc
 */
public class IdleStateHandlerTrigger {

    final IdleStateHandler idleStateHandler;
    final ChannelContext channelContext;
    final long readerIdleTimeNanos;
    final long writerIdleTimeNanos;

    long lastReadNanos;
    long idleReadTriggerCnt;
    long idleReadTriggerConsecutiveCnt;
    long lastWriteNanos;
    long idleWriteTriggerCnt;
    long idleWriteTriggerConsecutiveCnt;

    ScheduleWriteTask scheduleWriteTask;
    ScheduleReadTask scheduleReadTask;
    ScheduledFuture readIdleFuture;
    ScheduledFuture writeIdleFuture;
    final static long MIN_NANOS = 1000000000L;

    public IdleStateHandlerTrigger(IdleStateHandler idleStateHandler, ChannelContext channelContext) {
        this.idleStateHandler = idleStateHandler;
        this.channelContext = channelContext;
        this.readerIdleTimeNanos = idleStateHandler.getReaderIdleTimeNanos();
        this.writerIdleTimeNanos = idleStateHandler.getWriterIdleTimeNanos();
        if (readerIdleTimeNanos >= MIN_NANOS) {
            this.scheduleReadTask = new ScheduleReadTask();
            scheduleReadTask(readerIdleTimeNanos);
            onReadTriggered();
        }
        if (writerIdleTimeNanos >= MIN_NANOS) {
            this.scheduleWriteTask = new ScheduleWriteTask();
            scheduleWriteTask(writerIdleTimeNanos);
            onWriteTriggered();
        }
    }

    public void release() {
        if (readIdleFuture != null) {
            readIdleFuture.cancel(false);
        }
        if (writeIdleFuture != null) {
            writeIdleFuture.cancel(false);
        }
    }

    class ScheduleWriteTask implements Runnable {
        @Override
        public void run() {
            long useNanos = System.nanoTime() - lastWriteNanos;
            long rem = writerIdleTimeNanos - useNanos;
            writeIdleFuture.cancel(false);
            if (rem > MIN_NANOS) {
                scheduleWriteTask(rem);
                return;
            }
            try {
                increaseTriggerWriteCount();
                idleStateHandler.onIdleTriggered(channelContext, IdleStateHandler.IdleType.IdleTypeWrite, idleWriteTriggerCnt, idleWriteTriggerConsecutiveCnt);
            } catch (Throwable e) {
            } finally {
                scheduleWriteTask(writerIdleTimeNanos);
            }
        }
    }

    class ScheduleReadTask implements Runnable {
        @Override
        public void run() {
            long useNanos = System.nanoTime() - lastReadNanos;
            long rem = readerIdleTimeNanos - useNanos;
            readIdleFuture.cancel(false);
            if (rem > MIN_NANOS) {
                scheduleReadTask(rem);
                return;
            }
            try {
                increaseTriggerReadCount();
                idleStateHandler.onIdleTriggered(channelContext, IdleStateHandler.IdleType.IdleTypeRead, idleReadTriggerCnt, idleReadTriggerConsecutiveCnt);
            } catch (Throwable e) {
            } finally {
                scheduleReadTask(readerIdleTimeNanos);
            }
        }
    }

    private void scheduleWriteTask(long timeNanos) {
        writeIdleFuture = channelContext.schedule(scheduleWriteTask, timeNanos, TimeUnit.NANOSECONDS);
    }

    private void increaseTriggerWriteCount() {
        ++idleWriteTriggerCnt;
        ++idleWriteTriggerConsecutiveCnt;
        if (idleWriteTriggerCnt <= 0) {
            idleWriteTriggerCnt = 1;
        }
        if (idleWriteTriggerConsecutiveCnt <= 0) {
            idleWriteTriggerConsecutiveCnt = 1;
        }
    }

    private void scheduleReadTask(long timeNanos) {
        readIdleFuture = channelContext.schedule(scheduleReadTask, timeNanos, TimeUnit.NANOSECONDS);
    }

    private void increaseTriggerReadCount() {
        ++idleReadTriggerCnt;
        ++idleReadTriggerConsecutiveCnt;
        if (idleReadTriggerCnt <= 0) {
            idleReadTriggerCnt = 1;
        }
        if (idleReadTriggerConsecutiveCnt <= 0) {
            idleReadTriggerConsecutiveCnt = 1;
        }
    }

    public void onReadTriggered() {
        lastReadNanos = System.nanoTime();
        this.idleReadTriggerConsecutiveCnt = 0;
    }

    public void onWriteTriggered() {
        lastWriteNanos = System.nanoTime();
        this.idleWriteTriggerConsecutiveCnt = 0;
    }
}
