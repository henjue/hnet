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


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TypedFile implements TypedInput, TypedOutput {
    private final String filename;
    protected final String mimeType;
    protected final InputStream in;
    private static final int BUFFER_SIZE = 4096;

    public TypedFile(InputStream in, String mimeType, String filename) {
        if (in == null) {
            throw new NullPointerException("InputStream");
        }
        if (mimeType == null) {
            throw new NullPointerException("mimeType");
        }
        if (filename == null) {
            throw new NullPointerException("filename");
        }
        this.mimeType = mimeType;
        this.in = in;
        this.filename = filename;
    }

    /**
     * Constructs a new typed file.
     *
     * @throws NullPointerException if file or mimeType is null
     */
    public TypedFile(File file, String mimeType) throws FileNotFoundException {
        this(new FileInputStream(file), mimeType, file.getName());
    }

    @Override
    public long length() {
        try {
            return in.available();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public String mimeType() {
        return mimeType;
    }

    @Override
    public String fileName() {
        return filename;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        try {
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } finally {
            in.close();
        }
    }

    @Override
    public InputStream in() throws IOException {
        return this.in;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof TypedFile) {
            TypedFile rhs = (TypedFile) o;
            return in.equals(rhs.in);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return in.hashCode();
    }

    @Override
    public String toString() {
        return in.toString() + " (" + mimeType() + ")";
    }
}
