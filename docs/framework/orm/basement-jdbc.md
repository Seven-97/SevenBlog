---
title: JDBC连接数据库的核心原理
category: 常用框架
tags:
  - JDBC
head:
  - - meta
    - name: keywords
      content: JDBC,实现原理,源码阅读,JDBC 底层原理
  - - meta
    - name: description
      content: 全网最全的JDBC知识点总结，让天下没有难学的八股文！
---



## JDBC实现及原理

JDBC（Java DataBase Connectivity）是Java和数据库之间的一个桥梁，是一个「规范」而不是一个实现，能够执行SQL语句。JDBC由一组用Java语言编写的类和接口组成。各种不同类型的数据库都有相应的实现，注意：本文中的代码都是针对MySQL数据库实现的。

 

先看一个案例：

```java
public class JdbcDemo {
    public static final String URL = "jdbc:mysql://localhost:3306/mblog";
    public static final String USER = "root";
    public static final String PASSWORD = "123456";

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT id, name, age FROM m_user where id =1");
        while (rs.next()) {
            System.out.println("name: " + rs.getString("name") + " ：年龄" + rs.getInt("age"));
        }
    }
}
```



JDBC 步骤如下：

1. 数据库驱动：Class.forName("com.mysql.jdbc.Driver");
2. 获取链接：Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
3. 创建Statement或者PreparedStatement对象： Statement stmt = conn.createStatement();
4. 执行sql数据库查询：ResultSet rs = stmt.executeQuery("SELECT id, name, age FROM m_user where id =1");
5. 解析结果集：System.out.println("name: "+rs.getString("name")+" ：年龄"+rs.getInt("age"));
6. 最后就是各种资源的关闭。

 

### 数据库驱动

安装好数据库之后，应用程序是不能直接使用数据库的，必须要通过相应的数据库驱动程序，通过驱动程序去和数据库打交道。其实也就是数据库厂商的JDBC接口实现，即对Connection等接口的实现类的jar文件。

Driver接口：此接口是提供给数据库厂商实现的。比如说MySQL的，需要依赖对应的jar包

MySQL数据库对应的实现驱动实现类：

```java
package com.mysql.cj.jdbc;
import java.sql.SQLException;

public class Driver extends NonRegisteringDriver implements java.sql.Driver {
    static {
        try {
            //注册驱动
            java.sql.DriverManager.registerDriver(new Driver());
        } catch (SQLException E) {
            throw new RuntimeException("Can't register driver!");
        }
    }

    public Driver() throws SQLException {
    }
}
```



DriverManager是rt.jar包下的类，（rt=runtime），把程序需要驱动类注册进去。

```java
//DriverManager类中的方法
public static synchronized void registerDriver(java.sql.Driver driver,DriverAction da)throws SQLException{
        /* Register the driver if it has not already been added to our list */
        if(driver!=null){
            registeredDrivers.addIfAbsent(new DriverInfo(driver,da));
        }else{
        // This is for compatibility with the original DriverManager
            throw new NullPointerException();
        }
        println("registerDriver: "+driver);
}
```



类似的，可以加载其它厂商的驱动

- Oracle驱动：Class.forName("oracle.jdbc.driver.OracleDriver");

- Sql Server驱动：Class.forName("com.microsoft.jdbc.sqlserver.SQLServerDriver");



### 获取连接

看起来只有这一行代码

Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);

深入聊聊这行代码，到底底层是怎么连接数据库的？

方法三个参数：链接地址，用户名和密码。

```java
public static Connection getConnection(String url,String user, String password) throws SQLException {
        java.util.Properties info = new java.util.Properties();
        if (user != null) {
            info.put("user", user);
        }
        if (password != null) {
            info.put("password", password);
        }
        return (getConnection(url, info, Reflection.getCallerClass()));
}
```



获取连接的关键代码aDriver.driver.connect(url,info); 这个方法是每个数据库驱动自己的实现的。

