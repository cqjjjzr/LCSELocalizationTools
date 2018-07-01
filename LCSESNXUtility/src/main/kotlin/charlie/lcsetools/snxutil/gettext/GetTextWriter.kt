package charlie.lcsetools.snxutil.gettext

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
        script.strings.filter { it is LCSEDialogString }
                .forEach {
                    (it as LCSEDialogString).let {
                        addMessage(Message().apply {
                            msgctxt = "dialog"
                            msgid = it.content.removePrefix("\u3000") + "|" + it.ordinal
                            msgstr = ""
                            addComment("${it.ordinal}|${it.speakerOrdinal}|${it.content.startsWith("\u3000")}")
                        })
                    }
                }
    }, stream, Charsets.UTF_8)

    Files.write(path, stream.bytes, StandardOpenOption.TRUNCATE_EXISTING)
}

fun patchScript(script: LCSEParsedScript, path: Path) {
    val catalog = PoParser().parseCatalog(path.toFile())
    val speakerMsgs = catalog.filter { it.msgctxt == "speaker" }
    val dialogMsgs = catalog.filter {it.msgctxt == "dialog"}
    script.speakers.clear()
    speakerMsgs.forEach {
        script.speakers += LCSESpeaker(it.comments.first().toInt(), it.msgstr)
    }
    script.strings.replaceAll { original ->
        if (original is LCSEDialogString) {
            LCSEDialogString(original.ordinal, original.speakerOrdinal,
                    dialogMsgs.find { original.ordinal == it.comments.first().split('|')[0].toInt() }
                            .let { if (it == null)
                                original.content
                            else (
                                    (if (it.comments.first().endsWith("true"))
                                        "\u3000"
                                    else
                                        "") +
                                            (if (it.msgstr.isNotEmpty()) it.msgstr else it.msgid.substringBeforeLast('|'))) })
        }
        else original
    }
}