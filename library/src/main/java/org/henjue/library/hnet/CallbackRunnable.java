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

import org.henjue.library.hnet.exception.HNetError;

import java.util.concurrent.Executor;

import static org.henjue.library.hnet.exception.HNetError.unexpectedError;

abstract class CallbackRunnable<T> implements Runnable {
    private final Callback<T> callback;
    private final Executor callbackExecutor;
    private final ErrorHandler errorHandler;

    CallbackRunnable(Callback<T> callback, Executor callbackExecutor, ErrorHandler errorHandler) {
        this.callback = callback;
        this.callbackExecutor = callbackExecutor;
        this.errorHandler = errorHandler;
    }

    @Override
    public void run() {
        try {
            callbackExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.start();
                }
            });
            final ResponseWrapper wrapper = obtainResponse();
            callbackExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.success((T) wrapper.responseBody, wrapper.response);
                    callback.end();
                }
            });
        } catch (HNetError e) {
            Throwable cause = errorHandler.handleError(e);
            final HNetError handled = cause == e ? e : unexpectedError(e.getUrl(), cause);
            callbackExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.failure(handled);
                    callback.end();
                }
            });
        }
    }

    public abstract ResponseWrapper obtainResponse();
}