```java
// Worker method called by the public getConnection() methods.
private static Connection getConnection(String url,java.util.Properties info,Class caller)throws SQLException{
        ClassLoader callerCL = caller != null ? caller.getClassLoader() : null;
        SQLException reason = null;
        //遍历气门注册的数据库驱动
        for(DriverInfo aDriver:registeredDrivers){
            try{
                //获取连接
                Connection con = aDriver.driver.connect(url,info);
                if(con!=null){
                    // Success!
                    println("getConnection returning "+aDriver.driver.getClass().getName());
                    return(con);
                }
            }catch(SQLException ex){
                if(reason==null){
                    reason=ex;
                }
            }
        }
}
```

获取连接的关键代码aDriver.driver.connect(url,info); 这个方法是每个数据库驱动自己的实现的。

```java
package com.mysql.cj.jdbc;

public class NonRegisteringDriver implements java.sql.Driver {
    @Override
    public java.sql.Connection connect(String url, Properties info) throws SQLException {
        //部分无关键要的代码省略
        //...
        
        //下面是重点
        //ConnectionUrl从这个类名应该能猜到还不到真正连接的，只是创建一个连接Url相关信息封装。
        ConnectionUrl conStr = ConnectionUrl.getConnectionUrlInstance(url, info);
        switch (conStr.getType()) {
            //SINGLE_CONNECTION("jdbc:mysql:", HostsCardinality.SINGLE), //
            case SINGLE_CONNECTION:
                //这里就是获取一个实例，连接就在这里面产生的
                return com.mysql.cj.jdbc.ConnectionImpl.getInstance(conStr.getMainHost());
            case LOADBALANCE_CONNECTION:
                return LoadBalancedConnectionProxy.createProxyInstance((LoadbalanceConnectionUrl) conStr);
            case FAILOVER_CONNECTION:
                return FailoverConnectionProxy.createProxyInstance(conStr);
            case REPLICATION_CONNECTION:
                return ReplicationConnectionProxy.createProxyInstance((ReplicationConnectionUrl) conStr);
            default:
                return null;
        }
    }
}


public static JdbcConnection getInstance(HostInfo hostInfo) throws SQLException {
        return new ConnectionImpl(hostInfo);
}
```

ConnectionImpl构造方法里有调用createNewIO方法：

```java
@Override
public void createNewIO(boolean isForReconnect){
    synchronized (getConnectionMutex()){
        try{
            if(!this.autoReconnect.getValue()){
                connectOneTryOnly(isForReconnect);
                return;
            }
            connectWithRetries(isForReconnect);
        }catch(SQLException ex){
        }
    }
}

private void connectOneTryOnly(boolean isForReconnect)throws SQLException{
        Exception connectionNotEstablishedBecause=null;
        JdbcConnection c=getProxy();
        //又看到熟悉的connet方法，
        //其中，这里的session是NativeSession
        this.session.connect(this.origHostInfo,this.user,this.password,this.database,DriverManager.getLoginTimeout()*1000,c);
        this.session.setQueryInterceptors(this.queryInterceptors);
 }
 
 public void connect(HostInfo hi,String user,String password,String database,int loginTimeout,TransactionEventHandler transactionManager)throws IOException{
        SocketConnection socketConnection=new NativeSocketConnection();
        //看到socket连接了，后续就是socket的连接数据库的过程了
        socketConnection.connect(this.hostInfo.getHost(),this.hostInfo.getPort(),this.propertySet,getExceptionInterceptor(),this.log,loginTimeout);
        this.protocol.connect(user,password,database);this.protocol.getServerSession().setErrorMessageEncoding(this.protocol.getAuthenticationProvider().getEncodingForHandshake());
}
```

com.mysql.cj.protocol.a.NativeSocketConnection#connect

```java
@Override
public void connect(String hostName, int portNumber, PropertySet propSet, ExceptionInterceptor excInterceptor, Log log, int loginTimeout) {
    this·mysqlSocket = this.socketFactory.connect(this.host, this.port, propSet, loginTimeout);
    //...
}
```

这里的socketFactory是StandardSocketFactory。所以也就是调用的是StandardSocketFactory的connect方法：

