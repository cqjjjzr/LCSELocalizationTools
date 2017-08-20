package charlie.lcsetools.snxutil.parsed

import charlie.lcsetools.snxutil.raw.LCSERawSNXInstruction
import charlie.lcsetools.snxutil.raw.LCSERawSNXScript
import charlie.lcsetools.snxutil.raw.LCSERawSNXString

private val DISPLAY_INST_CACHE = LCSERawSNXInstruction(DISPLAY_TEXT_INST, DISPLAY_TEXT_PARAM1, DISPLAY_TEXT_PARAM2)
fun generateRawSNXScript(script: LCSEParsedScript): LCSERawSNXScript {
    return LCSERawSNXScript().apply {
        var ptr = 0
        script.strings.forEachIndexed { ordinal, str ->
            val arr = str.convertToByteArray(script)
            strings += LCSERawSNXString(ordinal, ptr, arr)
            ptr += 4
            ptr += arr.size
        }

        script.instructions.forEach { inst ->
            if (inst is LCSEStringDisplayInstruction)
                instructions += DISPLAY_INST_CACHE
            else if (inst is LCSEStringReferInstruction)
                instructions += LCSERawSNXInstruction(STRING_REF_INST, STRING_REF_PARAM1,
                        strings.find { it.ordinal == inst.stringOrdinary }!!.offset)
            else if (inst is LCSEUnknownInstruction)
                instructions += LCSERawSNXInstruction(inst.instruction, inst.param1, inst.param2)
        }
    }
}