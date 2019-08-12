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
package zipkin2.storage.logzio.client;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import zipkin2.Call;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

public class HttpCallTest {
  @Rule
  public MockWebServer mws = new MockWebServer();

  HttpCall.Factory http = new HttpCall.Factory(new OkHttpClient(), mws.url(""));
  Request request = new Request.Builder().url(http.baseUrl).build();

  @After
  public void close() {
    http.close();
  }

  @Test
  public void executionException_conversionException() throws Exception {
    mws.enqueue(new MockResponse());

    Call<?> call = http.newCall(request, b -> {
      throw new IllegalArgumentException("eeek");
    });

    try {
      call.execute();
      failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException expected) {
      assertThat(expected).isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  public void cloned() throws Exception {
    mws.enqueue(new MockResponse());

    Call<?> call = http.newCall(request, b -> null);
    call.execute();

    try {
      call.execute();
      failBecauseExceptionWasNotThrown(IllegalStateException.class);
    } catch (IllegalStateException expected) {
      assertThat(expected).isInstanceOf(IllegalStateException.class);
    }

    mws.enqueue(new MockResponse());

    call.clone().execute();
  }

  @Test
  public void executionException_httpFailure() throws Exception {
    mws.enqueue(new MockResponse().setResponseCode(500));

    Call<?> call = http.newCall(request, b -> null);

    try {
      call.execute();
      failBecauseExceptionWasNotThrown(IllegalStateException.class);
    } catch (IllegalStateException expected) {
      assertThat(expected).isInstanceOf(IllegalStateException.class);
    }
  }
}
