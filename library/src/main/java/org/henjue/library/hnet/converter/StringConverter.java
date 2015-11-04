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
package org.henjue.library.hnet.converter;

import org.henjue.library.hnet.MimeUtil;
import org.henjue.library.hnet.typed.TypedInput;
import org.henjue.library.hnet.typed.TypedOutput;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;

public class StringConverter implements Converter {
    private String charset;

    public StringConverter() {
        this("UTF-8");
    }

    public StringConverter(String charset) {
        this.charset = charset;
    }

    @Override
    public boolean match(Type type) {
        return String.class.equals(type);
    }

    @Override
    public Object fromBody(TypedInput body, Type type) {
        String charset = this.charset;
        if (body.mimeType() != null) {
            charset = MimeUtil.parseCharset(body.mimeType(), charset);
        }
        ByteArrayOutputStream bos = null;
        try {
            byte[] buffer = new byte[2048];
            int readBytes = 0;
            bos = new ByteArrayOutputStream();
            while ((readBytes = body.in().read(buffer)) > 0) {
                bos.write(buffer, 0, readBytes);
            }
            bos.flush();
            return new String(bos.toByteArray(), charset);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bos != null) {
                    bos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public TypedOutput toBody(Object object) {
        try {
            return new TextTypedOutput(object.toString(), charset);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class TextTypedOutput implements TypedOutput {
        private final byte[] bytes;
        private final String mimeType;

        TextTypedOutput(String text, String encode) throws UnsupportedEncodingException {
            this.bytes = text.getBytes(encode);
            this.mimeType = "text/plain;charset=" + encode;
        }

        @Override
        public String fileName() {
            return null;
        }

        @Override
        public String mimeType() {
            return mimeType;
        }

        @Override
        public long length() {
            return bytes.length;
        }

        @Override
        public void writeTo(OutputStream out) throws IOException {
            out.write(bytes);
        }
    }
}
