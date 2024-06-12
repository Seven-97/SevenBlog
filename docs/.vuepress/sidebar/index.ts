import { sidebar } from "vuepress-theme-hope";

import { books } from "./books.js";


export default sidebar({
  // 应该把更精确的路径放置在前边
  "/books/": books,
  // 必须放在最后面
  "/": [
    {
      text: "Java",
      icon: "java",
      collapsible: true,
      prefix: "java/",
      children: [
        {
          text: "基础",
          prefix: "basis/",
          icon: "basic",
          collapsible: true,
          children: [
            "01-basic-knowledge",
            "02-generics",
            "03-annotations",
            "04-exceptions",
            "05-reflection",
            "06-SPI",
          ],
        },
        {
          text: "集合",
          prefix: "collection/",
          icon: "container",
          collapsible: true,
          children: [
            "01-collection-summary",
            "02-collection1-arraylist",
            "02-collection2-linkedlist",
            "02-collection3-priorityqueue",
            "02-collection4-queue-stack",
            "03-map1-hashset-hashmap",
            "03-map2-treeset-treemap",
            "03-map3-linkedhashset-map",
            "03-map4-weakhashmap",
            "04-juc1-copyonwritearrayList",
            "04-juc2-concurrenthashmap",
            "04-juc3-concurrentlinkedqueue",
            "05-collection-mining-pit",
          ],
        },
        {
          text: "并发编程",
          prefix: "concurrent/",
          icon: "et-performance",
          collapsible: true,
          children: [
            "01-fundamentalsofconcurrency1-theory",
            "01-fundamentalsofconcurrency2-thread",
            "01-fundamentalsofconcurrency3-lockofjava",
            "02-keyword1-synchronized",
            "02-keyword2-volatile",
            "02-keyword3-final",
            "03-juclock1-cas-unsafe-atomic",
            "03-juclock2-aqs",
            "03-juclock3-reentrantreadwritelock",
            "03-juclock4-locksupport",
            "04-threadpool1-threadpoolexecutor",
            "04-threadpool2-scheduledthreadpoolexecutor",
            "04-threadpool3-futuretask",
            "04-threadpool4-forkjoin",
            "04-threadpool5-tomcat",
            "05-concurrenttools7-completablefuture",
            "05-concurrenttools1-countdownlatch",
            "05-concurrenttools2-cyclicbarrier",
            "05-concurrenttools3-semaphore",
            "05-concurrenttools4-exchanger",
            "05-concurrenttools5-phaser",
            "05-concurrenttools6-condition",
            "06-threadlocal",
          ],
        },
        {
          text: "IO",
          prefix: "io/",
          icon: "code",
          collapsible: true,
          children: [
            "01-io-inputstream-sourcecode", 
            "02-io-outputstream-sourcecode", 
            "03-networkprogramming1-bio", 
            "04-networkprogramming2-nio", 
            "05-networkprogramming3-aio", 
          ],
        },
        {
          text: "JVM",
          prefix: "jvm/",
          icon: "virtual_machine",
          collapsible: true,
          children: [
            "01-jvmbasic1-classbytecode",
            "01-jvmbasic2-classloadingmechanism",
            "01-jvmbasic3-jvmmemorystructure",
            "02-gc1-theory",
            "02-gc2-g1",
            "02-gc3-zgc",
            "03-debug1-jvmparameter",
            "03-debug2-debugtools",
            "03-debug3-dynamicdebug",
            "03-debug4-threaddump",
            "03-debug5-cmsdebug",
          ],
        },
        {
          text: "版本新特性",
          prefix: "new-features/",
          icon: "featured",
          collapsible: true,
          children: [
            "java8",
            "java9",
            "java10-13",
            "java14-15",
            "java16-18",
            "java19",
            "java21",
            "java22",
            "upgrade-java8tojava11",
            "upgrade-java11tojava17",
          ],
        },
      ],
    },
    {
      text: "数据库",
      icon: "database",
      prefix: "database/",
      collapsible: true,
      children: [
        {
          text: "MySQL",
          prefix: "mysql/",
          icon: "mysql",
          collapsible: true,
          children: [
            "01-basement1-innodbstoragestructure ",
            "01-basement2-indexclassification ",
            "01-basement3-linkedlists",
            "01-basement4-logs",
            "01-basement5-transactions-isolationlevel",
            "02-lock1-lockofmysql",
            "02-lock2-howtoaddrowlocks",
            "03-execute1-statementexecutionprocess",
            "03-execute2-executionplan-explain",
            "04-tuning-overviewoftuning",
          ],
        },
        {
          text: "Redis",
          prefix: "redis/",
          icon: "redis",
          collapsible: true,
          children: [
            "01-datatypesandreferencescenarios",
            "02-basement1-datastructure",
            "02-basement2-pipelining",
            "02-basement3-whyisitsofast",
            "03-strategy1-persistence",
            "03-strategy2-expirationdeletion",
            "03-strategy3-memoryelimination",
            "04-ha1-masterslavereplication",
            "04-ha2-sentinelmechanism",
            "04-ha3-colony",
            "05-implementdistributedlocks",
            "05-transactionofredis",
            "06-tuning0-capacityassessmentmodel",
            "06-tuning2-commonlatencyissues",
            "06-tuning3-memoryfragmentationissue",
          ],
        },
        {
          text: "Elasticsearch",
          prefix: "elasticsearch/",
          icon: "elasticsearch",
          collapsible: true,
          children: [
            
          ],
        },
        {
          text: "MongoDB",
          prefix: "mongodb/",
          icon: "mongodb",
          collapsible: true,
          children: [

          ],
        },
      ],
      
    },
    {
      text: "常用框架",
      prefix: "framework/",
      icon: "component",
      collapsible: true,
      children: [
        {
          text: "Spring",
          icon: "bxl-spring-boot",
          prefix: "spring/",
          collapsible: true,
          children: [
            "ioc1-summary",
            "ioc2-initializationprocess",
            "ioc3-Instantiationofbeans",
            "ioc4-resolvecirculardependencies",
            "aop1-summary",
            "aop2-implementationofcrosssections",
            "aop3-proxy",
            "spring-transactions",
            "mvc1-summary",
            "mvc2-servlet",
            "mvc3-initializationprocessofdispatcherservlet",
            "mvc4-processofdispatcherservletprocessingrequests",
          ],
        },
        {
          text: "Mybatis",
          icon: "database",
          prefix: "mybatis/",
          collapsible: true,
          children: [
            "basement-jdbc",
            "initializationprocessofmybatis",
            "configurationparsingprocess",
            "sqlsessionexecutionprocess",
            "principleofdynamicSQL",
            "principleofpluginmechanism",
            "datasourcesandconnectionpools",
            "transactionmanagementmechanism",
            "cacheimplementationmechanism",
          ],
        },
        {
          text: "Spring Boot",
          icon: "bxl-spring-boot",
          prefix: "springboot/",
          collapsible: true,
          children: [
            "principleofautomaticassembly",
          ],
        },
        {
          text: "Netty",
          icon: "network",
          prefix: "netty/",
          collapsible: true,
          children: [
            
          ],
        },
        {
          text: "Web容器",
          icon: "et-performance",
          prefix: "web/",
          collapsible: true,
          children: [
            "tomcat1",
            "tomcat-logs",
          ],
        },
      ],
    },
    {
      text: "微服务",
      icon: "distributed-network",
      prefix: "microservices/",
      collapsible: true,
      children: [
        {
          text: "理论 & 算法",
          icon: "suanfaku",
          prefix: "protocol/",
          collapsible: true,
          children: [
            "cap-base",
            "gossip-detail",
            "paxos-detail",
            "zab-detail",
            "raft-detail",
            "consistencyhash",
            "loadbalance",
            "requestflowlimitingalgorithm",
            "idempotence",
          ],
        },
        {
          text: "API网关",
          icon: "gateway",
          prefix: "gateway/",
          collapsible: true,
          children: [
            
          ],
        },
        {
          text: "服务注册与发现",
          icon: "configuration",
          prefix: "service-registration-and-discovery/",
          collapsible: true,
          children: [
            "nacos-sourcecode",
            "nacos-interactionmodel",
            "zookeeper-colony",
            "zookeeper-distributedlocks",
            "zookeeper-sourcecode",
          ],
        },
        {
          text: "消息队列",
          icon: "MQ",
          prefix: "message-queue/",
          collapsible: true,
          children: [
            "rabbitMQ-basic",
            "rabbitMQ-advancedusage",
            "rocketMQ-basic",
            "rocketMQ-advancedusage",
            "rocketMQ-scenarios",
            "rocketMQ-sourcecode",
            "kafka-basic",
            "kafka-advancedusage",
          ],
        },
        {
          text: "RPC",
          icon: "network",
          prefix: "rpc/",
          collapsible: true,
          children: [
            "dubbo-advancedusage",
            "dubbo-spimechanism",
            "dubbo-serviceexposure-sourcecode",
            "dubbo-servicenvocation-sourcecode",
            "dubbo-newfeature",
          ],
        },
        {
          text: "监控中心",
          icon: "limit_rate",
          prefix: "monitoring-center/",
          collapsible: true,
          children: [
            "serviceprotection-summary",
            "sentinel-sourcecode",
          ],
        },
        {
          text: "分布式系统",
          icon: "distributed-network",
          prefix: "distributed-system/",
          collapsible: true,
          children: [
            "snowflake",
            "distributedlocks",
            "distributedtransactions",
          ],
        },
      ],
    },
    {
      text: "计算机基础",
      icon: "computer",
      prefix: "cs-basics/",
      collapsible: true,
      children: [
        {
          text: "数据结构",
          prefix: "data-structure/",
          icon: "people-network-full",
          collapsible: true,
          children: [
            "linear-data-structure",
            "tree",
            "heap",
            "graph",
            "bitmap",
            "bloomfilter",
          ],
        },
        {
          text: "算法",
          prefix: "algorithms/",
          icon: "suanfaku",
          collapsible: true,
          children: [
            "10-classical-sorting-algorithms",
            "lru-lfu",
            "backtracking",
            "greedy",
            "dynamicprogramming",
          ],
        },
        {
          text: "计算机网络",
          prefix: "network/",
          icon: "network",
          collapsible: true,
          children: [
            "01-fundamentalsofdatacommunicationandheaders",
            "02-tcp1-threehandshakesandfourwaves",
            "02-tcp2-flowcontrol",
            "03-http1-statuscodeandheader",
            "03-http2-https",
            "03-http3-http2and3",
            "tcpandhttp-keepalive",
          ],
        },
        {
          text: "操作系统",
          prefix: "operating-system/",
          icon: "caozuoxitong",
          collapsible: true,
          children: [
            "processthreadsanddeadlocks",
            "memorypagereplacementalgorithm",
            "cpucacheconsistency",
            "zerocopytechnology",
          ],
        },
        {
          text: "Linux常用命令",
          prefix: "linuxcommand/",
          icon: "linux",
          collapsible: true,
          children: [
            "filemanagement",
            "documentedit",
            "vi-vim-command",
            "systemmanagement",
          ],
        },
      ],
    },
    {
      text: "系统设计",
      icon: "design",
      prefix: "system-design/",
      collapsible: true,
      children: [
        {
          text: "设计模式",
          prefix: "design-pattern/",
          icon: "Tools",
          collapsible: true,
          children: [
            "overviewofdesignpatterns",
            "creationalpattern",
            "singletonpattern",
            "structuralpatterns",
            "behavioralpattern",
          ],
        },
        {
          text: "认证授权",
          prefix: "security/",
          icon: "security-fill",
          collapsible: true,
          children: [
             "basisofauthoritycertification",
             "jwt-detail",
             "sso-detail",
             "oauth2",
             "QRcodelogin",
             "designofpermissionsystem",
             "designofpermissionsystemzhuanzhuan",     
          ],
        },
        {
          text: "缓存专栏",
          prefix: "cache-column/",
          icon: "planet",
          collapsible: true,
          children: [
             "cache-basic",
             "cacheavalancheandbreakdownandpenetration",
             "cacheanddatabaseconsistencyissues",
             "localcache1",
             "localcache2",
             "javacacherule",
             "guavacache1",
             "guavacache2",
             "guavacache3",
             "caffeine1",
             "caffeine2",
             "caffeine3",
             "ehcache1",
             "ehcache2",
             "ehcache3",
             "redis",
             "usecache",
          ],
        },
        {
          text: "SpringBoot专栏",
          prefix: "springboot/",
          icon: "bxl-spring-boot",
          collapsible: true,
          children: [
            "springboot-annotation",
            "parameterverification",
            "elegantlyclosetheprogram",
            "kafka-springboot",
          ],
        },
        "paymenttechnology",
        "webrealtimemessagepush",
      ],
    },
    {
      text: "架构思想",
      icon: "et-performance",
      prefix: "architectural-ideas/",
      collapsible: true,
      children: [
          "DDD",
      ],
    },
    {
      text: "大数据框架",
      icon: "big-data",
      prefix: "bigdata/",
      collapsible: true,
      children: [
        {
          text: "理论 & 算法 & 数据结构",
          icon: "suanfaku",
          prefix: "protocol/",
          collapsible: true,
          children: [
            "datastructure-LSMtree",
          ],
        },
      ],
    },
    {
      text: "开发工具",
      icon: "tool",
      prefix: "tools/",
      collapsible: true,
      children: [
          "maven-detail",
          "git-detail",
        {
          text: "Gradle",
          icon: "gradle",
          prefix: "gradle/",
          collapsible: true,
          children: [
            "decompilation-jadx",
          ],
        },
        {
          text: "Docker",
          icon: "docker1",
          prefix: "docker/",
          collapsible: true,
          children: [
          ],
        },
        "redis-managertools",
        "onlinetools",
      ],
    },
  ],
});
