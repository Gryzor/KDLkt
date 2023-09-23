package com.marconcini.kdlparser.objects

import com.marconcini.kdlparser.print.PrintConfig
import com.marconcini.kdlparser.print.PrintUtil
import java.io.Writer
@Suppress("unused")
data class KDLString @JvmOverloads constructor(
    private val value: String,
    private val type: String? = null,
) : KDLValue<String?>(type) {

    override fun getValue(): String = value

    override fun isString(): Boolean = true

    override fun getAsString(): KDLString = this

    override fun getAsNumber(): KDLNumber? = KDLNumber.from(value, type)

    override fun getAsNumberOrElse(defaultValue: Number): Number = defaultValue

    override fun getAsBoolean(): KDLBoolean? = KDLBoolean.fromString(value, type)

    override fun getAsBooleanOrElse(defaultValue: Boolean): Boolean = defaultValue

    override fun writeKDLValue(writer: Writer, printConfig: PrintConfig) {
        PrintUtil.writeStringQuotedAppropriately(writer, value, false, printConfig)
    }

    override fun toKDLValue(): String = value

    override fun toString(): String = """KDLString(value='$value', type=$type)"""

    companion object {
        @JvmStatic
        fun from(value: String): KDLString = from(value, null)
        @JvmStatic
        fun from(value: String, type: String?): KDLString = KDLString(value, type)
        @JvmStatic
        fun empty(): KDLString = empty(null)
        @JvmStatic
        fun empty(type: String?): KDLString = KDLString("", type)
    }
}
