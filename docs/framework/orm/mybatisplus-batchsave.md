---
title: MyBatis-Plus批量插入的实现原理
category: 常用框架
tags:
  - ORM框架
  - MyBatis
  - MyBatisPlus
head:
  - - meta
    - name: keywords
      content: MyBatis,MyBatisPlus,MyBatisPlus 批量插入,实现原理,源码阅读
  - - meta
    - name: description
      content: 全网最全的MyBatis知识点总结，让天下没有难学的八股文
---


## 前言：批量插入的性能挑战

### 场景描述

在实际开发中，如考试系统、订单处理、日志存储等场景，经常需要批量插入大量数据。例如，在一个在线考试系统中，创建一份试卷需要插入多张表的数据：

- 试卷表（exam）：存储试卷的基本信息。
- 题目表（question）：存储题目信息。
- 选项表（option）：存储题目下所有选项信息。

在保存试卷时，需要关联保存试卷、题目以及题目选项，此时对于保存的性能就有较高的要求了。

### 性能瓶颈

- **逐条插入效率低**：传统的逐条插入模式效率欠佳，每次插入数据时都要与数据库进行交互，从而产生较高的网络开销以及数据库解析成本。
- **外键关系处理复杂**：题目与选项之间存在外键关联，这就需要在插入数据后获取主键 ID，无疑增加了操作的复杂程度。
- **批量操作性能有限**：使用默认的 saveBatch 方法，其性能提升并不显著，难以满足高并发、大数据量的实际需求。



## 初探 MyBatis-Plus 的 saveBatch 方法

### saveBatch 方法简介

在 MyBatis-Plus 中，saveBatch 方法是用于**批量保存数据**的方法。它能够在单次操作中将多条数据同时插入数据库，从而提高插入效率，减少数据库连接次数，提升性能。

```java
boolean saveBatch(Collection<T> entityList);
boolean saveBatch(Collection<T> entityList, int batchSize);
```

- entityList：要插入的实体类集合。可以是任何实现了 Collection 接口的集合类型，如 List、Set 等。
- batchSize（可选）：指定每次批量插入的大小。默认情况下，MyBatis-Plus 会一次性插入所有数据。如果设置了 batchSize，则会按指定大小分批插入，避免一次性插入大量数据时出现性能问题或内存溢出。

### 常用场景

- **批量插入数据**：当需要插入大量数据时，使用 saveBatch 可以显著提高性能。
- **提高数据库写入效率**：减少数据库连接和插入的次数，有效提升性能。
- **处理大数据量时的内存优化**：通过分批插入，避免一次性插入大量数据导致内存溢出。



### 默认实现的局限性

- **不支持多条 SQL 合并**：在默认情况下，即便使用 saveBatch，也有可能是逐条发送 SQL 语句。这会导致生成的 SQL 更冗长、性能较低，尤其是在数据量较大时，执行效率会明显下降，无法充分利用数据库批量插入的特性。
- **性能提升有限**：默认实现并未针对批量插入进行特殊优化。例如，它可能无法充分利用 JDBC 的批量操作特性，导致性能不如手动实现的批量插入逻辑。对于大批量插入，性能可能不理想。
- **主键生成方式局限性**：如果实体类中主键是由数据库自动生成（如自增主键），默认实现会多次与数据库交互获取主键值。这会增加额外的数据库开销。尤其是当数据量较大时，主键生成的额外查询操作会显著降低性能。
- **外键关系处理复杂**：需要在插入数据后获取主键 ID，这导致无法在批量插入时建立关联关系，使得外键关系处理变得复杂。
- **缺乏灵活性**：默认实现只能进行简单的插入操作，不能处理条件性插入（如：插入前判断是否已存在相同记录）或插入冲突处理（如主键冲突时自动更新数据）。对需要动态逻辑的场景不适用。



## 深度解析 rewriteBatchedStatements=true 的作用

### JDBC 批处理机制

JDBC 批处理机制是一种优化数据库操作性能的技术，允许将多条 SQL 语句作为一个批次发送到数据库服务器执行，从而减少客户端与数据库之间的交互次数，显著提高性能。通常用于 批量插入、批量更新 和 批量删除 等场景。具体的流程如下：

