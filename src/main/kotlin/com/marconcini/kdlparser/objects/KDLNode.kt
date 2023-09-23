package com.marconcini.kdlparser.objects

import com.marconcini.kdlparser.print.PrintConfig
import com.marconcini.kdlparser.print.PrintUtil.writeStringQuotedAppropriately
import java.io.IOException
import java.io.Writer
import java.math.BigDecimal
import java.util.Objects
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Predicate

@Suppress("unused")
data class KDLNode constructor(
    val identifier: String,
    val type: String?,
    val props: Map<String, KDLValue<*>>,
    val args: List<KDLValue<*>>,
    val child: KDLDocument?,
) : KDLObject {

    override fun writeKDL(writer: Writer, printConfig: PrintConfig) {
        writeKDLPretty(writer, 0, printConfig)
    }

    @Throws(IOException::class)
    fun writeKDLPretty(writer: Writer, depth: Int, printConfig: PrintConfig) {
        type?.let {
            writer.write('('.code)
            writeStringQuotedAppropriately(writer, it, true, printConfig)
            writer.write(')'.code)
        }

        writeStringQuotedAppropriately(writer, identifier, true, printConfig)

        if (args.isNotEmpty() || props.isNotEmpty() || child != null) {
            writer.write(' '.code)
        }
        for (i in args.indices) {
            val value = args[i]
            if (value !is KDLNull || printConfig.shouldPrintNullArgs()) {
                value.writeKDL(writer, printConfig)
                if (i < args.size - 1 || props.isNotEmpty() || child != null) {
                    writer.write(' '.code)
                }
            }
        }
        val keys: ArrayList<String?> = ArrayList(props.keys)
        keys.forEachIndexed { index, key ->
            val value = props[key]
            if (value !is KDLNull || printConfig.shouldPrintNullProps()) {
                if (key != null && value != null) {
                    writeStringQuotedAppropriately(writer, key, true, printConfig)
                    writer.write('='.code)
                    value.writeKDL(writer, printConfig)
                    if (index < keys.size - 1 || child != null) {
                        writer.write(' '.code)
                    }
                }
            }
        }

        child?.let {
            if (it.nodes.isNotEmpty() || printConfig.shouldPrintEmptyChildren()) {
                writer.write('{'.code)
                writer.write(printConfig.newline)
                it.writeKDL(writer, depth + 1, printConfig)
                for (i in 0 until printConfig.indent * depth) {
                    writer.write(printConfig.indentChar.code)
                }
                writer.write('}'.code)
            }
        }
    }

    override fun toString(): String =
        """KDLNode{identifier=$identifier, type=$type, props=$props, args=$args, child=$child}"""

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private val args: ArrayList<KDLValue<*>> = arrayListOf()
        private val props: MutableMap<String, KDLValue<*>> = ConcurrentHashMap()

        private lateinit var identifier: String
        private var type: String? = null
        private var child: KDLDocument? = null

        fun setIdentifier(value: String) = apply { identifier = value }
        fun setType(value: String) = apply { type = value }
        fun setChild(value: KDLDocument?) = apply { child = value }
        fun addArg(value: KDLValue<*>) = apply { args.add(value) }
        fun insertArgAt(position: Int, value: KDLValue<*>) = apply {
            if (position < args.size) {
                args.add(position, value)
            } else {
                while (args.size < position - 1) {
                    args.add(KDLNull())
                }
                args.add(value)
            }
        }

        fun removeArgIf(predicate: Predicate<KDLValue<*>>) = apply { args.removeIf(predicate) }
        fun removeArg(value: KDLValue<*>) = also { args.remove(value) }
        fun removePropIf(predicate: Predicate<String?>) = also {
            props.keys.forEach {
                if (predicate.test(it)) {
                    props.remove(it)
                }
            }
        }

        fun removeProp(key: String) = apply { props.remove(key) }
        fun addArg(value: String?) = addArg(value, null)
        fun addArg(value: String?, type: String?) = apply {
            args.add(if (value == null) KDLNull(type) else KDLString(value, type))
        }

        fun addArg(value: BigDecimal?, type: String?) = addArg(value, 10, type)
        fun addArg(value: BigDecimal?, radix: Int, type: String?) = apply {
            args.add(if (value == null) KDLNull(type) else KDLNumber(value, radix, type))
        }

        fun addArg(value: Long) = addArg(value, 10)
        fun addArg(value: Long, radix: Int) = addArg(value, radix, null)
        fun addArg(value: Long, radix: Int, type: String?) = apply {
            args.add(KDLNumber(BigDecimal(value), radix, type))
        }

        fun addArg(value: Double) = addArg(value, 10)
        fun addArg(value: Double, radix: Int) = addArg(value, radix, null)
        fun addArg(value: Double, radix: Int, type: String?) = apply {
            args.add(KDLNumber(BigDecimal(value), radix, type))
        }

        fun addNullArg() = addNullArg(null)
        fun addNullArg(type: String?) = apply { args.add(KDLNull(type)) }

        fun addArg(value: Boolean) = addArg(value, null)
        fun addArg(value: Boolean, type: String?) = apply { args.add(KDLBoolean(value, type)) }

        fun addAllArgs(arguments: List<KDLValue<*>>) = apply { args.addAll(arguments) }
        fun addProp(key: String, value: KDLValue<*>?) = apply { props[key] = value ?: KDLNull() }

        fun addProp(key: String, value: String?) = addProp(key, value, null)
        fun addProp(key: String, value: String?, type: String?) = apply {
            props[key] = if (value == null) KDLNull(type) else KDLString(value, type)
        }

        fun addProp(key: String, value: BigDecimal?) = addProp(key, value, null)
        fun addProp(key: String, value: BigDecimal?, type: String?) = apply {
            props[key] = if (value == null) KDLNull(type) else KDLNumber(value, 10, type)
        }

        fun addProp(key: String, value: BigDecimal?, radix: Int) = addProp(key, value, radix, null)
        fun addProp(key: String, value: BigDecimal?, radix: Int, type: String?) = apply {
            props[key] = if (value == null) KDLNull(type) else KDLNumber(value, radix, type)
        }

        fun addProp(key: String, value: Int) = addProp(key, value, null)
        fun addProp(key: String, value: Int, type: String?) = addProp(key, value, 10, type)
        fun addProp(key: String, value: Int, radix: Int) = addProp(key, value, radix, null)
        fun addProp(key: String, value: Int, radix: Int, type: String?) = apply {
            props[key] = KDLNumber(BigDecimal(value), radix, type)
        }

        fun addProp(key: String, value: Double) = addProp(key, value, null)
        fun addProp(key: String, value: Double, type: String?) = addProp(key, value, 10, type)
        fun addProp(key: String, value: Double, radix: Int) = addProp(key, value, radix, null)
        fun addProp(key: String, value: Double, radix: Int, type: String?) = apply {
            props[key] = KDLNumber(BigDecimal(value), radix, type)
        }

        fun addProp(key: String, value: Boolean) = addProp(key, value, null)
        fun addProp(key: String, value: Boolean, type: String?) =
            apply { props[key] = KDLBoolean(value, type) }


        fun addProp(value: KDLProperty) = apply { props[value.getKey()] = value.getValue() }

        fun addAllProps(values: Map<String, KDLValue<*>>) = apply { props.putAll(values) }

        fun addNullProp(key: String) = apply { props.put(key, KDLNull()) }

        fun clearArgs() = apply { args.clear() }

        fun clearProps() = apply { props.clear() }

        fun build(): KDLNode {
            Objects.requireNonNull(identifier, "Identifier must be set")
            return KDLNode(
                identifier = identifier,
                type = type,
                props = props,
                args = args,
                child = child
            )
        }
    }
}