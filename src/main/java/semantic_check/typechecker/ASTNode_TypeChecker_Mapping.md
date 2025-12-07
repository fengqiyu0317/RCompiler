# AST节点与TypeChecker映射文档

本文档描述了RCompiler项目中每种AST节点应该使用哪种类型的typechecker进行类型检查。

## 架构概述

RCompiler的类型检查系统采用了分层架构，包含以下主要组件：

1. **TypeChecker** - 主类型检查器，作为所有类型检查的入口点
2. **TypeCheckerRefactored** - 重构后的表达式类型检查器，作为各个专门检查器的协调器
3. **StatementTypeChecker** - 语句类型检查器，处理各种语句节点
4. **ControlFlowTypeChecker** - 控制流类型检查器，处理控制流表达式
5. **SimpleExpressionTypeChecker** - 简单表达式类型检查器
6. **OperatorExpressionTypeChecker** - 运算符表达式类型检查器
7. **ComplexExpressionTypeChecker** - 复杂表达式类型检查器

## AST节点与TypeChecker映射表

### 表达式节点 (Expression Nodes)

| 节点类型 | 使用的TypeChecker | 说明 |
|---------|------------------|------|
| `LiteralExprNode` | `SimpleExpressionTypeChecker` | 字面量表达式，处理数字、字符串、布尔值等 |
| `PathExprNode` | `SimpleExpressionTypeChecker` | 路径表达式，处理变量、字段访问等 |
| `PathExprSegNode` | `SimpleExpressionTypeChecker` | 路径表达式段 |
| `GroupExprNode` | `SimpleExpressionTypeChecker` | 分组表达式 (expression) |
| `UnderscoreExprNode` | `SimpleExpressionTypeChecker` | 下划线表达式，用作通配符 |

### 运算符表达式节点 (Operator Expression Nodes)

| 节点类型 | 使用的TypeChecker | 说明 |
|---------|------------------|------|
| `ArithExprNode` | `OperatorExpressionTypeChecker` | 算术表达式 (+, -, *, /, %, &, \|, ^, <<, >>) |
| `CompExprNode` | `OperatorExpressionTypeChecker` | 比较表达式 (==, !=, >, <, >=, <=) |
| `LazyExprNode` | `OperatorExpressionTypeChecker` | 惰性逻辑表达式 (&&, \|\|) |
| `NegaExprNode` | `OperatorExpressionTypeChecker` | 取反表达式 (!, -) |
| `AssignExprNode` | `OperatorExpressionTypeChecker` | 赋值表达式 (=) |
| `ComAssignExprNode` | `OperatorExpressionTypeChecker` | 复合赋值表达式 (+=, -=, *=, /=, %=, &=, \|=, ^=, <<=, >>=) |

### 复杂表达式节点 (Complex Expression Nodes)

| 节点类型 | 使用的TypeChecker | 说明 |
|---------|------------------|------|
| `CallExprNode` | `ComplexExpressionTypeChecker` | 函数调用表达式 |
| `MethodCallExprNode` | `ComplexExpressionTypeChecker` | 方法调用表达式 |
| `FieldExprNode` | `ComplexExpressionTypeChecker` | 字段访问表达式 |
| `IndexExprNode` | `ComplexExpressionTypeChecker` | 索引访问表达式 |
| `BorrowExprNode` | `ComplexExpressionTypeChecker` | 借用表达式 (&, &&) |
| `DerefExprNode` | `ComplexExpressionTypeChecker` | 解引用表达式 (*) |
| `TypeCastExprNode` | `ComplexExpressionTypeChecker` | 类型转换表达式 (as) |
| `ArrayExprNode` | `ComplexExpressionTypeChecker` | 数组表达式 |
| `StructExprNode` | `ComplexExpressionTypeChecker` | 结构体表达式 |
| `FieldValNode` | `ComplexExpressionTypeChecker` | 字段值节点（在结构体表达式中使用） |

### 控制流表达式节点 (Control Flow Expression Nodes)

| 节点类型 | 使用的TypeChecker | 说明 |
|---------|------------------|------|
| `BlockExprNode` | `ControlFlowTypeChecker` | 块表达式 { ... } |
| `IfExprNode` | `ControlFlowTypeChecker` | if表达式 |
| `LoopExprNode` | `ControlFlowTypeChecker` | 循环表达式 (loop, while) |
| `BreakExprNode` | `ControlFlowTypeChecker` | break表达式 |
| `ContinueExprNode` | `ControlFlowTypeChecker` | continue表达式 |
| `ReturnExprNode` | `ControlFlowTypeChecker` | return表达式 |

### 语句节点 (Statement Nodes)

| 节点类型 | 使用的TypeChecker | 说明 |
|---------|------------------|------|
| `LetStmtNode` | `StatementTypeChecker` | let语句 |
| `ExprStmtNode` | `StatementTypeChecker` | 表达式语句 |

### 项节点 (Item Nodes)

