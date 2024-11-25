---
title: 过滤器和拦截器
category: 常用框架
tag:
  - SpringBoot
head:
  - - meta
    - name: keywords
      content: SpringBoot,Filter,Interceptor
  - - meta
    - name: description
      content: 全网最全的SpringBoot知识点总结，让天下没有难学的八股文！
---



过滤器（Filter）和拦截器（Interceptor）都是用于解决项目中与请求处理、响应管理和业务逻辑控制相关问题的工具，但它们之间存在明显的区别。

## 基本使用

1. 实现过滤器  

过滤器可以使用 Servlet 3.0 提供的 @WebFilter 注解，配置过滤的 URL 规则，然后再实现 Filter 接口，重写接口中的 doFilter 方法，具体实现代码如下：

```java
import jakarta.servlet.*; 
import jakarta.servlet.annotation.WebFilter; 
import java.io.IOException; 

@WebFilter(urlPatterns = "/*") 
public class TestFilter implements Filter { 
	@Override 
	public void init(FilterConfig filterConfig) throws ServletException { 
		System.out.println("执行过滤器 init() 方法。"); 
	} 
	
	@Override 
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException { 
		System.out.println("开始执行过滤器 doFilter() 方法。"); 
		// 请求放行 
		filterChain.doFilter(servletRequest, servletResponse); 
		System.out.println("结束执行过滤器 doFilter() 方法。"); 
	} s
	
	@Override 
	public void destroy() { 
		System.out.println("执行过滤器 destroy() 方法。"); 
	} 
}
```

其中： 
- void init(FilterConfig filterConfig)：容器启动（初始化 Filter）时会被调用，整个程序运行期只会被调用一次。用于实现 Filter 对象的初始化。 
- void doFilter(ServletRequest request, ServletResponse response,FilterChain chain)：具体的过滤功能实现代码，通过此方法对请求进行过滤处理，其中 FilterChain 参数 是用来调用下一个过滤器或执行下一个流程。 
- void destroy()：用于 Filter 销毁前完成相关资源的回收工作。 

2. 实现拦截器

实现 HandlerInterceptor 接口并重写 preHandle/postHandle/afterCompletion 方法，具体实 现代码如下：
```java
import jakarta.servlet.http.HttpServletRequest; 
import jakarta.servlet.http.HttpServletResponse; 
import org.springframework.stereotype.Component; 
import org.springframework.web.servlet.HandlerInterceptor; 
import org.springframework.web.servlet.ModelAndView; 

@Component 
public class TestInterceptor implements HandlerInterceptor { 
	@Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception { 
		System.out.println("执行拦截器 preHandle() 方法。"); 
		return true; } 
	
	@Override 
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
		System.out.println("执行拦截器 postHandle() 方法。"); 
	} 
	
	@Override 
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
		System.out.println("执行拦截器 afterCompletion() 方法。"); 
	} 
}
```

实现拦截器 拦截器的实现 分为两步，第一步，创建一个普通的拦截器，实现 HandlerInterceptor 接口，并重写接口中 的相关方法；第二步，将上一步创建的拦截器加入到 Spring Boot 的配置文件中。

其中： 
- boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handle)：在请求方法执行前被调用，也就是调用目标方法之前被调用。比如我们在操 作数据之前先要验证用户的登录信息，就可以在此方法中实现，如果验证成功则返回 true，继续执行数据操作业务；否则就返回 false，后续操作数据的业务就不会被执行了。 
- void postHandle(HttpServletRequest request, HttpServletResponse response, Object handle, ModelAndView modelAndView)：调用请求方法之后执行，但它会在 DispatcherServlet 进行渲染视图之前被执行。 
- void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handle, Exception ex)：会在整个请求结束之后再执行，也就是在 DispatcherServlet 渲染了对应的视图之后再执行。 最后，我们再将上面的拦截器注入到项目配置文件中，并设置相应拦截规则，具体实现代码如 下：
```java
import org.springframework.beans.factory.annotation.Autowired; 
import org.springframework.context.annotation.Configuration; 
import org.springframework.web.servlet.config.annotation.InterceptorRegistry; 
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer; 

@Configuration 
public class InterceptorConfig implements WebMvcConfigurer { 
	// 注入拦截器 
	@Autowired 
	private TestInterceptor testInterceptor; 
	
	@Override 
	public void addInterceptors(InterceptorRegistry registry) { 
		registry.addInterceptor(testInterceptor) // 添加拦截器 
		.addPathPatterns("/**"); // 拦截所有地址 
	} 
}
```






