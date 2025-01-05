---
title: OAuth2详解
category: 系统设计
tag:
  - 认证授权
---



## 介绍


### 前言  

传统的客户端-服务器身份验证模型中存在的问题。在这种模型中，客户端通过使用资源所有者的凭据对服务器进行身份验证，从而请求访问受限资源（受保护的资源）。为了使第三方应用程序能够访问受限资源，资源所有者需与第三方共享其凭据。然而，这种做法存在一些问题和限制：  

- 第三方应用程序通常需要明文存储资源所有者的凭据（通常是密码），以备将来使用。  
- 服务器需要支持密码身份验证，而且密码身份验证存在安全弱点。  
- 第三方应用程序可能获得对资源所有者受保护资源的过于广泛的访问权限，而资源所有者无法限制对资源的访问时长或访问的资源子集。  
- 资源所有者无法单独撤销对个别第三方的访问权限，而不影响所有第三方的访问权限，只能通过更改第三方的密码来执行此操作。  
- 如果任何第三方应用程序遭到破坏，将导致最终用户密码以及由该密码保护的所有数据的泄露。

### 什么是OAuth 2.0  
OAuth 2.0（开放授权2.0）是一种用于授权的开放标准，允许用户让第三方应用访问他们在某一网站上存储的私有资源，而无需将用户名和密码提供给第三方应用。OAuth 2.0是OAuth协议的升级版本，提供了更简化和灵活的授权流程。  

在OAuth 2.0中，授权过程包括以下主要角色：  
- 资源所有者（Resource Owner）： 即用户，是拥有受保护资源的实体。用户通过授权第三方应用访问他们的资源。  
- 客户端（Client）： 即第三方应用，需要访问资源所有者的受保护资源。  
- 授权服务器（Authorization Server）： 负责验证资源所有者并颁发访问令牌给客户端。授权服务器和资源服务器可以是同一个服务或不同的服务。  
- 资源服务器（Resource Server）： 存储受保护资源的服务器，通过访问令牌验证并提供受保护资源。  

在OAuth 2.0中，定义了多种授权方式（授权码模式、隐式授权模式、密码模式、客户端凭证模式等），客户端通过与授权服务器交互，获取访问令牌，然后使用访问令牌访问受保护资源。

### 应用场景

假如你正在“网站A”上冲浪，看到一篇帖子表示非常喜欢，当你情不自禁的想要点赞时，它会提示你进行登录操作。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092006179.webp)

打开登录页面你会发现，除了最简单的账户密码登录外，还为我们提供了微博、微信、QQ等快捷登录方式。假设选择了快捷登录，它会提示我们扫码或者输入账号密码进行登录。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092013550.webp)

登录成功之后便会将QQ/微信的昵称和头像等信息回填到“网站A”中，此时你就可以进行点赞操作了。



### 名词定义

在详细讲解`oauth2`之前，先来了解一下它里边用到的名词定义吧：

- Client：客户端，它本身不会存储用户快捷登录的账号和密码，只是通过资源拥有者的授权去请求资源服务器的资源，即例子中的网站A；
- Resource Owner：资源拥有者，通常是用户，即例子中拥有QQ/微信账号的用户；
- Authorization Server：认证服务器，可以提供身份认证和用户授权的服务器，即给客户端颁发`token`和校验`token`；
- Resource Server：资源服务器，存储用户资源的服务器，即例子中的QQ/微信存储的用户信息；



### 认证流程

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092013863.webp)

如图是`oauth2`官网的认证流程图，我们来分析一下：

- A 客户端向资源拥有者发送授权申请；
- B 资源拥有者同意客户端的授权，返回授权码；
- C 客户端使用授权码向认证服务器申请令牌`token`；
- D 认证服务器对客户端进行身份校验，认证通过后发放令牌；
- E 客户端拿着认证服务器颁发的令牌去资源服务器请求资源；
- F 资源服务器校验令牌的有效性，返回给客户端资源信息；



流程如下图：

![图片](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092013865.webp)



## 实战

在正式开始搭建项目之前我们先来做一些准备工作：要想使用`oauth2`的服务，得先创建几张表。

### 数据库

`oauth2`相关的建表语句可以参考官方初始化sql，也可以查看项目中的**init.sql**文件



表结构，字段的含义如下：

