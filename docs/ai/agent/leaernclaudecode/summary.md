---
title: 引言
category: AI
tags:
  - agent
  - prompt
head:
  - - meta
    - name: description
      content: 全网最全的AI大模型知识点总结，让天下没有难学的八股文！
---




## 引言

最近刷到一个宝藏网站，叫 [**Learn Claude Code**](https://learn.shareai.run/zh/)。名字看着挺像学习使用 Claude Code 的指南，但实际并不是，而是教你从 0 到 1 构建 nano Claude Code-like agent，每次只加一个机制。

网站链接我也再贴一遍：https://learn.shareai.run/zh/

而且网站开头就直白表示：所有 AI 编程 Agent 共享同一个循环：调用模型、执行工具、回传结果。生产级系统会在其上叠加策略、权限和生命周期层。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202604022045651.png)


```python
while True:
    response = client.messages.create(messages=messages, tools=tools)
    if response.stop_reason != "tool_use":
        break
    for tool_call in response.content:
        result = execute_tool(tool_call.name, tool_call.input)
        messages.append(result)
```

这段代码人工翻译一下就是：
1. 调模型（给指令）
2. 执行工具（读写文件、跑命令）
3. 回传结果（告诉模型干了啥）
4. 继续迭代（直到任务干完）

就这？就这。剩下的全是围绕这个循环的各种优化和补丁。

用 Java 来写，核心循环大概是这样：

```java
while (true) {  
    MessageResponse response = client.messagesCreate(messages, tools);  
    if (!"tool_use".equals(response.getStopReason())) {  
        break;  
    }  
    for (ToolCall toolCall : response.getContent()) {  
        ToolResult result = executeTool(toolCall.getName(), toolCall.getInput());  
        messages.add(result);  
    }  
}
```

正是考虑到我们的读者多数是Java同学，因此我决定用Java来和大家一块学习下


## 渐进式学习路径

网站将这 12 个阶段（s01-s12）归纳为五个核心能力的进阶，看看一个成熟的 Agent 系统是如何一步步被做出来的：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202604022052924.png)
