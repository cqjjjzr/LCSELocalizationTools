package charlie.lcsetools.snxutil.parsed

import charlie.lcsetools.snxutil.raw.LCSERawSNXInstruction
import charlie.lcsetools.snxutil.raw.LCSERawSNXScript
import java.io.Serializable

class LCSEParsedScript: Serializable {
    val strings: MutableList<LCSEString> = ArrayList()
    val instructions: MutableList<LCSEInstruction> = ArrayList()

    val speakers: MutableList<LCSESpeaker> = ArrayList()
}

interface LCSEInstruction: Serializable {
    fun toRawInstruction(context: LCSERawSNXScript): LCSERawSNXInstruction
}

interface LCSEString: Serializable {
    val ordinal: Int
    fun convertToByteArray(context: LCSEParsedScript): ByteArray
}

data class LCSEUnknownInstruction(val instruction: Int,
                                  val param1: Int,
                                  val param2: Int): LCSEInstruction {
    override fun toRawInstruction(context: LCSERawSNXScript): LCSERawSNXInstruction
            = LCSERawSNXInstruction(instruction, param1, param2)
}


internal val STRING_REF_INST = 0x00000011
internal val STRING_REF_PARAM1 = 0x00000002
data class LCSEStringReferInstruction(val stringOrdinary: Int): LCSEInstruction {
    override fun toRawInstruction(context: LCSERawSNXScript): LCSERawSNXInstruction
            = LCSERawSNXInstruction(STRING_REF_INST, STRING_REF_PARAM1, context.strings.find { it.ordinal == stringOrdinary }!!.offset)
}

internal val DISPLAY_TEXT_INST = 0x0000000D
internal val DISPLAY_TEXT_PARAM1 = 0x0000002C
internal val DISPLAY_TEXT_PARAM2 = 0x00000000
internal val DISPLAY_TEXT_RAW = LCSERawSNXInstruction(DISPLAY_TEXT_INST, DISPLAY_TEXT_PARAM1, DISPLAY_TEXT_PARAM2)
class LCSEStringDisplayInstruction: LCSEInstruction {
    override fun toRawInstruction(context: LCSERawSNXScript): LCSERawSNXInstruction = DISPLAY_TEXT_RAW
}

internal val CHOICE_INST = 0x0000000D
internal val CHOICE_PARAM1 = 0x0000004F
internal val CHOICE_PARAM2 = 0x00000000
internal val CHOICE_RAW = LCSERawSNXInstruction(CHOICE_INST, CHOICE_PARAM1, CHOICE_PARAM2)
class LCSEChoiceInstruction: LCSEInstruction {
    override fun toRawInstruction(context: LCSERawSNXScript): LCSERawSNXInstruction = CHOICE_RAW
}

data class LCSESpeaker(val ordinal: Int,
                       val name: String): Serializable

data class LCSEDialogString(override val ordinal: Int,
                            val speakerOrdinal: Int,
                            val content: String,
                            val withTrailer: Boolean = false): LCSEString {
    override fun convertToByteArray(context: LCSEParsedScript): ByteArray {
        return (context.speakers.find { it.ordinal == speakerOrdinal }!!.name +
                '\u0001' + content.replace('\n', '\u0001') +
                if (withTrailer) "\u0002\u0003\u0000" else "\u0000")
                .toByteArray(GBK)
    }
}

data class LCSEChoiceString(override val ordinal: Int,
                            val content: String): LCSEString {
    override fun convertToByteArray(context: LCSEParsedScript): ByteArray {
        return (content + "\u0000").toByteArray(GBK)
    }
}

data class LCSESystemString(override val ordinal: Int,
                            val content: String): LCSEString {
    override fun convertToByteArray(context: LCSEParsedScript): ByteArray {
        return (content + "\u0000").toByteArray(GBK)
    }
}