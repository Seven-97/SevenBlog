---
title: Mybatis基础操作
category: 常用框架
tags:
  - ORM框架
  - MyBatis
head:
  - - meta
    - name: keywords
      content: JDBC,实现原理,源码阅读
  - - meta
    - name: description
      content: 全网最全的JDBC知识点总结，让天下没有难学的八股文！
---

## Mybatis基础使用

###  Mybatis编程式开发

1. mybatis和MySQL jar包依赖

```xml
<dependencies>
    <!-- MyBatis 核心 -->
    <dependency>
        <groupId>org.mybatis</groupId>
        <artifactId>mybatis</artifactId>
        <version>3.5.10</version>
    </dependency>
    
    <!-- MySQL 驱动 -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.33</version>
    </dependency>
    
    <!-- 连接池（可选，推荐） -->
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
        <version>5.0.1</version>
    </dependency>
</dependencies>
```


2. 全局配置文件mybatis-config.xml

配置文件对应标签可以看官方文档：https://mybatis.org/mybatis-3/configuration.html

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    
    <!-- 1. 加载外部属性文件 -->
    <properties resource="jdbc.properties"/>
    
    <!-- 2. 全局设置 -->
    <settings>
        <!-- 开启下划线到驼峰命名自动映射 -->
        <setting name="mapUnderscoreToCamelCase" value="true"/>
        <!-- 开启二级缓存 -->
        <setting name="cacheEnabled" value="true"/>
        <!-- 延迟加载的触发方法 -->
        <setting name="lazyLoadTriggerMethods" value=""/>
        <!-- 查询时，关闭关联对象即时加载 -->
        <setting name="lazyLoadingEnabled" value="true"/>
        <!-- 设置超时时间 -->
        <setting name="defaultStatementTimeout" value="3000"/>
        <!-- 使用列标签代替列名 -->
        <setting name="useColumnLabel" value="true"/>
        <!-- 允许JDBC支持自动生成主键 -->
        <setting name="useGeneratedKeys" value="true"/>
    </settings>
    
    <!-- 3. 类型别名配置 -->
    <typeAliases>
        <!-- 扫描包，自动注册别名 -->
        <package name="com.example.entity"/>
        <!-- 也可以单独配置 -->
        <!-- <typeAlias type="com.example.entity.User" alias="User"/> -->
    </typeAliases>
    
    <!-- 4. 环境配置（可配置多个，通过default属性切换） -->
    <environments default="development">
        <!-- 开发环境 -->
        <environment id="development">
            <!-- 事务管理器 -->
            <transactionManager type="JDBC">
                <property name="closeConnection" value="false"/>
            </transactionManager>
            
            <!-- 数据源配置 -->
            <dataSource type="POOLED">
                <property name="driver" value="${jdbc.driver}"/>
                <property name="url" value="${jdbc.url}"/>
                <property name="username" value="${jdbc.username}"/>
                <property name="password" value="${jdbc.password}"/>
                <!-- 连接池配置 -->
                <property name="poolMaximumActiveConnections" value="20"/>
                <property name="poolMaximumIdleConnections" value="10"/>
                <property name="poolMaximumCheckoutTime" value="20000"/>
                <property name="poolTimeToWait" value="20000"/>
            </dataSource>
        </environment>
        
        <!-- 测试环境 -->
        <environment id="test">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="${jdbc.driver}"/>
                <property name="url" value="${jdbc.test.url}"/>
                <property name="username" value="${jdbc.test.username}"/>
                <property name="password" value="${jdbc.test.password}"/>
            </dataSource>
        </environment>
    </environments>
    
    <!-- 5. 映射器配置 -->
    <mappers>
        <!-- 方式1：通过resource指定XML文件 -->
        <mapper resource="com/example/mapper/UserMapper.xml"/>
        
        <!-- 方式2：通过class指定接口（需要接口和XML同名同路径） -->
        <!-- <mapper class="com.example.mapper.UserMapper"/> -->
        
        <!-- 方式3：扫描包下所有mapper接口 -->
        <!-- <package name="com.example.mapper"/> -->
    </mappers>
