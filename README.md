# hnet
## 基于注解的Restful api 风格网络框架，用最少的代码实现网络请求


## Build:
![build status](https://travis-ci.org/henjue/hnet.svg?branch=master)

## Last Version:
[ ![Download](https://api.bintray.com/packages/henjue/maven/hnet/images/download.svg) ](https://bintray.com/henjue/maven/hnet/_latestVersion)

Use Document to [See](http://www.j99.so/2015/07/12/HNet-Android-Fast-Network-Framework-HNet-Android-%E7%BD%91%E7%BB%9C%E5%BF%AB%E9%80%9F%E5%BC%80%E5%8F%91%E6%A1%86%E6%9E%B6%E4%BD%BF%E7%94%A8%E6%95%99%E7%A8%8B/)!

## 关于代码混淆
在proguard-rules.pro中加入以下部分:

```
-keepattributes *Annotation*
-keep class org.henjue.library.hnet.anntoation.** {*;}
-keep class * implements org.henjue.library.hnet.RequestFilter {*;}
```

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
