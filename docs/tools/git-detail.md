---
title: Git指南：从入门到精通
category: 开发工具
icon: git
tags:
  - Git
head:
  - - meta
    - name: keywords
      content: Git,Git命令,Git代码管理规范
  - - meta
    - name: description
      content: 全网最全的Git知识点总结，让天下没有难学的八股文！
---







## 代码提交和同步命令

流程图如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405032226465.png)



- 第零步: 工作区与仓库保持一致
- 第一步: 文件增删改，变为已修改状态
- 第二步: git add ，变为已暂存状态

```bash
$ git status
$ git add --all # 当前项目下的所有更改
$ git add .  # 当前目录下的所有更改
$ git add xx/xx.py xx/xx2.py  # 添加某几个文件
```

- 第三步: git commit，变为已提交状态

```bash
$ git commit -m "<这里写commit的描述>"
```

- 第四步: git push，变为已推送状态

```bash
$ git push -u origin master # 第一次需要关联上
$ git push # 之后再推送就不用指明应该推送的远程分支了
$ git branch # 可以查看本地仓库的分支
$ git branch -a # 可以查看本地仓库和本地远程仓库(远程仓库的本地镜像)的所有分支
```



一般来说，在某个分支下，最常用的操作如下：

```bash
$ git status
$ git add -a
$ git status
$ git commit -m 'xxx'
$ git pull --rebase
$ git push origin xxbranch
```



## 代码撤销和撤销同步命令

流程图如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405032227058.png)

### 已修改，但未暂存

```bash
$ git diff # 列出所有的修改
$ git diff xx/xx.py xx/xx2.py # 列出某(几)个文件的修改

$ git checkout # 撤销项目下所有的修改
$ git checkout . # 撤销当前文件夹下所有的修改
$ git checkout xx/xx.py xx/xx2.py # 撤销某几个文件的修改
$ git clean -f # untracked状态，撤销新增的文件
$ git clean -df # untracked状态，撤销新增的文件和文件夹

# Untracked files:
#  (use "git add <file>..." to include in what will be committed)
#
#	xxx.py
```

>`git checkout` 与后文的切换分支命令几乎一致，事实上，`checkout` 作为单个命令有点超载（它承载了很多独立的功能），因此在 Git 2.23 版本中，引入了一个名为 `git switch` 的新命令，最终会取代 `git checkout`

### 已暂存，未提交

> 这个时候已经执行过git add，但未执行git commit，但是用git diff已经看不到任何修改。 因为git diff检查的是工作区与暂存区之间的差异。

```bash
$ git diff --cached # 这个命令显示暂存区和本地仓库的差异

$ git reset # 暂存区的修改恢复到工作区
$ git reset --soft # 与git reset等价，回到已修改状态，修改的内容仍然在工作区中
$ git reset --hard # 回到未修改状态，清空暂存区和工作区
```

> git reset --hard 操作等价于 git reset 和 git checkout 2步操作

### 已提交，未推送

> 执行完commit之后，会在仓库中生成一个版本号(hash值)，标志这次提交。之后任何时候，都可以借助这个hash值回退到这次提交。

```bash
$ git diff <branch-name1> <branch-name2> # 比较2个分支之间的差异
$ git diff master origin/master # 查看本地仓库与本地远程仓库的差异

$ git reset --hard origin/master # 回退与本地远程仓库一致
$ git reset --hard HEAD^ # 回退到本地仓库上一个版本
$ git reset --hard <hash code> # 回退到任意版本
$ git reset --soft/git reset # 回退且回到已修改状态，修改仍保留在工作区中。
```

### 已推送到远程

```java
$ git push -f orgin master # 强制覆盖远程分支
$ git push -f # 如果之前已经用 -u 关联过，则可省略分支名
```

> 慎用，一般情况下，本地分支比远程要新，所以可以直接推送到远程，但有时推送到远程后发现有问题，进行了版本回退，旧版本或者分叉版本推送到远程，需要添加 -f参数，表示强制覆盖。



## 其它常用命令

### 关联远程仓库

- 如果还没有Git仓库，需要初始化

```bash
$ git init
```

- 如果想关联远程仓库

```bash
$ git remote add <name> <git-repo-url>
# 例如 git remote add origin https://github.com/xxxxxx # origin是远程仓库的名称
```

- 如果想关联多个远程仓库

