---
title: Maven入门，读完这篇就够了
category: 开发工具
icon: configuration
tags:
  - Maven
head:
  - - meta
    - name: keywords
      content: Maven,打包命令,Maven标签,依赖管理
  - - meta
    - name: description
      content: 全网最全的Maven知识点总结，让天下没有难学的八股文！
---





## Maven 项⽬⽣命周期

Maven从项⽬的三个不同的⻆度，定义了三套⽣命周期，三套⽣命周期是相互独⽴的，它们之间不会相互影响。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202407192157695.png)

- 清理⽣命周期(Clean Lifecycle)：该⽣命周期负责清理项⽬中的多余信息，保持项⽬资源和代码的整洁性。⼀般拿来清空directory(即⼀般的target)⽬录下的⽂件。
- 默认构建⽣命周期(Default Lifeclyle)：该⽣命周期表示这项⽬的构建过程，定义了⼀个项⽬的构建要经过的不同的阶段。
- 站点管理⽣命周期(Site Lifecycle)：向我们创建⼀个项⽬时，我们有时候需要提供⼀个站点，来介绍这个项⽬的信息，如项⽬介绍，项⽬进度状态、项⽬组成成员，版本控制信息，项⽬javadoc索引信息等等。站点管理⽣命周期定义了站点管理过程的各个阶段



## 常用命令

### 常用打包命令

```css
mvn clean package -Dmaven.test.skip=true		-- 跳过单测打包
mvn clean install -Dmaven.test.skip=true		-- 跳过单测打包，并把打好的包上传到本地仓库
mvn clean deploy -Dmaven.test.skip=true			-- 跳过单测打包，并把打好的包上传到远程仓库
```



### 其他命令

```css
mvn -v //查看版本 
mvn archetype:create //创建 Maven 项目 
mvn compile //编译源代码 
mvn test-compile //编译测试代码 
mvn test //运行应用程序中的单元测试 
mvn site //生成项目相关信息的网站 
mvn package //依据项目生成 jar 文件 
mvn package -P profileName //指定profile进行打包,依据项目生成 jar 文件 
mvn install //在本地 Repository 中安装 jar 
mvn -Dmaven.test.skip=true //忽略测试文档编译 
mvn clean //清除目标目录中的生成结果 
mvn clean compile //将.java类编译为.class文件 
mvn clean package //进行打包 
mvn clean test //执行单元测试 
mvn clean deploy //部署到版本仓库 
mvn clean install //使其他项目使用这个jar,会安装到maven本地仓库中 
mvn archetype:generate //创建项目架构 
mvn dependency:list //查看已解析依赖 
mvn dependency:tree com.xx.xxx //看到依赖树 
mvn dependency:analyze //查看依赖的工具 
mvn help:system //从中央仓库下载文件至本地仓库 
mvn help:active-profiles //查看当前激活的profiles 
mvn help:all-profiles //查看所有profiles 
mvn help:effective -pom //查看完整的pom信息
```



## 标签解释

常用标签详解

