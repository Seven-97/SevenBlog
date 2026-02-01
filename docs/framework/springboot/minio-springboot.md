---
title: 整合Minio - 实现文件切片极速上传
category: 系统设计
tag:
  - SpringBoot
  - Minio
---





## 概述

官网地址：https://min.io/

文档地址：https://docs.min.io/

Minio是一款开源的对象存储服务器，它可以运行在多种操作系统上，包括Linux、Windows和MacOS等。它提供了一种简单、可扩展、高可用的对象存储解决方案，支持多种数据格式，包括对象、块和文件等。

以下是Minio的主要特点：

- 简单易用： Minio的安装和配置非常简单，只需要下载并运行相应的二进制文件即可。它提供了一个Web UI,可以通过界面管理存储桶和对象。
- 可扩展性： Minio可以轻松地扩展到多个节点，以提供高可用性和容错能力。它支持多种部署模式，包括单节点、主从复制和集群等。
- 高可用性： Minio提供了多种机制来保证数据的可靠性和可用性，包括冗余备份、数据复制和故障转移等。
- 安全性： Minio提供了多种安全机制来保护数据的机密性和完整性，包括SSL/TLS加密、访问控制和数据加密等。
- 多语言支持： Minio支持多种编程语言，包括Java、Python、Ruby和Go等。
- 社区支持： Minio是一个开源项目，拥有庞大的社区支持和贡献者。它的源代码可以在GitHub上获得，并且有一个活跃的邮件列表和论坛。
- 对象存储： Minio的核心功能是对象存储。它允许用户上传和下载任意数量和大小的对象，并提供了多种API和SDK来访问这些对象。
- 块存储： Minio还支持块存储，允许用户上传和下载大型文件(例如图像或视频)。块存储是一种快速、高效的方式来处理大型文件。
- 文件存储： Minio还支持文件存储，允许用户上传和下载单个文件。文件存储是一种简单、快速的方式来处理小型文件。

总之，Minio是一款强大、灵活、可扩展的对象存储服务器，适用于各种应用场景，包括云存储、大数据存储和物联网等。

## 应用场景

MinIO是一种高性能、扩展性好的对象存储系统，它可以适用于许多应用场景，其中包括但不限于以下几种：

- 大规模数据存储： 由于MinIO使用分布式环境来存储数据，因此可以轻松扩展以满足需要管理大量数据的组织和企业的需求。
- 图像和媒体存储： 由于MinIO对原始二进制数据进行了优化，因此非常适合存储图像、音频和视频等媒体文件。它还支持WebP、JPEG和PNG等格式，可在多种设备和浏览器上工作。
- 云原生应用程序： MinIO是一个云原生的对象存储系统，可以与Kubernetes、Docker Swarm和Mesosphere等容器编排工具无缝集成，可以很好地满足基于云的应用程序的需求。
- 数据保护和灾难恢复： MinIO的多副本写入功能和内置的纠删码支持，使得数据备份和恢复变得简单而强大。
- 分布式计算和机器学习： MinIO提供STS（S3 Select）和HDFS接口，支持在数据仓库中直接运行SQL查询和MapReduce等并行处理框架。这使得它成为用于Big Data、AI和ML等分布式计算任务的理想选择。

需要注意的是，以上列出的应用场景并不是MinIO所有可适用的场景。具体取决于每个使用情况的细节和需求。

## Minio实现分片上传的主要步骤

使用SpringBoot和MinIO实现分片上传、秒传、续传主要包含以下几个步骤：

- 前端选择文件并对其进行切割： 可以使用JavaScript等前端技术将文件切成多个片段，并为每个片段生成唯一标识。
- 将每个分片上传到MinIO对象存储： 调用MinIO的Java SDK将每个分片上传到MinIO中，每个分片的KEY名称包含基础名称和片段ID。
- 将所有分片合并成最终文件： 在前端完成所有分片的上传之后，在后台开发一个接口，按照唯一标识将所有分片合并成最终文件。合并过程可以在应用服务器上完成，也可以使用MinIO Object Storage本身的合并功能完成。
- 实现秒传： 在前端上传分片之前，通过请求后台接口来根据文件名称和文件MD5值判断该文件是否已经存在，如果存在则可以直接返回文件URL，即可实现秒传。
- 实现续传： 在前端上传分片时出现了网络问题或客户端故障导致文件上传被中断，这时候只需记录已上传的分片序列号和状态标志，从下一个分片重新开始上传即可。
- 处理错误和异常： 在文件上传过程中可能会遇到各种问题，比如服务故障、网络中断、客户端处理超时等。因此需要加入错误和异常处理，保证整个上传过程顺利进行。

