package org.henjue.hnet.demo;

import org.henjue.hnet.demo.model.User;
import org.henjue.library.hnet.Callback;
import org.henjue.library.hnet.anntoation.FormUrlEncoded;
import org.henjue.library.hnet.anntoation.Get;
import org.henjue.library.hnet.anntoation.Param;
import org.henjue.library.hnet.anntoation.Post;
import org.henjue.library.hnet.anntoation.Query;

import java.util.ArrayList;

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
    ArrayList<User> list();
}
