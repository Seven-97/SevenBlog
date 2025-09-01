---
title: Agent设计模式介绍
category: AI
tags:
  - agent
  - prompt
head:
  - - meta
    - name: description
      content: 全网最全的AI大模型知识点总结，让天下没有难学的八股文！
---



AI Agent有八种设计模式。对于这八种设计模式，整理了一张图，来阐明它们之间的关系。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506021244241.jpg)

ReAct模式最早出现的Agent设计模式，目前也是应用最广泛的。从ReAct出发，有两条发展路线：

一条更偏重Agent的规划能力，包括**REWOO、Plan & Execute、LLM Compiler**。

另一条更偏重反思能力，包括**Basic Reflection、Reflexion、Self Discover、LATS**。

## ReACT
### ReAct的概念

ReAct的概念来自论文《ReAct: Synergizing Reasoning and Acting in Language Models》，这篇论文提出了一种新的方法，通过结合语言模型中的推理（reasoning）和行动（acting）来解决多样化的语言推理和决策任务。ReAct 提供了一种更易于人类理解、诊断和控制的决策和推理过程。

它的典型流程如下图所示，可以用一个有趣的循环来描述：思考（Thought）→ 行动（Action）→ 观察（Observation），简称TAO循环。

- 思考（Thought）：面对一个问题，我们需要进行深入的思考。这个思考过程是关于如何定义问题、确定解决问题所需的关键信息和推理步骤。
- 行动（Action）：确定了思考的方向后，接下来就是行动的时刻。根据我们的思考，采取相应的措施或执行特定的任务，以期望推动问题向解决的方向发展。
- 观察（Observation）：行动之后，我们必须仔细观察结果。这一步是检验我们的行动是否有效，是否接近了问题的答案。
- 循环迭代

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506021246855.jpg)


如果观察到的结果并不匹配我们预期的答案，那么就需要回到思考阶段，重新审视问题和行动计划。这样，我们就开始了新一轮的TAO循环，直到找到问题的解决方案。

和ReAct相对应的是Reasoning-Only和Action-Only。在Reasoning-Only的模式下，大模型会基于任务进行逐步思考，并且不管有没有获得结果，都会把思考的每一步都执行一遍。在Action-Only的模式下，大模型就会处于完全没有规划的状态下，先进行行动再进行观察，基于观察再调整行动，导致最终结果不可控。

假设我们正在构建一个智能助手，用于管理我们的日程安排。

在reason-only模式中，智能助手专注于分析和推理，但不直接采取行动。

- 你告诉智能助手：“我明天有个会议。”
- 智能助手分析这句话，确定明天的会议时间、地点等细节。
- 它可能会提醒你：“明天下午3点有个会议，在公司会议室。”

在action-only模式中，智能助手专注于执行任务，但不做深入的推理或分析。

- 你告诉智能助手：“把我明天的会议改到上午10点。”
- 智能助手立即执行这个任务，将会议时间修改为上午10点。
- 它可能会简单确认：“您的会议已改到明天上午10点。”

在ReAct模式中，智能助手结合推理和行动，形成一个循环的感知-动作循环。不仅分析了你的需求（推理），还实际修改了日程安排（行动）。

- 你告诉智能助手：“我明天有个会议，但我想提前到上午10点。”
- 智能助手首先分析这句话，确定会议的原始时间和地点（感知阶段）。
- 然后，它更新你的日程安排，将会议时间改为上午10点（决策和动作执行阶段）。
- 最后，智能助手确认修改，并提供额外的信息：“您的会议已成功改到明天上午10点。提醒您，会议地点不变。

### ReAct的实现过程

下面，通过实际的源码，详细介绍ReAct模式的实现方法。

#### 第一步 准备Prompt模板

在实现ReAct模式的时候，首先需要设计一个清晰的Prompt模板，主要包含以下几个元素：

- 思考（Thought）：这是推理过程的文字展示，阐明我们想要LLM帮我们做什么，为了达成目标的前置条件是什么
- 行动（Action）：根据思考的结果，生成与外部交互的指令文字，比如需要LLM进行外部搜索
- 行动参数（Action Input）：用于展示LLM进行下一步行动的参数，比如LLM要进行外部搜索的话，行动参数就是搜索的关键词。主要是为了验证LLM是否能提取准确的行动参数
- 观察（Observation）：和外部行动交互之后得到的结果，比如LLM进行外部搜索的话，那么观察就是搜索的结果。

Prompt模板示例：
```c++
TOOL_DESC = """{name_for_model}: Call this tool to interact with the {name_for_human} API. What is the {name_for_human} API useful for? {description_for_model} Parameters: {parameters} Format the arguments as a JSON object."""
REACT_PROMPT = """Answer the following questions as best you can. You have access to the following tools:
{tool_descs}
Use the following format:

Question: the input question you must answer
Thought: you should always think about what to do
Action: the action to take, should be one of [{tool_names}]
Action Input: the input to the action
Observation: the result of the action
... (this Thought/Action/Action Input/Observation can be repeated zero or more times)
Thought: I now know the final answer
Final Answer: the final answer to the original input question
Begin!
Question: {query}"""
```


#### 第二步 构建Agent

一个ReAct Agent需要定义好以下元素

- llm：背后使用的LLM大模型
- tools：后续会用到的Tools集合
- stop：什么情况下ReAct Agent停止循环

```python
class LLMSingleActionAgent {
  llm: AzureLLM
  tools: StructuredTool[]
  stop: string[]
  private _prompt: string = '{input}'
  constructor({ llm, tools = [], stop = [] }: LLMSingleActionAgentParams) {
    this.llm = llm
    this.tools = tools
    if (stop.length > 4)
      throw new Error('up to 4 stop sequences')
    this.stop = stop
  }
 }
```

#### 第三步 定义Tools

Tools有两个最重要的参数，name和description。

Name就是函数名，description是工具的自然语言描述，LLM 根据description来决定是否需要使用该工具。工具的描述应该非常明确，说明工具的功能、使用的时机以及不适用的情况。

```python
export abstract class StructuredTool {
  name: string
  description: string
  constructor(name: string, description: string) {
    this.name = name
    this.description = description
  }

  abstract call(arg: string, config?: Record<string, any>): Promise<string>

  getSchema(): string {
    return `${this.declaration} | ${this.name} | ${this.description}`
  }

  abstract get declaration(): string
}
```

我们先简单地将两个描述信息拼接一下，为Agent提供4个算数工具：

```python
1. Addition Tool: A tool for adding two numbers
2. Subtraction Tool: A tool for subtracting two numbers
3. Division Tool: A tool for dividing two numbers
4. MultiplicationTool: Atoolformultiplyingtwonumbers
```

一个很有意思的事情是，这几个算数工具函数并不需要实际的代码，大模型可以仅靠自身的推理能力就完成实际的算数运算。当然，对于更复杂的工具函数，还是需要进行详细的代码构建。

#### 第四步 循环执行

执行器executor是在Agent的运行时，协调各个组件并指导操作。还记得ReAct模式的流程吗？Thought、Action、Observation、循环，Executor的作用就是执行这个循环。

```python
class AgentExecutor {
  agent: LLMSingleActionAgent
  tools: StructuredTool[] = []
  maxIterations: number = 15

  constructor(agent: LLMSingleActionAgent) {
    this.agent = agent
  }
  
  addTool(tools: StructuredTool | StructuredTool[]) {
    const _tools = Array.isArray(tools) ? tools : [tools]
    this.tools.push(..._tools)
  }
}
```

executor会始终进行如下事件循环直到目标被解决了或者思考迭代次数超过了最大次数：

- 根据之前已经完成的所有步骤（Thought、Action、Observation）和 目标（用户的问题），规划出接下来的Action（使用什么工具以及工具的输入）
- 检测是否已经达成目标，即Action是不是ActionFinish。是的话就返回结果，不是的话说明还有行动要完成
- 根据Action，执行具体的工具，等待工具返回结果。工具返回的结果就是这一轮步骤的Observation
- 保存当前步骤到记忆上下文，如此反复

```python
async call(input: promptInputs): Promise<AgentFinish> {
    const toolsByName = Object.fromEntries(
      this.tools.map(t => [t.name, t]),
    )

    const steps: AgentStep[] = []
    let iterations = 0

    while (this.shouldContinue(iterations)) {
      const output = await this.agent.plan(steps, input)
      console.log(iterations, output)

      // Check if the agent has finished
      if ('returnValues' in output)
        return output

      const actions = Array.isArray(output)
        ? output as AgentAction[]
        : [output as AgentAction]

      const newSteps = await Promise.all(
        actions.map(async (action) => {
          const tool = toolsByName[action.tool]

          if (!tool)
            throw new Error(`${action.tool} is not a valid tool, try another one.`)

          const observation = await tool.call(action.toolInput)

          return { action, observation: observation ?? '' }
        }),
      )

      steps.push(...newSteps)

      iterations++
    }

    return {
      returnValues: { output: 'Agent stopped due to max iterations.' },
      log: '',
    }
  }
```

#### 第五步 实际运行

