---
title: RAG设计模式介绍
category: AI
tags:
  - agent
  - prompt
head:
  - - meta
    - name: description
      content: 全网最全的AI大模型知识点总结，让天下没有难学的八股文！
---





前面，在讲述AI AGENT设计模式时，从最经典的ReAct模式开始，沿着规划路线介绍了REWOO、Plan&Execute和LLM Compiler，沿着反思路线介绍了Basic Reflection、Self Discover和Reflexion，并以最强大的设计模式LATS作为收尾。

但是，所有的这些设计模式，都只是在告诉AI Agent应该如何规划和思考，且只能依赖于大模型既有的知识储备。而实际应用中，我们往往更希望AI Agent结合我们给定的知识和信息，在更专业的垂直领域内进行规划和思考。

比如我们希望Agent帮我们做论文分析、书籍总结，或者在企业级场景中，让AI Agent写营销计划、内部知识问答、智能客服等等非常多的场景，只靠上面几种Agent设计模式是远远不够的，我们必须给大模型外挂知识库，并且通过工作流进一步约束和规范Agent的思考方向和行为模式。

解决这个问题的最佳方式是利用RAG技术，接下来我们正式看《RAG实战篇》系列。对于RAG还不太熟悉的朋友，可以先看文章[Rag入门知识](https://www.seven97.top/ai/rag-summary.html)

## 构建一个最小可行性的Rag系统
### RAG系统实现方案概览

#### Indexing（索引）

Indexing是任何RAG系统的第一步，在实际应用场景中，文档尺寸可能非常大，因此需要将长篇文档分割成多个文本块，以便更高效地处理和检索信息。

Indexing环节主要面临三个难题：

首先，内容表述不完整，内容块的语义信息受分割方式影响，致使在较长的语境中，重要信息被丢失或被掩盖。

其次，块相似性搜索不准确，随着数据量增多，检索中的噪声增大，导致频繁与错误数据匹配，使得检索系统脆弱且不可靠。

最后，参考轨迹不明晰，检索到的内容块可能来自任何文档，没有引用痕迹，可能出现来自多个不同文档的块，尽管语义相似，但包含的却是完全不同主题的内容。

在这个框架中，我们将在索引环节实现Chunk optimization（块优化）、Multi-representation indexing、Specialized Embeddings（特殊嵌入）和Hierachical Indexing（多级索引）这四种优化方案。

#### Query Translation

Query Translation主要处理用户的输入。在初始的RAG系统中，往往直接使用原始query进行检索，可能会存在三个问题：

第一，原始query的措辞不当，尤其是涉及到很多专业词汇时，query可能存在概念使用错误的问题；

第二，往往知识库内的数据无法直接回答，需要组合知识才能找到答案；

第三，当query涉及比较多的细节时，由于检索效率有限，大模型往往无法进行高质量的回答。

在这个框架中，我们将在这个环节实现Multi-query（多查询）、Rag-Fusion、Decomposition（查询分解）、Stepback和HYDE这五种优化方案

#### Routing（路由）

路由的作用，是为每个Query选择最合适的处理管道，以及依据来自模型的输入或补充的元数据，来确定将启用哪些模块。比如在索引环节引入多重索引技术后，就需要使用多级路由机制，根据Query引导至最合适的父级索引。

在路由环节，我们将实现Logical routing（基于逻辑的路由）和Sematic Routing（基于语义的路由）两种方案。

####  Query Construction（查询构建）

查询构建主要是为了将自然语言的Query，转化为某种特定机器或软件能理解的语言。因为随着大模型在各行各业的渗透，除文本数据外，诸如表格和图形数据等越来越多的结构化数据正被融入 RAG 系统。

比如在一些ChatBI的场景下，就需要将用户的Query内容，转化为SQL语句，进行数据库查询，这就是Text-to-SQL。再比如工业设计场景下，可能需要将用户的Query转化为设计指令，或者设备控制指令，这就是Text-to-Cypher。

在查询构建环节，我们将实现Text-to-SQL、Text-to-Cypher和Self-Query（让大模型自行构建Query）三种优化方案。

#### Retrieval（检索）

在检索的时候，用户的问题会被输入到嵌入模型中进行向量化处理，然后系统会在向量数据库中搜索与该问题向量语义上相似的知识文本或历史对话记录并返回。

在朴素RAG中，系统会将所有检索到的块直接输入到 LLM生成回答，导致出现中间内容丢失、噪声占比过高、上下文长度限制等问题。

在检索环节，我们将实现Reranking（重排序）、Refinement（压缩）、Corrective Rag（纠正性Rag）等方案。

#### Generation（生成）

在生成环节，可能会出现以下问题：

第一，当系统忽略了以特定格式（例如表格或列表）提取信息的指令时，输出可能会出现格式错误；

第二，输出错误或者输出不完整，比如对于一些比较类问题的处理往往不尽人意，以及可能出现的幻觉问题；

第三，可能会输出一些不太符合人类/社会偏好，政治不正确的回答

在生成环节，我们将重点介绍Self-Rag方案。

要覆盖所有上面提到的优化环节，需要较长的内容篇幅，因此风叔会分成几篇文章来写。接下来，我们先从整体上，看看一个最小化的RAG系统是如何实现的。

### 构建最小化的Naive Rag系统

RAG发展初期，其核心框架由索引、检索和生成构成，这种范式被称作Naive RAG。Naive Rag的原理非常简单，包括以下三个步骤：

索引：这一过程通常在离线状态下进行，将原始文档或数据进行清洗并分块，然后将分块后的知识通过embedding模型生成语义向量，并创建索引。

检索：对用户输入的Query问题，使用相同的embedding模型，计算Query嵌入和文档块嵌入之间的向量相似度，然后选择相似度最高的前N个文档块作为当前问题的增强上下文信息。

生成：将原始Query和相关文档合并为新的提示，然后由大型语言模型基于提供的信息回答问题。如果有历史对话信息，也可以合并到提示中，用于进行多轮对话。

下面，通过实际的源码，详细介绍如何构建一个最小化的Naive Rag系统。

#### 第一步建立索引

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081128406.jpg)

