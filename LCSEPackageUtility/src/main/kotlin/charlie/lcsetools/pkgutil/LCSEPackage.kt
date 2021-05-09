package charlie.lcsetools.pkgutil

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*
import kotlin.experimental.xor

var keyIndex = 0x02020202
fun indexDecryptInt(original: Int) = original xor keyIndex
val indexEncryptInt = ::indexDecryptInt

var keySNX = 0x03030303
fun snxDecryptInt(original: Int) = original xor keySNX
val snxEncryptInt = ::indexDecryptInt
internal fun Int.expandByteToInt(): Int {
    return this + (this shl 8) + (this shl 16) + (this shl 24)
}

val SHIFT_JIS = Charset.forName("SHIFT-JIS")!!

fun createIfNotExist(listPath: Path) {
    if (!Files.exists(listPath)) Files.createFile(listPath)
}

class LCSEIndexList {
    companion object {
        fun readFromBuffer(buffer: ByteBuffer): LCSEIndexList {
            val arr = ByteArray(buffer.limit())
            buffer.position(0)
            buffer[arr, 0, buffer.limit()]

            val buf = ByteBuffer.wrap(arr).order(ByteOrder.LITTLE_ENDIAN)
            val entriesCount = indexDecryptInt(buf.int)

            if (arr.size != 4 + entriesCount * LCSEIndexEntry.ENTRY_SIZE)
                throw IllegalArgumentException("文件长度错误！")

            return LCSEIndexList().apply {
                repeat(entriesCount) {
                    entries += LCSEIndexEntry.readFromBuffer(buf)
                }
            }
        }
    }

    var entries: List<LCSEIndexEntry> = ArrayList()

    fun getByteArray(): ByteArray {
        return ByteBuffer.allocate(4 + entries.size * LCSEIndexEntry.ENTRY_SIZE)
                .order(ByteOrder.LITTLE_ENDIAN)
                .apply {
                    putInt(indexEncryptInt(entries.size))
                    entries.forEach { put(it.getByteArray()) }
                }
                .array()
    }

    fun writePatchedPackage(patches: Iterable<LCSEPatch>, originalPackage: FileChannel, listPath: Path, packagePath: Path) {
        Files.createDirectories(listPath.parent)

        createIfNotExist(listPath)

        if (Files.exists(packagePath)) Files.delete(packagePath)
        Files.createFile(packagePath)

        val newList = LCSEIndexList()
        FileChannel.open(packagePath, StandardOpenOption.WRITE).use { packageChannel ->
            entries.forEach { oldEntry ->
                val applicablePatches = patches
                        .filter { it.filename == oldEntry.filename && it.type == oldEntry.type }
                if (applicablePatches.isNotEmpty()) {
                    val patch = applicablePatches.first()
                    Files.newByteChannel(patch.newFilePath).use { patchChannel ->
                        ByteBuffer.allocate(Files.size(patch.newFilePath).toInt()).apply {
                            patchChannel.read(this)
                            flip()
                            if (patch.type == LCSEResourceType.SCRIPT) {
                                val arr = array()!!
                                arr.forEachIndexed { i, original ->
                                    arr[i] = original xor keySNX.toByte()
                                }
                            }
                            packageChannel.write(this)
                        }
                        newList.entries += LCSEIndexEntry(
                                (packageChannel.position() - patchChannel.size()).toInt(),
                                Files.size(patch.newFilePath).toInt(),
                                oldEntry.filename,
                                oldEntry.type)
                    }
                } else {
                    originalPackage.position(oldEntry.offset.toLong())
                    ByteBuffer.allocate(oldEntry.length).apply {
                        originalPackage.read(this)
                        flip()
                        packageChannel.write(this)
                    }
                    newList.entries += LCSEIndexEntry(
                            (packageChannel.position() - oldEntry.length).toInt(),
                            oldEntry.length,
                            oldEntry.filename,
                            oldEntry.type)
                }
            }
        }

        Files.write(listPath, newList.getByteArray(), StandardOpenOption.TRUNCATE_EXISTING)
    }

