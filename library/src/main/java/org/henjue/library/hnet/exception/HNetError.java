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
package org.henjue.library.hnet.exception;

import org.henjue.library.hnet.Response;
import org.henjue.library.hnet.converter.Converter;
import org.henjue.library.hnet.typed.TypedInput;

import java.io.IOException;
import java.lang.reflect.Type;



public class HNetError extends RuntimeException {

    private final String url;
    private final Response response;
    private final Converter converter;
    private final Type successType;
    private final Kind kind;

    HNetError(String message, String url, Response response, Converter converter,
              Type successType, Kind kind, Throwable exception) {
        super(message, exception);
        this.url = url;
        this.response = response;
        this.converter = converter;
        this.successType = successType;
        this.kind = kind;
    }

    public static HNetError networkError(String url, IOException exception) {
        return new HNetError(exception.getMessage(), url, null, null, null, Kind.NETWORK,
                exception);
    }

    public static HNetError conversionError(String url, Response response, Converter converter,
                                            Type successType, ConversionException exception) {
        return new HNetError(exception.getMessage(), url, response, converter, successType,
                Kind.CONVERSION, exception);
    }

    public static HNetError httpError(String url, Response response, Converter converter,
                                      Type successType) {
        String message = response.getStatus() + " " + response.getReason();
        return new HNetError(message, url, response, converter, successType, Kind.HTTP, null);
    }

    public static HNetError unexpectedError(String url, Throwable exception) {
        return new HNetError(exception.getMessage(), url, null, null, null, Kind.UNEXPECTED,
                exception);
    }

    public String getUrl() {
        return url;
    }

    public Response getResponse() {
        return response;
    }

    @Deprecated
    public boolean isNetworkError() {
        return kind == Kind.NETWORK;
    }

    public Kind getKind() {
        return kind;
    }

    public Object getBody() {
        return getBodyAs(successType);
    }

    public Type getSuccessType() {
        return successType;
    }

    public Object getBodyAs(Type type) {
        if (response == null) {
            return null;
        }
        TypedInput body = response.getBody();
        if (body == null) {
            return null;
        }
        try {
            return converter.fromBody(body, type);
        } catch (ConversionException e) {
            throw new RuntimeException(e);
        }
    }

    public enum Kind {
        /**
         * 和服务器通讯发生问题
         */
        NETWORK,
        /**
         * 数据转换发生错误
         */
        CONVERSION,
        /**
         * http 错误
         */
        HTTP,
        /**
         * 程序内部错误
         */
        UNEXPECTED
    }
}
