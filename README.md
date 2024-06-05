## 运行
0.执行 ```mvn clean compile assembly:single```可以生成带全依赖可运行的jar包
1.查看Main方法中的参数
2.执行 -init 生成配置文件
3.修改配置文件
4.执行 -start 进行翻译


## 字符串加载流程
1. 读取配置文件支持语言目录的string.xml
2. 没读取到的生成对应的stringInfo信息
3. 判断字符串信息进行翻译
4. 写入excel
5. 写回xml

## 字符串数组加载流程
1. 读取配置文件支持语言目录的string.xml，并删除内部的数组信息
2. 创建并读取array.xml

# 翻译工具使用说明

## 一. 工具的使用

### 1. 先执行 `-init` 导出配置文件
- 命令：`java -jar translator_v1.0.5.jar -init`
- 然后修改配置文件中的值，正常使用只需要配置 `sourcePath`/`resPaths` 中的一个和 `poxyPort` 即可

    - `sourcePath`：指定翻译的项目路径，路径需要用双斜杠“\\”
        - 示例：`X:\\AsrAndroid8.1_2023112121\\vendor\\sgtc\\apps\\commons_source\\SgtcSettings`
        - 这里是翻译 `SgtcSettings` 项目

    - `proxyPort`：代理端口根据自己实际情况配置自己电脑的端口

### 2. 执行 `-run` 进行翻译
- 命令：`java -jar translator-v1.0.5.jar -run`

### 3. 执行 `-lan` 查看支持的语言
- 如果有遇到不支持语言报错，可以执行 `java -jar translator_v1.0.2.jar -lan` 查看谷歌翻译是否支持该语言，不支持可上报

#### 注意：
1. 有带 `values-zh` 的需要特殊处理。生成的 `values-zh-rCN` 优先级更高，所以即使是原有中文的字符串展示出来的也是被翻译后的。
    - 解决方法: 删除 `values-zh-rCN` 目录，把目录名 `values-zh` 改成 `values-zh-rCN`，重新运行工具
2. 有的字符串，例如单位 cm 这些，没配置中文也没加 `translate=fase` 属性也可能会有问题
    - 对特殊字符串手动加 `translate=fase`
3. 英文有缩写的也要注意一下，比如 `sunday` 缩写 `sun`，其他语言直接翻译成太阳了
    - 翻译前手动补全，翻译完后再改回去
4. 翻译字符串数组时，需要特别注意，里面可能有很多属性不需要翻译，需加 `@`

## 二. 版本记录与bug反馈：

### `translator_v1.0.1.jar`
- ~~bug：10101：nb,in,in-rID语言翻译错误~~
- ~~bug：10102：引号内部有引号会翻译失败，加`\`处理~~
- ~~bug：10103：需要同级目录下创建个classes目录把translate_config.txt放进去。~~

### `translator_v1.0.2.jar`
- fix：10101，不支持对应语言翻译
- fix：10102，已对单双引号做处理
- fix：10103，直接修改translate_config.txt，不需要移动目录了
- ~~bug：10201：hi与th语言会出现unicode中字母被翻译然后构建失败问题~~
- ~~bug：10202：es语言有音标的字符出现编码问题~~
- ~~bug：10203：带locale的文件中出现默认语言未翻译的字符串~~
- ~~bug：10204：暂时不支持字符串数组翻译~~
- ~~bug：10205：xml中会生成空行~~

### `translator_v1.0.4.jar`
- fix：10201：对有unicode编码的字符串不翻译
- fix：10205：写入xml都会对空行优化处理
- feature：1.对有`translate=false`的字符串不进行翻译
- feature：2.不处理带有locale的文件，直接生成不带locale的翻译
- feature：3.优化输出的excel文档显示格式
- ~~bug：10401：如果有字符串替换，可能有带locale的字符串为处理但被优先加载而显示异常~~
- ~~bug：10402：有zh目录的字符串会被zh-rCN的优先使用，有en目录被优先使用~~
- ~~bug：10403：有 % 的占位符出现翻译问题~~

### `translator_v1.0.5.jar`
- fix：10204：已支持数组翻译，但需要单独检验数组中的信息，并配置 `isSupportTranslateArrays=true`
- fix：10401：不翻译中英文解决该问题
- fix：10402：不翻译中文、英文
- fix：10403：用正则匹配占位符与unicode不进行翻译
- feature：1.若有 `translate=false` 或者 `translatable=false` 则会删除其他语言的翻译，也可用来单独删除某一个字符串翻译重新翻
- feature：2.支持读取sgtc_strings.xml
- feature：3.支持配置，仅生成excel，仅通过excel替换字符串功能
- feature：4.支持nb，in语言的翻译
- feature：5.配置`isOverwrite=true`，可以删除字符串两侧英文引号，若编译或验证出现问题需单独提出
- ~~bug：10501：类似带@String\的会被翻译，需手动添加`translate=false`~~
- ~~bug：10502：默认语言是中文时不会翻译成英文~~

### `translator_v1.0.6.jar`
- fix: 10501：类似@String\字符串或者item中有此格式的素组不翻译
- feature: 1.支持读取已经生成的最新版excel作为默认属性,且不会修改excel已有的格式
- feature: 2.优化config中的属性作用及其命名
- feature: 3.支持配置是否增删外侧双引号，以及是否对单双引号进行转义处理

### `translator_v1.0.7.jar`
- fix：10502：默认语言是中文会移动至zh-rCN中，并对values中的翻译成英语
- feature：1.可以用xliff标签包裹不需要翻译的字符串，例如 "hello <xliff:g>world</xliff:g>" 翻译后 "你好 world"
- feature：2.可以通过配置 translateStrings 来选择只翻译某些字符串
- todo：3.数组加载有问题
- todo：4.excel读取与写入数组
- todo：5.stringArray 没有带上属性


## 三. 参数说明
- `isOverwrite`: 是否覆盖已有翻译，需要对原字符串特殊处理的情况下使用，例如去除双引号
- `isGenerateExcelMode`: 是否只生成excel，不进行翻译与替换
- `isTranslate`: 如果仅替换，需要把属性设置为false
- `isSupportTranslateArrays`: 是否支持翻译数组

### 文件路径配置
- `sourcePath`: 源文件路径,如果配置了则会加载目录下所有res目录下的strings.xml文件并将路径设置到resPaths中，sourcePath与resPaths只用配置一个
- `resPaths`: 需要翻译的文件路径列表，具体到res目录的路径，可以单独配置多个目录进行翻译,如果配置了sourcePath则会忽略该配置

- `targetPath`: 翻译后的文件存放路径,如果有配置路径，则会在路径下生成有翻译后的 appName/res/values-xx 文件夹
- `excelPath`: 需要读取的excel文件路径,如果有配置，则会读取excel中的信息文件，并将文件并将路径设置到customExcelPaths中，excelPath与customExcelPaths只用配置一个，需要配置为空
- `customExcelPaths`: 需要读取的excel文件路径，可以单独配置目录进行读取,如果配置了excelPath则会忽略该配置

### 其他配置
- `apiKey`: google翻译api的key
- `poxyPort`: 代理端口，需要配置代理，不然谷歌翻译会被墙

## 四. Excel 颜色意义说明
- 字体黑色：默认已有翻译的字符串
- 字体绿色：被翻译的字符串
- 字体蓝色：被替换（可通过读取excel获取）的字符串
- 背景灰色：有`translate=false`的字符串
- 背景橙色：有特殊字符（例如 unicode）的字符串
- 背景黄色：出现翻译错误或者有异常的字符串
