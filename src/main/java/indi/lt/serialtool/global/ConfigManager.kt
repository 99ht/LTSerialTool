package indi.lt.serialtool.global

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

/**
 * 保存键值对
 */
object ConfigManager {
    private val LOG: Logger = LogManager.getLogger(ConfigManager::class.java)

    private val CONFIG_DIR = "${System.getProperty("user.home")}${File.separator}.serialtool"
    private val CONFIG_FILE = "$CONFIG_DIR${File.separator}config.properties"

    private val props = Properties()

    // 常量定义
    const val KEY_RECEIVE_SPLIT_PANE_DIVIDER_POSITIONS: String = "receive.splitPane.dividerPositions"

    init {
        load()
    }

    private fun load() {
        try {
            val dirPath = Path.of(CONFIG_DIR)
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath)
            }
            val file = File(CONFIG_FILE)
            if (file.exists()) {
                FileReader(file).use { reader ->
                    props.load(reader)
                }
            }
        } catch (e: IOException) {
            LOG.error(e)
        }
    }

    fun save() {
        try {
            FileWriter(CONFIG_FILE).use { writer ->
                props.store(writer, "SerialTool Configuration")
            }
        } catch (e: IOException) {
            LOG.error(e)
        }
    }

    @JvmStatic
    fun set(key: String?, value: String?) {
        props.setProperty(key, value)
        save()
    }

    @JvmStatic
    fun get(key: String?, defaultValue: String?): String {
        return props.getProperty(key, defaultValue)
    }

    @JvmStatic
    fun get(key: String?): String {
        return props.getProperty(key)
    }

    @JvmStatic
    fun <T> set(key: String?, value: T) {
        props.setProperty(key, value.toString())
        save()
    }

    @JvmStatic
    fun <T> get(key: String?, clazz: Class<T>, defaultValue: T): T {
        val value = props.getProperty(key)
        return if (value != null) {
            try {
                when (clazz) {
                    Int::class.java -> value.toInt() as T
                    Long::class.java -> value.toLong() as T
                    Boolean::class.java -> value.toBoolean() as T
                    Float::class.java -> value.toFloat() as T
                    Double::class.java -> value.toDouble() as T
                    String::class.java -> value as T
                    else -> defaultValue
                }
            } catch (e: Exception) {
                defaultValue
            }
        } else {
            defaultValue
        }
    }

    @JvmStatic
    fun <T> get(key: String?, clazz: Class<T>): T? {
        val value = props.getProperty(key)
        return if (value != null) {
            try {
                when (clazz) {
                    Int::class.java -> value.toInt() as T
                    Long::class.java -> value.toLong() as T
                    Boolean::class.java -> value.toBoolean() as T
                    Float::class.java -> value.toFloat() as T
                    Double::class.java -> value.toDouble() as T
                    String::class.java -> value as T
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
}