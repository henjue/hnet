package org.henjue.hnet.demo;

import org.henjue.library.hnet.Callback;
import org.henjue.library.hnet.anntoation.FormUrlEncoded;
import org.henjue.library.hnet.anntoation.Get;
import org.henjue.library.hnet.anntoation.Param;
import org.henjue.library.hnet.anntoation.Post;
import org.henjue.library.hnet.anntoation.Query;

/**
 * Created by android on 2015/11/4.
 */
@FormUrlEncoded
public interface PostService {
    @Post("/user")
    void AddUser(@Param("name") String name, Callback.SimpleCallBack<String> callBack);

    @Get("/user")
    void Info(@Query("id") long id, Callback.SimpleCallBack<String> callBack);

    @Get("/user/list")
    String list();
}