总体而言，使用SpringBoot和MinIO实现分片上传、秒传、续传的难度不算大，可以根据上述步骤进行开发和实现。



## SpringBoot整合

### 引入依赖

```xml
<!-- 操作minio的java客户端-->
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.2</version>
</dependency>
<!-- 操作minio的java客户端-->
 <dependency>
    <groupId>io.minio</groupId>
     <artifactId>minio</artifactId>
     <version>8.2.1</version>
</dependency>
<!--        jwt鉴权相应依赖-->
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.2.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.2</version>
</dependency>
```

### 创建容器桶

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406301042156.webp)

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406301042182.webp)

### 获取API访问凭证

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406301042197.webp)

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406301042149.webp)

### 编写配置文件

```properties
server:
    port: 8080
spring:
    servlet:
        multipart:
            max-file-size: 10MB
            max-request-size: 10MB
    #minio配置
    minio:
        access-key: dAMaxkWaXUD1CV1JHbqw
        secret-key: AXt3SD0JFkDENFbMeJKOOQb5wj8KvabZWu33Rs84
        url: http://192.168.18.14:9090  #访问地址
        bucket-name: wanghui
```

首先是服务器的配置：

- 端口号为8080,用于监听请求。
- 使用了一个Servlet来处理`multipart/form-data`类型的请求。
- 在接收到`multipart/form-data`类型的请求时，会将上传的文件大小限制在10MB以内，并将请求大小限制在10MB以内。

接下来是minio的配置：

- `access-key`和`secret-key`是访问minio服务的凭证，需要根据实际情况进行填写。
- url是minio服务的地址，需要根据实际情况进行填写。
- `bucket-name`是存储文件的桶名，需要根据实际情况进行填写。

### http请求状态

```java
public class HttpStatus{
    /**
     * 操作成功
     */
    public static final int SUCCESS = 200;

    /**
     * 对象创建成功
     */
    public static final int CREATED = 201;

    /**
     * 请求已经被接受
     */
    public static final int ACCEPTED = 202;

    /**
     * 操作已经执行成功，但是没有返回数据
     */
    public static final int NO_CONTENT = 204;

    /**
     * 资源已被移除
     */
    public static final int MOVED_PERM = 301;

    /**
     * 重定向
     */
    public static final int SEE_OTHER = 303;

    /**
     * 资源没有被修改
     */
    public static final int NOT_MODIFIED = 304;

    /**
     * 参数列表错误（缺少，格式不匹配）
     */
    public static final int BAD_REQUEST = 400;

    /**
     * 未授权
     */
    public static final int UNAUTHORIZED = 401;

    /**
     * 访问受限，授权过期
     */
    public static final int FORBIDDEN = 403;

    /**
     * 资源，服务未找到
     */
    public static final int NOT_FOUND = 404;

    /**
     * 不允许的http方法
     */
    public static final int BAD_METHOD = 405;

    /**
     * 资源冲突，或者资源被锁
     */
    public static final int CONFLICT = 409;

    /**
     * 不支持的数据，媒体类型
     */
    public static final int UNSUPPORTED_TYPE = 415;

    /**
     * 系统内部错误
     */
    public static final int ERROR = 500;

    /**
     * 接口未实现
     */
    public static final int NOT_IMPLEMENTED = 501;

    /**
     * 系统警告消息
     */
    public static final int WARN = 601;
}
```

### 通用常量信息

