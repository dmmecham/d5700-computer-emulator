package d5700

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class D5700CpuTest {
    private fun cpuFixture(
        romWritable: Boolean = false,
        keyboardInput: KeyboardInput = StubKeyboardInput { 0u }
    ): Fixture {
        val rom = D5700Rom(writable = romWritable)
        val ram = D5700Ram()
        val display = D5700Display()
        val timer = D5700Timer(ManualTimerSchedulerStrategy())
        val cpu = D5700CPU(rom, ram, keyboardInput, display, timer)
        return Fixture(cpu, rom, ram, display, timer)
    }

    private fun loadProgram(rom: D5700Rom, vararg instructions: Int) {
        val bytes = mutableListOf<UByte>()
        instructions.forEach { instruction ->
            bytes.add(((instruction ushr 8) and 0xFF).toUByte())
            bytes.add((instruction and 0xFF).toUByte())
        }
        rom.load(bytes.toUByteArray())
    }

    @Test
    fun store_sets_register_value() {
        val f = cpuFixture()
        loadProgram(f.rom, 0x00FF)

        f.cpu.step()

        assertEquals(0xFFu, f.cpu.registers[0])
        assertEquals(2, f.cpu.programCounter)
    }

    @Test
    fun add_wraps_to_byte() {
        val f = cpuFixture()
        loadProgram(f.rom, 0x00FF, 0x0101, 0x1012)

        f.cpu.step()
        f.cpu.step()
        f.cpu.step()

        assertEquals(0x00u, f.cpu.registers[2])
    }

    @Test
    fun subtract_wraps_to_byte() {
        val f = cpuFixture()
        loadProgram(f.rom, 0x0001, 0x0102, 0x2012)

        f.cpu.step()
        f.cpu.step()
        f.cpu.step()

        assertEquals(0xFFu, f.cpu.registers[2])
    }

    @Test
    fun read_and_write_use_ram_when_memory_select_is_zero() {
        val f = cpuFixture()
        loadProgram(f.rom, 0x00AB, 0xA100, 0x4000, 0x3100)

        repeat(4) { f.cpu.step() }

        assertEquals(0xABu, f.ram.read(0x100))
        assertEquals(0xABu, f.cpu.registers[1])
    }

    @Test
    fun read_uses_rom_when_memory_select_is_one() {
        val f = cpuFixture()
        // Program: set A=0x000, switch to ROM, read memory at A into r2
        loadProgram(f.rom, 0xA000, 0x7000, 0x3200)

        repeat(3) { f.cpu.step() }

        // ROM at 0x000 contains the high byte of instruction 0xA000, which is 0xA0
        assertEquals(0xA0u, f.cpu.registers[2])
    }

    @Test
    fun write_to_rom_throws_when_not_writable() {
        val f = cpuFixture(romWritable = false)
        loadProgram(f.rom, 0x0012, 0x7000, 0xA010, 0x4000)

        f.cpu.step()
        f.cpu.step()
        f.cpu.step()

        assertFailsWith<RomWriteException> { f.cpu.step() }
    }

    @Test
    fun switch_memory_toggles_between_ram_and_rom() {
        val f = cpuFixture()
        loadProgram(f.rom, 0x7000, 0x7000)

        assertEquals(false, f.cpu.memorySelectRom)
        f.cpu.step()
        assertEquals(true, f.cpu.memorySelectRom)
        f.cpu.step()
        assertEquals(false, f.cpu.memorySelectRom)
    }

    @Test
    fun jump_sets_program_counter_without_auto_increment() {
        val f = cpuFixture()
        loadProgram(f.rom, 0x5004, 0x0000, 0x00AA)

        f.cpu.step()

        assertEquals(4, f.cpu.programCounter)
        f.cpu.step()
        assertEquals(0xAAu, f.cpu.registers[0])
    }

    @Test
    fun jump_to_odd_address_terminates_program() {
        val f = cpuFixture()
        loadProgram(f.rom, 0x5001)

        assertFailsWith<ProgramTerminatedException> { f.cpu.step() }
    }

    @Test
    fun skip_equal_skips_next_instruction() {
        val f = cpuFixture()
        loadProgram(f.rom, 0x0002, 0x0102, 0x8010, 0x02FF, 0x02AA)

        repeat(4) { f.cpu.step() }

        assertEquals(0xAAu, f.cpu.registers[2])
    }

    @Test
    fun skip_equal_does_not_skip_when_values_differ() {
        val f = cpuFixture()
        loadProgram(f.rom, 0x0002, 0x0103, 0x8010, 0x02AA, 0x02BB)

        f.cpu.step()
        f.cpu.step()
        f.cpu.step()
        assertEquals(6, f.cpu.programCounter)
        f.cpu.step()

        assertEquals(0xAAu, f.cpu.registers[2])
    }

    @Test
    fun skip_not_equal_skips_next_instruction() {
        val f = cpuFixture()
        loadProgram(f.rom, 0x0002, 0x0103, 0x9010, 0x02FF, 0x02AA)

        repeat(4) { f.cpu.step() }

        assertEquals(0xAAu, f.cpu.registers[2])
    }

    @Test
    fun skip_not_equal_does_not_skip_when_values_match() {
        val f = cpuFixture()
        loadProgram(f.rom, 0x0002, 0x0102, 0x9010, 0x02AA, 0x02BB)

        f.cpu.step()
        f.cpu.step()
        f.cpu.step()
        assertEquals(6, f.cpu.programCounter)
        f.cpu.step()

        assertEquals(0xAAu, f.cpu.registers[2])
    }

    @Test
    fun set_a_sets_address_register() {
        val f = cpuFixture()
        loadProgram(f.rom, 0xA2A5)

        f.cpu.step()

        assertEquals(0x2A5, f.cpu.addressRegister)
    }

    @Test
    fun set_t_and_read_t_work_with_manual_ticks() {
        val scheduler = ManualTimerSchedulerStrategy()
        val rom = D5700Rom()
        val ram = D5700Ram()
        val display = D5700Display()
        val timer = D5700Timer(scheduler)
        val cpu = D5700CPU(rom, ram, StubKeyboardInput { 0u }, display, timer)
        loadProgram(rom, 0xB0A0, 0xC100)

        timer.start()
        cpu.step()
        scheduler.tick(3)
        cpu.step()

        assertEquals(7u, cpu.registers[1])
        timer.stop()
    }

    @Test
    fun convert_to_base_10_writes_digits_to_memory() {
        val f = cpuFixture()
        loadProgram(f.rom, 0x00FF, 0xA050, 0xD000)

        repeat(3) { f.cpu.step() }

        assertEquals(2u, f.ram.read(0x50))
        assertEquals(5u, f.ram.read(0x51))
        assertEquals(5u, f.ram.read(0x52))
    }

    @Test
    fun convert_byte_to_ascii_converts_hex_digit() {
        val f = cpuFixture()
        loadProgram(f.rom, 0x000A, 0xE010)

        repeat(2) { f.cpu.step() }

        assertEquals(0x41u, f.cpu.registers[1])
    }

    @Test
    fun convert_byte_to_ascii_throws_for_non_hex_value() {
        val f = cpuFixture()
        loadProgram(f.rom, 0x0010, 0xE010)

        f.cpu.step()
        assertFailsWith<ProgramTerminatedException> { f.cpu.step() }
    }

    @Test
    fun draw_places_ascii_character_on_screen() {
        val f = cpuFixture()
        loadProgram(f.rom, 0x0041, 0x0102, 0x0203, 0xF012)

        repeat(4) { f.cpu.step() }

        val buffer = f.display.snapshot()
        assertEquals(0x41u, buffer[2 * 8 + 3])
    }

    @Test
    fun draw_throws_for_non_ascii_byte() {
        val f = cpuFixture()
        loadProgram(f.rom, 0x00FF, 0x0100, 0x0200, 0xF012)

        f.cpu.step()
        f.cpu.step()
        f.cpu.step()
        assertFailsWith<ProgramTerminatedException> { f.cpu.step() }
    }

    @Test
    fun instruction_0000_halts_program() {
        val f = cpuFixture()
        loadProgram(f.rom, 0x0000, 0x00AA)

        f.cpu.step()

        assertTrue(f.cpu.halted)
        assertEquals(0, f.cpu.programCounter)
    }

    @Test
    fun normal_instructions_increment_program_counter_by_two() {
        val f = cpuFixture()
        loadProgram(f.rom, 0x0001, 0x0102, 0x2012)

        assertEquals(0, f.cpu.programCounter)
        f.cpu.step()
        assertEquals(2, f.cpu.programCounter)
        f.cpu.step()
        assertEquals(4, f.cpu.programCounter)
        f.cpu.step()
        assertEquals(6, f.cpu.programCounter)
    }

    @Test
    fun keyboard_instruction_reads_parsed_hex_input() {
        val keyboard = ConsoleKeyboardInput()
        assertEquals(0xAFu, keyboard.parseHexInput("AF2"))
        assertEquals(0u, keyboard.parseHexInput(""))
        assertEquals(0x1Bu, keyboard.parseHexInput("1BZZ"))
    }

    @Test
    fun timer_decrements_while_keyboard_instruction_blocks() {
        val rom = D5700Rom()
        val ram = D5700Ram()
        val display = D5700Display()
        val timer = D5700Timer(RealTimeTimerSchedulerStrategy())
        val keyboard = StubKeyboardInput {
            Thread.sleep(90)
            0u
        }
        val cpu = D5700CPU(rom, ram, keyboard, display, timer)

        loadProgram(rom, 0xB050, 0x6000, 0xC100)

        timer.start()
        cpu.step()
        cpu.step()
        cpu.step()
        timer.stop()

        assertTrue(cpu.registers[1].toInt() < 5)
    }

    private data class Fixture(
        val cpu: D5700CPU,
        val rom: D5700Rom,
        val ram: D5700Ram,
        val display: D5700Display,
        val timer: D5700Timer
    )
}
