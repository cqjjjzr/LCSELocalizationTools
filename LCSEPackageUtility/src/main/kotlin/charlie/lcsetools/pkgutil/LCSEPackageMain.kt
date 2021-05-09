package charlie.lcsetools.pkgutil

import org.apache.commons.cli.*
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.Paths
import kotlin.system.exitProcess

val VERSION = "rv1"

val opts = Options().apply {
    addOption("h", "help", false, "显示帮助")
    addOptionGroup(OptionGroup().apply {
        addOption(Option("u", "unpack", false, "解包。"))
        addOption(Option("r", "patch", false, "封包（需与-e配合使用）。"))
        isRequired = true
    })
    addOption(Option("s", "process-snx", false, "是否处理SNX格式数据"))
    addOption(Option("p", "process-png", false, "是否处理PNG格式数据"))
    addOption(Option("b", "process-bmp", false, "是否处理BMP格式数据"))
    addOption(Option("w", "process-wav", false, "是否处理WAV格式数据"))
    addOption(Option("o", "process-ogg", false, "是否处理OGG格式数据"))

    addOption(Option("l", "list", true, "指定.lst清单文件").apply { isRequired = true; argName = "lst-file" })
    addOption(Option("a", "package", true, "指定lcsebody数据包文件").apply { isRequired = true; argName = "package-file" })
    addOption(Option("d", "out-dir", true, "指定输出目录").apply { argName = "out-dir" })
    addOption(Option("e", "patch-dir", true, "指定用以替换数据包中文件的文件所在目录")
            .apply { argName = "patch-dir" })
    addOption(Option("k", "key", true, "指定.lst清单文件的的加密key，格式为16进制两位数字"))
    addOption(Option("K", "key-snx", true, "指定SNX文件的的加密key，格式为16进制两位数字"))
}

fun main(vararg args: String) {
    println("LC-ScriptEngine资源包封包处理实用工具 $VERSION\n\tBy Charlie Jiang\n\n")
    try {
        DefaultParser().parse(opts, args).apply {
            if (options.isEmpty() || hasOption('h')
                    || (hasOption('r') && !hasOption('e')) || (hasOption('u') && hasOption('e'))) {
                printUsageAndBoom()
            }
            if (hasOption('k'))
                keyIndex = getOptionValue('k').toInt(16).expandByteToInt()
            if (hasOption('K'))
                keySNX = getOptionValue('k').toInt(16).expandByteToInt()

            FileChannel.open(Paths.get(getOptionValue('l').removeSurrounding("\""))).use {
                it.map(FileChannel.MapMode.READ_ONLY, 0, it.size()).let { listBuffer ->
                    FileChannel.open(Paths.get(getOptionValue('a').removeSurrounding("\""))).use { packageChannel ->
                        val outDirectory =
                                if (hasOption('d')) Paths.get(getOptionValue('d').removeSurrounding("\"").removeSuffix("\""))
                                else if (hasOption('u')) Paths.get(".", "extracted") else Paths.get(".", "patched")
                        if (hasOption('u')) {
                            LCSEIndexList.readFromBuffer(listBuffer)
                                    .entries
                                    .filter { it.type != LCSEResourceType.SCRIPT || hasOption('s') }
                                    .filter { it.type != LCSEResourceType.PNG_PICTURE || hasOption('p') }
                                    .filter { it.type != LCSEResourceType.BMP_PICTURE || hasOption('b') }
                                    .filter { it.type != LCSEResourceType.WAVE_AUDIO || hasOption('w') }
                                    .filter { it.type != LCSEResourceType.OGG_AUDIO || hasOption('o') }
                                    .forEach {
                                        it.extractFromChannelToFile(packageChannel, outDirectory)
                                        print("提取：${it.fullFilename}              \r")
                                    }
                        } else if (hasOption('r')) {
                            val patches = File(getOptionValue('e').removeSurrounding("\"")).walkTopDown()
                                    .filter { it.isFile }
                                    .map { try { LCSEPatch(it.toPath()) } catch(e: Exception) { null } }
                                    .filterNotNull()
                                    .filter { it.type != LCSEResourceType.SCRIPT || hasOption('s') }
                                    .filter { it.type != LCSEResourceType.PNG_PICTURE || hasOption('p') }
                                    .filter { it.type != LCSEResourceType.BMP_PICTURE || hasOption('b') }
                                    .filter { it.type != LCSEResourceType.WAVE_AUDIO || hasOption('w') }
                                    .filter { it.type != LCSEResourceType.OGG_AUDIO || hasOption('o') }
                                    .asIterable()
                            LCSEIndexList
                                    .readFromBuffer(listBuffer)
                                    .writePatchedPackage(
                                            patches,
                                            packageChannel,
                                            outDirectory.resolve(Paths.get(getOptionValue('l').removeSurrounding("\"")).fileName),
                                            outDirectory.resolve(Paths.get(getOptionValue('a').removeSurrounding("\"")).fileName))

                        }

                    }
                }
            }
        }
    } catch(e: ParseException) {
        e.printStackTrace()
        printUsageAndBoom()
    }

    println("\n完成。")
}

fun printUsageAndBoom(): Nothing {
    HelpFormatter().printHelp("java -jar LCSEPackage.jar <-u/r/h> <-l xxx> <-a xxx> [其他开关/参数]", opts)
    exitProcess(0)
}