```bash
$ git remote add <name> <another-git-repo-url>
# 例如 git remote add coding https://coding.net/xxxxxx
```

- 查看关联了哪些仓库或者地址

```bash
$ git remote -v
# origin https://github.com/Seven-97/SevenBlog.git (fetch)
# origin https://github.com/Seven-97/SevenBlog.git (push)
```

- 如果远程有仓库，需要clone到本地

```bash
$ git clone <git-repo-url>
# 关联的远程仓库将被命名为origin，这是默认的。
```

- 如果想把别人仓库的地址改为自己的

```bash
$ git remote set-url origin <your-git-url>
```



### 配置自己的Git

- 查看当前的配置

```bash
$ git config --list
```

- 配置自己的名字

```bash
$ git config --global user.name "<name>"
#  --global为可选参数，该参数表示配置全局信息
```

- 希望别人看到你的commit可以联系到你

```bash
$ git config --global user.email "<email address>"
```

- 有些命令很长，能不能简化一下

```bash
$ git config --global alias.logg "log --graph --decorate --abbrev-commit --all"
# 之后就可以开心地使用 git log了
```

​	

### 切换分支

> 新建仓库后，默认生成了master分支

- 新建分支并切换

```bash
$ git checkout -b <new-branch-name>
# 例如 git checkout -b dev
# 如果仅新建，不切换，则去掉参数 -b
```

- 查看当前有哪些分支

```bash
$ git branch
# * dev
#   master # 标*号的代表当前所在的分支
```

- 查看当前本地&远程有哪些分支

```bash
$ git branch -a
# * dev
#   master
#   remotes/origin/master
```

- 切换到现有的分支

```bash
$ git checkout master
```



### 更新本地/远端代码 push/poll

- 将本地master分支推送到远程去

```bash
$ git push origin master
# 你可以使用git push -u origin master将本地分支与远程分支关联，之后仅需要使用git push即可。
```

- 远程分支更新了，需要更新代码

```bash
$ git pull origin <branch-name>
# 之前如果push时使用过-u，那么就可以省略为git pull
```

- 本地有修改，能不能先git pull

```shell
$ git stash # 工作区修改暂存
$ git pull  # 更新分支
$ git stash pop # 暂存修改恢复到工作区
```



`git pull` 实际上包含了两个操作：fetch和merge。当使用`git pull`命令时，Git会自动下载最新代码，并尝试将最新代码合并到当前分支。`git fetch`命令只是从远程库下载最新代码，但并不自动合并到本地分支。

- 如果我们希望自动合并最新代码到当前分支，并且不需要查看最新代码的变动，可以使用`git pull`命令。
- 如果我们只想下载最新代码到本地，并需要查看最新代码的变动后才决定是否进行合并，可以使用`git fetch`命令。



### 撤销操作 reset

- 恢复**暂存区文件到工作区**

```bash
$ git checkout <file-name>
```

- 恢复**暂存区的所有文件到工作区**

```bash
$ git checkout .
```

- 重置暂存区的某文件，与上一次commit保持一致，但**工作区不变**

```bash
$ git reset <file-name>
```

- 重置**暂存区与工作区**，与上一次commit保持一致

```bash
$ git reset --hard <file-name>
# 如果是回退版本(commit)，那么file，变成commit的hash码就好了。
```

- 去掉某个commit

```bash
$ git revert <commit-hash>
# 实质是新建了一个与原来完全相反的commit，抵消了原来commit的效果
```

- reset回退错误恢复

```bash
$ git reflog #查看最近操作记录
$ git reset --hard HEAD{5} #恢复到前五笔操作
$ git pull origin backend-log #再次拉取代码
```



### 版本回退与前进 status、log、reflog

- 查看当前状态

```bash
$ git status
```



- 查看历史版本

```bash
$ git log
commit 9a3d34be1e26563e198ee6aea72d32ad68b607d0 (HEAD -> main, origin/main, origin/HEAD)
Author: seven <415849169@qq.com>
Date:   Tue Jul 9 19:10:11 2024 +0800

    更新

commit 47c32de67a28df58ea3bd0b88137f1bd19a12969
Author: Seven <415849169@qq.com>
Date:   Tue Jul 9 00:30:11 2024 +0800

    更新

commit 294814faf55fc4f01e3b0a28c96b5acdf1348333
Author: Seven <415849169@qq.com>
Date:   Tue Jul 9 00:22:15 2024 +0800

    更新内容

commit 15026d42c4d3a6093bde4578484e2bb64824b9e8
Author: Seven <415849169@qq.com>
```

