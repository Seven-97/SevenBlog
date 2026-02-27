---
title: AI大模型入门知识
category: AI
tags:
  - Prompt
  - MCP
  - Agent
head:
  - - meta
    - name: description
      content: 全网最全的AI大模型知识点总结，让天下没有难学的八股文！
---





这段时间各种AI名词一波接一波的冲击着我的屏幕，Agent，MCP，FunctionCalling，RAG，它们都是什么东西

有人说Agent是智能体，那智能体又是什么呢？

有人说MCP是AI时代的USB协议，那么它可以接U盘吗？

它们到底都是什么意思？



## Prompt

2023年，OpenAI则刚发布GPT的时候，Al看起来只是一个聊天框。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202505272322604.png)

我们通过聊天框发送一条消息给AI模型，然后AI模型生成一个回复，我们发的消息就叫 **User Prompt，也就是用户提示词**，一般就是我们提出的问题或者想说的话。

但是现实生活中，当我们和不同人聊天时，即便是完全相同的话，对方也会根据自己的经验给出不同的答案。

比如我说我肚子疼，我妈可能会问我要不要去医院，我爸可能会让我去厕所，我女朋友可能直接就来一句： 滚一边去，老娘也疼。

但是AI并没有这样的人设，所以他就只能给出一个通用的四平八稳的回答，显得非常无趣。于是我们就希望给AI也加上人设。

最直接的方法就是把人设信息和用户要说的话，打包成一条User Prompt发过去。比如你演我的女朋友，我说我肚子疼，然后AI就可能回复：滚一边去，老娘也疼。这样就对味了。

但问题是，你扮演我温柔的女朋友，这句话并不是我们真正想说的内容，显得有一点出戏。于是人们干脆把人设信息单独的拎了出来，放到另外一个Prompt里面，这就是**System Prompt，系统提示词**。

**System Prompt主要用来描选AI的角色、性格、背景信息、语气等等**等等。总之只要不是用户直接说出来的内容，都可以放进System Prompt里面，每次用户发送User Prompt的时候，系统会自动把System Prompt也一起发给AI模型，这样整个对话就显得更加自然了。

不过即使人设设定的再完美，说到底AI还是个聊天机器人，你问一个问题，他最多给你答案，或者告诉你怎么做，但实际动手的还是你自己。那么能不能让AI自己去完成任务呢？

## AI Agent

那么能不能让AI自己去完成任务呢，第一个做出尝试的是一个开源项目，叫做AutoGPT，它是本地运行的一个小程序。

如果你想让AutoGPT帮你管理电脑里的文件，那你得先写好一些文件的管理函数，比如说 list_files 用来列目录， read_files 用来读文件等等等等。然后你把这些函数以及它们的功能描述、使用方法，注册到AutoGPT中。

AutoGPT会根据这些信息生成一个System Prompt，告诉AI模型用户给了你哪些工具，他们都是干什么的，以及AI如果想要使用它们，应该返回什么样的格式，最后把这个System Prompt连同用户的请求，比如说帮我找一找原神的安装目录，一起发给AI模型。

如果AI模型足够的聪明，就会按照要求的格式，返回一个调用某个函数的消息。AutoGPT进行解析之后，就可以调用对应的函数了，然后再把结果丟回给AI，AI再根据函数调用的结果，决定下一步应该做什么操作，这个过程就这样反复，直到任务完成为止。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202505280936579.png)

人们把AutoGPT这种负责在模型、工具和最终用户之间传话的程序，就叫做**AI Agent**。

而这些提供给AI 调用的函效或者服务，就叫做Agent Tool。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202505280936357.png)

不过这个架构有一个小问题，虽然我们在System Prompt 里面写清楚了Al应该用什么格式返回。但AI模型嘛，说到底它是一个概率模型，还是有可能返回格式不对的内容。

为了处理这些不听话的情况，很多AIAgent会在发现AI回的格式不对时,自动进行重试。一次不行我们就来第二次。现在市面上很多知名的Agent，比如CIine乃然采用的是这种方式。

说白了就是，LLM模型是脑子，Agent是手，模型提供的是思路，具体做事的是Agent

## Function Calling

但这种反复的重试总归让人觉得不太靠谱，于是大模型厂商开始出手了，ChatGPT, Claude, Gemini等等，纷纷推出了一个叫做 **Function Calling** 的新功能。这个功能的**核心思想就是统一格式，规范描述**。

回到之前原神的例子，我们通过System Prompt，告诉AI有哪些工具以及返口的格式。但是这些描述是用自然语言随意写的，只要AI看得懂就行，Function Calling则对这些描述进行了标准化。比如每个Tool都用一个JSON对象来定义工具名。工具名写在name字段、功能说明写在desc 字段，所需要的参数写在params里面等等等等。然后这些JSON对象也从System Prompt中被剥离了出来，单独放到了一个字段里面。最后Function Calling 也规定了AI使用工具时应该返回的格式。所以System Prompt中的格式定义也可以删掉了，这样一来，所有的工具描述都放在相同的地方，所有工具描述也都依照相同的格式，Al使用工具时的回复也都依照相同的格式。

于是人们就能更加有针对性的训练AI模型，让他理解这种调用的场景。甚至在这种情况下，如果AI依然生成了错误的回复，因为回复的格式是固定的，AI服务器端自己就可以检测到。井且进行重试，用户根本感觉不到。这样一来，不仅降低了用户端的开发难度，也节省了用户端重试带来的Token开销。

正是由于这些好处，现在越来越多的AIAgent 开始从System Prompt特向Function Calling

相关原理如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202505311258844.png)