</configuration>
```


3. 映射器 Mapper.xml

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.mapper.UserMapper">
    
    <!-- 基础ResultMap映射 -->
    <resultMap id="BaseResultMap" type="User">
        <id property="id" column="id"/>
        <result property="username" column="username"/>
        <result property="password" column="password"/>
        <result property="email" column="email"/>
        <result property="age" column="age"/>
        <result property="status" column="status"/>
        <result property="createTime" column="create_time"/>
        <result property="updateTime" column="update_time"/>
        <!-- 枚举类型处理 -->
        <result property="gender" column="gender" 
                typeHandler="org.apache.ibatis.type.EnumOrdinalTypeHandler"/>
    </resultMap>
    
    <!-- 包含订单的ResultMap（一对多） -->
    <resultMap id="UserWithOrdersResultMap" type="User" extends="BaseResultMap">
        <!-- 一对多关联：用户的订单 -->
        <collection property="orders" ofType="Order" column="id"
                    select="com.example.mapper.OrderMapper.selectByUserId"/>
    </resultMap>
    
    <!-- 插入用户 -->
    <insert id="insert" parameterType="User" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO users (username, password, email, age, status, create_time, update_time, gender)
        VALUES (#{username}, #{password}, #{email}, #{age}, #{status}, #{createTime}, #{updateTime}, 
                #{gender, typeHandler=org.apache.ibatis.type.EnumOrdinalTypeHandler})
    </insert>
    
    <!-- 批量插入用户 -->
    <insert id="batchInsert" parameterType="java.util.List" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO users (username, password, email, age, status, create_time, update_time)
        VALUES
        <foreach collection="list" item="user" separator=",">
            (#{user.username}, #{user.password}, #{user.email}, #{user.age}, 
             #{user.status}, #{user.createTime}, #{user.updateTime})
        </foreach>
    </insert>
    
    <!-- 根据ID删除 -->
    <delete id="deleteById" parameterType="Long">
        DELETE FROM users WHERE id = #{id}
    </delete>
    
    <!-- 根据用户名删除 -->
    <delete id="deleteByUsername" parameterType="String">
        DELETE FROM users WHERE username = #{username}
    </delete>
    
    <!-- 更新用户 -->
    <update id="update" parameterType="User">
        UPDATE users
        <set>
            <if test="username != null">username = #{username},</if>
            <if test="password != null">password = #{password},</if>
            <if test="email != null">email = #{email},</if>
            <if test="age != null">age = #{age},</if>
            <if test="status != null">status = #{status},</if>
            <if test="updateTime != null">update_time = #{updateTime},</if>
        </set>
        WHERE id = #{id}
    </update>
    
    <!-- 更新用户状态 -->
    <update id="updateStatus">
        UPDATE users SET status = #{status} WHERE id = #{id}
    </update>
    
    <!-- 根据ID查询 -->
    <select id="selectById" parameterType="Long" resultMap="BaseResultMap">
        SELECT * FROM users WHERE id = #{id}
    </select>
    
    <!-- 查询所有用户 -->
    <select id="selectAll" resultMap="BaseResultMap">
        SELECT * FROM users ORDER BY create_time DESC
    </select>
    
    <!-- 根据用户名查询（模糊查询） -->
    <select id="selectByUsername" parameterType="String" resultMap="BaseResultMap">
        SELECT * FROM users 
        WHERE username LIKE CONCAT('%', #{username}, '%')
    </select>
    
    <!-- 条件查询（动态SQL） -->
    <select id="selectByCondition" parameterType="map" resultMap="BaseResultMap">
        SELECT * FROM users
        <where>
            <if test="username != null and username != ''">
                AND username LIKE CONCAT('%', #{username}, '%')
            </if>
            <if test="email != null and email != ''">
                AND email = #{email}
            </if>
            <if test="minAge != null">
                AND age >= #{minAge}
            </if>
            <if test="maxAge != null">
                AND age &lt;= #{maxAge}
            </if>
            <if test="status != null">
                AND status = #{status}
            </if>
            <if test="startTime != null">
                AND create_time >= #{startTime}
            </if>
            <if test="endTime != null">
                AND create_time &lt;= #{endTime}
            </if>
        </where>
        ORDER BY id DESC
    </select>
    
    <!-- 分页查询 -->
    <select id="selectByPage" resultMap="BaseResultMap">
        SELECT * FROM users 
        ORDER BY id DESC
        LIMIT #{offset}, #{pageSize}
    </select>
    
    <!-- 统计数量 -->
    <select id="count" resultType="int">
        SELECT COUNT(*) FROM users
    </select>
    
    <!-- 查询用户及其订单（关联查询） -->
    <select id="selectUserWithOrders" parameterType="Long" resultMap="UserWithOrdersResultMap">
        SELECT * FROM users WHERE id = #{userId}
    </select>
</mapper>
```