```java
import io.jsonwebtoken.Claims;

public class Constants{
    /**
     * UTF-8 字符集
     */
    public static final String UTF8 = "UTF-8";

    /**
     * GBK 字符集
     */
    public static final String GBK = "GBK";

    /**
     * www主域
     */
    public static final String WWW = "www.";

    /**
     * http请求
     */
    public static final String HTTP = "http://";

    /**
     * https请求
     */
    public static final String HTTPS = "https://";

    /**
     * 通用成功标识
     */
    public static final String SUCCESS = "0";

    /**
     * 通用失败标识
     */
    public static final String FAIL = "1";

    /**
     * 登录成功
     */
    public static final String LOGIN_SUCCESS = "Success";

    /**
     * 注销
     */
    public static final String LOGOUT = "Logout";

    /**
     * 注册
     */
    public static final String REGISTER = "Register";

    /**
     * 登录失败
     */
    public static final String LOGIN_FAIL = "Error";
 
    /**
     * 验证码有效期（分钟）
     */
    public static final Integer CAPTCHA_EXPIRATION = 2;

    /**
     * 令牌
     */
    public static final String TOKEN = "token";

    /**
     * 令牌前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * 令牌前缀
     */
    public static final String LOGIN_USER_KEY = "login_user_key";

    /**
     * 用户ID
     */
    public static final String JWT_USERID = "userid";

    /**
     * 用户名称
     */
    public static final String JWT_USERNAME = Claims.SUBJECT;

    /**
     * 用户头像
     */
    public static final String JWT_AVATAR = "avatar";

    /**
     * 创建时间
     */
    public static final String JWT_CREATED = "created";

    /**
     * 用户权限
     */
    public static final String JWT_AUTHORITIES = "authorities";

    /**
     * 资源映射路径 前缀
     */
    public static final String RESOURCE_PREFIX = "/profile";

    /**
     * RMI 远程方法调用
     */
    public static final String LOOKUP_RMI = "rmi:";

    /**
     * LDAP 远程方法调用
     */
    public static final String LOOKUP_LDAP = "ldap:";

    /**
     * LDAPS 远程方法调用
     */
    public static final String LOOKUP_LDAPS = "ldaps:";

    /**
     * 定时任务白名单配置（仅允许访问的包名，如其他需要可以自行添加）
     */
    public static final String[] JOB_WHITELIST_STR = { "com.ruoyi" };

    /**
     * 定时任务违规的字符
     */
    public static final String[] JOB_ERROR_STR = { "java.net.URL", "javax.naming.InitialContext", "org.yaml.snakeyaml",
            "org.springframework", "org.apache", "com.ruoyi.common.utils.file", "com.ruoyi.common.config" };
}
```

### 创建Minio的配置类

```java
import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.minio")
public class MinioConfig {
    private String accessKey;

    private String secretKey;

    private String url;

    private String bucketName;

    @Bean
    public MinioClient minioClient(){
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey,secretKey)
                .build();
    }
}
```

这段代码是Java中的一个配置类，用于配置与MinIO(一个对象存储服务)相关的属性。具体来说：

- `@Configuration`注解表示这是一个配置类，用于将该类中定义的属性注入到其他组件中使用。
- `@ConfigurationProperties`注解表示该类使用了`spring.minio.*`前缀的属性来配置Minio相关的属性。
- `@Data`注解表示自动生成getter和setter方法，简化了代码编写。
- `accessKey`和`secretKey`属性分别表示访问密钥和密钥值，用于连接到MinIO服务。
- url属性表示MinIO服务的URL地址。
- `bucketName`属性表示存储桶名称。
- `@Bean`注解表示将`minioClient()`方法返回的对象注册为bean,以便在其他组件中使用。
- `minioClient()`方法返回了一个`MinioClient`对象，用于连接到MinIO服务并操作存储桶。其中，`endpoint()`方法用于设置MinIO服务的URL地址，`credentials()`方法用于设置访问密钥和密钥值。

### 创建Minio的工具类

