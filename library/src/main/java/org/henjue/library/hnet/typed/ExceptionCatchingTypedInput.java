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
package org.henjue.library.hnet.typed;

import java.io.IOException;
import java.io.InputStream;

public class ExceptionCatchingTypedInput implements TypedInput {
    private final TypedInput delegate;
    private final ExceptionCatchingInputStream delegateStream;

    public ExceptionCatchingTypedInput(TypedInput delegate) throws IOException {
        this.delegate = delegate;
        this.delegateStream = new ExceptionCatchingInputStream(delegate.in());
    }

    @Override
    public String mimeType() {
        return delegate.mimeType();
    }

    @Override
    public long length() {
        return delegate.length();
    }

    @Override
    public InputStream in() throws IOException {
        return delegateStream;
    }

    public IOException getThrownException() {
        return delegateStream.thrownException;
    }

    public boolean threwException() {
        return delegateStream.thrownException != null;
    }

    private static class ExceptionCatchingInputStream extends InputStream {
        private final InputStream delegate;
        private IOException thrownException;

        ExceptionCatchingInputStream(InputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            try {
                return delegate.read();
            } catch (IOException e) {
                thrownException = e;
                throw e;
            }
        }

        @Override
        public int read(byte[] buffer) throws IOException {
            try {
                return delegate.read(buffer);
            } catch (IOException e) {
                thrownException = e;
                throw e;
            }
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            try {
                return delegate.read(buffer, offset, length);
            } catch (IOException e) {
                thrownException = e;
                throw e;
            }
        }

        @Override
        public long skip(long byteCount) throws IOException {
            try {
                return delegate.skip(byteCount);
            } catch (IOException e) {
                thrownException = e;
                throw e;
            }
        }

        @Override
        public int available() throws IOException {
            try {
                return delegate.available();
            } catch (IOException e) {
                thrownException = e;
                throw e;
            }
        }

        @Override
        public void close() throws IOException {
            try {
                delegate.close();
            } catch (IOException e) {
                thrownException = e;
                throw e;
            }
        }

        @Override
        public synchronized void mark(int readLimit) {
            delegate.mark(readLimit);
        }

        @Override
        public synchronized void reset() throws IOException {
            try {
                delegate.reset();
            } catch (IOException e) {
                thrownException = e;
                throw e;
            }
        }

        @Override
        public boolean markSupported() {
            return delegate.markSupported();
        }
    }
}
