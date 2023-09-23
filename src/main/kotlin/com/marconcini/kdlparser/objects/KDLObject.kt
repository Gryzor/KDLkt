package com.marconcini.kdlparser.objects

import com.marconcini.kdlparser.print.PrintConfig
import java.io.BufferedWriter
import java.io.IOException
import java.io.StringWriter
import java.io.Writer


interface KDLObject {
    /**
     * Write the object to the provided stream.
     *
     * @param writer the Writer to write to
     * @param printConfig a configuration object controlling how the object is printed
     */
    fun writeKDL(writer: Writer, printConfig: PrintConfig)

    /**
     * Generate a string with the text representation of the given object.
     *
     * @return the string
     *
     * @throws RuntimeException
     */
    @Throws(RuntimeException::class)
    fun toKDL(): String {
        val writer = StringWriter()

        try {
            val bufferedWriter = BufferedWriter(writer)
            this.writeKDL(bufferedWriter, printConfig = PrintConfig.PRETTY_DEFAULT)
            bufferedWriter.flush()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        return writer.toString()
    }
}