首先，我们导入一些示例Documents，以导入外部博客为例，我们直接使用WebBaseLoader从目标地址读取数据。

```python
import bs4
from langchain_community.document_loaders import WebBaseLoader
loader = WebBaseLoader(
    web_paths=("https://lilianweng.github.io/posts/2023-06-23-agent/",),
    bs_kwargs=dict(
        parse_only=bs4.SoupStrainer(
            class_=("post-content", "post-title", "post-header")
        )
    ),)
blog_docs = loader.load()
```

然后我们需要对文档进行分块。在这个例子中，我们先把流程跑通，采用最简单的文本分割器，尽量按照段落进行分割。

```python
# Split
from langchain.text_splitter import RecursiveCharacterTextSplitter
text_splitter = RecursiveCharacterTextSplitter.from_tiktoken_encoder(
    chunk_size=300, 
    chunk_overlap=50)
# Make splits
splits = text_splitter.split_documents(blog_docs)
```

接下来，我们需要将文本分割的结果存入向量数据库，默认使用了OpenAI的Embedding模型。

```python
# Index
from langchain_openai import OpenAIEmbeddings
from langchain_community.vectorstores import Chroma
vectorstore=Chroma.from_documents(documents=splits,embedding=OpenAIEmbeddings())
```

#### 第二步 检索

检索过程非常简单。首先构建检索器retriever，设置K=1，即只召回最相关的一个内容块；然后根据问题找到最相关的内容，存入docs

```python
retriever = vectorstore.as_retriever(search_kwargs={"k": 1})
docs=retriever.get_relevant_documents("What is Task Decomposition?")
```

#### 第三步 生成

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081230651.jpg)

生成环节，我们先定义Prompt。先跑通流程，我们定义一个最简单的Prompt

```python
from langchain_openai import ChatOpenAI
from langchain.prompts import ChatPromptTemplate
# Prompt
template = """Answer the question based only on the following context:{context}
Question: {question}"""
prompt=ChatPromptTemplate.from_template(template)
```

然后调用大模型生成最终回复，我们使用了gpt-3.5-turbo。我们先把temperature调到0，减少大模型输出的随机性。

```python
from langchain_core.output_parsers import StrOutputParser
from langchain_core.runnables import RunnablePassthrough
llm = ChatOpenAI(model_name="gpt-3.5-turbo", temperature=0)
rag_chain = (
    {"context": retriever, "question": RunnablePassthrough()}
    | prompt
    | llm
    | StrOutputParser())
rag_chain.invoke("What is Task Decomposition?")
```