```java
import com.xiaohui.config.MinioConfig;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.SneakyThrows;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class MinioUtils {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfig configuration;

    /**
     * @param name 名字
     * @return boolean
     * @Description description: 判断bucket是否存在，不存在则创建
     */
    public boolean existBucket(String name) {
        boolean exists;
        try {
            exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(name).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(name).build());
                exists = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            exists = false;
        }
        return exists;
    }

    /**
     * @param bucketName 存储bucket名称
     * @return {@link Boolean }
     * @Description 创建存储bucket
     */
    public Boolean makeBucket(String bucketName) {
        try {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * @param bucketName 存储bucket名称
     * @return {@link Boolean }
     * @Description 删除存储bucket
     */
    public Boolean removeBucket(String bucketName) {
        try {
            minioClient.removeBucket(RemoveBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * @param fileName 文件名称
     * @param time     时间
     * @return {@link Map }
     * @Description 获取上传临时签名
     */
    @SneakyThrows
    public Map getPolicy(String fileName, ZonedDateTime time) {
        PostPolicy postPolicy = new PostPolicy(configuration.getBucketName(), time);
        postPolicy.addEqualsCondition("key", fileName);
        try {
            Map<String, String> map = minioClient.getPresignedPostFormData(postPolicy);
            HashMap<String, String> map1 = new HashMap<>();
            map.forEach((k, v) -> {
                map1.put(k.replaceAll("-", ""), v);
            });
            map1.put("host", configuration.getUrl() + "/" + configuration.getBucketName());
            return map1;
        } catch (ErrorResponseException e) {
            e.printStackTrace();
        } catch (InsufficientDataException e) {
            e.printStackTrace();
        } catch (InternalException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidResponseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (XmlParserException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param objectName 对象名称
     * @param method     方法
     * @param time       时间
     * @param timeUnit   时间单位
     * @return {@link String }
     * @Description 获取上传文件的url
     */
    public String getPolicyUrl(String objectName, Method method, int time, TimeUnit timeUnit) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(method)
                    .bucket(configuration.getBucketName())
                    .object(objectName)
                    .expiry(time, timeUnit).build());
        } catch (ErrorResponseException e) {
            e.printStackTrace();
        } catch (InsufficientDataException e) {
            e.printStackTrace();
        } catch (InternalException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidResponseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (XmlParserException e) {
            e.printStackTrace();
        } catch (ServerException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * @param file     文件
     * @param fileName 文件名称
     * @Description 上传文件
     */
    public void upload(MultipartFile file, String fileName) {
        // 使用putObject上传一个文件到存储桶中。
        try {
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(configuration.getBucketName())
                    .object(fileName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (ErrorResponseException e) {
            e.printStackTrace();
        } catch (InsufficientDataException e) {
            e.printStackTrace();
        } catch (InternalException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidResponseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (XmlParserException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param objectName 对象名称
     * @param time       时间
     * @param timeUnit   时间单位
     * @return {@link String }
     * @Description 根据filename获取文件访问地址
     */
    public String getUrl(String objectName, int time, TimeUnit timeUnit) {
        String url = null;
        try {
            url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(configuration.getBucketName())
                    .object(objectName)
                    .expiry(time, timeUnit).build());
        } catch (ErrorResponseException e) {
            e.printStackTrace();
        } catch (InsufficientDataException e) {
            e.printStackTrace();
        } catch (InternalException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidResponseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (XmlParserException e) {
            e.printStackTrace();
        } catch (ServerException e) {
            e.printStackTrace();
        }
        return url;
    }

    /**
     * @param fileName
     * @return {@link ResponseEntity }<{@link byte[] }>
     * @Description description: 下载文件
     */
    public ResponseEntity<byte[]> download(String fileName) {
        ResponseEntity<byte[]> responseEntity = null;
        InputStream in = null;
        ByteArrayOutputStream out = null;
        try {
            in = minioClient.getObject(GetObjectArgs.builder().bucket(configuration.getBucketName()).object(fileName).build());
            out = new ByteArrayOutputStream();
            IOUtils.copy(in, out);
            //封装返回值
            byte[] bytes = out.toByteArray();
            HttpHeaders headers = new HttpHeaders();
            try {
                headers.add("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            headers.setContentLength(bytes.length);
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setAccessControlExposeHeaders(Arrays.asList("*"));
            responseEntity = new ResponseEntity<byte[]>(bytes, headers, HttpStatus.SUCCESS);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return responseEntity;
    }

    /**
     * @param objectFile 对象文件
     * @return {@link String }
     * @Description 根据文件名和桶获取文件路径
     */
    public String getFileUrl(String objectFile) {
        try {

            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(configuration.getBucketName())
                    .object(objectFile)
                    .build()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


}
```

该代码是一个工具类，用于使用阿里云的对象存储服务(OSS)进行文件上传和下载。具体功能如下：

- `getPolicy(String fileName, ZonedDateTime time)`：根据文件名和时间戳获取上传临时签名。
- `getPolicyUrl(String objectName, Method method, int time, TimeUnit timeUnit)`：获取上传文件的url。
- `upload(MultipartFile file, String fileName)`：将文件上传到OSS中。
- `getUrl(String objectName, int time, TimeUnit timeUnit)`：获取文件的下载url。

代码中使用了枚举类型来定义不同的上传和下载方法。

使用了注解`@Autowired`来自动注入`MinioClient`对象。

该工具类没有提供异常处理机制，需要根据实际情况进行补充。

### 创建Ajax请求工具类

