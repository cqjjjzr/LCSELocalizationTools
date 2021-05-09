package charlie.lcsetools.snxutil

import charlie.lcsetools.snxutil.gettext.patchScript
import charlie.lcsetools.snxutil.parsed.generateRawSNXScript
import charlie.lcsetools.snxutil.parsed.parseRawSNXScript
import charlie.lcsetools.snxutil.raw.readSNXScript
import charlie.lcsetools.snxutil.raw.writeSNXScript
import java.io.File
import java.io.StringReader
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

fun main(args: Array<String>) {
    File("""D:\gal\MUGENRANKAN\mugen_ere\dictInjected1""").walkTopDown().forEach { poFile ->
        if (!poFile.isFile || poFile.extension != "po") return@forEach
        println(poFile.absolutePath)

        val txt = poFile.readText().replace("\\v", "")
        val originalPath = Paths.get("D:\\gal\\MUGENRANKAN\\mugen_ere\\lcsebody1Extracted\\${poFile.nameWithoutExtension}.snx")
        val resultPath = Paths.get("D:\\gal\\MUGENRANKAN\\mugen_ere\\patchedInjectSNX2\\${poFile.nameWithoutExtension}.snx")
        if (!Files.exists(originalPath)) {
            println("error: Not found: $originalPath")
            return
        }

        FileChannel.open(originalPath, StandardOpenOption.READ).use {
            val originalBuf = it.map(FileChannel.MapMode.READ_ONLY, 0, it.size())
            val parsedSNX = parseRawSNXScript(readSNXScript(originalBuf))
            patchScript(parsedSNX, StringReader(txt), poFile.nameWithoutExtension)

            if (!Files.exists(resultPath)) Files.createFile(resultPath)
            FileChannel.open(resultPath, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE).use { channel ->
                channel.write(writeSNXScript(generateRawSNXScript(parsedSNX)))
            }
        }
    }

    /*val originalPath = Paths.get("D:\\gal\\无限錬姦\\mugen\\originalSNX\\CHOICE_01.snx")
    val resultPath = Paths.get("D:\\gal\\无限錬姦\\mugen\\patchedSNX\\CHOICE_01.snx")
    if (!Files.exists(originalPath)) {
        println("error: Not found: $originalPath")
        return
    }

    FileChannel.open(originalPath, StandardOpenOption.READ).use {
        val originalBuf = it.map(FileChannel.MapMode.READ_ONLY, 0, it.size())
        val raw = readSNXScript(originalBuf)
        val parsedSNX = parseRawSNXScript(raw)
        //patchScript(parsedSNX, poFile.toPath())

        if (!Files.exists(resultPath)) Files.createFile(resultPath)
        FileChannel.open(resultPath, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE).use { channel ->
            channel.write(writeSNXScript(generateRawSNXScript(parsedSNX)))
        }
    }*/
}