package charlie.lcsetools.snxutil

import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import org.fedorahosted.tennera.jgettext.Catalog
import org.fedorahosted.tennera.jgettext.Message
import org.fedorahosted.tennera.jgettext.PoWriter
import java.io.ByteArrayOutputStream
import java.lang.Integer.parseInt

class LCSETextProcessor(val rawStrings: LCSESNXStringTable) {
    val speakers: MutableList<LCSESpeaker> = ArrayList()
    val parsedStrings: MutableList<LCSEDialogText> = ArrayList()

    private fun parse(rawString: LCSERawStringEntry): LCSEDialogText {
        var (speakerStr, content) = escape(rawString.content).split("<1>")
        val hasSuffix = if (content.endsWith("<02><03>")) {
            content = content.removeSuffix("<02><03>")
            true
        } else false
        val speakerOrdinal = if (speakers.isEmpty()) {
            speakers += LCSESpeaker(0, speakerStr)
            0
        } else {
            (speakers
                    .filter { it.originalString == speakerStr }
                    .firstOrNull() ?: (LCSESpeaker(speakers.last().ordinal + 1, speakerStr).apply { speakers += this }))
                    .ordinal
        }
        return LCSEDialogText(rawString.ordinal, speakerOrdinal, content, hasSuffix)
    }

    fun makePoTemplate(): String {
        return ByteArrayOutputStream().let {
            PoWriter().write(Catalog(true).apply {
                speakers.forEach {
                    addMessage(Message().apply {
                        msgctxt = "speaker"
                        msgid = it.originalString
                        msgstr = ""
                        addComment(LCSESpeaker.MOSHI_ADAPTER.toJson(it.extraData))
                    })
                }

                parsedStrings.forEach {
                    addMessage(Message().apply {
                        msgctxt = "dialog"
                        msgid = it.originalString
                        msgstr = ""
                        addComment(LCSEDialogText.MOSHI_ADAPTER.toJson(it.extraData))
                    })
                }
            }, it)
            String(it.toByteArray())
        }
    }

    init {
        parsedStrings.addAll(rawStrings.entries.map { parse(it) })
    }
}

data class LCSEDialogText(val ordinal: Int, val speakerOrdinal: Int, val originalString: String, val hasSuffix: Boolean, val translatedString: String = "") {
    companion object {
        val MOSHI_ADAPTER = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build().adapter(LCSEDialogTextExtraData::class.java)!!

        fun fromJson(originalString: String, translatedString: String, extraData: LCSEDialogTextExtraData)
                = LCSEDialogText(extraData.original, extraData.speakerOrdinal, originalString, extraData.hasSuffix, translatedString)
    }

    val extraData get() = LCSEDialogTextExtraData(ordinal, speakerOrdinal, hasSuffix)

    fun toByteArray(processor: LCSETextProcessor): ByteArray {
        val str = recoverEscape(if (translatedString.isBlank()) originalString else translatedString)
        val speaker = processor.speakers.filter { it.ordinal == speakerOrdinal }.first().translatedString.toByteArray(GBK)
        val len = speaker.size + 1 + str.size + (if (hasSuffix) 2 else 0) + 1

        return ByteArray(len).apply {
            var ptr = 0
            System.arraycopy(speaker, 0, this, ptr, speaker.size)
            ptr += speaker.size
            System.arraycopy(str, 0, this, ptr, str.size)
            ptr += str.size
            if (hasSuffix) {
                str[++ptr] = 0x02
                str[++ptr] = 0x03
            }
            str[++ptr] = 0x00
        }
    }

    data class LCSEDialogTextExtraData(val original: Int, val speakerOrdinal: Int, val hasSuffix: Boolean)
}

data class LCSESpeaker(val ordinal: Int, val originalString: String, private val _translatedString: String = "") {
    companion object {
        val MOSHI_ADAPTER = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build().adapter(LCSESpeakerExtraData::class.java)!!
    }

    val translatedString get() = if (_translatedString.isBlank()) originalString else _translatedString
    val extraData get() = LCSESpeakerExtraData(ordinal)

    data class LCSESpeakerExtraData(val ordinal: Int)
}

fun escape(arr: ByteArray): String {
    return String(arr, 0, arr.indexOfFirst { it == 0.toByte() }, SHIFT_JIS).toCharArray()
            .map { if (it < 20.toChar()) it.toString() else "<${it.toInt()}>" }
            .joinToString(separator = "") { it }
}

fun recoverEscape(str: String): ByteArray {
    return str.replace(Regex("<(\\d+)>")) { out -> String(CharArray(1, { parseInt(out.value).toChar() })) }
            .toByteArray(GBK).let {
        if (it.size > 1) {
            val lst: MutableList<Byte> = ArrayList(it.size)
            val i = 1
            while (i < it.size) {
                if (it[i - 1] == 0.toByte() && it[i] < 20)
                    lst += it[i]
                else {
                    lst += it[i - 1]
                    lst += it[i]
                }
            }
            lst.toByteArray()
        } else it
    }
}