我们提出一个问题，看看Agent怎么通过ReAct方式进行解决。 “一种减速机的价格是750元，一家企业需要购买12台。每台减速机运行一小时的电费是0.5元，企业每天运行这些减速机8小时。请计算企业购买及一周运行这些减速机的总花费。”

```python
describe('agent', () => {
  const llm = new AzureLLM({
    apiKey: Config.apiKey,
    model: Config.model,
  })
  const agent = new LLMSingleActionAgent({ llm })
  agent.setPrompt(REACT_PROMPT)
  agent.addStop(agent.observationPrefix)
  agent.addTool([new AdditionTool(), new SubtractionTool(), new DivisionTool(), new MultiplicationTool()])

  const executor = new AgentExecutor(agent)
  executor.addTool([new AdditionTool(), new SubtractionTool(), new DivisionTool(), new MultiplicationTool()])
  it('test', async () => {
    const res = await executor.call({ input: '一种减速机的价格是750元，一家企业需要购买12台。每台减速机运行一小时的电费是0.5元，企业每天运行这些减速机8小时。请计算企业购买及一周运行这些减速机的总花费。' })
    expect(res).toMatchInlineSnapshot(`
      {
        "log": "Final Answer: The total cost of purchasing and operating the gearboxes for a week is 9336 yuan.",
        "returnValues": {
          "output": "The total cost of purchasing and operating the gearboxes for a week is 9336 yuan.",
        },
      }
    `)
  }, { timeout: 50000 })
})
```

我们来看一下Agent的输出，以及Agent在这个过程，是如何思考和行动的。可以看到，通过Thought、Action、Observation的循环，AI Agent很好地一步步完成最终答案的输出。

```python
Question:一种减速机的价格是750元，一家企业需要购买12台。每台减速机运行一小时的电费是0.5元，企业每天运行这些减速机8小时。请计算企业购买及一周运行这些减速机的总花费
Thought:I need to calculate the total cost of purchasing and operating the gearboxes for a week.
Action: Multiplication Tool
Action Input: [750, 12]
Observation: 9000
Thought: Now I need to calculate the cost of operating the gearboxes for a day.
Action: Multiplication Tool
ActionInput:[0.5,8,12]
Observation: 48
Thought: Now I need to calculate the cost of operating the gearboxes for a week.
Action: Multiplication Tool
Action Input: [48, 7]
Observation: 336
Thought: Now I need to calculate the total cost of purchasing and operating the gearboxes for a week.
Action: Addition Tool
Action Input: [9000, 336]
Observation: 9336
```

### 小结

在AI Agent的多种实现模式中，ReAct模式是最早出现、也是目前使用最广泛的模式。ReAct的核心思想就是模拟人思考和行动的过程，通过Thought、Action、Observation的循环，一步步解决目标问题。

ReAct模式也存在很多的不足：

- 首先是LLM大模型的通病，即产出内容不稳定，不仅仅是输出内容存在波动，也体现在对复杂问题的分析，解决上存在一定的波动
- 然后是成本，采用ReAct方式，我们是无法控制输入内容的。因为在任务提交给LLM后，LLM对任务的拆解、循环次数是不可控的。因此存在一种可能性，过于复杂的任务导致Token过量消耗。
- 最后是响应时间，比起大部分API接口毫秒级的响应，LLM响应时间是秒级以上。在ReAct模式下，这个时间变得更加不可控。因为无法确定需要拆分多少步骤，需要访问多少次LLM模型。因此在在秒级接口响应的背景下，做成同步接口显然是不合适的，需要采用异步的方式。而异步方式，又会影响用户体验，对应用场景的选择又造成了限制。

但是无论如何，ReAct框架提出了一种非常好的思路，让现有的应用得到一次智能化的进化机会。现在很多场景已经有了非常成熟的ReAct Agent应用，比如智能客服、知识助手、个性化营销、智能销售助理等等。



## REWOO

### REWOO的概念

REWOO的全称是Reason without Observation，是相对ReAct中的Observation 来说的。它旨在通过以下方式改进 ReACT 风格的Agent架构：

第一，通过生成一次性使用的完整工具链来减少token消耗和执行时间，因为ReACT模式的Agent架构需要多次带有冗余前缀的 LLM 调用；

第二，简化微调过程。由于规划数据不依赖于工具的输出，因此可以在不实际调用工具的情况下对模型进行微调。

ReWOO 架构主要包括三个部分：

- Planner：规划器，负责将任务分解并制定包含多个相互关联计划的蓝图，每个计划都分配给Worker执行。
- Worker：执行器，根据规划器提供的蓝图，使用外部工具获取更多证据或者其他具体动作。
- Solver：合并器，将所有计划和证据结合起来，形成对原始目标任务的最终解决方案。

下图是REWOO的原理：

- Planner接收来自用户的输入，输出详细的计划Task List，Task List由多个Plan（Reason）和 Execution（Tool[arguments for tool]）组成；
- Worker接收Task List，循环执行完成task；
- Woker将所有任务的执行结果同步给Solver，Solver将所有的计划和执行结果整合起来，形成最终的答案输出给用户。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506071607669.jpg)

详细对比一下ReAct和REWOO，如下图所示。

左侧是ReAct方法，当User输入Task后，把上下文Context和可能的样本Example输入到LLM中，LLM会调用一个目标工具Tool，从而产生想法Thought，行动Action，观察Observation。由于拆解后的下一次循环也需要调用LLM，又会调用新的工具Tool，产生新的Thought，Action，Observation。如果这个步骤变得很长，就会导致巨大的重复计算和开销。

右侧ReWOO的方法，计划器Planner把任务进行分解，分解的依据是它们内部哪些用同类Tool，就把它分成同一类。在最开始，依旧是User输入Task，模型把上下文Context和Examplar进行输入。这里与先前有所不同的是，输入到Planner中，进行分解，然后调用各自的工具Tool。在得到了所有的Tool的输出后，生成计划结果Plan和线索，放到Solver进行总结，然后生成回答。这个过程只调用了两次LLM。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506071609740.jpg)


### REWOO的实现过程

下面，通过实际的源码，详细介绍REWOO模式的实现方法。

#### 第一步 构建Planner

Planner的作用是根据用户输入，输出详细的Task List。

首先需要给Planner规定Prompt模板，包括可以使用的Tools，以及Task List的规范。在下面的例子中，我们告诉Planner，“对于目标任务，要制定可以逐步解决问题的计划。对于每个计划，要指出哪个外部工具以及工具输入来检索证据，并将证据存储到变量 #E 中，以供后续工具调用”。在工具层面，我们定义了两个工具，google search和LLM。

```python
from langchain_openai import ChatOpenAI

model=ChatOpenAI(temperature=0)

prompt = “””For the following task, make plans that can solve the problem step by step. For each plan, indicate which external tool together with tool input to retrieve evidence. You can store the evidence into a variable #E that can be called by later tools. (Plan, #E1, Plan, #E2, Plan, …)
```

```python
Tools can be one of the following:

(1) Google[input]: Worker that searches results from Google. Useful when you need to find shortand succinct answers about a specific topic. The input should be a search query.

(2) LLM[input]: A pretrained LLM like yourself. Useful when you need to act with generalworld knowledge and common sense. Prioritize it when you are confident in solving the problemyourself. Input can be any instruction.
```

```python
For example,Task: Thomas, Toby, and Rebecca worked a total of 157 hours in one week. Thomas worked xhours. Toby worked 10 hours less than twice what Thomas worked, and Rebecca worked 8 hoursless than Toby. How many hours did Rebecca work?

Plan: Given Thomas worked x hours, translate the problem into algebraic expressions and solvewith Wolfram Alpha.

#E1 = WolframAlpha[Solve x + (2x − 10) + ((2x − 10) − 8) = 157]

Plan: Find out the number of hours Thomas worked.

#E2 = LLM[What is x, given #E1]

Plan: Calculate the number of hours Rebecca worked.

#E3 = Calculator[(2 ∗ #E2 − 10) − 8]
```

```python
Begin!

Describe your plans with rich details. Each Plan should be followed by only one #E.Task: {task}”””

task = “what is the hometown of the 2024 australian open winner”

result = model.invoke(prompt.format(task=task))

print(result.content)
```

下面是根据给定的问题，Planner输出的Task List。可以看到，Planner按照Prompt的要求给出了计划的每个步骤，以及需要调用的工具。

```python
Plan: Use Google to search for the 2024 Australian Open winner.

#E1 = Google[2024 Australian Open winner]

Plan: Retrieve the name of the 2024 Australian Open winner from the search results.

#E2 = LLM[What is the name of the 2024 Australian Open winner, given #E1]

Plan: Use Google to search for the hometown of the 2024 Australian Open winner.

#E3 = Google[hometown of 2024 Australian Open winner, given #E2]

Plan: Retrieve the hometown of the 2024 Australian Open winner from the search results.

#E4 = LLM[What is the hometown of the 2024 Australian Open winner, given #E3]
```

因为使用了LangGraph，为了将Planner连接到图表，我们将创建一个 get_plan 节点，该节点接受 ReWOO 状态并返回steps和 plan_string 字段。

```python
import refrom langchain_core.prompts

import ChatPromptTemplate
```

