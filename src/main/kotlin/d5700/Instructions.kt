package d5700

fun interface InstructionStrategy {
    fun execute(cpu: D5700CPU, instruction: Int)
}

interface InstructionFactory {
    fun get(opcode: Int): InstructionStrategy?
}

object DefaultInstructionFactory : InstructionFactory {
    private val instructionMap: Map<Int, InstructionStrategy> = mapOf(
        0x0 to InstructionStrategy { cpu, instruction ->
            val rX = (instruction ushr 8) and 0xF
            val bb = instruction and 0xFF
            cpu.setRegister(rX, bb.toUByte())
        },
        0x1 to InstructionStrategy { cpu, instruction ->
            val rX = (instruction ushr 8) and 0xF
            val rY = (instruction ushr 4) and 0xF
            val rZ = instruction and 0xF
            cpu.add(rX, rY, rZ)
        },
        0x2 to InstructionStrategy { cpu, instruction ->
            val rX = (instruction ushr 8) and 0xF
            val rY = (instruction ushr 4) and 0xF
            val rZ = instruction and 0xF
            cpu.subtract(rX, rY, rZ)
        },
        0x3 to InstructionStrategy { cpu, instruction ->
            val rX = (instruction ushr 8) and 0xF
            cpu.readMemoryInto(rX)
        },
        0x4 to InstructionStrategy { cpu, instruction ->
            val rX = (instruction ushr 8) and 0xF
            cpu.writeRegisterToMemory(rX)
        },
        0x5 to InstructionStrategy { cpu, instruction ->
            val aaa = instruction and 0x0FFF
            cpu.jump(aaa)
        },
        0x6 to InstructionStrategy { cpu, instruction ->
            val rX = (instruction ushr 8) and 0xF
            cpu.readKeyboardInto(rX)
        },
        0x7 to InstructionStrategy { cpu, _ ->
            cpu.toggleMemorySelection()
        },
        0x8 to InstructionStrategy { cpu, instruction ->
            val rX = (instruction ushr 8) and 0xF
            val rY = (instruction ushr 4) and 0xF
            cpu.skipIfEqual(rX, rY)
        },
        0x9 to InstructionStrategy { cpu, instruction ->
            val rX = (instruction ushr 8) and 0xF
            val rY = (instruction ushr 4) and 0xF
            cpu.skipIfNotEqual(rX, rY)
        },
        0xA to InstructionStrategy { cpu, instruction ->
            val aaa = instruction and 0x0FFF
            cpu.setAddress(aaa)
        },
        0xB to InstructionStrategy { cpu, instruction ->
            val bb = (instruction ushr 4) and 0xFF
            cpu.setTimer(bb.toUByte())
        },
        0xC to InstructionStrategy { cpu, instruction ->
            val rX = (instruction ushr 8) and 0xF
            cpu.readTimer(rX)
        },
        0xD to InstructionStrategy { cpu, instruction ->
            val rX = (instruction ushr 8) and 0xF
            cpu.convertToBase10(rX)
        },
        0xE to InstructionStrategy { cpu, instruction ->
            val rX = (instruction ushr 8) and 0xF
            val rY = (instruction ushr 4) and 0xF
            cpu.convertByteToAscii(rX, rY)
        },
        0xF to InstructionStrategy { cpu, instruction ->
            val rX = (instruction ushr 8) and 0xF
            val rY = (instruction ushr 4) and 0xF
            val rZ = instruction and 0xF
            cpu.draw(rX, rY, rZ)
        }
    )

    override fun get(opcode: Int): InstructionStrategy? = instructionMap[opcode]
}
