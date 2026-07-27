import java.io.File

fun main(args: Array<String>) {
    val bytes = mutableListOf<Byte>()
    val outfile = File(args[1])

    File(args[0]).forEachLine {
        bytes.add(it.substring(0,2).lowercase().toInt(16).toByte())
        bytes.add(it.substring(2,4).lowercase().toInt(16).toByte())
    }

    outfile.writeBytes(bytes.toByteArray())
}
