package d5700

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

interface TimerSchedulerStrategy {
    fun start(onTick: () -> Unit): AutoCloseable
}

class RealTimeTimerSchedulerStrategy : TimerSchedulerStrategy {
    override fun start(onTick: () -> Unit): AutoCloseable {
        val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "d5700-timer").apply { isDaemon = true }
        }
        executor.scheduleAtFixedRate(onTick, 16L, 16L, TimeUnit.MILLISECONDS)
        return AutoCloseable { executor.shutdownNow() }
    }
}

class ManualTimerSchedulerStrategy : TimerSchedulerStrategy {
    private var onTick: (() -> Unit)? = null

    override fun start(onTick: () -> Unit): AutoCloseable {
        this.onTick = onTick
        return AutoCloseable { this.onTick = null }
    }

    fun tick(times: Int = 1) {
        repeat(times) {
            onTick?.invoke()
        }
    }
}

class D5700Timer(
    private val schedulerStrategy: TimerSchedulerStrategy = RealTimeTimerSchedulerStrategy()
) {
    private val value = AtomicInteger(0)
    private var handle: AutoCloseable? = null

    fun start() {
        if (handle != null) {
            return
        }
        handle = schedulerStrategy.start { tick60Hz() }
    }

    fun stop() {
        handle?.close()
        handle = null
    }

    fun set(newValue: UByte) {
        value.set(newValue.toInt() and 0xFF)
    }

    fun get(): UByte = value.get().toUByte()

    fun tick60Hz() {
        while (true) {
            val current = value.get()
            if (current <= 0) {
                return
            }
            if (value.compareAndSet(current, current - 1)) {
                return
            }
        }
    }
}
