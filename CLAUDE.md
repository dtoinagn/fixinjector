# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Java console application that reads messages from files and injects them into TCP sockets for high-throughput, low-latency benchmarking. The application supports multiple message protocols (FIX, BYTE_HEADER_XML) and uses Java 17 with Maven for build management.

## Common Commands

### Build and Run
```bash
# Compile and package the application
mvn clean compile
mvn clean package

# Run message injector (default FIX protocol)
mvn exec:java
mvn exec:java -Dexec.args="--file c:\share\data\messages.txt --rate 5000"
mvn exec:java -Dexec.args="--host localhost --port 9999 --rate 1000"

# Run with different protocols
mvn exec:java -Dexec.args="--protocol BYTE_HEADER_XML --header-length 16"
mvn exec:java -Dexec.args="--protocol FIX --file c:\share\data\fix-messages.txt"

# Run as socket server (for testing)
mvn exec:java -Dexec.args="--server"

# Run executable JAR
java -jar target/fixinjector-1.0-SNAPSHOT.jar --help
java -jar target/fixinjector-1.0-SNAPSHOT.jar --server
java -jar target/fixinjector-1.0-SNAPSHOT.jar --protocol BYTE_HEADER_XML
```

### Testing
The project includes a built-in socket server for testing. Run the server in one terminal and the injector in another to test message delivery and measure performance.

## Architecture

### Package Structure

The codebase is organized into logical packages:

- **`com.dtian.fixinjector`** - Main application and legacy server
- **`com.dtian.fixinjector.config`** - Configuration management
- **`com.dtian.fixinjector.core`** - Core processing components
- **`com.dtian.fixinjector.factory`** - Factory classes for protocol creation
- **`com.dtian.fixinjector.metrics`** - Performance monitoring
- **`com.dtian.fixinjector.model`** - Message model classes
- **`com.dtian.fixinjector.protocol`** - Protocol abstractions and implementations
- **`com.dtian.fixinjector.util`** - Utility classes

### Core Components

- **Application** - Main application class and entry point
- **MessageReader** - Universal message reader supporting multiple protocols
- **MessageInjector** - Handles TCP socket connections and message injection
- **MessageProcessor** - Coordinates message processing with rate limiting
- **PerformanceMetrics** - Real-time performance monitoring and reporting
- **ConfigurationManager** - Handles configuration from files and command-line

### Protocol Support

The application uses a plugin-like architecture for message protocols:

- **MessageProtocol** interface - Defines protocol contract
- **FixProtocol** - Implementation for FIX messages
- **ByteHeaderXmlProtocol** - Implementation for binary header + XML payload
- **MessageProtocolFactory** - Creates protocol instances using registry pattern

### Configuration

Uses `src/main/resources/application.properties` for defaults with command-line overrides:

- **Protocol settings**: `message.protocol`, `header.length`
- **File processing**: `input.file`, `recursive.enabled`, `file.extensions`
- **Network settings**: `target.host`, `target.port`, `socket.timeout`
- **Performance**: `injection.rate`, `batching.enabled`, `buffer.size`
- **Monitoring**: `metrics.enabled`, `metrics.interval`

### Extensibility

To add a new message protocol:
1. Implement `MessageProtocol` interface
2. Create message model class extending `Message`
3. Register protocol in `MessageProtocolFactory`
4. Add protocol-specific configuration properties

### Performance Design

- Plugin architecture with minimal overhead
- Protocol-specific optimizations
- Thread pools and async I/O for concurrency
- Configurable message batching
- Real-time metrics with sub-millisecond latency tracking
- Support for GB-scale file processing