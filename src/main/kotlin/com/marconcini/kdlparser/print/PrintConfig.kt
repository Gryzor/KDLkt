package com.marconcini.kdlparser.print

import com.marconcini.kdlparser.parse.isCommonEscape
import com.marconcini.kdlparser.parse.isNonAscii
import com.marconcini.kdlparser.parse.isPrintableAscii
import com.marconcini.kdlparser.parse.isUnicodeLinespace
import com.marconcini.kdlparser.parse.isUnicodeWhitespace
import com.marconcini.kdlparser.parse.mustEscape


/**
 * A config object controlling various aspects of how KDL documents are printed.
 */
data class PrintConfig(
    val escapes: Map<Int, Boolean> = emptyMap(),
    val escapeNonPrintableAscii: Boolean = true,
    val escapeLineSpace: Boolean = true,
    val escapeNonAscii: Boolean = false,
    val escapeCommon: Boolean = true,
    val requireSemicolons: Boolean = false,
    val respectRadix: Boolean = true,
    val newline: String = "\n",
    val indent: Int = 4,
    val indentChar: Char = ' ',
    val exponentChar: Char = 'E',
    val printEmptyChildren: Boolean = true,
    val printNullArgs: Boolean = true,
    val printNullProps: Boolean = true,
) {

    init {
        if (exponentChar != 'e' && exponentChar != 'E') {
            throw IllegalArgumentException("Exponent character must be either 'e' or 'E'")
        }

        for (element in newline) {
            require(element.code.isUnicodeLinespace()) { "All characters in specified 'newline' must be unicode vertical space" }
        }

        if (!indentChar.code.isUnicodeWhitespace()) {
            throw IllegalArgumentException("Indent character must be unicode whitespace")
        }
    }


    fun requiresEscape(charCode: Int): Boolean {
        return if (shouldForceEscape(charCode)) {
            true
        } else if (charCode.mustEscape()) {
            true
        } else if (escapeLineSpace && charCode.isUnicodeLinespace()) {
            true
        } else if (escapeNonPrintableAscii && !charCode.isNonAscii() && !charCode.isPrintableAscii()) {
            true
        } else if (escapeNonAscii && charCode.isNonAscii()) {
            true
        } else escapeCommon && charCode.isCommonEscape()
    }


    /**
     * @return true if empty children should be printed with braces containing no nodes, false if they shouldn't be printed
     */
    fun shouldPrintEmptyChildren(): Boolean = printEmptyChildren

    /**
     * @return true if node arguments with the literal value 'null' will be printed
     */
    fun shouldPrintNullArgs(): Boolean = printNullArgs

    /**
     * @return true if node properties with the literal value 'null' will be printed
     */
    fun shouldPrintNullProps(): Boolean = printNullProps

    /**
     * @return true if each node should be terminated with a ';', false if semicolons will be omitted entirely
     */
    fun shouldRequireSemicolons(): Boolean = requireSemicolons

    /**
     * @return true if each number should be printed with its specified radix, false if they should be printed just base-10
     */
    fun shouldRespectRadix(): Boolean = respectRadix

    /**
     * Check if character has been set to force strings containing it to be escaped
     *
     * @param c the character to check
     * @return true if the character should be escaped, false otherwise.
     */
    fun shouldForceEscape(c: Int): Boolean = escapes[c] ?: false

    fun shouldEscapeNonPrintableAscii(): Boolean = escapeNonPrintableAscii

    fun shouldEscapeStandard(): Boolean = escapeCommon

//    class Builder() { //TODO
//
//        private val escapes: Map<Int, Boolean> = HashMap()
//        private val requireSemicolons = false
//        private val escapeNonAscii = false
//        private val escapeNonPrintableAscii = true
//        private val escapeCommon = true
//        private val escapeLinespace = true
//        private val respectRadix = true
//        private val newline = "\n"
//        private val indent = 4
//        private val indentChar = ' '
//        private val exponentChar = 'E'
//        private val printEmptyChildren = true
//        private val printNullArgs = true
//        private val printNullProps = true
//    }

    companion object {
        val PRETTY_DEFAULT = PrintConfig()
        val RAW_DEFAULT =
            PrintConfig(indent = 0, escapeNonAscii = false, printEmptyChildren = false)
    }
}