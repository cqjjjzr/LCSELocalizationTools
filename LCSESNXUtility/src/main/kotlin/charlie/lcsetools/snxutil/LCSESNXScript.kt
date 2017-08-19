package charlie.lcsetools.snxutil

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

val SHIFT_JIS = Charset.forName("Shift-JIS")!!
val GBK = Charset.forName("GBK")!!


val PUSH_STACK_INST = 0x00000011
val PUSH_STACK_PARAM1 = 0x00000000
val DISPLAY_TEXT_INST = 0x0000000D
val DISPLAY_TEXT_PARAM1 = 0x0000002C
val DISPLAY_TEXT_PARAM2 = 0x00000000
/*
 * Byte code system of LCSE is stack-based, instruction to display dialog text, these instruction is used:
 * PUSH    00     <STRING_ID>
 * DISPLAY DIALOG 00
 * Hex:
 * 00000011 00000000 <STRING_ID>
 * 0000000D 0000002C 00000000
 */
class LCSESNXScript {
    val instructions: MutableList<LCSESNXScriptInstruction> = ArrayList()
    val stringTable: LCSESNXStringTable = LCSESNXStringTable()
    val dialogStringOrdinals: MutableList<Int> = ArrayList()

    companion object {
        fun readFromBuffer(fullFileBuffer: ByteBuffer): LCSESNXScript {
            val leBuffer = fullFileBuffer.order(ByteOrder.LITTLE_ENDIAN)
            val instructionsCount = leBuffer.int
            val stringTableLength = leBuffer.int
            if (leBuffer.limit() != 4 + 4 + instructionsCount * LCSESNXScriptInstruction.INSTRUCTION_LENGTH + stringTableLength)
                throw IllegalArgumentException("文件格式错误！")
            return LCSESNXScript().apply {
                repeat(instructionsCount) {
                    instructions += LCSESNXScriptInstruction(fullFileBuffer.int, fullFileBuffer.int, fullFileBuffer.int)
                }

                stringTable.fillFromBuffer(leBuffer)
                resolveDialogStrings()
            }
        }
    }

    fun resolveDialogStrings() {
        var stackTop = 0x00000000
        var stackUpdated = false
        instructions.forEach {
            if (it.instruction == PUSH_STACK_INST && it.param1 == PUSH_STACK_PARAM1) {
                stackTop = it.param2
                stackUpdated = true
            }
            else if (it.instruction == DISPLAY_TEXT_INST
                    && it.param1 == DISPLAY_TEXT_PARAM1
                    && it.param2 == DISPLAY_TEXT_PARAM2
                         && stackUpdated)
                dialogStringOrdinals += stackTop
        }
    }

    fun writePoTemplate(path: Path) {
        if (!Files.exists(path)) Files.createFile(path)
        Files.write(path, LCSETextProcessor(LCSESNXStringTable()
                .apply {
                    entries.addAll(stringTable.entries
                            .filter { dialogStringOrdinals.contains(it.ordinal) })
                }).makePoTemplate().toByteArray(), StandardOpenOption.TRUNCATE_EXISTING)
    }
}

data class LCSESNXScriptInstruction(val instruction: Int,
                               val param1: Int,
                               val param2: Int) {
    companion object {
        @JvmField
        val INSTRUCTION_LENGTH = 4 * 3
    }
}

class LCSESNXStringTable {
    val entries: MutableList<LCSERawStringEntry> = ArrayList()

    fun fillFromBuffer(fullFileBuffer: ByteBuffer) {
        var ordinal = 0
        while (fullFileBuffer.remaining() > 4) {
            val length = fullFileBuffer.int
            val tempBuf = ByteArray(length)
            fullFileBuffer[tempBuf, 0, length]
            entries += LCSERawStringEntry(ordinal, tempBuf)

            ordinal++
        }
    }
}

data class LCSERawStringEntry(val ordinal: Int,
                              var content: ByteArray)