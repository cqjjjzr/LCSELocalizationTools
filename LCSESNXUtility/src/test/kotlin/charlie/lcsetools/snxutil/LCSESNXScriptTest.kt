package charlie.lcsetools.snxutil

import org.junit.Test
import java.nio.channels.FileChannel
import java.nio.file.Paths

class LCSESNXScriptTest {
    @Test
    fun test() {
        FileChannel.open(Paths.get("D:\\无限錬姦\\mugen\\originalSNX\\AGO01_01.snx")).use {
            LCSESNXScript.readFromBuffer(it.map(FileChannel.MapMode.READ_ONLY, 0, it.size()))
                    .writePoTemplate(Paths.get("D:\\无限錬姦\\mugen\\AGO01_01.pot"))
        }
    }
}