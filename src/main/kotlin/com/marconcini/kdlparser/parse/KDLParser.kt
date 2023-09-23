package com.marconcini.kdlparser.parse

import com.marconcini.kdlparser.objects.KDLBoolean
import com.marconcini.kdlparser.objects.KDLDocument
import com.marconcini.kdlparser.objects.KDLNode
import com.marconcini.kdlparser.objects.KDLNull
import com.marconcini.kdlparser.objects.KDLNumber
import com.marconcini.kdlparser.objects.KDLObject
import com.marconcini.kdlparser.objects.KDLProperty
import com.marconcini.kdlparser.objects.KDLString
import com.marconcini.kdlparser.objects.KDLValue
import com.marconcini.kdlparser.parse.KDLParser.WhitespaceResult.*
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.io.StringReader
import java.math.BigDecimal
import java.math.BigInteger
import java.util.TreeMap
import java.util.function.Predicate


/**
 * The core parser object. Instances are stateless and safe to share between threads.
 */
class KDLParser {
    enum class WhitespaceResult {
        NO_WHITESPACE, END_NODE, SKIP_NEXT, NODE_SPACE
    }

    enum class SlashAction {
        END_NODE, SKIP_NEXT, NOTHING
    }


    /**
     * Parse the given stream into a KDLDocument model object.
     *
     * @param reader the stream reader to parse from
     * @return the parsed document
     * @throws IOException if any error occurs while reading the stream
     * @throws KDLParseException if the document is invalid for any reason
     */
    fun parse(reader: Reader): KDLDocument {
        val context = KDLParseContext(reader)

        return try {
            parseDocument(context, true)
        } catch (e: KDLParseException) {
            val message =
                String.format("%s\n%s", e.message, context.getErrorLocationAndInvalidateContext())
            throw KDLParseException(message, e)
        } catch (e: IOException) {
            throw IOException(context.getErrorLocationAndInvalidateContext(), e)
        } catch (e: KDLInternalException) {
            throw KDLInternalException(context.getErrorLocationAndInvalidateContext(), e)
        } catch (t: Throwable) {
            throw KDLInternalException(
                String.format(
                    "Unexpected exception:\n%s",
                    context.getErrorLocationAndInvalidateContext()
                ), t
            )
        }
    }

    /**
     * Parse the given stream into a KDLDocument model object.
     *
     * @param stream the stream to parse from
     * @return the parsed document
     * @throws IOException if any error occurs while reading the stream
     * @throws KDLParseException if the document is invalid for any reason
     */
    @Suppress("unused")
    @Throws(IOException::class)
    fun parse(stream: InputStream): KDLDocument {
        return parse(InputStreamReader(stream))
    }


