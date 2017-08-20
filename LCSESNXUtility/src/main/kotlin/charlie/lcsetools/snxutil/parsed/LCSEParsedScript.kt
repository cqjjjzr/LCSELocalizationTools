package charlie.lcsetools.snxutil.parsed

import java.io.Serializable

class LCSEParsedScript: Serializable {
    val strings: MutableList<LCSEString> = ArrayList()
    val instructions: MutableList<LCSEInstruction> = ArrayList()

    val speakers: MutableList<LCSESpeaker> = ArrayList()
}

interface LCSEInstruction: Serializable

interface LCSEString: Serializable {
    val ordinal: Int
    fun convertToByteArray(context: LCSEParsedScript): ByteArray
}

data class LCSEUnknownInstruction(val instruction: Int,
                                  val param1: Int,
                                  val param2: Int): LCSEInstruction


internal val STRING_REF_INST = 0x00000011
internal val STRING_REF_PARAM1 = 0x00000002
data class LCSEStringReferInstruction(val stringOrdinary: Int): LCSEInstruction

internal val DISPLAY_TEXT_INST = 0x0000000D
internal val DISPLAY_TEXT_PARAM1 = 0x0000002C
internal val DISPLAY_TEXT_PARAM2 = 0x00000000
class LCSEStringDisplayInstruction: LCSEInstruction

data class LCSESpeaker(val ordinal: Int,
                       val name: String): Serializable

data class LCSEDialogString(override val ordinal: Int,
                            val speakerOrdinal: Int,
                            val content: String): LCSEString {
    override fun convertToByteArray(context: LCSEParsedScript): ByteArray {
        return (context.speakers.find { it.ordinal == speakerOrdinal }!!.name +
                '\u0001' + content.replace('\n', '\u0001') + "\u0002\u0003\u0000")
                .toByteArray(GBK)
    }
}

data class LCSESystemString(override val ordinal: Int,
                            val content: String): LCSEString {
    override fun convertToByteArray(context: LCSEParsedScript): ByteArray {
        return (content + "\u0000").toByteArray(GBK)
    }
}