# 符号调试指南

## 概述

符号调试功能旨在帮助开发者理解在命名空间语义检查期间符号如何被解析并与AST节点关联。这对于调试符号解析问题和理解命名空间分析器的内部工作特别有用。

## 组件

### 1. SymbolDebugVisitor

`SymbolDebugVisitor`类是一个访问者，遍历AST并输出每个节点的符号信息。它扩展了`VisitorBase`并重写了各种节点类型的访问方法来打印它们关联的符号。

**位置**: `src/main/java/semantic_check/debug/SymbolDebugVisitor.java`

**主要功能**:
- 遍历所有AST节点
- 为有符号关联的节点打印符号信息
- 收集关于节点类型和符号关联的统计信息
- 提供符号使用情况的摘要

### 2. SymbolDebugTest

`SymbolDebugTest`类是一个独立的测试应用程序，演示如何使用`SymbolDebugVisitor`。它执行完整的编译流程，然后使用调试访问者输出符号信息。

**位置**: `src/main/java/semantic_check/debug/SymbolDebugTest.java`

## 使用方法

### 方法1：使用独立测试

1. 编译项目：
   ```bash
   javac -cp src src/main/java/semantic_check/debug/SymbolDebugTest.java
   ```

2. 从标准输入运行测试：
   ```bash
   java -cp src main.java.semantic_check.debug.SymbolDebugTest
   ```
   然后输入或粘贴您的Rust代码，按Ctrl+D结束。

3. 使用文件运行测试：
   ```bash
   java -cp src main.java.semantic_check.debug.SymbolDebugTest < your_file.rx
   ```

### 方法2：使用提供的脚本

提供了一个便捷脚本来运行测试：

```bash
chmod +x run_symbol_debug_test.sh
./run_symbol_debug_test.sh
```

这将编译项目并使用`test_symbol_debug.rx`运行测试。

### 方法3：集成到您自己的代码中

您也可以将`SymbolDebugVisitor`集成到自己的代码中：

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

## 理解输出

### 节点类型

调试访问者跟踪各种节点类型，包括：
- `IdentifierNode`: 简单标识符
- `PathExprNode`: 路径表达式（例如，`Point::new`）
- `PathExprSegNode`: 路径表达式的段
- `StructExprNode`: 结构体表达式（例如，`Point { x: 10, y: 20 }`）
- `FunctionNode`: 函数定义
- `StructNode`: 结构体定义
- `EnumNode`: 枚举定义
- `TraitNode`: 特征定义
- `ImplNode`: 实现块
- 等等...

### 符号信息

每个符号包括：
- `name`: 符号名称
- `kind`: 符号类型（例如，STRUCT、FUNCTION、FIELD等）
- `scopeLevel`: 符号定义的作用域级别
- 根据符号类型的附加信息

### 摘要统计

在输出的最后，调试访问者提供摘要：
- 访问的总节点数
- 有符号的节点数
- 无符号的节点数
- 节点类型分布

## 示例

以下是使用调试访问者处理简单Rust程序的示例：

**输入**:
```rust
struct Point {
    x: i32,
    y: i32,
}

fn main() {
    let p = Point { x: 10, y: 20 };
}
```

**输出**:
```
Node: Point              | Type: StructNode           | Symbol: Symbol{name='Point', kind=STRUCT, scopeLevel=0}
Node: x                  | Type: IdentifierNode        | Symbol: Symbol{name='x', kind=FIELD, scopeLevel=1}
Node: y                  | Type: IdentifierNode        | Symbol: Symbol{name='y', kind=FIELD, scopeLevel=1}
Node: main               | Type: IdentifierNode        | Symbol: Symbol{name='main', kind=FUNCTION, scopeLevel=0}
Node: p                  | Type: IdentifierNode        | Symbol: Symbol{name='p', kind=LOCAL_VARIABLE, scopeLevel=1}
Node: Point              | Type: IdentifierNode        | Symbol: Symbol{name='Point', kind=STRUCT_CONSTRUCTOR, scopeLevel=0}
Node: x                  | Type: IdentifierNode        | Symbol: Symbol{name='x', kind=FIELD, scopeLevel=1}
Node: y                  | Type: IdentifierNode        | Symbol: Symbol{name='y', kind=FIELD, scopeLevel=1}

=== Symbol Debug Summary ===
Total nodes visited: 15
Nodes with symbols: 8
Nodes without symbols: 7

Node type distribution:
  StructNode: 1
  IdentifierNode: 7
  FieldNode: 2
  FunctionNode: 1
  LetStmtNode: 1
  StructExprNode: 1
  FieldValNode: 2
```

## 故障排除

### 编译错误

如果遇到编译错误，请确保：
1. 所有必需的依赖都在类路径中
2. 项目结构得到维护
3. 所有必需的导入都存在

### 运行时错误

如果遇到运行时错误：
1. 检查输入的Rust代码在语法上是否正确
2. 确保语义检查通过没有错误
3. 验证AST是否正确构造

### 没有符号信息

如果节点显示符号为"null"：
1. 确保已执行命名空间语义检查
2. 检查节点是否应该有符号（并非所有节点都有）
3. 验证符号解析过程是否正常工作

## 扩展功能

`SymbolDebugVisitor`可以扩展以：
1. 添加更详细的符号信息
2. 按节点类型或符号种类过滤输出
3. 以不同格式输出（JSON、XML等）
4. 添加可视化功能

要扩展，只需在`SymbolDebugVisitor`类中重写额外的访问方法或修改现有方法。