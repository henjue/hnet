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

import org.henjue.library.hnet.anntoation.Body;
import org.henjue.library.hnet.anntoation.Field;
import org.henjue.library.hnet.anntoation.Filter;
import org.henjue.library.hnet.anntoation.FormUrlEncoded;
import org.henjue.library.hnet.anntoation.Headers;
import org.henjue.library.hnet.anntoation.Multipart;
import org.henjue.library.hnet.anntoation.NoneEncoded;
import org.henjue.library.hnet.anntoation.Param;
import org.henjue.library.hnet.anntoation.Part;
import org.henjue.library.hnet.anntoation.Path;
import org.henjue.library.hnet.anntoation.Query;
import org.henjue.library.hnet.anntoation.RestMethod;
import org.henjue.library.hnet.anntoation.Streaming;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MethodInfo {
    private static final String PARAM = "[a-zA-Z][a-zA-Z0-9_-]*";
    private static final Pattern PARAM_NAME_REGEX = Pattern.compile(PARAM);
    private static final Pattern PARAM_URL_REGEX = Pattern.compile("\\{(" + PARAM + ")\\}");
    final boolean isSynchronous;
    private final Method method;
    private final ResponseType responseType;
    private final RequestType defaultRequestType;
    public boolean isStreaming;
    Annotation[] requestParamAnnotations;
    Set<String> requestUrlParamNames;
    String requestQuery;
    List<Header> headers;//parseMethodAnnotations 方法中根据注解来添加header
    String contentTypeHeader;
    Type responseObjectType;
    String requestUrl;
    String endpointUrl = null;
    RequestType requestType;//默认使用类上面的注解定义，方法上的定义将覆盖这个属性
    String requestMethod;
    boolean needEncode = false;
    private boolean requestHasBody;
    boolean appendPath = true;
    boolean callIntercept = true;
    Class<? extends RequestFilter> filter;

    public MethodInfo(RequestType defaultRequestType, Method method) {
        if (defaultRequestType == null) defaultRequestType = RequestType.SIMPLE;
        this.method = method;
        this.defaultRequestType = defaultRequestType;
        responseType = parseResponseType();
        isSynchronous = (responseType == ResponseType.OBJECT);
    }

    static Set<String> parsePathParameters(String path) {
        Matcher m = PARAM_URL_REGEX.matcher(path);
        Set<String> patterns = new LinkedHashSet<String>();
        while (m.find()) {
            patterns.add(m.group(1));
        }
        return patterns;
    }

    private static Type getParameterUpperBound(ParameterizedType type) {
        Type[] types = type.getActualTypeArguments();
        for (int i = 0; i < types.length; i++) {
            Type paramType = types[i];
            if (paramType instanceof WildcardType) {
                types[i] = ((WildcardType) paramType).getUpperBounds()[0];
            }
        }
        return types[0];
    }

    synchronized void init() {
        parseMethodAnnotations();
        parseParameters();
    }

    List<Header> parseHeaders(String[] headers) {
        List<Header> headerList = new ArrayList<Header>();
        for (String header : headers) {
            int colon = header.indexOf(':');
            if (colon == -1 || colon == 0 || colon == header.length() - 1) {
                throw methodError("@Headers value must be in the form \"Name: Value\". Found: \"%s\"",
                        header);
            }
            String headerName = header.substring(0, colon);
            String headerValue = header.substring(colon + 1).trim();
            if ("Content-Type".equalsIgnoreCase(headerName)) {
                contentTypeHeader = headerValue;
            } else {
                headerList.add(new Header(headerName, headerValue));
            }
        }
        return headerList;
    }

    private void parseParameters() {
        Class<?>[] paramTypes = method.getParameterTypes();
        Annotation[][] paramAnnotationArrays = method.getParameterAnnotations();
        int count = paramAnnotationArrays.length;
        if (!isSynchronous) {
            count -= 1;
        }
        Annotation[] requestParamAnnotations = new Annotation[count];

        boolean gotField = false;
        boolean gotParam = false;
        boolean gotPart = false;
        boolean gotBody = false;
        for (int i = 0; i < count; i++) {
            Annotation[] paramAnnotations = paramAnnotationArrays[i];
            boolean hasAnnotation = true;
            for (Annotation annotation : paramAnnotations) {
                Class<? extends Annotation> annType = annotation.annotationType();
                if (annType == Query.class) {
                } else if (annType == Field.class) {
                    if (requestType != RequestType.FORM_URL_ENCODED) {
                        throw parameterError(i, "@Field parameters can only be used with form encoding.");
                    }
                    gotField = true;
                } else if (annType == Path.class) {
                    String name = ((Path) annotation).value();
                    validatePathName(i, name);
                } else if (annType == Part.class) {
                    if (requestType != RequestType.MULTIPART) {
                        throw parameterError(i, "@Part parameters can only be used with multipart encoding.");
                    }
                    gotPart = true;
                } else if (annType == Param.class) {
//                    if (requestType != RequestType.MULTIPART || requestType != RequestType.FORM_URL_ENCODED) {
//                        throw parameterError(i, "@Parm parameters can only be used with multipart encoding or form encoding.");
//                    }
                    gotParam = true;
                } else if (annType == Body.class) {
                    if (requestType != RequestType.SIMPLE) {
                        throw parameterError(i,
                                "@Body parameters cannot be used with form or multi-part encoding.");
                    }
                    if (gotBody) {
                        throw methodError("Multiple @Body method annotations found.");
                    }

                    gotBody = true;
                } else {
                    hasAnnotation = false;
                }
                requestParamAnnotations[i] = annotation;
            }
            if (!hasAnnotation) {
                throw methodError("Must has @Field、@Part、@Query or @Path at " + i + " Parameters");
            }
        }
        if (requestType == RequestType.SIMPLE && !requestHasBody && gotBody) {
            throw methodError("Non-body HTTP method cannot contain @Body or @TypedOutput.");
        }
        if (requestType == RequestType.FORM_URL_ENCODED && (!gotField && !gotParam)) {
            throw methodError("Form-encoded method must contain at least one @Field or @Param.");
        }
        if (requestType == RequestType.MULTIPART && (!gotPart && !gotParam)) {
            throw methodError("Multipart method must contain at least one @Part or @Param.");
        }
        this.requestParamAnnotations = requestParamAnnotations;
    }

    private void validatePathName(int index, String name) {
        if (!PARAM_NAME_REGEX.matcher(name).matches()) {
            throw parameterError(index, "@Path parameter name must match %s. Found: %s",
                    PARAM_URL_REGEX.pattern(), name);
        }
        // Verify URL replacement name is actually present in the URL path.
        if (!requestUrlParamNames.contains(name)) {
            throw parameterError(index, "URL \"%s\" does not contain \"{%s}\".", requestUrl, name);
        }
    }

    private RuntimeException parameterError(int index, String message, Object... args) {
        return methodError(message + " (parameter #" + (index + 1) + ")", args);
    }

    private void parseMethodAnnotations() {
        Annotation[] annotations = method.getAnnotations();
        for (Annotation annotation : annotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            RestMethod methodInfo = null;
            for (Annotation innerAnnotation : annotationType.getAnnotations()) {
                if (RestMethod.class == innerAnnotation.annotationType()) {
                    methodInfo = (RestMethod) innerAnnotation;
                    break;
                }
            }
            if (methodInfo != null) {
                if (requestMethod != null) {
                    throw methodError("Only one HTTP method is allowed. Found: %s and %s.", requestMethod,
                            methodInfo.value());
                }
                try {
                    String path = (String) annotationType.getMethod("value").invoke(annotation);
                    appendPath = path != null && !path.startsWith("http") && !path.startsWith("ftp") && !path.replaceAll("\\{.+?\\}", "").isEmpty();
                    callIntercept = (Boolean) annotationType.getMethod("intercept").invoke(annotation);
                    parsePath(path);
                    requestMethod = methodInfo.value();
                    requestHasBody = methodInfo.hasBody();
                    needEncode = needEncode();
                } catch (Exception e) {
                    throw methodError("Failed to extract String 'value' from @%s annotation.\n%s",
                            annotationType.getSimpleName(), e.getMessage());
                }
            } else if (annotationType == Headers.class) {
                String[] headersToParse = ((Headers) annotation).value();
                if (headersToParse.length == 0) {
                    throw methodError("@Headers annotation is empty.");
                }
                headers = parseHeaders(headersToParse);
            } else if (annotationType == FormUrlEncoded.class) {
                if (requestType != null) {
                    throw methodError("Only one encoding annotation is allowed.");
                }
                requestType = RequestType.FORM_URL_ENCODED;
            } else if (annotationType == NoneEncoded.class) {
                if (requestType != null) {
                    throw methodError("Only one encoding annotation is allowed.");
                }
                requestType = RequestType.SIMPLE;
            } else if (annotationType == Multipart.class) {
                if (requestType != null) {
                    throw methodError("Only one encoding annotation is allowed.");
                }
                requestType = RequestType.MULTIPART;
            } else if (annotationType == Filter.class) {
                filter = ((Filter) annotation).value();
            } else if (annotationType == Streaming.class) {
                if (responseObjectType != Response.class) {
                    throw methodError(
                            "Only methods having %s as data type are allowed to have @%s annotation.",
                            Response.class.getSimpleName(), Streaming.class.getSimpleName());
                }
                isStreaming = true;
            } else if (annotationType == org.henjue.library.hnet.anntoation.Endpoint.class) {
                endpointUrl = ((org.henjue.library.hnet.anntoation.Endpoint) annotation).value();
            }

        }
        if (requestType == null) {
            requestType = defaultRequestType;
        }
        if (requestMethod == null) {
            throw methodError("HTTP method annotation is required (e.g., @GET, @POST, etc.).");
        }
        if (!needEncode) {
            requestType = RequestType.SIMPLE;
        }
        if (!requestHasBody) {
            if (requestType == RequestType.MULTIPART) {
                throw methodError(
                        "Multipart can only be specified on HTTP methods with request body (e.g., @POST).");
            }
            if (requestType == RequestType.FORM_URL_ENCODED) {
                throw methodError("FormUrlEncoded can only be specified on HTTP methods with request body "
                        + "(e.g., @POST).");
            }
        }
    }

    /**
     * 处理path参数
     *
     * @param path
     */
    private void parsePath(String path) {
        if (appendPath && (path == null || path.length() == 0 || path.charAt(0) != '/')) {
            throw methodError("URL path \"%s\" must start with '/'.", path);
        }

        // Get the relative URL path and existing query string, if present.
        String url = path;
        String query = null;
        int question = path.indexOf('?');
        if (question != -1 && question < path.length() - 1) {
            url = path.substring(0, question);
            query = path.substring(question + 1);

            // Ensure the query string does not have any named parameters.
            Matcher queryParamMatcher = PARAM_URL_REGEX.matcher(query);
            if (queryParamMatcher.find()) {
                throw methodError("URL query string \"%s\" must not have replace block. For dynamic query"
                        + " parameters use @Query.", query);
            }
        }

        Set<String> urlParams = parsePathParameters(path);

        requestUrl = url;
        requestUrlParamNames = urlParams;
        requestQuery = query;
    }

    private ResponseType parseResponseType() {
        Type returnType = method.getGenericReturnType();
        Type lastArgType = null;
        Class<?> lastArgClass = null;
        Type[] parameterTypes = method.getGenericParameterTypes();
        if (parameterTypes.length > 0) {
            Type typeToCheck = parameterTypes[parameterTypes.length - 1];
            lastArgType = typeToCheck;
            if (typeToCheck instanceof ParameterizedType) {
                typeToCheck = ((ParameterizedType) typeToCheck).getRawType();
            }
            if (typeToCheck instanceof Class) {
                lastArgClass = (Class<?>) typeToCheck;
            }
        }

        boolean hasReturnType = returnType != void.class;
        boolean hasCallback = lastArgClass != null && Callback.class.isAssignableFrom(lastArgClass);

        // Check for invalid configurations.
        if (hasReturnType && hasCallback) {
            throw methodError("Must have return type or Callback as last argument, not both.");
        }
        if (!hasReturnType && !hasCallback) {
            throw methodError("Must have either a return type or Callback as last argument.");
        }
        if (hasReturnType) {
            responseObjectType = returnType;
            return ResponseType.OBJECT;
        }
        lastArgType = Types.getSupertype(lastArgType, Types.getRawType(lastArgType), Callback.class);
        if (lastArgType instanceof ParameterizedType) {
            responseObjectType = getParameterUpperBound((ParameterizedType) lastArgType);
            return ResponseType.VOID;
        }

        throw methodError("Last parameter must be of type Callback<X> or Callback<? super X>.");
    }

    private RuntimeException methodError(String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        return new IllegalArgumentException(
                method.getDeclaringClass().getSimpleName() + "." + method.getName() + ": " + message);
    }

    enum RequestType {
        /**
         * No content-specific logic required.
         */
        SIMPLE,
        /**
         * Multi-part request body.
         */
        MULTIPART,
        /**
         * Form URL-encoded request body.
         */
        FORM_URL_ENCODED
    }

    private enum ResponseType {
        VOID,
        OBSERVABLE,
        OBJECT
    }

    private boolean needEncode() {
        return !requestMethod.equalsIgnoreCase("get");
    }
}
