package com.marconcini.kdlparser.objects

import com.marconcini.kdlparser.print.PrintConfig
import com.marconcini.kdlparser.print.PrintUtil
import java.io.IOException
import java.io.StringWriter
import java.io.Writer
import java.math.BigDecimal
import java.math.BigInteger


abstract class KDLValue<T>(private val type: String?) : KDLObject {
    abstract fun getValue(): T
    abstract fun getAsString(): KDLString
    abstract fun getAsNumber(): KDLNumber?
    abstract fun getAsNumberOrElse(defaultValue: Number): Number
    abstract fun getAsBoolean(): KDLBoolean?
    abstract fun getAsBooleanOrElse(defaultValue: Boolean): Boolean
    abstract fun writeKDLValue(writer: Writer, printConfig: PrintConfig)
    abstract fun toKDLValue(): String
    fun getType() = type
    open fun isString(): Boolean = false
    open fun isNumber(): Boolean = false
    open fun isBoolean(): Boolean = false
    open fun isNull(): Boolean = false

    override fun writeKDL(writer: Writer, printConfig: PrintConfig) {
        type?.let {
            writer.write("(")
            PrintUtil.writeStringQuotedAppropriately(writer, it, true, printConfig)
            writer.write(")")
        }
        writeKDLValue(writer, printConfig)
    }

    override fun toKDL(): String {
        val writer = StringWriter()
        type?.let {
            writer.write("(")
            try {
                PrintUtil.writeStringQuotedAppropriately(
                    writer,
                    it,
                    true,
                    PrintConfig.PRETTY_DEFAULT
                )
            } catch (e: IOException) {
                throw RuntimeException(
                    String.format(
                        "Failed to convert KDL value to KDL: '%s'",
                        this
                    ), e
                )
            }
            writer.write(")")
        }

        writer.write(toKDLValue())

        return writer.toString()
    }

    companion object {
        @JvmStatic
        fun from(o: Any?): KDLValue<*> = from(o, null)

        @JvmStatic
        fun from(o: Any?, type: String?): KDLValue<*> {
            if (o == null) return KDLNull(type)

            if (o is Boolean) {
                return KDLBoolean(o, type)
            }

            if (o is BigInteger) {
                return KDLNumber(BigDecimal(o as BigInteger?), 10, type)
            }

            if (o is BigDecimal) {
                return KDLNumber(o, 10, type)
            }

            if (o is Number) {
                return KDLNumber(BigDecimal(o.toString()), 10, type)
            }

            if (o is String) {
                return KDLString(o, type)
            }

            if (o is KDLValue<*>) return o

            throw RuntimeException(String.format("No KDLValue for object '%s'", o))
        }
    }
}