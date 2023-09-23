package com.marconcini.kdlparser.objects

import com.marconcini.kdlparser.print.PrintConfig
import com.marconcini.kdlparser.print.PrintUtil
import java.io.Writer

data class KDLProperty constructor(
    private val key: String,
    private val value: KDLValue<*>,
) : KDLObject {

    fun getValue() = value
    fun getKey() = key

    override fun writeKDL(writer: Writer, printConfig: PrintConfig) {
        if (value is KDLNull && !printConfig.shouldPrintNullProps()) return

        PrintUtil.writeStringQuotedAppropriately(writer, key, true, printConfig)
        writer.write("=")
        value.writeKDL(writer, printConfig)
    }

    override fun toString(): String = """KDLProperty{key=$key, value=$value}"""
}