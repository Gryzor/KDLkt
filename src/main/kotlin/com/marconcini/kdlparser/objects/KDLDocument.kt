package com.marconcini.kdlparser.objects

import com.marconcini.kdlparser.print.PrintConfig
import java.io.BufferedWriter
import java.io.IOException
import java.io.StringWriter
import java.io.Writer

@Suppress("unused")
data class KDLDocument(val nodes: List<KDLNode>) : KDLObject {

    override fun writeKDL(writer: Writer, printConfig: PrintConfig) {
        writeKDLPretty(writer, printConfig)
    }

    /**
     * Writes a text representation of the document to the provided writer
     *
     * @param writer the writer to write to
     * @param printConfig configuration controlling how the document is written
     * @throws IOException if there's any error writing the document
     */
    @Throws(IOException::class)
    fun writeKDLPretty(writer: Writer, printConfig: PrintConfig) {
        writeKDL(writer, 0, printConfig)
    }

    /**
     * Write a text representation of the document to the provided writer with default 'pretty' settings
     *
     * @param writer the writer to write to
     * @throws IOException if there's any error writing the document
     */
    @Throws(IOException::class)
    fun writeKDLPretty(writer: Writer) {
        writeKDLPretty(writer, PrintConfig.PRETTY_DEFAULT)
    }

    /**
     * Get a string representation of the document
     *
     * @param printConfig configuration controlling how the document is written
     * @return the string
     */
    fun toKDLPretty(printConfig: PrintConfig): String {
        val writer = StringWriter()
        val bufferedWriter = BufferedWriter(writer)
        try {
            writeKDLPretty(bufferedWriter, printConfig)
            bufferedWriter.flush()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        return writer.toString()
    }

    /**
     * Get a string representation of the document with default 'pretty' settings
     */
    fun toKDLPretty(): String = toKDLPretty(PrintConfig.PRETTY_DEFAULT)

    @Throws(IOException::class)
    fun writeKDL(writer: Writer, depth: Int, printConfig: PrintConfig) {
        if (nodes.isEmpty() && depth == 0) {
            writer.write(printConfig.newline)
            return
        }
        for (node in nodes) {
            for (i in 0 until printConfig.indent * depth) {
                writer.write(printConfig.indentChar.code)
            }

            node.writeKDLPretty(writer, depth, printConfig)

            if (printConfig.shouldRequireSemicolons()) {
                writer.write(';'.code)
            }

            writer.write(printConfig.newline)
        }
    }

    override fun toString(): String = """KDLDocument{nodes=$nodes}"""

    class Builder {
        private val nodes = mutableListOf<KDLNode>()

        fun addNode(node: KDLNode) = apply { nodes.add(node) }
        fun addNodes(nodeCollection: Collection<KDLNode>) = apply { nodes.addAll(nodeCollection) }
        fun build(): KDLDocument = KDLDocument(nodes.toList())
    }

    companion object {
        /**
         * Get a document with no nodes
         *
         * @return the empty document
         */
        @JvmStatic
        fun empty(): KDLDocument = KDLDocument(emptyList())

        /**
         * Get a builder used to gradually build a document
         *
         * @return the builder
         */
        @JvmStatic
        fun builder(): Builder = Builder()
    }
}