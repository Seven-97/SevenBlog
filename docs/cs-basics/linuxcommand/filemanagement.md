---
title: 文件管理
category: 计算机基础
tag:
  - Linux
---



## MV命令 - 移动或改名文件

mv命令来自英文单词move的缩写，中文译为“移动”，其功能与英文含义相同，能够对文件进行剪切和重命名操作。这是一个被高频使用的文件管理命令，需要留意它与复制命令的区别。cp命令是用于文件的复制操作，文件个数是增加的，而mv则为剪切操作，也就是对文件进行移动（搬家）操作，文件位置发生变化，但总个数并无增‍加。

 在同一个目录内对文件进行剪切的操作，实际上应理解成重命名操作。



### 语法格式

```shell
mv 参数 源文件名 目标文件名 
```



常用参数：

- **-b**: 当目标文件或目录存在时，在执行覆盖前，会为其创建一个备份。

- **-i**: 如果指定移动的源目录或文件与目标的目录或文件同名，则会先询问是否覆盖旧文件，输入 y 表示直接覆盖，输入 n 表示取消该操作。
- **-f**: 如果指定移动的源目录或文件与目标的目录或文件同名，不会询问，直接覆盖旧文件。
- **-n**: 不要覆盖任何已存在的文件或目录。
- **-u**：当源文件比目标文件新或者目标文件不存在时，才执行移动操作。



### 实例

将文件 aaa 改名为 bbb :

```shell
mv aaa bbb
```



将 info 目录放入 logs 目录中。注意，如果 logs 目录不存在，则该命令将 info 改名为 logs。

```shell
mv info/ logs 
```



将 **/usr/runoob** 下的所有文件和目录移到当前目录下：

```shell
mv /usr/runoob/*  . 
```



## ls命令 - 显示目录中文件及其属性信息

ls命令来自英文单词list的缩写，中文译为“列出”，其功能是显示目录中的文件及其属性信息，是最常使用的Linux命令之‍一。 

默认不添加任何参数的情况下，ls命令会列出当前工作目录中的文件信息，常与cd或pwd命令搭配使用，十分方便。带上参数后，我们可以做更多的事情。作为最基础、最频繁使用的命令，有必要仔细了解其常用功‍能。


### 语法格式

```shell
ls 参数 文件名
```



常用参数 :

- -a：显示所有文件及目录 (**.** 开头的隐藏文件也会列出)
- -d ：只列出目录（不递归列出目录内的文件）。
- -l ：以长格式显示文件和目录信息，包括权限、所有者、大小、创建时间等。
- -r ：倒序显示文件和目录。
- -t ：将按照修改时间排序，最新的文件在最前面。
- -A ：同 -a ，但不列出 "." (目前目录) 及 ".." (父目录)
- -F ：在列出的文件名称后加一符号；例如可执行档则加 "*", 目录则加 "/"
- -R ：递归显示目录中的所有文件和子目录。



### 实例

列出根目录(\)下的所有目录：

```shell
# ls /
bin               dev   lib         media  net   root     srv  upload  www
boot              etc   lib64       misc   opt   sbin     sys  usr
home  lost+found  mnt    proc  selinux  tmp  var
```



将 /bin 目录以下所有目录及文件详细资料列出:

```shell
ls -lR /bin
```



当文件名包含空格、特殊字符或者开始字符为破折号时，可以使用反斜杠（\）进行转义，或者使用引号将文件名括起来。例如：

```shell
ls "my file.txt"    # 列出文件名为"my file.txt"的文件
ls my\ file.txt     # 列出文件名为"my file.txt"的文件
ls -- -filename     # 列出文件名为"-filename"的文件
```



ls 命令还可以使用通配符进行模式匹配，例如 ***** 表示匹配任意字符，**?** 表示匹配一个字符，**[...]** 表示匹配指定范围内的字符。例如：

