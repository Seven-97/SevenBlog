---
title: 网络编程 - BIO详解
category: Java
tag:
 - IO流
---





关于同步/异步，阻塞/非阻塞，Unix IO模型，可以先看这篇文章[网络系统 - Unix IO模型](https://www.seven97.top/cs-basics/operating-system/selectpollepoll.html)

## BIO概述

阻塞式IO。也就是说io没有就绪的时候，操作IO当前线程会被阻塞。也就是用户线程需要等待IO线程完成

服务器实现模式为**一个连接一个线程**，也就是说，客户端每当有一个连接请求的时候，服务器就需要启动一个对应线程进行处理。但是如果这个连接不做任何事情，就会造成不必要的线程开销。这种模型一般适用于连接数目小且固定的架构。

BIO 其实就是 [Reactor的 单reactor 单进程/线程模型](https://www.seven97.top/cs-basics/operating-system/reactor-proactor.html#单-reactor-单进程-线程)

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250756371.jpg)

### BIO的问题

- 同一时间，服务器只能接受来自于客户端A的请求信息；虽然客户端A和客户端B的请求是同时进行的，但客户端B发送的请求信息只能等到服务器接受完A的请求数据后，才能被接受。

- 由于服务器一次只能处理一个客户端请求，当处理完成并返回后(或者异常时)，才能进行第二次请求的处理。很显然，这样的处理方式在高并发的情况下，是不能采用的。

### 多线程方式 - 伪异步方式

上面说的情况是服务器只有一个线程的情况，那么我们就能想到使用多线程技术来解决这个问题:

- 当服务器收到客户端X的请求后，(读取到所有请求数据后)将这个请求送入一个独立线程进行处理，然后主线程继续接受客户端Y的请求。

- 客户端一侧，也可以使用一个子线程和服务器端进行通信。这样客户端主线程的其他工作就不受影响了，当服务器端有响应信息的时候再由这个子线程通过 监听模式/观察模式(等其他设计模式)通知主线程。

如下图所示:

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404250756383.jpg)