- oauth_client_details：存储客户端的配置信息，操作该表的类主要是`JdbcClientDetailsService.java`；
- oauth_access_token：存储生成的令牌信息，操作该表的类主要是`JdbcTokenStore.java`；
- oauth_client_token：在客户端系统中存储从服务端获取的令牌数据，操作该表的类主要是`JdbcClientDetailsService.java`；
- oauth_code：存储授权码信息与认证信息，即只有`grant_type`为`authorization_code`时，该表才会有数据，操作该表的类主要是`JdbcAuthorizationCodeServices.java`；
- oauth_approvals：存储用户的授权信息；
- oauth_refresh_token：存储刷新令牌的`refresh_token`，如果客户端的`grant_type`不支持`refresh_token`，那么不会用到这张表，操作该表的类主要是`JdbcTokenStore`；



在`oauth_client_details`表中添加一条数据

```sql
client_id:cheetah_one //客户端名称，必须唯一
resource_ids:product_api //客户端所能访问的资源id集合,多个资源时用逗号(,)分隔
client_secret:$2a$10$h/TmLPvXozJJHXDyJEN22ensJgaciomfpOc9js9OonwWIdAnRQeoi //客户端的访问密码
scope:read,write //客户端申请的权限范围,可选值包括read,write,trust。若有多个权限范围用逗号(,)分隔
authorized_grant_types:client_credentials,implicit,authorization_code,refresh_token,password //指定客户端支持的grant_type,可选值包括authorization_code,password,refresh_token,implicit,client_credentials, 若支持多个grant_type用逗号(,)分隔
web_server_redirect_uri:http://www.baidu.com //客户端的重定向URI,可为空, 当grant_type为authorization_code或implicit时, 在Oauth的流程中会使用并检查与注册时填写的redirect_uri是否一致
access_token_validity:43200 //设定客户端的access_token的有效时间值(单位:秒),可选, 若不设定值则使用默认的有效时间值(60 * 60 * 12, 12小时)
autoapprove:false //设置用户是否自动Approval操作, 默认值为 'false', 可选值包括 'true','false', 'read','write'
```



### 依赖引入

```xml
<dependency>
 <groupId>org.springframework.boot</groupId>
 <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
 <groupId>org.springframework.cloud</groupId>
 <artifactId>spring-cloud-starter-security</artifactId>
</dependency>
<dependency>
 <groupId>org.springframework.cloud</groupId>
 <artifactId>spring-cloud-starter-oauth2</artifactId>
</dependency>
<dependency>
 <groupId>org.springframework.security</groupId>
 <artifactId>spring-security-jwt</artifactId>
</dependency>
```



### 资源服务

模拟资源访问

```java
@RestController
@RequestMapping("/product")
public class ProductController {

    @GetMapping("/findAll")
    public String findAll(){
        return "产品列表查询成功";
    }
}
```

接着创建配置类继承`ResourceServerConfigurerAdapter`并增加`@EnableResourceServer`注解开启资源服务，重写两个`configure`方法

```java
/**
 * 指定token的持久化策略
 * InMemoryTokenStore 表示将token存储在内存中
 * RedisTokenStore 表示将token存储在redis中
 * JdbcTokenStore 表示将token存储在数据库中
 * @return
 */
@Bean
public TokenStore jdbcTokenStore(){
    return new JdbcTokenStore(dataSource);
}

/**
 * 指定当前资源的id和token的存储策略
 * @param resources
 * @throws Exception
 */
@Override
public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
    //此处的id可以写在配置文件中，这里我们先写死
 resources.resourceId("product_api").tokenStore(jdbcTokenStore());
}


/**
 * 设置请求权限和header处理
 * @param http
 * @throws Exception
 */
@Override
public void configure(HttpSecurity http) throws Exception {
 //固定写法
 http.authorizeRequests()
   //指定不同请求方式访问资源所需的权限，一般查询是read，其余都是write
   .antMatchers(HttpMethod.GET,"/**").access("#oauth2.hasScope('read')")
   .antMatchers(HttpMethod.POST,"/**").access("#oauth2.hasScope('write')")
   .antMatchers(HttpMethod.PATCH,"/**").access("#oauth2.hasScope('write')")
   .antMatchers(HttpMethod.PUT,"/**").access("#oauth2.hasScope('write')")
   .antMatchers(HttpMethod.DELETE,"/**").access("#oauth2.hasScope('write')")
   .and()
   .headers().addHeaderWriter((request,response) -> {
    //域名不同或者子域名不一样并且是ajax请求就会出现跨域问题
    //允许跨域
    response.addHeader("Access-Control-Allow-Origin","*");
    //跨域中会出现预检请求，如果不能通过，则真正请求也不会发出
    //如果是跨域的预检请求，则原封不动向下传递请求头信息，否则预检请求会丢失请求头信息（主要是token信息）
    if(request.getMethod().equals("OPTIONS")){
     response.setHeader("Access-Control-Allow-Methods",request.getHeader("Access-Control-Allow-Methods"));
     response.setHeader("Access-Control-Allow-Headers",request.getHeader("Access-Control-Allow-Headers"));
    }
 });
}
```

