package com.github.chaudhryfaisal.tinylog;

import com.github.chaudhryfaisal.Humio;
import com.github.chaudhryfaisal.batch.BatchProcessor;
import com.github.chaudhryfaisal.dto.Event;
import com.github.chaudhryfaisal.dto.EventPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.tinylog.core.LogEntry;
import org.tinylog.core.LogEntryValue;
import org.tinylog.writers.Writer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A tinylog {@link org.tinylog.writers.Writer} writing log messages to a humio server
 */

@Builder
@AllArgsConstructor
@NoArgsConstructor
public final class HumioWriter implements Writer {
    private static final String FIELD_SEPARATOR = ",";
    private static final String FIELD_VALUE_SEPARATOR = ":";
    private static final EnumSet<LogEntryValue> BASIC_LOG_ENTRY_VALUES = EnumSet.of(
            LogEntryValue.DATE,
            LogEntryValue.LEVEL,
            LogEntryValue.MESSAGE
    );

    private BatchProcessor<EventPayload, Event> processor;
    private String hostname;
    private Set<LogEntryValue> requiredLogEntryValues;
    private Map<String, Object> staticFields;

    private int batchActions = BatchProcessor.DEFAULT_ACTION_SIZE;
    private int batchFlushInterval = BatchProcessor.DEFAULT_FLUSH_INTERVAL;
    private int batchBufferLimit = BatchProcessor.DEFAULT_BATCH_SIZE;
    private int batchJitterInterval = BatchProcessor.DEFAULT_JITTER_INTERVAL;
    private String serverEndpoint = Humio.DEFAULT_HOST_NAME;
    private String serverUri = Humio.DEFAULT_PATH;
    private String serverToken;


    public static String prop(String key, String _def) {
        String ret = System.getProperty(key, _def);
        if (System.getenv(key) != null) {
            ret = System.getenv(key);
        }
        return ret;
    }

    public HumioWriter(Map<String, String> p) {
        hostname = buildHostName(p.getOrDefault("hostname", prop("HUMIO_HOSTNAME", "")));
        requiredLogEntryValues = buildLogEntryValuesFromString(p.getOrDefault("additionalLogEntryValues", "EXCEPTION"));
        staticFields = buildStaticFields(p.getOrDefault("staticFields", ""));

        batchActions = Integer.parseInt(p.getOrDefault("batchActions", String.valueOf(batchActions)));
        batchFlushInterval = Integer.parseInt(p.getOrDefault("batchFlushInterval", String.valueOf(batchFlushInterval)));
        batchBufferLimit = Integer.parseInt(p.getOrDefault("batchBufferLimit", String.valueOf(batchBufferLimit)));
        batchJitterInterval = Integer.parseInt(p.getOrDefault("batchJitterInterval", String.valueOf(batchJitterInterval)));
        serverEndpoint = p.getOrDefault("serverEndpoint", prop("HUMIO_SERVER_ENDPOINT", serverEndpoint));
        serverToken = p.getOrDefault("serverToken", prop("HUMIO_SERVER_TOKEN", ""));
        serverUri = p.getOrDefault("uri", serverUri);

        if (serverToken.length() == 0) {
            throw new RuntimeException("serverToken is required");
        }
    }

    private Map<String, Object> buildStaticFields(String fields) {
        return Arrays.stream(fields.split(FIELD_SEPARATOR)).map(s -> s.split(FIELD_VALUE_SEPARATOR)).collect(Collectors.toMap(s -> s[0], s -> s[1]));
    }

    private static EnumSet<LogEntryValue> buildLogEntryValuesFromString(String logEntryValues) {
        final EnumSet<LogEntryValue> result = EnumSet.noneOf(LogEntryValue.class);
        for (String logEntryValue : logEntryValues.split(FIELD_SEPARATOR)) {
            result.add(LogEntryValue.valueOf(logEntryValue));
        }
        return result;
    }

    private String buildHostName(final String hostname) {
        if (null == hostname || hostname.isEmpty()) {
            try {
                return InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                return "localhost";
            }
        }
        return hostname;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<LogEntryValue> getRequiredLogEntryValues() {
        return requiredLogEntryValues;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final LogEntry logEntry) {
        Event.EventBuilder builder = Event.builder();
        builder.timestamp(logEntry.getTimestamp().toInstant().toEpochMilli())
                .attribute("message", logEntry.getMessage())
                .attribute("hostname", hostname)
                .attribute("level", logEntry.getLevel());


        final Thread thread = logEntry.getThread();
        if (null != thread) {
            builder.attribute("threadName", thread.getName())
                    .attribute("threadGroup", thread.getThreadGroup().getName())
                    .attribute("threadPriority", thread.getPriority());
        }

        final String className = logEntry.getClassName();
        if (null != className) {
            builder.attribute("sourceClassName", className);
        }

        final String methodName = logEntry.getMethodName();
        if (null != methodName && !"<unknown>".equals(methodName)) {
            builder.attribute("sourceMethodName", methodName);
        }

        final String fileName = logEntry.getFileName();
        if (null != fileName) {
            builder.attribute("sourceFileName", fileName);
        }

        final int lineNumber = logEntry.getLineNumber();
        if (lineNumber != -1) {
            builder.attribute("sourceLineNumber", lineNumber);
        }

        @SuppressWarnings("all") final Throwable throwable = logEntry.getException();
        if (null != throwable) {
            final StringBuilder stackTraceBuilder = new StringBuilder();
            for (StackTraceElement stackTraceElement : throwable.getStackTrace()) {
                new Formatter(stackTraceBuilder).format("%s.%s(%s:%d)%n",
                        stackTraceElement.getClassName(), stackTraceElement.getMethodName(),
                        stackTraceElement.getFileName(), stackTraceElement.getLineNumber());
            }
            builder.attribute("exceptionClass", throwable.getClass().getCanonicalName());
            builder.attribute("exceptionMessage", throwable.getMessage());
            builder.attribute("exceptionStackTrace", stackTraceBuilder.toString());
        }
        getProcessor().add(builder.build());
    }

    private synchronized BatchProcessor<EventPayload, Event> getProcessor() {
        if (processor == null) {
            processor = BatchProcessor.<EventPayload, Event>builder()
                    .actions(batchActions)
                    .flushInterval(batchFlushInterval)
                    .bufferLimit(batchBufferLimit)
                    .jitterInterval(batchJitterInterval)
                    .sink(new HumioSink(Humio.builder()
                            .endpoint(serverEndpoint)
                            .token(serverToken)
                            .uri(serverUri)
                            .tags(staticFields).build()))
                    .build();
        }
        return processor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() {
        if (processor != null) {
            processor.flush();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
//        VMShutdownHook.unregister(this);
        if (processor != null) {
            processor.close();
        }
    }
}
