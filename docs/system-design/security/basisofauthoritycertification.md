---
title: 认证授权基础概念详解
category: 系统设计
tag:
  - 认证授权
---



## 认证和授权的区别

这是一个绝大多数人都会混淆的问题。首先先从读音上来认识这两个名词，很多人都会把它俩的读音搞混，所以我建议你先先去查一查这两个单词到底该怎么读，他们的具体含义是什么。

说简单点就是：

- **认证 (Authentication)：** 你是谁。
- **授权 (Authorization)：** 你有权限干什么。

稍微正式点（啰嗦点）的说法就是：

- **Authentication（认证）** 是验证您的身份的凭据（例如用户名/用户 ID 和密码），通过这个凭据，系统得以知道你就是你，也就是说系统存在你这个用户。所以，Authentication 被称为身份/用户验证。
- **Authorization（授权）** 发生在 **Authentication（认证）** 之后。授权嘛，光看意思大家应该就明白，它主要掌管我们访问系统的权限。比如有些特定资源只能具有特定权限的人才能访问比如 admin，有些对系统资源操作比如删除、添加、更新只能特定人才具有。

认证：

![认证登录](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404272056453.png)

授权：

![没有权限](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404272056075.png)

这两个一般在我们的系统中被结合在一起使用，目的就是为了保护我们系统的安全性。



## RBAC 模型

系统权限控制最常采用的访问控制模型就是 **RBAC 模型** 。

**什么是 RBAC 呢？** RBAC 即基于角色的权限访问控制（Role-Based Access Control）。这是一种通过角色关联权限，角色同时又关联用户的授权的方式。

简单地说：一个用户可以拥有若干角色，每一个角色又可以被分配若干权限，这样就构造成“用户-角色-权限” 的授权模型。在这种模型中，用户与角色、角色与权限之间构成了多对多的关系。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404272056108.png)

在 RBAC 权限模型中，权限与角色相关联，用户通过成为包含特定角色的成员而得到这些角色的权限，这就极大地简化了权限的管理。

为了实现 RBAC 权限模型，数据库表的常见设计如下（一共 5 张表，2 张用户建立表之间的联系）：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404272057328.png)

通过这个权限模型，我们可以创建不同的角色并为不同的角色分配不同的权限范围（菜单）。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404272057668.png)

通常来说，如果系统对于权限控制要求比较严格的话，一般都会选择使用 RBAC 模型来做权限控制。

##  Cookie 和Session

**Cookie 、Session和token** 都是应用在web中对http无状态协议的补充，达到状态保持的目的

### Cookie

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404272057425.png)

`Cookie` 和 `Session` 都是用来跟踪浏览器用户身份的会话方式，但是两者的应用场景不太一样。

#### 什么是Cookie

维基百科是这样定义 `Cookie` 的：

> `Cookies` 是某些网站为了辨别用户身份而储存在用户本地终端上的数据（通常经过加密）。

cookie中的信息是以键值对的形式储存在浏览器中，而且在浏览器中可以直接看到数据。 

一般流程：

1. 服务器发送cookie：当用户首次访问-个网站时，服务器会在响应头中添加一个Set-Cookie字段，该字段主要包含了cookie的名称、值；以及用户的登录信息。
2. 浏览器保存cookie：一旦浏览器收到来自服务器的Set-Cookie字段，它会将cookie保存在本地的cookie存储中。每个网站都有自己的cookie存储，不同网站之间的cookie是相互隔离的。
3. 浏览器发送cookie：当用户再次访问同一个网站时，浏览器会在请求头中添加一个Cookie字段，该段包含了用户之前保存的所有cookie。这样，服务器就能够根据cookie中的信息来识别用户，并提供个性化的服务。
4. 服务器读取cookie：当服务器收到带有Cookie字段的请求时，它会解析该字段（也就是说服务器不需要存储Cookie，只需要解析方法即可），提取出其中的cookie信息。根据这些信息，服务器可以判断用户的身份、偏好等，并根据需要返回相应的内容。
5. 服务器更新cookie：在一次会话中， 服务器可能会根据用户的操作更新cookie的值或属性。例如，当户在购物网站中添加商品到购物车时，服务器可以更新购物车对应的cookie，以便在下次访问时能够恢复购物车中的商品。
6. 浏览器删除cookie：当cookie过期或用户主动删除cookie时，浏览器会将其从本地的cookie存储中删除。此时，再次访问网站时，浏览器就不会发送该cookie了。

需要注意的是，cookie是存储在客户端的，因此它可能会被篡改、删除或被其他网站访问到。一般来说，简单的cookie实现是没有过期时间的，也就是说，cookie可以永久使用。因此为了增强，cookie的安全性，可以使用一些技术手段，如使用加密算法对cookie进行加密、设置只在HTTPS连接中传输cookie等。



