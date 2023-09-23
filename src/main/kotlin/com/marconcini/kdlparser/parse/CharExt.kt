package com.marconcini.kdlparser.parse


/**
 * Check if the character is valid at the beginning of a numeric value
 * @return true if the character is valid, false otherwise
 */
fun Int.isValidNumericStart(): Boolean = when (this) {
    '+'.code,
    '-'.code,
    '0'.code,
    '1'.code,
    '2'.code,
    '3'.code,
    '4'.code,
    '5'.code,
    '6'.code,
    '7'.code,
    '8'.code,
    '9'.code,
    -> true

    else -> false
}

/**
 * Check if a string is a valid bare identifier
 *
 * @return true if the string is a valid bare id, false otherwise
 */
fun String.isValidBareId(): Boolean {
    if (isEmpty()) {
        return false
    }

    val char = first().code
    val validBareIdStart: Boolean = char.isValidBareIdStart()
    if (length == 1 || !validBareIdStart) {
        return validBareIdStart
    }

    for (element in this) {

        if (!element.code.isValidBareIdChar()) {
            return false
        }
    }

    return true
}

/**
 * Check if the character is valid in a bare identifier after the first character
 *
 * * @return true if the character is valid, false otherwise
 */
fun Int.isValidBareIdChar(): Boolean = when {
    this <= 0x20 || this > 0x10FFFF -> {
        false
    }

    else -> {
        when (this) {
            LineSeparator,
            NextLine,
            ParagraphSeparator,
            NonBreakSpace,
            OghamSpaceMark,
            EnQuad,
            EmQuad,
            EnSpace,
            EmSpace,
            ThreePerEmSpace,
            FourPerEmSpace,
            SixPerEmSpace,
            FigureSpace,
            PunctuationSpace,
            ThinSpace,
            HairSpace,
            NarrowNonBreakSpace,
            MediumMathematicalSpace,
            IdeographicSpace,
            '\\'.code,
            '/'.code,
            '('.code,
            ')'.code,
            '{'.code,
            '}'.code,
            '<'.code,
            '>'.code,
            ';'.code,
            '['.code,
            ']'.code,
            '='.code,
            ','.code,
            '"'.code,
            -> false

            else -> true
        }
    }
}


/**
 * Check if the character is valid in a bare identifier as the first character
 *
 * @return true if the character is valid, false otherwise
 */
fun Int.isValidBareIdStart(): Boolean {
    return !this.isValidDecimalChar() && this.isValidBareIdChar()
}

/**
 * Check if the character is a valid decimal digit
 *
 * @return true if the character is valid, false otherwise
 */
fun Int.isValidDecimalChar(): Boolean {
    return '0'.code <= this && this <= '9'.code
}

/**
 * Check if the character is a valid hexadecimal digit
 *
 * @return true if the character is valid, false otherwise
 */
fun Int.isValidHexChar(): Boolean = when (this) {
    '0'.code,
    '1'.code,
    '2'.code,
    '3'.code,
    '4'.code,
    '5'.code,
    '6'.code,
    '7'.code,
    '8'.code,
    '9'.code,
    'A'.code,
    'a'.code,
    'B'.code,
    'b'.code,
    'C'.code,
    'c'.code,
    'D'.code,
    'd'.code,
    'E'.code,
    'e'.code,
    'F'.code,
    'f'.code,
    -> true

    else -> false
}

/**
 * Check if the character is a valid octal digit
 *
 * @return true if the character is valid, false otherwise
 */
fun Int.isValidOctalChar(): Boolean = '0'.code <= this && this <= '7'.code

/**
 * Check if the character is a valid binary digit
 *
 * @return true if the character is valid, false otherwise
 */
fun Int.isValidBinaryChar(): Boolean = this == '0'.code || this == '1'.code

/**
 * Check if the character is contained in one of the three literal values: true, false, and null
 *
 * @return true if the character appears in a literal, false otherwise
 */
fun Int.isLiteralChar(): Boolean = when (this) {
    't'.code,
    'r'.code,
    'u'.code,
    'e'.code,
    'n'.code,
    'l'.code,
    'f'.code,
    'a'.code,
    's'.code,
    -> true

    else -> false
}

/**
 * Check if the character is a unicode newline of any kind
 *
 * @return true if the character is a unicode newline, false otherwise
 */
