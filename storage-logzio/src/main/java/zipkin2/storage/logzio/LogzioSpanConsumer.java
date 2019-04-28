package zipkin2.storage.logzio;

import com.squareup.moshi.JsonWriter;
import io.logz.sender.LogzioSender;
import okio.Buffer;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zipkin2.Annotation;
import zipkin2.Call;
import zipkin2.Callback;
import zipkin2.Span;
import zipkin2.codec.SpanBytesEncoder;
import zipkin2.storage.SpanConsumer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class LogzioSpanConsumer implements SpanConsumer {

    private static final int INDEX_CHARS_LIMIT = 256;
    private volatile LogzioSender logzioSender;
    private boolean closeCalled;
    private final ByteString EMPTY_JSON = ByteString.of(new byte[]{'{', '}'});
    private static final Logger logger = LoggerFactory.getLogger(LogzioStorage.class);

    public LogzioSpanConsumer(ConsumerParams params) {
        if (logzioSender == null && !params.getToken().isEmpty()) {
            synchronized (this) {
                if (logzioSender == null) {
                    logzioSender = params.getLogzioSender();
                    logzioSender.start();
                }
            }
        }
    }

    public Call<Void> accept(List<Span> spans) {
        if (closeCalled) throw new IllegalStateException("closed");
        if (spans.size() == 0) {
            return Call.create(null);
        }
        byte[] message = new byte[0];
        try {
            message = spansToJsonBytes(spans);
        } catch (IOException e) {
            logger.error("error converting spans to byte array: {}", e.getMessage());
        }
        return new LogzioCall(message);
    }

    private LogzioSender getSender() {
        return logzioSender;
    }

    public void close() {
        if (closeCalled) {
            return;
        }
        logzioSender.stop();
        this.closeCalled = true;
    }

    private byte[] spansToJsonBytes(List<Span> spans) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            for (Span span : spans) {
                long spanTimestamp = span.timestampAsLong();
                byte[] spanBytes = getSpanBytes(span, spanTimestamp);
                bos.write(spanBytes);
                bos.write("\n".getBytes(StandardCharsets.UTF_8));
            }
            return bos.toByteArray();
        }
    }

    private byte[] getSpanBytes(Span span, long timestampMillis) {
        return prefixWithTimestampMillisAndQuery(span, timestampMillis);
    }

    private byte[] prefixWithTimestampMillisAndQuery(Span span, long timestampMillis) {
        Buffer prefix = new Buffer();
        JsonWriter writer = JsonWriter.of(prefix);
        try {
            writer.beginObject();

            if (timestampMillis != 0L) writer.name(LogzioStorage.JSON_TIMESTAMP_MILLIS_FIELD).value(timestampMillis);
            if (!span.tags().isEmpty() || !span.annotations().isEmpty()) {
                writer.name(LogzioStorage.JSON_ANNOTATION);
                writer.beginArray();
                for (Annotation a : span.annotations()) {
                    if (a.value().length() > INDEX_CHARS_LIMIT) continue;
                    writer.value(a.value());
                }
                for (Map.Entry<String, String> tag : span.tags().entrySet()) {
                    int length = tag.getKey().length() + tag.getValue().length() + 1;
                    if (length > INDEX_CHARS_LIMIT) continue;
                    writer.value(tag.getKey()); // search is possible by key alone
                    writer.value(tag.getKey() + "=" + tag.getValue());
                }
                writer.endArray();
            }
            writer.endObject();
        } catch (IOException e) {
            // very unexpected to have an IOE for an in-memory write
            logger.error("Error indexing query for span: " + span);

            return SpanBytesEncoder.JSON_V2.encode(span);
        }
        byte[] document = SpanBytesEncoder.JSON_V2.encode(span);
        if (prefix.rangeEquals(0L, EMPTY_JSON)) return document;
        return mergeJson(prefix.readByteArray(), document);
    }

    private byte[] mergeJson(byte[] prefix, byte[] suffix) {
        byte[] newSpanBytes = new byte[prefix.length + suffix.length - 1];
        int pos = 0;
        System.arraycopy(prefix, 0, newSpanBytes, pos, prefix.length);
        pos += prefix.length;
        newSpanBytes[pos - 1] = ',';
        // starting at position 1 discards the old head of '{'
        System.arraycopy(suffix, 1, newSpanBytes, pos, suffix.length - 1);
        return newSpanBytes;
    }

    class LogzioCall extends Call.Base<Void> {
        byte[] message;

        public LogzioCall(byte[] message) {
            this.message = message;
        }

        @Override
        protected Void doExecute() {
            getSender().send(message);
            getSender().drainQueueAndSend();
            return null;
        }

        @Override
        protected void doEnqueue(Callback<Void> callback) {
            getSender().send(message);
            callback.onSuccess(null);
        }

        @Override
        public Call<Void> clone() {
            return new LogzioCall(message.clone());
        }
    }

}