```java
//创建 PreparedStatement 对象,用于定义批处理的 SQL 模板。
PreparedStatement pstmt = conn.prepareStatement(sql);
for (Data data : dataList) {
   // 多次调用 addBatch() 方法,每次调用都会将一条 SQL 加入批处理队列。
   pstmt.addBatch();
}
//执行批处理,调用 executeBatch() 方法，批量发送 SQL 并执行。
pstmt.executeBatch();
```



### MySQL JDBC 驱动的默认行为对批处理的影响

- **未开启重写**：在默认状态下，MySQL JDBC 驱动会逐一条目地发送批处理中的 SQL 语句，未开启重写功能。
- **性能瓶颈**：频繁的网络交互以及数据库解析操作，使得批量操作的性能提升效果有限，形成了性能瓶颈。

### rewriteBatchedStatements=true 的魔力

- **启用批处理重写**：启用批处理重写功能后，驱动能够将多条同类型的 SQL 语句进行合并，进而发送给数据库执行。
- **减少网络交互**：一次发送多条 SQL，可有效降低网络延迟，减少网络交互次数。
- **提高执行效率**：当所有数据都通过一条 SQL 插入时，MySQL 只需要解析一次 SQL，降低了解析和执行的开销。
- **减少内存消耗**：虽然批量操作时将数据合并到一条 SQL 中，理论上会增加内存使用（因为需要构建更大的 SQL 字符串），但相比多次单条插入的网络延迟和处理开销，整体的资源消耗和执行效率是更优的。

未开启参数时的批处理 SQL：

```sql
INSERT INTO question (exam_id, content) VALUES (?, ?);
INSERT INTO question (exam_id, content) VALUES (?, ?);
INSERT INTO question (exam_id, content) VALUES (?, ?);
```

开启参数后的批处理 SQL：

```sql
INSERT INTO question (exam_id, content) VALUES (?, ?), (?, ?), (?, ?);
```



## 批量插入优化

### 预先生成 ID：解决外键关系的关键

#### 问题分析

在插入题目和选项时，选项需要引用对应题目的主键 ID。如果等待题目插入后再获取 ID，会导致无法进行批量操作，影响性能。所以，预先生成ID就成了我们解决问题的关键。

使用 雪花算法（分布式 ID 生成器）：

- **全局唯一**：生成的 ID 在全局范围内唯一，避免了主键冲突。
- **本地生成**：无需依赖数据库生成，减少了数据库交互。
- **支持批量生成**：提升获取分布式唯一ID的效率

#### 具体的代码业务执行逻辑

在构建题目和选项数据时，预先生成 ID，并在选项中引用对应的题目 ID：

```java
public Boolean createExamPaper(HeroExamRequest<ExamPaperRequest> request) throws BusinessException{
    // 构建题目数据
    Question question = new Question();
    question.setId(questionId);
    question.setExamId(examId);
    // ...

    // 构建选项数据
    Option option = new Option();
    option.setQuestionId(questionId);
    // ...
}
```



### 多线程并发插入的实现

#### 问题分析

直接在多线程中调用 saveBatch 方法，可能导致以下问题：

- **程安全性**：在 MyBatis 中，SqlSession 在默认情况下并非线程安全的。若在多线程环境下共享同一个 SqlSession，极有可能导致数据错误或引发异常。
- **事务管理**：对于多线程操作而言，需要独立的事务管理机制，以此来确保数据的一致性。
- **资源竞争**：过多的并发线程有可能致使数据库连接池被耗尽，进而降低性能。

#### 正确的多线程实现方式

##### 使用 @Async 异步方法

利用 Spring 的 @Async 注解，实现异步方法调用，每个异步方法都有自己的事务和 SqlSession。

配置异步支持：

```java
@Configuration
@EnableAsync
public class AsyncConfig {
@Bean(name = "taskExecutor")
public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4); // 核心线程数
    executor.setMaxPoolSize(8); // 最大线程数
    executor.setQueueCapacity(100); // 队列容量
    executor.setThreadNamePrefix("AsyncExecutor-");
    executor.initialize();
    return executor;
  }
}
```

修改批量插入方法：

```java
@Service
public class QuestionServiceImpl implements QuestionService {

  @Autowired
  private QuestionMapper questionMapper;

  @Override
  @Async("taskExecutor")
  @Transactional(rollbackFor = Exception.class)
  public CompletableFuture<Void> saveBatchAsync(List<Question> questionList) {
    saveBatch(questionList, BATCH_SIZE);
    return CompletableFuture.completedFuture(null);
  }
}
```

