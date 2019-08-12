/*
 * Copyright 2015-2018 The OpenZipkin Authors
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

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import zipkin2.internal.Nullable;

public class SearchCallFactory {
    private static final MediaType APPLICATION_JSON = MediaType.parse("application/json");
    public static final String API_TOKEN_HEADER = "X-API-TOKEN";

    final HttpCall.Factory http;
    final String apiToken;
    final JsonAdapter<SearchRequest> searchRequest =
            new Moshi.Builder().build().adapter(SearchRequest.class);

    public SearchCallFactory(HttpCall.Factory http, String apiToken) {
        this.http = http;
        this.apiToken = apiToken;
    }

    public <V> HttpCall<V> newCall(SearchRequest request, HttpCall.BodyConverter<V> bodyConverter) {
        Request httpRequest = new Request.Builder().url(lenientSearch(request.type))
                .post(RequestBody.create(APPLICATION_JSON, searchRequest.toJson(request)))
                .header("Content-Type", "application/json")
                .header(API_TOKEN_HEADER, apiToken)
                .tag(request.tag()).build();

        return http.newCall(httpRequest, bodyConverter);
    }

    HttpUrl lenientSearch(@Nullable String type) {
        HttpUrl.Builder builder = http.baseUrl.newBuilder();
        if (type != null) builder.addPathSegment(type);
        return builder.build();
    }
}
