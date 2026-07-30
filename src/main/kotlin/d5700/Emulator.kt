package d5700

import java.io.File

class D5700Emulator private constructor(
    private val cpu: D5700CPU,
    private val rom: D5700Rom,
    private val display: D5700Display,
    private val timer: D5700Timer
) {
    fun loadRom(bytes: UByteArray) {
        rom.load(bytes)
        cpu.reset()
    }

    fun reset() {
        cpu.reset()
    }

    fun run() {
        timer.start()
        val stepDurationNanos = 2_000_000L // 500 Hz
        try {
            while (!cpu.halted) {
                val start = System.nanoTime()
                cpu.step()
                display.renderToConsole()

                val elapsed = System.nanoTime() - start
                val remaining = stepDurationNanos - elapsed
                if (remaining > 0) {
                    Thread.sleep(remaining / 1_000_000L, (remaining % 1_000_000L).toInt())
                }
            }
        } finally {
            timer.stop()
        }
        display.renderToConsole(force = true)
    }

    companion object Factory {
        fun create(
            keyboardInput: KeyboardInput = ConsoleKeyboardInput(),
            timerScheduler: TimerSchedulerStrategy = RealTimeTimerSchedulerStrategy(),
            romWritable: Boolean = false
        ): D5700Emulator {
            val rom = D5700Rom(4096, romWritable)
            val ram = D5700Ram(4096)
            val display = D5700Display()
            val timer = D5700Timer(timerScheduler)
            val cpu = D5700CPU(rom, ram, keyboardInput, display, timer)
            return D5700Emulator(cpu, rom, display, timer)
        }
    }
}

object D5700RomLoader {
    fun loadFromPath(path: String): UByteArray {
        val file = File(path)
        if (!file.exists() || !file.isFile) {
            throw IllegalArgumentException("Program file does not exist: $path")
        }

        return if (file.extension.equals("d5700", ignoreCase = true)) {
            parseHexInstructionText(file.readLines())
        } else {
            file.readBytes().asUByteArray()
        }
    }

    private fun parseHexInstructionText(lines: List<String>): UByteArray {
        val bytes = mutableListOf<UByte>()
        lines.forEachIndexed { index, raw ->
            val line = raw.trim()
            if (line.isEmpty()) {
                return@forEachIndexed
            }
            val compact = line.replace(" ", "")
            if (!compact.matches(Regex("^[0-9A-Fa-f]{4}$"))) {
                throw IllegalArgumentException("Invalid instruction on line ${index + 1}: '$raw'")
            }
            bytes.add(compact.substring(0, 2).toInt(16).toUByte())
            bytes.add(compact.substring(2, 4).toInt(16).toUByte())
        }
        return bytes.toUByteArray()
    }
}

object D5700App {
    fun run() {
        print("Enter path to ROM program: ")
        val path = readlnOrNull().orEmpty().trim()
        if (path.isEmpty()) {
            println("No path provided. Exiting.")
            return
        }

        try {
            val programBytes = D5700RomLoader.loadFromPath(path)
            val emulator = D5700Emulator.create()
            emulator.loadRom(programBytes)
            emulator.run()
            println("Program halted (encountered 0000).")
        } catch (ex: Exception) {
            println("Emulator terminated: ${ex.message}")
        }
    }
}