```python
# Regex to match expressions of the form E#… = …[…]

regex_pattern = r”Plan:s*(.+)s*(#Ed+)s*=s*(w+)s*[([^]]+)]”

prompt_template = ChatPromptTemplate.from_messages([(“user”, prompt)])

planner = prompt_template | model
```

```python
def get_plan(state: ReWOO):

task = state[“task”]

result = planner.invoke({“task”: task})

# Find all matches in the sample text

matches = re.findall(regex_pattern, result.content)

return {“steps”: matches, “plan_string”: result.content}
```


#### 第二步 构建Worker

Worker负责接收Task List并按顺序使用工具执行任务。下面实例化搜索引擎，并定义工具执行节点。

```python
from langchain_community.tools.tavily_search import TavilySearchResults
```

```python
search = TavilySearchResults()

def _get_current_task(state: ReWOO):

if state[“results”] is None:

return 1

if len(state[“results”]) == len(state[“steps”]):

return None

else:

return len(state[“results”]) + 1
```


```python
def tool_execution(state: ReWOO):

“””Worker node that executes the tools of a given plan.”””

_step = _get_current_task(state)

_, step_name, tool, tool_input = state[“steps”][_step – 1]

_results = state[“results”] or {}

for k, v in _results.items():

tool_input = tool_input.replace(k, v)

if tool == “Google”:

result = search.invoke(tool_input)

elif tool == “LLM”:

result = model.invoke(tool_input)

else:

raise ValueError

_results[step_name] = str(result)

return {“results”: _results}
```

#### 第三步 构建Solver

Solver接收完整的计划，并根据来自Worker的工具调用结果，生成最终响应。

我们给Solver的Prompt很简单，“根据上面提供的证据，解决问题或任务，直接回答问题，不要多说”。

```python
solve_prompt = “””Solve the following task or problem. To solve the problem, we have made step-by-step Plan and retrieved corresponding Evidence to each Plan. Use them with caution since long evidence might contain irrelevant information.{plan}

Now solve the question or task according to provided Evidence above. Respond with the answerdirectly with no extra words.

Task: {task}

Response:””
```

```python
def solve(state: ReWOO):

plan = “”

for _plan, step_name, tool, tool_input in state[“steps”]:

_results = state[“results”] or {}

for k, v in _results.items():

tool_input = tool_input.replace(k, v)

step_name = step_name.replace(k, v)

plan += f”Plan: {_plan}n{step_name} = {tool}[{tool_input}]”

prompt = solve_prompt.format(plan=plan, task=state[“task”])

result = model.invoke(prompt)

return {“result”: result.content}
```


#### 第四步 构建Graph

下面，我们构建流程图，将Planner、Worker、Solver等节点添加进来，执行并输出结果。

```python
def _route(state):

_step = _get_current_task(state)

if _step is None:

# We have executed all tasks

return “solve”

else:

# We are still executing tasks, loop back to the “tool” node

return “tool”
```

```python
from langgraph.graph import END, StateGraph, START
```

```python
graph = StateGraph(ReWOO)

graph.add_node(“plan”, get_plan)

graph.add_node(“tool”, tool_execution)

graph.add_node(“solve”, solve)

graph.add_edge(“plan”, “tool”)

graph.add_edge(“solve”, END)

graph.add_conditional_edges(“tool”, _route)

graph.add_edge(START, “plan”)

app = graph.compile()

for s in app.stream({“task”: task}):

print(s)

print(“—“)
```

下图详细介绍了Planner、Worker和Solver的协作流程。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506071616192.jpg)


### 小结

相比ReAct，ReWOO 的创新点主要包括以下几个方面：

- 分离推理与观察：ReWOO 将推理过程与使用外部工具的过程分开，避免了在依赖观察的推理中反复提示的冗余问题，从而大幅减少了 Token 的消耗。
- 模块化设计：ReWOO 使用模块化框架，通过planer、worker和solver的分工合作，提高了系统的扩展性和效率。
- 效率提升：实验结果表明，REWOO不仅提升了准确率，还显著降低token消耗。
- 工具调用的鲁棒性：ReWOO 在工具失效的情况下表现出更高的鲁棒性，这意味着即使某些工具无法返回有效证据，ReWOO 仍能通过合理的计划生成和证据整合，提供有效的解决方案。

但是，REWOO的缺陷在于，非常依赖于Planner的规划能力，如果规划有误，则后续所有的执行都会出现错误。尤其是对于复杂任务，很难在初始阶段就制定合理且完备的计划清单。

因此，如果要提升Agent的准确率，需要有规划调整机制，即在执行的过程中，根据环境反馈，不断调整计划。

## Plan and Execute

ReWOO发源于ReAct，加入了规划器以减少Token的消耗。但是ReWOO的规划器完成规划之后，执行器就只负责执行，即使计划存在错误，执行器也只是机械式地执行命令，这就导致ReWOO这种模式非常依赖于规划器的准确性。

为了优化这个问题，我们需要为规划器增加一种Replan机制，即在计划执行的过程中，根据实际的条件和反馈，重新调整计划。这个也很符合人类做计划的模式，比如你之前计划要去自驾游，但是突然看到新闻说前往目的地的主干道路发生了泥石流，因此你肯定会调整计划，取消自驾游或者换一个目的地。

### Plan-and-Execute的概念

Plan-and-Execute这个方法的本质是先计划再执行，即先把用户的问题分解成一个个的子任务，然后再执行各个子任务，并根据执行情况调整计划。Plan-and-Execute相比ReWOO，最大的不同就是加入了Replan机制，其架构上包含规划器、执行器和重规划器：

- 规划器Planner负责让 LLM 生成一个多步计划来完成一个大任务，在实际运行中，Planner负责第一次生成计划；
- 执行器接收规划中的步骤，并调用一个或多个工具来完成该任务；
- 重规划器Replanner负责根据实际的执行情况和信息反馈来调整计划

下图是Plan-and-Execute的原理：

- Planner接收来自用户的输入，输出具体的任务清单；
- 将任务清单给到Single-Task Agent，即执行器，执行器会在循环中逐个处理任务；
- 执行器每处理一个任务，就将处理结果和状态同步给Replanner，Replanner一方面会输出反馈给用户，另一方面会更新任务清单；
- 任务清单再次给到执行器进行执行。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506071619020.jpg)

### Plan-and-Execute的实现过程

下面，通过实际的源码，详细介绍Plan-and-Execute模式的实现方法。

#### 第一步 构建执行器

下面，我们先创建要用来执行任务的执行器。在这个示例中，为了简单起见，我们将为每个任务使用相同的执行器，即搜索工具。但实际情况下，可以为不同的任务使用不同的执行器。

```python
from langchain import hub  
from langchain_openai import ChatOpenA  
from langgraph.prebuilt import create_react_agent  
from langchain_community.tools.tavily_search import TavilySearchResults  
tools = [TavilySearchResults(max_results=3)]

# Get the prompt to use – you can modify this!  
prompt = hub.pull(“wfh/react-agent-executor”)  
prompt.pretty_print()

# Choose the LLM that will drive the agent  
llm = ChatOpenAI(model=”gpt-4-turbo-preview”)  
agent_executor = create_react_agent(llm, tools, messages_modifier=prompt)
```

#### 第二步 定义系统状态

为什么要定义系统状态？因为在处理复杂的不确定性问题时，一个非常有效的方法是将执行阶段拆分为状态机和执行器的循环。

执行器将外部事件输入状态机，状态机告诉执行器必须采取的操作，而原始计划则成为状态机起始状态的初始化程序。这样做的优点在于状态机不依赖于混乱的执行细节，因此我们可以对其进行详尽而彻底的测试。

首先，我们需要跟踪当前计划，将其表示为字符串列表；然后跟踪先前执行的步骤，将其表示为元组列表；最后，还需要状态来表示最终响应以及原始输入。因此，整个状态机定义如下：

```python
import operator  
from typing import Annotated, List, Tuple, TypedDict  
class PlanExecute(TypedDict):  
input: str  
plan: List[str]  
past_steps: Annotated[List[Tuple], operator.add]  
response: str
```

#### 第三步 定义Planner

Planner的主要任务就是接收输入，并输出初始的Task List。

相比ReWOO的Planner，这里的Planner的Prompt会有所不同，“对于给定的目标，制定一个简单的分步计划。该计划涉及单个任务，如果正确执行，将产生正确的答案，不要添加任何多余的步骤，最后一步的结果应该是最终答案。确保每一步都包含所需的所有信息，不要跳过步骤。”

```python
from langchain_core.pydantic_v1 import BaseModel, Field

class Plan(BaseModel):  
“””Plan to follow in future”””  
steps: List[str] = Field(  
description=”different steps to follow, should be in sorted order”  
)

from langchain_core.prompts import ChatPromptTemplate

planner_prompt = ChatPromptTemplate.from_messages(  
[  
(  
“system”,  
“””For the given objective, come up with a simple step by step plan.  
This plan should involve individual tasks, that if executed correctly will yield the correct answer. Do not add any superfluous steps.  
The result of the final step should be the final answer. Make sure that each step has all the information needed – do not skip steps.”””,  
),  
(“placeholder”, “{messages}”),  
]  
)

planner = planner_prompt | ChatOpenAI(  
model=”gpt-4o”, temperature=0  
).with_structured_output(Plan)

planner.invoke(  
{  
“messages”: [  
(“user”, “what is the hometown of the current Australia open winner?”)  
]  
}  
)
```