```java
/**
 * @Description ajax结果
 */
public class AjaxResult extends HashMap<String, Object>
{
    private static final long serialVersionUID = 1L;

    /** 状态码 */
    public static final String CODE_TAG = "code";

    /** 返回内容 */
    public static final String MSG_TAG = "msg";

    /** 数据对象 */
    public static final String DATA_TAG = "data";

    /**
     * 初始化一个新创建的 AjaxResult 对象，使其表示一个空消息。
     */
    public AjaxResult()
    {
    }

    /**
     * 初始化一个新创建的 AjaxResult 对象
     *
     * @param code 状态码
     * @param msg 返回内容
     */
    public AjaxResult(int code, String msg)
    {
        super.put(CODE_TAG, code);
        super.put(MSG_TAG, msg);
    }

    /**
     * 初始化一个新创建的 AjaxResult 对象
     *
     * @param code 状态码
     * @param msg 返回内容
     * @param data 数据对象
     */
    public AjaxResult(int code, String msg, Object data)
    {
        super.put(CODE_TAG, code);
        super.put(MSG_TAG, msg);
        if (data!=null)
        {
            super.put(DATA_TAG, data);
        }
    }

    /**
     * 返回成功消息
     *
     * @return 成功消息
     */
    public static AjaxResult success()
    {
        return AjaxResult.success("操作成功");
    }

    /**
     * 返回成功数据
     *
     * @return 成功消息
     */
    public static AjaxResult success(Object data)
    {
        return AjaxResult.success("操作成功", data);
    }

    /**
     * 返回成功消息
     *
     * @param msg 返回内容
     * @return 成功消息
     */
    public static AjaxResult success(String msg)
    {
        return AjaxResult.success(msg, null);
    }

    /**
     * 返回成功消息
     *
     * @param msg 返回内容
     * @param data 数据对象
     * @return 成功消息
     */
    public static AjaxResult success(String msg, Object data)
    {
        return new AjaxResult(HttpStatus.SUCCESS, msg, data);
    }

    /**
     * 返回警告消息
     *
     * @param msg 返回内容
     * @return 警告消息
     */
    public static AjaxResult warn(String msg)
    {
        return AjaxResult.warn(msg, null);
    }

    /**
     * 返回警告消息
     *
     * @param msg 返回内容
     * @param data 数据对象
     * @return 警告消息
     */
    public static AjaxResult warn(String msg, Object data)
    {
        return new AjaxResult(HttpStatus.WARN, msg, data);
    }

    /**
     * 返回错误消息
     *
     * @return 错误消息
     */
    public static AjaxResult error()
    {
        return AjaxResult.error("操作失败");
    }

    /**
     * 返回错误消息
     *
     * @param msg 返回内容
     * @return 错误消息
     */
    public static AjaxResult error(String msg)
    {
        return AjaxResult.error(msg, null);
    }

    /**
     * 返回错误消息
     *
     * @param msg 返回内容
     * @param data 数据对象
     * @return 错误消息
     */
    public static AjaxResult error(String msg, Object data)
    {
        return new AjaxResult(HttpStatus.ERROR, msg, data);
    }

    /**
     * 返回错误消息
     *
     * @param code 状态码
     * @param msg 返回内容
     * @return 错误消息
     */
    public static AjaxResult error(int code, String msg)
    {
        return new AjaxResult(code, msg, null);
    }

    /**
     * 方便链式调用
     *
     * @param key 键
     * @param value 值
     * @return 数据对象
     */
    @Override
    public AjaxResult put(String key, Object value)
    {
        super.put(key, value);
        return this;
    }
}
```

### 创建Minio文件操作接口层

```java
import com.xiaohui.utils.AjaxResult;
import com.xiaohui.utils.MinioUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;

/**
 * @Description minio文件上传控制器
 */
@CrossOrigin
@RestController
@RequestMapping("/api")
public class MinioFileUploadController {
    @Autowired
    private MinioUtils minioUtils;

    /**
     * @param file     文件
     * @param fileName 文件名称
     * @return {@link AjaxResult }
     * @Description 上传文件
     */
    @GetMapping("/upload")
    public AjaxResult uploadFile(@RequestParam("file") MultipartFile file, String fileName) {

        minioUtils.upload(file, fileName);
        return AjaxResult.success("上传成功");

    }

    /**
     * @param fileName 文件名称
     * @return {@link ResponseEntity }
     * @Description dowload文件
     */
    @GetMapping("/dowload")
    public ResponseEntity dowloadFile(@RequestParam("fileName") String fileName) {
        return minioUtils.download(fileName);
    }

    /**
     * @param fileName 文件名称
     * @return {@link AjaxResult }
     * @Description 得到文件url
     */
    @GetMapping("/getUrl")
    public AjaxResult getFileUrl(@RequestParam("fileName") String fileName){
        HashMap map=new HashMap();
        map.put("FileUrl",minioUtils.getFileUrl(fileName));
        return AjaxResult.success(map);
    }
}
```

<!-- @include: @article-footer.snippet.md -->     