## Project Overview

This is a java console application that reads a large volume of sample FIX messages from disk and injects them into a TCP socket. The goal is to simulate a high-throughput, low-latency environment for benchmarking a Kafka-based FIX message streaming and processing.

## Goals

- Efficiently read and parse FIX messages from file
- Inject messages into a socket with minimal latency
- Support configurable throughput (e.g., messages /sec)
- Log performance metrics (e.g., injection rate, latency)
- Ensure thread-safe, scalable architecture

### General Guidance

- Use Java 21+ with standard libraries (no Spring Boot)
- Prioritize performance and low GC overhead
- Avoid unnecessary object creation
- Use thread pools or async I/O for concurrency

### Core Components

#### FIX Message Reader

- Read messages from a file (e.g., line-delimited FIX strings)
- Support large files (GB-scale) with buffered streaming
- Optionally parse FIX tags for validation or filtering

### Socket Injector

- Connect to a configurable TCP endpoint
- Inject messages with minimal delay
- Support batching or throttling (e.g., 10K msgs/sec)

### Configuration

- Use a properties or YAML file for:
- Input file path
- Target host/port
- Injection rate
- Logging options

### Metrics & Logging

- Track total message sent, throughput, and latency
- Log to console or file user SLF4J + Logback
- Optionally expose metrics via JMX

### Performance considerations

- Use 'ByteBuffer' and 'FileChannel' for efficient I/O
- Minimize synchronization overhead
- Consider using 'Disruptor' or custom ring buffer for ultra-low latency

### Unit Testing

#### Test Frameworks

- JUnit Jupiter 5.10.2 (main testing framework)
- Mockito 5.11.0 (mocking framework)
- Maven Surefire Plugin 3.2.5 (test execution)
- Covers key functionality including edge cases, error handling, and concurrent access
- Tests validate configuration defaults against application.properties

#### Test Quality Features:

- Comprehensive edge case coverage
- Thread safety testing for metrics
- File I/O testing with temporary directories
- Error condition validation
- Mock configurations for isolated testing
- Simulate socket server to verify message delivery
- Measure injection latency under load
- Validate FIX message integrity (TODO)

### Local Integration Testing

#### Test with a dummy socket server locally

Created console-runnable application with socket server. Here's how to use it:

```Console Commands

  # Run as FIX message injector:
  mvn exec:java
  mvn exec:java -Dexec.args="--file c:\share\data\messages.txt" --rate 5000"
  mvn exec:java -Dexec.args="--host localhost --port 9999 --rate 1000"

  # Run as socket server (for testing):
  mvn exec:java -Dexec.args="--server"

  # Build executable JAR:
  mvn clean package
  java -jar target/demo-1.0-SNAPSHOT.jar --help
  java -jar target/demo-1.0-SNAPSHOT.jar --server
  java -jar target/demo-1.0-SNAPSHOT.jar --file c:\share\data\messages.txt

  # Support recursive mode for all files and subdirectories up to 10 levels deep. Non-recursive mode only processes files in the root directory
  java -jar target/demo-1.0-SNAPSHOT.jar --file c:\share\data\ --recursive

```

#### Test Results

1. With console printing of debugging messages
   Socket connection closed
   Performance metrics stopped at: 2025-08-12T21:23:43.607400700Z
   Metrics - Messages: 4250 (375/s current, 688/s avg) | Latency: avg=0.18ms, min=0.01ms, max=14.06ms | Data: 0.00 MB (0.00 MB/s) | Errors: 0 | Runtime: 0:00:06
   Metrics - Messages: 4250 (0/s current, 592/s avg) | Latency: avg=0.18ms, min=0.01ms, max=14.06ms | Data: 0.00 MB (0.00 MB/s) | Errors: 0 | Runtime: 0:00:07
   Metrics - Messages: 4250 (0/s current, 519/s avg) | Latency: avg=0.18ms, min=0.01ms, max=14.06ms | Data: 0.00 MB (0.00 MB/s) | Errors: 0 | Runtime: 0:00:08
   Metrics - Messages: 4250 (0/s current, 462/s avg) | Latency: avg=0.18ms, min=0.01ms, max=14.06ms | Data: 0.00 MB (0.00 MB/s) | Errors: 0 | Runtime: 0:00:09
   Metrics - Messages: 4250 (0/s current, 416/s avg) | Latency: avg=0.18ms, min=0.01ms, max=14.06ms | Data: 0.00 MB (0.00 MB/s) | Errors: 0 | Runtime: 0:00:10
   Injection completed.

=== FINAL PERFORMANCE REPORT ===
Total runtime: 10.451 sec
Messages processed: 4250
Data processed: 0.00 MB
Average throughput: 406.66 messages/sec
Average data rate: 0.00 MB/sec
Latency statistics:

- Average: 0.18 ms
- Minimum: 0.01 ms
- Maximum: 14.06 ms
  Total errors: 0
  Success rate: 100.00%

Socket connection closed
Performance metrics stopped at: 2025-08-12T21:28:34.087253900Z
Injection completed.

1. Console printing of debug info commented out.
   === FINAL PERFORMANCE REPORT ===
   Total runtime: 5.539 sec
   Messages processed: 4250
   Data processed: 0.00 MB
   Average throughput: 767.29 messages/sec
   Average data rate: 0.00 MB/sec
   Latency statistics:

- Average: 0.03 ms
- Minimum: 0.01 ms
- Maximum: 1.48 ms, Total errors: 0, Success rate: 100.00%
