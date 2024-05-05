---
​---
title: 反编译工具 - jadx
category: 开发工具
tag:
 - 反编译
​---
---



## 项目介绍

jadx 是一款功能强大的 Java 反编译工具，基于 Java 开发，使用起来简单方便（拖拽式操作），不光提供了命令行程序，还提供了 GUI 程序。一般情况下，我们直接使用 GUI 程序就可以了。

jadx 支持 Windows、Linux、 macOS，能够打开`.apk`, `.dex`, `.jar`,`.zip`等格式的文件



比如需要反编译一个 jar 包查看其源码的话，直接将 jar 包拖入到 jadx 中就可以了。效果如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405051049865.webp)



再比如想看看某个 apk 的源码，我们拿到 apk 之后直接拖入进 jadx 中就可以了。效果如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405051050494.webp)



## jadx 安装

jadx 是一款开源软件，是可以免费使用的。可以在 jadx 的项目主页下载 jadx 最新版。

- 项目地址：**https://github.com/skylot/jadx**
- 下载地址：**https://github.com/skylot/jadx/releases**



选择其中的一个版本进行下载即可，目前最新版本是 1.4.7，这里选择 1.3.1 版本进行演示。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405051053091.webp)



下载之后，解压下载好的 jadx 压缩文件后进入 `bin` 目录即可找到可执行文件。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405051053284.webp)

- jadx：命令行版本
- jadx-gui：图形操作界面版本



也可以自己克隆源码，本地编译，这也是比较推荐的方式。

```
git clone https://github.com/skylot/jadx.git
```

jadx 由 Java 语言编写，使用 Gradle 进行构建。克隆到本地之后，可以直接使用 Gradle 命令进行构建：

```
cd jadx
# Windows 平台使用 gradlew.bat 而不是 ./gradlew
./gradlew dist
```

也可以直接使用 IDE 打开，然后像运行普通 Java 程序那样使用它：

![图片](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405051054898.webp)



## jadx 使用

注意：Jadx 无法反编译 100% 的代码，因此可能会出现错误。如果遇到错误，可以参考常见问题解答：**https://github.com/skylot/jadx/wiki/Troubleshooting-Q&A** 。

### 反编译文件

通过 `File -> Open files...` 打开需要反编译的文件或者直接将文件拖拽进 jadx 中就可以了。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405051055220.webp)



从上图可以看出，jadx 支持`.apk`, `.dex`, `.jar`,`.zip`,`.class`等格式的文件。

### 搜索功能

jadx 自带强大的搜索功能，支持多种匹配模式。

通过 `Navigation` 即可打开搜索功能，我们可以选择搜索指定的类，方法，属性，代码，文件，甚至是注释。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405051055595.webp)

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405051055270.webp)



### 查看类,变量或者方法使用情况

对于某个类、变量或者方法，我们还可以查看哪些地方使用了它。

直接选中对应的类、变量或者方法，然后点击右键选择 Find Usage 即可。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405051055584.webp)

很快，jadx 就会帮你找出整个项目有哪些地方使用了它。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405051058221.webp)

### 添加注释

还可以自定义注释到源代码中。

选中对应的位置之后，点击右键选择 Comment 即可。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405051059376.webp)

### 反混淆

一般情况下，为了项目的安全，在打包发布一个 apk 之前都会对其代码进行混淆加密比如用无意义的短变量去重命名类、变量、方法，以免代码被轻易破解泄露。

经过混淆的代码在功能上是没有变化的，但是去掉了部分名称中的语义信息。



为了代码的易读性，可以对代码进行反混淆。

在 jadx 中，我们通过 Tools -> Deobfuscation 即可开启反混淆功能。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405051059812.webp)



















