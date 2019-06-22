package charlie.lcsetools.snxutil.parsed

import charlie.lcsetools.snxutil.raw.LCSERawSNXScript
import charlie.lcsetools.snxutil.raw.readSNXScript
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.*

val SHIFT_JIS = Charset.forName("Shift-JIS")!!
val GBK = Charset.forName("GBK")!!

fun parseRawSNXScript(script: LCSERawSNXScript): LCSEParsedScript
        = LCSEParsedScript().apply {
    val dialogStringOrdinals = ArrayList<Int>()
    val choiceStringOrdinals = ArrayList<Int>()
    script.instructions.forEach { (instruction, param1, param2) ->
        instructions +=
                if (instruction == STRING_REF_INST && param1 == STRING_REF_PARAM1)
                    LCSEStringReferInstruction(script.strings.indexOfFirst { it.offset == param2 })
                else if (instruction == DISPLAY_TEXT_INST && param1 == DISPLAY_TEXT_PARAM1 && param2 == DISPLAY_TEXT_PARAM2)
                    LCSEStringDisplayInstruction()
                else if (instruction == CHOICE_INST && param1 == CHOICE_PARAM1 && param2 == CHOICE_PARAM2)
                    LCSEChoiceInstruction()
                else
                    LCSEUnknownInstruction(instruction, param1, param2)
    }

    val stringStack = Stack<Int>()
    instructions.forEach {
        if (it is LCSEStringReferInstruction)
            stringStack.push(it.stringOrdinary)
        else if (it is LCSEStringDisplayInstruction && !stringStack.empty())
            dialogStringOrdinals += stringStack.pop()
        else if (it is LCSEChoiceInstruction) {
            val str2 = stringStack.pop() // LIFO
            val str1 = stringStack.pop()

            choiceStringOrdinals += str1
            choiceStringOrdinals += str2
        }
    }

    script.strings.forEachIndexed { ordinal, content ->
        when {
            dialogStringOrdinals.contains(ordinal) -> this.strings += parseDialogString(byteArrayToString(content.content), this)
            choiceStringOrdinals.contains(ordinal) -> this.strings += LCSEChoiceString(strings.size, byteArrayToString(content.content))
            else -> this.strings += LCSESystemString(strings.size, byteArrayToString(content.content))
        }
    }
}

fun byteArrayToString(arr: ByteArray) = String(arr, 0, arr.indexOfFirst { it == 0x00.toByte() }, SHIFT_JIS)

fun parseDialogString(str: String, context: LCSEParsedScript): LCSEString {
    val withTrailer = str.endsWith("\u0002\u0003")
    val elements = str.replace("\u0001", "\n")
            .removeSuffix("\u0002\u0003")
            .split('\n', limit = 2)
    if (elements.size < 2)
        return LCSESystemString(context.strings.size, elements[0])
    val speakerOrdinal = (context.speakers.find { it.name == elements[0] }
            ?: LCSESpeaker(context.speakers.size, elements[0]).apply { context.speakers += this }).ordinal

    return LCSEDialogString(context.strings.size, speakerOrdinal, elements[1], withTrailer)
}

fun main(args: Array<String>) {
    FileChannel.open(Paths.get("D:\\gal\\无限錬姦\\mugen\\originalSNX\\AGO01_04_03.snx"), StandardOpenOption.READ).use {
        val originalBuf = it.map(FileChannel.MapMode.READ_ONLY, 0, it.size())
        val parsedSNX = parseRawSNXScript(readSNXScript(originalBuf))
    }
}