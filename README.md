# LCSELocalizationTools

Tools to translate Virtual Novels(Galgame)/TAVG written by LC-ScriptEngine.  
一些翻译 LC-ScriptEngine 编写的 Galgame 的工具。

测试用游戏：
Test game: 無限煉姦～淫辱にまみれし不死姫の輪舞～

(以下文档分中英文版)
(Document below has both Chinese and English ver.)

## Chinese ver.

### LCSE Package Utility (LC-ScriptEngine资源包封包处理实用工具)

处理 LC-ScriptEngine 游戏封包（一个大文件带着一个.lst的清单文件）的工具。所有程序说明都是中文，下面是详细使用：

本工具有两个模式，解包和封包。解包会将包中的文件提取为独立文件，封包会利用将原版包中的一些文件利用一系列新文件进行替换来产生一个新的封包（并不会修改原版封包）。

#### 通用参数

两个模式都必须指定 `-l(--list)` 参数提供 .lst 清单文件，`-a(--package)` 提供封包文件。你还可以指定一个可选的 `-d(--out-dir)` 参数来设置输出目录。

通过 `-k (--key)` 可以指定 `.lst`  文件的加密 key，而 `-K (--key-snx)` 可以指定 `.snx` 文件的 key。这两个参数的格式均为两位 16 进制数。

请总是使用 `-h` 指令查看最全面的参数列表！

你需要选择你想要处理的资源文件类型。下面是用来设置是否处理这些类型的开关：

```
-s --process-snx 处理SNX脚本
-p --process-png 处理PNG图片
-b --process-bmp 处理BMP图片
-w --process-wav 处理WAV音频
-o --process-ogg 处理OGG音频
```

#### 解包模式

使用解包模式你必须指定 `-u(--unpack)` 开关，且不能指定 `-e(--patch-dir)` 参数。

例子：

```cmd
java -jar .\LCSEPackageUtility-rv1.jar --unpack --list lcsebody1.lst --package lcsebody1 -s -d "D:\mugen\extracted" --key 02
```

意味着解包 lcsebody1 封包文件，利用 lcsebody1.lst 清单文件，只解包SNX格式脚本，输出到 D:\mugen\extracted 目录。

#### 封包模式

使用封包模式你需要指定 `-r(--patch)` 开关，且必须指定 `-e(--patch-dir)` 来提供包含你希望用来替换掉原封包中文件的文件的文件夹。

例子：

```cmd
java -jar .\LCSEPackageUtility-rv1.jar --patch --patch-dir "D:\mugen\patches" --list lcsebody1.lst --package lcsebody1 -s -d "D:\mugen\patched" --key 02
```

意味着利用原封包文件 lcsebody1 和清单 lcsebody1.lst，用 D:\mugen\patches 替换掉原封包中的同名文件，生成一个新的封包和对应清单到 D:\mugen\patched，且仅替换SNX格式脚本。

## English ver.

### LCSE Package Utility

A tool to process the package of LC-ScriptEngine games (a big file with a .lst file). All string written in Chiense, here's the English usage:

First, we have 2 modes, Unpack and Patch. Unpack is to extract files from the package to dedicated files. Patch is to replace files in the package, and then create a new archive (modify the original version of pkg is not recommended).

#### Common args

For both modes you need to specify the -l(--list) value to provide .lst file, the -a(--package) to provide package file. You can also specify a optional -d(--out-dir) value to set the out directory.

Use `-k (--key)` and `-K (--key-snx)` to specify encryption key for `.lst` and `.snx` files, respectively. The format is 2-digits hex number e.g. `-k 0F`.

Please always use `-h` to view the full arg list!

You need to choose the resource file type that you needed to process. Below is switches to decide that if process them or not.

```
-s --process-snx Process SNX Script.
-p --process-png Process PNG Picture.
-b --process-bmp Process BMP Pirture.
-w --process-wav Process WAV Audio.
-o --process-ogg Process OGG Audio.
```

#### Using Unpack Mode

To use the Unpack mode you need to specify -u(--unpack) switch, no -e(--patch-dir) allowed. 

Example:

```cmd
java -jar .\LCSEPackageUtility-rv1.jar --unpack --list lcsebody1.lst --package lcsebody1 -s -d "D:\mugen\extracted"
```

Means unpack the lcsebody1 package with lcsebody1.lst list file, extract SNX scripts only, to `D:\mugen\extracted` folder.

#### Using Patch Mode

To use the Patch mode you need to specify -r(--patch) switch, -e(--patch-dir) required to set a directory contains files you want to replace the files in the package.

Example:

```cmd
java -jar .\LCSEPackageUtility-rv1.jar --patch --patch-dir "D:\mugen\patches" --list lcsebody1.lst --package lcsebody1 -s -d "D:\mugen\patched"
```

Means from the lcsebody1 package with lcsebody1.lst file, create a new package to `D:\mugen\patched` folder that some files in the original package is replaced with files in `D:\mugen\patches` folder.