```xml
<!-- project 是根标签-->
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    
    <!--指定父项目的坐标。如果项目中没有规定某个元素的值，那么父项目中的对应值即为项目的默认值。 坐标包括group ID，artifact ID和 version。--> 
    <parent> 
         <!--继承的父项目的构件标识符--> 
         <artifactId>maventest</artifactId>
         <!--继承的父项目的全球唯一标识符--> 
         <groupId>com.seven</groupId>
         <!--继承的父项目的版本:大版本.次版本.小版本 ;snapshot快照 alpha内部测试 beta公测 release稳定 GA正式发布--> 
         <version>1.0.0-SNAPSHOT</version>
         <!-- 父项目的pom.xml文件的相对路径。相对路径允许你选择一个不同的路径。默认值是../pom.xml。Maven首先在构建当前项目的地方寻找父项 目的pom，其次在文件系统的这个位置（relativePath位置），然后在本地仓库，最后在远程仓库寻找父项目的pom。--> 
         <relativePath/> 
 	</parent> 
    
    <!-- 当前项目的标识 -->
     <!--项目的全球唯一标识符，通常使用全限定的包名区分该项目和其他项目。并且构建时生成的路径也是由此生成， 如com.mycompany.app生成的相对路径为：/com/mycompany/app-->  
    <groupId>com.seven.ch</groupId>  
    <!-- 构件的标识符，它和group ID一起唯一标识一个构件。换句话说，你不能有两个不同的项目拥有同样的artifact ID和groupID；在某个特定的group ID下，artifact ID也必须是唯一的-->  
    <artifactId>Question8_1</artifactId>
    <!--项目产生的构件类型，例如jar、war、ear、pom。插件也可以创建自己的构件类型，所以前面列的不是全部构件类型-->  
    <packaging>jar</packaging>  
    <!--项目当前版本，格式为:主版本.次版本.增量版本-限定版本号-->  
    <version>1.0-SNAPSHOT</version>  
    <!-- 项目 描述名-->
    <name></name>
    <!--项目主页的URL, Maven产生的文档用-->  
    <url></url>
    <!-- 项目的详细描述, Maven 产生的文档用。-->
    <description></description>
    
    <!-- 集合多个子模块，在父中设置-->
    <modules></modules>
    <!--指定了当前的pom的版本-->
    <modelVersion>4.0.0</modelVersion>
    
    <!--在列的项目构建profile，如果被激活，会修改构建处理；一般在子pom中设置--> 
    <profiles>
        <!--根据环境参数或命令行参数激活某个构建处理--> 
        <profile>
            <id>betanoah</id>
            <properties>
                <deploy.type>betanoah</deploy.type>
            </properties>
        </profile>
    </profiles>
    
    <!--定义标签，一般在父pom中设置--> 
    <properties>
        <!-- 自定义便签，设置依赖版本 -->
        <java_target_version>11</java_target_version>
        <java_source_version>11</java_source_version>
		<junit.junit>4.12</junit.junit>
        <spring-boot.version>2.6.6</spring-boot.version>
    </properties>
  
    <!-- 继承自该项目的所有子项目的默认依赖信息。这部分的依赖信息不会被立即解析,而是当子项目声明一个依赖（必须描述group ID和 artifact ID信息），如果group ID和artifact ID以外的一些信息没有描述，则通过group ID和artifact ID 匹配到这里的依赖，并使用这里的依赖信息。--> 
    <!-- 一般在父pom文件里配置 -->
     <dependencyManagement> 
          <dependencies> 
               <!--参见dependencies/dependency元素--> 
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
               //...
          </dependencies> 
     </dependencyManagement>    
    
    <!-- 依赖列表，一般只在子pom文件里配置，父pom文件只做依赖的版本管理 -->
    <dependencies>
        <dependency>
            <!-- 指定坐标从而知道依赖的是哪个项目-->
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <!-- 这个类的依赖范围-->
            <scope></scope>
            <!-- 设置依赖是否可选   默认false 是默认继承-->
            <optional></optional>
            <!-- 排除依赖传递列表-->
            <exclusions>
                <exclusion></exclusion>
            </exclusions>
        </dependency>
    </dependencies>
   
    <!-- 插件列表-->
    <build>
        <plugins></plugins>
    </build>
</project>
```



## 依赖管理

Maven项目，依赖，构建配置，以及构件：所有这些都是要建模和表述的对象。这些对象通过一个名为项目对象模型(Project Object Model, POM)的XML文件描述。

POM是Maven项目管理和构建的核心文件，它通常是一个名为`pom.xml`的XML文件。POM文件包含了项目的所有配置信息，Maven通过这些信息来构建项目、管理依赖以及执行其他构建任务。

这个POM告诉Maven它正处理什么类型的项目，如何修改默认的行为来从源码生成输出。同样的方式，一个Java Web应用有一个web.xml文件来描述，配置，及自定义该应用，一个Maven项目则通过一个 pom.xml 文件定义。该文件是Maven中一个项目的描述性陈述；也是当Maven构建项目的时候需要理解的一份“地图”。

### 坐标详解

坐标，其实就是从众多jar包中找到需要的那个jar包

#### 传递性依赖

先考虑一个基于Spring Framework 的项目，如果不使用Maven, 那么在项目中就需要手动 下载相关依赖。由于Spring Framework 又会依赖于其他开源类库，因此实际中往往会下载一个很大的如 spring-framework-2.5.6-with-dependencies.zip 的包，这里包含了所有Spring Framework 的 jar包，以及所有它依赖的其他 jar包。这么做往往就引入了很多不必要的依赖。另一种做法是只下载 spring-framework-2.5.6.zip 这样一个包，这里不包含其他相关依赖，到实际使用的时候，再根据出错信息，或者查询相关文档，加入需要的其他依赖。

Maven 的传递性依赖机制可以很好地解决这一问题。