#### 第四步 定义Replanner

Replanner的主要任务是根据子任务的执行结果，更新计划。

Replanner和Planner的prompt模板非常相似，但是约束了Replanner的目标任务、原始Plan、已执行的步骤、以及更新计划。

比如更新计划，我们要求“根据执行的步骤更新计划。如果不需要更多步骤，直接可以返回给用户；否则就填写计划，并向计划中添加仍需完成的步骤，不要将之前完成的步骤作为计划的一部分返回”

```python
from typing import Union  
class Response(BaseModel):  
“””Response to user.”””  
response: str

class Act(BaseModel):  
“””Action to perform.”””  
action: Union[Response, Plan] = Field(  
description=”Action to perform. If you want to respond to user, use Response. ”  
“If you need to further use tools to get the answer, use Plan.”  
)

replanner_prompt = ChatPromptTemplate.from_template(  
“””For the given objective, come up with a simple step by step plan.  
This plan should involve individual tasks, that if executed correctly will yield the correct answer. Do not add any superfluous steps.  
The result of the final step should be the final answer. Make sure that each step has all the information needed – do not skip steps.  
Your objective was this:  
{input}  
Your original plan was this:  
{plan}  
You have currently done the follow steps:  
{past_steps}  
Update your plan accordingly. If no more steps are needed and you can return to the user, then respond with that. Otherwise, fill out the plan. Only add steps to the plan that still NEED to be done. Do not return previously done steps as part of the plan.”””  
)

replanner = replanner_prompt | ChatOpenAI(  
model=”gpt-4o”, temperature=0  
).with_structured_output(Act)
```

#### 第五步 构建流程图

下面，我们构建流程图，将Planner、Replanner、执行器等节点添加进来，执行并输出结果。

```python
from typing import Literal  
async def execute_step(state: PlanExecute):  
plan = state[“plan”]  
plan_str = “n”.join(f”{i+1}. {step}” for i, step in enumerate(plan))  
task = plan[0]  
task_formatted = f”””For the following plan: {plan_str}. You are tasked with          executing step {1}, {task}.”””  
agent_response = await agent_executor.ainvoke(  
{“messages”: [(“user”, task_formatted)]}  
)  
return {  
“past_steps”: (task, agent_response[“messages”][-1].content),  
}

async def plan_step(state: PlanExecute):  
plan = await planner.ainvoke({“messages”: [(“user”, state[“input”])]})  
return {“plan”: plan.steps}

async def replan_step(state: PlanExecute):  
output = await replanner.ainvoke(state)  
if isinstance(output.action, Response):  
return {“response”: output.action.response}  
else:  
return {“plan”: output.action.steps}  
def should_end(state: PlanExecute) -> Literal[“agent”, “__end__”]:  
if “response” in state and state[“response”]:  
return “__end__”  
else:  
return “agent”

from langgraph.graph import StateGraph, START

workflow = StateGraph(PlanExecute)  
# Add the plan node  
workflow.add_node(“planner”, plan_step)  
# Add the execution step  
workflow.add_node(“agent”, execute_step)  
# Add a replan node  
workflow.add_node(“replan”, replan_step)  
workflow.add_edge(START, “planner”)  
# From plan we go to agent  
workflow.add_edge(“planner”, “agent”)  
# From agent, we replan  
workflow.add_edge(“agent”, “replan”)  
workflow.add_conditional_edges(“replan”, should_end)  
app = workflow.compile()
```

### 小结

从原理上看，Plan-and-Execute和ReAct也有一定的相似度，但是Plan-and-Execute的优点是具备明确的长期规划，这一点即使非常强大的LLM也难以做到。同时可以只使用较大的模型做规划，而使用较小的模型执行步骤，降低执行成本。

但是Plan-and-execute的局限性在于，每个任务是按顺序执行的，下一个任务都必须等上一个任务完成之后才能执行，这可能会导致总执行时间的增加。

一种有效改进的办法是将每个任务表示为有向无环图DAG，从而实现任务的并行执行。

## LLM Compiler

Plan-and-execute的局限性在于，每个任务是按顺序执行的，这可能会导致总执行时间的增加。

一种有效改进的办法是将每个任务表示为有向无环图DAG，这样可以让多个任务并行执行，大幅降低执行总时间。

### LLM Compiler的概念

LLM Compiler是伯克利大学的SqueezeAILab于2023年12月提出的新项目。这个项目在ReWOO引入的变量分配的基础上，进一步训练大语言模型生成一个有向无环图（Directed Acyclic Graph，DAG，如下图所示）类的规划。DAG可以明确各步骤任务之间的依赖关系，从而并行执行任务，实现类似处理器“乱序执行”的效果，可以大幅加速AI Agent完成任务的速度。

比如下图的例子，向Agent提问“微软的市值需要增加多少才能超过苹果的市值？”，Planner并行搜索微软的市值和苹果的市值，然后进行合并计算。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506072058868.jpg)

#### LLM Compiler设计模式主要有以下组件：

- Planner：输出流式传输任务的DAG，每个任务都包含一个工具、参数和依赖项列表。相比ReWOO的Planner，依赖项列表是最大的不同。
- Task Fetching Unit：调度并执行任务，一旦满足任务的依赖性，该单元就会安排任务。由于许多工具涉及对搜索引擎或LLM的其他调用，因此额外的并行性可以显著提高速度。
- Joiner：由LLM根据整个历史记录（包括任务执行结果），决定是否响应最终答案或是否将进度重新传递回Planner。

#### LLM Compiler的原理

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506072100664.jpg)

- Planner接收来自用户的输入，输出流式传输任务的DAG
- Task Fetching Unit从式传输任务DAG中读取任务，通过处理工具并行执行
- Task Fetching Unit将状态和结果传递给Joiner（或Replanner），Joiner来决定是将结果输出给用户，还是增加更多任务交由Task Fetching Unit处理

### LLM Compiler的实现过程

下面，通过实际的源码，详细介绍LLM Compiler模式的实现方法。

#### 第一步 构建工具Tools

首先，我们要定义Agent需要使用的工具。在这个例子中，我们将使用搜索引擎 + 计算器这两个工具。

```python
from langchain_community.tools.tavily_search import TavilySearchResults  
from langchain_openai import ChatOpenAI  
from math_tools import get_math_tool

_get_pass(“TAVILY_API_KEY”)

calculate = get_math_tool(ChatOpenAI(model=”gpt-4-turbo-preview”))  
search = TavilySearchResults(  
max_results=1,  
description=’tavily_search_results_json(query=”the search query”) – a search engine.’,  
)

tools = [search, calculate]
```

#### 第二步 构建Planner

Planner接收用户输入，并生成一个待执行的任务清单的DAG。

以下代码构建了Planner的提示模板，并将其与 LLM 和输出解析器组合在一起，输出解析器处理以下形式的任务列表。在Planner中，我们同时定义了replanner的Prompt，这个prompt提出了三项核心的约束

- 启动当前计划时，应该从概述下一个计划策略的“Thought”开始
- 在当前计划中，绝不应该重复上一个计划中已经执行的操作
- 必须从上一个任务索引的末尾继续任务索引，不要重复任务索引

```python
def create_planner(  
llm: BaseChatModel, tools: Sequence[BaseTool], base_prompt: ChatPromptTemplate  
):  
tool_descriptions = “n”.join(  
f”{i+1}. {tool.description}n”  
for i, tool in enumerate(  
tools  
)  # +1 to offset the 0 starting index, we want it count normally from 1.  
)  
planner_prompt = base_prompt.partial(  
replan=””,  
num_tools=len(tools)  
+ 1,  # Add one because we’re adding the join() tool at the end.  
tool_descriptions=tool_descriptions,  
)  
replanner_prompt = base_prompt.partial(  
replan=’ – You are given “Previous Plan” which is the plan that the previous agent created along with the execution results ‘  
“(given as Observation) of each plan and a general thought (given as Thought) about the executed results.”  
‘You MUST use these information to create the next plan under “Current Plan”.n’  
‘ – When starting the Current Plan, you should start with “Thought” that outlines the strategy for the next plan.n’  
” – In the Current Plan, you should NEVER repeat the actions that are already executed in the Previous Plan.n”  
” – You must continue the task index from the end of the previous one. Do not repeat task indices.”,  
num_tools=len(tools) + 1,  
tool_descriptions=tool_descriptions,  
)

def should_replan(state: list):  
# Context is passed as a system message  
return isinstance(state[-1], SystemMessage)

def wrap_messages(state: list):  
return {“messages”: state}

def wrap_and_get_last_index(state: list):  
next_task = 0  
for message in state[::-1]:  
if isinstance(message, FunctionMessage):  
next_task = message.additional_kwargs[“idx”] + 1  
break  
state[-1].content = state[-1].content + f” – Begin counting at : {next_task}”  
return {“messages”: state}

return (  
RunnableBranch(  
(should_replan, wrap_and_get_last_index | replanner_prompt),  
wrap_messages | planner_prompt,  
)  
| llm  
| LLMCompilerPlanParser(tools=tools)  
)  
llm = ChatOpenAI(model=”gpt-4-turbo-preview”)  
planner = create_planner(llm, tools, prompt)
```

