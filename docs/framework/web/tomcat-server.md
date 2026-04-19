---
title: Tomcat - Server的设计和实现：StandardServer
category: 常用框架
tags:
  - Tomcat
head:
  - - meta
    - name: keywords
      content: Tomcat
  - - meta
    - name: description
      content: 全网最全的Tomcat知识点总结，让天下没有难学的八股文！
---


## 理解思路

- **第一：抓住StandardServer整体类依赖结构来理解**

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202603082024048.jpeg)

- **第二：结合server.xml来理解**

见下文具体阐述。

- **第三：结合Server Config官方配置文档**

http://tomcat.apache.org/tomcat-9.0-doc/config/server.html

## Server结构设计

> 我们需要从高一点的维度去理解Server的结构设计，而不是多少方法多少代码；这里的理解一定是要结合Server.xml对应理解。

### server.xml

- 首先要看下server.xml，这样你便知道了需要了解的四个部分

```xml
<Server port="8005" shutdown="SHUTDOWN">
  <!-- 1.属性说明
    port:指定一个端口，这个端口负责监听关闭Tomcat的请求
    shutdown:向以上端口发送的关闭服务器的命令字符串
  -->

  <!-- 2.Listener 相关 -->
  <Listener className="org.apache.catalina.core.AprLifecycleListener" />
  <Listener className="org.apache.catalina.mbeans.ServerLifecycleListener" />
  <Listener className="org.apache.catalina.mbeans.GlobalResourcesLifecycleListener" />
  <Listener className="org.apache.catalina.storeconfig.StoreConfigLifecycleListener"/>

  <!-- 3.GlobalNamingResources 相关 -->
  <GlobalNamingResources>

    <Environment name="simpleValue" type="java.lang.Integer" value="30"/>

    <Resource name="UserDatabase" auth="Container"
              type="org.apache.catalina.UserDatabase"
       description="User database that can be updated and saved"
           factory="org.apache.catalina.users.MemoryUserDatabaseFactory"
          pathname="conf/tomcat-users.xml" />

  </GlobalNamingResources>

  <!-- 4.service 相关 -->
  <Service name="Catalina">

  </Service>
</Server>
```

### Server中的接口设计

- **公共属性**, 包括上面的port，shutdown, address等

```java
/**
  * @return the port number we listen to for shutdown commands.
  *
  * @see #getPortOffset()
  * @see #getPortWithOffset()
  */
public int getPort();


/**
  * Set the port number we listen to for shutdown commands.
  *
  * @param port The new port number
  *
  * @see #setPortOffset(int)
  */
public void setPort(int port);

/**
  * Get the number that offsets the port used for shutdown commands.
  * For example, if port is 8005, and portOffset is 1000,
  * the server listens at 9005.
  *
  * @return the port offset
  */
public int getPortOffset();

/**
  * Set the number that offsets the server port used for shutdown commands.
  * For example, if port is 8005, and you set portOffset to 1000,
  * connector listens at 9005.
  *
  * @param portOffset sets the port offset
  */
public void setPortOffset(int portOffset);

/**
  * Get the actual port on which server is listening for the shutdown commands.
  * If you do not set port offset, port is returned. If you set
  * port offset, port offset + port is returned.
  *
  * @return the port with offset
  */
public int getPortWithOffset();

/**
  * @return the address on which we listen to for shutdown commands.
  */
public String getAddress();


/**
  * Set the address on which we listen to for shutdown commands.
  *
  * @param address The new address
  */
public void setAddress(String address);


/**
  * @return the shutdown command string we are waiting for.
  */
public String getShutdown();


/**
  * Set the shutdown command we are waiting for.
  *
  * @param shutdown The new shutdown command
  */
public void setShutdown(String shutdown);

/**
  * Get the utility thread count.
  * @return the thread count
  */
public int getUtilityThreads();


/**
  * Set the utility thread count.
  * @param utilityThreads the new thread count
  */
public void setUtilityThreads(int utilityThreads);
```