传递性依赖就是，当项目A依赖于B，而B又依赖于C的时候，自然的A会依赖于C，这样Maven在建立项目A的时候，会自动加载对C的依赖。

#### groupId

```xml
<groupId>org.sonatype.nexus</groupId>
<artifactId>nexus-indexer</artifactId>
<version>2.0.0<Nersion>
<packaging>jar<packaging>
```

这是 nexus-indexer 的坐标定义， nexus-indexer 是一个对Maven 仓库编纂索引并提供搜索功能的类库，它是 Nexus 项目的一个子模块。后面会详细介绍 Nexus。上述代码片段中，其坐标分别为 groupld:org.sonatype.nexus、artifactld:nexus-indexer、version:2.0.0、packaging: jar, 没有 classifier。下面详细解释一下各个坐标元素：

- groupId：定义当前 Maven 项目隶属的实际项目。首先， Maven 项目和实际项目不一定是一对一的关系。比如 Spring Framework 这一实际项目，其对应的 Maven 项目会有很多，如 spring-core、spring-context 等。这是由于Maven 中模块的概念，因此，一个实际项目往往会被划分成很多模块。其次， groupId不应该对应项目隶属的组织或公司。原因很简单，一个组织下会有很多实际项目，如果 groupId 只定义到组织级别， 而后面我们会看到，artifactId 只能定义Maven 项目(模块), 那么实际项目这个层将难以定义。最后， groupId 的表示方式与Java包名的表示方式类似，通常与域名反向一一对应。上例中， groupId 为 org.sonatype.nexus, org.sonatype 表示 Sonatype 公司建立的一 个非盈利性组织，nexus 表示 Nexus 这一实际项目，该 groupId 与域名 nexus.sonatype.org 对应。

- artifactId：该元素定义实际项目中的一个Maven项目(模块), 推荐的做法是使用实际项目名称作为 artifactId 的前缀。比如上例中的 artifactId 是 nexus-indexer, 使用了实际项目名 nexus 作为前缀，这样做的好处是方便寻找实际构件。在默认情况下， Maven生成的构件，其文件名会以 artifactId 作为开头，如 nexus-indexer-2.0.0.jar, 使用实际项目名称作为前缀之后，就能方便从一个 lib文件夹中找到某个项目的一组构件。考虑有5个项目，每个项目都有一个 core模块，如果没有前缀，我们会看到很多 core-1.2.jar这样的文件，加上实际项目名前缀之后，便能很容易区分 foo-core-1.2.jar、bar-core-1.2.jar … … 。

- version：该元素定义Maven 项目当前所处的版本，如上例中 nexus-indexer 的版本是 2.0.0。需要注意的是， Maven 定义了一套完整的版本规范，以及快照 (SNAPSHOT)的概念。

- packaging：该元素定义 Maven 项目的打包方式。首先，打包方式通常与所生成构件的文件扩展名对应， 如上例中 packaging 为 jar, 最终文件名为 nexus-indexer-2.0.0.jar, 而使用 war 打包方式的Maven 项目，最终生成的构件会有一个 .war 文件， 不过这不是绝对的。其次，打包方式会影响到构建的生命周期，比如 jar打包和 war打包会使用不同的命令。最后，当不定义 packaging 的时候，Maven 会使用默认值 jar。

- classifier：该元素用来帮助定义构建输出的一些附属构件。附属构件与主构件对应， 如上例中的主构件是 nexus-indexer-2.0.0.jar, 该项目可能还会通过使用一些插件生成如nexus-indexer-2.0.0-javadoc.jar、nexus-indexer-2.0.0-sources. jar 这样一些附属构件，其包含了Java 文档和源代码。这时候， javadoc和 sources 就是这两个附属构件的classifier。这样，附属构件也就拥有了自己唯一的坐标。还有一个关于classifier 的典型例子是 TestNG, TestNG 的主构件是基于Java 1.4平台的，而它又提供了一个classifier为 jdk5 的附属构件。注意，不能直接定义项目的 classifier, 因为附属构件不是项目直接默认生成的，而是由附加的插件帮助生成。

上述5个元素中， groupId、artifactId、version 是必须定义的， packaging是可选的(默认为jar), 而 classifier是不能直接定义的。

