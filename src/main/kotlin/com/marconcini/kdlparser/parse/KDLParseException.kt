package com.marconcini.kdlparser.parse


/**
 * Thrown if a document cannot be parsed for any reason. The message will indicate the error and contain the line
 * and character where the parse failure occurred.
 */
class KDLParseException(message: String?, cause: Throwable?): RuntimeException(message, cause) {
    internal constructor(message: String?): this(message, null)

}