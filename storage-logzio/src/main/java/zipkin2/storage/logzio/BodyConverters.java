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
package zipkin2.storage.logzio;

import com.squareup.moshi.JsonReader;
import okio.BufferedSource;
import zipkin2.Span;
import zipkin2.elasticsearch.internal.client.HttpCall;
import zipkin2.elasticsearch.internal.client.SearchResultConverter;

import java.io.IOException;
import java.util.List;

import static zipkin2.elasticsearch.internal.JsonReaders.collectValuesNamed;

public final class BodyConverters {
    static final HttpCall.BodyConverter<Object> NULL =
            new HttpCall.BodyConverter<Object>() {
                @Override
                public Object convert(BufferedSource content) {
                    return null;
                }
            };
    static final HttpCall.BodyConverter<List<String>> KEYS =
            new HttpCall.BodyConverter<List<String>>() {
                @Override
                public List<String> convert(BufferedSource b) throws IOException {
                    return collectValuesNamed(JsonReader.of(b), "key");
                }
            };
    public static final HttpCall.BodyConverter<List<Span>> SPANS =
            SearchResultConverter.create(JsonAdapters.SPAN_ADAPTER);
    //todo: check normal behavior of dependencies (dependecy link has been deleted here)
}
