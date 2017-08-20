package charlie.lcsetools.snxutil.parsed

import charlie.lcsetools.snxutil.raw.LCSERawSNXScript
import java.nio.charset.Charset

val SHIFT_JIS = Charset.forName("Shift-JIS")!!
val GBK = Charset.forName("GBK")!!

fun parseRawSNXScript(script: LCSERawSNXScript): LCSEParsedScript
        = LCSEParsedScript().apply {
    val dialogStringOrdinals = ArrayList<Int>()
    script.instructions.forEach { (instruction, param1, param2) ->
        if (instruction == STRING_REF_INST && param1 == STRING_REF_PARAM1)
            instructions += LCSEStringReferInstruction(script.strings.indexOfFirst { it.offset == param2 })
        else if (instruction == DISPLAY_TEXT_INST && param1 == DISPLAY_TEXT_PARAM1 && param2 == DISPLAY_TEXT_PARAM2)
            instructions += LCSEStringDisplayInstruction()
        else
            instructions += LCSEUnknownInstruction(instruction, param1, param2)
    }

    var registerString = -1
    instructions.forEach {
        if (it is LCSEStringReferInstruction)
            registerString = it.stringOrdinary
        else if (it is LCSEStringDisplayInstruction && registerString != -1)
            dialogStringOrdinals += registerString
    }

    script.strings.forEachIndexed { ordinal, content ->
        if (dialogStringOrdinals.contains(ordinal)) {
            this.strings += parseDialogString(byteArrayToString(content.content), this)
        } else {
            this.strings += LCSESystemString(strings.size, byteArrayToString(content.content))
        }
    }
}

fun byteArrayToString(arr: ByteArray) = String(arr, 0, arr.indexOfFirst { it == 0x00.toByte() }, SHIFT_JIS)

fun parseDialogString(str: String, context: LCSEParsedScript): LCSEString {
    val elements = str.replace("\u0001", "\n")
            .removeSuffix("\u0002\u0003")
            .split('\n', limit = 2)
    if (elements.size < 2)
        return LCSESystemString(context.strings.size, elements[0])
    val speakerOrdinal = (context.speakers.find { it.name == elements[0] }
            ?: LCSESpeaker(context.speakers.size, elements[0]).apply { context.speakers += this }).ordinal

    return LCSEDialogString(context.strings.size, speakerOrdinal, elements[1])
}