##### 调用异步方法

```java
public void createExam(Exam exam, int questionCount, int optionCountPerQuestion) {
  // ... 数据准备部分略 ...

  // 将题目列表拆分成多个批次
  List<List<Question>> questionBatches = Lists.partition(questionList, BATCH_SIZE);
  List<List<Option>> optionBatches = Lists.partition(allOptionList, BATCH_SIZE);

  List<CompletableFuture<Void>> futures = new ArrayList<>();

  // 异步批量插入题目
  for (List<Question> batch : questionBatches) {
    CompletableFuture<Void> future = questionService.saveBatchAsync(batch);
    futures.add(future);
  }

  // 异步批量插入选项
  for (List<Option> batch : optionBatches) {
    CompletableFuture<Void> future = optionService.saveBatchAsync(batch);
    futures.add(future);
  }

  // 等待所有异步任务完成
  CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
}
```

##### 注意事项

- **线程安全**：每个异步方法均拥有自身独立的 SqlSession 和事务，从而有效地避免了线程安全方面的问题。
- **事务管理**：在异步方法上添加 @Transactional 注解，能够确保事务的独立性。
- **异步结果处理**：通过使用 CompletableFuture 来等待异步任务的完成，以此确保所有数据均已成功插入。

### 数据库层面的优化

#### 调整数据库连接池

- **增加连接池大小**：在多线程并发的情形下，务必确保数据库连接池具备足够数量的连接可供使用。
- **合理配置**：应根据实际情况对连接池的最小连接数和最大连接数进行适当调整，以避免出现连接不足或者资源浪费的情况。

#### 配置 MyBatis 的执行器类型

修改执行器类型为 BATCH：在 MyBatis 配置中，设置执行器类型，可以提高批量操作的性能。

```xml
<configuration>
  <settings>
    <setting name="defaultExecutorType" value="BATCH"/>
  </settings>
</configuration>
```

注意：使用 BATCH 执行器时，需要手动调用 sqlSession.flushStatements()，并处理返回的 BatchResult，复杂度较高，建议谨慎使用。

### 监控与调优

####  监控异步任务的执行情况

- **使用 CompletableFuture**：在调用异步方法时，返回 CompletableFuture，可以方便地等待所有任务完成。
- **日志记录**：在异步方法中添加日志，记录开始和结束时间，监控执行情况。

```java
@Async("taskExecutor")
@Transactional(rollbackFor = Exception.class)
public CompletableFuture<Void> saveBatchAsync(List<Question> questionList) {
  long startTime = System.currentTimeMillis();
  saveBatch(questionList, BATCH_SIZE);
  long endTime = System.currentTimeMillis();
  logger.info("Inserted batch of {} questions in {} ms", questionList.size(), (endTime - startTime));
  return CompletableFuture.completedFuture(null);
}
```

#### 调整线程池参数

- **线程池大小**：依据服务器的 CPU 核心数以及数据库的承载能力，对线程池的 corePoolSize 和 maxPoolSize 进行合理设置。
- **队列容量**：设置线程池的 queueCapacity，以防止因任务过多而导致内存溢出的情况发生。

### 最佳实践总结

#### 综合优化策略

- **将 rewriteBatchedStatements 配置为 true**：以此启用 JDBC 驱动的批处理重写功能，可显著提高批量插入的性能表现。
- **预先生成 ID**：预先生成主键 ID，有效解决外键关系问题，进而支持批量插入操作。
- **使用异步方法进行多线程批量插入**：运用异步方法来进行多线程批量插入，确保线程安全与事务独立，避免出现资源竞争的情况。
- **调整数据库连接池和线程池参数**：对数据库连接池和线程池的参数进行调整，以满足多线程并发操作的实际需求。
- **监控异步任务和数据库性能**：对异步任务和数据库性能进行实时监控，以便能够及时发现并解决性能瓶颈问题。

#### 注意事项

- **线程安全性**：在多线程的环境之中，务必确保所有资源要么是线程安全的，要么是线程独立的。
- **事务一致性**：每个异步任务均拥有自身的事务，以此确保数据的一致性。
- **资源合理利用**：避免因过多的并发线程而致使系统资源被耗尽，进而影响整体性能表现。


<!-- @include: @article-footer.snippet.md -->     