到这里，一个最最简单的Rag系统就搭建完了，其原理非常简单易懂。“麻雀虽小，五脏俱全”，大家也可以拿这段代码自己做一些修改，比如读取pdf文件、word文档等等。

### 小结

经过上述流程，我们搭建了一个非常简单的Naive RAG系统，这个系统解析了一篇博客文章，然后接收用户提问，并使用博客的内容做增强生成。这是一个非常简单的框架，也很易于理解。

但是在实际应用中还有非常多需要优化的地方，包括Indexing（索引）、Query Translation（查询转换）、Routing（路由）、Query Construction（查询构建）、Retrival（检索）和Generation（生成），每个环节都有多种有效的优化方式。

## 优化数据索引的四种高级方法

在实际应用场景中，文档尺寸可能非常大，因此需要将长篇文档分割成多个文本块，以便更高效地处理和检索信息。

Indexing（索引）环节主要面临三个难题：

首先，内容表述不完整，内容块的语义信息容易受分割方式影响，致使在较长的语境中，重要信息被丢失或被掩盖。

其次，块相似性搜索不准确，随着数据量增多，检索中的噪声增大，导致频繁与错误数据匹配，使得检索系统脆弱且不可靠。

最后，参考轨迹不明晰，检索到的内容块可能来自任何文档，没有引用痕迹，可能出现来自多个不同文档的块，尽管语义相似，但包含的却是完全不同主题的内容。

下面，我们结合源代码，介绍Chunk optimization（块优化）、Multi-representation indexing（多层表达索引）、Specialized embeddings（特殊嵌入）和Hierachical Indexing（多级索引）这四种优化索引的高级方法。

### Chunk optimization（块优化）

在内容分块的时候，分块大小对索引结果会有很大的影响。较大的块能捕捉更多的上下文，但也会产生更多噪声，需要更长的处理时间和更高的成本；而较小的块噪声更小，但可能无法完整传达必要的上下文。

**第一种优化方式：固定大小重叠滑动窗口**

该方法根据字符数将文本划分为固定大小的块，实现简单。但是其局限性包括对上下文大小的控制不精确、存在切断单词或句子的风险以及缺乏语义考虑。适用于探索性分析，但不推荐用于需要深度语义理解的任务。

```python
text = "..." # your text
from langchain.text_splitter import CharacterTextSplitter
text_splitter = CharacterTextSplitter(
    chunk_size = 256,
    chunk_overlap  = 20)
docs = text_splitter.create_documents([text])
```

**第二种优化方式：递归感知**

一种结合固定大小滑动窗口和结构感知分割的混合方法。它试图平衡固定块大小和语言边界，提供精确的上下文控制。实现复杂度较高，存在块大小可变的风险，对于需要粒度和语义完整性的任务有效，但不推荐用于快速任务或结构划分不明确的任务。

```python
text = "..." # your text
from langchain.text_splitter import RecursiveCharacterTextSplitter
text_splitter = RecursiveCharacterTextSplitter(
    chunk_size = 256,
    chunk_overlap  = 20,
    separators = ["nn", "n"])
docs = text_splitter.create_documents([text])
```

**第三种优化方式：结构感知切分**

该方法考虑文本的自然结构，根据句子、段落、节或章对其进行划分。尊重语言边界可以保持语义完整性，但结构复杂性的变化会带来挑战。对于需要上下文和语义的任务有效，但不适用于缺乏明确结构划分的文本

```python
text = "..." # your text
docs = text.split(".")
```

**第四种优化方式：内容感知切分**

此方法侧重于内容类型和结构，尤其是在 Markdown、LaTeX 或 HTML 等结构化文档中。它确保内容类型不会在块内混合，从而保持完整性。挑战包括理解特定语法和不适用于非结构化文档。适用于结构化文档，但不推荐用于非结构化内容。以markdown为例

```python
from langchain.text_splitter import MarkdownTextSplitter
markdown_text = "..."
markdown_splitter = MarkdownTextSplitter(chunk_size=100, chunk_overlap=0)
docs = markdown_splitter.create_documents([markdown_text])
```

**第五种块优化方式：基于语义切分**

