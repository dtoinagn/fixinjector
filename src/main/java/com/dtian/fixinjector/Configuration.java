package com.dtian.fixinjector;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class Configuration {
    private static final String DEFAULT_CONFIG_FILE = "application.properties";
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 9999;
    private static final int DEFAULT_INJECTION_RATE = 1000;
    private static final String DEFAULT_INPUT_FILE = "c:\\share\\data\\fix-messages.txt";

    private final Properties properties;

    private Configuration(Properties properties) {
        this.properties = properties;
    }

    public static Configuration load() throws IOException {
        return load(new String[0]);
    }
    
    public static Configuration load(String[] args) throws IOException {
        Properties props = new Properties();
        
        Path configPath = Paths.get(DEFAULT_CONFIG_FILE);
        if (Files.exists(configPath)) {
            try (InputStream is = Files.newInputStream(configPath)) {
                props.load(is);
            }
        } else {
            try (InputStream is = Configuration.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILE)) {
                if (is != null) {
                    props.load(is);
                }
            }
        }
        
        parseCommandLineArgs(args, props);
        
        return new Configuration(props);
    }

    public String getInputFile() {
        return properties.getProperty("input.file", DEFAULT_INPUT_FILE);
    }

    public String getHost() {
        return properties.getProperty("target.host", DEFAULT_HOST);
    }

    public int getPort() {
        return Integer.parseInt(properties.getProperty("target.port", String.valueOf(DEFAULT_PORT)));
    }

    public int getInjectionRate() {
        return Integer.parseInt(properties.getProperty("injection.rate", String.valueOf(DEFAULT_INJECTION_RATE)));
    }

    public int getBufferSize() {
        return Integer.parseInt(properties.getProperty("buffer.size", "8192"));
    }

    public int getSocketTimeout() {
        return Integer.parseInt(properties.getProperty("socket.timeout", "30000"));
    }

    public boolean isMetricsEnabled() {
        return Boolean.parseBoolean(properties.getProperty("metrics.enabled", "true"));
    }

    public int getMetricsInterval() {
        return Integer.parseInt(properties.getProperty("metrics.interval", "1000"));
    }

    public String getLogLevel() {
        return properties.getProperty("log.level", "INFO");
    }

    public boolean isBatchingEnabled() {
        return Boolean.parseBoolean(properties.getProperty("batching.enabled", "false"));
    }

    public int getBatchSize() {
        return Integer.parseInt(properties.getProperty("batch.size", "100"));
    }

    public boolean isValidationEnabled() {
        return Boolean.parseBoolean(properties.getProperty("validation.enabled", "false"));
    }
    
    public boolean isServerMode() {
        return Boolean.parseBoolean(properties.getProperty("server.mode", "false"));
    }
    
    public String getOutputDirectory() {
        return properties.getProperty("output.directory", "c:\\share\\output");
    }
    
    public boolean isRecursiveEnabled() {
        return Boolean.parseBoolean(properties.getProperty("recursive.enabled", "true"));
    }
    
    public int getMaxRecursionDepth() {
        return Integer.parseInt(properties.getProperty("recursive.max.depth", "10"));
    }
    
    public List<String> getSupportedFileExtensions() {
        String extensions = properties.getProperty("file.extensions", "txt,gz,fix,log,def");
        return Arrays.asList(extensions.split(","));
    }
    
    public String getDirectoryStreamPattern() {
        List<String> extensions = getSupportedFileExtensions();
        if (extensions.size() == 1) {
            return "*." + extensions.get(0);
        } else {
            return "*.{" + String.join(",", extensions) + "}";
        }
    }
    
    private static void parseCommandLineArgs(String[] args, Properties props) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--file":
                    if (i + 1 < args.length) {
                        props.setProperty("input.file", args[++i]);
                    }
                    break;
                case "--host":
                    if (i + 1 < args.length) {
                        props.setProperty("target.host", args[++i]);
                    }
                    break;
                case "--port":
                    if (i + 1 < args.length) {
                        props.setProperty("target.port", args[++i]);
                    }
                    break;
                case "--rate":
                    if (i + 1 < args.length) {
                        props.setProperty("injection.rate", args[++i]);
                    }
                    break;
                case "--config":
                    if (i + 1 < args.length) {
                        String configFile = args[++i];
                        try (InputStream is = Files.newInputStream(Paths.get(configFile))) {
                            Properties configProps = new Properties();
                            configProps.load(is);
                            configProps.forEach((key, value) -> props.setProperty((String) key, (String) value));
                        } catch (IOException e) {
                            System.err.println("Failed to load config file: " + configFile);
                        }
                    }
                    break;
                case "--server":
                    props.setProperty("server.mode", "true");
                    break;
                case "--recursive":
                    props.setProperty("recursive.enabled", "true");
                    break;
                case "--no-recursive":
                    props.setProperty("recursive.enabled", "false");
                    break;
            }
        }
    }
}