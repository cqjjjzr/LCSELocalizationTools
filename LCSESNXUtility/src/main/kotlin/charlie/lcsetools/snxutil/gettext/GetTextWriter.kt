package charlie.lcsetools.snxutil.gettext

import charlie.lcsetools.snxutil.parsed.LCSEChoiceString
import charlie.lcsetools.snxutil.parsed.LCSEDialogString
import charlie.lcsetools.snxutil.parsed.LCSEParsedScript
import charlie.lcsetools.snxutil.parsed.LCSESpeaker
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream
import org.fedorahosted.tennera.jgettext.Catalog
import org.fedorahosted.tennera.jgettext.Message
import org.fedorahosted.tennera.jgettext.PoParser
import org.fedorahosted.tennera.jgettext.PoWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

fun writePoTemplate(script: LCSEParsedScript, path: Path) {
    if (!Files.exists(path)) Files.createFile(path)
    val stream = ByteOutputStream()
    PoWriter().write(Catalog().apply {
        script.speakers.forEach {
            addMessage(Message().apply {
                msgctxt = "speaker"
                msgid = it.name
                msgstr = ""
                addComment(it.ordinal.toString())
            })
        }
        script.strings
                .forEach {
                    when (it) {
                        is LCSEDialogString -> {
                            addMessage(Message().apply {
                                msgctxt = "dialog"
                                msgid = it.content.removePrefix("\u3000") + "|" + it.ordinal
                                msgstr = ""
                                addComment("${it.ordinal}|${it.speakerOrdinal}|${it.content.startsWith("\u3000")}")
                            })
                        }
                        is LCSEChoiceString -> {
                            addMessage(Message().apply {
                                msgctxt = "choice"
                                msgid = it.content.removePrefix("\u3000") + "|" + it.ordinal
                                msgstr = ""
                                addComment("${it.ordinal}|${it.content.startsWith("\\u3000")}")
                            })
                        }
                    }
                }
    }, stream, Charsets.UTF_8)

    val bytes = stream.bytes
    val res = bytes.sliceArray(0 until (bytes.indexOf(0x00).takeIf { it != -1 } ?: bytes.size))

    Files.write(path, res, StandardOpenOption.TRUNCATE_EXISTING)
}

fun convertDialogOrChoiceString(msg: Message): String {
    return buildString {
        if (msg.comments.first().endsWith("true")) append("\u3000")
        (if (msg.msgstr.isNotEmpty())
            msg.msgstr
        else msg.msgid)
                .substringBeforeLast('|')
                .replace("......", "\u2026\u2026")
    }
}

fun patchScript(script: LCSEParsedScript, path: Path) {
    val catalog = PoParser().parseCatalog(path.toFile())
    val speakerMsgs = catalog.filter { it.msgctxt == "speaker" }
    val dialogMsgs = catalog.filter { it.msgctxt == "dialog" && it.msgstr.isNotEmpty() }
    val choiceMsgs = catalog.filter { it.msgctxt == "choice" && it.msgstr.isNotEmpty() }
    script.speakers.replaceAll {  original ->
        if (original.name == "") original
        else LCSESpeaker(original.ordinal, speakerMsgs.find { it.msgid == original.name }?.msgstr ?: throw Exception("bad file $path"))
    }
    script.strings.replaceAll { original ->
        when (original) {
            is LCSEDialogString -> {
                LCSEDialogString(original.ordinal, original.speakerOrdinal,
                        dialogMsgs
                                .find { it.msgid.substringBefore('|') == original.content
                                        ||  original.ordinal == it.comments.first().substringBefore('|').toInt() }
                                ?.let(::convertDialogOrChoiceString) ?: original.content, original.withTrailer)
            }
            is LCSEChoiceString -> {
                LCSEChoiceString(original.ordinal,
                        choiceMsgs.find {
                            original.ordinal == it.comments.first().substringBefore('|').toInt()
                        }?.let(::convertDialogOrChoiceString) ?: original.content)
            }
            else -> original
        }
    }
}