但Function Calling也有自己的问题，就是没有统一的标准，每家大厂的API定义都不一样，而且很多开源模型还不支持Function Calling。所以真的要写一个跨模型通用的Al Agent其实还挺麻烦的。

因此System Prompt和 Function Calling这两种方式，现在在市面上是并存的。

## Skills

Skills，翻译过来就是技能，字面意思上非常简单，给Agent用的技能。

Agent+skills，在很多时候，就是workflow的一种呈现，Skill本质是规范化的Prompt，甚至宝玉老师在一篇文章中的原话更为激进：“几乎所有能用 workflow 完成的AI任务，都可以用Agent + Skills实现。”

Prompt是啥呢，Prompt就像你站在他旁边，当场口头交代任务。而Skills，就像你给他一本公司内部的那种SOP手册，你们肯定见过无数了。而且这手册不是那种一张长到让人窒息的Word，它更像一个知识库般的文件夹，里面可以放规范、脚本、模板、参考资料等等，Agent呢，会在需要时自己去翻。

skill在md文件中有 名称 和 描述 两个元数据，其它都不是必须的。而元数据会合并到 tools 的工具描述中，Agent每次请求都会把skills带上，大模型会根据用户的问题去找对应的工具，因此也就能找到对应的skill，并要求Agent去读取那个 skill的md文件，大模型再给出解决方案。

再往详细了说，Skill其实是对Function Call做了一层业务化的封装和组织。因为在实际项目中，单个函数的能力太零散了，比如我们要做一个"智能邮件助手"，可能需要"发送邮件"、"查询邮件"、"标记已读"、"删除邮件"等好几个函数配合使用，这时候我们就会**把这些相关的Function打包成一个"邮件处理Skill"**。

这样做的好处是代码组织更清晰，复用性也更强，团队协作的时候也能按Skill来分工。我之前在项目里就是这么干的，会专门建一个skills目录，每个Skill对应一个独立的模块，里面包含这个领域的所有函数定义和实现逻辑。从架构角度看，Skill是Function Call在工程实践中自然演化出来的一种组织形式。

## MCP

以上我们讲的都是Al Agent和AI模型之间的通方式，按下来我们再看另一边，Al Agent是怎么跟Agent Tookls来进行通信的。最简单的做法是把AIAgent和Agent Tools写在同一个程序里面，直接函数调用搞定，这也是现在大多数Agent的做法。

但是后来人们逐渐发现，有些Tool的功能其实挺通用的，比如说一个浏览网页的工具，可能多个Agent都需要，那我总不能在每个Agent面都拷贝一份相同的代码吧，太麻烦了，也不优雅，于是大家想到了一个办法。把Tool变成服务统一的托管，让所有的Agent都采调用，**这就是MCP**

**MCP是一个通信协议，专门用来规范Agent和Tool服务之间是怎么交互的，运行Tool的服务叫做MCP Server，週用它的Agent叫做MCP Client**。它想解决的核心问题是，现在市面上各家大模型、各种工具、各类数据源之间的对接都是"各自为政"的，开发者要对接不同的系统就得写不同的适配代码，特别麻烦。

MCP规定了MCP Server如何和MCP Client通信，以及MCP Server要提供哪些接口，比如说用采查询MCP Server中有哪些Tool，Tool的功能、描述需要的参数、格式等等的接口。

**也就是说，所谓的MCP，就是Agent工具列表的一个扩展和延伸，让Agent可调用的工具变得更多**。具体来说，MCP定义了服务端和客户端之间如何传递上下文、如何声明可用工具、如何返回执行结果等一整套规范。

除了普通的Tool这这种函数调用的形式，MCP Server也可以直接提供数据，提供类似文件读写的服务叫做Resources，或者为Agent提供提示词的模板叫做Prompt

MCP Server既可以和Agent跑在同一台机器上，通过标准输入输出进行通信，也可以被部署在网络上，通过HTTP进行通信。

这里需要注意的是，虽然然MCP是为了AI而定制出来的标准，但买际上MCP本身却和AI模型没有关系

他并不关心Agent用的是哪个模型，MCP只负责帮Agent管理工具、资源和提示词

从这个角度看，MCP是站在更高的生态层面去思考问题，它不是要替代Function Call或者Skill，而是要给它们提供一套统一的"交流语言"。



## 小结

最后我们流理一下整个流程：

1. 我听说女朋友肚子疼，于是问 AIAgent 或者说 MCP Clent，我女朋友肚子疼应该怎么办。
2. AI Agent会把问题包裝在User Prompt中，然后Agent通过MCP协议从MCP Server里面获取所有Tool的信息。
3. Al Agen会把这些Tool的信息或者转化成System Prompt  或者转化成Function Calling的格式，然后和用户请求User Prompt一起打包发送给Al模型。
4. AI模型发现有一个叫做web browse的网页执览工具，于是通过普通回复或者Function Calling格式，产生一个调用这个Tool的请求，希望去网上搜索答案。
5. Agent收到了这个请求之后，通过MCP协议去调用MCP Server里的web_browse 工具
6. web_browse访问指定的网站之后,将内容返还给Agent,Agent再转发给AI模型
7. AI模型再根据网页内容和自己的头脑风暴，生成最终的答案：多喝热水
8. 最后由Agent把结果展示给用户

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202505280944526.png)

总之这就是System Prompt、User prompt、 Al Agent、Agent Tool、Function Calling、MCP和AI模型之间的联系与区别了，他们不是彼此取代的关系，而是像齿轮一样，一起构成了AI自动化协作的完整体系。


## AI相关链接汇总

- [提示词工程指南](https://www.promptingguide.ai/zh)
- [国内AI排名](https://www.superclueai.com/)
- [LMArena](https://lmarena.ai/?leaderboard)

