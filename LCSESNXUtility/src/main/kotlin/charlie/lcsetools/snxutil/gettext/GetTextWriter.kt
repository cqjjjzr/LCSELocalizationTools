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
import java.io.File
import java.io.Reader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

fun writeCatalog(catalog: Catalog, path: Path): Boolean {
    if (catalog.isEmpty) {
        println("[WARNING] Empty file: $path")
        return false
    }
    if (!Files.exists(path)) Files.createFile(path)
    val stream = ByteOutputStream()
    PoWriter().write(catalog, stream, Charsets.UTF_8)

    val bytes = stream.bytes
    val res = bytes.sliceArray(0 until (bytes.indexOf(0x00).takeIf { it != -1 } ?: bytes.size))

    Files.write(path, res, StandardOpenOption.TRUNCATE_EXISTING)
    return true
}

fun writePoTemplate(script: LCSEParsedScript, path: Path): Boolean {
    if (script.strings.isEmpty()) {
        println("[WARNING] Empty file: $path")
        return false
    }

    return writeCatalog(Catalog().apply {
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
    }, path)
}

fun convertDialogOrChoiceString(msg: Message, context: String, ordinal: Int): String {
    return buildString {
        if (msg.comments.first().endsWith("true")) append("\u3000")
        //append('[')
        //append(context)
        //append('$')
        //append(ordinal)
        //append("] ")
        (if (msg.msgstr.isNotEmpty())
            msg.msgstr
        else msg.msgid)
                .substringBeforeLast('|')
                .replace("......", "\u2026\u2026")
                .let(::append)
    }
}

fun patchScript(script: LCSEParsedScript, reader: Reader, context: String) {
    val catalog = PoParser().parseCatalog(reader, false)
    val speakerMsgs = catalog.filter { it.msgctxt == "speaker" }
    val dialogMsgs = catalog.filter { it.msgctxt == "dialog" && it.msgstr.isNotEmpty() }
    val choiceMsgs = catalog.filter { it.msgctxt == "choice" && it.msgstr.isNotEmpty() }
    script.speakers.replaceAll {  original ->
        if (original.name == "") original
        else LCSESpeaker(original.ordinal, speakerMsgs.find { it.msgid == original.name }?.msgstr ?: throw Exception("bad file $context"))
    }
    script.strings.replaceAll { original ->
        when (original) {
            is LCSEDialogString -> {
                LCSEDialogString(original.ordinal, original.speakerOrdinal,
                        dialogMsgs
                                .find { it.msgid.substringBefore('|') == original.content
                                        ||  original.ordinal == it.comments.first().substringBefore('|').toInt() }
                                ?.let {
                                    convertDialogOrChoiceString(it, context, original.ordinal)
                                } ?: original.content, original.withTrailer)
            }
            is LCSEChoiceString -> {
                LCSEChoiceString(original.ordinal,
                        choiceMsgs.find {
                            original.ordinal == it.comments.first().substringBefore('|').toInt()
                        }?.let {
                            convertDialogOrChoiceString(it, context, original.ordinal)
                        } ?: original.content)
            }
            else -> original
        }
    }
}

fun buildDictionary(dir: File): Map<String, String> {
    val result = mutableMapOf<String, String>()
    val reader = PoParser()
    dir.walkTopDown().filter { it.isFile && it.extension == "po" }.forEach { file ->
        val catalog = reader.parseCatalog(file)
        catalog.forEach {
            if (it.msgid.contains("Content-Transfer-Encoding"))
                it.msgid = ""
            if (it.msgstr.contains("Content-Transfer-Encoding"))
                it.msgstr = ""
            val key = it.msgid.substringBeforeLast('|').trim()
            if (result.containsKey(key) && result[key] != it.msgstr) {
                println("[WARNING] Inconsistent translation for ${key}: ${it.msgstr} <-> ${result[key]} in ${file.name}")
            }
            result[key] = it.msgstr
        }
    }

    return result
}

fun injectDictionary(template: Path, dest: Path, dict: Map<String, String>) {
    try {
        val catalog = PoParser().parseCatalog(template.toFile())
        catalog.forEach {
            val key = it.msgid.substringBeforeLast('|').trim()
            val t = dict[key] ?: return@forEach
            it.msgstr = t
        }
        writeCatalog(catalog, dest)
    } catch (ex: Exception) {
        println("[WARNING] Failed to inject $template")
        ex.printStackTrace()
    }
}