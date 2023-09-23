package com.marconcini.kdlparser.objects

import com.marconcini.kdlparser.print.PrintConfig
import java.io.Writer
import java.math.BigDecimal
import java.math.BigInteger


/**
 * TODO: Fix this javadoc
 * Representation of a KDL number. Numbers may be base 16, 10, 8, or 2 as stored in the radix field. Base 10 numbers may
 * be fractional, but all others are limited to integers. {@link dev.hbeck.kdl.parse.KDLParser} reads in all numbers as
 * either {@link BigDecimal} (for decimal) or {@link BigInteger} (for non-decimal), so if you <i>absolutely</i> need
 * that precision then don't hesitate to use instanceof and cast.
 */
data class KDLNumber @JvmOverloads constructor(
    private val value: Number,
    private val radix: Int,
    private val type: String? = null,
) : KDLValue<Number?>(type) {

    override fun getValue(): Number = value

    override fun getAsString(): KDLString = KDLString.from(value.toString(), type)

    override fun getAsNumber(): KDLNumber = this

    override fun getAsNumberOrElse(defaultValue: Number): Number = value

    override fun getAsBoolean(): KDLBoolean? = null

    override fun getAsBooleanOrElse(defaultValue: Boolean): Boolean = defaultValue

    override fun writeKDLValue(writer: Writer, printConfig: PrintConfig) {
        if (printConfig.shouldRespectRadix()) {
            /*
             Print out a number while respecting radix!
             If it's binary, octal, or hex, this converts it to a `BigInteger` to format.
             This is kludge-y, but someone *might* for some reason want to have a more-than-64-bit number in KDL.
             I have no idea *why* you'd want that, but who am I to judge?
             Plus one of our test cases uses an 80-bit hex number so let's just roll with it lol
             ~LemmaEOF
              */
            when (radix) {
                10 -> {
                    writer.write(value.toString().replace('E', printConfig.exponentChar))
                }

                2 -> {
                    writer.write("0b")
                    writer.write(BigInteger(value.toString()).toString(radix))
                }

                8 -> {
                    writer.write("0o")
                    writer.write(BigInteger(value.toString()).toString(radix))
                }

                16 -> {
                    writer.write("0x")
                    writer.write(BigInteger(value.toString()).toString(radix))
                }
            }
        } else {
            writer.write(value.toString().replace('E', printConfig.exponentChar))
        }
    }

    override fun toKDLValue(): String = value.toString()

    override fun isNumber(): Boolean = true

    override fun toString(): String = """KDLNumber{value=$value, radix=$radix, type=$type}"""

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KDLNumber) return false

        //TODO: Is this still TRUE for Kotlin?

        // SOURCE: https://github.com/kdl-org/kdl4j/blob/da984e3a2a677ab8c3b662694c740def1ee08ee8/src/main/java/dev/hbeck/kdl/objects/KDLNumber.java#L220C1-L225C12
        // Oh, right. You technically can't compare two `Number`s. Thaaaanks, Oracle.
        // Ah, well. Just use `toString` and check if those are equal.
        // Numbers in Java all stringify in the same way, so this should work fine.
        // ~LemmaEOF
        val (value1, radix1, type1) = other
        return radix == radix1 && value.toString() == value1.toString() && type == type1
    }

    override fun hashCode(): Int = super.hashCode()

    companion object {
        /**
         * Get the Zero value for a given radix, which must be one of [2, 8, 10, 16]
         *
         * @return a new number with the value 0 and the given radix
         */
        @Suppress("unused")
        @JvmStatic
        fun zero(radix: Int): KDLNumber {
            return zero(radix, null)
        }

        @JvmStatic
        fun zero(radix: Int, type: String?): KDLNumber {
            return when (radix) {
                2, 8, 10, 16 -> KDLNumber(BigDecimal.ZERO, radix, type)
                else -> throw RuntimeException("Radix must be one of: [2, 8, 10, 16]")
            }
        }

        @Suppress("unused")
        @JvmStatic
        fun from(number: Number, radix: Int): KDLNumber = from(number, radix, null)

        @JvmStatic
        fun from(number: Number, radix: Int, type: String?): KDLNumber =
            KDLNumber(number, radix, type)

        @JvmStatic
        fun from(number: Number, type: String?): KDLNumber = from(number, 10, type)

        @Suppress("unused")
        @JvmStatic
        fun from(number: String, radix: Int): KDLNumber? = from(number, radix, null)

        @JvmStatic
        fun from(number: String, radix: Int, type: String?): KDLNumber? {
            val kdlNumber = from(number, type)
            return if (kdlNumber?.radix == radix) kdlNumber else null
        }

        /**
         * Parse the provided string into a KDLNumber if possible.
         *
         * @return an optional wrapping the new KDLNumber if the parse was successful, or null if not
         */
        @Suppress("unused")
        @JvmStatic
        fun from(number: String?): KDLNumber? {
            return from(number, null)
        }

        fun from(number: String?, type: String?): KDLNumber? {
            if (number.isNullOrBlank()) {
                return null
            }

            val radix: Int
            val toParse: String
            if (number[0] == '0') {
                if (number.length == 1) {
                    return zero(10, type)
                }
                when (number[1]) {
                    'x' -> {
                        radix = 16
                        toParse = number.substring(2)
                    }

                    'o' -> {
                        radix = 8
                        toParse = number.substring(2)
                    }

                    'b' -> {
                        radix = 2
                        toParse = number.substring(2)
                    }

                    else -> {
                        radix = 10
                        toParse = number
                    }
                }
            } else {
                radix = 10
                toParse = number
            }
            val parsed: BigDecimal = try {
                if (radix == 10) {
                    BigDecimal(toParse)
                } else {
                    BigDecimal(BigInteger(toParse, radix))
                }
            } catch (e: NumberFormatException) {
                null
            } ?: return null

            return from(parsed, radix, type)
        }
    }
}