4. Mapper接口

```java
package com.example.mapper;

import com.example.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * User 数据访问接口
 * 注意：不使用Spring时，这个接口不需要添加@Repository等注解
 */
public interface UserMapper {
    
    // ========== 增 ==========
    
    /**
     * 插入用户
     */
    int insert(User user);
    
    /**
     * 批量插入用户
     */
    int batchInsert(List<User> users);
    
    // ========== 删 ==========
    
    /**
     * 根据ID删除用户
     */
    int deleteById(Long id);
    
    /**
     * 根据用户名删除用户
     */
    int deleteByUsername(String username);
    
    // ========== 改 ==========
    
    /**
     * 更新用户
     */
    int update(User user);
    
    /**
     * 更新用户状态
     * 使用@Param注解指定参数名
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    
    // 使用注解定义SQL（不需要在XML中配置）
    @Update("UPDATE users SET age = #{age} WHERE id = #{id}")
    int updateAge(@Param("id") Long id, @Param("age") Integer age);
    
    // ========== 查 ==========
    
    /**
     * 根据ID查询用户
     */
    User selectById(Long id);
    
    /**
     * 查询所有用户
     */
    List<User> selectAll();
    
    /**
     * 根据用户名查询
     */
    List<User> selectByUsername(String username);
    
    /**
     * 条件查询
     * @param condition 查询条件
     */
    List<User> selectByCondition(Map<String, Object> condition);
    
    /**
     * 分页查询
     * @param pageNum 页码
     * @param pageSize 每页大小
     */
    List<User> selectByPage(@Param("offset") int offset, @Param("pageSize") int pageSize);
    
    /**
     * 统计用户数量
     */
    int count();
    
    /**
     * 使用注解定义查询
     */
    @Select("SELECT * FROM users WHERE email = #{email}")
    User selectByEmail(String email);
    
    /**
     * 关联查询：查询用户及其订单（一对多）
     * 需要在XML中配置resultMap
     */
    User selectUserWithOrders(Long userId);
}
```


5. mybatis工具类

```java
package com.example.app;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;

/**
 * MyBatis 工具类 - 手动管理 SqlSessionFactory
 */
public class MyBatisUtil {
    
    private static SqlSessionFactory sqlSessionFactory;
    
    // 静态代码块，在类加载时初始化
    static {
        try {
            // 1. 加载 MyBatis 配置文件
            String resource = "mybatis-config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            
            // 2. 创建 SqlSessionFactory
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
            
            System.out.println("MyBatis SqlSessionFactory 初始化成功!");
        } catch (IOException e) {
            System.err.println("MyBatis 初始化失败: " + e.getMessage());
            throw new RuntimeException("MyBatis 初始化失败", e);
        }
    }
    
    /**
     * 获取 SqlSession 对象
     */
    public static SqlSession getSqlSession() {
        return sqlSessionFactory.openSession();
    }
    
    /**
     * 获取 SqlSession 对象（自动提交事务）
     */
    public static SqlSession getSqlSessionWithAutoCommit() {
        return sqlSessionFactory.openSession(true);
    }
    
    /**
     * 关闭 SqlSession
     */
    public static void closeSession(SqlSession session) {
        if (session != null) {
            session.close();
        }
    }
    
    /**
     * 获取 SqlSessionFactory
     */
    public static SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }
    
    /**
     * 获取 Mapper 接口的代理对象
     */
    public static <T> T getMapper(Class<T> type) {
        try (SqlSession session = getSqlSession()) {
            return session.getMapper(type);
        }
    }
}
```

6. 编程式使用示例