    override fun toString(): String {
        return "IndexList(entries=$entries)"
    }
}

class LCSEIndexEntry(var offset: Int,
                     var length: Int,
                     var filename: String,
                     var type: LCSEResourceType) {
    companion object {
        val ENTRY_SIZE = 76
        val FILENAME_SIZE = 0x40

        fun readFromBuffer(buffer: ByteBuffer): LCSEIndexEntry {
            val arr = ByteArray(ENTRY_SIZE)
            buffer[arr, 0, ENTRY_SIZE]

            val buf = ByteBuffer.wrap(arr).order(ByteOrder.LITTLE_ENDIAN)
            val offset = indexDecryptInt(buf.int)
            val length = indexDecryptInt(buf.int)
            var nameBuf = ByteArray(FILENAME_SIZE)
            buf[nameBuf, 0, FILENAME_SIZE]
            nameBuf = nameBuf
                    .takeWhile { it != 0.toByte() }
                    .map { it xor keyIndex.toByte() }
                    .toByteArray()
            val filename = String(nameBuf, SHIFT_JIS)
            val type = LCSEResourceType.forId(buf.int)

            return LCSEIndexEntry(offset, length, filename, type)
        }
    }

    val fullFilename get() = filename + type.extensionName

    fun getByteArray(): ByteArray {
        return ByteBuffer.allocate(ENTRY_SIZE)
                .order(ByteOrder.LITTLE_ENDIAN)
                .apply {
                    putInt(indexEncryptInt(offset))
                    putInt(indexEncryptInt(length))
                    val srcName = filename
                            .toByteArray(SHIFT_JIS)
                            .map { it xor keyIndex.toByte() }
                            .toByteArray()
                    val tempBuf = ByteArray(FILENAME_SIZE)
                    System.arraycopy(srcName, 0, tempBuf, 0, minOf(srcName.size, FILENAME_SIZE - 1))
                    put(tempBuf)
                    putInt(type.typeId)
        }.array()
    }

    fun extractFromChannel(channel: FileChannel)
            = channel.map(FileChannel.MapMode.READ_ONLY, offset.toLong(), length.toLong())
            .let { if (!it.hasArray()) ByteArray(it.limit()).apply { it[this, 0, size] } else it.array()!! }
            .let {
        if (type == LCSEResourceType.SCRIPT) {
            it.map { it xor keySNX.toByte() }.toByteArray()
        } else it
    }

    fun extractFromChannelToFile(channel: FileChannel, outDirectory: Path) {
        Files.createDirectories(outDirectory)
        outDirectory.resolve(fullFilename).apply {
            createIfNotExist(this)
            Files.write(this, extractFromChannel(channel), StandardOpenOption.TRUNCATE_EXISTING)
        }
    }

    override fun toString(): String {
        return "[IndexEntry(offset=$offset, length=$length, filename='$filename', type=$type)]"
    }
}

enum class LCSEResourceType
constructor(internal val typeId: Int,
            internal val extensionName: String) {
    SCRIPT(1, ".snx"),
    BMP_PICTURE(2, ".bmp"),
    PNG_PICTURE(3, ".png"),
    WAVE_AUDIO(4, ".wav"),
    OGG_AUDIO(5, ".ogg"),
    UNKNOWN_1(-1, ".dat"),
    UNKNOWN_2(0, ".dat2");

    companion object {
        fun forId(typeId: Int): LCSEResourceType {
            return LCSEResourceType.values()
                    .filter { it.typeId == typeId }
                    .firstOrNull() ?: SCRIPT
        }

        fun forExtensionName(extensionName: String): LCSEResourceType {
            return LCSEResourceType.values()
                    .filter { it.extensionName ==
                            if (extensionName.startsWith("."))
                                extensionName.toLowerCase()
                            else "." + extensionName.toLowerCase() }
                    .firstOrNull() ?: UNKNOWN_1
        }
    }
}