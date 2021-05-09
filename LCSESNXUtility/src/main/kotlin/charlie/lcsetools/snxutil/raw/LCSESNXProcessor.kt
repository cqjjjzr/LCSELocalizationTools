package charlie.lcsetools.snxutil.raw

import charlie.lcsetools.snxutil.raw.LCSERawSNXInstruction.Companion.INSTRUCTION_LENGTH
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Suppress("UsePropertyAccessSyntax")
fun readSNXScript(buffer: ByteBuffer): LCSERawSNXScript {
    val leBuffer = buffer.order(ByteOrder.LITTLE_ENDIAN)
    return LCSERawSNXScript().apply {
        val instructionsCount = leBuffer.getInt()
        val stringTableLength = leBuffer.getInt()

        if (leBuffer.limit() != 4 + 4 + instructionsCount * INSTRUCTION_LENGTH + stringTableLength)
            throw IllegalArgumentException("文件格式错误！")

        instructions.ensureCapacity(instructionsCount)
        repeat(instructionsCount) {
            instructions += LCSERawSNXInstruction(leBuffer.getInt(), leBuffer.getInt(), leBuffer.getInt())
        }

        val stringTableStartingOffset = leBuffer.position()
        var ordinal = 0
        while (leBuffer.remaining() >= 4) {
            val length = leBuffer.getInt()
            val tempBuf = ByteArray(length)
            leBuffer[tempBuf, 0, length]
            strings += LCSERawSNXString(ordinal++, leBuffer.position() - 4 - length - stringTableStartingOffset, tempBuf)
        }
    }
}

fun writeSNXScript(script: LCSERawSNXScript): ByteBuffer {
    val stringTableLength = script.stringTableLength
    return ByteBuffer.allocate(4 + 4
            + script.instructions.size * INSTRUCTION_LENGTH
            + stringTableLength).order(ByteOrder.LITTLE_ENDIAN).apply {
        putInt(script.instructions.size)
        putInt(stringTableLength)
        script.instructions.forEach {
            putInt(it.instruction)
            putInt(it.param1)
            putInt(it.param2)
        }

        script.strings.sortedBy { it.offset }.forEach {
            putInt(it.content.size)
            put(it.content, 0, it.content.size)
        }

        flip()
    }
}