一种基于语义理解的复杂方法，通过检测主题的重大转变将文本划分为块。确保语义一致性，但需要高级 NLP 技术。对于需要语义上下文和主题连续性的任务有效，但不适合高主题重叠或简单的分块任务

```python
text = "..." # your text
from langchain.text_splitter import NLTKTextSplitter
text_splitter = NLTKTextSplitter()
docs = text_splitter.split_text(text)
```

### 多层表达索引

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081234106.jpg)

多层表达索引是一种构建多级索引的方法，在长上下文环境比较有用。

这种方法通过将原始数据生成 summary后，重新作为embedding再存到summary database中。检索的时候，首先通过summary database找到最相关的summary，再回溯到原始文档中去。

首先，我们使用 WebBaseLoader 加载两个网页的文档，在这个例子中，我们加载了 Lilian Weng 的两篇博客文章：

```python
from langchain_community.document_loaders import WebBaseLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
loader = WebBaseLoader("https://lilianweng.github.io/posts/2023-06-23-agent/")
docs = loader.load()
loader = WebBaseLoader("https://lilianweng.github.io/posts/2024-02-05-human-data-quality/")
docs.extend(loader.load())
```

模型使用 ChatOpenAI，设置为 gpt-3.5-turbo 版本，利用 chain.batch 批量处理文档，使用 max_concurrency 参数限制并发数。

```python
import uuid
from langchain_core.documents import Document
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI
chain = (
    {"doc": lambda x: x.page_content}
    | ChatPromptTemplate.from_template("Summarize the following document:nn{doc}")
    | ChatOpenAI(model="gpt-3.5-turbo",max_retries=0)
    | StrOutputParser())
summaries = chain.batch(docs, {"max_concurrency": 5})
```

我们引入了 InMemoryByteStore 和 Chroma 两个模块，分别用于存储原始文档和总结文档。InMemoryByteStore 是一个内存中的存储层，用于存储原始文档，而 Chroma 则是一个文档向量数据库，用于存储文档的向量表示。

```python
from langchain.storage import InMemoryByteStore
from langchain_openai import OpenAIEmbeddings
from langchain_community.vectorstores import Chroma
from langchain.retrievers.multi_vector import MultiVectorRetriever
#The vector store to use to index the child chunks
vectorstore = Chroma(collection_name="summaries",                     embedding_function=OpenAIEmbeddings())
#The storage layer for the parent documents
store = InMemoryByteStore()
```

MultiVectorRetriever 类帮助我们在一个统一的接口中管理文档和向量存储，使得检索过程更加高效。

```python
id_key = "doc_id"
#The retriever
retriever = MultiVectorRetriever(
    vectorstore=vectorstore,
    byte_store=store,
    id_key=id_key,)
doc_ids = [str(uuid.uuid4()) for _ in docs]
```

将总结文档添加到 Chroma 向量数据库中，同时在 InMemoryByteStore 中关联原始文档和 doc_id。

```python
summary_docs = [
    Document(page_content=s, metadata={id_key: doc_ids[i]})
    for i, s in enumerate(summaries)]#Add
retriever.vectorstore.add_documents(summary_docs)
retriever.docstore.mset(list(zip(doc_ids, docs)))
```

执行检索操作，对于给定的查询 query = “Memory in agents”，我们使用 vectorstore 进行相似性检索，k=1 表示只返回最相关的一个文档。然后使用 retriever 进行检索，n_results=1 表示只返回一个文档结果。

```python
query = "Memory in agents"
sub_docs=vectorstore.similarity_search(query,k=1)
#打印sub_docs[0]
retrieved_docs=retriever.get_relevant_documents(query,n_results=1)
#打印retrieved_docs[0].page_content[0:500]
```

### 特殊向量

特殊向量方法常用于多模态数据，比如图片数据，利用特殊的向量去做索引。

ColBERT是一种常用的特殊向量方法，它为段落中的每个标记生成一个受上下文影响的向量，同时也会为查询中的每个标记生成向量。然后，每个文档的得分是每个查询嵌入与任何文档嵌入的最大相似度之和。

可以使用RAGatouille工具来快速实现ColBERT，首先引入RAGatouille。

```python
from ragatouille import RAGPretrainedModel
RAG = RAGPretrainedModel.from_pretrained("colbert-ir/colbertv2.0")
```

