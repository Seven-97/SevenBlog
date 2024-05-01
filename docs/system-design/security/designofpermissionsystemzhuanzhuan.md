---
title: 权限系统设计实例
category: 系统设计
tag:
  - 认证授权
---



> 来源：https://mp.weixin.qq.com/s/hFTDckfxhSnoM_McP18Vkg
>
> 转转技术

## 一、权限系统框架

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404272059102.png)

如图所示，权限系统主要解决两个问题：

1. 前端渲染：接入系统用户登录后，获取自己有权限的菜单，也就是前端sdk请求权限系统获取有权限的菜单并进行自动渲染。
2. 后端鉴权：用户请求接入系统后端，拒绝没有权限的接口访问，防止无权限用户获取后端接口地址后直接访问无权限的接口。

为了解决这两个问题，必然需要引入一些配套内容，其中重要的功能点如下：

1. 用户管理：统一登录系统，支撑权限系统识别登录用户。
2. 权限管理：系统管理、角色管理、菜单管理、数据管理、角色人员绑定等功能，方便为用户绑定相应的权限。
3. 后端鉴权：访问接入系统接口调用时使用。

后面会一一介绍用户管理、权限管理、后端鉴权sdk的实现。

## 二、用户管理

权限系统支持转转、转转精神、发条爱客、自注册四种来源的用户数据，支持用户在一个系统登录在其他系统保持登录状态功能（统一登录），支持员工入职自动加入权限系统（员工同步），这里重点介绍统一登录。

### 2.1、统一登录

先从接入权限的域名都是*.zhuanspirit.com说起，统一登录利用浏览器携带Cookie特点：不同二级域名可以携带一级域名的Cookie。统一登录系统为接入系统种植一级域名Cookie，例如OA系统oa.zhuanspirit.com，权限系统自身id.zhuanspirit.com统一种植域名为*.zhuanspirit.com的Cookie，这样OA、权限系统等接入统一登录系统的系统就可以做到一处登录处处访问。现在抛出两个问题：

1. 接入系统并未引入统一登录系统的任何代码，未登录用户是怎样跳转到统一登录页的？
2. 如何校验Cookie是否合法的？

从以OA系统为例的图中可以看出问题答案：

1. ngix会对*.zhuanspirit.com域名的请求做Cookie合法校验，校验不通过跳转到登录页，同时携带原url信息。
2. 校验合法性是通过Cookie<sso_uid,sso_code>的值是否和Redis中一致，不一致就需要重新登录。

## 三、权限管理

转转权限管理系统是一个基于经典RBAC权限管理模型的实现，上图为转转权限系统的主要功能，本身实现复杂度并不高。但是由于历史原因，转转权限系统在实现的时候需要考虑兼容原有权限系统的数据，这是一个比较琐碎复杂的事情，在这里不做过多介绍。对于简单的管理系统来说，灵魂在数据。对应到转转权限系统，血肉就是管理UI，它的使命就是方便为数据表填充数据，具体一点就是为用户表、系统表、菜单表、数据表、角色表以及中间关系表填充数据。下面我就展现转转权限系统的灵魂--数据库表关系。从上图中可以看到转转权限系统和RBAC一些差异：**用户直接可以与菜单或者数据绑定，增加灵活性。**

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404272100782.png)

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404272100256.png)



## 四、后端鉴权

### 4.1、工作原理

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404272100815.png)

如上图所示，后端鉴权sdk通过切面编程的思想，对请求url或者code进行拦截鉴权，如果登录用户没有权限，则返回错误。

### 4.2、核心方法

```java
AuthResult res = authentication.check(new AppCodeAuthParmBuilder(APP_CODE, CODE, request));
```

上面是sdk面向用户的接口很容易可以看出来，进入非常简单，仅仅需要三个参数：

- APP_CODE：系统的系统编号

- CODE：权限编码（可为空，为空时根据url鉴权）

- request：HttpServletRequest（从中获取Cookie信息解析出登录用户，以及url信息判断用户是否有此url权限）

### 4.3、使用demo

利用Spring Interceptor切面技术，下面是架构管理平台关于ZZLock后台操作的一个例子

```java
@Component
public class ZZLockInterceptor implements HandlerInterceptor {
    @Resource
    private Authentication authentication;
    private static final String APP_CODE = "arch_ipms";
    private static final String CODE = "ro_zzlock";
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        AuthResult res = authentication.check(new AppCodeAuthParmBuilder(APP_CODE, CODE, request));

        if (res.isSuccess() && res.getCode() == ResultCodeEnum.SUCCESS.getCode()) {
            return true;
        } else {
            UserDTO user = authentication.parseUser(request);
            logger.info("菜单 zzlock_get user {} 鉴权结果 code {} msg {}", user.getLoginName(), res.getCode(), res.getMsg());
            response.getWriter().write("user: " + user.getLoginName() + " no auth");
            response.getWriter().close();
        }

        return false;
    }
}
```



## 五、总结

本篇文章着重介绍转转权限系统的后端实现，从使用方的视角出发，也就是前端渲染和接口鉴权，引出转转权限系统如何识别用户（统一登录），如何存储权限数据（权限管理），如何实现后端鉴权。

简而言之，权限系统的主要功能：权限系统UI编辑权限数据，用户登录后，获取配置好的菜单和数据，并且校验用户访问的后端接口。