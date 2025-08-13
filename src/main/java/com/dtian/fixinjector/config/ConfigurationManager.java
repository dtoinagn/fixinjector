package com.dtian.fixinjector.config;

import com.dtian.fixinjector.factory.MessageProtocolFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Manages application configuration from properties files and command line arguments.
 * Implements the InjectorConfig interface.
 */
public class ConfigurationManager implements InjectorConfig {
    private static final String DEFAULT_CONFIG_FILE = "application.properties";
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 9999;
    private static final int DEFAULT_INJECTION_RATE = 1000;
    private static final String DEFAULT_INPUT_FILE = "c:\\share\\data\\fix-messages.txt";
    private static final String DEFAULT_MESSAGE_PROTOCOL = "FIX";

    private final Properties properties;

    private ConfigurationManager(Properties properties) {
        this.properties = properties;
        validateConfiguration();
    }

    public static ConfigurationManager load() throws IOException {
        return load(new String[0]);
    }
    
    public static ConfigurationManager load(String[] args) throws IOException {
        Properties props = new Properties();
        
        // Load from file first
        loadPropertiesFile(props);
        
        // Override with command line arguments
        parseCommandLineArgs(args, props);
        
        return new ConfigurationManager(props);
    }

    private static void loadPropertiesFile(Properties props) throws IOException {
        Path configPath = Paths.get(DEFAULT_CONFIG_FILE);
        if (Files.exists(configPath)) {
            try (InputStream is = Files.newInputStream(configPath)) {
                props.load(is);
            }
        } else {
            try (InputStream is = ConfigurationManager.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILE)) {
                if (is != null) {
                    props.load(is);
                }
            }
        }
    }

    private void validateConfiguration() {
        // Validate protocol
        String protocol = getMessageProtocol();
        if (!MessageProtocolFactory.isProtocolSupported(protocol)) {
            throw new IllegalArgumentException("Unsupported protocol: " + protocol + 
                ". Supported protocols: " + Arrays.toString(MessageProtocolFactory.getAvailableProtocols()));
        }

        // Validate rates and sizes
        if (getInjectionRate() < 0) {
            throw new IllegalArgumentException("Injection rate cannot be negative");
        }
        
        if (getBufferSize() <= 0) {
            throw new IllegalArgumentException("Buffer size must be positive");
        }
        
        if (getHeaderLength() < 0) {
            throw new IllegalArgumentException("Header length cannot be negative");
        }
    }

    // File configuration
    @Override
    public String getInputFile() {
        return properties.getProperty("input.file", DEFAULT_INPUT_FILE);
    }

    @Override
    public List<String> getSupportedFileExtensions() {
        String extensions = properties.getProperty("file.extensions", "txt,gz,fix,log,def,xml,bin,dat");
        return Arrays.asList(extensions.split(","));
    }

    @Override
    public boolean isRecursiveEnabled() {
        return Boolean.parseBoolean(properties.getProperty("recursive.enabled", "true"));
    }

    @Override
    public int getMaxRecursionDepth() {
        return Integer.parseInt(properties.getProperty("recursive.max.depth", "10"));
    }

    // Network configuration
    @Override
    public String getHost() {
        return properties.getProperty("target.host", DEFAULT_HOST);
    }

    @Override
    public int getPort() {
        return Integer.parseInt(properties.getProperty("target.port", String.valueOf(DEFAULT_PORT)));
    }

    @Override
    public int getSocketTimeout() {
        return Integer.parseInt(properties.getProperty("socket.timeout", "30000"));
    }

    // Performance configuration
    @Override
    public int getInjectionRate() {
        return Integer.parseInt(properties.getProperty("injection.rate", String.valueOf(DEFAULT_INJECTION_RATE)));
    }

    @Override
    public int getBufferSize() {
        return Integer.parseInt(properties.getProperty("buffer.size", "8192"));
    }

    @Override
    public boolean isBatchingEnabled() {
        return Boolean.parseBoolean(properties.getProperty("batching.enabled", "false"));
    }

    @Override
    public int getBatchSize() {
        return Integer.parseInt(properties.getProperty("batch.size", "100"));
    }

    // Protocol configuration
    @Override
    public String getMessageProtocol() {
        return properties.getProperty("message.protocol", DEFAULT_MESSAGE_PROTOCOL);
    }

    @Override
    public int getHeaderLength() {
        return Integer.parseInt(properties.getProperty("header.length", "8"));
    }

    // Application configuration
    @Override
    public boolean isValidationEnabled() {
        return Boolean.parseBoolean(properties.getProperty("validation.enabled", "false"));
    }

    @Override
    public boolean isMetricsEnabled() {
        return Boolean.parseBoolean(properties.getProperty("metrics.enabled", "true"));
    }

    @Override
    public int getMetricsInterval() {
        return Integer.parseInt(properties.getProperty("metrics.interval", "1000"));
    }

    @Override
    public boolean isServerMode() {
        return Boolean.parseBoolean(properties.getProperty("server.mode", "false"));
    }

    @Override
    public String getOutputDirectory() {
        return properties.getProperty("output.directory", "c:\\share\\output");
    }

    @Override
    public String getLogLevel() {
        return properties.getProperty("log.level", "INFO");
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
                case "--protocol":
                    if (i + 1 < args.length) {
                        props.setProperty("message.protocol", args[++i]);
                    }
                    break;
                case "--header-length":
                    if (i + 1 < args.length) {
                        props.setProperty("header.length", args[++i]);
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