优点：

1. 结构简单。cookie是一种基于文本的轻量结构，包含简单的键值对。
2. 数据持久。虽然客户端计算机上cookie的持续时间取决于客户端上的cookie过期处理和用户干预，cookie通常是客户端上持续时间最长的数据保留形式。

缺点：

1. 大小受到限制。大多数浏览器对 cookie 的大小有 4096 字节的限制，尽管在当今新的浏览器和客户端设备版本中，支持 8192 字节的 cookie 大小已愈发常见。
2. 非常不安全。cookie将数据裸露在浏览器中，这样大大增大了数据被盗取的风险，所以我们不应该将中要的数据放在cookie中，或者将数据加密处理。
3. 容易被csrf攻击。可以设置csrf_token来避免攻击。



#### 如何使用 Cookie ？

我这里以 Spring Boot 项目为例。

**1)设置 `Cookie` 返回给客户端**

```java
@GetMapping("/change-username")
public String setCookie(HttpServletResponse response) {
    // 创建一个 cookie
    Cookie cookie = new Cookie("username", "Jovan");
    //设置 cookie过期时间
    cookie.setMaxAge(7 * 24 * 60 * 60); // expires in 7 days
    //添加到 response 中
    response.addCookie(cookie);

    return "Username is changed!";
}
```

**2) 使用 Spring 框架提供的 `@CookieValue` 注解获取特定的 cookie 的值**

```java
@GetMapping("/")
public String readCookie(@CookieValue(value = "username", defaultValue = "Atta") String username) {
    return "Hey! My username is " + username;
}
```

**3) 读取所有的 `Cookie` 值**

```java
@GetMapping("/all-cookies")
public String readAllCookies(HttpServletRequest request) {

    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
        return Arrays.stream(cookies)
                .map(c -> c.getName() + "=" + c.getValue()).collect(Collectors.joining(", "));
    }

    return "No cookies";
}
```

更多关于如何在 Spring Boot 中使用 `Cookie` 的内容可以查看这篇文章：[How to use cookies in Spring Boot](https://attacomsian.com/blog/cookies-spring-boot) 。

### **session**

Cookie可以让服务端程序跟踪每个客户端的访问，但是每次客户端的访问都必须传回这些Cookie，如果Cookie很多，这无形地增加了客户端与服务端的数据传输量，为了解决这个问题，Session就出现了。相比于保存在客户端的Cookie，Session将用户交互信息保存在了服务器端，使得同一个客户端每次和服务端交互时，不需要每次都传回所有的Cookie值，而是只要传回一个ID，这个ID是客户端第一次访问服务器的时候生成的，而且每个客户端是唯一的。这样就实现了一个ID就能在服务器取得所有的用户交互信息。

一般流程：

1. session的创建：session是在客户端与服务器交互的过程中，由服务器创建的，并且会返回一个session的Id给客户端，一个会话只能有一个session对象；
2. 在此后的交互过程中，客户端在请求中带上这个ID；
3. 服务器可根据此Session ID获取到对应的保存在服务器内存中的session内容，以便于识别用户并提取用户信息。

注：一般情况下客户端session的ID都是通过Cookie的方式与服务器交互的，对于客户端禁用了Cookie的情况，可以通过在请求的Url中带上这个Session ID达到使用Session的目的。（也有一些在页面表单隐藏字段添加session id的，但是需要请求有提交表单的行为时才可实现session交互）



优点：

1. session的信息存储在服务端，相比于cookie就在一定程度上加大了数据的安全性；相比于jwt方便进行管理，也就是说当用户登录和主动注销，只需要添加删除对应的session就可以，这样管理起来很方便。

缺点：

1. session存储在服务端，这就增大了服务器的开销，当用户多的情况下，服务器性能会大大降低。
2. 因为是基于cookie来进行用户识别的, cookie如果被截获，用户就会很容易受到跨站请求伪造的攻击。
3. 用户认证之后，服务端做认证记录，如果认证的记录被保存在内存中的话，这意味着用户下次请求还必须要请求在这台服务器上,这样才能拿到授权的资源，这样在分布式的应用上，会限制负载均衡和集群水平拓展的能力。

### **两者的区别**

**Session 的主要作用就是通过服务端记录用户的状态。** 典型的场景是购物车，当要添加商品到购物车的时候，系统不知道是哪个用户操作的，因为 HTTP 协议是无状态的。服务端给特定的用户创建特定的 Session 之后就可以标识这个用户并且跟踪这个用户了。而只需要返回客户端保存session ID

Cookie 的具体用户数据保存在客户端(浏览器端)，Session 数据保存在服务器端。相对来说 Session 安全性更高。如果使用 Cookie 的一些敏感信息不要写入 Cookie 中，最好能将 Cookie 信息加密然后使用到的时候再去服务器端解密。



### Session-Cookie 方案

很多时候我们都是通过 `SessionID` 来实现特定的用户，`SessionID` 一般会选择存放在 Redis 中。举个例子：

1. 用户成功登陆系统，然后返回给客户端具有 `SessionID` 的 `Cookie` 。
2. 当用户向后端发起请求的时候会把 `SessionID` 带上，这样后端就知道你的身份状态了。

关于这种认证方式更详细的过程如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404272058639.png)

