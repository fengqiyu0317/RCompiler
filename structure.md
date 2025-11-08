# RCompiler 项目架构建议

## 当前项目分析

基于当前文件结构，这是一个用Java实现的Rust编译器项目，包含以下主要组件：
- 编译器核心代码（Java源文件）
- 编译后的类文件
- 测试用例和测试工具
- 文档和参考资料
- 测试结果输出

## 建议的目录结构

```
RCompiler/
├── src/                          # 源代码目录
│   ├── main/                     # 主要源代码
│   │   ├── java/                 # Java源代码
│   │   │   ├── ast/              # AST相关类
│   │   │   │   ├── AST.java
│   │   │   │   ├── ASTNode.java
│   │   │   │   └── [其他AST相关类]
│   │   │   ├── parser/           # 解析器相关
│   │   │   │   ├── Parser.java
│   │   │   │   ├── ParseException.java
│   │   │   │   └── [其他解析器类]
│   │   │   ├── tokenizer/        # 词法分析器
│   │   │   │   ├── Tokenizer.java
│   │   │   │   ├── token_t.java
│   │   │   │   └── [其他词法分析类]
│   │   │   ├── nodes/            # AST节点类
│   │   │   │   ├── expr/         # 表达式节点
│   │   │   │   ├── stmt/         # 语句节点
│   │   │   │   └── item/         # 项目节点
│   │   │   ├── utils/            # 工具类
│   │   │   │   ├── PrintAST.java
│   │   │   │   └── [其他工具类]
│   │   │   └── Main.java         # 主入口
│   │   └── resources/            # 资源文件
│   └── test/                     # 测试源代码
│       └── java/                 # 测试Java代码
│           ├── ErrorTestRunner.java
│           ├── TestRunner.java
│           └── BatchTestRunner.java
├── docs/                         # 文档目录
│   ├── design/                   # 设计文档
│   │   ├── grammar.md
│   │   ├── parse_detailed.md
│   │   ├── Parse strategy.md
│   │   └── compiler_analysis.md
│   ├── api/                      # API文档
│   └── reference/                # 参考资料
│       ├── Tokens - The Rust Reference.html
│       └── Tokens - The Rust Reference_files/
├── tests/                        # 测试目录
│   ├── inputs/                   # 测试输入文件
│   │   ├── test.rs
│   │   ├── a.rs
│   │   └── [其他测试输入文件]
│   ├── outputs/                  # 测试输出文件
│   │   ├── test.out
│   │   ├── result.out
│   │   └── temp_result.out
│   └── testcases/                # 测试用例
│       └── RCompiler-Testcases/
├── results/                      # 结果输出目录
│   └── semantic-1/               # 语义分析结果
│       └── [所有语义分析结果文件]
├── scripts/                      # 脚本目录
│   ├── build.sh                  # 构建脚本
│   ├── test.sh                   # 测试脚本
│   └── [其他脚本]
├── lib/                          # 依赖库目录
├── target/                       # 编译输出目录
│   └── classes/                  # 编译后的类文件
│       └── [所有.class文件]
├── config/                       # 配置文件目录
│   ├── sources.txt
│   └── sources_new.txt
├── README.md                     # 项目说明
├── structure.md                  # 项目架构说明（本文件）
└── .gitignore                    # Git忽略文件
```

## 目录说明

### src/ - 源代码目录
- `src/main/java/` 存放主要的Java源代码
- 按功能模块进一步细分：ast、parser、tokenizer、nodes、utils等
- `src/test/java/` 存放测试相关的Java代码

### docs/ - 文档目录
- `design/` 存放设计相关文档
- `api/` 存放API文档
- `reference/` 存放参考资料

### tests/ - 测试目录
- `inputs/` 存放测试输入文件（如.rs文件）
- `outputs/` 存放测试输出文件
- `testcases/` 存放测试用例集合

### results/ - 结果输出目录
- 存放编译器运行产生的各种结果文件

### target/ - 编译输出目录
- 存放编译后的.class文件，与源代码分离

### scripts/ - 脚本目录
- 存放构建、测试等自动化脚本

## 重组的好处

1. **清晰的模块分离**：源代码、测试、文档、输出分别存放
2. **便于维护**：按功能模块组织代码，便于定位和修改
3. **标准化结构**：符合Java项目的标准目录结构
4. **便于构建**：源代码和编译产物分离，便于构建工具管理
5. **版本控制友好**：可以合理配置.gitignore，避免提交不必要的文件

## 迁移建议

1. 首先创建新的目录结构
2. 按照功能将现有文件移动到对应目录
3. 更新构建脚本和配置文件以适应新结构
4. 更新README.md文档以反映新的项目结构
5. 考虑添加适当的构建工具（如Maven或Gradle）

## 注意事项

- 保持现有的功能不变，只是重新组织文件结构
- 确保所有相对路径引用在重组后仍然有效
- 考虑添加适当的构建配置文件
- 建议添加.gitignore文件，排除.class文件和其他临时文件