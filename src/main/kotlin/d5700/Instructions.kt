package d5700

fun interface InstructionStrategy {
    fun execute(cpu: D5700CPU, instruction: Int)
}

interface InstructionFactory {
    fun get(opcode: Int): InstructionStrategy?
}

data class InstructionOperands(
    val registerX: Int = -1,
    val registerY: Int = -1,
    val registerZ: Int = -1,
    val address: Int = 0,
    val immediateByte: Int = 0
)

abstract class AbstractInstructionFamily : InstructionStrategy {
    final override fun execute(cpu: D5700CPU, instruction: Int) {
        val operands = decodeOperands(instruction)
        executeWithOperands(cpu, operands)
    }

    protected abstract fun decodeOperands(instruction: Int): InstructionOperands
    protected abstract fun executeWithOperands(cpu: D5700CPU, operands: InstructionOperands)
}

private object LoadImmediateInstruction : AbstractInstructionFamily() {
    override fun decodeOperands(instruction: Int): InstructionOperands = InstructionOperands(
        registerX = (instruction ushr 8) and 0xF,
        immediateByte = instruction and 0xFF
    )

    override fun executeWithOperands(cpu: D5700CPU, operands: InstructionOperands) {
        cpu.writeRegister(operands.registerX, operands.immediateByte.toUByte())
    }
}

private object AddInstruction : AbstractInstructionFamily() {
    override fun decodeOperands(instruction: Int): InstructionOperands = InstructionOperands(
        registerX = (instruction ushr 8) and 0xF,
        registerY = (instruction ushr 4) and 0xF,
        registerZ = instruction and 0xF
    )

    override fun executeWithOperands(cpu: D5700CPU, operands: InstructionOperands) {
        val result = (cpu.readRegister(operands.registerX).toInt() + cpu.readRegister(operands.registerY).toInt()) and 0xFF
        cpu.writeRegister(operands.registerZ, result.toUByte())
    }
}

private object SubtractInstruction : AbstractInstructionFamily() {
    override fun decodeOperands(instruction: Int): InstructionOperands = InstructionOperands(
        registerX = (instruction ushr 8) and 0xF,
        registerY = (instruction ushr 4) and 0xF,
        registerZ = instruction and 0xF
    )

    override fun executeWithOperands(cpu: D5700CPU, operands: InstructionOperands) {
        val result = (cpu.readRegister(operands.registerX).toInt() - cpu.readRegister(operands.registerY).toInt()) and 0xFF
        cpu.writeRegister(operands.registerZ, result.toUByte())
    }
}

private object ReadMemoryInstruction : AbstractInstructionFamily() {
    override fun decodeOperands(instruction: Int): InstructionOperands = InstructionOperands(
        registerX = (instruction ushr 8) and 0xF
    )

    override fun executeWithOperands(cpu: D5700CPU, operands: InstructionOperands) {
        cpu.writeRegister(operands.registerX, cpu.readMemoryAtAddress(cpu.getAddressRegister()))
    }
}

private object WriteMemoryInstruction : AbstractInstructionFamily() {
    override fun decodeOperands(instruction: Int): InstructionOperands = InstructionOperands(
        registerX = (instruction ushr 8) and 0xF
    )

    override fun executeWithOperands(cpu: D5700CPU, operands: InstructionOperands) {
        cpu.writeMemoryAtAddress(cpu.getAddressRegister(), cpu.readRegister(operands.registerX))
    }
}

private object JumpInstruction : AbstractInstructionFamily() {
    override fun decodeOperands(instruction: Int): InstructionOperands = InstructionOperands(
        address = instruction and 0x0FFF
    )

    override fun executeWithOperands(cpu: D5700CPU, operands: InstructionOperands) {
        cpu.setProgramCounter(operands.address)
    }
}

private object ReadKeyboardInstruction : AbstractInstructionFamily() {
    override fun decodeOperands(instruction: Int): InstructionOperands = InstructionOperands(
        registerX = (instruction ushr 8) and 0xF
    )

    override fun executeWithOperands(cpu: D5700CPU, operands: InstructionOperands) {
        cpu.writeRegister(operands.registerX, cpu.readKeyboard())
    }
}

private object ToggleMemorySelectionInstruction : AbstractInstructionFamily() {
    override fun decodeOperands(instruction: Int): InstructionOperands = InstructionOperands()

    override fun executeWithOperands(cpu: D5700CPU, operands: InstructionOperands) {
        cpu.toggleMemorySelection()
    }
}

private object SkipIfEqualInstruction : AbstractInstructionFamily() {
    override fun decodeOperands(instruction: Int): InstructionOperands = InstructionOperands(
        registerX = (instruction ushr 8) and 0xF,
        registerY = (instruction ushr 4) and 0xF
    )

    override fun executeWithOperands(cpu: D5700CPU, operands: InstructionOperands) {
        val skip = cpu.readRegister(operands.registerX) == cpu.readRegister(operands.registerY)
        cpu.setPendingProgramCounterAdvance(if (skip) 4 else 2)
    }
}

