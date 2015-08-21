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

import org.henjue.library.hnet.anntoation.Field;
import org.henjue.library.hnet.anntoation.Param;
import org.henjue.library.hnet.anntoation.Part;
import org.henjue.library.hnet.anntoation.Path;
import org.henjue.library.hnet.anntoation.Query;
import org.henjue.library.hnet.converter.Converter;
import org.henjue.library.hnet.typed.FormUrlEncodedTypedOutput;
import org.henjue.library.hnet.typed.MultipartTypedOutput;
import org.henjue.library.hnet.typed.TypedOutput;
import org.henjue.library.hnet.typed.TypedString;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class RequestBuilder implements RequestFacade {
    private final Annotation[] paramAnnotations;
    private final boolean isSync;
    private final Converter converter;
    private final String apiUrl;
    private final FormUrlEncodedTypedOutput formBody;
    private final MultipartTypedOutput multipartBody;
    private final String requestMethod;
    private final RequestIntercept intercept;
    private final MethodInfo.RequestType requestType;
    private final boolean appendPath;
    private final boolean callIntercept;
    private final RequestFilter filter;
    private String relativeUrl;
    private StringBuilder queryParams;
    private TypedOutput body;
    private List<Header> headers;
    private String contentTypeHeader;

    RequestBuilder(String apiUrl, RequestFilter filter, boolean callIntercept, boolean appendPath, MethodInfo info, Converter converter, RequestIntercept intercept) {
        if (info.endpointUrl == null) {
            this.apiUrl = apiUrl;
        } else {
            this.apiUrl = info.endpointUrl;
        }
        this.appendPath = appendPath;
        this.callIntercept = callIntercept;
        this.intercept = intercept;
        this.converter = converter;
        this.filter = filter;
        paramAnnotations = info.requestParamAnnotations;
        this.requestType = info.requestType;
        isSync = info.isSynchronous;
        requestMethod = info.requestMethod;
        if (info.headers != null) {
            headers = new ArrayList<>(info.headers);
        }
        contentTypeHeader = info.contentTypeHeader;


        relativeUrl = info.requestUrl;
        String requestQuery = info.requestQuery;
        if (requestQuery != null) {
            queryParams = new StringBuilder().append('?').append(requestQuery);
        }
        switch (info.requestType) {
            case FORM_URL_ENCODED:
                formBody = new FormUrlEncodedTypedOutput();
                multipartBody = null;
                body = formBody;
                break;
            case MULTIPART:
                formBody = null;
                multipartBody = new MultipartTypedOutput();
                body = multipartBody;
                break;
            case SIMPLE:
                formBody = null;
                multipartBody = null;
                // If present, 'body' will be set in 'setArguments' call.
                break;
            default:
                throw new IllegalArgumentException("Unknown request type: " + info.requestType);
        }
    }


    void bindArgs(Object[] args, RequestIntercept intercept) {
        if (args == null) {
            return;
        }
        int count = args.length;
        if (!isSync) {
            count -= 1;
        }
        filter.onStart(this);
        if (callIntercept) intercept.onStart(this);
        for (int i = 0; i < count; i++) {
            Object value = args[i];
            Annotation annotation = paramAnnotations[i];
            Class<? extends Annotation> type = annotation.annotationType();
            if (type == Path.class) {
                Path path = (Path) annotation;
                String name = path.value();
                if (value == null) {
                    throw new IllegalArgumentException(
                            "Path parameter \"" + name + "\" value must not be null.");
                }
                addPathParam(name, String.valueOf(value), appendPath && path.encode());
            } else if (type == Query.class) {
                if (value != null) { // Skip null values.
                    Query query = (Query) annotation;
                    addQueryParam(query.value(), String.valueOf(value), query.encodeName(), query.encodeValue());
                }
            } else if (type == Field.class) {
                if (value != null) { // Skip null values.
                    Field field = (Field) annotation;
                    String name = field.value();
                    boolean encodeName = field.encodeName();
                    boolean encodeValue = field.encodeValue();
                    addToForm(name, value, encodeName, encodeValue);
                }
            } else if (type == Param.class) {
                if (requestType == MethodInfo.RequestType.FORM_URL_ENCODED) {
                    Param field = (Param) annotation;
                    String name = field.value();
                    boolean encodeName = field.encodeName();
                    boolean encodeValue = field.encodeValue();
                    addToForm(name, value, encodeName, encodeValue);
                } else if (requestType == MethodInfo.RequestType.MULTIPART) {
                    String name = ((Param) annotation).value();
                    String transferEncoding = ((Param) annotation).encoding();
                    addPart(name, transferEncoding, value);
                }

            } else if (type == Part.class) {
                if (value != null) { // Skip null values.
                    String name = ((Part) annotation).value();
                    String transferEncoding = ((Part) annotation).encoding();
                    addPart(name, transferEncoding, value);
                }
            } else if (type == org.henjue.library.hnet.anntoation.Header.class) {
                if (value != null) { // Skip null values.
                    String name = ((org.henjue.library.hnet.anntoation.Header) annotation).value();
                    if (value instanceof Iterable) {
                        for (Object iterableValue : (Iterable<?>) value) {
                            if (iterableValue != null) { // Skip null values.
                                addHeader(name, iterableValue.toString());
                            }
                        }
                    } else if (value.getClass().isArray()) {
                        for (int x = 0, arrayLength = Array.getLength(value); x < arrayLength; x++) {
                            Object arrayValue = Array.get(value, x);
                            if (arrayValue != null) { // Skip null values.
                                addHeader(name, arrayValue.toString());
                            }
                        }
                    } else {
                        addHeader(name, value.toString());
                    }
                }
            } else if (type == org.henjue.library.hnet.anntoation.Body.class) {
                if (value == null) {
                    throw new IllegalArgumentException("Body parameter value must not be null.");
                }
                if (value instanceof TypedOutput) {
                    body = (TypedOutput) value;
                } else {
                    body = converter.toBody(value);
                }
            } else {
                throw new IllegalArgumentException(
                        "Unknown annotation: " + type.getCanonicalName());
            }

        }
    }

    private void addToForm(String key, Object value, boolean encodeName, boolean encodeValue) {
        if (value instanceof Iterable) {
            for (Object iterableValue : (Iterable<?>) value) {
                if (iterableValue != null) { // Skip null values.
                    String value1 = iterableValue.toString();
                    addField(key, encodeName, value1, encodeValue);
                }
            }
        } else if (value.getClass().isArray()) {
            for (int x = 0, arrayLength = Array.getLength(value); x < arrayLength; x++) {
                Object arrayValue = Array.get(value, x);
                if (arrayValue != null) { // Skip null values.
                    String value1 = arrayValue.toString();
                    addField(key, encodeName, value1, encodeValue);
                }
            }
        } else {
            addField(key, encodeName, String.valueOf(value), encodeValue);
        }
    }

    private void addQueryParam(String name, String value, boolean encodeName, boolean encodeValue) {
        if (name == null) {
            throw new IllegalArgumentException("Query param name must not be null.");
        }
        if (value == null) {
            throw new IllegalArgumentException("Query param \"" + name + "\" value must not be null.");
        }
        try {
            StringBuilder queryParams = this.queryParams;
            if (queryParams == null) {
                this.queryParams = queryParams = new StringBuilder();
            }

            queryParams.append(queryParams.length() > 0 ? '&' : '?');

            if (encodeName) {
                name = URLEncoder.encode(name, "UTF-8");
            }
            if (encodeValue) {
                value = URLEncoder.encode(value, "UTF-8");
            }
            queryParams.append(name).append('=').append(value);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(
                    "Unable to convert query parameter \"" + name + "\" value to UTF-8: " + value, e);
        }
    }

    @Override
    public void addHeader(String name, String value) {
        if (name == null) {
            throw new IllegalArgumentException("Header name must not be null.");
        }
        if ("Content-Type".equalsIgnoreCase(name)) {
            contentTypeHeader = value;
            return;
        }

        List<Header> headers = this.headers;
        if (headers == null) {
            this.headers = headers = new ArrayList<Header>(2);
        }
        headers.add(new Header(name, value));
    }

    @Override
    public void addPathParam(String name, String value) {
        addPathParam(name, value, true);
    }

    @Override
    public void addEncodedPathParam(String name, String value) {
        addPathParam(name, value, false);
    }

    @Override
    public void addQueryParam(String name, String value) {
        addQueryParam(name, value, false, true);
    }

    @Override
    public void addEncodedQueryParam(String name, String value) {
        addQueryParam(name, value, false, false);
    }

    @Override
    public void add(String name, Object obj) {
        if (obj == null) return;
        switch (requestType) {
            case FORM_URL_ENCODED:
                addField(name, true, String.valueOf(obj), true);
                break;
            case MULTIPART:
                addPart(name, MultipartTypedOutput.DEFAULT_TRANSFER_ENCODING, obj);
                break;
            case SIMPLE:
                addQueryParam(name, String.valueOf(obj));
                break;
            default:
                throw new IllegalArgumentException("Unknown request type: " + requestType);
        }
    }

    @Override
    public String getPath() {
        return relativeUrl;
    }

    public void addPart(String name, String transferEncoding, Object obj) {
        if (obj instanceof TypedOutput) {
            multipartBody.addPart(name, transferEncoding, (TypedOutput) obj);
        } else if (obj instanceof String) {
            multipartBody.addPart(name, transferEncoding, new TypedString((String) obj));
        } else {
            multipartBody.addPart(name, transferEncoding, converter.toBody(obj));
        }
        filter.onAdd(name, obj);
        if (callIntercept) intercept.onAdd(name, obj);
    }

    public void addField(String name, boolean encodeName, String value, boolean encodeValue) {
        if (formBody != null)
            formBody.addField(callIntercept, intercept, filter, name, encodeName, value, encodeValue);
    }

    private void addPathParam(String name, String value, boolean urlEncodeValue) {
        if (name == null) {
            throw new IllegalArgumentException("Path replacement name must not be null.");
        }
        if (value == null) {
            throw new IllegalArgumentException(
                    "Path replacement \"" + name + "\" value must not be null.");
        }
        try {
            if (urlEncodeValue) {
                String encodedValue = URLEncoder.encode(String.valueOf(value), "UTF-8");
                // URLEncoder encodes for use as a query parameter. Path encoding uses %20 to
                // encode spaces rather than +. Query encoding difference specified in HTML spec.
                // Any remaining plus signs represent spaces as already URLEncoded.
                encodedValue = encodedValue.replace("+", "%20");
                relativeUrl = relativeUrl.replace("{" + name + "}", encodedValue);
            } else {
                relativeUrl = relativeUrl.replace("{" + name + "}", String.valueOf(value));
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(
                    "Unable to convert path parameter \"" + name + "\" value to UTF-8:" + value, e);
        }
    }

    public Request build() throws UnsupportedEncodingException {
        final StringBuilder url;
        if (appendPath) {
            String apiUrl = this.apiUrl;
            url = new StringBuilder(apiUrl);
            if (apiUrl.endsWith("/")) {
                // We require relative paths to start with '/'. Prevent a double-slash.
                url.deleteCharAt(url.length() - 1);
            }

            url.append(relativeUrl);
        } else {
            url = new StringBuilder(this.apiUrl);
        }
        StringBuilder queryParams = this.queryParams;
        if (queryParams != null) {
            url.append(queryParams);
        }

        TypedOutput body = this.body;
        List<Header> headers = this.headers;
        if (contentTypeHeader != null) {
            if (body != null) {
                body = new MimeOverridingTypedOutput(body, contentTypeHeader);
            } else {
                Header header = new Header("Content-Type", contentTypeHeader);
                if (headers == null) {
                    headers = Collections.singletonList(header);
                } else {
                    headers.add(header);
                }
            }
        }

        return new Request(requestMethod, url.toString(), headers, body);
    }

    private static class MimeOverridingTypedOutput implements TypedOutput {
        private final TypedOutput delegate;
        private final String mimeType;

        MimeOverridingTypedOutput(TypedOutput delegate, String mimeType) {
            this.delegate = delegate;
            this.mimeType = mimeType;
        }

        @Override
        public String fileName() {
            return delegate.fileName();
        }

        @Override
        public String mimeType() {
            return mimeType;
        }

        @Override
        public long length() {
            return delegate.length();
        }

        @Override
        public void writeTo(OutputStream out) throws IOException {
            delegate.writeTo(out);
        }
    }
}