当然我们也可以配置忽略校验的`url`，在上边的`public void configure(HttpSecurity http) throws Exception`中进行配置

```java
ExpressionUrlAuthorizationConfigurer<HttpSecurity>
  .ExpressionInterceptUrlRegistry config = http.requestMatchers().anyRequest()
  .and()
  .authorizeRequests();
properties.getUrls().forEach(e -> {
 config.antMatchers(e).permitAll();
});
```

然后将实现了`UserDetails`的`SysUser`和实现了`GrantedAuthority`的`SysRole`放到项目中，当请求发过来时，`oauth2`会帮我们自行校验。



### 认证服务

配置文件对服务端口、应用名称、数据库、`mybatis`和日志进行了配置。



**Security配置**

①将继承了`UserDetailsService`的`ISysUserService`的实现类`SysUserServiceImpl`重写`loadUserByUsername`方法

```java
@Override
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
 return this.baseMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
}
```

②继承`WebSecurityConfigurerAdapter`类，增加`@EnableWebSecurity`注解并重写方法

```java
/**
 * 指定认证对象的来源和加密方式
 * @param auth
 * @throws Exception
 */
@Override
public void configure(AuthenticationManagerBuilder auth) throws Exception {
 auth.userDetailsService(userService).passwordEncoder(passwordEncoder());
}

/**
 * 安全拦截机制（最重要）
 * @param httpSecurity
 * @throws Exception
 */
@Override
public void configure(HttpSecurity httpSecurity) throws Exception {
 httpSecurity
   //CSRF禁用，因为不使用session
   .csrf().disable()
   .authorizeRequests()
   //登录接口和静态资源不需要认证
   .antMatchers("/login*","/css/*").permitAll()
   //除上面的所有请求全部需要认证通过才能访问
   .anyRequest().authenticated()
   //返回HttpSecurity以进行进一步的自定义,证明是一次新的配置的开始
   .and()
   .formLogin()
   //如果未指定此页面，则会跳转到默认页面
//                .loginPage("/login.html")
   .loginProcessingUrl("/login")
   .permitAll()
   //认证失败处理类
   .failureHandler(customAuthenticationFailureHandler);
}

/**
 * AuthenticationManager 对象在OAuth2.0认证服务中要使用，提前放入IOC容器中
 * @return
 * @throws Exception
 */
@Override
@Bean
public AuthenticationManager authenticationManagerBean() throws Exception {
 return super.authenticationManagerBean();
}
```



**AuthorizationServer配置**

①继承`AuthorizationServerConfigurerAdapter`类，增加`@EnableAuthorizationServer`注解开启认证服务

②依赖注入，注入7个实例`Bean`对象

```java
/**
 * 数据库连接池对象
 */
private final DataSource dataSource;

/**
 * 认证业务对象
 */
private final ISysUserService userService;

/**
 * 授权码模式专用对象
 */
private final AuthenticationManager authenticationManager;

/**
 * 客户端信息来源
 * @return
 */
@Bean
public JdbcClientDetailsService jdbcClientDetailsService(){
   return new JdbcClientDetailsService(dataSource);
}

/**
 * token保存策略
 * @return
 */
@Bean
public TokenStore tokenStore(){
 return new JdbcTokenStore(dataSource);
}

/**
 * 授权信息保存策略
 * @return
 */
@Bean
public ApprovalStore approvalStore(){
 return new JdbcApprovalStore(dataSource);
}

/**
 * 授权码模式数据来源
 * @return
 */
@Bean
public AuthorizationCodeServices authorizationCodeServices(){
 return new JdbcAuthorizationCodeServices(dataSource);
}
```

