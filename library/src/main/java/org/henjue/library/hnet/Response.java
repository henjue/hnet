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

import org.henjue.library.hnet.typed.TypedInput;

import java.util.List;

public final class Response {
    private final String url;
    private final int status;
    private final String reason;
    private final List<Header> headers;
    private final TypedInput body;

    public Response(String url, int status, String reason, List<Header> headers, TypedInput body) {
        this.body = body;
        this.url = url;
        this.status = status;
        this.reason = reason;
        this.headers = headers;
    }

    public TypedInput getBody() {
        return body;
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public String getReason() {
        return reason;
    }

    public int getStatus() {
        return status;
    }

    public String getUrl() {
        return url;
    }
}