然后我们获取文档数据，这里我们选择了使用wiki页面

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081236808.png)

最后，完成索引的构建，自动使用ColBERT方法完成索引。

```python
RAG.index(
    collection=[full_document],
    index_name="Miyazaki-123",
    max_document_length=180,
    split_documents=True,)
```

### 分层索引

分层索引，指的是带层级结构的去索引，比如可以先从关系数据库里索引找出对应的关系，然后再利用索引出的关系再进一步去搜寻basic数据库。前文介绍的多层表达索引也属于分层索引的一种。

还有一种更有效的分层索引方法叫做Raptor，Recursive Abstractive Processing for Tree-Organized Retrieval，该方法核心思想是将doc构建为一棵树，然后逐层递归的查询

RAPTOR 根据向量递归地对文本块进行聚类，并生成这些聚类的文本摘要，从而自下而上构建一棵树。聚集在一起的节点是兄弟节点；父节点包含该集群的文本摘要。这种结构使 RAPTOR 能够将代表不同级别文本的上下文块加载到 LLM 的上下文中，以便它能够有效且高效地回答不同层面的问题。

查询有两种方法，基于树遍历（tree traversal）和折叠树（collapsed tree）。遍历是从 RAPTOR 树的根层开始，然后逐层查询；折叠树就是全部平铺，用ANN库查询。

Raptor是一种非常高级和复杂的方法，源代码也相对比较复杂，这里就不贴出来了，只从整体上介绍一下Raptor的逻辑。大家可以通过上文介绍的方法来获取源码。

首先，我们使用LangChain 的 LCEL 文档作为输入数据，并对文档进行分块以适合我们的 LLM 上下文窗口，生成全局嵌入列表，并将维度减少到2来简化生成的聚类，并可视化。

然后，为每个Raptor步骤定义辅助函数，并构建树。这一段代码是整个Raptor中最复杂的一段，其主要做了以下事情：

- global_cluster_embeddings使用UAMP算法对所有的Embeddings进行全局降维，local_cluster_embeddings则使用UAMP算法进行局部降维。
- get_optimal_clusters函数使用高斯混合模型的贝叶斯信息准则 (BIC) 确定最佳聚类数。
- GMM_cluster函数使用基于概率阈值的高斯混合模型 (GMM) 进行聚类嵌入，返回包含聚类标签和确定的聚类数量的元组。
- Perform_clustering函数则对嵌入执行聚类，首先全局降低其维数，然后使用高斯混合模型进行聚类，最后在每个全局聚类内执行局部聚类。
- Embed_cluster_texts函数则用于嵌入文本列表并对其进行聚类，返回包含文本、其嵌入和聚类标签的 DataFrame。
- Embed_cluster_summarize_texts函数首先为文本生成嵌入，根据相似性对它们进行聚类，扩展聚类分配以便于处理，然后汇总每个聚类内的内容。
- recursive_embed_cluster_summarize函数递归地嵌入、聚类和汇总文本，直至指定级别或直到唯一聚类的数量变为 1，并在每个级别存储结果。

接下来，生成最终摘要，有两种方法：

1. 树遍历检索：树的遍历从树的根级开始，并根据向量嵌入的余弦相似度检索节点的前 k 个文档。因此，在每一级，它都会从子节点检索前 k 个文档。
2. 折叠树检索：折叠树检索是一种更简单的方法。它将所有树折叠成一层，并根据查询向量的余弦相似度检索节点，直到达到阈值数量的标记。

接下来，我们将提取数据框文本、聚类文本、最终摘要文本，并将它们组合起来，创建一个包含根文档和摘要的大型文本列表。然后将该文本存储到向量存储中，构建索引，并创建查询引擎

最后，用一个实际问题进行检验，可以看到实际的回复内容还是比较准确的。

```python
# Question
response =rag_chain.invoke("What is LCEL?")
print(str(response))
############# Response ######################################
LangChain Expression Language (LCEL) is a declarative way to easily compose chains together in LangChain. It was designed from day 1 to support putting prototypes in production with no code changes, from the simplest "prompt + LLM" chain to complex chains with hundreds of steps. Some reasons why one might want to use LCEL include streaming support (allowing for the best possible time-to-first-token), async support (enabling use in both synchronous and asynchronous APIs), optimized parallel execution (automatically executing parallel steps with the smallest possible latency), retries and fallbacks (a great way to make chains more reliable at scale), access to intermediate results (useful for letting end-users know something is happening or debugging), input and output schemas (providing Pydantic and JSONSchema schemas inferred from chain structure for validation), seamless LangSmith tracing integration (maximum observability and debuggability), and seamless LangServe deployment integration (easy chain deployment).
```

