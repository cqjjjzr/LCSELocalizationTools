package charlie.lcsetools.snxutil.raw

import java.util.*
import kotlin.collections.ArrayList

class LCSERawSNXScript {
    val instructions = ArrayList<LCSERawSNXInstruction>()
    val strings = ArrayList<LCSERawSNXString>()

    val stringTableLength get() = strings.map { it.content.size + 4 }.sum()

    override fun toString(): String {
        return "LCSERawSNXScript(instructions=$instructions, strings=$strings)"
    }
}

data class LCSERawSNXInstruction(val instruction: Int,
                                 val param1: Int,
                                 val param2: Int) {
    companion object {
        const val INSTRUCTION_LENGTH = 4 * 3
    }

    override fun toString(): String {
        return "LCSERawSNXInstruction(0x${Integer.toHexString(instruction)}, 0x${Integer.toHexString(param1)}, 0x${Integer.toHexString(param2)})"
    }
}

data class LCSERawSNXString(val ordinal: Int,
                            val offset: Int,
                            val content: ByteArray) {
    override fun equals(other: Any?): Boolean {
        return if (other !is LCSERawSNXString) false
        else offset == other.offset && Arrays.equals(content, other.content)
    }

    override fun hashCode(): Int{
        var result = offset
        result = 31 * result + Arrays.hashCode(content)
        return result
    }
}