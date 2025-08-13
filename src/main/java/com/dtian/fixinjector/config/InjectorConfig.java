package com.dtian.fixinjector.config;

import java.util.List;

/**
 * Interface for injector configuration.
 * Provides a contract for configuration implementations.
 */
public interface InjectorConfig {
    
    // File configuration
    String getInputFile();
    List<String> getSupportedFileExtensions();
    boolean isRecursiveEnabled();
    int getMaxRecursionDepth();
    
    // Network configuration
    String getHost();
    int getPort();
    int getSocketTimeout();
    
    // Performance configuration
    int getInjectionRate();
    int getBufferSize();
    boolean isBatchingEnabled();
    int getBatchSize();
    
    // Protocol configuration
    String getMessageProtocol();
    int getHeaderLength();
    
    // Application configuration
    boolean isValidationEnabled();
    boolean isMetricsEnabled();
    int getMetricsInterval();
    boolean isServerMode();
    String getOutputDirectory();
    String getLogLevel();
}