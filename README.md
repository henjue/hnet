# hnet

## Android Studio (Gradle)
```gradle
    compile 'org.henjue.library:hnet:1.0.0_beta2'
```
## Eclipse
#### he he 


```java
@FormUrlEncoded
public interface UserService {
    //异步请求
    @Post("/user/login")
    void login(String name,String pwd,Callback<LoginResponse> callback);

    //同步请求
    @Post("/user/login")
    LoginResponse login2(String name,String pwd);
}
```

基于注解的Restful api 风格网络框架，用最少的代码实现网络请求

## 注解说明
```java
@Post 和 @Get 请求方式，参数为请求的path其他参数自己看注释或代码
```
```java
@Path 替换url中的{}部分
```
```java
@Query url后面get方法的参数，如?naame=henjue&password=12345
```
```java
@Field 表单中的参数
```
```java
@Param 和@Query、@Field功能一样，自动识别
```
```java
@FormUrlEncoded 表单方式提交数据，只能用与post方法，用@Field或@Param指定参数
```
```java
@Multipart Multi Part 提交，只能用于Post，用@Part或@Param指定参数
```
```java
@Post 和 @Get 请求方式，参数为请求的path其他参数自己看注释或代码
```
```java
@Endpoint 指定host（指定后此请求将不会使用默认的全局的Endpoint）
```