#### 第三步 构建Task Fetching Unit

这个部分负责安排任务，它接收以下格式的数据流。

tool:BaseTool,

dependencies:number[]

其核心思想是，一旦满足依赖关系，就开始执行工具，可以通过多线程实现。下面这段代码的关键就在于schedule_tasks，会将所有任务处理成有向无环图。在当前任务存在尚未完成的依赖关系时，放入pending task；在当前任务所有依赖关系都已完成时，执行任务。

```python
@as_runnable

def schedule_task(task_inputs, config):  
task: Task = task_inputs[“task”]  
observations: Dict[int, Any] = task_inputs[“observations”]  
try:  
observation = _execute_task(task, observations, config)  
except Exception:  
import traceback

observation = traceback.format_exception()  # repr(e) +

observations[task[“idx”]] = observation  
def schedule_pending_task(task: Task, observations: Dict[int, Any], retry_after: float = 0.2):  
while True:  
deps = task[“dependencies”]  
if deps and (any([dep not in observations for dep in deps])):  
# Dependencies not yet satisfied  
time.sleep(retry_after)  
continue  
schedule_task.invoke({“task”: task, “observations”: observations})  
break

@as_runnable  
def schedule_tasks(scheduler_input: SchedulerInput) -> List[FunctionMessage]:  
“””Group the tasks into a DAG schedule.”””  
tasks = scheduler_input[“tasks”]  
args_for_tasks = {}  
messages = scheduler_input[“messages”]

observations = _get_observations(messages)  
task_names = {}  
originals = set(observations)

futures = []  
retry_after = 0.25  # Retry every quarter second  
with ThreadPoolExecutor() as executor:  
for task in tasks:  
deps = task[“dependencies”]  
task_names[task[“idx”]] = (  
task[“tool”] if isinstance(task[“tool”], str) else task[“tool”].name  
)  
args_for_tasks[task[“idx”]] = task[“args”]  
if (  
# Depends on other tasks  
deps  
and (any([dep not in observations for dep in deps]))  
):  
futures.append(  
executor.submit(  
schedule_pending_task, task, observations, retry_after  
)  
)  
else:  
# No deps or all deps satisfied，can schedule now  
schedule_task.invoke(dict(task=task, observations=observations))  
# futures.append(executor.submit(schedule_task.invoke dict(task=task, observations=observations)))

# All tasks have been submitted or enqueued  
# Wait for them to complete  
wait(futures)  
# Convert observations to new tool messages to add to the state  
new_observations = {  
k: (task_names[k], args_for_tasks[k], observations[k])  
for k in sorted(observations.keys() – originals)  
}

tool_messages = [  
FunctionMessage(  
name=name, content=str(obs), additional_kwargs={“idx”: k, “args”: task_args}  
)  
for k, (name, task_args, obs) in new_observations.items()  
]

return tool_messages

import itertools

@as_runnable  
def plan_and_schedule(state):  
messages = state[“messages”]  
tasks = planner.stream(messages)  
# Begin executing the planner immediately  
try:  
tasks = itertools.chain([next(tasks)], tasks)  
except StopIteration:  
# Handle the case where tasks is empty.  
tasks = iter([])  
scheduled_tasks = schedule_tasks.invoke(  
{  
“messages”: messages,  
“tasks”: tasks,  
}  
)  
return {“messages”: [scheduled_tasks]}
```

#### 第四步 构建Joiner

前面我们构建了Planner和Task Fetching Unit，下一步我们需要构建Joiner来处理工具的输出，以及决定是否需要使用新的计划并开启新的循环。

```python
class FinalResponse(BaseModel):  
“””The final response/answer.”””  
response: str

class Replan(BaseModel):  
feedback: str = Field(  
description=”Analysis of the previous attempts and recommendations on what needs to be fixed.”  
)

class JoinOutputs(BaseModel):  
“””Decide whether to replan or whether you can return the final response.”””  
thought: str = Field(  
description=”The chain of thought reasoning for the selected action”  
)  
action: Union[FinalResponse, Replan]

joiner_prompt = hub.pull(“wfh/llm-compiler-joiner”).partial(  
examples=””  
)  # You can optionally add examples

llm = ChatOpenAI(model=”gpt-4-turbo-preview”)  
runnable = create_structured_output_runnable(JoinOutputs, llm, joiner_prompt)
```

如果Agent需要继续循环，我们需要选择状态机内的最新消息，并按照Planner的要求输出相应的格式。

```python
def _parse_joiner_output(decision: JoinOutputs) -> List[BaseMessage]:  
response = [AIMessage(content=f”Thought: {decision.thought}”)]  
if isinstance(decision.action, Replan):  
return response + [  
SystemMessage(  
content=f”Context from last attempt: {decision.action.feedback}”  
)  
]  
else:

return {“messages”: response + [AIMessage(content=decision.action.response)]}  
def select_recent_messages(state) -> dict:  
messages = state[“messages”]  
selected = []  
for msg in messages[::-1]:  
selected.append(msg)  
if isinstance(msg, HumanMessage):  
break

return {“messages”: selected[::-1]}  
joiner = select_recent_messages | runnable | _parse_joiner_output  
input_messages = [HumanMessage(content=example_question)] + tool_messages  
joiner.invoke(input_messages)
```

#### 第五步 构建流程图

下面，我们构建流程图，将Planner、Task Fetching Unit、Joiner等节点添加进来，循环执行并输出结果。

```python
from langgraph.graph import END, StateGraph, START  
from langgraph.graph.message import add_messages  
from typing import Annotated

class State(TypedDict):  
messages: Annotated[list, add_messages]

graph_builder = StateGraph(State)

graph_builder.add_node(“plan_and_schedule”, plan_and_schedule)  
graph_builder.add_node(“join”, joiner)  
graph_builder.add_edge(“plan_and_schedule”, “join”)

def should_continue(state):  
messages = state[“messages”]  
if isinstance(messages[-1], AIMessage):  
return END  
return “plan_and_schedule”

graph_builder.add_conditional_edges(  
start_key=”join”,  
# Next, we pass in the function that will determine which node is called next.  
condition=should_continue,  
)

graph_builder.add_edge(START, “plan_and_schedule”)  
chain = graph_builder.compile()
```

### 小结

通过前面内容，按照递进关系，依次介绍了REWOO、Plan-and-Execute和LLM Compiler三种更侧重规划能力的AI Agent设计模式。从最初的ReAct模式出发，加入规划能力即演变成REWOO；再加上Replan能力即演变成Plan-and-Execute；最后再加上DAG和并行处理能力，即演变成LLM Compiler

## Basic Reflection

AI Agent的左右互搏之术

### Basic Reflection的概念

Basic Reflection可以类比于左右互博。左手是Generator，负责根据用户指令生成结果；右手是Reflector，来审查Generator的生成结果并给出建议。在左右互搏的情况下，Generator生成的结果越来越好，Reflector的检查越来越严格，输出的结果也越来越有效。

下图是Basic Reflection的原理，非常简单。
- Generator接收来自用户的输入，输出initial response
- Reflector接收来自Generator的response，根据开发者设置的要求，给出Reflections，即评语、特征、建议
- Generator再根据Reflector给出的反馈进行修改和优化，输出下一轮response
- 循环往复，直到循环次数

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506072105975.jpg)

### Basic Reflection的实现过程

下面，通过实际的源码，详细介绍Basic Reflection模式的实现方法

#### 第一步 构建Generator

在下面的例子中，我们先构建一个能够生成5段话的文章生成器。

首先，我们给Generator约定了Prompt，告诉Generator具体的角色和目标，并且要求如果用户给出建议，需要给出修改后的版本

然后选择LLM模型，并构建Generator

最后要求输出Generator生成的内容

```python
from langchain_core.messages import AIMessage, BaseMessage, HumanMessage  
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder  
from langchain_fireworks import ChatFireworks

prompt = ChatPromptTemplate.from_messages(  
[  
(  
“system”,  
“You are an essay assistant tasked with writing excellent 5-paragraph essays.”  
” Generate the best essay possible for the user’s request.”  
” If the user provides critique, respond with a revised version of your previous attempts.”,  
),  
MessagesPlaceholder(variable_name=”messages”),  
]  
)  
llm = ChatFireworks(  
model=”accounts/fireworks/models/mixtral-8x7b-instruct”,  
model_kwargs={“max_tokens”: 32768},  
)

generate = prompt | llm  
essay = “”  
request = HumanMessage(  
content=”Write an essay on why the little prince is relevant in modern childhood”  
)

for chunk in generate.stream({“messages”: [request]}):  
print(chunk.content, end=””)  
essay += chunk.content
```

下面是Generator的Initial Response，可以看出Generator虽然按照我们的要求生成了初始回复，但是回复的整体质量还有很大的提升空间。

