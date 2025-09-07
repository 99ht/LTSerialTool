package indi.lt.serialtool;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ConfigManager {

    private static final Logger LOG = LogManager.getLogger(ConfigManager.class);

    private static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + ".serialtool";
    private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "config.properties";

    private static Properties props = new Properties();

    static {
        load();
    }

    private static void load() {
        try {
            Path dirPath = Path.of(CONFIG_DIR);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            File file = new File(CONFIG_FILE);
            if (file.exists()) {
                try (FileReader reader = new FileReader(file)) {
                    props.load(reader);
                }
            }
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            props.store(writer, "SerialTool Configuration");
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    public static void set(String key, String value) {
        props.setProperty(key, value);
        save();
    }

    public static String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }
}
