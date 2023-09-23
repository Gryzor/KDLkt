package com.marconcini.kdlparser.parse

import com.marconcini.kdlparser.parse.KDLParser.Companion.EOF
import java.io.IOException
import java.io.PushbackReader
import java.io.Reader

/**
 * Internal class wrapping the stream containing the document being read. Maintains a list of the last three lines read
 * in order to provide context in the event of a parse error.
 */
class KDLParseContext(reader: Reader) {
    private val reader: PushbackReader
    private val lines: ArrayDeque<StringBuilder> = ArrayDeque()
    private var positionInLine = 0
    private var lineNumber = 1
    private var invalidated = false

    init {
        this.lines.add(StringBuilder())
        this.reader = PushbackReader(reader, 2)
    }

    /**
     * Read a character from the underlying stream. Stores it in a buffer as well for error reporting.
     *
     * @return the character read or EOF if the stream has been exhausted
     * @throws IOException if any error is encountered in the stream read operation
     */
    @Throws(IOException::class)
    fun read(): Int {
        if (invalidated) {
            throw KDLInternalException("Attempt to read from an invalidated context")
        }
        val char = reader.read()
        when {
            char == EOF -> return char
            char.isUnicodeLinespace() -> {
                // We're cheating a bit here and not checking for CRLF
                positionInLine = 0
                lineNumber++
                lines.add(StringBuilder())
                while (lines.size > 3) {
                    lines.removeLast()
                }
            }

            else -> {
                positionInLine++
                lines.first().appendCodePoint(char)
            }
        }

        return char
    }

    /**
     * Pushes a single character back into the stream. If this method and the peek() function are invoked more than
     * two times without a read() in between an exception will be thrown.
     *
     * @param char the character to be pushed
     */
    fun unread(char: Int) {
        if (invalidated) {
            throw KDLInternalException("Attempt to unread from an invalidated context")
        }
        when {
            char.isUnicodeLinespace() -> {
                lines.removeFirst()
                lineNumber--
                positionInLine = lines.first().length - 1
            }

            char == EOF -> throw KDLInternalException("Attempted to unread() EOF")
            else -> {
                positionInLine--
                val currLine: java.lang.StringBuilder = lines.first()
                currLine.deleteCharAt(currLine.length - 1)
            }
        }
        try {
            reader.unread(char)
        } catch (e: IOException) {
            throw KDLInternalException("Attempted to unread more than 2 characters in sequence", e)
        }
    }


    /**
     * Gets the next character in the stream without consuming it. See unread() for a note on calling this function
     *
     * @return the next character in the stream
     * @throws IOException if any error occurs reading from the stream
     */
    @Throws(IOException::class)
    fun peek(): Int {
        if (invalidated) {
            throw KDLInternalException("Attempt to peek at an invalidated context")
        }
        val char = reader.read()
        if (char != EOF) reader.unread(char)
        return char
    }


    /**
     * For use following parse and internal errors for error reporting. Invalidates the context, after which any
     * following operation on the context will fail. Reads the remainder of the current line and returns a string
     * holding the current line followed by a pointer to the character where the context had read to prior to this call.
     *
     * @return the string outlined above
     */
    fun getErrorLocationAndInvalidateContext(): String {
        if (invalidated) {
            throw KDLInternalException("Attempted to getErrorLocationAndInvalidateContext from an invalid context")
        }
        invalidated = true

        val line: StringBuilder = lines.first()
        //TODO  ?: throw KDLInternalException("Attempted to report an error, but there were no line objects in the stack")
        try {
            var c = reader.read()
            while (!c.isUnicodeLinespace() && c != EOF) {
                line.appendCodePoint(c)
                c = reader.read()
            }
        } catch (e: IOException) {
            line.append("<Read Error>")
        }

        val stringBuilder = StringBuilder()
        stringBuilder
            .append("Line ")
            .append(lineNumber)
            .append(":\n")
            .append(line)
            .append('\n')
        for (i in 0 until positionInLine - 1) {
            stringBuilder.append('-')
        }

        return stringBuilder.append('^').toString()
    }
}