```python
Title: The Relevance of The Little Prince in Modern Childhood

The Little Prince, a novella by Antoine de Saint-Exupéry, has been a childhood favorite for generations. Despite being published over seven decades ago, its timeless themes continue to resonate with modern children, making it highly relevant in contemporary childhood.

Firstly, the story explores the complex nature of human relationships, which is particularly relevant for modern children growing up in an increasingly connected yet impersonal world. Through the little prince’s encounters with various grown-ups on different planets, the book highlights the importance of genuine connections and understanding. In an age where digital communication often replaces face-to-face interaction, this message is more pertinent than ever. The Little Prince encourages children to look beyond superficial relationships and seek deeper connections, fostering empathy and emotional intelligence.

Secondly, the book deals with the concept of responsibility and self-discovery, elements that are integral to a child’s growth. The little prince’s journey is essentially a quest for self-discovery, leading him to realize his responsibility towards his beloved rose. This narrative encourages modern children to embrace their individuality while understanding the significance of their actions. In a society that often overlooks the emotional well-being of children, The Little Prince offers a refreshing perspective on personal growth and responsibility.

Thirdly, the book addresses the challenging theme of loss and bereavement. The little prince’s departure from his asteroid and his subsequent encounters with the fox and the snake are profound reflections on the inevitability of loss and the importance of cherishing relationships. In a time when children are exposed to various forms of loss, from the death of loved ones to environmental degradation, The Little Prince provides a gentle yet powerful way to understand and cope with these experiences.

However, some critics argue that the book’s pace and abstract concepts might be challenging for modern children with short attention spans. To address this, a revised version could incorporate more visual elements and interactive activities to engage young readers better. Additionally, supplementary materials explaining the book’s themes in simpler terms could be provided for parents and educators to use in discussions with children.

In conclusion, The Little Prince remains relevant in modern childhood due to its exploration of human relationships, self-discovery, and loss. These themes, wrapped in a captivating narrative, offer valuable lessons for modern children. While some adaptations may be necessary to cater to the preferences of today’s children, the essence of the story remains a powerful tool for teaching emotional intelligence, personal growth, and resilience.
```


#### 第二步 构建Reflector

针对Generator，我们构建一个专门的Reflector。

同样的，我们要先给Reflector约定Prompt，告诉Reflector“你是一名正在给论文评分的老师。请对用户的论文提出批评和建议，包括长度、深度、风格等要求。”

然后选择LLM模型，并构建Reflector

```python
reflection_prompt = ChatPromptTemplate.from_messages(  
[  
(  
“system”,  
“You are a teacher grading an essay submission. Generate critique and recommendations for the user’s submission.”  
” Provide detailed recommendations, including requests for length, depth, style, etc.”,  
),  
MessagesPlaceholder(variable_name=”messages”),  
]  
)  
reflect = reflection_prompt | llm
```

下面是针对Generator的Initial Response，Reflector反馈的评语和建议。Reflector给出了B+的评级，并且按照我们的要求，从文章的长度、深度、风格等维度给出了具体的建议和总结。

```python
Essay Grade: B+

The essay you submitted provides a clear and well-structured argument about the relevance of The Little Prince in modern childhood. You have demonstrated a strong understanding of the text and its themes, and have effectively applied them to the context of contemporary childhood. However, there are some areas where improvement could be made to enhance the depth, style, and overall flow of your essay.

1. Length: While your essay is well-written and informative, it is relatively brief. Expanding on each point with more detailed analysis and examples would strengthen your argument and demonstrate a more comprehensive understanding of the text. Aim for a minimum of 500 words to allow for a more in-depth exploration of your ideas.

2. Depth: Although you have touched upon the relevance of the novel’s themes, further analysis is needed to truly establish its significance in modern childhood. For example, when discussing the complex nature of human relationships, delve into how the digital age affects children’s communication skills, and how The Little Prince addresses this issue. Providing concrete examples from the text and connecting them to real-world scenarios will make your argument more compelling.

3. Style: To engage your readers more effectively, consider varying your sentence structure and length. Using a mix of simple, compound, and complex sentences will improve the flow of your essay and make it more engaging to read. Additionally, watch your tense consistency. Ensure that you maintain the same tense throughout your essay to avoid confusion.

4. Recommendations: While your suggestions for adaptation are a good start, they could be expanded upon to provide more comprehensive recommendations. For example, you may want to discuss different methods of incorporating visual elements and interactive activities, such as illustrations, quizzes, or discussion questions. This will demonstrate that you have thoughtfully considered the needs of modern children and have developed strategies to address these challenges.

5. Conclusion: Your conclusion could benefit from a stronger summarization of your key points and an assertive final statement about the relevance of The Little Prince in modern childhood. Tying all your arguments together in a concise and powerful manner will leave a lasting impression on your readers and solidify your position.

Overall, your essay is well-researched and provides a solid foundation for a compelling argument about the relevance of The Little Prince in modern childhood. With some expansion, deeper analysis, and stylistic improvements, your essay can achieve an even higher level of excellence.
```

#### 第三步 循环执行

接下来，我们就可以循环执行这个“生成 – 检查”的过程，重复规定的次数，或者约定当Generator的生成结果达到多少分时，停止循环。

大家可以看到，在循环过程中，我们也加入了人类的反馈建议，有助于Generator和Reflector学习迭代。

```python
for chunk in generate.stream(  
{“messages”: [request, AIMessage(content=essay), HumanMessage(content=reflection)]}  
):  
print(chunk.content, end=””)
```

#### 第四步 构建流程图

下面，我们构建流程图，将Generator、Reflector等节点添加进来，循环执行并输出结果。

```python
from typing import Annotated, List, Sequence  
from langgraph.graph import END, StateGraph, START  
from langgraph.graph.message import add_messages  
from typing_extensions import TypedDict

class State(TypedDict):  
messages: Annotated[list, add_messages]

async def generation_node(state: Sequence[BaseMessage]):  
return await generate.ainvoke({“messages”: state})

async def reflection_node(messages: Sequence[BaseMessage]) -> List[BaseMessage]:  
# Other messages we need to adjust  
cls_map = {“ai”: HumanMessage, “human”: AIMessage}  
# First message is the original user request. We hold it the same for all nodes  
translated = [messages[0]] + [  
cls_map[msg.type](content=msg.content) for msg in messages[1:]  
]  
res = await reflect.ainvoke({“messages”: translated})  
# We treat the output of this as human feedback for the generator  
return HumanMessage(content=res.content)

builder = StateGraph(State)  
builder.add_node(“generate”, generation_node)  
builder.add_node(“reflect”, reflection_node)  
builder.add_edge(START, “generate”)

def should_continue(state: List[BaseMessage]):  
if len(state) > 6:  
# End after 3 iterations  
return END  
return “reflect”

builder.add_conditional_edges(“generate”, should_continue)  
builder.add_edge(“reflect”, “generate”)  
graph = builder.compile()

async for event in graph.astream(  
[  
HumanMessage(  
content=”Generate an essay on the topicality of The Little Prince and its message in modern life”  
)  
],  
):  
print(event)  
print(“—“)
```

至此，Basic Reflection的完整流程就介绍完了，非常简单，相信哪怕是没有AI基础的同学也能看懂。

### 小结

Basic Reflection的架构，非常适合于进行相对比较发散的内容生成类工作，比如文章写作、图片生成、代码生成等等。

总体而言，Basic Reflection是一种非常高效的反思类AI Agent设计模式。Basic Reflection 的思路非常朴素，使用成本较低。但是在实际应用中，Basic Reflection也面临着一些缺陷：

- 对于一些比较复杂的问题，显然需要Generator具备更强大的推理能力
- Generator生成的结果可能会过于发散，和我们要求的结果相去甚远
- 在一些复杂场景下，Generator和Reflector之间的循环次数不太好定义，如果次数太少，生成效果不够理想；如果次数太多，对token的消耗会很大。

我们有两种方法来优化Basic Reflection，一种是边推理边执行的Self Discover模式，一种是增加了强化学习的Reflexion模式。

## Self Discover

### Self Discover的概念

Self-Discover 是由Google的研究人员提出的一种AI Agent框架，可实现自动发现和构建推理结构，以解决各种推理任务。这种方法的核心是一个自发现过程，它允许大型语言模型在没有明确标签的情况下，自主地从多个原子推理模块（如批判性思维和逐步思考）中选择，并将其组合成一个推理结构。

Self-Discover框架包含两个主要阶段，自发现特定任务的推理结构、应用推理结构解决问题。

#### 阶段一：自发现特定任务的推理结构

主要包含三个主要动作：选择（SELECT）、适应（ADAPT）和实施（IMPLEMENT）。

**选择**：在这个阶段，模型从一组原子推理模块（例如“批判性思维”和“逐步思考”）中选择对于解决特定任务有用的模块。模型通过一个元提示来引导选择过程，这个元提示结合了任务示例和原子模块描述。选择过程的目标是确定哪些推理模块对于解决任务是有助的。

**适应**：一旦选定了相关的推理模块，下一步是调整这些模块的描述使其更适合当前任务。这个过程将一般性的推理模块描述，转化为更具体的任务相关描述。例如对于算术问题，“分解问题”的模块可能被调整为“按顺序计算每个算术操作”。同样，这个过程使用元提示和模型来生成适应任务的推理模块描述。

