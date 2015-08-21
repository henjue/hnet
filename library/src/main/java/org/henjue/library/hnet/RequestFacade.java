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

public interface RequestFacade {
    void addHeader(String name, String value);

    void addPathParam(String name, String value);

    void addEncodedPathParam(String name, String value);

    void addQueryParam(String name, String value);

    void addEncodedQueryParam(String name, String value);

    /**
     * 添加field/part/query/根据当前方法请求类型而确定
     *
     * @param name
     * @param obj
     */
    void add(String name, Object obj);

    String getPath();
}
