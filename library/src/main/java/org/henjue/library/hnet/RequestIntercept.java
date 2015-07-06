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


public interface RequestIntercept {
    void onComplite(RequestFacade request);
    void onStart(RequestFacade request);
    void onAdd(String name,Object value);
    RequestIntercept NONE = new RequestIntercept() {
        @Override public void onComplite(RequestFacade request) {
            // Do nothing.
        }

        @Override
        public void onStart(RequestFacade request) {

        }

        @Override
        public void onAdd(String name, Object value) {

        }
    };
}