|属性|描述|
|---|---|
|className|使用的Java类名称。此类必须实现org.apache.catalina.Server接口。如果未指定类名，则将使用标准实现。|
|address|该服务器等待关闭命令的TCP / IP地址。如果未指定地址，localhost则使用。|
|port|该服务器等待关闭命令的TCP / IP端口号。设置为-1禁用关闭端口。注意：当使用Apache Commons Daemon启动Tomcat （在Windows上作为服务运行，或者在un * xes上使用jsvc运行）时，禁用关闭端口非常有效。但是，当使用标准shell脚本运行Tomcat时，不能使用它，因为它将阻止shutdown.bat|
|portOffset|应用于port和嵌套到任何嵌套连接器的端口的偏移量。它必须是一个非负整数。如果未指定，0则使用默认值。|
|shutdown|为了关闭Tomcat，必须通过与指定端口号的TCP / IP连接接收的命令字符串。|
|utilityThreads|此service中用于各种实用程序任务（包括重复执行的线程）的线程数。特殊值0将导致使用该值 Runtime.getRuntime().availableProcessors()。Runtime.getRuntime().availableProcessors() + value除非小于1，否则将使用负值， 在这种情况下将使用1个线程。预设值是1。|

- NamingResources

```java
/**
  * @return the global naming resources.
  */
public NamingResourcesImpl getGlobalNamingResources();


/**
  * Set the global naming resources.
  *
  * @param globalNamingResources The new global naming resources
  */
public void setGlobalNamingResources
    (NamingResourcesImpl globalNamingResources);


/**
  * @return the global naming resources context.
  */
public javax.naming.Context getGlobalNamingContext();
```

- Service相关， 包括添加Service， 查找Service，删除service等

```java
/**
  * Add a new Service to the set of defined Services.
  *
  * @param service The Service to be added
  */
public void addService(Service service);


/**
  * Wait until a proper shutdown command is received, then return.
  */
public void await();


/**
  * Find the specified Service
  *
  * @param name Name of the Service to be returned
  * @return the specified Service, or <code>null</code> if none exists.
  */
public Service findService(String name);


/**
  * @return the set of Services defined within this Server.
  */
public Service[] findServices();


/**
  * Remove the specified Service from the set associated from this
  * Server.
  *
  * @param service The Service to be removed
  */
public void removeService(Service service);
```

## StandardServer的实现

### 线程池

```java
// 此service中用于各种实用程序任务（包括重复执行的线程）的线程数
@Override
public int getUtilityThreads() {
    return utilityThreads;
}


/**
  * 获取内部进程数计算逻辑：
  * > 0时，即utilityThreads的值。
  * <=0时，Runtime.getRuntime().availableProcessors() + result...
  */
private static int getUtilityThreadsInternal(int utilityThreads) {
    int result = utilityThreads;
    if (result <= 0) {
        result = Runtime.getRuntime().availableProcessors() + result;
        if (result < 2) {
            result = 2;
        }
    }
    return result;
}


@Override
public void setUtilityThreads(int utilityThreads) {
    // Use local copies to ensure thread safety
    int oldUtilityThreads = this.utilityThreads;
    if (getUtilityThreadsInternal(utilityThreads) < getUtilityThreadsInternal(oldUtilityThreads)) {
        return;
    }
    this.utilityThreads = utilityThreads;
    if (oldUtilityThreads != utilityThreads && utilityExecutor != null) {
        reconfigureUtilityExecutor(getUtilityThreadsInternal(utilityThreads));
    }
}

// 线程池
private synchronized void reconfigureUtilityExecutor(int threads) {
    // The ScheduledThreadPoolExecutor doesn't use MaximumPoolSize, only CorePoolSize is available
    if (utilityExecutor != null) {
        utilityExecutor.setCorePoolSize(threads);
    } else {
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor =
                new ScheduledThreadPoolExecutor(threads,
                        new TaskThreadFactory("Catalina-utility-", utilityThreadsAsDaemon, Thread.MIN_PRIORITY));
        scheduledThreadPoolExecutor.setKeepAliveTime(10, TimeUnit.SECONDS);
        scheduledThreadPoolExecutor.setRemoveOnCancelPolicy(true);
        scheduledThreadPoolExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        utilityExecutor = scheduledThreadPoolExecutor;
        utilityExecutorWrapper = new org.apache.tomcat.util.threads.ScheduledThreadPoolExecutor(utilityExecutor);
    }
}


/**
  * Get if the utility threads are daemon threads.
  * @return the threads daemon flag
  */
public boolean getUtilityThreadsAsDaemon() {
    return utilityThreadsAsDaemon;
}


/**
  * Set the utility threads daemon flag. The default value is true.
  * @param utilityThreadsAsDaemon the new thread daemon flag
  */
public void setUtilityThreadsAsDaemon(boolean utilityThreadsAsDaemon) {
    this.utilityThreadsAsDaemon = utilityThreadsAsDaemon;
}
```

### Service相关方法实现