③重写方法进行配置

```java
/**
 * 用来配置客户端详情服务（ClientDetailsService）
 * 客户端详情信息在这里进行初始化
 * 指定客户端信息的数据库来源
 * @param clients
 * @throws Exception
 */
@Override
public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
 clients.withClientDetails(jdbcClientDetailsService());
}

/**
 * 检测 token 的策略
 * @param security
 * @throws Exception
 */
@Override
public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
 security
   //允许客户端以form表单的方式将token传达给我们
   .allowFormAuthenticationForClients()
   //检验token必须需要认证
   .checkTokenAccess("isAuthenticated()");
}


/**
 * OAuth2.0的主配置信息
 * @param endpoints
 * @throws Exception
 */
@Override
public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
 endpoints
   //刷新token时会验证当前用户是否已经通过认证
   .userDetailsService(userService)
   .approvalStore(approvalStore())
   .authenticationManager(authenticationManager)
   .authorizationCodeServices(authorizationCodeServices())
   .tokenStore(tokenStore());
}
```



## 模式

### 授权码模式

#### 原理

前边所讲的内容都是基于授权码模式，授权码模式被称为最安全的一种模式，它获取令牌的操作是在两个服务端进行的，极大的减小了令牌泄漏的风险。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202501051534224.png)

1. 客户端请求授权：用户访问客户端，客户端将用户导向授权服务器，并包含以下参数：
	- response_type=code：表示使用授权码模式。
	- client_id：标识客户端。
	- redirect_uri：授权成功后重定向的URI。
	- scope：请求的权限范围。

2. 用户同意授权：用户在授权服务器登录并同意授权请求。
3. 授权服务器发放授权码：授权服务器验证用户身份和授权请求后，向客户端发放授权码。
4. 客户端获取访问令牌：客户端通过后端将授权码和客户端凭证发送到授权服务器。包含以下参数：
	- grant_type=authorization_code：表示使用授权码模式。
	- code：授权码。
	- redirect_uri：必须与步骤1中的重定向URI一致。
	- client_id：客户端标识。
	- client_secret：客户端秘钥（可选）。
5. 授权服务器发放访问令牌：授权服务器验证授权码和客户端凭证，如果有效则发放访问令牌。
6. 客户端使用访问令牌：客户端可以使用访问令牌访问用户的受保护资源。

授权码模式相对于其他模式更安全，因为客户端不直接接触用户凭证，且在授权码的交换过程中可以使用安全的后端通信。


#### 案例

启动两个服务，当再次请求`127.0.0.1:9002/product/findAll`接口时会提示以下错误

```
{
    "error": "unauthorized",
    "error_description": "Full authentication is required to access this resource"
}
```

①调用接口获取授权码

发送`127.0.0.1:9001/oauth/authorize?response_type=code&client_id=cheetah_one`请求，前边的路径是固定形式的，`response_type=code`表示获取授权码，`client_id=cheetah_one`表示客户端的名称是我们数据库配置的数据。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092028303.webp)

该页面是`oauth2`的默认页面，输入用户的账户密码点击登录会提示我们进行授权，这是数据库`oauth_client_details`表我们设置`autoapprove`为`false`起到的效果。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092031210.webp)

选择`Approve`点击`Authorize`按钮，会发现我们设置的回调地址（`oauth_client_details`表中的`web_server_redirect_uri`）后边拼接了`code`值，该值就是授权码。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092032058.webp)

查看数据库发现`oauth_approvals`和`oauth_code`表已经存入数据了。

拿着授权码去获取`token`

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092032055.webp)

获取到`token`之后`oauth_access_token`和`oauth_refresh_token`表中会存入数据以用于后边的认证。而`oauth_code`表中的数据被清除了，这是因为`code`值是直接暴漏在网页链接上的，`oauth2`为了防止他人拿到`code`非法请求而特意设置为仅用一次。

拿着获取到的`token`去请求资源服务的接口，此时有两种请求方式

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092032070.webp)

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092032078.webp)



### 简化模式

所谓简化模式是针对授权码模式进行的简化，它将授权码模式中获取授权码的步骤省略了，直接去请求获取`token`。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092032112.webp)

流程：
1. 发起认证请求（Authorization Request）：客户端（浏览器或移动应用）向授权服务器发送认证请求。  请求包括：  
	- response_type 参数，设为 "token"，表示使用隐式授权模式。  
	- client_id 参数，标识客户端。  
	- redirect_uri 参数，用于接收授权服务器的响应。  
