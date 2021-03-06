/*
 * Copyright 2015-2019 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package zipkin2.storage.logzio.json;

import java.io.IOException;
import okio.Buffer;
import org.junit.Test;
import zipkin2.DependencyLink;
import zipkin2.Endpoint;
import zipkin2.codec.DependencyLinkBytesEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static zipkin2.storage.logzio.json.JsonAdapters.SPAN_ADAPTER;

public class JsonAdaptersTest {
  @Test
  public void span_ignoreNull_parentId() throws IOException {
    String json =
        "{\n"
            + "  \"traceId\": \"6b221d5bc9e6496c\",\n"
            + "  \"name\": \"get-traces\",\n"
            + "  \"id\": \"6b221d5bc9e6496c\",\n"
            + "  \"parentId\": null\n"
            + "}";

    SPAN_ADAPTER.fromJson(new Buffer().writeUtf8(json));
  }

  @Test
  public void span_ignoreNull_timestamp() throws IOException {
    String json =
        "{\n"
            + "  \"traceId\": \"6b221d5bc9e6496c\",\n"
            + "  \"name\": \"get-traces\",\n"
            + "  \"id\": \"6b221d5bc9e6496c\",\n"
            + "  \"timestamp\": null\n"
            + "}";

    SPAN_ADAPTER.fromJson(new Buffer().writeUtf8(json));
  }

  @Test
  public void span_ignoreNull_duration() throws IOException {
    String json =
        "{\n"
            + "  \"traceId\": \"6b221d5bc9e6496c\",\n"
            + "  \"name\": \"get-traces\",\n"
            + "  \"id\": \"6b221d5bc9e6496c\",\n"
            + "  \"duration\": null\n"
            + "}";

    SPAN_ADAPTER.fromJson(new Buffer().writeUtf8(json));
  }

  @Test
  public void span_ignoreNull_debug() throws IOException {
    String json =
        "{\n"
            + "  \"traceId\": \"6b221d5bc9e6496c\",\n"
            + "  \"name\": \"get-traces\",\n"
            + "  \"id\": \"6b221d5bc9e6496c\",\n"
            + "  \"debug\": null\n"
            + "}";

    SPAN_ADAPTER.fromJson(new Buffer().writeUtf8(json));
  }

  @Test
  public void span_ignoreNull_annotation_endpoint() throws IOException {
    String json =
        "{\n"
            + "  \"traceId\": \"6b221d5bc9e6496c\",\n"
            + "  \"name\": \"get-traces\",\n"
            + "  \"id\": \"6b221d5bc9e6496c\",\n"
            + "  \"annotations\": [\n"
            + "    {\n"
            + "      \"timestamp\": 1461750491274000,\n"
            + "      \"value\": \"cs\",\n"
            + "      \"endpoint\": null\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    SPAN_ADAPTER.fromJson(new Buffer().writeUtf8(json));
  }

  @Test
  public void span_endpointHighPort() throws IOException {
    String json =
        "{\n"
            + "  \"traceId\": \"6b221d5bc9e6496c\",\n"
            + "  \"name\": \"get-traces\",\n"
            + "  \"id\": \"6b221d5bc9e6496c\",\n"
            + "  \"localEndpoint\": {\n"
            + "    \"serviceName\": \"service\",\n"
            + "    \"port\": 65535\n"
            + "  }\n"
            + "}";

    assertThat(SPAN_ADAPTER.fromJson(json).localEndpoint())
        .isEqualTo(Endpoint.newBuilder().serviceName("service").port(65535).build());
  }

  @Test
  public void span_noServiceName() throws IOException {
    String json =
        "{\n"
            + "  \"traceId\": \"6b221d5bc9e6496c\",\n"
            + "  \"name\": \"get-traces\",\n"
            + "  \"id\": \"6b221d5bc9e6496c\",\n"
            + "  \"localEndpoint\": {\n"
            + "    \"port\": 65535\n"
            + "  }\n"
            + "}";

    assertThat(SPAN_ADAPTER.fromJson(json).localEndpoint())
        .isEqualTo(Endpoint.newBuilder().serviceName("").port(65535).build());
  }

  @Test
  public void span_nullServiceName() throws IOException {
    String json =
        "{\n"
            + "  \"traceId\": \"6b221d5bc9e6496c\",\n"
            + "  \"name\": \"get-traces\",\n"
            + "  \"id\": \"6b221d5bc9e6496c\",\n"
            + "  \"localEndpoint\": {\n"
            + "    \"serviceName\": NULL,\n"
            + "    \"port\": 65535\n"
            + "  }\n"
            + "}";

    assertThat(SPAN_ADAPTER.fromJson(json).localEndpoint())
        .isEqualTo(Endpoint.newBuilder().serviceName("").port(65535).build());
  }

  @Test
  public void span_readsTraceIdHighFromTraceIdField() throws IOException {
    String with128BitTraceId =
        ("{\n"
            + "  \"traceId\": \"48485a3953bb61246b221d5bc9e6496c\",\n"
            + "  \"name\": \"get-traces\",\n"
            + "  \"id\": \"6b221d5bc9e6496c\"\n"
            + "}");
    String withLower64bitsTraceId =
        ("{\n"
            + "  \"traceId\": \"6b221d5bc9e6496c\",\n"
            + "  \"name\": \"get-traces\",\n"
            + "  \"id\": \"6b221d5bc9e6496c\"\n"
            + "}");

    assertThat(SPAN_ADAPTER.fromJson(with128BitTraceId))
        .isEqualTo(
            SPAN_ADAPTER
                .fromJson(withLower64bitsTraceId)
                .toBuilder()
                .traceId("48485a3953bb61246b221d5bc9e6496c")
                .build());
  }

  @Test
  public void dependencyLinkRoundTrip() throws IOException {
    DependencyLink link =
        DependencyLink.newBuilder().parent("foo").child("bar").callCount(2).build();

    Buffer bytes = new Buffer();
    bytes.write(DependencyLinkBytesEncoder.JSON_V1.encode(link));
    assertThat(JsonAdapters.DEPENDENCY_LINK_ADAPTER.fromJson(bytes)).isEqualTo(link);
  }

  @Test
  public void dependencyLinkRoundTrip_withError() throws IOException {
    DependencyLink link =
        DependencyLink.newBuilder().parent("foo").child("bar").callCount(2).errorCount(1).build();

    Buffer bytes = new Buffer();
    bytes.write(DependencyLinkBytesEncoder.JSON_V1.encode(link));
    assertThat(JsonAdapters.DEPENDENCY_LINK_ADAPTER.fromJson(bytes)).isEqualTo(link);
  }
}
