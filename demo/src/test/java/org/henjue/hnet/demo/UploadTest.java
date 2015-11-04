package org.henjue.hnet.demo;

import org.henjue.library.hnet.Callback;
import org.henjue.library.hnet.Response;
import org.henjue.library.hnet.exception.HNetError;
import org.henjue.library.hnet.typed.TypedFile;
import org.junit.Test;

import java.io.File;

/**
 * Created by android on 2015/11/4.
 */

public class UploadTest extends AbstractTest {
    private UploadService service;

    @Override
    protected void before() {
        service = client.create(UploadService.class);
    }

    @Test
    public void upload() {
        long uid = 1;
        String filename = "test.png";
        String filepath = "";
        service.upload(uid, filename, new TypedFile("images/png", new File(filepath)), new Callback.SimpleCallBack<String>() {
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
