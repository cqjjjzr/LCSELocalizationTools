package charlie.lcsetools.pkgutil

import org.apache.commons.io.FilenameUtils
import java.nio.file.Path

data class LCSEPatch(val newFilePath: Path) {
    val filename: String = FilenameUtils.getBaseName(newFilePath.toString())
    val type: LCSEResourceType = LCSEResourceType.forExtensionName(FilenameUtils.getExtension(newFilePath.toString()))
}