2. 用户身份验证和授权：  用户在授权服务器上进行身份验证。  用户同意授权请求，授权服务器生成访问令牌。  
3. 生成令牌（Access Token）：  授权服务器生成访问令牌。  生成的访问令牌直接包含在重定向 URI 中，作为 URI 片段的一部分。  
4. 重定向到客户端：  授权服务器将包含访问令牌的重定向 URI 返回给客户端。  重定向 URI 中的访问令牌可通过前端 JavaScript 访问。  
5. 客户端使用令牌：  客户端从重定向 URI 中提取访问令牌。  客户端可以使用令牌访问资源服务器上的受保护资源。  


流程：发送请求`127.0.0.1:9001/oauth/authorize?response_type=token&client_id=cheetah_one`跳转到登录页进行登录，`response_type=token`表示获取`token`。

输入账号密码登录之后会直接在浏览器返回`token`，就可以像授权码方式一样携带`token`去请求资源了。

![图片](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092032086.webp)

该模式的弊端就是`token`直接暴漏在浏览器中，非常不安全，**不建议使用**。



### 密码模式

密码模式下，用户需要将账户和密码提供给客户端向认证服务器申请令牌，所以该种模式需要用户高度信任客户端。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092032712.webp)

流程：
1. 客户端向授权服务器发送请求：  客户端通过安全通道直接向授权服务器发送包含以下参数的请求：  
	- grant_type：固定为 "password"，表示使用密码模式。  
	- client_id：标识客户端。  
	- client_secret：客户端的秘密（如果有的话）。  
	- username：用户的用户名。  
	- password：用户的密码。  
	- scope：请求的范围（可选）。  
2. 授权服务器验证用户身份：  授权服务器验证客户端的身份和用户的身份。  如果验证成功，授权服务器生成访问令牌和可能的刷新令牌。  
3. 授权服务器响应：  授权服务器以 JSON 格式返回访问令牌和刷新令牌。  

密码模式的使用场景通常受到一些限制，因为它需要客户端直接存储用户的密码。因此，它主要适用于受信任的客户端，如后端服务器。在使用密码模式时，需要特别注意确保安全性，并确保通过安全通道（如 HTTPS）进行通信。


请求如下

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092032781.webp)

获取成功之后可以去访问资源了。



### 客户端模式

客户端模式已经不太属于`oauth2`的范畴了，用户直接在客户端进行注册，然后客户端去认证服务器获取令牌时不需要携带用户信息，完全脱离了用户，也就不存在授权问题了。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092032773.webp)

流程：
1. 客户端向授权服务器发送请求：  客户端通过安全通道直接向授权服务器发送包含以下参数的请求：  
	- grant_type：固定为 "client_credentials"，表示使用客户端凭证模式。  
	- client_id：标识客户端。  
	- client_secret：客户端的秘密。  
2. 授权服务器验证客户端身份：  授权服务器验证客户端的身份，确保客户端合法且有权限使用此模式。  
3. 授权服务器响应：  授权服务器以 JSON 格式返回访问令牌。  响应中包含访问令牌以及令牌的有效期等信息。  

客户端凭证模式适用于那些不涉及用户的、由客户端自己访问自己资源的情况，例如后端服务之间的通信。在使用客户端凭证模式时，同样需要注意保障传输安全性，并限制客户端凭证的使用范围。



发送请求如下

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092032846.webp)

获取成功之后可以去访问资源了。



### 刷新token

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202406092032887.webp)



### 权限校验

除了在数据库中为客户端配置资源服务外，还可以动态的给用户分配接口的权限。

①开启`Security`内置的动态配置

在开启资源服务时给`ResourceServerConfig`类增加注解`@EnableGlobalMethodSecurity(securedEnabled = true,prePostEnabled = true)`

②给接口增加权限

```java
@GetMapping("/findAll")
@Secured("ROLE_PRODUCT")
public String findAll(){
    return "产品列表查询成功";
}
```

③在用户登录时设置用户权限

```java
@Override
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
 SysUser sysUser = this.baseMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
 sysUser.setRoleList(AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_PRODUCT"));
 return sysUser;
}
```

然后测试会发现可以正常访问。

<!-- @include: @article-footer.snippet.md -->     