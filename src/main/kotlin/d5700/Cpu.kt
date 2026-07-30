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
            halt()
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

    internal fun readRegister(index: Int): UByte = getRegister(index)

    internal fun writeRegister(index: Int, value: UByte) {
        setRegister(index, value)
    }

    internal fun readMemoryAtAddress(address: Int): UByte {
        val memory = if (memorySelectRom) rom else ram
        return memory.read(address)
    }

    internal fun writeMemoryAtAddress(address: Int, value: UByte) {
        val memory = if (memorySelectRom) rom else ram
        memory.write(address, value)
    }

    internal fun getAddressRegister(): Int = addressRegister

    internal fun setProgramCounter(value: Int) {
        ensureEvenProgramCounter(value)
        programCounter = value
        pendingProgramCounterAdvance = 0
    }

    internal fun setPendingProgramCounterAdvance(value: Int) {
        pendingProgramCounterAdvance = value
    }

    internal fun readKeyboard(): UByte = keyboard.readHexByte()

    internal fun toggleMemorySelection() {
        memorySelectRom = !memorySelectRom
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

    internal fun getTimer(): UByte = timer.get()

    internal fun drawCharacter(row: Int, column: Int, value: UByte) {
        display.draw(row, column, value)
    }

    internal fun halt() {
        halted = true
        pendingProgramCounterAdvance = 0
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