```java
public T connect(String hostname, int portNumber, PropertySet pset, int loginTimeout) throws IOException {
    this.rawSocket = createSocket(pset);
    this.rawSocket.connect(sockAddr, getRealTimeout(connectTimeout));
}
protected Socket createSocket(PropertySet props) {
    return new Socket();
}
```





### 小结

数据库驱动依赖[SPI类加载机制](https://www.seven97.top/java/basis/06-SPI.html)

获取连接是通过socket与数据库取得连接的

 
## 使用JDBC原生API面对的问题

- 需要手动管理资源
- 代码重复
- 业务逻辑与数据操作的代码耦合
- 结果集需要手动处理


## 更简单的操作数据库的方式？

### JdbcTemplate


`JdbcTemplate` 是 Spring Framework 提供的一个核心类，用于简化 JDBC 编程。它的主要作用包括：

- 自动管理数据库连接（获取、释放）
- 自动处理异常（将 SQLException 转换为 Spring 的 DataAccessException）
- 简化 SQL 执行（增删改查）
- 自动映射结果集（ResultSet）到 Java 对象
- 避免样板代码（如 try-catch-finally、资源关闭等）


`JdbcTemplate`的核心设计是**模板方法模式**，将固定的操作流程（如获取连接、执行语句、释放资源）封装起来，而将可变部分（如 SQL 语句、参数设置、结果映射）通过回调接口留给我们开发者


1. 引入依赖

```java
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jdbc</artifactId>
    <version>5.3.10</version> <!-- 请使用您的Spring版本 -->
</dependency>
```

2. Spring Boot 配置（application.properties）

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/testdb
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

3. 在 Spring Boot 中，配置数据源后，可直接注入 `JdbcTemplate`

```java
@Service
public class UserService {
    private final JdbcTemplate jdbcTemplate;

    // 构造器注入
    public UserService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}
```

4. 执行 DML 操作（增、删、改）

使用 `update()`方法执行 `INSERT`, `UPDATE`, `DELETE`操作。它返回受影响的行数

```java
@Repository
public class UserDao {

    // 插入
    public int addUser(String name, Integer age) {
        String sql = "INSERT INTO users (name, age) VALUES (?, ?)";
        return jdbcTemplate.update(sql, name, age);
    }

    // 更新
    public int updateUserAge(String name, Integer newAge) {
        String sql = "UPDATE users SET age = ? WHERE name = ?";
        return jdbcTemplate.update(sql, newAge, name); // 注意参数顺序
    }

    // 删除
    public int deleteUser(String name) {
        String sql = "DELETE FROM users WHERE name = ?";
        return jdbcTemplate.update(sql, name);
    }
}
```

5. 执行 DQL 操作（查询）及结果集映射

这是 JdbcTemplate 最强大的功能之一，提供了多种灵活的映射方式。

- 使用 `RowMapper<T>` 自定义映射

`RowMapper`接口用于将结果集的每一行映射为一个 Java 对象。需要实现 `mapRow`方法

```java
public class User {
    private Long id;
    private String name;
    private Integer age;
    // 省略了构造函数、Getter和Setter
}

@Repository
public class UserDao {
    // 查询单个对象
    public User findUserById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, new UserRowMapper(), id);
    }

    // 查询列表
    public List<User> findAllUsers() {
        String sql = "SELECT * FROM users";
        return jdbcTemplate.query(sql, new UserRowMapper());
    }

    // 实现 RowMapper 接口
    private static class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setName(rs.getString("name"));
            user.setAge(rs.getInt("age"));
            return user;
        }
    }
}
```

- 使用 `BeanPropertyRowMapper<T>`（推荐）

这是最常用的映射方式。如果数据库字段名（或下划线命名）和 Java 对象的属性名（驼峰命名）能够对应，Spring 会自动完成映射

```java
@Repository
public class UserDao {
    // 使用 BeanPropertyRowMapper 自动映射
    public User findUserByName(String name) {
        String sql = "SELECT * FROM users WHERE name = ?";
        return jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(User.class), name);
    }

    public List<User> findAllUsers() {
        String sql = "SELECT * FROM users";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(User.class));
    }
}
```

- 使用BaseRowMapper工具类，通用映射器，可以彻底告别为每个实体类编写重复的 `RowMapper`实现

```java
import org.springframework.jdbc.core.RowMapper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用BaseRowMapper - 支持泛型、反射、自动类型转换和下划线转驼峰命名
 * @param <T> 目标实体类型
 */
public class BaseRowMapper<T> implements RowMapper<T> {
    
    private final Class<T> targetClass;
    private final Map<String, Field> fieldMap;
    private final Map<String, Method> writeMethodCache;
    
    // 缓存类的字段信息，避免每次反射
    private static final Map<Class<?>, Map<String, Field>> CLASS_FIELD_CACHE = new ConcurrentHashMap<>();
    
    public BaseRowMapper(Class<T> targetClass) {
        this.targetClass = targetClass;
        this.fieldMap = getCachedFields(targetClass);
        this.writeMethodCache = new HashMap<>();
    }
    
    /**
     * 核心映射方法 - 将ResultSet的一行数据映射到Java对象
     */
    @Override
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        try {
            // 创建目标对象实例
            T targetObject = targetClass.getDeclaredConstructor().newInstance();
            
            // 获取结果集元数据
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            // 遍历结果集的所有列
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i); // 数据库列名（下划线格式）
                String propertyName = underscoreToCamel(columnName); // 转换为驼峰属性名
                
                // 查找对应的字段
                Field field = fieldMap.get(propertyName);
                if (field != null) {
                    // 获取字段值并设置到对象中
                    Object value = getValueFromResultSet(rs, columnName, field.getType());
                    if (value != null) {
                        setFieldValue(targetObject, field, value);
                    }
                }
            }
            
            return targetObject;
            
        } catch (Exception e) {
            throw new SQLException("映射结果集到对象失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从ResultSet中获取值，并根据字段类型进行转换
     */
    private Object getValueFromResultSet(ResultSet rs, String columnName, Class<?> fieldType) throws SQLException {
        try {
            // 处理基本类型和常用类型
            if (fieldType == int.class || fieldType == Integer.class) {
                return rs.getInt(columnName);
            } else if (fieldType == long.class || fieldType == Long.class) {
                return rs.getLong(columnName);
            } else if (fieldType == double.class || fieldType == Double.class) {
                return rs.getDouble(columnName);
            } else if (fieldType == float.class || fieldType == Float.class) {
                return rs.getFloat(columnName);
            } else if (fieldType == boolean.class || fieldType == Boolean.class) {
                return rs.getBoolean(columnName);
            } else if (fieldType == short.class || fieldType == Short.class) {
                return rs.getShort(columnName);
            } else if (fieldType == byte.class || fieldType == Byte.class) {
                return rs.getByte(columnName);
            } else if (fieldType == String.class) {
                return rs.getString(columnName);
            } else if (fieldType == BigDecimal.class) {
                return rs.getBigDecimal(columnName);
            } else if (fieldType == Date.class) {
                Timestamp timestamp = rs.getTimestamp(columnName);
                return timestamp != null ? new Date(timestamp.getTime()) : null;
            } else if (fieldType == java.sql.Date.class) {
                return rs.getDate(columnName);
            } else if (fieldType == Timestamp.class) {
                return rs.getTimestamp(columnName);
            } else if (fieldType == LocalDateTime.class) {
                Timestamp timestamp = rs.getTimestamp(columnName);
                return timestamp != null ? timestamp.toLocalDateTime() : null;
            } else if (fieldType == LocalDate.class) {
                java.sql.Date sqlDate = rs.getDate(columnName);
                return sqlDate != null ? sqlDate.toLocalDate() : null;
            } else if (fieldType.isEnum()) {
                // 枚举类型处理
                String enumValue = rs.getString(columnName);
                return getEnumValue(fieldType, enumValue);
            } else {
                // 默认使用Object类型
                return rs.getObject(columnName);
            }
        } catch (SQLException e) {
            // 如果列不存在，返回null
            if (e.getMessage().contains("Column") && e.getMessage().contains("not found")) {
                return null;
            }
            throw e;
        }
    }
    
    /**
     * 设置字段值到目标对象
     */
    private void setFieldValue(T targetObject, Field field, Object value) {
        try {
            // 方法1: 直接设置字段值（性能更好）
            field.setAccessible(true);
            field.set(targetObject, value);
            
            // 方法2: 使用setter方法（如果需要验证逻辑）
            // setValueUsingSetter(targetObject, field, value);
            
        } catch (IllegalAccessException e) {
            throw new RuntimeException("设置字段值失败: " + field.getName(), e);
        }
    }
    
    /**
     * 使用setter方法设置值（备用方案）
     */
    private void setValueUsingSetter(T targetObject, Field field, Object value) {
        try {
            String fieldName = field.getName();
            String setterMethodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            
            Method setter = writeMethodCache.get(setterMethodName);
            if (setter == null) {
                setter = targetClass.getMethod(setterMethodName, field.getType());
                writeMethodCache.put(setterMethodName, setter);
            }
            
            setter.invoke(targetObject, value);
            
        } catch (Exception e) {
            throw new RuntimeException("调用setter方法失败: " + field.getName(), e);
        }
    }
    
    /**
     * 下划线命名转驼峰命名
     * 例如: user_name -> userName, user_age -> userAge
     */
    private String underscoreToCamel(String underscoreName) {
        if (underscoreName == null || underscoreName.isEmpty()) {
            return underscoreName;
        }
        
        StringBuilder result = new StringBuilder();
        boolean nextUpperCase = false;
        
        for (int i = 0; i < underscoreName.length(); i++) {
            char currentChar = underscoreName.charAt(i);
            
            if (currentChar == '_') {
                nextUpperCase = true;
            } else {
                if (nextUpperCase) {
                    result.append(Character.toUpperCase(currentChar));
                    nextUpperCase = false;
                } else {
                    result.append(Character.toLowerCase(currentChar));
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * 获取枚举值
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object getEnumValue(Class<?> enumType, String enumValue) {
        if (enumValue == null) {
            return null;
        }
        
        try {
            return Enum.valueOf((Class<Enum>) enumType, enumValue);
        } catch (IllegalArgumentException e) {
            // 尝试忽略大小写匹配
            for (Object enumConstant : enumType.getEnumConstants()) {
                if (enumConstant.toString().equalsIgnoreCase(enumValue)) {
                    return enumConstant;
                }
            }
            return null;
        }
    }
    
    /**
     * 获取缓存的字段映射（避免重复反射）
     */
    private Map<String, Field> getCachedFields(Class<T> clazz) {
        return CLASS_FIELD_CACHE.computeIfAbsent(clazz, k -> {
            Map<String, Field> fields = new HashMap<>();
            Class<?> currentClass = clazz;
            
            // 遍历所有父类，包括继承的字段
            while (currentClass != null && currentClass != Object.class) {
                for (Field field : currentClass.getDeclaredFields()) {
                    // 排除静态字段
                    if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                        fields.put(field.getName(), field);
                    }
                }
                currentClass = currentClass.getSuperclass();
            }
            
            return fields;
        });
    }
}
```



### 工具小结

工具类解决了
- 方法封装
- 支持数据源
- 映射结果集，实现从数据库到对象的映射

没解决
- SQL语句硬编码
- 参数只能按顺序传入(占位符)
- 没有实现对象到数据库记录的映射
- 没有实现缓存等功能


## 什么是ORM框架

ORM : Object Relational Mapping

ORM解决的是 程序对象 和关系型数据库的 数据映射的问题

mybatis特性
- 使用连接池对连接进行管理
- SQL和代码分离，集中管理
- 参数映射和动态SQL
- 结果集映射
- 缓存管理
- 重复SQL的提取`<sql>`
- 插件机制



<!-- @include: @article-footer.snippet.md -->     

