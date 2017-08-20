package charlie.lcsetools.snxutil.raw

import org.junit.Assert
import org.junit.Test
import java.nio.channels.FileChannel
import java.nio.file.Paths

class LCSESNXProcessorTest {
    @Test
    fun readSNXScriptTest() {
        FileChannel.open(Paths.get("""D:\无限錬姦\mugen\originalSNX\AGO01_01.snx""")).use {
            val originalBuf = it.map(FileChannel.MapMode.READ_ONLY, 0, it.size())
            val originalContent = ByteArray(it.size().toInt())
            originalBuf[originalContent, 0, it.size().toInt()]

            originalBuf.position(0)
            val newBuf = writeSNXScript(readSNXScript(originalBuf))
            val newContent = ByteArray(newBuf.limit())
            newBuf[newContent, 0, newBuf.limit()]

            Assert.assertArrayEquals(originalContent, newContent)


        }
    }
}