private object SkipIfNotEqualInstruction : AbstractInstructionFamily() {
    override fun decodeOperands(instruction: Int): InstructionOperands = InstructionOperands(
        registerX = (instruction ushr 8) and 0xF,
        registerY = (instruction ushr 4) and 0xF
    )

    override fun executeWithOperands(cpu: D5700CPU, operands: InstructionOperands) {
        val skip = cpu.readRegister(operands.registerX) != cpu.readRegister(operands.registerY)
        cpu.setPendingProgramCounterAdvance(if (skip) 4 else 2)
    }
}

private object SetAddressInstruction : AbstractInstructionFamily() {
    override fun decodeOperands(instruction: Int): InstructionOperands = InstructionOperands(
        address = instruction and 0x0FFF
    )

    override fun executeWithOperands(cpu: D5700CPU, operands: InstructionOperands) {
        cpu.setAddress(operands.address)
    }
}

private object SetTimerInstruction : AbstractInstructionFamily() {
    override fun decodeOperands(instruction: Int): InstructionOperands = InstructionOperands(
        immediateByte = (instruction ushr 4) and 0xFF
    )

    override fun executeWithOperands(cpu: D5700CPU, operands: InstructionOperands) {
        cpu.setTimer(operands.immediateByte.toUByte())
    }
}

private object ReadTimerInstruction : AbstractInstructionFamily() {
    override fun decodeOperands(instruction: Int): InstructionOperands = InstructionOperands(
        registerX = (instruction ushr 8) and 0xF
    )

    override fun executeWithOperands(cpu: D5700CPU, operands: InstructionOperands) {
        cpu.writeRegister(operands.registerX, cpu.getTimer())
    }
}

private object ConvertToBase10Instruction : AbstractInstructionFamily() {
    override fun decodeOperands(instruction: Int): InstructionOperands = InstructionOperands(
        registerX = (instruction ushr 8) and 0xF
    )

    override fun executeWithOperands(cpu: D5700CPU, operands: InstructionOperands) {
        val value = cpu.readRegister(operands.registerX).toInt() and 0xFF
        val hundreds = value / 100
        val tens = (value % 100) / 10
        val ones = value % 10

        cpu.writeMemoryAtAddress(cpu.getAddressRegister(), hundreds.toUByte())
        cpu.writeMemoryAtAddress(cpu.getAddressRegister() + 1, tens.toUByte())
        cpu.writeMemoryAtAddress(cpu.getAddressRegister() + 2, ones.toUByte())
    }
}

private object ConvertByteToAsciiInstruction : AbstractInstructionFamily() {
    override fun decodeOperands(instruction: Int): InstructionOperands = InstructionOperands(
        registerX = (instruction ushr 8) and 0xF,
        registerY = (instruction ushr 4) and 0xF
    )

    override fun executeWithOperands(cpu: D5700CPU, operands: InstructionOperands) {
        val value = cpu.readRegister(operands.registerX).toInt() and 0xFF
        if (value > 0x0F) {
            throw ProgramTerminatedException("Cannot convert non-hex digit to ASCII: $value")
        }
        val ascii = if (value <= 9) {
            (0x30 + value).toUByte()
        } else {
            (0x41 + (value - 10)).toUByte()
        }
        cpu.writeRegister(operands.registerY, ascii)
    }
}

private object DrawInstruction : AbstractInstructionFamily() {
    override fun decodeOperands(instruction: Int): InstructionOperands = InstructionOperands(
        registerX = (instruction ushr 8) and 0xF,
        registerY = (instruction ushr 4) and 0xF,
        registerZ = instruction and 0xF
    )

    override fun executeWithOperands(cpu: D5700CPU, operands: InstructionOperands) {
        val ascii = cpu.readRegister(operands.registerX).toInt() and 0xFF
        if (ascii > 0x7F) {
            throw ProgramTerminatedException("DRAW value must be <= 0x7F, got 0x${ascii.toString(16)}")
        }
        val row = cpu.readRegister(operands.registerY).toInt() and 0xFF
        val col = cpu.readRegister(operands.registerZ).toInt() and 0xFF
        cpu.drawCharacter(row, col, ascii.toUByte())
    }
}

object DefaultInstructionFactory : InstructionFactory {
    private val instructionMap: Map<Int, InstructionStrategy> = mapOf(
        0x0 to LoadImmediateInstruction,
        0x1 to AddInstruction,
        0x2 to SubtractInstruction,
        0x3 to ReadMemoryInstruction,
        0x4 to WriteMemoryInstruction,
        0x5 to JumpInstruction,
        0x6 to ReadKeyboardInstruction,
        0x7 to ToggleMemorySelectionInstruction,
        0x8 to SkipIfEqualInstruction,
        0x9 to SkipIfNotEqualInstruction,
        0xA to SetAddressInstruction,
        0xB to SetTimerInstruction,
        0xC to ReadTimerInstruction,
        0xD to ConvertToBase10Instruction,
        0xE to ConvertByteToAsciiInstruction,
        0xF to DrawInstruction
    )

    override fun get(opcode: Int): InstructionStrategy? = instructionMap[opcode]
}
