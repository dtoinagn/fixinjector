# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Java console application that reads FIX messages from files and injects them into TCP sockets for high-throughput, low-latency benchmarking of Kafka-based FIX message streaming. The application uses Java 17 with Maven for build management.

## Common Commands

### Build and Run
```bash
# Compile and package the application
mvn clean compile
mvn clean package

# Run as FIX message injector (default mode)
mvn exec:java
mvn exec:java -Dexec.args="--file c:\share\data\messages.txt --rate 5000"
mvn exec:java -Dexec.args="--host localhost --port 9999 --rate 1000"

# Run as socket server (for testing)
mvn exec:java -Dexec.args="--server"

# Run executable JAR
java -jar target/fixinjector-1.0-SNAPSHOT.jar --help
java -jar target/fixinjector-1.0-SNAPSHOT.jar --server
java -jar target/fixinjector-1.0-SNAPSHOT.jar --file c:\share\data\messages.txt
java -jar target/fixinjector-1.0-SNAPSHOT.jar --file c:\share\data\ --recursive
```

### Testing
The project includes a built-in socket server for testing. Run the server in one terminal and the injector in another to test message delivery and measure performance.

## Architecture

### Core Components

- **FixMessageInjector** (`src/main/java/com/dtian/fixinjector/FixMessageInjector.java`) - Main application class and entry point
- **FixMessageReader** - Handles reading FIX messages from files/directories with support for various formats (.txt, .gz, .fix, .log)
- **SocketInjector** - Manages TCP socket connections and message injection with throttling
- **FixMessageServer** - Built-in test server for validating message delivery
- **Configuration** - Handles command-line arguments and application.properties configuration
- **PerformanceMetrics** - Real-time performance monitoring and reporting

### Configuration

The application uses `src/main/resources/application.properties` for default settings and supports command-line overrides. Key configuration areas:

- Input file/directory paths with recursive processing support
- Target socket host/port configuration  
- Injection rate throttling and performance tuning
- Metrics collection and reporting intervals
- File format support (.txt, .gz, .fix, .log, .def)

### Performance Design

The application is optimized for high-throughput, low-latency message injection:
- Uses thread pools and async I/O for concurrency
- Implements configurable message batching
- Minimizes object creation and GC overhead
- Supports file streaming for large datasets (GB-scale)
- Real-time performance metrics with sub-millisecond latency tracking

### Package Structure

All source code is under `com.dtian.fixinjector` package with main classes in `src/main/java/com/dtian/fixinjector/`.