```shell
ls *.txt         # 列出所有扩展名为.txt的文件
ls file?.txt     # 列出文件名为file?.txt的文件，其中?表示任意一个字符
ls [abc]*.txt    # 列出以a、b或c开头、扩展名为.txt的文件
```



列出目前工作目录下所有名称是 s 开头的文件，越新的排越后面:

```shell
ls -ltr s*
```



在使用 **ls -l** 命令时，第一列的字符表示文件或目录的类型和权限。其中第一个字符表示文件类型，例如：

- \- 表示普通文件
- d 表示目录
- l 表示符号链接
- c 表示字符设备文件
- b 表示块设备文件
- s 表示套接字文件
- p 表示管道文件

在使用 **ls -l** 命令时，第一列的其余 9 个字符表示文件或目录的访问权限，分别对应三个字符一组的 **rwx** 权限。例如：

- r 表示读取权限
- w 表示写入权限
- x 表示执行权限
- \- 表示没有对应权限

前三个字符表示所有者的权限，中间三个字符表示所属组的权限，后三个字符表示其他用户的权限。例如：

```shell
-rw-r--r-- 1 user group 4096 Feb 21 12:00 file.txt
```

表示文件名为file.txt的文件，所有者具有读写权限，所属组和其他用户只有读取权限。



## cp命令 - 复制文件或目录

cp命令来自英文单词copy的缩写，中文译为“复制”，其功能是复制文件或目录。cp命令能够将一个或多个文件或目录复制到指定位置，亦常用于文件的备份工作。-r参数用于递归操作，复制目录时若忘记添加则会直接报错；-f参数则用于当目标文件已存在时会直接覆盖而不再询问。这两个参数尤为常用。 



### 语法格式：

```shell
cp 参数 源文件名 目标文件名
```



常用参数：

- -a：此选项通常在复制目录时使用，它保留链接、文件属性，并复制目录下的所有内容。其作用等于 dpR 参数组合。
- -d：复制时保留链接。这里所说的链接相当于 Windows 系统中的快捷方式。
- `-r` 或 `--recursive`：用于复制目录及其所有的子目录和文件，如果要复制目录，需要使用该选项。
- `-i` 或 `--interactive`：在复制前提示确认，如果目标文件已存在，则会询问是否覆盖，回答 **y** 时目标文件将被覆盖。。
- `-u` 或 `--update`：仅复制源文件中更新时间较新的文件。
- `-v` 或 `--verbose`：显示详细的复制过程。
- `-p` 或 `--preserve`：保留源文件的权限、所有者和时间戳信息。
- `-f` 或 `--force`：强制复制，即使目标文件已存在也会覆盖，而且不给出提示。
- -l：不复制文件，只是生成链接文件。



### 实例

将文件 file.txt 复制到目录 /path/to/destination/ 中：

```shell
cp file.txt /path/to/destination/
```