1. 用户向服务器发送用户名、密码、验证码用于登陆系统。
2. 服务器验证通过后，服务器为用户创建一个 `Session`，并将 `Session` 信息存储起来。
3. 服务器向用户返回一个 `SessionID`，写入用户的 `Cookie`。
4. 当用户保持登录状态时，`Cookie` 将与每个后续请求一起被发送出去。
5. 服务器可以将存储在 `Cookie` 上的 `SessionID` 与存储在内存中或者数据库中的 `Session` 信息进行比较，以验证用户的身份，返回给用户客户端响应信息的时候会附带用户当前的状态。

使用 `Session` 的时候需要注意下面几个点：

- 依赖 `Session` 的关键业务一定要确保客户端开启了 `Cookie`。
- 注意 `Session` 的过期时间。



### 多服务器节点下 Session-Cookie 方案

Session-Cookie 方案在单体环境是一个非常好的身份认证方案。但是，当服务器水平拓展成多节点时，Session-Cookie 方案就要面临挑战了。

举个例子：假如我们部署了两份相同的服务 A，B，用户第一次登陆的时候 ，Nginx 通过负载均衡机制将用户请求转发到 A 服务器，此时用户的 Session 信息保存在 A 服务器。结果，用户第二次访问的时候 Nginx 将请求路由到 B 服务器，由于 B 服务器没有保存 用户的 Session 信息，导致用户需要重新进行登陆。



**那么应该如何避免上面这种情况的出现呢？**

有几个方案可供大家参考：

1. 某个用户的所有请求都通过特性的哈希策略分配给同一个服务器处理。这样的话，每个服务器都保存了一部分用户的 Session 信息。服务器宕机，其保存的所有 Session 信息就完全丢失了。
2. 每一个服务器保存的 Session 信息都是互相同步的，也就是说每一个服务器都保存了全量的 Session 信息。每当一个服务器的 Session 信息发生变化，我们就将其同步到其他服务器。这种方案成本太大，并且，节点越多时，同步成本也越高。
3. 单独使用一个所有服务器都能访问到的数据节点（比如缓存）来存放 Session 信息。为了保证高可用，数据节点尽量要避免是单点。
4. Spring Session 是一个用于在多个服务器之间管理会话的项目。它可以与多种后端存储（如 Redis、MongoDB 等）集成，从而实现分布式会话管理。通过 Spring Session，可以将会话数据存储在共享的外部存储中，以实现跨服务器的会话同步和共享。

### 如果没有 Cookie 的话 Session 还能用吗？

这是一道经典的面试题！

一般是通过 `Cookie` 来保存 `SessionID` ，假如你使用了 `Cookie` 保存 `SessionID` 的方案的话， 如果客户端禁用了 `Cookie`，那么 `Session` 就无法正常工作。

但是，并不是没有 `Cookie` 之后就不能用 `Session` 了，比如你可以将 `SessionID` 放在请求的 `url` 里面`https://javaguide.cn/?Session_id=xxx` 。这种方案的话可行，但是安全性和用户体验感降低。当然，为了安全你也可以对 `SessionID` 进行一次加密之后再传入后端。



### 为什么 Cookie 无法防止 CSRF 攻击，而 Token 可以？

**CSRF(Cross Site Request Forgery)** 一般被翻译为 **跨站请求伪造** 。那么什么是 **跨站请求伪造** 呢？说简单点，就是用你的身份去发送一些对你不友好的请求。举个简单的例子：

小壮登录了某网上银行，他来到了网上银行的帖子区，看到一个帖子下面有一个链接写着“科学理财，年盈利率过万”，小壮好奇的点开了这个链接，结果发现自己的账户少了 10000 元。这是这么回事呢？原来黑客在链接中藏了一个请求，这个请求直接利用小壮的身份给银行发送了一个转账请求,也就是通过你的 Cookie 向银行发出请求。

```html
<a src=http://www.mybank.com/Transfer?bankId=11&money=10000>科学理财，年盈利率过万</>
```

