# 符号调试实现总结

## 概述

本文档总结了为RCompiler项目实现的符号调试功能，该功能允许开发者在命名空间语义检查期间输出和检查与AST中每个点关联的符号。

## 实现组件

### 1. SymbolDebugVisitor 类

**文件**: `src/main/java/semantic_check/debug/SymbolDebugVisitor.java`

**目的**: 一个访问者，遍历AST并输出每个节点的符号信息。

**主要功能**:
- 扩展`VisitorBase`以遍历所有AST节点
- 重写各种节点类型的访问方法以提取和显示符号信息
- 收集关于节点类型和符号关联的统计信息
- 提供格式化输出，显示节点名称、类型和关联的符号
- 生成包含统计信息的摘要报告

**支持的节点类型**:
- `IdentifierNode`: 简单标识符
- `PathExprNode`: 路径表达式（例如，`Point::new`）
- `PathExprSegNode`: 路径表达式的段
- `StructExprNode`: 结构体表达式
- `FieldValNode`: 结构体表达式中的字段值
- `FunctionNode`: 函数定义
- `StructNode`: 结构体定义
- `EnumNode`: 枚举定义
- `TraitNode`: 特征定义
- `ConstItemNode`: 常量定义
- `ImplNode`: 实现块
- `SelfParaNode`: Self参数
- `FieldNode`: 字段定义
- `IdPatNode`: 标识符模式

### 2. SymbolDebugTest 类

**文件**: `src/main/java/semantic_check/debug/SymbolDebugTest.java`

**目的**: 独立的测试应用程序，演示如何使用`SymbolDebugVisitor`。

**功能**:
- 执行完整的编译流程（词法分析、语法分析、语义检查）
- 使用`SymbolDebugVisitor`输出符号信息
- 优雅地处理错误并提供适当的错误消息
- 可以从标准输入或从文件重定向运行

### 3. 测试用例

**文件**: `test_symbol_debug.rx`

**目的**: 包含各种构造的示例Rust程序，用于测试符号解析。

**内容**:
- 带字段的结构体定义
- 带变体的枚举定义
- 特征定义
- 实现块（固有和特征）
- 函数定义
- 常量定义
- 变量声明和使用
- 方法调用
- 路径表达式

### 4. 测试脚本

**文件**: `run_symbol_debug_test.sh`

**目的**: 便捷脚本，用于编译和运行符号调试测试。

**功能**:
- 编译项目
- 使用提供的测试文件运行测试
- 提供清晰的输出和错误处理

### 5. 文档

**文件**: `docs/symbol_debug_guide.md`

**目的**: 关于如何使用符号调试功能的综合指南。

**内容**:
- 符号调试功能概述
- 每个组件的详细解释
- 不同方法的使用说明
- 输出格式解释
- 使用示例和输出
- 故障排除指南
- 扩展指南

## 使用说明

### 方法1：独立测试

1. 编译项目：
   ```bash
   javac -cp src src/main/java/semantic_check/debug/SymbolDebugTest.java
   ```

2. 从标准输入运行：
   ```bash
   java -cp src main.java.semantic_check.debug.SymbolDebugTest
   ```
   然后输入或粘贴Rust代码，按Ctrl+D结束。

3. 使用文件运行：
   ```bash
   java -cp src main.java.semantic_check.debug.SymbolDebugTest < your_file.rx
   ```

### 方法2：使用测试脚本

1. 使脚本可执行：
   ```bash
   chmod +x run_symbol_debug_test.sh
   ```

2. 运行测试：
   ```bash
   ./run_symbol_debug_test.sh
   ```

### 方法3：集成到现有代码

```java
// 执行命名空间语义检查后
SymbolDebugVisitor debugVisitor = new SymbolDebugVisitor();

for (StmtNode stmt : parser.getStatements()) {
    stmt.accept(debugVisitor);
}

// 打印摘要统计信息
debugVisitor.printSummary();
```

## 输出格式

调试访问者以以下格式输出信息：

```
Node: <节点名称> | Type: <节点类型> | Symbol: <符号信息>
```

例如：
```
Node: Point              | Type: StructNode           | Symbol: Symbol{name='Point', kind=STRUCT, scopeLevel=0}
Node: x                  | Type: IdentifierNode        | Symbol: Symbol{name='x', kind=FIELD, scopeLevel=1}
Node: new                | Type: IdentifierNode        | Symbol: Symbol{name='new', kind=FUNCTION, scopeLevel=1}
```

对于`ImplNode`，输出包括类型和特征符号：
```
Node: Impl(for Point)    | Type: ImplNode             | TypeSymbol: Symbol{name='Point', kind=STRUCT, scopeLevel=0} | TraitSymbol: null
```

## 优点

1. **调试符号解析**: 帮助识别为什么某些符号没有被正确解析
2. **理解命名空间结构**: 提供关于符号如何在不同命名空间中组织的洞察
3. **教育工具**: 帮助开发者理解编译器的内部工作
4. **测试辅助**: 用于编写和调试测试用例
5. **文档**: 作为符号解析过程的活文档

## 未来增强

符号调试功能的潜在改进：

1. **过滤选项**: 允许按节点类型、符号种类或作用域级别过滤输出
2. **输出格式**: 支持不同的输出格式（JSON、XML等）
3. **可视化**: 符号关系的图形表示
4. **集成**: 直接集成到主编译器中，带有命令行标志
5. **性能**: 对大型代码库的优化
6. **交互模式**: AST中符号的交互式探索

## 结论

符号调试实现为理解和调试RCompiler中的符号解析过程提供了强大的工具。它提供了多种访问功能的方式、全面的文档和未来增强的坚实基础。

通过使用此工具，开发者可以更深入地了解符号如何被解析并与AST节点关联，从而更容易识别和修复命名空间语义检查过程中的问题。