- 这样的log不好看，可以试试以下命令

```bash
$ git log --graph --decorate --abbrev-commit --all
* commit 9a3d34b (HEAD -> main, origin/main, origin/HEAD)
| Author: seven <415849169@qq.com>
| Date:   Tue Jul 9 19:10:11 2024 +0800
|
|     更新
|
* commit 47c32de
| Author: Seven <415849169@qq.com>
| Date:   Tue Jul 9 00:30:11 2024 +0800
|
|     更新
|
* commit 294814f
| Author: Seven <415849169@qq.com>
| Date:   Tue Jul 9 00:22:15 2024 +0800
|
|     更新内容
|
* commit 15026d4
| Author: Seven <415849169@qq.com>
| Date:   Tue Jul 9 00:16:23 2024 +0800
```



- 检出到任意版本

```bash
$ git checkout a5d88ea
# hash码很长，通常6-7位就够了
```

- 远程仓库的版本很新，但是还是想用老版本覆盖

```bash
$ git push origin master --force
# 或者 git push -f origin master
```

- 觉得commit太多了? 可以使用rebase将多个commit合并为1个

```bash
$ git rebase -i HEAD~4
# 这个命令，将最近4个commit合并为1个，HEAD代表当前版本。将进入VIM界面，你可以修改提交信息。推送到远程分支的commit，不建议这样做，多人合作时，通常不建议修改历史。
```

- 想回退到某一个版本

```bash
$ git reset --hard <hash>
# 例如 git reset --hard a3hd73r
# --hard代表丢弃工作区的修改，让工作区与版本代码一模一样，与之对应，--soft参数代表保留工作区的修改。
```

- 想回退到上一个版本，有没有简便方法?

```bash
$ git reset --hard HEAD^
```

- 回退到上上个版本呢?

```bash
$ git reset --hard HEAD^^
# HEAD^^可以换作具体版本hash值。
```



- 回退错了，想到下一个版本

```bash
$ git reflog
# 这个命令保留了最近执行的操作及所处的版本，每条命令前的hash值，则是对应版本的hash值。使用上述的git checkout 或者 git reset命令 则可以检出或回退到对应版本。
```

- 刚才commit信息写错了，修改上一次提交的commit信息

```bash
$ git commit --amend
```





### 分支合并 merge

每个分支上都有各自独有的提交，这意味着没有一个分支包含了修改的所有内容。因此通过合并分支来解决这个问题。

git merge 用来做分支合并，将其他分支中的内容合并到当前分支中。比如分支结构如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405032318912.png)

#### 其它分支合并到master分支

当前分支是master

```shell
git checkout master
```



把issueFix中的内容Merge进来：

```shell
git merge issueFix
```



如果没有冲突的话，merge完成。有冲突的话，git会提示那个文件中有冲突，比如有如下冲突：

```shell
<<<<<<< HEAD:test.c
printf (“test1″);
=======
printf (“test2″);
\>>>>>>> issueFix:test.c
```

可以看到 ======= 隔开的上半部分，是 HEAD（即 master 分支，在运行 merge 命令时检出的分支）中的内容，下半部分是在 issueFix 分支中的内容。

解决冲突的办法无非是二者选其一或者由你亲自整合到一起。比如你可以通过把这段内容替换为下面这样来解决：

```shell
printf (“test2″);
```

这个解决方案各采纳了两个分支中的一部分内容，而且删除了 <<<<<<<，=======，和>>>>>>> 这些行。在解决了所有文件里的所有冲突后，运行 git add 将把它们标记为已解决（resolved）。因为一旦暂存，就表示冲突已经解决。

合并后的分支图如下：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405032320674.png)
注意，这次合并的实现，由于当前 master 分支所指向的 commit (C4)并非想要并入分支（issueFix）的直接祖先，Git 不得不进行一些处理。就此例而言，Git 会用两个分支的末端（C4 和 C5）和它们的共同祖先（C2）进行一次简单的三方合并。对三方合并的结果作一新的快照，并自动创建一个指向它的 commit（C6)

退出合并工具以后，Git 会询问你合并是否成功。如果回答是，它会为你把相关文件暂存起来，以表明状态为已解决。然后可以用 git commit 来完成这次合并提交。



