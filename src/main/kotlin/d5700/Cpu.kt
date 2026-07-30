package d5700

abstract class CpuCycleTemplate {
    fun step() {
        beforeStep()
        val instruction = fetchInstruction()
        executeInstruction(instruction)
        afterStep()
    }

    protected open fun beforeStep() {}
    protected abstract fun fetchInstruction(): Int
    protected abstract fun executeInstruction(instruction: Int)
    protected open fun afterStep() {}
}

class D5700CPU(
    private val rom: D5700Rom,
    private val ram: D5700Ram,
    private val keyboard: KeyboardInput,
    private val display: D5700Display,
    private val timer: D5700Timer,
    private val instructionFactory: InstructionFactory = DefaultInstructionFactory
) : CpuCycleTemplate() {

    val registers: UByteArray = UByteArray(8)
    var programCounter: Int = 0
        private set
    var addressRegister: Int = 0
        private set
    var memorySelectRom: Boolean = false
        private set
    var halted: Boolean = false
        private set

    private var pendingProgramCounterAdvance: Int = 2

    fun reset() {
        registers.fill(0u)
        programCounter = 0
        addressRegister = 0
        memorySelectRom = false
        halted = false
        pendingProgramCounterAdvance = 2
        timer.set(0u)
        display.clear()
    }

    override fun fetchInstruction(): Int {
        ensureEvenProgramCounter(programCounter)
        val high = rom.read(programCounter).toInt() and 0xFF
        val low = rom.read(programCounter + 1).toInt() and 0xFF
        return (high shl 8) or low
    }

    override fun executeInstruction(instruction: Int) {
        if (instruction == 0x0000) {
            halted = true
            pendingProgramCounterAdvance = 0
            return
        }

        val opcode = (instruction ushr 12) and 0xF
        pendingProgramCounterAdvance = 2
        val strategy = instructionFactory.get(opcode)
            ?: throw InvalidInstructionException("Unknown opcode: ${opcode.toString(16)}")
        strategy.execute(this, instruction)
    }

    override fun afterStep() {
        if (!halted) {
            val next = programCounter + pendingProgramCounterAdvance
            ensureEvenProgramCounter(next)
            programCounter = next
        }
    }

    internal fun setRegister(index: Int, value: UByte) {
        validateRegister(index)
        registers[index] = value
    }

    internal fun getRegister(index: Int): UByte {
        validateRegister(index)
        return registers[index]
    }

    internal fun add(rX: Int, rY: Int, rZ: Int) {
        val result = (getRegister(rX).toInt() + getRegister(rY).toInt()) and 0xFF
        setRegister(rZ, result.toUByte())
    }

    internal fun subtract(rX: Int, rY: Int, rZ: Int) {
        val result = (getRegister(rX).toInt() - getRegister(rY).toInt()) and 0xFF
        setRegister(rZ, result.toUByte())
    }

    internal fun readMemoryInto(rX: Int) {
        val memory = if (memorySelectRom) rom else ram
        setRegister(rX, memory.read(addressRegister))
    }

    internal fun writeRegisterToMemory(rX: Int) {
        val memory = if (memorySelectRom) rom else ram
        memory.write(addressRegister, getRegister(rX))
    }

    internal fun jump(address: Int) {
        ensureEvenProgramCounter(address)
        programCounter = address
        pendingProgramCounterAdvance = 0
    }

    internal fun readKeyboardInto(rX: Int) {
        setRegister(rX, keyboard.readHexByte())
    }

    internal fun toggleMemorySelection() {
        memorySelectRom = !memorySelectRom
    }

    internal fun skipIfEqual(rX: Int, rY: Int) {
        pendingProgramCounterAdvance = if (getRegister(rX) == getRegister(rY)) 4 else 2
    }

    internal fun skipIfNotEqual(rX: Int, rY: Int) {
        pendingProgramCounterAdvance = if (getRegister(rX) != getRegister(rY)) 4 else 2
    }

    internal fun setAddress(value: Int) {
        if (value !in 0..0x0FFF) {
            throw ProgramTerminatedException("Address register out of range: $value")
        }
        addressRegister = value
    }

    internal fun setTimer(value: UByte) {
        timer.set(value)
    }

    internal fun readTimer(rX: Int) {
        setRegister(rX, timer.get())
    }

    internal fun convertToBase10(rX: Int) {
        val value = getRegister(rX).toInt() and 0xFF
        val hundreds = value / 100
        val tens = (value % 100) / 10
        val ones = value % 10

        val memory = if (memorySelectRom) rom else ram
        memory.write(addressRegister, hundreds.toUByte())
        memory.write(addressRegister + 1, tens.toUByte())
        memory.write(addressRegister + 2, ones.toUByte())
    }

    internal fun convertByteToAscii(rX: Int, rY: Int) {
        val value = getRegister(rX).toInt() and 0xFF
        if (value > 0x0F) {
            throw ProgramTerminatedException("Cannot convert non-hex digit to ASCII: $value")
        }
        val ascii = if (value <= 9) {
            (0x30 + value).toUByte()
        } else {
            (0x41 + (value - 10)).toUByte()
        }
        setRegister(rY, ascii)
    }

    internal fun draw(rX: Int, rY: Int, rZ: Int) {
        val ascii = getRegister(rX).toInt() and 0xFF
        if (ascii > 0x7F) {
            throw ProgramTerminatedException("DRAW value must be <= 0x7F, got 0x${ascii.toString(16)}")
        }
        val row = getRegister(rY).toInt() and 0xFF
        val col = getRegister(rZ).toInt() and 0xFF
        display.draw(row, col, ascii.toUByte())
    }

    private fun ensureEvenProgramCounter(value: Int) {
        if (value % 2 != 0) {
            throw ProgramTerminatedException("Program counter must be even. Received: $value")
        }
    }

    private fun validateRegister(index: Int) {
        if (index !in 0..7) {
            throw ProgramTerminatedException("Register index out of bounds: $index")
        }
    }
}
