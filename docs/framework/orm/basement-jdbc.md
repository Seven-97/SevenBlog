---
title: JDBC 底层原理
category: 常用框架
tag:
  - JDBC
head:
  - - meta
    - name: keywords
      content: JDBC,实现原理,源码阅读,JDBC 底层原理
  - - meta
    - name: description
      content: 全网最全的的JDBC知识点总结，让天下没有难学的八股文！
---



## 概述

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

 

## 数据库驱动

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



## 获取链接

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





## 总结

数据库驱动依赖SPI类加载机制

获取连接是通过socket与数据库取得连接的

 


<!-- @include: @article-footer.snippet.md -->     