因此，多人协作的工作模式通常是这样：

1. 首先，可以试图用git push origin 推送自己的修改；
2. 如果推送失败，则因为远程分支比你的本地更新，需要先用git pull试图合并；
3. 如果合并有冲突，则解决冲突，并在本地提交；
4. 没有冲突或者解决掉冲突后，再用git push origin 推送就能成功！
5. 如果git pull提示no tracking information，则说明本地分支和远程分支的链接关系没有创建，用命令git branch --set-upstream-to origin/。



#### master分支合并到当前分支

以下是合并master分支到你的当前分支的基本步骤：

1. 确保在正确的分支上：首先，确保当前所在的分支是想要合并master分支到的地方。例如，如果想要将master合并到feature-branch，需要先切换到feature-branch：
```shell
git checkout feature-branch
```

2. 更新分支：在合并之前，最好先更新分支，以确保它是最新的。可以通过拉取master分支的最新更改来实现这一点：

```shell
git pull origin master
```

   这会从远程仓库的master分支拉取最新的更改，并尝试将它们合并到你的当前分支。如果master分支有新的提交，这可能会导致合并冲突。

3. 合并master分支：一旦分支是最新的，可以开始合并过程。使用git merge命令来合并master分支：

```shell
git merge master
```

   这会将master分支的更改合并到当前分支。如果两个分支之间没有冲突，合并将自动完成。如果有冲突，Git会告诉你哪些文件有冲突，此时就需要手动解决这些冲突。

4. 解决冲突（如果有）：如果合并过程中出现冲突，Git会暂停合并并告诉你哪些文件需要手动解决冲突。你需要打开这些文件，找到并解决冲突。解决冲突后，使用以下命令标记冲突已解决：
```shell
git add <冲突文件>
```

   然后继续合并：
```shell
git commit
```

   这将创建一个新的合并提交，包含所有解决的冲突。

5. 推送更改：一旦合并完成并且所有冲突都已解决，你可以将更改推送到远程仓库：
```shell
git push origin feature-branch
```

   确保替换feature-branch为你的分支名称。



#### 注意事项

- 在合并之前，始终确保你的分支是最新的，以避免不必要的冲突。
- 如果你在合并过程中遇到问题，可以使用`git merge --abort`来中止合并并返回到合并前的状态。
- 使用git log或gitk可以帮助理解分支的历史和可能的冲突点。



## Git代码管理规范

### 分支命名

#### master 分支

master 为主分支，也是用于部署生产环境的分支，需要确保master分支稳定性。master 分支一般由 release 以及 hotfix 分支合并，任何时间都不能直接修改代码。

#### develop 分支

develop 为开发环境分支，始终保持最新完成以及bug修复后的代码，用于前后端联调。一般开发的新功能时，feature分支都是基于develop分支创建的。

#### feature 分支

开发新功能时，以develop为基础创建feature分支。

分支命名时以 `feature/` 开头，后面可以加上开发的功能模块， 命名示例：`feature/user_module`、`feature/cart_module`

#### test分支

test为测试环境分支，外部用户无法访问，专门给测试人员使用，版本相对稳定。

#### release分支

release 为预上线分支（预发布分支），UAT测试阶段使用。一般由 test 或 hotfix 分支合并，不建议直接在 release 分支上直接修改代码。

#### hotfix 分支

线上出现紧急问题时，需要及时修复，以master分支为基线，创建hotfix分支。修复完成后，需要合并到 master 分支和 develop 分支。

分支命名以`hotfix/` 开头的为修复分支，它的命名规则与 feature 分支类似。

### 分支与环境对应关系

在系统开发过程中常用的环境：

- DEV 环境（Development environment）：用于开发者调试使用
- FAT环境（Feature Acceptance Test environment）：功能验收测试环境，用于测试环境下的软件测试者测试使用
- UAT环境 （User Acceptance Test environment）：用户验收测试环境，用于生产环境下的软件测试者测试使用
- PRO 环境（Production environment）：生产环境

对应关系：

| 分支    | 功能                      | 环境 | 可访问 |
| :------ | :------------------------ | :--- | :----- |
| master  | 主分支，稳定版本          | PRO  | 是     |
| develop | 开发分支，最新版本        | DEV  | 是     |
| feature | 开发分支，实现新特性      |      | 否     |
| test    | 测试分支，功能测试        | FAT  | 是     |
| release | 预上线分支，发布新版本    | UAT  | 是     |
| hotfix  | 紧急修复分支，修复线上bug |      | 否     |

