# RCompiler

一个用 Java 编写的类 Rust 语言编译器，将 `.rx` 源代码编译为 LLVM IR，再经由 clang 生成 RISC-V 汇编，最终在 REIMU 虚拟机上运行。

## 语言特性

RCompiler 实现了一个 Rust 风格的编程语言，支持以下特性：

- 基本类型：`i32`, `u32`, `i64`, `u64`, `f32`, `f64`, `bool`, `char`, `usize`, `String`, `&str`
- 所有权与借用：引用 (`&T`, `&mut T`)，移动语义
- 可变性控制：`let` / `let mut`
- 结构体与枚举：用户自定义类型，支持方法
- Trait 与 Impl：trait 定义与实现
- 控制流：`if`/`else`、`while` 循环、`match` 表达式、`continue`/`break`/`return`
- 数组：定长数组，支持索引访问
- 模式匹配：`let` 语句中的解构
- 常量：`const` 编译期常量
- 类型转换：`as` 表达式
- 内建函数：`getInt()`, `printInt()`, `printlnInt()`, `exit()` 等 I/O 函数

## 编译流程

```
源代码 (.rx)
    │
    ▼
 Tokenizer ── 词法分析 ──▶ Token 流
    │
    ▼
  Parser ── 语法分析 ──▶ AST
    │
    ▼
 语义分析（符号表构建、类型检查、所有权检查、可变性检查）
    │
    ▼
 IRGenerator ── 代码生成 ──▶ LLVM IR
    │
    ▼
  clang ── 汇编生成 ──▶ RISC-V 汇编 (.s)
    │
    ▼
  REIMU ── 虚拟机执行 ──▶ 程序输出
```

## 项目结构

```
src/main/java/
├── Main.java                  # 入口，串联整个编译流程
├── tokenizer/                 # 词法分析器
├── parser/                    # 递归下降语法分析器
├── ast/                       # AST 节点定义
├── types/                     # Token、字面量、运算符类型枚举
├── semantic_check/
│   ├── analyzer/              # 符号表构建与符号检查
│   ├── symbol/                # 符号定义
│   ├── type/                  # 类型系统 (PrimitiveType, StructType, EnumType, ...)
│   ├── typechecker/           # 类型检查器
│   ├── ownership/             # 所有权检查
│   ├── mutability/            # 可变性检查
│   ├── assignability/         # 赋值合法性检查
│   └── self/                  # self 语义分析
├── codegen/
│   ├── IRGenerator.java       # LLVM IR 生成
│   ├── IRPrinter.java         # IR 文本输出
│   ├── inst/                  # IR 指令定义
│   ├── ir/                    # IR 结构 (Module, Function, BasicBlock)
│   ├── type/                  # IR 类型
│   └── value/                 # IR 值
└── utils/                     # 工具类
```

## 环境要求

- Java (javac + java)
- clang（用于 LLVM IR → RISC-V 汇编）
- REIMU（RISC-V 模拟器，用于运行生成的汇编）
- Python 3（测试脚本依赖）
- GNU Make

## 构建与运行

### 编译项目

```bash
make build
```

### 编译并运行单个源文件

从 stdin 读入源代码，stdout 输出 LLVM IR：

```bash
cat test.rx | make run
```

或使用重定向：

```bash
make run-redirect INPUT=test.rx OUTPUT=output.ll
```

### 完整测试流程（编译 → IR → 汇编 → 执行）

```bash
./test_ir.sh tests/test_basic/test1.rx
```

带输入数据：

```bash
./test_ir.sh tests/test_comprehensive/test1.rx tests/test_comprehensive/test1.in
```

### 批量测试对比

```bash
./tests/compare_outputs.sh
```

## 示例代码

```rust
fn calculate_sum(n: i32) -> i32 {
    let mut sum: i32 = 0;
    let mut i: i32 = 1;
    while (i <= n) {
        sum += i;
        i += 1;
    }
    return sum;
}

fn main() {
    let n: i32 = getInt();
    let result: i32 = calculate_sum(n);
    printlnInt(result);
    exit(0);
}
```

## 测试

测试用例位于 `tests/` 目录下，按功能分类：

| 目录 | 内容 | 数量 |
|------|------|------|
| `test_basic/` | 基础功能 | 19 |
| `test_comprehensive/` | 综合测试（线段树合并等算法题） | 50 |
| `test_expr/` | 表达式求值 | 7 |
| `test_array/` | 数组操作 | 3 |
| `test_autoref/` | 自动引用 | 7 |
| `test_if/` | 条件分支 | 10 |
| `test_loop/` | 循环 | 5 |
| `test_misc/` | 综合杂项 | 56 |
| `test_return/` | 返回语句 | 6 |

每个测试包含：
- `*.rx` — 源代码
- `*.ans` — 期望输出
- `*.in` — 输入数据（可选）
- `*.out` — 实际输出（运行后生成）

## Makefile 常用命令

| 命令 | 说明 |
|------|------|
| `make build` | 编译项目 |
| `make compile` | 增量编译 |
| `make run` | 从 stdin 读入代码并输出 IR |
| `make test` | 运行单元测试 |
| `make test-all` | 运行所有测试 |
| `make clean` | 清理编译产物 |
| `make rebuild` | 清理后重新编译 |
| `make info` | 显示项目信息 |