## 优化查询转换的五种高级方法

Query Translation（查询转换）主要处理用户的输入。在Naive Rag中，往往直接使用原始Query进行检索，这样会存在三个问题：

第一，原始query的措辞不当，尤其是涉及到很多专业词汇时，query可能存在概念使用错误的问题；

第二，往往知识库内的数据无法直接回答，需要组合知识才能找到答案；

第三，当query涉及比较多的细节时，由于检索效率有限，大模型往往无法进行高质量的回答。

下面，我们结合源代码，在查询转换环节实现Multi-query（多查询）、Rag-Fusion、Decomposition（查询分解）、Stepback和HYDE这五种优化方案。

### Multi-query（多查询）

Multi-query是指借助提示工程通过大型语言模型来扩展查询，将原始Query扩展成多个相似的Query，然后并行执行，是一种非常简单直观的优化方案，如下图所示。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081239314.jpg)

通过构建Prompt，告诉大模型在收到Query之后，生成5个相似的扩展问题。后续的步骤和Naive Rag一样，对所有Query进行检索和生成。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081239568.png)

### Rag-Fusion

Rag-Fusion也是Multi-Query的一种，相比Multi-query只是多了一个步骤，即在对多个query进行检索之后，应用倒数排名融合算法，根据文档在多个查询中的相关性重新排列文档，生成最终输出。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081240626.jpg)

以下代码中的reciprocal_rank_fusion，就是rag-fusion多出来的一步。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081240532.png)

### Decomposition（问题分解）

通过分解和规划复杂问题，将原始Query分解成为多个子问题。比如原始Query的问题是“请详细且全面的介绍Rag“，这个问题就可以拆解为几个子问题，“Rag的概念是什么？”，“为什么会产生Rag？”，“Rag的原理是怎样的？”，“Rag有哪些使用场景”等等。

首先，构建Prompt，告诉大模型要将输入的问题分解成3个子问题。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081240309.png)

在最终回答子问题的时候有两种方式。

第一种是递归回答，即先接收一个子问题，先回答这个子问题并接受这个答案，并用它来帮助回答第二个子问题。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081241135.jpg)

给出prompt：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081241076.png)

下面是递归回答的主逻辑，生成最终回答：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081241990.png)

第二种方式是独立回答，然后再把所有的这些答案串联起来，得出最终答案。这更适合于一组有几个独立的问题，问题之间的答案不互相依赖的情况。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081241610.jpg)

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081242388.png)

### Step-back（Query后退）

如果原始查询太复杂或返回的信息太广泛，我们可以选择生成一个抽象层次更高的“退后”问题，与原始问题一起用于检索，以增加返回结果的数量。

例如，对于问题“勒布朗詹姆斯在2005年至2010年在哪些球队？”这个问题因为有时间范围的详细限制，比较难直接解决，可以提出一个后退问题，“勒布朗詹姆斯的职业生涯是怎么样的？”，从这个回答的召回结果中再检索上一个问题的答案。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081242289.jpg)

先给大模型提供一些step-back的示例：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081243056.png)

然后对输入问题进行step-back

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081243562.png)

结合prompt，生成最终回答

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081243834.png)

### HYDE

全称是Hypothetical Document Embeddings，即用LLM生成一个“假设”答案，将其和问题一起进行检索。

HyDE的核心思想是接收用户提问后，先让LLM在没有外部知识的情况下生成一个假设性的回复。然后，将这个假设性回复和原始查询一起用于向量检索。假设回复可能包含虚假信息，但蕴含着LLM认为相关的信息和文档模式，有助于在知识库中寻找类似的文档。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081243118.png)

## 优化路由环节

路由的作用，是为每个Query选择最合适的处理管道，以及依据来自模型的输入或补充的元数据，来确定将启用哪些模块。比如当用户的输入问题涉及到跨文档检索、或者对于复杂文档构建了多级索引时，就需要使用路由机制。