里面的方法都很简单。

```java
/**
  * Add a new Service to the set of defined Services.
  *
  * @param service The Service to be added
  */
@Override
public void addService(Service service) {

    service.setServer(this);

    synchronized (servicesLock) {
        Service results[] = new Service[services.length + 1];
        System.arraycopy(services, 0, results, 0, services.length);
        results[services.length] = service;
        services = results;

        if (getState().isAvailable()) {
            try {
                service.start();
            } catch (LifecycleException e) {
                // Ignore
            }
        }

        // Report this property change to interested listeners
        support.firePropertyChange("service", null, service);
    }

}

public void stopAwait() {
    stopAwait=true;
    Thread t = awaitThread;
    if (t != null) {
        ServerSocket s = awaitSocket;
        if (s != null) {
            awaitSocket = null;
            try {
                s.close();
            } catch (IOException e) {
                // Ignored
            }
        }
        t.interrupt();
        try {
            t.join(1000);
        } catch (InterruptedException e) {
            // Ignored
        }
    }
}

/**
  * Wait until a proper shutdown command is received, then return.
  * This keeps the main thread alive - the thread pool listening for http
  * connections is daemon threads.
  */
@Override
public void await() {
    // Negative values - don't wait on port - tomcat is embedded or we just don't like ports
    if (getPortWithOffset() == -2) {
        // undocumented yet - for embedding apps that are around, alive.
        return;
    }
    if (getPortWithOffset() == -1) {
        try {
            awaitThread = Thread.currentThread();
            while(!stopAwait) {
                try {
                    Thread.sleep( 10000 );
                } catch( InterruptedException ex ) {
                    // continue and check the flag
                }
            }
        } finally {
            awaitThread = null;
        }
        return;
    }

    // Set up a server socket to wait on
    try {
        awaitSocket = new ServerSocket(getPortWithOffset(), 1,
                InetAddress.getByName(address));
    } catch (IOException e) {
        log.error(sm.getString("standardServer.awaitSocket.fail", address,
                String.valueOf(getPortWithOffset()), String.valueOf(getPort()),
                String.valueOf(getPortOffset())), e);
        return;
    }

    try {
        awaitThread = Thread.currentThread();

        // Loop waiting for a connection and a valid command
        while (!stopAwait) {
            ServerSocket serverSocket = awaitSocket;
            if (serverSocket == null) {
                break;
            }

            // Wait for the next connection
            Socket socket = null;
            StringBuilder command = new StringBuilder();
            try {
                InputStream stream;
                long acceptStartTime = System.currentTimeMillis();
                try {
                    socket = serverSocket.accept();
                    socket.setSoTimeout(10 * 1000);  // Ten seconds
                    stream = socket.getInputStream();
                } catch (SocketTimeoutException ste) {
                    // This should never happen but bug 56684 suggests that
                    // it does.
                    log.warn(sm.getString("standardServer.accept.timeout",
                            Long.valueOf(System.currentTimeMillis() - acceptStartTime)), ste);
                    continue;
                } catch (AccessControlException ace) {
                    log.warn(sm.getString("standardServer.accept.security"), ace);
                    continue;
                } catch (IOException e) {
                    if (stopAwait) {
                        // Wait was aborted with socket.close()
                        break;
                    }
                    log.error(sm.getString("standardServer.accept.error"), e);
                    break;
                }

                // Read a set of characters from the socket
                int expected = 1024; // Cut off to avoid DoS attack
                while (expected < shutdown.length()) {
                    if (random == null)
                        random = new Random();
                    expected += (random.nextInt() % 1024);
                }
                while (expected > 0) {
                    int ch = -1;
                    try {
                        ch = stream.read();
                    } catch (IOException e) {
                        log.warn(sm.getString("standardServer.accept.readError"), e);
                        ch = -1;
                    }
                    // Control character or EOF (-1) terminates loop
                    if (ch < 32 || ch == 127) {
                        break;
                    }
                    command.append((char) ch);
                    expected--;
                }
            } finally {
                // Close the socket now that we are done with it
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException e) {
                    // Ignore
                }
            }

            // Match against our command string
            boolean match = command.toString().equals(shutdown);
            if (match) {
                log.info(sm.getString("standardServer.shutdownViaPort"));
                break;
            } else
                log.warn(sm.getString("standardServer.invalidShutdownCommand", command.toString()));
        }
    } finally {
        ServerSocket serverSocket = awaitSocket;
        awaitThread = null;
        awaitSocket = null;

        // Close the server socket and return
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}


/**
  * @return the specified Service (if it exists); otherwise return
  * <code>null</code>.
  *
  * @param name Name of the Service to be returned
  */
@Override
public Service findService(String name) {
    if (name == null) {
        return null;
    }
    synchronized (servicesLock) {
        for (Service service : services) {
            if (name.equals(service.getName())) {
                return service;
            }
        }
    }
    return null;
}


/**
  * @return the set of Services defined within this Server.
  */
@Override
public Service[] findServices() {
    return services;
}

/**
  * @return the JMX service names.
  */
public ObjectName[] getServiceNames() {
    ObjectName onames[]=new ObjectName[ services.length ];
    for( int i=0; i<services.length; i++ ) {
        onames[i]=((StandardService)services[i]).getObjectName();
    }
    return onames;
}


/**
  * Remove the specified Service from the set associated from this
  * Server.
  *
  * @param service The Service to be removed
  */
@Override
public void removeService(Service service) {

    synchronized (servicesLock) {
        int j = -1;
        for (int i = 0; i < services.length; i++) {
            if (service == services[i]) {
                j = i;
                break;
            }
        }
        if (j < 0)
            return;
        try {
            services[j].stop();
        } catch (LifecycleException e) {
            // Ignore
        }
        int k = 0;
        Service results[] = new Service[services.length - 1];
        for (int i = 0; i < services.length; i++) {
            if (i != j)
                results[k++] = services[i];
        }
        services = results;

        // Report this property change to interested listeners
        support.firePropertyChange("service", service, null);
    }

}
```