**实施**：在适应了推理模块之后，Self-Discover框架将这些适应后的推理模块描述转化为一个结构化的可执行计划。这个计划以键值对的形式呈现，类似于JSON，以便于模型理解和执行。这个过程不仅包括元提示，还包括一个人类编写的推理结构示例，帮助模型更好地将自然语言转化为结构化的推理计划。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081013879.jpg)


#### 阶段二：应用推理结构

完成阶段一之后，模型将拥有一个专门为当前任务定制的推理结构。在解决问题的实例时，模型只需遵循这个结构，逐步填充JSON中的值，直到得出最终答案。

### Self Discover的实现过程

下面，风叔通过实际的源码，详细介绍Self Discover模式的实现方法，具体的源代码地址可以在文章结尾处获取。在手机端阅读源码的体验不太好，建议大家在PC端打开。

#### 第一步 定义Select、Adapt和Implement

第一步要做的事情，是前面所讲的阶段一，定义好select、adapt和implement这三个过程。

首先，定义select。给Select的prompt是，“对于给定的任务，找出最相关的reasoning module，即确定通过什么方法来解决任务”

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081032024.jpg)

然后，定义adapt。给Adapt的prompt是，“在不需要给出完整解决方案的情况下，让大模型调整上一步得到的reasoning module，以更好地适应任务。”

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081032977.png)

接下来，定义implement。给Implement的prompt是，“在不需要给出完整解决方案的情况下，利用上一步得到的优化后的reasoning module，创建一个可执行的推理结构。”

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081032217.png)

#### 第二步 执行推理结构

第二步要做的事情，就是前面所讲的阶段二，应用这个推理结构获得最终答案

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081033244.png)

#### 第三步 提出问题，开始推理

第三步就是将这个过程进行推理实现，如下面的代码所示，这一步非常关键，大家可以逐条仔细看：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081033605.png)

这段代码非常简单。首先，我们提出了39种reasoning modules，即推理的具体方法，大家也可以自己提炼出一些推理方法，通过prompt告知给Agent。

然后，我们提出一个具体的推理任务：“Lisa 有 10 个苹果。她给了朋友 3 个苹果，然后从商店买了 5 个苹果。Lisa 现在有多少个苹果？”

然后大模型会自动选出最合适的推理方法。实践中发现，大模型选择了方法9和10。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081033362.png)

接下来，针对目标任务，大模型自动优化推理方法的描述。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081033466.png)

然后，大模型自动给出了具体的推理结构。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081033409.png)

至此，Self Discover的原理和流程就介绍完了。**通过这种推理方式，未来Agent就能解决更加多样化的问题，因为任何类型的问题，总能有其对应的推理和思考方式。**

### 小结

整个Self-Discover的关键在于，它允许模型在没有人类干预的情况下，自主地生成适合特定任务的推理结构。这样不仅提高了模型的推理能力，而且提高了推理过程的可解释性。通过这种方式，模型能够更有效地处理复杂和多样化的任务。

本篇文章提到的例子，虽然结构很清晰，但不足之处在于没有使用Tools。对于需要使用搜索工具、数学工具等外部工具的场景，我们也可以让大模型在adapt环节给出适合解决目标任务的外部工具，然后在后续过程中执行。

## Reflexion

Basic Reflection 的思路非常朴素，就是通过左右互搏让两个Agent相互进化，实现成本也较低。

但是在实际应用中，Basic Reflection的Generator生成的结果可能会过于发散，和我们要求的结果相去甚远。同时，当面对一些复杂度很高的问题时，Basic Reflection框架也难以解决。有两种方法来优化Basic Reflection，一种是边推理边执行的Self Discover模式，一种是增加了强化学习的Reflexion模式。

### Reflexion的概念

Reflexion本质上是强化学习，可以理解为是Basic reflection 的升级版。Reflexion机制下，整个架构包括Responder和Revisor，和Basic Reflection机制中的Generator和Reflector有点类似。但不同之处在于， Responder自带批判式思考的陈述，Revisor会以 Responder 中的批判式思考作为上下文参考对初始回答做修改。此外，Revisor还引入了外部数据来评估回答是否准确，这使得反思的内容更加具备可靠性。

下图是Reflexion的原理：

- Responder接收来自用户的输入，输出initial response，其中包括了Response、Critique和工具指示（示例图中是Search）
- Responder将Initial Response给到执行工具，比如搜索接口，对Initial Response进行初步检索
- 将初步检索的结果给到Revisor，Revisor输出修改后的Response，并给出引用来源Citations
- 再次给到执行工具，循环往复，直到循环次数
- Revisor将最终结果输出给用户

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081051816.jpg)

### Reflexion的实现过程

下面，通过实际的源码，详细介绍Basic Reflection模式的实现方法。

#### 第一步 构建Responder

在下面的例子中，我们先构建一个Responder

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081051966.png)

为Responder确定Prompt模板，并建立一个Responder。通过Prompt，我们告诉Responder，“你需要反思自己生成的答案，要最大化严谨程度，同时需要搜索查询最新的研究信息来改进答案”。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081051712.png)

#### 第二步 构建Revisor

接下来我们开始构建Revisor，通过Prompt告诉Revisor

- 应该使用之前生成的critique为答案添加重要信息
- 必须在修改后的答案中包含引用，以确保答案来源可验证
- 在答案底部要添加参考，形式为[1] https://example.com
- 使用之前的批评从答案中删除多余的信息，并确保其不超过 250 个字。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081051868.png)

#### 第三步构建Tools

接下来，创建一个节点来执行工具调用。虽然我们为 LLM 赋予了不同的模式名称，但我们希望它们都路由到同一个工具。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081051802.png)

#### 第四步构建Graph

下面，我们构建流程图，将Responder、Revisor、工具等节点添加进来，循环执行并输出结果。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081051774.png)

以上内容就是Reflexion的核心思想，其实完整的Reflexion框架要比上文介绍的更复杂，包括Actor、Evaluator和self-Reflection三块，上文的内容只涵盖了Actor。

1. 参与者（Actor）：主要作用是根据状态观测量生成文本和动作。参与者在环境中采取行动并接受观察结果，从而形成轨迹。前文所介绍的Reflexion Agent，其实指的就是这一块
2. 评估者（Evaluator）：主要作用是对参与者的输出进行评价。具体来说，它将生成的轨迹（也被称作短期记忆）作为输入并输出奖励分数。根据人物的不同，使用不同的奖励函数（决策任务使用LLM和基于规则的启发式奖励）。
3. 自我反思（Self-Reflection）：由大语言模型承担，能够为未来的试验提供宝贵的反馈。自我反思模型利用奖励信号、当前轨迹和其持久记忆生成具体且相关的反馈，并存储在记忆组件中。Agent会利用这些经验（存储在长期记忆中）来快速改进决策。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081056615.jpg)

关于Reflexion完整的实现方案可参考：https://github.com/noahshinn/reflexion

### 小结

Reflexion是一个带强化学习的设计模式，这种模式最适合以下情况：

智能体需要从尝试和错误中学习：自我反思旨在通过反思过去的错误并将这些知识纳入未来的决策来帮助智能体提高表现。这非常适合智能体需要通过反复试验来学习的任务，例如决策、推理和编程。

传统的强化学习方法失效：传统的强化学习（RL）方法通常需要大量的训练数据和昂贵的模型微调。自我反思提供了一种轻量级替代方案，不需要微调底层语言模型，从而使其在数据和计算资源方面更加高效。

需要细致入微的反馈：自我反思利用语言反馈，这比传统强化学习中使用的标量奖励更加细致和具体。这让智能体能够更好地了解自己的错误，并在后续的试验中做出更有针对性的改进。

但是，Reflexion也存在一些使用上的限制：

- 依赖自我评估能力：反思依赖于智能体准确评估其表现并产生有用反思的能力。这可能是具有挑战性的，尤其是对于复杂的任务，但随着模型功能的不断改进，预计自我反思会随着时间的推移而变得更好。
- 长期记忆限制：自我反思使用最大容量的滑动窗口，但对于更复杂的任务，使用向量嵌入或 SQL 数据库等高级结构可能会更有利。
- 代码生成限制：测试驱动开发在指定准确的输入输出映射方面存在限制（例如，受硬件影响的非确定性生成器函数和函数输出）。

## LATS

LATS可能是目前最强大的AI Agent设计框架，集多种规划和反思技术的集大成者。文章内容会相对比较复杂难懂，值得收藏和反复研读。

### LATS的概念

LATS，全称是Language Agent Tree Search，说的更直白一些，LATS = Tree search + ReAct + Plan&Execute+ Reflexion。这么来看，LATS确实非常高级和复杂，下面我们根据上面的等式，先从宏观上拆解一下LATS。

#### Tree Search

Tree Search是一种树搜索算法，LATS 使用蒙特卡罗树搜索（MCTS）算法，通过平衡探索和利用，找到最优决策路径。

蒙特卡罗方法可能大家都比较熟悉了，是一种通过随机采样模拟来求解问题的方法。通过生成随机数，建立概率模型，以解决难以通过其他方法解决的数值问题。蒙特卡罗方法的一个典型应用是求定积分。假设我们要计算函数 f(x) 在[a, b]之间的积分，即阴影部分面积。

