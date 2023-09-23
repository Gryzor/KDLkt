package com.marconcini.kdlparser.parse

/**
 * Thrown if an unexpected state is encountered while parsing a document. If you encounter this
 * please create an issue on https://github.com/hkolbeck/kdl4j/issues with the offending document
 */
class KDLInternalException(message: String?, cause: Throwable?): RuntimeException(message, cause) {
    internal constructor(message: String?): this(message, null)}