同时，项目构件的文件名是与坐标相对应的， 一般的规则为 artifactId-version [-classifier].packaging, [-classifier] 表示可选。比如上例 nexus-indexer 的主构件为 nexus-indexer-2.0.0.jar, 附属构件有 nexus-indexer-2.0.0-javadoe.jar。这里还要强调的一点是，packaging 并非一定与构件扩展名对应，比如 packaging 为 maven-plugin 的构件扩展名为 jar。

此外， Maven 仓库的布局也是基于Maven 坐标，这一点会在介绍 Maven 仓库的时候详细解释。同样地，理解清楚 Maven 坐标之后，我们就能开始讨论Maven 的依赖管理了。

#### dependencies

在dependencies标签中添加需要添加的jar对应的Maven坐标

```xml
<project>
    ...
    <dependencies>
        <dependency>
            <groupId>...</groupId>
            <artifactId>...</artifactId>
            <version>...</version>
            <type>...</type>
            <scope>...</scope>
            <optional>...</optional>
            <exclusions>
                <exclusion>
                ...
                </exclusion>
            </exclusions>
        </dependency>
    </dependencies>
    ...
</project>
```

根元素 project 下的 dependencies 可以包含一个或者多个 dependency 元素，以声明一个或者多个项目依赖。每个依赖可以包含的元素有：

- groupId 、artifactId 和 version：依赖的基本坐标，对于任何一个依赖来说，基本坐标是最重要的， Maven 根据坐标才能找到需要的依赖。

- type：依赖的类型，对应于项目坐标定义的 packaging 。大部分情况下，该元素不必声明，其默认值为jar。

- scope：依赖的范围。

- optional：标记依赖是否可选。

- exclusions：用来排除传递性依赖。

大部分依赖声明只包含基本坐标，然而在一些特殊情况下，其他元素至关重要。

##### scope

| 依赖范围 | 编译有效 | 测试有效 | 运行时有效 | 打包有效 | 例子                    |
| -------- | -------- | -------- | ---------- | -------- | ----------------------- |
| Complie  | √        | √        | √          | √        | spring-core             |
| test     | ×        | √        | ×          | ×        | Junit                   |
| provided | √        | √        | ×          | ×        | servlet-api,lombok      |
| runtime  | ×        | √        | √          | √        | JDBC驱动                |
| system   | √        | √        | ×          | ×        | 本地maven仓库之外的类库 |
| import   | N/A      | N/A      | N/A        | N/A      | BOM文件                 |

##### optional

假设有这样一个依赖关系，项目A 依赖于项目B, 项目B 依赖于项目X 和Y, B 对于X 和Y 的依赖都是可选依赖：A->B、B->X(可选)、B->Y(可选)。根据传递性依赖的定义，如果所有这三个依赖的范围都是 compile, 那么 X、Y 就是A 的 compile 范围传递性依赖。然而，由于这里X、Y 是可选依赖，依赖将不会得以传递。换句话说， X、Y 将不会对 A有任何影响，如下图所示。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270842208.png)

为什么要使用可选依赖这一特性呢? 可能项目B 实现了两个特性，其中的特性一依赖于X, 特性二依赖于Y, 而且这两个特性是互斥的，用户不可能同时使用两个特性。比如 B 是一个持久层隔离工具包，它支持多种数据库，包括 MySQL、PostgreSQL 等，在构建这个工具包的时候，需要这两种数据库的驱动程序，但在使用这个工具包的时候，只会依赖一种数据库。

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.xiaoshan.mvnbook</groupId> 
    <artifactId>project-b</artifactId>
    <version>1.0.0</version>
    <dependencies>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId> 
            <version>5.1.10</version>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>8.4-701.jdbc3</version>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

上述 XML代码片段中，使用&lt;optional>元素表示 mysql-connector-java 和 postgresql 这两个依赖为可选依赖，它们只会对当前项目产生影响，当其他项目依赖于这个项目的时候，这两个依赖不会被传递。

 

因此，当项目A依赖于项目B的时候，如果其实际使用基于MySQL数据库，那么在项目A中就需要显式地声明 mysgl-connectorjava这一依赖，见以下代码清单。

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.xiaoshan.mvnbook</groupId>
    <artifactId>project-a</artifactId>
    <version>1.0.0</version>
    <dependencies>
        <dependency>
            <groupId>com.xiaoshan.mvnbook</groupId>
            <artifactId>project-b</artifactId>
            <version>1.0.0</version>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>5.1.10</version>
        </dependency>
    </dependencies>
