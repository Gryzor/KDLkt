package com.marconcini.kdlparser.objects

import com.marconcini.kdlparser.print.PrintConfig
import java.io.Writer

data class KDLBoolean @JvmOverloads constructor(
    private val value: Boolean,
    private val type: String? = null,
) : KDLValue<Boolean?>(type) {

    override fun getValue(): Boolean = value

    override fun getAsString(): KDLString =
        if (value) KDLString("true", type) else KDLString("false", type)

    override fun getAsNumber(): KDLNumber? = null

    override fun getAsNumberOrElse(defaultValue: Number): Number = defaultValue

    override fun getAsBoolean(): KDLBoolean = this

    override fun getAsBooleanOrElse(defaultValue: Boolean): Boolean = value

    override fun isBoolean(): Boolean = true


    override fun writeKDLValue(writer: Writer, printConfig: PrintConfig) {
        writer.write(toKDLValue())
    }

    override fun toKDLValue(): String = if (value) "true" else "false"

    override fun toString(): String = """KDLBoolean{value=$value, type=$type}"""

    companion object {
        fun fromString(value: String, type: String?): KDLBoolean? = when (value) {
            "true" -> KDLBoolean(true, type)
            "false" -> KDLBoolean(false, type)
            else -> null
        }
    }
}