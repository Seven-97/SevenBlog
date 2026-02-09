---
title: 为什么要有ORM框架？
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

#### 基本使用

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

- BaseRowMapper工具类，通用映射器，可以彻底告别为每个实体类编写重复的 `RowMapper`实现

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



### 其它工具小结

工具类解决了
- 方法封装
- 支持数据源
- 映射结果集，实现从数据库到对象的映射

没解决
- SQL语句硬编码
- 参数只能按顺序传入(占位符)
- 没有实现对象到数据库记录的映射
- 没有实现缓存等功能