</project>
```

但是实际上，在理想的情况下，是不应该使用可选依赖的。 使用可选依赖的原因是某一个项目实现了多个特性，在面向对象设计中，有个单一职责性原则，意指一个类应该只有一项职责，而不是糅合太多的功能。

这个原则在规划 Maven 项目的时候也同样适用。在上面的例子中，更好的做法是为MySQL 和 PostgreSQL分别创建一个 Maven 项目 ， 基于同样的 groupId 分配不同的artifactId, 如 com.xiaoshan. mvnbook:project-b-mysql 和 com.xiaoshan. mvnbook:project-b-postgresgl, 在各自的 POM 中声明对应的JDBC 驱动依赖，而且不使用可选依赖，用户则根据需要选择使用 pro-ject-b-mysql 或者 project-b-postgresql。 由于传递性依赖的作用，就不用再声明JDBC 驱动依赖。

##### 排除依赖exclusions

假设有这样一种依赖关系，A->B->C，这个时候由于某些原因，不需要对C的依赖，但是又必须要对B的依赖，针对这种情况，可以在添加A对B的依赖时申明不需要引进B对C的依赖。具体做法如下：

```xml
<dependency>
  <groupId>org.apache.struts</groupId>
  <artifactId>struts2-spring-plugin</artifactId>
  <version>2.5.20</version>
  <exclusions>
    <exclusion>
      <groupId>org.springframework</groupId>
      <artifactId>spring-beans</artifactId>
    </exclusion>
  </exclusions>
</dependency>
```



### 依赖冲突

#### 冲突产生的根本原因

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270842062.png)

由于传递依赖的原因，a会通过b引入c的依赖，也会通过d引入c的依赖，因此出现了冲突

| 依赖关系 | 实例           |
| -------- | -------------- |
| 直接依赖 | a和b的依赖关系 |
| 间接依赖 | a和c的依赖关系 |

#### 依赖冲突的解决方案

##### 路径最近者优先

Maven 依赖调解 (Dependency Mediation) 的第一原则是：路径最近者优先

项目A 有这样的依赖关系： A->B->C->X(1.0)、A->D->X(2.0)，根据路径最近者优先原则，X(1.0) 的路径长度为 3 , 而 X(2.0) 的路径长度为2, 因此X(2.0) 会被解析使用。

##### 第一优先声明

但是如果路径长度一样呢，如A->B->Y(1.0)、A-> C->Y(2.0)

从 Maven 2.0.9开始，第二原则是：第一优先声明，也就是谁先定义就使用谁的

##### 覆写优先原则

⼦ POM 内声明的依赖优先于⽗ POM 中声明的依赖。

1. 找到 Maven 加载的 Jar 包版本，使⽤ mvn dependency:tree 查看依赖树，根据依赖原则来调整依赖在POM ⽂件的声明顺序。
2. 发现了冲突的包之后，剩下的就是选择⼀个合适版本的包留下，如果是传递依赖的包正确，那么把显示依赖的包exclude掉。如果是某⼀个传递依赖的包有问题，那需要⼿动把这个传递依赖execlude掉



## 如何处理无法拉取的jar包

注：本文中所有解决方案均使用IDEA操作

### 设置了离线工作

有些用户的IDEA中可能设置了离线工作，这项设置会让IDEA无法连接网络，自然也无法下载所需资源了。要修改这一设置，具体操作如下：

点击File>>Settings，在弹出的菜单中选择Build,Execution,Deployment >> Build Tools >> Maven，然后查看页面中的Work Offline项是否处于勾选状态，如果是，则IDEA无法联网，应该取消勾选。如下图所示：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270843946.png)

 

### 配置文件问题

设置maven的 settings.xml文件的镜像 为阿里云镜像

```xml
	<mirror>
          <id>alimaven-new</id>
          <name>aliyun maven</name>
          <url>https://maven.aliyun.com/repository/central</url>
          <mirrorOf>central</mirrorOf>
    </mirror>
    
    <mirror>
        <id>aliyun-public</id>
        <mirrorOf>central</mirrorOf>
        <name>aliyun public</name>
        <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
```



### 手动下载

到以下网站寻找所需要的jar包：

https://repo.maven.apache.org/maven2/

 

根据控制台输出信息可知，需要的 com.github.spotbugs:spotbugs-maven-plugin:4.2.2

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270843327.png)

 

找到所需的jar包，进行下载

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202404270843124.png)

 

并放到本地的maven仓库中

 

 

 

 

 

 

 <!-- @include: @article-footer.snippet.md -->     

 





