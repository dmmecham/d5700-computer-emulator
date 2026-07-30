package d5700

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class RomLoaderTest {
    private fun fixturePath(name: String): String {
        return File("src/test/resources/fixtures/$name").absolutePath
    }

    @Test
    fun loads_d5700_text_program() {
        val bytes = D5700RomLoader.loadFromPath(fixturePath("program.d5700"))

        assertContentEquals(ubyteArrayOf(0x00u, 0xFFu, 0xA1u, 0x23u), bytes)
    }

    @Test
    fun loads_binary_program() {
        val bytes = D5700RomLoader.loadFromPath(fixturePath("program.out"))

        assertContentEquals(ubyteArrayOf(0x00u, 0x11u, 0x22u, 0x33u), bytes)
    }

    @Test
    fun throws_for_invalid_d5700_line() {
        assertFailsWith<IllegalArgumentException> {
            D5700RomLoader.loadFromPath(fixturePath("invalid.d5700"))
        }
    }
}