### Lifecycle相关模板方法

这里只展示startInternal方法

```java
/**
 * Start nested components ({@link Service}s) and implement the requirements
 * of {@link org.apache.catalina.util.LifecycleBase#startInternal()}.
 *
 * @exception LifecycleException if this component detects a fatal error
 *  that prevents this component from being used
 */
@Override
protected void startInternal() throws LifecycleException {

    fireLifecycleEvent(CONFIGURE_START_EVENT, null);
    setState(LifecycleState.STARTING);

    globalNamingResources.start();

    // Start our defined Services
    synchronized (servicesLock) {
        for (int i = 0; i < services.length; i++) {
            services[i].start();
        }
    }

    if (periodicEventDelay > 0) {
        monitorFuture = getUtilityExecutor().scheduleWithFixedDelay(
                new Runnable() {
                    @Override
                    public void run() {
                        startPeriodicLifecycleEvent();
                    }
                }, 0, 60, TimeUnit.SECONDS);
    }
}
    
protected void startPeriodicLifecycleEvent() {
    if (periodicLifecycleEventFuture == null || (periodicLifecycleEventFuture != null && periodicLifecycleEventFuture.isDone())) {
        if (periodicLifecycleEventFuture != null && periodicLifecycleEventFuture.isDone()) {
            // There was an error executing the scheduled task, get it and log it
            try {
                periodicLifecycleEventFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                log.error(sm.getString("standardServer.periodicEventError"), e);
            }
        }
        periodicLifecycleEventFuture = getUtilityExecutor().scheduleAtFixedRate(
                new Runnable() {
                    @Override
                    public void run() {
                        fireLifecycleEvent(Lifecycle.PERIODIC_EVENT, null);
                    }
                }, periodicEventDelay, periodicEventDelay, TimeUnit.SECONDS);
    }
}
```

方法的第一行代码先触发 CONFIGURE_START_EVENT 事件，以便执行 StandardServer 的 LifecycleListener 监听器，然后调用 setState 方法设置成 LifecycleBase 的 state 属性为 LifecycleState.STARTING。 接着就 globalNamingResources.start()，跟 initInternal 方法其实是类似的。

再接着就调用 Service 的 start 方法来启动 Service 组件。可以看出，StandardServe 的 startInternal 跟 initInternal 方法类似，都是调用内部的 service 组件的相关方法。

调用完 service.init 方法后，就使用 getUtilityExecutor() 返回的线程池延迟执行startPeriodicLifecycleEvent 方法，而在 startPeriodicLifecycleEvent 方法里，也是使用 getUtilityExecutor() 方法，定期执行 fireLifecycleEvent 方法，处理 Lifecycle.PERIODIC_EVENT 事件，如果有需要定期处理的，可以再 Server 的 LifecycleListener 里处理 Lifecycle.PERIODIC_EVENT 事件。


<!-- @include: @article-footer.snippet.md -->    