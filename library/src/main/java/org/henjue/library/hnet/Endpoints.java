/*
 * Copyright (C) 2015 Henjue, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.henjue.library.hnet;

public class Endpoints {
    private static final String DEFAULT_NAME = "default";

    private Endpoints() {
    }

    public static Endpoint newFixedEndpoint(String url) {
        return new FixedEndpoint(url, DEFAULT_NAME);
    }

    public static Endpoint newFixedEndpoint(String url, String name) {
        return new FixedEndpoint(url, name);
    }

    private static class FixedEndpoint implements Endpoint {
        private final String apiUrl;
        private final String name;

        FixedEndpoint(String apiUrl, String name) {
            this.apiUrl = apiUrl;
            this.name = name;
        }

        @Override
        public String getUrl() {
            return apiUrl;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
