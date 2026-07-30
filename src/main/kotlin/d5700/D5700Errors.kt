package d5700

open class D5700Exception(message: String) : RuntimeException(message)

class ProgramTerminatedException(message: String) : D5700Exception(message)

class InvalidInstructionException(message: String) : D5700Exception(message)

class MemoryAccessException(message: String) : D5700Exception(message)

class RomWriteException(message: String) : D5700Exception(message)