fun Int.isUnicodeLinespace(): Boolean = when (this) {
    '\r'.code,
    '\n'.code,
    NextLine,
    FormFeed,
    LineSeparator,
    ParagraphSeparator,
    -> true

    else -> false
}

/**
 * Check if the character is unicode whitespace of any kind
 *
 * @return true if the character is unicode whitespace, false otherwise
 */
fun Int.isUnicodeWhitespace(): Boolean {
    return when (this) {
        CharacterTabulation,
        Space,
        NonBreakSpace,
        OghamSpaceMark,
        EnQuad,
        EmQuad,
        EnSpace,
        EmSpace,
        ThreePerEmSpace,
        FourPerEmSpace,
        SixPerEmSpace,
        FigureSpace,
        PunctuationSpace,
        ThinSpace,
        HairSpace,
        NarrowNonBreakSpace,
        MediumMathematicalSpace,
        IdeographicSpace,
        -> true

        else -> false
    }
}

/**
 * Check if the character is an ASCII character that can be printed unescaped
 *
 * @return true if the character is printable unescaped, false otherwise
 */
fun Int.isPrintableAscii(): Boolean {
    return ' '.code <= this && this <= '~'.code
}

fun Int.isNonAscii(): Boolean {
    return this > 127
}

fun Int.mustEscape(): Boolean {
    return this == '\\'.code || this == '"'.code
}


/**
 * //TODO Potential Behavior Change?
 * Get the escape sequence for characters from the ASCII character set
 *
 * @return A String wrapping the escape sequence string if the character needs to be escaped, or null otherwise
 */
fun Int.getCommonEscape(): String? {
    return when (this) {
        '\\'.code -> ESC_BACKSLASH
        '\b'.code -> ESC_BACKSPACE
        '\n'.code -> ESC_NEWLINE
        FormFeed -> ESC_FORM_FEED //Kotlin doesn't support '\f' so use the unicode directly,
        '/'.code -> ESC_FORWARD_SLASH
        '\t'.code -> ESC_TAB
        '\r'.code -> ESC_CR
        '"'.code -> ESC_QUOTE
        else -> null
    }
}

fun Int.isCommonEscape(): Boolean {
    return when (this) {
        '\\'.code,
        '\b'.code,
        '\n'.code,
        FormFeed,
        '\t'.code,
        '\r'.code,
        '"'.code,
        -> true

        else -> false
    }
}

/**
 * Get the escape sequence for any character
 *
 * @return The escape sequence string
 */
fun Int.getEscapeIncludingUnicode(): String {
    return getCommonEscape() ?: String.format("\\u{%x}", this)
}

internal const val ESC_BACKSPACE = "\\b"
internal const val ESC_NEWLINE = "\\n"
internal const val ESC_FORM_FEED = "\\u000c"
internal const val ESC_FORWARD_SLASH = "\\/"
internal const val ESC_TAB = "\\t"
internal const val ESC_CR = "\\r"
internal const val ESC_QUOTE = "\\\""
internal const val ESC_BACKSLASH = "\\\\"

internal const val LineSeparator = '\u2028'.code
internal const val ParagraphSeparator = '\u2029'.code
internal const val NextLine = '\u0085'.code
internal const val NonBreakSpace = '\u00A0'.code
internal const val OghamSpaceMark = '\u1680'.code
internal const val EnQuad = '\u2000'.code
internal const val EmQuad = '\u2001'.code
internal const val EnSpace = '\u2002'.code
internal const val EmSpace = '\u2003'.code
internal const val ThreePerEmSpace = '\u2004'.code
internal const val FourPerEmSpace = '\u2005'.code
internal const val SixPerEmSpace = '\u2006'.code
internal const val FigureSpace = '\u2007'.code
internal const val PunctuationSpace = '\u2008'.code
internal const val ThinSpace = '\u2009'.code
internal const val HairSpace = '\u200A'.code
internal const val NarrowNonBreakSpace = '\u202F'.code
internal const val MediumMathematicalSpace = '\u205F'.code
internal const val IdeographicSpace = '\u3000'.code
internal const val FormFeed = '\u000C'.code
internal const val CharacterTabulation = '\u0009'.code
internal const val Space = '\u0020'.code