```java
package com.example.app;

import com.example.entity.User;
import com.example.mapper.UserMapper;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 不使用 Spring 时，MyBatis 编程式使用示例
 */
public class MyBatisDemo {
    
    public static void main(String[] args) {
        // 示例1：基础用法
        basicUsage();
        
        // 示例2：事务管理
        transactionManagement();
        
        // 示例3：动态SQL
        dynamicSqlExample();
        
        // 示例4：批量操作
        batchOperation();
        
        // 示例5：手动构建 SqlSessionFactory
        manualSqlSessionFactory();
    }
    
    /**
     * 示例1：基础CRUD操作
     */
    private static void basicUsage() {
        System.out.println("=== 示例1：基础CRUD操作 ===");
        
        SqlSession sqlSession = null;
        try {
            // 1. 获取 SqlSession
            sqlSession = MyBatisUtil.getSqlSession();
            
            // 2. 获取 Mapper 接口的代理对象
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            
            // 3. 插入数据
            User newUser = new User();
            newUser.setUsername("zhangsan");
            newUser.setPassword("123456");
            newUser.setEmail("zhangsan@example.com");
            newUser.setAge(25);
            newUser.setStatus(1);
            newUser.setCreateTime(new Date());
            newUser.setUpdateTime(LocalDateTime.now());
            
            int insertResult = userMapper.insert(newUser);
            System.out.println("插入结果：" + insertResult + "，生成ID：" + newUser.getId());
            
            // 4. 查询数据
            User user = userMapper.selectById(newUser.getId());
            System.out.println("查询用户：" + user);
            
            // 5. 更新数据
            user.setEmail("updated@example.com");
            int updateResult = userMapper.update(user);
            System.out.println("更新结果：" + updateResult);
            
            // 6. 分页查询
            List<User> userList = userMapper.selectByPage(0, 10);
            System.out.println("分页查询结果：" + userList.size() + " 条");
            
            // 7. 统计
            int count = userMapper.count();
            System.out.println("总用户数：" + count);
            
            // 8. 提交事务
            sqlSession.commit();
            System.out.println("事务提交成功");
            
        } catch (Exception e) {
            // 回滚事务
            if (sqlSession != null) {
                sqlSession.rollback();
            }
            System.err.println("操作失败：" + e.getMessage());
            e.printStackTrace();
        } finally {
            // 关闭 SqlSession
            MyBatisUtil.closeSession(sqlSession);
        }
    }
    
    /**
     * 示例2：事务管理
     */
    private static void transactionManagement() {
        System.out.println("\n=== 示例2：事务管理 ===");
        
        SqlSession sqlSession = null;
        try {
            sqlSession = MyBatisUtil.getSqlSession();
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            
            // 开启事务（默认不会自动提交）
            
            // 操作1：插入用户
            User user1 = new User("lisi", "lisi@example.com");
            user1.setPassword("123456");
            user1.setCreateTime(new Date());
            userMapper.insert(user1);
            
            // 操作2：模拟业务逻辑异常
            if (user1.getUsername().equals("lisi")) {
                throw new RuntimeException("模拟业务异常，事务应该回滚");
            }
            
            // 操作3：更新用户（不会执行，因为上面抛出异常）
            userMapper.updateAge(user1.getId(), 30);
            
            // 提交事务
            sqlSession.commit();
            System.out.println("事务提交成功");
            
        } catch (Exception e) {
            if (sqlSession != null) {
                sqlSession.rollback();
                System.out.println("事务回滚：" + e.getMessage());
            }
        } finally {
            MyBatisUtil.closeSession(sqlSession);
        }
    }
    
    /**
     * 示例3：动态SQL和条件查询
     */
    private static void dynamicSqlExample() {
        System.out.println("\n=== 示例3：动态SQL和条件查询 ===");
        
        try (SqlSession sqlSession = MyBatisUtil.getSqlSession()) {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            
            // 构建查询条件
            Map<String, Object> condition = new HashMap<>();
            condition.put("username", "zhang");  // 模糊查询
            condition.put("minAge", 20);         // 年龄 >= 20
            condition.put("maxAge", 30);         // 年龄 <= 30
            condition.put("status", 1);          // 状态为启用
            
            List<User> users = userMapper.selectByCondition(condition);
            System.out.println("条件查询结果：" + users.size() + " 条记录");
            
            for (User user : users) {
                System.out.println(user);
            }
            
            sqlSession.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 示例4：批量操作
     */
    private static void batchOperation() {
        System.out.println("\n=== 示例4：批量操作 ===");
        
        SqlSession sqlSession = null;
        try {
            sqlSession = MyBatisUtil.getSqlSession();
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            
            // 批量插入
            List<User> users = createTestUsers(5);
            int result = userMapper.batchInsert(users);
            System.out.println("批量插入：" + result + " 条记录");
            
            sqlSession.commit();
            
        } catch (Exception e) {
            if (sqlSession != null) {
                sqlSession.rollback();
            }
            e.printStackTrace();
        } finally {
            MyBatisUtil.closeSession(sqlSession);
        }
    }
    
    /**
     * 示例5：手动构建 SqlSessionFactory
     */
    private static void manualSqlSessionFactory() {
        System.out.println("\n=== 示例5：手动构建 SqlSessionFactory ===");
        
        String resource = "mybatis-config.xml";
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            
            // 1. 手动构建 SqlSessionFactoryBuilder
            SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
            
            // 2. 构建 SqlSessionFactory
            SqlSessionFactory sqlSessionFactory = builder.build(inputStream);
            
            // 3. 从 SqlSessionFactory 中获取 SqlSession
            try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
                
                // 4. 获取 Mapper
                UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
                
                // 5. 执行查询
                List<User> users = userMapper.selectAll();
                System.out.println("手动构建查询结果：" + users.size() + " 条记录");
                
                // 6. 手动提交事务
                sqlSession.commit();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 创建测试用户
     */
    private static List<User> createTestUsers(int count) {
        List<User> users = new java.util.ArrayList<>();
        for (int i = 1; i <= count; i++) {
            User user = new User();
            user.setUsername("test" + i);
            user.setPassword("pass" + i);
            user.setEmail("test" + i + "@example.com");
            user.setAge(20 + i);
            user.setStatus(1);
            user.setCreateTime(new Date());
            users.add(user);
        }
        return users;
    }
}
```

