package org.henjue.hnet.demo;

import org.henjue.library.hnet.Callback;
import org.henjue.library.hnet.Response;
import org.junit.Test;

/**
 * Created by android on 2015/11/4.
 */
public class DefaultTest extends AbstractTest {
    private PostService service;

    @Override
    protected void before() {
        service = client.create(PostService.class);
    }


    @Test
    public void testListAll() {
        testAddUser();
        testAddUser();
        String users = service.list();
        assert users != null;
    }

    @Test
    public void testAddUser() {
        service.AddUser("henjue33", new Callback.SimpleCallBack<String>() {
            @Override
            public void success(String s, Response response) {

            }
        });
    }

    @Test
    public void testGetUserInfo() {
        service.Info(959, new Callback.SimpleCallBack<String>() {
            @Override
            public void success(String s, Response response) {

            }
        });
    }
}
