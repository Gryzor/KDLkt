package com.marconcini.kdlparser.objects

import com.marconcini.kdlparser.print.PrintConfig
import java.io.IOException
import java.io.Writer

data class KDLNull @JvmOverloads constructor(
    private val type: String? = null,
) : KDLValue<Any?>(type) {

    override fun getValue(): Any? = null

    override fun getAsString(): KDLString = KDLString("null", type)

    override fun getAsNumber(): KDLNumber? = null

    override fun getAsNumberOrElse(defaultValue: Number): Number = defaultValue

    override fun getAsBoolean(): KDLBoolean? = null

    override fun getAsBooleanOrElse(defaultValue: Boolean): Boolean = defaultValue

    @Throws(IOException::class)
    override fun writeKDLValue(writer: Writer, printConfig: PrintConfig) {
        writer.write(toKDLValue())
    }

    override fun toKDLValue(): String = "null"

    override fun isNull(): Boolean = true

    override fun toString(): String = "KDLNull"
}