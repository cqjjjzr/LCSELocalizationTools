package charlie.lcsetools.snxutil.gettext

import charlie.lcsetools.snxutil.parsed.generateRawSNXScript
import charlie.lcsetools.snxutil.parsed.parseRawSNXScript
import charlie.lcsetools.snxutil.raw.readSNXScript
import charlie.lcsetools.snxutil.raw.writeSNXScript
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

class GetTextWriterTest {
    @Ignore
    fun writePoTemplateTest() {
        /*FileChannel.open(Paths.get("""D:\无限錬姦\mugen\originalSNX\AGO01_01.snx""")).use {
            val originalBuf = it.map(FileChannel.MapMode.READ_ONLY, 0, it.size())
            val parsedSNX = parseRawSNXScript(readSNXScript(originalBuf))

            writePoTemplate(parsedSNX, Paths.get("""D:\无限錬姦\mugen\AGO01_01.po"""))
        }*/

        File("""D:\无限錬姦\mugen\originalSNX\""").walkTopDown().filter { !it.isDirectory }
                .forEach { file ->
                    FileChannel.open(file.toPath()).use {
                        val originalBuf = it.map(FileChannel.MapMode.READ_ONLY, 0, it.size())
                        val parsedSNX = parseRawSNXScript(readSNXScript(originalBuf))

                        writePoTemplate(parsedSNX, Paths.get("""D:\无限錬姦\mugen\templates\""" + file.nameWithoutExtension + ".pot"))
                    }
                }
    }

    @Test
    fun patchScriptTest() {
        FileChannel.open(Paths.get("""D:\无限錬姦\mugen\originalSNX\AGO05_01_01.snx""")).use {
            val originalBuf = it.map(FileChannel.MapMode.READ_ONLY, 0, it.size())
            val parsedSNX = parseRawSNXScript(readSNXScript(originalBuf))
            patchScript(parsedSNX, Paths.get("""D:\无限錬姦\mugen\第五章01 01\第五章01 01\AGO05_01_01.po"""))
            Paths.get("""D:\无限錬姦\mugen\AGO05_01_01.snx""").apply {
                if (!Files.exists(this)) Files.createFile(this)
                FileChannel.open(this, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE).use {
                    it.write(writeSNXScript(generateRawSNXScript(parsedSNX)))
                }
            }
        }
    }
}