上面也提到过，进行 `Session` 认证的时候，我们一般使用 `Cookie` 来存储 `SessionId`,当我们登陆后后端生成一个 `SessionId` 放在 Cookie 中返回给客户端，服务端通过 Redis 或者其他存储工具记录保存着这个 `SessionId`，客户端登录以后每次请求都会带上这个 `SessionId`，服务端通过这个 `SessionId` 来标示你这个人。如果别人通过 `Cookie` 拿到了 `SessionId` 后就可以代替你的身份访问系统了。

`Session` 认证中 `Cookie` 中的 `SessionId` 是由浏览器发送到服务端的，借助这个特性，攻击者就可以通过让用户误点攻击链接，达到攻击效果。

但是，我们使用 `Token` 的话就不会存在这个问题，在我们登录成功获得 `Token` 之后，一般会选择存放在 `localStorage` （浏览器本地存储）中。然后我们在前端通过某些方式会给每个发到后端的请求加上这个 `Token`,这样就不会出现 CSRF 漏洞的问题。因为，即使你点击了非法链接发送了请求到服务端，这个非法请求是不会携带 `Token` 的，所以这个请求将是非法的。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404272058926.png)

需要注意的是：不论是 `Cookie` 还是 `Token` 都无法避免 **跨站脚本攻击（Cross Site Scripting）XSS** 。

> 跨站脚本攻击（Cross Site Scripting）缩写为 CSS 但这会与层叠样式表（Cascading Style Sheets，CSS）的缩写混淆。因此，有人将跨站脚本攻击缩写为 XSS。

XSS 中攻击者会用各种方式将恶意代码注入到其他用户的页面中。就可以通过脚本盗用信息比如 `Cookie` 。

推荐阅读：[如何防止 CSRF 攻击？—美团技术团队](https://tech.meituan.com/2018/10/11/fe-security-csrf.html)



## 如何设计安全的外部API？

当向外部服务暴露 API 时，确保安全通信至关重要，以防止未经授权的访问和数据泄露。

两种常用的 API 安全方法是**基于 Token 的身份认证**和**基于 HMAC（哈希消息认证码）的认证**。

![设计安全的外部API](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202410191011914.png)

### 基于令牌

- 第 1 步 - 用户在客户端输入密码，客户端将密码发送给验证服务器。
- 第 2 步 - 验证服务器验证凭据并生成一个有有效期的令牌。
- 第 3 步和第 4 步 - 现在，客户端可以发送请求，客户端在每个 API 请求的 Authorization 头中包含这个Token，访问服务器资源。这种访问在令牌过期前一直有效。

优点

- 无状态：服务器不需要维护会话状态，Token 本身包含验证所需的所有信息。
- 灵活性：Token 可以包含角色或权限等元数据，支持细粒度的访问控制。
- 支持 OAuth 集成：与OAuth 2.0 兼容，适合第三方集成场景。

缺点

- Token 泄露风险：如果 Token 被拦截，攻击者可以在 Token 过期前滥用它，除非有其他机制（如 Token 吊销）在起作用。
- Token 存储：客户端需要安全存储 Token，对于 Web 或移动应用程序来说，这可能比较复杂。



### 基于 HMAC

该机制使用哈希函数（SHA256 或 MD5）生成消息验证码（签名）。

- 第 1 和 2 步 - 服务器生成两个密钥，一个是公共 APP ID（公钥），另一个是 API 密钥（私钥）。
- 第 3 步：现在我们在客户端生成一个 HMAC 签名（hmac A）。该签名是根据图中列出的一组属性生成的。
- 第 4 步 - 客户端发送访问服务器资源的请求，HTTP 头中包含 hmac A。
- 第 5 步 - 服务器收到包含请求数据和身份验证标头的请求。它从请求中提取必要的属性，并使用存储在服务器端的 API 密钥生成签名（hmac B。）
- 第 6 和 7 步 - 服务器会比较 hmac A（在客户端生成）和 hmac B（在服务器端生成）。如果两者匹配，请求的资源将返回给客户端。

优点

- 防篡改：HMAC 确保请求在传输过程中未被篡改，哪怕只改变一个字节，签名也会不匹配。
- 简单：不需要 Token 的发放或刷新，仅依赖共享密钥和哈希算法。
- 无 Token 泄露风险：因为没有 Token 可被盗取，这种方法天然安全。

缺点

- 密钥管理：客户端和服务器都必须安全地管理和存储共享密钥。一旦密钥泄露，安全性就会受到影响。
- 无状态 API 较复杂：HMAC 不提供内嵌元数据的无状态认证，访问控制需要单独处理。

在 HMAC 签名中加入时间戳是为了**防止重放攻击**（replay attack）。重放攻击是一种网络攻击方式，攻击者拦截合法的请求并在之后重复发送相同的请求，试图伪造身份或重复操作。通过时间戳，服务器可以验证请求是否在合理的时间范围内，从而大大提高系统的安全性。







<!-- @include: @article-footer.snippet.md -->     