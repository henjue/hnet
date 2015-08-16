package org.henjue.library.hnet.typed;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.henjue.library.hnet.anntoation.FormUrlEncoded;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.text.BreakIterator;

public class TypedBitmap implements TypedInput, TypedOutput {
    public static class Builder {

        private final String name;
        private Bitmap.CompressFormat format;
        private final Bitmap bitmap;

        public Builder setMaxHeight(int maxHeight) {
            this.maxHeight = maxHeight;
            return this;
        }

        public Builder setFormat(Bitmap.CompressFormat format) {
            this.format = format;
            return this;
        }

        public Builder setMaxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Builder setMaxWidth(int maxWidth) {
            this.maxWidth = maxWidth;
            return this;
        }

        public Builder setAutoMatch(Resources res) {
            this.maxHeight = res.getDisplayMetrics().heightPixels;
            this.maxWidth = res.getDisplayMetrics().widthPixels;
            return this;
        }

        /**
         * 图片最大大小
         */
        private int maxSize = -1;
        /**
         * 图片宽
         */
        private int maxWidth = -1;
        /**
         * 图片高
         */
        private int maxHeight = -1;

        public Builder(Bitmap bitmap) {
            this(bitmap, String.valueOf(System.currentTimeMillis()), -1, -1);
        }

        public Builder(Bitmap bitmap, int maxWidth, int maxHeight) {
            this(bitmap, String.valueOf(System.currentTimeMillis()), maxWidth, maxHeight);
        }

        public Builder(Bitmap bitmap, String name, int maxWidth, int maxHeight) {
            this.name = name;
            this.bitmap = bitmap;
            this.maxWidth = maxWidth;
            this.maxHeight = maxHeight;
            this.format = Bitmap.CompressFormat.PNG;
        }

        public TypedBitmap build() {
            return new TypedBitmap(maxWidth, maxHeight, maxSize, bitmap, name, format);
        }

    }

    private final String name;
    private final Bitmap.CompressFormat format;
    private byte[] bytes;
    /**
     * 图片最大大小
     */
    private int maxSize = -1;
    /**
     * 图片宽
     */
    private int maxWidth = -1;
    /**
     * 图片高
     */
    private int maxHeight = -1;


    private TypedBitmap(int maxW, int maxH, int maxSize, Bitmap bitmap, String name, Bitmap.CompressFormat format) {
        this.name = name;
        this.format = format;
        this.maxSize = maxSize;
        if (maxW > 0 || maxH > 0) {
            this.maxWidth = maxW;
            this.maxHeight = maxH;
            bytes = getBytes(compress(bitmap.getWidth(), bitmap.getHeight(), bytes));
        } else {
            //throw new IllegalArgumentException("maxWidth > 0 and maxHeight > 0");
            bytes = getBytes(bitmap);
        }
    }

    @Override
    public String fileName() {
        if (format == Bitmap.CompressFormat.JPEG) {
            return name + ".jpg";
        } else if (format == Bitmap.CompressFormat.PNG) {
            return name + ".png";
        } else if (format == Bitmap.CompressFormat.WEBP) {
            return name + ".webp";
        } else {
            return name;
        }
    }

    @Override
    public String mimeType() {
        if (format == Bitmap.CompressFormat.JPEG) {
            return "image/jpeg";
        } else if (format == Bitmap.CompressFormat.PNG) {
            return "image/png";
        } else if (format == Bitmap.CompressFormat.WEBP) {
            return "image/webp";
        } else {
            return "application/octet-stream";
        }
    }

    @Override
    public long length() {
        if (bytes != null) {
            return bytes.length;
        }
        return 0;

    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        if (bytes != null) {
            outputStream.write(bytes);
        }
    }

    @Override
    public InputStream in() throws IOException {
        if (bytes != null) {
            return new ByteArrayInputStream(bytes);
        } else {
            return new ByteArrayInputStream(new byte[]{0});
        }
    }

    private Bitmap compress(int width, int height, byte[] bytes) {
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        newOpts.inJustDecodeBounds = false;
        //现在主流手机比较多是800*480分辨率，所以高和宽我们设置为
        float hh = maxHeight;//这里设置高度为800f
        float ww = maxWidth;//这里设置宽度为480f
        //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;//be=1表示不缩放
        if (width > height && width > ww) {//如果宽度大的话根据宽度固定大小缩放
            be = (int) (newOpts.outWidth / ww);
        } else if (width < height && height > hh) {//如果高度高的话根据宽度固定大小缩放
            be = (int) (newOpts.outHeight / hh);
        }
        if (be <= 0)
            be = 1;
        newOpts.inSampleSize = be;//设置缩放比例
        //重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了

        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, newOpts);
        return bitmap;
    }

    private byte[] getBytes(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(format, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 100;
        if (maxSize > 0) {
            byte[] bytes = baos.toByteArray();
            while (bytes.length / 1024 > 100) {    //循环判断如果压缩后图片是否大于100kb,大于继续压缩
                baos.reset();//重置baos即清空baos
                image.compress(format, options, baos);//这里压缩options%，把压缩后的数据存放到baos中
                options -= 10;//每次都减少10
            }
        }
        bytes = baos.toByteArray();
        try {
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }
}
