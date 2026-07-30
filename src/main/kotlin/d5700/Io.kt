package d5700

import java.util.Locale

interface KeyboardInput {
    fun readHexByte(): UByte
}

class ConsoleKeyboardInput : KeyboardInput {
    override fun readHexByte(): UByte {
        val raw = readlnOrNull().orEmpty()
        return parseHexInput(raw)
    }

    internal fun parseHexInput(raw: String): UByte {
        val filtered = raw.uppercase(Locale.US).filter { it in '0'..'9' || it in 'A'..'F' }.take(2)
        if (filtered.isEmpty()) {
            return 0u
        }
        return filtered.toInt(16).toUByte()
    }
}

class StubKeyboardInput(private val supplier: () -> UByte) : KeyboardInput {
    override fun readHexByte(): UByte = supplier()
}

class D5700Display {
    private val frameBuffer = UByteArray(64)
    private var dirty = false

    fun draw(row: Int, column: Int, value: UByte) {
        if (row !in 0..7 || column !in 0..7) {
            throw ProgramTerminatedException("Draw coordinates out of bounds: row=$row col=$column")
        }
        val index = row * 8 + column
        frameBuffer[index] = value
        dirty = true
    }

    fun clear() {
        frameBuffer.fill(0u)
        dirty = true
    }

    fun snapshot(): UByteArray = frameBuffer.copyOf()

    fun renderToConsole(force: Boolean = false) {
        if (!dirty && !force) {
            return
        }
        println("+--------+")
        for (row in 0 until 8) {
            val line = buildString {
                for (col in 0 until 8) {
                    val v = frameBuffer[row * 8 + col].toInt()
                    append(if (v in 32..126) v.toChar() else ' ')
                }
            }
            println("|$line|")
        }
        println("+--------+")
        dirty = false
    }
}
