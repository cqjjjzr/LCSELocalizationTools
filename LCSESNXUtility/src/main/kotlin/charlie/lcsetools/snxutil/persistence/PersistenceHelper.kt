package charlie.lcsetools.snxutil.persistence

import charlie.lcsetools.snxutil.parsed.LCSEParsedScript
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

fun loadParsedScript(path: Path) = ObjectInputStream(Files.newInputStream(path, StandardOpenOption.READ)).readObject()
        as LCSEParsedScript

fun saveParsedScript(script: LCSEParsedScript, path: Path)
        = ObjectOutputStream(Files.newOutputStream(path.apply { if (!Files.exists(this)) Files.createFile(this) },
        StandardOpenOption.TRUNCATE_EXISTING)).writeObject(script)