下面，我们结合源代码，介绍一下Logical routing（基于逻辑的路由）和Sematic Routing（基于语义的路由）两种方案。

### Logical routing（基于逻辑的路由）

基于逻辑的路由，其原理非常简单。大模型接收到问题之后，根据决策步骤，去选择正确的索引数据库，比如图数据库、向量数据库等等，如下图所示。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081245425.jpg)

其使用函数调用（function calling）来产生结构化输出。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081245526.jpg)

下面我们结合源代码来分析一下Logical Routing的流程：

- 首先我们定义了三种文档，pytion、js、golang
- 然后通过prompt告诉大模型，需要根据所涉及的编程语言，将用户问题路由到适当的数据源
- 定义Router

```python
# Data model
class RouteQuery(BaseModel):
    """Route a user query to the most relevant datasource."""
    datasource: Literal["python_docs", "js_docs", "golang_docs"] = Field(
        ...,
        description="Given a user question choose which datasource would be most relevant for answering their question",
    )
```

```python
# LLM with function call 
llm = ChatOpenAI(model="gpt-3.5-turbo-0125", temperature=0)
structured_llm = llm.with_structured_output(RouteQuery)
```

```python
# Prompt 
system = """You are an expert at routing a user question to the appropriate data source.
Based on the programming language the question is referring to, route it to the relevant data source."""
prompt = ChatPromptTemplate.from_messages(
    [
        ("system", system),
        ("human", "{question}"),
    ])
# Define router 
router = prompt | structured_llm
```



接着给出了一个使用示例，用户提问后，路由器根据问题的内容判断出数据源为 python_docs，并返回了相应的结果。

```python
question = """Why doesn't the following code work:
from langchain_core.prompts import ChatPromptTemplate
prompt = ChatPromptTemplate.from_messages(["human", "speak in {language}"])
prompt.invoke("french")
"""
result = router.invoke({"question": question})
result.datasource
def choose_route(result):
    if "python_docs" in result.datasource.lower():
        ### Logic here 
        return "chain for python_docs"
    elif "js_docs" in result.datasource.lower():
        ### Logic here 
        return "chain for js_docs"
    else:
        ### Logic here 
        return "golang_docs"
```

```python
from langchain_core.runnables import RunnableLambda
full_chain = router | RunnableLambda(choose_route)
full_chain.invoke({"question": question})
```



### Sematicrouting（基于语义的路由）

基于语义的路由，其原理也非常简单，大模型根据query的语义相似度，去自动配置不同的prompt。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202506081247432.jpg)

我们先定义两种不同的Prompt，一个让大模型扮演物理专家，一个让大模型扮演数学专家，并将其转为嵌入向量。

```python
# Two prompts
physics_template = """You are a very smart physics professor. 
You are great at answering questions about physics in a concise and easy to understand manner. 
When you don't know the answer to a question you admit that you don't know.
Here is a question:
{query}"""
```



```python
math_template = """You are a very good mathematician. You are great at answering math questions. 
You are so good because you are able to break down hard problems into their component parts, 
answer the component parts, and then put them together to answer the broader question.
Here is a question:
{query}"""
```

```python
embeddings = OpenAIEmbeddings()
prompt_templates = [physics_template, math_template]
prompt_embeddings = embeddings.embed_documents(prompt_templates)
```

然后计算query embedding和prompt embedding的向量相似度

```python
# Route question to prompt 
def prompt_router(input):
    # Embed question
    query_embedding = embeddings.embed_query(input["query"])
    # Compute similarity
    similarity = cosine_similarity([query_embedding], prompt_embeddings)[0]
    most_similar = prompt_templates[similarity.argmax()]
    # Chosen prompt 
    print("Using MATH" if most_similar == math_template else "Using PHYSICS")
    return PromptTemplate.from_template(most_similar)
```

```python
chain = (
    {"query": RunnablePassthrough()}
    | RunnableLambda(prompt_router)
    | ChatOpenAI()
    | StrOutputParser()
)
print(chain.invoke("What's a black hole"))
```

在上述案例中，最终的输出会使用物理专家的Prompt。

到这里，两种常用的路由策略就介绍完了。当然，我们也可以自主构建更复杂的路由策略，比如构建专门的分类器、打分器等等，这里就不详细展开了。







































