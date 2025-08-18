package com.dtian.fixinjector.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class ConfigurationManagerTest {
    
    @TempDir
    Path tempDir;
    
    private Path createConfigFile(String content) throws IOException {
        Path configFile = tempDir.resolve("test-config.properties");
        Files.write(configFile, content.getBytes());
        return configFile;
    }
    
    @Test
    @DisplayName("Should load configuration with default values")
    public void shouldLoadConfigurationWithDefaults() throws IOException {
        ConfigurationManager config = ConfigurationManager.load();
        
        assertNotNull(config);
        assertEquals("localhost", config.getHost());
        assertEquals(9999, config.getPort());
        assertEquals(1000, config.getInjectionRate());
        assertEquals("FIX", config.getMessageProtocol());
        assertEquals(8, config.getHeaderLength());
        assertTrue(config.isMetricsEnabled());
        assertFalse(config.isServerMode());
    }
    
    @Test
    @DisplayName("Should parse command line arguments correctly")
    public void shouldParseCommandLineArgumentsCorrectly() throws IOException {
        String[] args = {
            "--host", "test.example.com",
            "--port", "8888",
            "--rate", "5000",
            "--protocol", "BYTE_HEADER_XML",
            "--header-length", "16",
            "--file", "test-messages.txt",
            "--server"
        };
        
        ConfigurationManager config = ConfigurationManager.load(args);
        
        assertEquals("test.example.com", config.getHost());
        assertEquals(8888, config.getPort());
        assertEquals(5000, config.getInjectionRate());
        assertEquals("BYTE_HEADER_XML", config.getMessageProtocol());
        assertEquals(16, config.getHeaderLength());
        assertEquals("test-messages.txt", config.getInputFile());
        assertTrue(config.isServerMode());
    }
    
    @Test
    @DisplayName("Should handle recursive flags correctly")
    public void shouldHandleRecursiveFlagsCorrectly() throws IOException {
        String[] argsRecursive = {"--recursive"};
        String[] argsNoRecursive = {"--no-recursive"};
        
        ConfigurationManager configRecursive = ConfigurationManager.load(argsRecursive);
        ConfigurationManager configNoRecursive = ConfigurationManager.load(argsNoRecursive);
        
        assertTrue(configRecursive.isRecursiveEnabled());
        assertFalse(configNoRecursive.isRecursiveEnabled());
    }
    
    @Test
    @DisplayName("Should get supported file extensions as list")
    public void shouldGetSupportedFileExtensionsAsList() throws IOException {
        ConfigurationManager config = ConfigurationManager.load();
        var extensions = config.getSupportedFileExtensions();
        
        assertNotNull(extensions);
        assertTrue(extensions.contains("txt"));
        assertTrue(extensions.contains("fix"));
        assertTrue(extensions.contains("gz")); // xml is not in default properties
    }
    
    @Test
    @DisplayName("Should validate injection rate")
    public void shouldValidateInjectionRate() {
        String[] negativeRateArgs = {"--rate", "-100"};
        
        assertThrows(IllegalArgumentException.class, () -> {
            ConfigurationManager.load(negativeRateArgs);
        });
    }
    
    @Test
    @DisplayName("Should validate protocol support")
    public void shouldValidateProtocolSupport() {
        String[] invalidProtocolArgs = {"--protocol", "INVALID_PROTOCOL"};
        
        assertThrows(IllegalArgumentException.class, () -> {
            ConfigurationManager.load(invalidProtocolArgs);
        });
    }
    
    @Test
    @DisplayName("Should validate header length")
    public void shouldValidateHeaderLength() {
        String[] negativeHeaderArgs = {"--header-length", "-5"};
        
        assertThrows(IllegalArgumentException.class, () -> {
            ConfigurationManager.load(negativeHeaderArgs);
        });
    }
    
    @Test
    @DisplayName("Should get default network configuration")
    public void shouldGetDefaultNetworkConfiguration() throws IOException {
        ConfigurationManager config = ConfigurationManager.load();
        
        assertEquals("localhost", config.getHost());
        assertEquals(9999, config.getPort());
        assertEquals(30000, config.getSocketTimeout());
    }
    
    @Test
    @DisplayName("Should get default performance configuration")
    public void shouldGetDefaultPerformanceConfiguration() throws IOException {
        ConfigurationManager config = ConfigurationManager.load();
        
        assertEquals(1000, config.getInjectionRate());
        assertEquals(8192, config.getBufferSize());
        assertTrue(config.isBatchingEnabled()); // Default is true in properties
        assertEquals(100, config.getBatchSize());
    }
    
    @Test
    @DisplayName("Should get default application configuration")
    public void shouldGetDefaultApplicationConfiguration() throws IOException {
        ConfigurationManager config = ConfigurationManager.load();
        
        assertFalse(config.isValidationEnabled());
        assertTrue(config.isMetricsEnabled());
        assertEquals(1000, config.getMetricsInterval());
        assertFalse(config.isServerMode());
        assertEquals("INFO", config.getLogLevel());
    }
    
    @Test
    @DisplayName("Should get file configuration")
    public void shouldGetFileConfiguration() throws IOException {
        ConfigurationManager config = ConfigurationManager.load();
        
        assertTrue(config.isRecursiveEnabled());
        assertEquals(10, config.getMaxRecursionDepth());
        assertNotNull(config.getSupportedFileExtensions());
        assertTrue(config.getSupportedFileExtensions().size() > 0);
    }
    
    @Test
    @DisplayName("Should handle incomplete command line arguments gracefully")
    public void shouldHandleIncompleteCommandLineArgumentsGracefully() throws IOException {
        String[] incompleteArgs = {"--host"}; // Missing value
        
        // Should not throw exception, should use default
        ConfigurationManager config = ConfigurationManager.load(incompleteArgs);
        assertEquals("localhost", config.getHost()); // Should fall back to default
    }
    
    @Test
    @DisplayName("Should override defaults with command line args")
    public void shouldOverrideDefaultsWithCommandLineArgs() throws IOException {
        String[] args = {
            "--host", "custom.host",
            "--port", "7777",
            "--rate", "2000"
        };
        
        ConfigurationManager config = ConfigurationManager.load(args);
        
        assertEquals("custom.host", config.getHost());
        assertEquals(7777, config.getPort());
        assertEquals(2000, config.getInjectionRate());
        
        // Other values should remain defaults
        assertEquals("FIX", config.getMessageProtocol());
        assertEquals(8, config.getHeaderLength());
    }
    
    @Test
    @DisplayName("Should get output directory configuration")
    public void shouldGetOutputDirectoryConfiguration() throws IOException {
        ConfigurationManager config = ConfigurationManager.load();
        
        assertNotNull(config.getOutputDirectory());
        assertEquals("c:\\share\\output", config.getOutputDirectory());
    }
    
    @Test
    @DisplayName("Should handle boolean configurations correctly")
    public void shouldHandleBooleanConfigurationsCorrectly() throws IOException {
        ConfigurationManager config = ConfigurationManager.load();
        
        // Test default boolean values
        assertTrue(config.isRecursiveEnabled());
        assertTrue(config.isMetricsEnabled());
        assertFalse(config.isValidationEnabled());
        assertTrue(config.isBatchingEnabled()); // Default is true in properties
        assertFalse(config.isServerMode());
    }
    
    @Test
    @DisplayName("Should handle integer configurations correctly")
    public void shouldHandleIntegerConfigurationsCorrectly() throws IOException {
        ConfigurationManager config = ConfigurationManager.load();
        
        // Test integer parsing
        assertTrue(config.getPort() > 0);
        assertTrue(config.getInjectionRate() > 0);
        assertTrue(config.getBufferSize() > 0);
        assertTrue(config.getSocketTimeout() > 0);
        assertTrue(config.getMetricsInterval() > 0);
    }
}