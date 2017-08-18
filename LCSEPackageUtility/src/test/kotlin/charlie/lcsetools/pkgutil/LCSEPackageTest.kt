package charlie.lcsetools.pkgutil

import org.junit.Ignore
import org.junit.Test
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

class LCSEIndexTest {
    @Test
    fun dummy() {  }

    @Ignore
    fun test() {
        val channel = FileChannel.open(Paths.get("D:", "无限錬姦", "mugen", "lcsebody1.lst"))
        val buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())

        Paths.get("D:", "无限錬姦", "mugen", "lcsebody1_exported.lst").apply {
            if (!Files.exists(this)) Files.createFile(this)
            Files.write(this, LCSEIndexList.readFromBuffer(buf).getByteArray(), StandardOpenOption.TRUNCATE_EXISTING)
        }
    }

    @Ignore
    fun test1() {
        //val channel = FileChannel.open(Paths.get("D:", "无限錬姦", "mugen", "lcsebody1.lst"))
        val channel = FileChannel.open(Paths.get("D:", "无限錬姦", "DISK1", "WinRoot", "Liquid", "mugen", "lcsebody1.lst"))
        val archiveChannel = FileChannel.open(Paths.get("D:", "无限錬姦", "DISK1", "WinRoot", "Liquid", "mugen", "lcsebody1"))
        val buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())

        LCSEIndexList.readFromBuffer(buf).entries
                .filter { it.filename == "_TITLE" && it.type == LCSEResourceType.SCRIPT }
                .forEach {
            it.extractFromChannelToFile(archiveChannel, Paths.get("D:", "无限錬姦", "mugen", "n"))
        }
    }

    @Ignore
    fun test2() {
        val channel = FileChannel.open(Paths.get("D:", "无限錬姦", "DISK1", "WinRoot", "Liquid", "mugen", "lcsebody1.lst"))
        val archiveChannel = FileChannel.open(Paths.get("D:", "无限錬姦", "DISK1", "WinRoot", "Liquid", "mugen", "lcsebody1"))
        val buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())

        val outDirectory = Paths.get("D:", "无限錬姦", "mugen", "m")
        LCSEIndexList.readFromBuffer(buf).writePatchedPackage(
                Paths.get("D:", "无限錬姦", "mugen", "patches").toFile()
                        .walk().filter { it.isFile }.map { LCSEPatch(it.toPath()) }.asIterable(), archiveChannel,
                outDirectory.resolve("lcsebody1.lst"),
                outDirectory.resolve("lcsebody1"))
        channel.close()
        archiveChannel.close()
    }
}