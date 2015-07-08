package org.henjue.hnet.demo;

import org.henjue.library.hnet.Callback;
import org.henjue.library.hnet.anntoation.Filter;
import org.henjue.library.hnet.Response;
import org.henjue.library.hnet.anntoation.Endpoint;
import org.henjue.library.hnet.anntoation.FormUrlEncoded;
import org.henjue.library.hnet.anntoation.Get;
import org.henjue.library.hnet.anntoation.Headers;
import org.henjue.library.hnet.anntoation.Multipart;
import org.henjue.library.hnet.anntoation.Param;
import org.henjue.library.hnet.anntoation.Path;
import org.henjue.library.hnet.anntoation.Post;
import org.henjue.library.hnet.anntoation.Query;
import org.henjue.library.hnet.typed.TypedFile;

/**<p>
 * 活命
 * @Path 替换url中的{}部分
 * @Query url后面get方法的参数，如?naame=henjue&password=12345
 * @Field 表单中的参数
 * @Param 和@Query、@Field功能一样，自动识别
 * @FormUrlEncoded 表单方式提交数据，只能用与post方法，用@Field或@Param指定参数
 * @Multipart Multi Part 提交，只能用于Post，用@Part或@Param指定参数
 * @Post 和 @Get 请求方式，参数为请求的path其他参数自己看注释或代码
 * @Endpoint 指定host（指定后此请求将不会使用默认的全局的Endpoint）
 * </p>
 */
@FormUrlEncoded//默认类上面的注释将应用到所有没有写@FormUrlEncoded或者@Multipart的方法上
public interface TestService {
    @Get("/users/{username}/repos")
    void getByPath(@Path("username") String name, Callback<Object> callback);
    @Get("/users/repos")
    void getByQuery(@Query("username") String name, Callback<Object> callback);

    @Filter(DownloadFilter.class)
    @Post(value = "/hall/getallcontents",append = false)
    @Endpoint("http://www.baidu.com")
    void postForm(@Param("uid") long uid,
                  @Param("page") int page,
                  @Param("token") String token,
                  Callback<Object> callback);
    @Multipart
    @Post("/hall/finelist")
    void postMulti(@Param("page") int page,
                   @Param("token") String token,
                   Callback<Object> callback);
    @Multipart
    @Post("/file/upload")
    @Endpoint("http://192.168.199.224:8080/")
    void upload(@Param("token") String token, @Param("img") TypedFile file,
                Callback<Object> callback);
    @Multipart
    @Post("/file/upload")
    @Endpoint("http://192.168.199.224:8080/")
    void upload2(@Param("token") String token, @Param("img") TypedFile file, @Query("uid") int uid,
                 Callback<Object> callback);

    @Multipart
    @Post("/{type}/upload")
    @Endpoint("http://192.168.199.224:8080/")
    void upload3(@Param("token") String token, @Param("img") TypedFile file, @Query("uid") int uid, @Path("type") String type,
                 Callback<Object> callback);
    @Get("/download/{fileid}")
    @Headers({"Content-Type: image/jpeg"})
    void download(@Path("fileid") int id, Callback<Response> callback);

    @Get(value = "{host}",append = false,intercept = false)
    void download(@Path("host")String host,Callback<Object> callback);
}