蒙特卡罗方法的解法如下：在[a, b]之间取一个随机数 x，用 f(x)⋅(b−a) 来估计阴影部分的面积。为了提高估计精度，可以取多个随机数 x，然后取这些估计值的平均值作为最终结果。当取的随机数 x 越多，结果将越准确，估计值将越接近真实值。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081100610.jpg)

蒙特卡罗树搜索（MCTS）则是一种基于树结构的蒙特卡罗方法。它在整个 2^N（N 为决策次数，即树深度）空间中进行启发式搜索，通过反馈机制寻找最优路径。MCTS 的五个主要核心部分是：

1. 树结构：每一个叶子节点到根节点的路径都对应一个解，解空间大小为 2^N。
2. 蒙特卡罗方法：通过随机统计方法获取观测结果，驱动搜索过程。
3. 损失评估函数：设计一个可量化的损失函数，提供反馈评估解的优劣。
4. 反向传播线性优化：采用反向传播对路径上的所有节点进行优化。
5. 启发式搜索策略：遵循损失最小化原则，在整个搜索空间上进行启发式搜索。

MCTS 的每个循环包括四个步骤：

1. 选择（Selection）：从根节点开始，按照最大化某种启发式价值选择子节点，直到到达叶子节点。使用上置信区间算法（UCB）选择子节点。
2. 扩展（Expansion）：如果叶子节点不是终止节点，扩展该节点，添加一个或多个子节点。
3. 仿真（Simulation）：从新扩展的节点开始，进行随机模拟，直到到达终止状态。
4. 反向传播（Backpropagation）：将模拟结果沿着路径反向传播，更新每个节点的统计信息。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081108689.jpg)

#### ReAct

ReAct的概念和设计模式，在上面已做过详细介绍。

它的典型流程如下图所示，可以用一个有趣的循环来描述：思考（Thought）→ 行动（Action）→ 观察（Observation），简称TAO循环。

- 思考（Thought）：面对一个问题，我们需要进行深入的思考。这个思考过程是关于如何定义问题、确定解决问题所需的关键信息和推理步骤。
- 行动（Action）：确定了思考的方向后，接下来就是行动的时刻。根据我们的思考，采取相应的措施或执行特定的任务，以期望推动问题向解决的方向发展。
- 观察（Observation）：行动之后，我们必须仔细观察结果。这一步是检验我们的行动是否有效，是否接近了问题的答案。
- 循环迭代

#### Plan & Execute

Plan & Execute的概念和设计模式，同样在上面已做过详细介绍，因此不再赘述。

Plan-and-Execute这个方法的本质是先计划再执行，即先把用户的问题分解成一个个的子任务，然后再执行各个子任务，并根据执行情况调整计划。

#### Reflexion

Reflexion的概念和设计模式，在上面做了详细介绍。

Reflexion的本质是Basic Reflection加上强化学习，完整的Reflexion框架由三个部分组成：

1. 参与者（Actor）：根据状态观测量生成文本和动作。参与者在环境中采取行动并接受观察结果，从而形成轨迹。前文所介绍的Reflexion Agent，其实指的就是这一块
2. 评估者（Evaluator）：对参与者的输出进行评价。具体来说，它将生成的轨迹（也被称作短期记忆）作为输入并输出奖励分数。根据人物的不同，使用不同的奖励函数（决策任务使用LLM和基于规则的启发式奖励）。
3. 自我反思（Self-Reflection）：这个角色由大语言模型承担，能够为未来的试验提供宝贵的反馈。自我反思模型利用奖励信号、当前轨迹和其持久记忆生成具体且相关的反馈，并存储在记忆组件中。智能体利用这些经验（存储在长期记忆中）来快速改进决策。

因此，融合了Tree Search、ReAct、Plan & Execute、Reflexion的能力于一身之后，LATS成为AI Agent设计模式中，集反思模式和规划模式的大成者。

### LATS的工作流程

LATS的工作流程如下图所示，包括以下步骤：

1. 选择 (Selection)：即从根节点开始，使用上置信区树 (UCT) 算法选择具有最高 UCT 值的子节点进行扩展。
2. 扩展 (Expansion)：通过从预训练语言模型 (LM) 中采样 n 个动作扩展树，接收每个动作并返回反馈，然后增加 n 个新的子节点。
3. 评估 (Evaluation)：为每个新子节点分配一个标量值，以指导搜索算法前进，LATS 通过 LM 生成的评分和自一致性得分设计新的价值函数。
4. 模拟 (Simulation)：扩展当前选择的节点直到达到终端状态，优先选择最高价值的节点。
5. 回溯 (Backpropagation)：根据轨迹结果更新树的值，路径中的每个节点的值被更新以反映模拟结果。
6. 反思 (Reflection)：在遇到不成功的终端节点时，LM 生成自我反思，总结过程中的错误并提出改进方案。这些反思和失败轨迹在后续迭代中作为额外上下文整合，帮助提高模型的表现。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081110827.jpg)

下图是在langchain中实现LATS的过程：

**第一步，选择**：根据下面步骤中的总奖励选择最佳的下一步行动，如果找到解决方案或达到最大搜索深度，做出响应；否则就继续搜索。

**第二步，扩展和执行**：生成N个潜在操作，并且并行执行。

**第三步，反思和评估**：观察行动的结果，并根据反思和外部反馈对决策评分。

**第四步，反向传播**：根据结果更新轨迹的分数。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081111919.jpg)

### LATS的实现过程

下面，通过实际的源码，详细介绍LATS模式的实现方法。

#### 第一步 构建树节点

LATS 基于蒙特卡罗树搜索。对于每个搜索步骤，它都会选择具有最高“置信上限”的节点，这是一个平衡开发（最高平均奖励）和探索（最低访问量）的指标。从该节点开始，它会生成 N（在本例中为 5）个新的候选操作，并将它们添加到树中。当它生成有效解决方案或达到最大次数（搜索树深度）时，会停止搜索。

在Node节点中，我们定义了几个关键的函数：

- best_child：选择 UCT 最高的子项进行下一步搜索
- best_child_score：返回具有最高价值的子项
- height：检查已经推进的树的深度
- upper_confidence_bound：返回 UCT 分数，平衡分支的探索与利用
- backpropogate：利用反向传播，更新此节点及其父节点的分数
- get_trajectory：获取代表此搜索分支的消息
- get_best_solution：返回当前子树中的最佳解决方案

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081111609.png)

#### 第二步 构建Agent

Agent将主要处理三个事项：

1. 反思：根据工具执行响应的结果打分
2. 初始响应：创建根节点，并开始搜索
3. 扩展：从当前树中的最佳位置，生成5个候选的下一步

对于更多实际的应用，比如代码生成，可以将代码执行结果集成到反馈或奖励中，这种外部反馈对Agent效果的提升将非常有用。

对于Agent，首先构建工具Tools，我们只使用了一个搜索引擎工具。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081111202.png)

然后，构建反射系统，反射系统将根据决策和工具使用结果，对Agent的输出进行打分，我们将在其他两个节点中调用此方法。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081111400.png)

接下来，我们从根节点开始，根据用户输入进行响应

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081111234.png)

然后开始根节点，我们将候选节点生成和reflection打包到单个节点中。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081111323.png)

#### 第三步 生成候选节点

对于每个节点，生成5个待探索的候选节点。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081111290.png)

将候选节点生成和refleciton步骤打包在下面的扩展节点中，所有操作都以批处理的方式进行，以加快执行速度。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081111417.png)

#### 第四步 构建流程图

下面，我们构建流程图，将根节点和扩展节点加入进来

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081111375.png)

至此，整个LATS的核心逻辑就介绍完了。

### 小结

与其他基于树的方法相比，LATS实现了自我反思的推理步骤，显著提升了性能。当采取行动后，LATS不仅利用环境反馈，还结合来自语言模型的反馈，以判断推理中是否存在错误并提出替代方案。这种自我反思的能力与其强大的搜索算法相结合，使得LATS更适合处理一些相对复杂的任务。

然而，由于算法本身的复杂性以及涉及的反思步骤，LATS通常比其他单智能体方法使用更多的计算资源，并且完成任务所需的时间更长。

## 总结

整个《AI大模型 设计模式》系列就全部介绍完了，从最经典的ReAct模式开始，沿着规划路线介绍了REWOO、Plan&Execute和LLM Compiler，沿着反思路线介绍了Basic Reflection、Self Discover和Reflexion，并以最强大的设计模式LATS作为收尾。整个系列基本上包含了目前AI大模型和AI Agent的全部主流设计框架，后续如果有新的前沿设计模式和具体案例，风叔还会零星做一些介绍。

但是，所有的这些设计模式，都只是在告诉AI Agent应该如何规划和思考，且只能依赖于大模型既有的知识储备。而实际应用中，我们往往更希望AI Agent结合我们给定的知识和信息，在更专业的垂直领域内进行规划和思考。

比如我们希望Agent帮我们做论文分析、书籍总结，或者在企业级场景中，让AI Agent写营销计划、内部知识问答、智能客服等等非常多的场景，只靠上面几种Agent设计模式是远远不够的，我们必须给大模型外挂知识库，并且通过工作流进一步约束和规范Agent的思考方向和行为模式。