### 整合Spring

1. 添加依赖

较编程式开发主要是需要添加spring核心和MyBatis-Spring整合包

```xml
<dependencies>
    <!-- Spring 核心依赖 -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-context</artifactId>
        <version>${spring.version}</version>
    </dependency>
    
    <!-- MyBatis-Spring 整合包（关键！） -->
    <dependency>
        <groupId>org.mybatis</groupId>
        <artifactId>mybatis-spring</artifactId>
        <version>${mybatis.spring.version}</version>
    </dependency>
</dependencies>
```


2. **mybatis.xml**​ - MyBatis 和事务配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:tx="http://www.springframework.org/schema/tx"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.springframework.org/schema/tx
       http://www.springframework.org/schema/tx/spring-tx.xsd
       http://www.springframework.org/schema/aop
       http://www.springframework.org/schema/aop/spring-aop.xsd">

    <!-- 导入数据源配置 -->
    <import resource="datasource.xml"/>
    
    <!-- 1. 配置 SqlSessionFactory -->
    <bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
        <!-- 引用数据源 -->
        <property name="dataSource" ref="dataSource"/>
        
        <!-- MyBatis 主配置文件路径（如果有的话） -->
        <property name="configLocation" value="classpath:mybatis-config.xml"/>
        
        <!-- 实体类别名包 -->
        <property name="typeAliasesPackage" value="com.example.entity"/>
        
        <!-- MyBatis 映射文件路径 -->
        <property name="mapperLocations">
            <array>
                <value>classpath:com/example/dao/mapper/*.xml</value>
            </array>
        </property>
        
        <!-- MyBatis 全局配置 -->
        <property name="configuration">
            <bean class="org.apache.ibatis.session.Configuration">
                <!-- 下划线转驼峰 -->
                <property name="mapUnderscoreToCamelCase" value="true"/>
                <!-- 开启二级缓存 -->
                <property name="cacheEnabled" value="true"/>
                <!-- 使用列标签代替列名 -->
                <property name="useColumnLabel" value="true"/>
                <!-- 延迟加载的触发方法 -->
                <property name="lazyLoadTriggerMethods">
                    <array>
                        <value>equals</value>
                        <value>clone</value>
                        <value>hashCode</value>
                        <value>toString</value>
                    </array>
                </property>
            </bean>
        </property>
        
        <!-- 插件配置 -->
        <property name="plugins">
            <array>
                <!-- 分页插件示例 -->
                <!--
                <bean class="com.github.pagehelper.PageInterceptor">
                    <property name="properties">
                        <props>
                            <prop key="helperDialect">mysql</prop>
                        </props>
                    </property>
                </bean>
                -->
            </array>
        </property>
    </bean>
    
    <!-- 2. 配置 Mapper 扫描器（方式一：扫描包） -->
    <bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
        <!-- 扫描的包路径 -->
        <property name="basePackage" value="com.example.dao"/>
        <!-- 可选：指定 SqlSessionFactory -->
        <property name="sqlSessionFactoryBeanName" value="sqlSessionFactory"/>
        <!-- 可选：指定注解，只有标记了指定注解的接口才会被扫描 -->
        <!-- <property name="annotationClass" value="org.springframework.stereotype.Repository"/> -->
    </bean>
    
    <!-- 3. 配置事务管理器 -->
    <bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
        <property name="dataSource" ref="dataSource"/>
    </bean>
    
    <!-- 4. 开启注解驱动的事务管理 -->
    <tx:annotation-driven transaction-manager="transactionManager"/>
    
    <!-- 5. 事务AOP配置（可选，如果使用注解则不需要） -->
    <!--
    <tx:advice id="txAdvice" transaction-manager="transactionManager">
        <tx:attributes>
            <tx:method name="get*" read-only="true" propagation="SUPPORTS"/>
            <tx:method name="find*" read-only="true" propagation="SUPPORTS"/>
            <tx:method name="select*" read-only="true" propagation="SUPPORTS"/>
            <tx:method name="*" propagation="REQUIRED" rollback-for="Exception"/>
        </tx:attributes>
    </tx:advice>
    
    <aop:config>
        <aop:pointcut id="serviceMethods" expression="execution(* com.example.service.*.*(..))"/>
        <aop:advisor advice-ref="txAdvice" pointcut-ref="serviceMethods"/>
    </aop:config>
    -->
    
    <!-- 6. 配置 SqlSessionTemplate（可选，如果需要在DAO中直接使用） -->
    <bean id="sqlSessionTemplate" class="org.mybatis.spring.SqlSessionTemplate">
        <constructor-arg index="0" ref="sqlSessionFactory"/>
    </bean>
</beans>
```

3. **applicationContext.xml**​ - Spring 主配置文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.springframework.org/schema/context
       http://www.springframework.org/schema/context/spring-context.xsd">

    <!-- 开启注解扫描 -->
    <context:component-scan base-package="com.example">
        <!-- 排除 Controller，如果使用 MVC 的话 -->
        <context:exclude-filter type="annotation" 
            expression="org.springframework.stereotype.Controller"/>
    </context:component-scan>
    
    <!-- 导入 MyBatis 配置 -->
    <import resource="classpath:spring/mybatis.xml"/>
    
    <!-- 其他 Bean 配置 -->
    <bean id="userService" class="com.example.service.impl.UserServiceImpl"/>
    
    <!-- 属性占位符配置 -->
    <context:property-placeholder location="classpath:*.properties"/>
</beans>
```

4. 测试案例

```java
package com.example.config;

import org.apache.commons.dbcp2.BasicDataSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@ComponentScan("com.example")
@PropertySource("classpath:jdbc.properties")
@EnableTransactionManagement
public class AppConfig {
    
    @Bean
    public DataSource dataSource(
            @Value("${jdbc.driverClassName}") String driverClassName,
            @Value("${jdbc.url}") String url,
            @Value("${jdbc.username}") String username,
            @Value("${jdbc.password}") String password) {
        
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setInitialSize(5);
        dataSource.setMaxTotal(20);
        return dataSource;
    }
    
    @Bean
    public SqlSessionFactoryBean sqlSessionFactory(DataSource dataSource) {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setTypeAliasesPackage("com.example.entity");
        
        // 配置 MyBatis
        org.apache.ibatis.session.Configuration configuration = 
            new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(configuration);
        
        // 设置映射文件位置
        Resource[] mapperLocations = new Resource[]{
            new ClassPathResource("com/example/dao/mapper/*.xml")
        };
        factoryBean.setMapperLocations(mapperLocations);
        
        return factoryBean;
    }
    
    @Bean
    public MapperScannerConfigurer mapperScannerConfigurer() {
        MapperScannerConfigurer scanner = new MapperScannerConfigurer();
        scanner.setBasePackage("com.example.dao");
        scanner.setSqlSessionFactoryBeanName("sqlSessionFactory");
        return scanner;
    }
    
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
```


### 整合SpringBoot

1. 添加依赖

```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.0</version>
</dependency>
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>
<!-- 无需显式添加连接池，Spring Boot 默认使用 HikariCP -->
```

2. ​ `application.yml`配置，无需创建`mybatis-config.xml`文件，可直接在yaml文件中配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/testdb
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.example.entity
  configuration:
    map-underscore-to-camel-case: true
```

3. 配置自动扫描 Mapper 接口

```java
@SpringBootApplication
@MapperScan("com.example.mapper") // 自动扫描 Mapper 接口
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

4. 自动注入 Mapper

```java
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper; // 直接注入
    
    public User getUser(Long id) {
        return userMapper.selectById(id); // 直接使用
    }
}
```



<!-- @include: @article-footer.snippet.md -->     