#### 分支合并流程规范

业界常见的两大主分支（master、develop）、三个辅助分支（feature、release、hotfix）的生命周期：

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405032242353.webp)

以上生命周期仅作参考，不同开发团队可能有不同的规范，可自行灵活定义。



### Git Commit Message规范

Git commit message规范指提交代码时编写的规范注释，编写良好的Commit messages可以达到3个重要的目的：

- 加快代码review的流程
- 帮助我们编写良好的版本发布日志
- 让之后的维护者了解代码里出现特定变化和feature被添加的原因

#### Angular Git Commit Guidelines

业界应用的比较广泛的是Angular Git Commit Guidelines：

```
<type>(<scope>): <subject>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

- type：提交类型
- scope：可选项，本次 commit 波及的范围
- subject：简明扼要的阐述下本次 commit 的主旨，在`Angular Git Commit Guidelines`中强调了三点。使用祈使句，首字母不要大写，结尾无需添加标点
- body: 同样使用祈使句，在主体内容中我们需要把本次 commit 详细的描述一下，比如此次变更的动机
- footer: 描述下与之关联的 issue 或 break change

#### 简易版

项目中实际可以采用简易版规范：

```
<type>(<scope>):<subject>
```

#### type规范

`Angular Git Commit Guidelines`中推荐的type类型如下：

- feat: 新增功能
- fix: 修复bug
- docs: 仅文档更改
- style: 不影响代码含义的更改（空白、格式设置、缺失 分号等）
- refactor: 既不修复bug也不添加特性的代码更改
- perf: 改进性能的代码更改
- test: 添加缺少的测试或更正现有测试
- chore: 对构建过程或辅助工具和库（如文档）的更改

除此之外，还有一些常用的类型：

- delete：删除功能或文件
- modify：修改功能
- build：改变构建流程，新增依赖库、工具等（例如webpack、gulp、npm修改）
- test：测试用例的新增、修改
- ci：自动化流程配置修改
- revert：回滚到上一个版本

#### 单次提交注意事项

- 提交问题必须为同一类别
- 提交问题不要超过3个
- 提交的commit发现不符合规范，`git commit --amend -m "新的提交信息"`或 `git reset --hard HEAD` 重新提交一次

### 配置.gitignore文件

`.gitignore`是一份用于忽略不必提交的文件的列表，项目中可以根据实际需求统一`.gitignore`文件，减少不必要的文件提交和冲突，净化代码库环境。

通用文件示例：

```
HELP.md
target/
!.mvn/wrapper/maven-wrapper.jar
!**/src/main/**/target/
!**/src/test/**/target/

### STS ###
.apt_generated
.classpath
.factorypath
.project
.settings
.springBeans
.sts4-cache

### IntelliJ IDEA ###
.idea
*.iws
*.iml
*.ipr

### NetBeans ###
/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.nb-gradle/
build/
!**/src/main/**/build/
!**/src/test/**/build/

### VS Code ###
.vscode/

# Log file
*.log
/logs*

# BlueJ files
*.ctxt

# Mobile Tools for Java (J2ME)
.mtj.tmp/

# Package Files #
*.jar
*.war
*.ear
*.zip
*.tar.gz
*.rar
*.cmd
```

### 其他

此外，还有一些其他建议：

- master 分支的每一次更新，都建议打 tag 添加标签，通常为对应版本号，便于管理
- feature分支、hotfix分支在合并后可以删除，避免分支过多管理混乱
- 每次 pull 代码前，提交本地代码到本地库中，否则可能回出现合并代码出错，导致代码丢失





## 学习书籍及网站

- 推荐书籍：

  - [Git Pro2英文Github仓库](https://github.com/progit/progit2)

  - [Git Pro2中文Gitbook](https://bingohuang.gitbooks.io/progit2/content/01-introduction/sections/about-version-control.html)

  - [Git Pro2对应的中文Markdown版本的仓库地址](https://github.com/bingohuang/progit2-gitbook)

  - [Git Pro中文阅读](http://git.oschina.net/progit/index.html)

- 在线学习
  - git在线学习网站： https://learngitbranching.js.org/

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405032218260.png)



<!-- @include: @article-footer.snippet.md -->     