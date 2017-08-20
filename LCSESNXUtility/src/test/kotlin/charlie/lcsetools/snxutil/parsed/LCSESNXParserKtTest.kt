package charlie.lcsetools.snxutil.parsed

import charlie.lcsetools.snxutil.raw.readSNXScript
import charlie.lcsetools.snxutil.raw.writeSNXScript
import org.junit.Test
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * @author Charlie Jiang
 * *
 * @since rv1
 */
class LCSESNXParserKtTest {
    @Test
    fun parseRawSNXScriptTest() {
        FileChannel.open(Paths.get("""D:\无限錬姦\mugen\originalSNX\AGO01_01.snx""")).use {
            val originalBuf = it.map(FileChannel.MapMode.READ_ONLY, 0, it.size())
            val originalContent = ByteArray(it.size().toInt())
            originalBuf[originalContent, 0, it.size().toInt()]
            originalBuf.position(0)

            val parsedSNX = parseRawSNXScript(readSNXScript(originalBuf))
            Paths.get("""D:\无限錬姦\mugen\AGO01_01.snx""").apply {
                if (!Files.exists(this)) Files.createFile(this)
                FileChannel.open(this, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE).use {
                    it.write(writeSNXScript(generateRawSNXScript(parsedSNX)))
                }
            }
        }
    }
}