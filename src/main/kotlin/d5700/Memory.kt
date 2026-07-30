package d5700

interface D5700AddressableMemory {
    val size: Int
    fun read(address: Int): UByte
    fun write(address: Int, value: UByte)
}

class D5700Ram(override val size: Int = 4096) : D5700AddressableMemory {
    private val storage = UByteArray(size)

    override fun read(address: Int): UByte {
        validateAddress(address)
        return storage[address]
    }

    override fun write(address: Int, value: UByte) {
        validateAddress(address)
        storage[address] = value
    }

    fun clear() {
        storage.fill(0u)
    }

    private fun validateAddress(address: Int) {
        if (address !in 0 until size) {
            throw MemoryAccessException("RAM address out of bounds: $address")
        }
    }
}

class D5700Rom(
    override val size: Int = 4096,
    private val writable: Boolean = false
) : D5700AddressableMemory {
    private val storage = UByteArray(size)

    fun load(bytes: UByteArray) {
        storage.fill(0u)
        val copySize = minOf(bytes.size, storage.size)
        for (i in 0 until copySize) {
            storage[i] = bytes[i]
        }
    }

    override fun read(address: Int): UByte {
        validateAddress(address)
        return storage[address]
    }

    override fun write(address: Int, value: UByte) {
        validateAddress(address)
        if (!writable) {
            throw RomWriteException("Attempted write to read-only ROM at address $address")
        }
        storage[address] = value
    }

    private fun validateAddress(address: Int) {
        if (address !in 0 until size) {
            throw MemoryAccessException("ROM address out of bounds: $address")
        }
    }
}