使用指令 **cp** 将当前目录 **test/** 下的所有文件复制到新目录 **newtest** 下，输入如下命令：

```shell
cp –r test/ newtest          
```

注意：用户使用该指令复制目录时，必须使用参数 **-r** 或者 **-R** 。



复制文件，并在目标文件已存在时进行确认：

```shell
cp -i file.txt /path/to/destination/
```

以上只是 cp 命令的一些常见用法，可以通过运行 **man cp** 命令查看更多选项和用法。



## mkdir命令 - 创建目录文件

mkdir命令来自英文词组make directories的缩写，其功能是创建目录文件。该命令的使用简单，但需要注意，若要创建的目标目录已经存在，则会提示已存在而不继续创建，不覆盖已有文件。若目录不存在，但具有嵌套的依赖关系时，例如/Dir1/Dir2/Dir3/Dir4/Dir5，要想一次性创建则需要加入-p参数，进行递归操‍作。 

### 语法格式 

```shell
mkdir 参数 目录名 
```



常用参数：

- -p ：确保目录名称存在，不存在的就建一个。



### 实例

在工作目录下，建立一个名为 runoob 的子目录 :

```shell
mkdir runoob
```



在工作目录下的 runoob2 目录中，建立一个名为 test 的子目录。若 runoob2 目录原本不存在，则建立一个。（注：本例若不加 -p 参数，且原本 runoob2 目录不存在，则产生错误。）

```shell
mkdir -p runoob2/test
```



## pwd命令 - 显示当前工作目录的路径

pwd命令来自英文词组print working directory的缩写，其功能是显示当前工作目录的路径，即显示所在位置的绝对路‍径。 

在实际工作中，我们经常会在不同目录之间进行切换，为了防止“迷路”，可以使用pwd命令快速查看当前所处的工作目录路径，方便开展后续工作。 



### 语法格式

```shell
pwd 参数 
```



常用参数

- --help ：在线帮助。
- --version ：显示版本信息。



### 实例

查看当前所在目录：

```shell
# pwd
/root/test           #输出结果
```



## tar命令 - 压缩和解压缩文件

tar命令的功能是压缩和解压缩文件，能够制作出Linux系统中常见的tar、tar.gz、tar.bz2等格式的压缩包文件。对于RHEL 7、CentOS 7版本及以后的系统，解压缩时不添加格式参数（如z或j），系统也能自动进行分析并解压。把要传输的文件先压缩再传输，能够很好地提高工作效率，方便分享。 



### 语法格式

```shell
tar 参数 压缩包名 文件或目录名 
```



常用参数：

- -A或--catenate ：新增文件到已存在的备份文件。
- -b<区块数目>或--blocking-factor=<区块数目> ：设置每笔记录的区块数目，每个区块大小为12Bytes。
- -B或--read-full-records ：读取数据时重设区块大小。
- -c或--create ：建立新的备份文件。
- -C<目的目录>或--directory=<目的目录> ：切换到指定的目录。
- -d或--diff或--compare ：对比备份文件内和文件系统上的文件的差异。
- -f<备份文件>或--file=<备份文件> ：指定备份文件。
- -F<Script文件>或--info-script=<Script文件> ：每次更换磁带时，就执行指定的Script文件。
- -g或--listed-incremental ：处理GNU格式的大量备份。
- -G或--incremental ：处理旧的GNU格式的大量备份。
- -h或--dereference ：不建立符号连接，直接复制该连接所指向的原始文件。
- -i或--ignore-zeros ：忽略备份文件中的0 Byte区块，也就是EOF。
- -k或--keep-old-files ：解开备份文件时，不覆盖已有的文件。
- -K<文件>或--starting-file=<文件> ：从指定的文件开始还原。
- -l或--one-file-system ：复制的文件或目录存放的文件系统，必须与tar指令执行时所处的文件系统相同，否则不予复制。
- -L<媒体容量>或-tape-length=<媒体容量> ：设置存放每体的容量，单位以1024 Bytes计算。
- -m或--modification-time ：还原文件时，不变更文件的更改时间。
- -M或--multi-volume ：在建立，还原备份文件或列出其中的内容时，采用多卷册模式。
- -N<日期格式>或--newer=<日期时间> ：只将较指定日期更新的文件保存到备份文件里。
- -o或--old-archive或--portability ：将资料写入备份文件时使用V7格式。
- -O或--stdout ：把从备份文件里还原的文件输出到标准输出设备。
- -p或--same-permissions ：用原来的文件权限还原文件。
- -P或--absolute-names ：文件名使用绝对名称，不移除文件名称前的"/"号。
- -r或--append ：新增文件到已存在的备份文件的结尾部分。
- -R或--block-number ：列出每个信息在备份文件中的区块编号。
- -s或--same-order ：还原文件的顺序和备份文件内的存放顺序相同。
- -S或--sparse ：倘若一个文件内含大量的连续0字节，则将此文件存成稀疏文件。
- -t或--list ：列出备份文件的内容。
- -T<范本文件>或--files-from=<范本文件> ：指定范本文件，其内含有一个或多个范本样式，让tar解开或建立符合设置条件的文件。
- -u或--update ：仅置换较备份文件内的文件更新的文件。
- -U或--unlink-first ：解开压缩文件还原文件之前，先解除文件的连接。
- -v或--verbose ：显示指令执行过程。
- -V<卷册名称>或--label=<卷册名称>： 建立使用指定的卷册名称的备份文件。
- -w或--interactive ：遭遇问题时先询问用户。
- -W或--verify ：写入备份文件后，确认文件正确无误。
- -x或--extract或--get ：从备份文件中还原文件。
- -X<范本文件>或--exclude-from=<范本文件> ：指定范本文件，其内含有一个或多个范本样式，让ar排除符合设置条件的文件。
- -z或--gzip或--ungzip ：通过gzip指令处理备份文件。
- -Z或--compress或--uncompress ：通过compress指令处理备份文件。
- -<设备编号><存储密度> ：设置备份用的外围设备编号及存放数据的密度。
- --after-date=<日期时间> ：此参数的效果和指定"-N"参数相同。
- --atime-preserve ：不变更文件的存取时间。
- --backup=<备份方式>或--backup ：移除文件前先进行备份。
- --checkpoint ：读取备份文件时列出目录名称。
- --concatenate ：此参数的效果和指定"-A"参数相同。
- --confirmation ：此参数的效果和指定"-w"参数相同。
- --delete ：从备份文件中删除指定的文件。
- --exclude=<范本样式> ：排除符合范本样式的文件。
- --group=<群组名称> ：把加入设备文件中的文件的所属群组设成指定的群组。
- --help ：在线帮助。
- --ignore-failed-read ：忽略数据读取错误，不中断程序的执行。
- --new-volume-script=<Script文件> ：此参数的效果和指定"-F"参数相同。
- --newer-mtime ：只保存更改过的文件。
- --no-recursion ：不做递归处理，也就是指定目录下的所有文件及子目录不予处理。
- --null ：从null设备读取文件名称。
- --numeric-owner ：以用户识别码及群组识别码取代用户名称和群组名称。
- --owner=<用户名称> ：把加入备份文件中的文件的拥有者设成指定的用户。
- --posix ：将数据写入备份文件时使用POSIX格式。
- --preserve ：此参数的效果和指定"-ps"参数相同。
- --preserve-order ：此参数的效果和指定"-A"参数相同。
- --preserve-permissions ：此参数的效果和指定"-p"参数相同。
- --record-size=<区块数目> ：此参数的效果和指定"-b"参数相同。
- --recursive-unlink ：解开压缩文件还原目录之前，先解除整个目录下所有文件的连接。
- --remove-files ：文件加入备份文件后，就将其删除。
- --rsh-command=<执行指令> ：设置要在远端主机上执行的指令，以取代rsh指令。
- --same-owner ：尝试以相同的文件拥有者还原文件。
- --suffix=<备份字尾字符串> ：移除文件前先行备份。
- --totals ：备份文件建立后，列出文件大小。
- --use-compress-program=<执行指令> ：通过指定的指令处理备份文件。
- --version ：显示版本信息。
- --volno-file=<编号文件> ：使用指定文件内的编号取代预设的卷册编号。



### 实例

1. 创建归档文件：将文件 file1、file2 和 directory 打包到一个名为 archive.tar 的归档文件中。

```shell
tar -cvf archive.tar file1 file2 directory
```

- `-c`: 创建新的归档文件
- `-v`: 显示详细输出，列出被添加到归档中的文件
- `-f`: 指定归档文件的名称



2. 解压归档文件：解压名为 archive.tar 的归档文件，还原其中包含的文件和目录。

```shell
tar -xvf archive.tar
```

- `-x`: 解压归档文件
- `-v`: 显示详细输出，列出被解压的文件
- `-f`: 指定要解压的归档文件的名称



3. 压缩归档文件：将名为 directory 的目录打包成一个归档文件，然后使用 gzip 进行压缩，生成名为 archive.tar.gz 的文件。

- `-c`: 创建新的归档文件
- `-z`: 使用 gzip 压缩归档文件
- `-v`: 显示详细输出，列出被添加到归档中的文件
- `-f`: 指定归档文件的名称



4. 列出归档文件中的内容：列出名为 archive.tar 的归档文件中包含的所有文件和目录。

```shell
tar -tvf archive.tar
```

- `-t`: 列出归档文件中的内容
- `-v`: 显示详细输出，列出归档文件中的所有文件和目录
- `-f`: 指定要列出内容的归档文件的名称



5. 追加文件到已存在的归档中：将名为 newfile 的文件添加到已存在的名为 archive.tar 的归档文件中。

```shell
tar -rvf archive.tar newfile
```

- `-r`: 向已存在的归档中追加文件
- `-v`: 显示详细输出，列出被添加到归档中的文件
- `-f`: 指定已存在的归档文件的名称



6. 创建一个经过 gzip 压缩的归档文件：打包 directory 目录下的所有文件和子目录，并使用 gzip 压缩，生成名为 archive.tar.gz 的归档文件。

```shell
tar -zcvf archive.tar.gz directory
```

- `-z`: 表示要使用 gzip 进行压缩。
- `-c`: 表示创建新的归档文件。
- `-v`: 表示详细输出，列出被添加到归档中的文件。
- `-f archive.tar.gz`: 指定归档文件的名称为 `archive.tar.gz`。



7. 解压一个已经被 gzip 压缩的归档文件：解压 example.tar.gz 文件，并在当前目录下恢复其中包含的文件和目录。

```shell
tar -zxvf example.tar.gz
```

- `-z`: 表示要使用 gzip 解压归档文件。
- `-x`: 表示解压操作。
- `-v`: 表示详细输出，列出被解压的文件。
- `-f example.tar.gz`: 指定要解压的归档文件的名称为 `example.tar.gz`。



8. 指定压缩格式

tar 可以结合不同的压缩程序来创建和解压压缩归档文件。

**z** : 使用 gzip 压缩。

```shell
tar -czvf archive.tar.gz directory
tar -xzvf archive.tar.gz
```

**j**: 使用 bzip2 压缩。

```shell
tar -cjvf archive.tar.bz2 directory
tar -xjvf archive.tar.bz2
```

**J**: 使用 xz 压缩。

```shell
tar -cJvf archive.tar.xz directory
tar -xJvf archive.tar.xz
```



## cd命令 - 切换目录

cd命令来自英文词组change directory的缩写，其功能是更改当前所处的工作目录，路径可以是绝对路径，也可以是相对路径，若省略不写则会跳转至当前使用者的家目‍录。 



### 语法格式

```shell
cd [dirName] 
```

- dirName：要切换的目标目录，可以是相对路径或绝对路径。

**换到绝对路径：**指定完整的目录路径来切换到目标目录。

```
cd /path/to/directory
```

**切换到相对路径：**指定相对于当前目录的路径来切换到目标目录。

```
cd relative/path/to/directory
```



### 实例

切换到 /usr/bin/ 目录:

```shell
cd /usr/bin
```



切换到上级目录：使用 .. 表示上级目录，可以通过连续多次使用 **..** 来切换到更高级的目录。

```shell
cd ..
cd ../../   // 切换到上上级目录
```



切换到用户主目录（home）：使用 ~ 表示当前用户的主目录，可以使用 cd 命令直接切换到主目录。

```shell
cd ~
```

切换到上次访问的目录：使用 cd - 可以切换到上次访问的目录。

```shell
cd -
```

切换到环境变量指定的目录：可以使用环境变量来指定目标目录，并使用 cd 命令切换到该目录。

```shell
cd $VAR_NAME
```



## chmod命令 - 改变文件或目录权限

chmod命令来自英文词组change mode的缩写，其功能是改变文件或目录权限的命令。默认只有文件的所有者和管理员可以设置文件权限，普通用户只能管理自己文件的权限属性。 

设置权限时可以使用数字法，亦可使用字母表达式，对于目录文件，建议加入-R参数进行递归操作，这意味着不仅对于目录本身，而且也对目录内的子文件/目录进行新权限的设定。 



Linux/Unix 的文件调用权限分为三级 : 文件所有者（Owner）、用户组（Group）、其它用户（Other Users）。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405251733778.jpeg)

只有文件所有者和超级用户可以修改文件或目录的权限。可以使用绝对模式（八进制数字模式），符号模式指定文件的权限。

![](https://seven97-blog.oss-cn-hangzhou.aliyuncs.com/imgs/202405251733788.png)



### 语法格式

```shell
chmod [-cfvR] [--help] [--version] mode file...
```



mode : 权限设定字串，格式如下 :

```
[ugoa...][[+-=][rwxX]...][,...]
```

其中：

- u 表示该文件的拥有者，g 表示与该文件的拥有者属于同一个群体(group)者，o 表示其他以外的人，a 表示这三者皆是。
- \+ 表示增加权限、- 表示取消权限、= 表示唯一设定权限。
- r 表示可读取，w 表示可写入，x 表示可执行，X 表示只有当该文件是个子目录或者该文件已经被设定过为可执行。



其他参数说明：

- -c : 若该文件权限确实已经更改，才显示其更改动作
- -f : 若该文件权限无法被更改也不要显示错误讯息
- -v : 显示权限变更的详细资料
- -R : 对目前目录下的所有文件与子目录进行相同的权限变更(即以递归的方式逐个变更)
- --help : 显示辅助说明
- --version : 显示版本



### 符号模式

使用符号模式可以设置多个项目：who（用户类型），operator（操作符）和 permission（权限），每个项目的设置可以用逗号隔开。 命令 chmod 将修改 who 指定的用户类型对文件的访问权限，用户类型由一个或者多个字母在 who 的位置来说明，如 who 的符号模式表所示:

| who  | 用户类型 | 说明                   |
| :--- | :------- | :--------------------- |
| `u`  | user     | 文件所有者             |
| `g`  | group    | 文件所有者所在组       |
| `o`  | others   | 所有其他用户           |
| `a`  | all      | 所有用户, 相当于 *ugo* |

operator 的符号模式表:

| Operator | 说明                                                   |
| :------- | :----------------------------------------------------- |
| `+`      | 为指定的用户类型增加权限                               |
| `-`      | 去除指定用户类型的权限                                 |
| `=`      | 设置指定用户权限的设置，即将用户类型的所有权限重新设置 |

permission 的符号模式表:

| 模式 | 名字         | 说明                                                         |
| :--- | :----------- | :----------------------------------------------------------- |
| `r`  | 读           | 设置为可读权限                                               |
| `w`  | 写           | 设置为可写权限                                               |
| `x`  | 执行权限     | 设置为可执行权限                                             |
| `X`  | 特殊执行权限 | 只有当文件为目录文件，或者其他类型的用户有可执行权限时，才将文件权限设置可执行 |
| `s`  | setuid/gid   | 当文件被执行时，根据who参数指定的用户类型设置文件的setuid或者setgid权限 |
| `t`  | 粘贴位       | 设置粘贴位，只有超级用户可以设置该位，只有文件所有者u可以使用该位 |

### 八进制语法

chmod命令可以使用八进制数来指定权限。文件或目录的权限位是由9个权限位来控制，每三位为一组，它们分别是文件所有者（User）的读、写、执行，用户组（Group）的读、写、执行以及其它用户（Other）的读、写、执行。历史上，文件权限被放在一个比特掩码中，掩码中指定的比特位设为1，用来说明一个类具有相应的优先级。

| #    | 权限           | rwx  | 二进制 |
| :--- | :------------- | :--- | :----- |
| 7    | 读 + 写 + 执行 | rwx  | 111    |
| 6    | 读 + 写        | rw-  | 110    |
| 5    | 读 + 执行      | r-x  | 101    |
| 4    | 只读           | r--  | 100    |
| 3    | 写 + 执行      | -wx  | 011    |
| 2    | 只写           | -w-  | 010    |
| 1    | 只执行         | --x  | 001    |
| 0    | 无             | ---  | 000    |

例如， 765 将这样解释：

- 所有者的权限用数字表达：属主的那三个权限位的数字加起来的总和。如 rwx ，也就是 4+2+1 ，应该是 7。
- 用户组的权限用数字表达：属组的那个权限位数字的相加的总和。如 rw- ，也就是 4+2+0 ，应该是 6。
- 其它用户的权限数字表达：其它用户权限位的数字相加的总和。如 r-x ，也就是 4+0+1 ，应该是 5。



### 实例

将文件 file1.txt 设为所有人皆可读取 :

```
chmod ugo+r file1.txt
```



将文件 file1.txt 设为所有人皆可读取 :

```
chmod a+r file1.txt
```



将文件 file1.txt 与 file2.txt 设为该文件拥有者，与其所属同一个群体者可写入，但其他以外的人则不可写入 :

```
chmod ug+w,o-w file1.txt file2.txt
```



为 ex1.py 文件拥有者增加可执行权限:

```
chmod u+x ex1.py
```



将目前目录下的所有文件与子目录皆设为任何人可读取 :

```
chmod -R a+r *
```



此外chmod也可以用数字来表示权限如 :

```
chmod 777 file
```



语法为：

```
chmod abc file
```

其中a,b,c各为一个数字，分别表示User、Group、及Other的权限。r=4，w=2，x=1

- 若要 rwx 属性则 4+2+1=7；
- 若要 rw- 属性则 4+2=6；
- 若要 r-x 属性则 4+1=5。

```
chmod a=rwx file
```

和

```
chmod 777 file
```

效果相同

```
chmod ug=rwx,o=x file
```

和

```
chmod 771 file
```

效果相同

若用 **chmod 4755 filename** 可使此程序具有 root 的权限。



### 更多说明

| `命令`                                     | 说明                                                         |
| :----------------------------------------- | :----------------------------------------------------------- |
| `chmod a+r *file*`                         | 给file的所有用户增加读权限                                   |
| `chmod a-x *file*`                         | 删除file的所有用户的执行权限                                 |
| `chmod a+rw *file*`                        | 给file的所有用户增加读写权限                                 |
| `chmod +rwx *file*`                        | 给file的所有用户增加读写执行权限                             |
| `chmod u=rw,go= *file*`                    | 对file的所有者设置读写权限，清空该用户组和其他用户对file的所有权限（空格代表无权限） |
| `chmod -R u+r,go-r *docs*`                 | 对目录docs和其子目录层次结构中的所有文件给用户增加读权限，而对用户组和其他用户删除读权限 |
| `chmod 664 *file*`                         | 对file的所有者和用户组设置读写权限, 为其其他用户设置读权限   |
| `chmod 0755 *file*`                        | 相当于`u=rwx (4+2+1),go=rx (4+1 & 4+1)`。`0` 没有特殊模式。  |
| `chmod 4755 *file*`                        | `4`设置了设置[用户ID](https://www.runoob.com/wiki/用户ID)位，剩下的相当于 u=rwx (4+2+1),go=rx (4+1 & 4+1)。 |
| `find path/ -type d -exec chmod a-x {} \;` | 删除可执行权限对path/以及其所有的目录（不包括文件）的所有用户，使用'-type f'匹配文件 |
| `find path/ -type d -exec chmod a+x {} \;` | 允许所有用户浏览或通过目录path/                              |