    /**
     * Parse the given string into a KDLDocument model object.
     *
     * @param string the string to parse
     * @return the parsed document
     * @throws KDLParseException if the document is invalid for any reason
     */
    fun parse(string: String): KDLDocument? {
        val reader = StringReader(string)
        return try {
            parse(reader)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @Throws(IOException::class)
    fun parseChild(context: KDLParseContext): KDLDocument {
        var c = context.read()
        if (c != '{'.code) {
            throw KDLInternalException(String.format("Expected '{' but found '%s'", c.toChar()))
        }
        val document = parseDocument(context, false)
        when (consumeWhitespaceAndLinespace(context)) {
            END_NODE -> throw KDLInternalException("Got unexpected END_NODE")
            SKIP_NEXT -> throw KDLParseException("Trailing skip markers are not allowed")
            else -> {}
        }
        c = context.read()
        if (c != '}'.code) {
            throw KDLParseException("No closing brace found for child")
        }
        return document
    }

    @Throws(IOException::class)
    private fun parseDocument(context: KDLParseContext, root: Boolean): KDLDocument {
        var c = context.peek()
        if (c == EOF) return KDLDocument(emptyList())

        val nodes: ArrayList<KDLNode> = arrayListOf()
        while (true) {
            var skippingNode = false
            when (consumeWhitespaceAndLinespace(context)) {
                NODE_SPACE,
                NO_WHITESPACE -> {}
                END_NODE -> {
                    c = context.peek()
                    if (c != EOF) continue
                }
                SKIP_NEXT -> skippingNode = true
            }

            c = context.peek()
            if (c == EOF) {
                if (root) {
                    return KDLDocument(nodes)
                } else {
                    throw KDLParseException("Got EOF, expected a node or '}'")
                }
            } else if (c == '}'.code) {
                if (!root) {
                    return KDLDocument(nodes)
                } else {
                    throw KDLParseException("Unexpected '}' in root document")
                }
            }

            val node = parseNode(context)
            consumeAfterNode(context)
            if (!skippingNode && node != null) {
                nodes.add(node)
            }
        }
    }

    fun parseNode(context: KDLParseContext): KDLNode? {
        val args: ArrayList<KDLValue<*>> = ArrayList()
        val properties: MutableMap<String, KDLValue<*>> = TreeMap()
        var child: KDLDocument? = null

        var c = context.peek()
        if (c == '}'.code) return null

        val type: String? = parseTypeIfPresent(context)
        val identifier: String = parseIdentifier(context)

        while (true) {
            val whitespaceResult = consumeWhitespaceAndBlockComments(context)

            c = context.peek()
            when (whitespaceResult) {
                NODE_SPACE -> {
                    if (c == '{'.code) {
                        child = parseChild(context)
                        return KDLNode(identifier, type, properties, args, child)
                    } else if (c.isUnicodeLinespace()) {
                        return KDLNode(identifier, type, properties, args, child)
                    }
                    if (c == EOF) {
                        //TODO: is this the same? I think the code has a missing space (uses if not else if)
                        return KDLNode(identifier, type, properties, args, child)
                    } else {
                        when (val kdlObject: KDLObject = parseArgOrProp(context)) {
                            is KDLValue<*> -> args.add(kdlObject)
                            is KDLProperty -> properties[kdlObject.getKey()] = kdlObject.getValue()
                            else -> {
                                throw KDLInternalException(
                                    String.format(
                                        "Unexpected type found, expected property, arg, or child: '%s' type: %s",
                                        kdlObject.toKDL(), kdlObject.javaClass.simpleName
                                    )
                                )
                            }
                        }
                    }
                }
                NO_WHITESPACE -> {
                    return if (c == '{'.code) {
                        child = parseChild(context)
                        KDLNode(identifier, type, properties, args, child)
                    } else if (c.isUnicodeLinespace() || c == EOF) {
                        KDLNode(identifier, type, properties, args, child)
                    } else if (c == ';'.code) {
                        context.read()
                        KDLNode(identifier, type, properties, args, child)
                    } else {
                        throw KDLParseException(
                            String.format(
                                "Unexpected character: '%s' (\\u%06X)",
                                c.toChar(),
                                c
                            )
                        )
                    }
                }
                END_NODE -> return KDLNode(identifier, type, properties, args, child)
                SKIP_NEXT -> {
                    if (c == '{'.code) {
                        parseChild(context) //Ignored
                        return KDLNode(identifier, type, properties, args, child)
                    } else if (c.isUnicodeLinespace()) {
                        throw KDLParseException("Unexpected skip marker before newline")
                    } else if (c == EOF) {
                        throw KDLParseException("Unexpected EOF following skip marker")
                    } else {
                        val kdlObject: KDLObject = parseArgOrProp(context)
                        if (kdlObject !is KDLValue<*> && kdlObject !is KDLProperty) {
                            throw KDLInternalException(
                                String.format(
                                    "Unexpected type found, expected property, arg, or child: '%s' type: %s",
                                    kdlObject.toKDL(), kdlObject.javaClass.simpleName
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    @Throws(IOException::class)
    fun parseArgOrProp(context: KDLParseContext): KDLObject {
        val kdlObject: KDLObject
        val type: String? = parseTypeIfPresent(context)
        var isBare = false
        var c = context.peek()

        if (c == '"'.code) {
            kdlObject = KDLString(parseEscapedString(context), type)
        } else if (c == '+'.code || c == '-'.code) {
            val sign = c
            context.read()
            c = context.peek()
            context.unread(sign)
            if (c.isValidDecimalChar()) {
                kdlObject = parseNumber(context, type)
            } else {
                isBare = true
                kdlObject = KDLString(parseBareIdentifier(context))
            }
        } else if (c.isValidNumericStart()) {
            kdlObject = parseNumber(context, type)
        } else if (c.isValidBareIdStart()) {
            val value: String
            if (c == 'r'.code) {
                context.read()
                val next = context.peek()
                context.unread('r'.code)
                if (next == '"'.code || next == '#'.code) {
                    value = parseRawString(context)
                } else {
                    isBare = true
                    value = parseBareIdentifier(context)
                }
            } else {
                isBare = true
                value = parseBareIdentifier(context)
            }

            kdlObject = if (isBare) {
                when (value) {
                    "true" -> KDLBoolean(true, type)
                    "false" -> KDLBoolean(false, type)
                    "null" -> KDLNull(type)
                    else -> KDLString(value, type)
                }
            } else {
                KDLString(value, type)
            }
        } else {
            throw KDLParseException(String.format("Unexpected character: '%s'", c.toChar()))
        }

        if (kdlObject is KDLString) {
            c = context.peek()
            if (c == '='.code) {
                if (type != null) {
                    throw KDLParseException(
                        "Illegal type annotation before property, annotations " +
                                "should follow the '=' and precede the value"
                    )
                }
                context.read()
                val value: KDLValue<*> = parseValue(context)
                return KDLProperty(kdlObject.getValue(), value)
            } else if (isBare) {
                throw KDLParseException(
                    String.format(
                        "Arguments may not be bare: '%s'",
                        kdlObject.getValue()
                    )
                )
            } else {
                return kdlObject
            }
        } else {
            return kdlObject
        }
    }

    @Throws(IOException::class)
    fun parseValue(context: KDLParseContext): KDLValue<*> {
        val type: String? = parseTypeIfPresent(context)
        var c = context.peek()
        return if (c == '"'.code) {
            KDLString(parseEscapedString(context), type)
        } else if (c == 'r'.code) {
            KDLString(parseRawString(context), type)
        } else if (c.isValidNumericStart()) {
            parseNumber(context, type)
        } else {
            val stringBuilder = StringBuilder()
            while (c.isLiteralChar()) {
                context.read()
                stringBuilder.appendCodePoint(c)
                c = context.peek()
            }
            when (val value = stringBuilder.toString()) {
                "true" -> KDLBoolean(true, type)
                "false" -> KDLBoolean(false, type)
                "null" -> KDLNull(type)
                else -> throw KDLParseException(
                    String.format(
                        "Unknown literal in property value: '%s' Expected 'true', 'false', or 'null'",
                        value
                    )
                )
            }
        }
    }

    @Throws(IOException::class)
    fun parseNumber(context: KDLParseContext, type: String?): KDLNumber {
        val radix: Int
        var legalChars: Predicate<Int>? = null
        var c = context.peek()
        var sign = '+'
        if (c == '+'.code) {
            context.read()
            c = context.peek()
        } else if (c == '-'.code) {
            sign = '-'
            context.read()
            c = context.peek()
        }
        if (c == '0'.code) {
            context.read()
            c = context.peek()
            when (c) {
                'x'.code -> {
                    context.read()
                    radix = 16
                    legalChars = Predicate<Int> { value -> value.isValidHexChar() }
                }
                'o'.code -> {
                    context.read()
                    radix = 8
                    legalChars = Predicate<Int> { value -> value.isValidOctalChar() }
                }
                'b'.code -> {
                    context.read()
                    radix = 2
                    legalChars = Predicate<Int> { value -> value.isValidBinaryChar() }
                }
                else -> {
                    context.unread('0'.code)
                    radix = 10
                }
            }
        } else {
            radix = 10
        }

        return if (radix == 10) {
            parseDecimalNumber(context, sign, type)
        } else {
            parseNonDecimalNumber(context, legalChars, sign, radix, type)
        }
    }

    // Unfortunately, in order to match the grammar we have to do a lot of parsing ourselves here
    @Throws(IOException::class)
    fun parseDecimalNumber(
        context: KDLParseContext,
        sign: Char,
        type: String?
    ): KDLNumber {
        val stringBuilder = StringBuilder()
        stringBuilder.append(sign)
        var inFraction = false
        var inExponent = false
        var signLegal = false
        var exponentLen = 0
        var c = context.peek()
        if (c == '_'.code || c == 'E'.code || c == 'e'.code) {
            throw KDLParseException(
                String.format(
                    "Decimal numbers may not begin with an '%s' character",
                    c.toChar()
                )
            )
        } else if (c == '+'.code || c == '-'.code) {
            throw KDLParseException("Numbers may not begin with multiple sign characters")
        }
        c = context.peek()
        while (c.isValidDecimalChar() || c == 'e'.code || c == 'E'.code || c == '_'.code || c == '.'.code || c == '-'.code || c == '+'.code) {
            context.read()
            if (inExponent && c != '-'.code && c != '+'.code) {
                exponentLen++
            }
            if (c == '.'.code) {
                if (inFraction || inExponent) {
                    throw KDLParseException("The '.' character is not allowed in the fraction or exponent of a decimal")
                }
                if (!context.peek().isValidDecimalChar()) {
                    throw KDLParseException("The character following '.' in a decimal number must be a decimal digit")
                }
                inFraction = true
                signLegal = false
                stringBuilder.appendCodePoint(c)
            } else if (c == 'e'.code || c == 'E'.code) {
                if (inExponent) {
                    throw KDLParseException(String.format("Found '%s' in exponent", c.toChar()))
                }
                inExponent = true
                inFraction = false
                signLegal = true
                stringBuilder.appendCodePoint(c)
                if (context.peek() == '_'.code) {
                    throw KDLParseException("Character following exponent marker must not be '_'")
                }
            } else if (c == '_'.code) {
                signLegal = false
            } else if (c == '+'.code || c == '-'.code) {
                if (!signLegal) {
                    throw KDLParseException(
                        String.format(
                            "The sign character '%s' is not allowed here",
                            c.toChar()
                        )
                    )
                }
                signLegal = false
                stringBuilder.appendCodePoint(c)
            } else {
                signLegal = false
                stringBuilder.appendCodePoint(c)
            }
            c = context.peek()
        }

        val value = stringBuilder.toString()
        if (exponentLen > 10) { //BigDecimal only accepts exponents up to 10 digits
            throw KDLInternalException(
                String.format(
                    "Exponent too long to be represented as a BigDecimal: '%s'",
                    value
                )
            )
        }
        return try {
            KDLNumber.from(BigDecimal(value), type)
        } catch (e: java.lang.NumberFormatException) {
            throw KDLInternalException(
                String.format(
                    "Couldn't parse pre-vetted input '%s' into a BigDecimal",
                    value
                ), e
            )
        }
    }

    @Throws(IOException::class)
    fun parseNonDecimalNumber(
        context: KDLParseContext,
        legalChars: Predicate<Int>?,
        sign: Char,
        radix: Int,
        type: String?
    ): KDLNumber {
        val stringBuilder = StringBuilder()
        stringBuilder.append(sign)
        var c = context.peek()
        if (c == '_'.code) {
            throw KDLParseException("The first character after radix indicator must not be '_'")
        }
        if (legalChars == null) {
            throw KDLParseException("With Radix = 10, we should not be parsing Non Decimal numbers ")
        }

        while (legalChars.test(c) || c == '_'.code) {
            context.read()
            if (c != '_'.code) {
                stringBuilder.appendCodePoint(c)
            }
            c = context.peek()
        }
        val str = stringBuilder.toString()
        if (str.length == 1) { //str is only the radix
            throw KDLParseException("Must include at least one digit following radix marker")
        }
        return try {
            KDLNumber.from(BigInteger(str, radix), radix, type)
        } catch (e: NumberFormatException) {
            throw KDLInternalException(
                String.format(
                    "Couldn't parse pre-vetted input '%s' into a BigDecimal",
                    str
                ), e
            )
        }
    }

    @Throws(IOException::class)
    fun parseBareIdentifier(context: KDLParseContext): String {
        var charCode = context.read()
        if (!charCode.isValidBareIdStart()) {
            throw KDLParseException("Illegal character at start of bare identifier")
        } else if (charCode == EOF) {
            throw KDLInternalException("EOF when a bare identifier expected")
        }
        val stringBuilder = StringBuilder()
        stringBuilder.appendCodePoint(charCode)
        charCode = context.peek()
        while (charCode.isValidBareIdChar() && charCode != EOF) {
            stringBuilder.appendCodePoint(context.read())
            charCode = context.peek()
        }
        return stringBuilder.toString()
    }

    @Throws(IOException::class)
    fun parseIdentifier(context: KDLParseContext): String {
        val c = context.peek()
        return when {
            c == '"'.code -> parseEscapedString(context)
            c.isValidBareIdStart() -> {
                if (c == 'r'.code) {
                    context.read()
                    val next = context.peek()
                    context.unread('r'.code)
                    if (next == '"'.code || next == '#'.code) {
                        parseRawString(context)
                    } else {
                        parseBareIdentifier(context)
                    }
                } else {
                    parseBareIdentifier(context)
                }
            }

            else -> {
                throw KDLParseException(
                    String.format(
                        "Expected an identifier, but identifiers can't start with '%s' (\\u%06X)",
                        c.toChar(),
                        c
                    )
                )
            }
        }
    }


    @Throws(IOException::class)
    fun parseTypeIfPresent(context: KDLParseContext): String? {
        var type: String? = null
        var c = context.peek()
        if (c == '('.code) {
            context.read()
            type = parseIdentifier(context)
            c = context.read()
            if (c != ')'.code) {
                throw KDLParseException("Un-terminated type annotation, missing closing paren.")
            }
        }
        return type
    }

    @Throws(IOException::class)
    fun consumeAfterNode(context: KDLParseContext) {
        var c = context.peek()
        while (c == ';'.code || c.isUnicodeWhitespace()) {
            context.read()
            c = context.peek()
        }
    }

    @Throws(IOException::class)
    fun consumeWhitespaceAndLinespace(context: KDLParseContext): WhitespaceResult {
        var skipNext = false
        var foundWhitespace = false
        var inEscape = false
        while (true) {
            var c = context.peek()
            var isLinespace: Boolean = c.isUnicodeLinespace()
            while (c.isUnicodeWhitespace() || isLinespace || c == '\uFEFF'.code) {
                foundWhitespace = true
                if (isLinespace && skipNext && !inEscape) {
                    throw KDLParseException("Unexpected newline after skip marker")
                }
                if (isLinespace && inEscape) {
                    inEscape = false
                }
                context.read()
                c = context.peek()
                isLinespace = c.isUnicodeLinespace()
            }
            if (c == '/'.code) {
                when (getSlashAction(context, inEscape)) {
                    SlashAction.END_NODE, SlashAction.NOTHING -> foundWhitespace = true
                    SlashAction.SKIP_NEXT -> {
                        foundWhitespace = true
                        skipNext = true
                    }
                }
            } else if (c == '\\'.code) {
                context.read()
                foundWhitespace = true
                inEscape = true
            } else if (c == EOF) {
                return if (skipNext) {
                    throw KDLParseException("Unexpected EOF after skip marker")
                } else if (inEscape) {
                    throw KDLParseException("Unexpected EOF after line escape")
                } else {
                    END_NODE
                }
            } else {
                if (inEscape) {
                    throw KDLParseException("Expected newline or line comment following escape")
                }
                return if (skipNext) {
                    SKIP_NEXT
                } else if (foundWhitespace) {
                    NODE_SPACE
                } else {
                    NO_WHITESPACE
                }
            }
        }
    }


    @Throws(IOException::class)
    fun parseEscapedString(context: KDLParseContext): String {
        var charCode = context.read()
        if (charCode != '"'.code) {
            throw KDLInternalException("No quote at the beginning of escaped string")
        }
        val stringBuilder = StringBuilder()
        var inEscape = false
        while (true) {
            charCode = context.read()
            if (!inEscape && charCode == '\\'.code) {
                inEscape = true
            } else if (charCode == '"'.code && !inEscape) {
                return stringBuilder.toString()
            } else if (inEscape) {
                stringBuilder.appendCodePoint(getEscaped(charCode, context))
                inEscape = false
            } else if (charCode == EOF) {
                throw KDLParseException("EOF while reading an escaped string")
            } else {
                stringBuilder.appendCodePoint(charCode)
            }
        }
    }

    @Throws(IOException::class)
    fun getEscaped(c: Int, context: KDLParseContext): Int {
        var charCode = c
        return when (charCode) {
            'n'.code -> '\n'.code
            'r'.code -> '\r'.code
            't'.code -> '\t'.code
            '\\'.code -> '\\'.code
            '/'.code -> '/'.code
            '"'.code -> '\"'.code
            'b'.code -> '\b'.code
            'f'.code -> FormFeed
            'u'.code -> {
                val stringBuilder = StringBuilder(6)
                charCode = context.read()
                if (charCode != '{'.code) {
                    throw KDLParseException("Unicode escape sequences must be surround by {} brackets")
                }
                charCode = context.read()
                while (charCode != '}'.code) {
                    if (charCode == EOF) {
                        throw KDLParseException("Reached EOF while reading unicode escape sequence")
                    } else if (!charCode.isValidHexChar()) {
                        throw KDLParseException(
                            String.format(
                                "Unicode escape sequences must be valid hex chars, got: '%s'",
                                charCode.toChar()
                            )
                        )
                    }
                    stringBuilder.appendCodePoint(charCode)
                    charCode = context.read()
                }
                val strCode = stringBuilder.toString()
                if (strCode.isEmpty() || strCode.length > 6) {
                    throw KDLParseException(
                        String.format(
                            "Unicode escape sequences must be between 1 and 6 characters in length. Got: '%s'",
                            strCode
                        )
                    )
                }
                val code: Int = try {
                    strCode.toInt(16)
                } catch (e: NumberFormatException) {
                    throw KDLParseException(
                        String.format(
                            "Couldn't parse '%s' as a hex integer",
                            strCode
                        )
                    )
                }
                if (code < 0 || MAX_UNICODE < code) {
                    throw KDLParseException(
                        java.lang.String.format(
                            "Unicode code point is outside allowed range [0, %x]: %x",
                            MAX_UNICODE,
                            code
                        )
                    )
                } else {
                    code
                }
            }

            else -> throw KDLParseException(
                String.format(
                    "Illegal escape sequence: '\\%s'",
                    charCode.toChar()
                )
            )
        }
    }

    @Throws(IOException::class)
    fun consumeWhitespaceAndBlockComments(context: KDLParseContext): WhitespaceResult {
        var skipping = false
        var foundWhitespace = false
        var inLineEscape = false
        var foundSemicolon = false
        var c = context.peek()

        while (c == '/'.code ||
            c == '\\'.code ||
            c == ';'.code ||
            c == '\uFEFF'.code ||
            c.isUnicodeWhitespace() ||
            c.isUnicodeLinespace()
        ) {
            if (c == '/'.code) {
                when (getSlashAction(context, inLineEscape)) {
                    SlashAction.END_NODE -> {
                        if (inLineEscape) {
                            foundWhitespace = true
                            inLineEscape = false
                        } else {
                            return END_NODE
                        }
                    }

                    SlashAction.SKIP_NEXT -> {
                        if (inLineEscape) {
                            throw KDLParseException("Found skip marker after line escape")
                        }
                        skipping = if (!skipping) true else throw KDLParseException("Node/Token skip may only be specified once per node/token")
                    }

                    SlashAction.NOTHING -> foundWhitespace = true
                }
            } else if (c == ';'.code) {
                context.read()
                foundSemicolon = true
            } else if (c == '\\'.code) {
                context.read()
                inLineEscape = true
            } else if (c.isUnicodeLinespace()) {
                if (inLineEscape) {
                    context.read()
                    if (c == '\r'.code) {
                        c = context.peek()
                        if (c == '\n'.code) context.read()
                    }
                    inLineEscape = false
                    foundWhitespace = true
                } else {
                    break
                }
            } else {
                context.read()
                foundWhitespace = true
            }
            c = context.peek()
        }
        return if (skipping) {
            SKIP_NEXT
        } else if (foundSemicolon) {
            END_NODE
        } else if (foundWhitespace) {
            NODE_SPACE
        } else {
            NO_WHITESPACE
        }
    }

    @Throws(IOException::class)
    fun getSlashAction(context: KDLParseContext, escaped: Boolean): SlashAction {
        var c = context.read()
        if (c != '/'.code) {
            throw KDLInternalException(
                String.format(
                    "Expected '/' but found '%s' (\\u%06x)",
                    c.toChar(),
                    c
                )
            )
        }
        c = context.read()
        return when (c) {
            '-'.code -> SlashAction.SKIP_NEXT
            '*'.code -> {
                consumeBlockComment(context)
                SlashAction.NOTHING
            }

            '/'.code -> {
                consumeLineComment(context)
                if (escaped) {
                    SlashAction.NOTHING
                } else {
                    SlashAction.END_NODE
                }
            }

            else -> throw KDLParseException(String.format("Unexpected character: '%s'", c.toChar()))
        }
    }

    @Throws(IOException::class)
    fun parseRawString(context: KDLParseContext): String {
        var c = context.read()
        if (c != 'r'.code) {
            throw KDLInternalException("Raw string should start with 'r'")
        }

        var hashDepth = 0
        c = context.read()
        while (c == '#'.code) {
            hashDepth++
            c = context.read()
        }


        if (c != '"'.code) {
            throw KDLParseException("Malformed raw string")
        }

        val stringBuilder = StringBuilder()
        while (true) {
            c = context.read()
            if (c == '"'.code) {
                val subStringBuilder = StringBuilder()
                subStringBuilder.append('"')
                var hashDepthHere = 0
                while (true) {
                    c = context.peek()
                    if (c == '#'.code) {
                        context.read()
                        hashDepthHere++
                        subStringBuilder.append('#')
                    } else {
                        break
                    }
                }

                if (hashDepthHere < hashDepth) {
                    stringBuilder.append(subStringBuilder)
                } else if (hashDepthHere == hashDepth) {
                    return stringBuilder.toString()
                } else {
                    throw KDLParseException("Too many # characters when closing raw string")
                }
            } else if (c == EOF) {
                throw KDLParseException("EOF while reading raw string")
            } else {
                stringBuilder.appendCodePoint(c)
            }
        }
    }

    @Throws(IOException::class)
    fun consumeBlockComment(context: KDLParseContext) {
        while (true) {
            var c = context.read()
            while (c != '/'.code && c != '*'.code && c != EOF) {
                c = context.read()
            }
            if (c == EOF) {
                throw KDLParseException("Got EOF while reading block comment")
            }
            if (c == '/'.code) {
                c = context.peek()
                if (c == '*'.code) {
                    context.read()
                    consumeBlockComment(context)
                }
            } else { // c == '*'
                c = context.peek()
                if (c == '/'.code) {
                    context.read()
                    return
                }
            }
        }
    }


    @Throws(IOException::class)
    fun consumeLineComment(context: KDLParseContext) {
        var c = context.peek()
        while (!c.isUnicodeLinespace() && c != EOF) {
            context.read()
            if (c == '\r'.code) {
                c = context.peek()
                if (c == '\n'.code) {
                    context.read()
                }
                return
            }
            c = context.peek()
        }
    }

    companion object {
        const val EOF = -1
        const val MAX_UNICODE = 0x10FFFF
    }

}