| 节点类型 | 使用的TypeChecker | 说明 |
|---------|------------------|------|
| `ItemNode` | `StatementTypeChecker` | 项基类（抽象） |
| `FunctionNode` | `StatementTypeChecker` | 函数定义 |
| `StructNode` | `StatementTypeChecker` | 结构体定义 |
| `EnumNode` | `StatementTypeChecker` | 枚举定义 |
| `ConstItemNode` | `StatementTypeChecker` | 常量定义 |
| `TraitNode` | `StatementTypeChecker` | trait定义 |
| `ImplNode` | `StatementTypeChecker` | impl块 |
| `AssoItemNode` | `StatementTypeChecker` | 关联项（函数或常量） |

### 参数和模式节点 (Parameter and Pattern Nodes)

| 节点类型 | 使用的TypeChecker | 说明 |
|---------|------------------|------|
| `SelfParaNode` | `StatementTypeChecker` | self参数 |
| `ParameterNode` | `StatementTypeChecker` | 函数参数 |
| `PatternNode` | `StatementTypeChecker` | 模式基类（抽象） |
| `IdPatNode` | `StatementTypeChecker` | 标识符模式 |
| `WildPatNode` | `StatementTypeChecker` | 通配符模式 |
| `RefPatNode` | `StatementTypeChecker` | 引用模式 |

### 类型表达式节点 (Type Expression Nodes)

| 节点类型 | 使用的TypeChecker | 说明 |
|---------|------------------|------|
| `TypeExprNode` | `StatementTypeChecker` | 类型表达式基类（抽象） |
| `TypePathExprNode` | `StatementTypeChecker` | 路径类型表达式 |
| `TypeRefExprNode` | `StatementTypeChecker` | 引用类型表达式 |
| `TypeArrayExprNode` | `StatementTypeChecker` | 数组类型表达式 |
| `TypeUnitExprNode` | `StatementTypeChecker` | 单元类型表达式 |

### 其他节点 (Other Nodes)

| 节点类型 | 使用的TypeChecker | 说明 |
|---------|------------------|------|
| `IdentifierNode` | `StatementTypeChecker` | 标识符 |
| `FieldNode` | `StatementTypeChecker` | 结构体字段 |
| `BuiltinFunctionNode` | `StatementTypeChecker` | 内置函数 |

## TypeChecker之间的协作关系

### 1. TypeChecker (主入口)
- 作为所有类型检查的入口点
- 将访问请求委托给适当的专门检查器
- 协调各个检查器之间的交互

### 2. TypeCheckerRefactored (表达式协调器)
- 作为表达式类型检查的中央协调器
- 管理表达式类型上下文
- 将表达式节点分发给适当的专门检查器：
  - 简单表达式 → `SimpleExpressionTypeChecker`
  - 运算符表达式 → `OperatorExpressionTypeChecker`
  - 复杂表达式 → `ComplexExpressionTypeChecker`
  - 控制流表达式 → `ControlFlowTypeChecker`

### 3. StatementTypeChecker (语句检查器)
- 处理所有语句级别的节点
- 使用`TypeCheckerRefactored`处理表达式
- 管理函数和impl块的上下文

### 4. ControlFlowTypeChecker (控制流检查器)
- 专门处理控制流表达式
- 维护控制流上下文栈
- 与`TypeCheckerRefactored`协作处理嵌套表达式

### 5. 专门表达式检查器
- `SimpleExpressionTypeChecker`: 处理基础表达式
- `OperatorExpressionTypeChecker`: 处理运算符表达式
- `ComplexExpressionTypeChecker`: 处理复杂表达式
- 都继承自`BaseExpressionTypeChecker`，共享通用功能

## 类型检查流程

1. **入口点**: `TypeChecker`接收AST节点访问请求
2. **分发**: 根据节点类型将请求分发给适当的检查器
3. **递归检查**: 检查器递归访问子节点
4. **上下文管理**: 维护类型检查上下文（如函数作用域、Self类型等）
5. **错误收集**: 收集和报告类型错误
6. **类型推断**: 为表达式推断和设置类型

## 注意事项

1. **递归访问**: 所有表达式检查器都使用`TypeCheckerRefactored`进行递归访问，确保使用正确的类型检查器
2. **上下文传递**: 通过`ExpressionTypeContext`和`ControlFlowContext`维护类型检查上下文
3. **错误处理**: 统一的错误收集和报告机制
4. **可变性检查**: 集成的可变性检查器确保变量和引用的可变性规则
5. **符号解析**: 与符号表系统紧密集成，确保符号和类型的一致性

## 扩展指南

当添加新的AST节点时：

1. 确定节点类型（表达式、语句、项等）
2. 选择适当的typechecker或创建新的专门检查器
3. 在`TypeChecker`中添加相应的visit方法
4. 如果是表达式节点，在`TypeCheckerRefactored`中添加分发逻辑
5. 确保递归访问使用正确的检查器
6. 添加相应的测试用例

这种分层架构使得类型检查系统更加模块化、可维护和可扩展。