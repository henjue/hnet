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

import android.os.Handler;
import android.os.Looper;

import org.henjue.library.hnet.anntoation.FormUrlEncoded;
import org.henjue.library.hnet.anntoation.Multipart;
import org.henjue.library.hnet.converter.Converter;
import org.henjue.library.hnet.converter.StringConverter;
import org.henjue.library.hnet.exception.ConversionException;
import org.henjue.library.hnet.exception.HNetError;
import org.henjue.library.hnet.http.ClientStack;
import org.henjue.library.hnet.http.UrlConnecttionStack;
import org.henjue.library.hnet.typed.ExceptionCatchingTypedInput;
import org.henjue.library.hnet.typed.TypedByteArray;
import org.henjue.library.hnet.typed.TypedInput;
import org.henjue.library.hnet.typed.TypedOutput;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

public class HNet {
    static final String THREAD_PREFIX = "HNet-";
    static final String IDLE_THREAD_NAME = THREAD_PREFIX + "Idle";
    final Executor httpExecutor;
    final Executor callbackExecutor;
    final Converter converter;
    final Log log;
    final Endpoint server;
    final ErrorHandler errorHandler;
    private final RequestIntercept intercept;
    private final ClientStack.Provider clientProvider;
    volatile LogLevel logLevel;

    private HNet(Endpoint server, ClientStack.Provider clientProvider, RequestIntercept intercept, Executor httpExecutor, Executor callbackExecutor, Converter converter, ErrorHandler errorHandler, Log log, LogLevel logLevel) {
        this.server = server;
        this.clientProvider = clientProvider;
        this.intercept = intercept;
        this.httpExecutor = httpExecutor;
        this.callbackExecutor = callbackExecutor;
        this.converter = converter;
        this.errorHandler = errorHandler;
        this.log = log;
        this.logLevel = logLevel;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> clazz) {
        Utils.validateServiceClass(clazz);
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, new NetHandler(clazz));
    }

    private void logResponseBody(TypedInput body, Object convert) {
        if (logLevel.ordinal() == LogLevel.HEADERS_AND_ARGS.ordinal()) {
            log.log("<--- BODY:");
            log.log(convert.toString());
        }
    }

    /**
     * Log an exception that occurred during the processing of a request or response.
     */
    public void logException(HNetError e) {
        log.log(String.format("---- ERROR %s", e.getUrl() != null ? e.getUrl() : ""));
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        log.log(sw.toString());
        log.log("---- END ERROR");
    }

    /**
     * Log an exception that occurred during the processing of a request or response.
     */
    public void logException(Throwable t, String url) {
        log.log(String.format("---- ERROR %s", url != null ? url : ""));
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        log.log(sw.toString());
        log.log("---- END ERROR");
    }

    /**
     * Log request headers and body. Consumes request body and returns identical replacement.
     */
    Request logAndReplaceRequest(String name, Request request, Object[] args) throws IOException {
        log.log(String.format("---> %s %s %s", name, request.getMethod(), request.getUrl()));

        if (logLevel.ordinal() >= LogLevel.HEADERS.ordinal()) {
            for (Header header : request.getHeaders()) {
                log.log(header.toString());
            }

            String bodySize = "no";
            TypedOutput body = request.getBody();
            if (body != null) {
                String bodyMime = body.mimeType();
                if (bodyMime != null) {
                    log.log("Content-Type: " + bodyMime);
                }

                long bodyLength = body.length();
                bodySize = bodyLength + "-byte";
                if (bodyLength != -1) {
                    log.log("Content-Length: " + bodyLength);
                }

                if (logLevel.ordinal() >= LogLevel.FULL.ordinal()) {
                    if (!request.getHeaders().isEmpty()) {
                        log.log("");
                    }
                    if (!(body instanceof TypedByteArray)) {
                        // Read the entire response body to we can log it and replace the original response
                        request = Utils.readBodyToBytesIfNecessary(request);
                        body = request.getBody();
                    }

                    byte[] bodyBytes = ((TypedByteArray) body).getBytes();
                    String bodyCharset = MimeUtil.parseCharset(body.mimeType(), "UTF-8");
                    log.log(new String(bodyBytes, bodyCharset));
                } else if (logLevel.ordinal() >= LogLevel.HEADERS_AND_ARGS.ordinal()) {
                    if (!request.getHeaders().isEmpty()) {
                        log.log("---> REQUEST:");
                    }
                    for (int i = 0; i < args.length; i++) {
                        log.log("#" + i + ": " + args[i]);
                    }
                }
            }

            log.log(String.format("---> END %s (%s body)", name, bodySize));
        }

        return request;
    }

    /**
     * Log response headers and body. Consumes response body and returns identical replacement.
     */
    private Response logAndReplaceResponse(String url, Response response, long elapsedTime)
            throws IOException {
        log.log(String.format("<--- HTTP %s %s (%sms)", response.getStatus(), url, elapsedTime));

        if (logLevel.ordinal() >= LogLevel.HEADERS.ordinal()) {
            for (Header header : response.getHeaders()) {
                log.log(header.toString());
            }

            long bodySize = 0;
            TypedInput body = response.getBody();
            if (body != null) {
                bodySize = body.length();

                if (logLevel.ordinal() >= LogLevel.FULL.ordinal()) {
                    if (!response.getHeaders().isEmpty()) {
                        log.log("");
                    }

                    if (!(body instanceof TypedByteArray)) {
                        // Read the entire response body so we can log it and replace the original response
                        response = Utils.readBodyToBytesIfNecessary(response);
                        body = response.getBody();
                    }

                    byte[] bodyBytes = ((TypedByteArray) body).getBytes();
                    bodySize = bodyBytes.length;
                    String bodyMime = body.mimeType();
                    String bodyCharset = MimeUtil.parseCharset(bodyMime, "UTF-8");
                    log.log(new String(bodyBytes, bodyCharset));
                }
            }

            log.log(String.format("<--- END HTTP (%s-byte body)", bodySize));
        }

        return response;
    }

    public enum LogLevel {
        NONE,
        BASIC,
        HEADERS,
        HEADERS_AND_ARGS,

        FULL;

        public boolean log() {
            return this != NONE;
        }
    }

    public interface Log {
        void log(String message);
    }

    public static class Builder {

        private Executor httpExecutor;
        private Executor callbackExecutor;
        private Converter converter;
        private Endpoint endpoint;
        private ClientStack.Provider clientProvider;
        private ErrorHandler errorHandler;
        private Log log;
        private LogLevel logLevel = LogLevel.NONE;
        private RequestIntercept intercept;

        public Builder setHttpExecutor(Executor httpExecutor) {
            if (httpExecutor == null) {
                throw new NullPointerException("httpExecutor may not be null.");
            }
            this.httpExecutor = httpExecutor;
            return this;
        }

        public ErrorHandler getErrorHandler() {
            return errorHandler;
        }

        public LogLevel getLogLevel() {
            return logLevel;
        }

        public Builder setConverter(Converter converter) {
            this.converter = converter;
            return this;
        }

        public Builder setLog(Log log) {
            if (log == null) {
                throw new NullPointerException("Log may not be null.");
            }
            this.log = log;
            return this;
        }

        public Builder setClient(ClientStack.Provider clientProvider) {
            if (clientProvider == null) {
                throw new NullPointerException("Client provider may not be null.");
            }
            this.clientProvider = clientProvider;
            return this;
        }

        public Builder setIntercept(RequestIntercept intercept) {
            this.intercept = intercept;
            return this;
        }

        /**
         * API endpoint URL.
         */
        public Builder setEndpoint(String endpoint) {
            if (endpoint == null || endpoint.trim().length() == 0) {
                throw new NullPointerException("Endpoint may not be blank.");
            }
            this.endpoint = Endpoints.newFixedEndpoint(endpoint);
            return this;
        }

        /**
         * API endpoint.
         */
        public Builder setEndpoint(Endpoint endpoint) {
            if (endpoint == null) {
                throw new NullPointerException("Endpoint may not be null.");
            }
            this.endpoint = endpoint;
            return this;
        }

        public HNet build() {
            ensureSaneDefaults();
            return new HNet(this.endpoint, this.clientProvider, this.intercept, this.httpExecutor, this.callbackExecutor, this.converter, this.errorHandler, this.log, logLevel);
        }

        private void ensureSaneDefaults() {
            if (intercept == null) {
                intercept = RequestIntercept.NONE;
            }
            if (httpExecutor == null) {
                httpExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
                    @Override
                    public Thread newThread(final Runnable r) {
                        return new Thread(new Runnable() {
                            @Override
                            public void run() {
                                android.os.Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND);
                                r.run();
                            }
                        }, HNet.IDLE_THREAD_NAME);
                    }
                });
            }
            if (log == null) {
                log = new Log() {
                    private static final int LOG_CHUNK_SIZE = 4000;

                    private final String tag = "hnet";

                    @Override
                    public final void log(String message) {
                        for (int i = 0, len = message.length(); i < len; i += LOG_CHUNK_SIZE) {
                            int end = Math.min(len, i + LOG_CHUNK_SIZE);
                            logChunk(message.substring(i, end));
                        }
                    }

                    public void logChunk(String chunk) {
                        android.util.Log.d(getTag(), chunk);
                    }

                    public String getTag() {
                        return tag;
                    }
                };
            }
            if (converter == null) {
                converter = new StringConverter();
            }
            if (clientProvider == null) {
                clientProvider = new ClientStack.Provider() {
                    @Override
                    public ClientStack get() {
                        return new UrlConnecttionStack();
                    }
                };
            }
            if (errorHandler == null) {
                errorHandler = ErrorHandler.DEFAULT;
            }
            if (callbackExecutor == null) {
                callbackExecutor = new Executor() {
                    private final Handler handler = new Handler(Looper.getMainLooper());

                    @Override
                    public void execute(Runnable runnable) {
                        handler.post(runnable);
                    }
                };
            }
        }
    }

    private class NetHandler implements InvocationHandler {
        private MethodInfo.RequestType defaultRequestType = MethodInfo.RequestType.SIMPLE;

        public <T> NetHandler(Class<T> clazz) {
            Annotation[] ass = clazz.getAnnotations();
            if (ass != null) {
                for (Annotation annotation : ass) {
                    if (annotation.annotationType() == FormUrlEncoded.class) {
                        defaultRequestType = MethodInfo.RequestType.FORM_URL_ENCODED;
                    } else if (annotation.annotationType() == Multipart.class) {
                        defaultRequestType = MethodInfo.RequestType.MULTIPART;
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object invoke(Object proxy, Method method, final Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }
            final MethodInfo info = new MethodInfo(defaultRequestType, method);
            if (info.isSynchronous) {
                return invokeRequest(intercept, info, args);
            }
            Callback<?> callback = (Callback<?>) args[args.length - 1];
            httpExecutor.execute(new CallbackRunnable(callback, callbackExecutor, errorHandler) {
                @Override
                public ResponseWrapper obtainResponse() {
                    return (ResponseWrapper) invokeRequest(intercept, info, args);
                }
            });
            return null;
        }

        /**
         * 同步方法直接返回response对象。异步方法返回{@link ResponseWrapper}对象
         */
        private Object invokeRequest(RequestIntercept intercept, MethodInfo methodInfo, Object[] args) {
            String url = null;
            try {
                methodInfo.init();
                String serverUrl = server.getUrl();
                final RequestFilter filter;
                if (methodInfo.filter != null) {
                    filter = methodInfo.filter.newInstance();
                } else {
                    filter = RequestFilter.NONE;
                }
                final RequestBuilder builder = new RequestBuilder(serverUrl, filter, methodInfo.callIntercept, methodInfo.appendPath, methodInfo, converter, intercept);
                builder.bindArgs(args, intercept);
                filter.onComplite(builder);
                intercept.onComplite(builder);

                Request request = builder.build();
                url = request.getUrl();
                if (!methodInfo.isSynchronous && methodInfo.appendPath) {
                    // If we are executing asynchronously then update the current thread with a useful name.
                    int substrEnd = url.indexOf("?", serverUrl.length());
                    if (substrEnd == -1) {
                        substrEnd = url.length();
                    }
                    Thread.currentThread().setName(THREAD_PREFIX
                            + url.substring(serverUrl.length(), substrEnd));
                }

                if (logLevel.log()) {
                    // Log the request data.
                    request = logAndReplaceRequest("HTTP", request, args);
                }
                long start = System.nanoTime();
                Response response = clientProvider.get().execute(request);
                long elapsedTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                int statusCode = response.getStatus();
                if (logLevel.log()) {
                    // Log the response data.
                    response = logAndReplaceResponse(url, response, elapsedTime);
                }
                Type type = methodInfo.responseObjectType;
                if (statusCode >= 200 && statusCode < 300) { // 2XX == successful request
                    // Caller requested the raw Response object directly.
                    if (type.equals(Response.class)) {
                        if (!methodInfo.isStreaming) {
                            // Read the entire stream and replace with one backed by a byte[].
                            response = Utils.readBodyToBytesIfNecessary(response);
                        }

                        if (methodInfo.isSynchronous) {
                            return response;
                        }
                        return new ResponseWrapper(response, response);
                    }

                    TypedInput body = response.getBody();
                    if (body == null) {
                        if (methodInfo.isSynchronous) {
                            return null;
                        }
                        return new ResponseWrapper(response, null);
                    }

                    ExceptionCatchingTypedInput wrapped = new ExceptionCatchingTypedInput(body);
                    try {
                        Object convert = converter.fromBody(wrapped, type);
                        logResponseBody(body, convert);
                        if (methodInfo.isSynchronous) {
                            return convert;
                        }
                        return new ResponseWrapper(response, convert);
                    } catch (ConversionException e) {

                        if (wrapped.threwException()) {
                            throw wrapped.getThrownException();
                        }
                        response = Utils.replaceResponseBody(response, null);
                        throw HNetError.conversionError(url, response, converter, type, e);
                    }
                }
                response = Utils.readBodyToBytesIfNecessary(response);
                throw HNetError.httpError(url, response, converter, type);
            } catch (HNetError e) {
                throw e;
            } catch (IOException e) {
                if (logLevel.log()) {
                    logException(e, url);
                }
                throw HNetError.networkError(url, e);
            } catch (Throwable t) {
                if (logLevel.log()) {
                    logException(t, url);
                }
                throw HNetError.unexpectedError(url, t);
            } finally {
                if (!methodInfo.isSynchronous) {
                    Thread.currentThread().setName(IDLE_THREAD_NAME);
                }
            }
        }
    }

}
