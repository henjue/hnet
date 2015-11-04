package org.henjue.hnet.demo;

import org.henjue.library.hnet.Callback;
import org.henjue.library.hnet.Response;
import org.henjue.library.hnet.exception.HNetError;
import org.henjue.library.hnet.typed.TypedFile;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by android on 2015/11/4.
 */

public class FileTest extends AbstractTest {
    private UploadService service;

    @Override
    protected void before() {
        service = client.create(UploadService.class);
    }

    @Test
    public void download() {
        service.download(1, new Callback.SimpleCallBack<Response>() {
            @Override
            public void success(Response response, Response response2) {
                try {
                    InputStream in = response.getBody().in();
                    System.out.println(in.available());
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        service.downloadFullPath("http://192.168.199.224:8001/download", 1, new Callback.SimpleCallBack<Response>() {
            @Override
            public void success(Response response, Response response2) {

            }
        });
    }

    @Test
    public void upload() {
        long uid = 1;
        String filename = "test.png";
        TypedFile file = new TypedFile(RuntimeEnvironment.application.getResources().openRawResource(R.raw.test), "image/png", "test.png");

        service.upload(uid, filename, file, new Callback.SimpleCallBack<String>() {
            @Override
            public void success(String s, Response response) {
                System.out.println(s);
            }

            @Override
            public void failure(HNetError error) {
                super.failure(error);
                error.printStackTrace();
            }

            @Override
            public void end() {
                super.end();
            }
        });
    }

}
