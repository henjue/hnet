package org.henjue.hnet.demo;

import org.henjue.library.hnet.Callback;
import org.henjue.library.hnet.Response;
import org.henjue.library.hnet.anntoation.Endpoint;
import org.henjue.library.hnet.anntoation.Get;
import org.henjue.library.hnet.anntoation.Multipart;
import org.henjue.library.hnet.anntoation.Param;
import org.henjue.library.hnet.anntoation.Path;
import org.henjue.library.hnet.anntoation.Post;
import org.henjue.library.hnet.anntoation.Query;
import org.henjue.library.hnet.typed.TypedFile;

/**
 * Created by android on 2015/11/4.
 */
@Multipart
public interface UploadService {
    @Post("/upload")
    void upload(@Query("uid") long Uid, @Param("filename") String filename, @Param("file") TypedFile file, Callback.SimpleCallBack<String> callback);

    @Get("{url}")
    void downloadFullPath(@Path("url") String url, @Query("fid") long fid, Callback.SimpleCallBack<Response> callback);

    @Get("/download")
    @Endpoint("http://192.168.199.224:8001/")
    void download(@Query("fid") long fid, Callback.SimpleCallBack<Response> callback);
}
