package com.marconcini.kdlparser.print

import com.marconcini.kdlparser.parse.getEscapeIncludingUnicode
import com.marconcini.kdlparser.parse.isValidBareId
import java.io.IOException
import java.io.Writer


object PrintUtil {

    @Throws(IOException::class)
    fun writeStringQuotedAppropriately(
        writer: Writer,
        string: String,
        bareAllowed: Boolean,
        printConfig: PrintConfig,
    ) {
        if (string.isEmpty()) {
            writer.write("\"\"")
            return
        }

        if (bareAllowed && string.isValidBareId()) {
            writer.write(string)
            return
        }

        writer.write("\"")
        for (char in string) {
            val charCode = char.code
            if (printConfig.requiresEscape(charCode)) {
                writer.write(charCode.getEscapeIncludingUnicode())
            } else {
                writer.write(charCode)
            }
        }
        writer.write("\"")
    }
}