这种方式其实就是[Reactor 的 单reactor 多线程/多进程模型](https://www.seven97.top/cs-basics/operating-system/reactor-proactor.html#单-reactor-多线程-多进程)，同样有是有局限性的，因此也就有了后文的[NIO方案](https://www.seven97.top/java/io/04-networkprogramming2-nio.html)：

- 虽然在服务器端，请求的处理交给了一个独立线程进行，但是操作系统通知accept()的方式还是单个的。也就是，实际上是服务器接收到数据报文后的“业务处理过程”可以多线程，但是数据报文的接受还是需要一个一个的来

- 在linux系统中，可以创建的线程是有限的。可以通过`cat /proc/sys/kernel/threads-max` 命令查看可以创建的最大线程数。当然这个值是可以更改的，但是线程越多，CPU切换所需的时间也就越长，用来处理真正业务的需求也就越少。

- 创建一个线程是有较大的资源消耗的。JVM创建一个线程的时候，即使这个线程不做任何的工作，JVM都会分配一个堆栈空间。这个空间的大小默认为128K，可以通过-Xss参数进行调整。当然还可以使用ThreadPoolExecutor线程池来缓解线程的创建问题，但是又会造成BlockingQueue积压任务的持续增加，同样消耗了大量资源。

- 另外，如果应用程序大量使用长连接的话，线程是不会关闭的。这样系统资源的消耗更容易失控。 那么，如果真想单纯使用线程解决阻塞的问题，那么自己都可以算出来您一个服务器节点可以一次接受多大的并发了。看来，单纯使用线程解决这个问题不是最好的办法。

## BIO通信方式深入分析

BIO的问题关键不在于是否使用了多线程(包括线程池)处理这次请求，而在于accept()、read()的操作点都是被阻塞。要测试这个问题，也很简单。这里模拟了20个客户端(用20根线程模拟)，利用JAVA的同步计数器CountDownLatch，保证这20个客户都初始化完成后然后同时向服务器发送请求，然后观察一下Server这边接受信息的情况。

以下案例[源码点击这里](https://github.com/Seven-97/IODemo/tree/master/01-bio-demo)

### 服务器端使用单线程

- 客户端代码(SocketClientDaemon)，模拟20个客户端连接

```java
public class SocketClientDaemon {
    public static void main(String[] args) throws Exception {
        Integer clientNumber = 20;
        CountDownLatch countDownLatch = new CountDownLatch(clientNumber);

        //分别开始启动这20个客户端
        for (int index = 0; index < clientNumber; index++) {
            SocketClientRequestThread client = new SocketClientRequestThread(countDownLatch, index);
            new Thread(client).start();
            countDownLatch.countDown();
        }

        //这个wait不涉及到具体的实验逻辑，只是为了保证守护线程在启动所有线程后，进入等待状态
        synchronized (SocketClientDaemon.class) {
            SocketClientDaemon.class.wait();
        }
    }
}
```



- 客户端代码(SocketClientRequestThread模拟20个请求)

```java
@Slf4j
public class SocketClientRequestThread implements Runnable {

    private CountDownLatch countDownLatch;

    //线程编号
    private Integer clientIndex;

    /**
     * countDownLatch是java提供的同步计数器。
     * 当计数器数值减为0时，所有受其影响而等待的线程将会被激活。这样保证模拟并发请求的真实性
     *
     * @param countDownLatch
     */
    public SocketClientRequestThread(CountDownLatch countDownLatch, Integer clientIndex) {
        this.countDownLatch = countDownLatch;
        this.clientIndex = clientIndex;
    }

    @Override
    public void run() {
        Socket socket = null;
        OutputStream clientRequest = null;
        InputStream clientResponse = null;

        try {
            socket = new Socket("localhost", 83);
            clientRequest = socket.getOutputStream();
            clientResponse = socket.getInputStream();

            //等待，直到SocketClientDaemon完成所有线程的启动，然后所有线程一起发送请求
            this.countDownLatch.await();

            //发送请求信息
            clientRequest.write(("这是第" + this.clientIndex + " 个客户端的请求。").getBytes());
            clientRequest.flush();

            //在这里等待，直到服务器返回信息
            log.info("第{}个客户端的请求发送完成，等待服务器返回信息", this.clientIndex);
            int maxLen = 1024;
            byte[] contextBytes = new byte[maxLen];
            int realLen;
            String message = "";

            //程序执行到这里，会一直等待服务器返回信息(注意，前提是in和out都不能close，如果close了就收不到服务器的反馈了)
            while ((realLen = clientResponse.read(contextBytes, 0, maxLen)) != -1) {
                message += new String(contextBytes, 0, realLen);
            }
            log.info("接收到来自服务器的信息:{}", message);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                if (clientRequest != null) {
                    clientRequest.close();
                }
                if (clientResponse != null) {
                    clientResponse.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
```



- 服务器端(SocketServer) 单个线程

```java
@Slf4j
public class SocketServer {

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(83);

        try {
            while (true) {
                //这里会被阻塞，直到能获取到连接
                Socket socket = serverSocket.accept();

                //下面开始收取信息
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();

                //获取端口
                Integer sourcePort = socket.getPort();
                int maxLen = 2048;
                byte[] contextBytes = new byte[maxLen];

                //这里会被阻塞，直到有数据准备好
                int realLen = in.read(contextBytes, 0, maxLen);
                //读取信息
                String message = new String(contextBytes, 0, realLen);

                //打印信息
                log.info("服务器收到来自于端口: {}的信息: {}", sourcePort, message);

                Thread.sleep(10000);//模拟执行业务逻辑
                //开始发送信息
                out.write("回发响应信息！".getBytes());

                //关闭
                out.close();
                in.close();
                socket.close();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (serverSocket != null) {
                serverSocket.close();
            }
        }
    }
}
```

经过执行就会发现，服务器一次只能处理一个客户端请求，当处理完成并返回后(或者异常时)，才能进行第二次请求的处理。这就是上面提到的BIO存在的问题



### 优化服务器端为多线程

客户端代码和上文一样，最主要是更改服务器端的代码:

```java
@Slf4j
public class SocketServer {

    static {
        BasicConfigurator.configure();
    }

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(83);

        try {
            while (true) {
                Socket socket = serverSocket.accept();
                //业务处理过程可以交给一个线程(这里可以使用线程池),并且线程的创建是很耗资源的。
                //但最终还是改变不了.accept()只能一个一个接受socket的情况,并且被阻塞的情况
                SocketServerThread socketServerThread = new SocketServerThread(socket);
                new Thread(socketServerThread).start();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (serverSocket != null) {
                serverSocket.close();
            }
        }
    }
}

@Slf4j
class SocketServerThread implements Runnable {

    private Socket socket;

    public SocketServerThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        InputStream in = null;
        OutputStream out = null;
        try {
            //下面收取信息
            in = socket.getInputStream();
            out = socket.getOutputStream();
            Integer sourcePort = socket.getPort();
            int maxLen = 1024;
            byte[] contextBytes = new byte[maxLen];
            //使用线程，同样无法解决read方法的阻塞问题，
            //也就是说read方法处同样会被阻塞，直到操作系统有数据准备好
            int realLen = in.read(contextBytes, 0, maxLen);
            //读取信息
            String message = new String(contextBytes, 0, realLen);

            log.info("服务器收到来自于端口: " + sourcePort + "的信息: " + message);

            Thread.sleep(10000);//模拟执行业务逻辑
            //下面开始发送信息
            out.write("回发响应信息！".getBytes());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            //关闭资源
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
                if (this.socket != null) {
                    this.socket.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
```

这里与单线程相比，使用了多线程来处理具体的业务。但还是改变不了.accept()只能一个一个阻塞处理 socket的情况



### 问题根源

那么重点的问题并不是“是否使用了多线程”，而是为什么accept()、read()方法会被阻塞。

API文档中对于 serverSocket.accept() 方法的使用描述：

> Listens for a connection to be made to this socket and accepts it. The method blocks until a connection is made.
> 翻译一下：监听与此套接字的连接并接受它。该方法会一直阻塞，直到建立连接为止。

这主要就涉及到阻塞式同步IO的工作原理:

1. 服务器线程发起一个accept动作，询问操作系统 是否有新的socket套接字信息从端口X发送过来。accept源码如下：
   ```java
   // java.net.ServerSocket#accept
   public Socket accept() throws IOException {
       if (isClosed())
           throw new SocketException("Socket is closed");
       if (!isBound())
           throw new SocketException("Socket is not bound yet");
       Socket s = new Socket((SocketImpl) null);
       implAccept(s);//显然会走到这个逻辑
       return s;
   }
   
   //java.net.ServerSocket#implAccept(java.net.Socket)
   protected final void implAccept(Socket s) throws IOException {
       SocketImpl si = s.impl;
       
       // Socket has no SocketImpl
       if (si == null) {//上面传进来的null
           si = implAccept();
           s.setImpl(si);
           s.postAccept();
           return;
       }
   
       //...省略
       s.postAccept();
   }
   
   //java.net.ServerSocket#implAccept()
   private SocketImpl implAccept() throws IOException {
       if (impl instanceof PlatformSocketImpl) {
           return platformImplAccept();
       } else {
           //...省略
       }
   }
   
   //java.net.ServerSocket#platformImplAccept
   private SocketImpl platformImplAccept() throws IOException {
       assert impl instanceof PlatformSocketImpl;
   
       // create a new platform SocketImpl and accept the connection
       SocketImpl psi = SocketImpl.createPlatformSocketImpl(false);
       implAccept(psi);
       return psi;
   }
   
   //java.net.ServerSocket#platformImplAccept
   private void implAccept(SocketImpl si) throws IOException {
      assert !(si instanceof DelegatingSocketImpl);
   
      // accept a connection
     impl.accept(si);
   
     //...省略
   }
   
   
   //java.net.AbstractPlainSocketImpl#accept
   protected void accept(SocketImpl si) throws IOException {
       si.fd = new FileDescriptor();
       acquireFD();
       try {
           socketAccept(si);
       } finally {
           releaseFD();
       }
       SocketCleanable.register(si.fd, true);
   }
   ```

   

2. 注意，是询问操作系统。也就是说socket套接字的IO模式支持是基于操作系统的，那么自然同步IO/异步IO的支持就是需要操作系统级别的了。如下：
   ```java
   // java.net.PlainSocketImpl#socketAccept
   void socketAccept(SocketImpl s) throws IOException {
           int nativefd = checkAndReturnNativeFD();
   
           if (s == null)
               throw new NullPointerException("socket is null");
   
           int newfd = -1;
           InetSocketAddress[] isaa = new InetSocketAddress[1];
           if (timeout <= 0) { //如果没有设置timeout，那么在调用JNI时会一直等待，直到有数据返回
               newfd = accept0(nativefd, isaa);//这是个JNI方法
           } else {
               configureBlocking(nativefd, false);
               try {
                   waitForNewConnection(nativefd, timeout);
                   newfd = accept0(nativefd, isaa);
                   if (newfd != -1) {
                       configureBlocking(newfd, true);
                   }
               } finally {
                   configureBlocking(nativefd, true);
               }
           }
           /* Update (SocketImpl)s' fd */
           fdAccess.set(s.fd, newfd);
           /* Update socketImpls remote port, address and localport */
           InetSocketAddress isa = isaa[0];
           s.port = isa.getPort();
           s.address = isa.getAddress();
           s.localport = localport;
           if (preferIPv4Stack && !(s.address instanceof Inet4Address))
               throw new SocketException("Protocol family not supported");
   }
   
   // java.net.PlainSocketImpl#accept0
   static native int accept0(int fd, InetSocketAddress[] isaa) throws IOException;
   ```

   

最后调用的accept0十个native方法，就是调用的操作系统级别的accept。因此如果操作系统没有发现有套接字从指定的端口X来，那么操作系统就会等待。这样serverSocket.accept()方法就会一直等待。这就是为什么accept()方法为什么会阻塞: 它**内部的实现是使用的操作系统级别的同步IO**

 

 

 

 

 

 <!-- @include: @article-footer.snippet.md -->     

 

 