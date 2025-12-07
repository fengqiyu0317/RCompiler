# ASTNode类型在TypeChecker中的分布

本文档记录了每个ASTNode类型在typechecker系统中的分布情况，说明它们由哪个部分负责类型检查。

## TypeChecker架构概述

TypeChecker系统分为以下几个主要部分：

1. **TypeChecker (主协调器)** - 协调各个专门的类型检查器
2. **ExpressionTypeChecker (原始表达式类型检查器)** - 处理所有表达式类型检查
3. **ExpressionTypeCheckerRefactored (重构后的表达式类型检查器)** - 作为协调器，委托给专门的检查器
4. **StatementTypeChecker (语句类型检查器)** - 处理语句类型检查
5. **ControlFlowTypeChecker (控制流类型检查器)** - 处理控制流表达式
6. **SimpleExpressionTypeChecker (简单表达式类型检查器)** - 处理简单表达式
7. **OperatorExpressionTypeChecker (运算符表达式类型检查器)** - 处理运算符表达式
8. **ComplexExpressionTypeChecker (复杂表达式类型检查器)** - 处理复杂表达式

## ASTNode类型分布

### 1. 语句节点 (Statement Nodes)

| ASTNode类型 | 负责的检查器 | 说明 |
|-------------|--------------|------|
| [`StmtNode`](src/main/java/ast/AST.java:51) | StatementTypeChecker | 基础语句节点，由StatementTypeChecker处理 |
| [`ItemNode`](src/main/java/ast/AST.java:58) | StatementTypeChecker | 基础项节点，由StatementTypeChecker处理 |
| [`LetStmtNode`](src/main/java/ast/AST.java:66) | StatementTypeChecker | let语句，由StatementTypeChecker处理 |
| [`ExprStmtNode`](src/main/java/ast/AST.java:79) | ExpressionTypeChecker/ExpressionTypeCheckerRefactored | 表达式语句，由表达式类型检查器处理 |
| [`FunctionNode`](src/main/java/ast/AST.java:92) | StatementTypeChecker | 函数定义，由StatementTypeChecker处理 |
| [`StructNode`](src/main/java/ast/AST.java:204) | StatementTypeChecker | 结构体定义，由StatementTypeChecker处理 |
| [`EnumNode`](src/main/java/ast/AST.java:262) | StatementTypeChecker | 枚举定义，由StatementTypeChecker处理 |
| [`ConstItemNode`](src/main/java/ast/AST.java:285) | StatementTypeChecker | 常量定义，由StatementTypeChecker处理 |
| [`TraitNode`](src/main/java/ast/AST.java:309) | StatementTypeChecker | trait定义，由StatementTypeChecker处理 |
| [`ImplNode`](src/main/java/ast/AST.java:346) | StatementTypeChecker | impl块，由StatementTypeChecker处理 |
| [`AssoItemNode`](src/main/java/ast/AST.java:331) | StatementTypeChecker | 关联项，由StatementTypeChecker处理 |

### 2. 参数和模式节点 (Parameter and Pattern Nodes)

| ASTNode类型 | 负责的检查器 | 说明 |
|-------------|--------------|------|
| [`SelfParaNode`](src/main/java/ast/AST.java:126) | StatementTypeChecker | self参数，由StatementTypeChecker处理 |
| [`ParameterNode`](src/main/java/ast/AST.java:150) | StatementTypeChecker | 函数参数，由StatementTypeChecker处理 |
| [`PatternNode`](src/main/java/ast/AST.java:162) | StatementTypeChecker | 模式基类，由StatementTypeChecker处理 |
| [`IdPatNode`](src/main/java/ast/AST.java:170) | StatementTypeChecker | 标识符模式，由StatementTypeChecker处理 |
| [`WildPatNode`](src/main/java/ast/AST.java:182) | StatementTypeChecker | 通配符模式，由StatementTypeChecker处理 |
| [`RefPatNode`](src/main/java/ast/AST.java:190) | StatementTypeChecker | 引用模式，由StatementTypeChecker处理 |

### 3. 字段和标识符节点 (Field and Identifier Nodes)

| ASTNode类型 | 负责的检查器 | 说明 |
|-------------|--------------|------|
| [`FieldNode`](src/main/java/ast/AST.java:238) | StatementTypeChecker | 结构体字段，由StatementTypeChecker处理 |
| [`IdentifierNode`](src/main/java/ast/AST.java:845) | StatementTypeChecker | 标识符，由StatementTypeChecker处理 |
| [`FieldValNode`](src/main/java/ast/AST.java:672) | ComplexExpressionTypeChecker | 结构体字段值，由复杂表达式类型检查器处理 |

### 4. 类型表达式节点 (Type Expression Nodes)

| ASTNode类型 | 负责的检查器 | 说明 |
|-------------|--------------|------|
| [`TypeExprNode`](src/main/java/ast/AST.java:868) | StatementTypeChecker | 类型表达式基类，由StatementTypeChecker处理 |
| [`TypePathExprNode`](src/main/java/ast/AST.java:877) | StatementTypeChecker | 路径类型表达式，由StatementTypeChecker处理 |
| [`TypeRefExprNode`](src/main/java/ast/AST.java:888) | StatementTypeChecker | 引用类型表达式，由StatementTypeChecker处理 |
| [`TypeArrayExprNode`](src/main/java/ast/AST.java:900) | StatementTypeChecker | 数组类型表达式，由StatementTypeChecker处理 |
| [`TypeUnitExprNode`](src/main/java/ast/AST.java:912) | StatementTypeChecker | 单元类型表达式，由StatementTypeChecker处理 |

### 5. 表达式节点 (Expression Nodes)

#### 5.1 基础表达式节点

| ASTNode类型 | 负责的检查器 | 说明 |
|-------------|--------------|------|
| [`ExprNode`](src/main/java/ast/AST.java:379) | 所有表达式检查器 | 表达式基类，由所有表达式检查器处理 |
| [`ExprWithBlockNode`](src/main/java/ast/AST.java:410) | ControlFlowTypeChecker | 带块的表达式基类，由控制流类型检查器处理 |
| [`ExprWithoutBlockNode`](src/main/java/ast/AST.java:418) | 各表达式检查器 | 不带块的表达式基类，由各表达式检查器处理 |

#### 5.2 简单表达式节点 (Simple Expressions)

| ASTNode类型 | 负责的检查器 | 说明 |
|-------------|--------------|------|
| [`LiteralExprNode`](src/main/java/ast/AST.java:431) | SimpleExpressionTypeChecker | 字面量表达式，由简单表达式类型检查器处理 |
| [`PathExprNode`](src/main/java/ast/AST.java:447) | SimpleExpressionTypeChecker | 路径表达式，由简单表达式类型检查器处理 |
| [`PathExprSegNode`](src/main/java/ast/AST.java:470) | SimpleExpressionTypeChecker | 路径表达式段，由简单表达式类型检查器处理 |
| [`GroupExprNode`](src/main/java/ast/AST.java:493) | SimpleExpressionTypeChecker | 分组表达式，由简单表达式类型检查器处理 |
| [`UnderscoreExprNode`](src/main/java/ast/AST.java:795) | SimpleExpressionTypeChecker | 下划线表达式，由简单表达式类型检查器处理 |

#### 5.3 运算符表达式节点 (Operator Expressions)

| ASTNode类型 | 负责的检查器 | 说明 |
|-------------|--------------|------|
| [`OperExprNode`](src/main/java/ast/AST.java:504) | OperatorExpressionTypeChecker | 运算符表达式基类，由运算符表达式类型检查器处理 |
| [`ArithExprNode`](src/main/java/ast/AST.java:545) | OperatorExpressionTypeChecker | 算术表达式，由运算符表达式类型检查器处理 |
| [`CompExprNode`](src/main/java/ast/AST.java:557) | OperatorExpressionTypeChecker | 比较表达式，由运算符表达式类型检查器处理 |
| [`LazyExprNode`](src/main/java/ast/AST.java:569) | OperatorExpressionTypeChecker | 惰性逻辑表达式，由运算符表达式类型检查器处理 |
| [`NegaExprNode`](src/main/java/ast/AST.java:534) | OperatorExpressionTypeChecker | 取反表达式，由运算符表达式类型检查器处理 |
| [`AssignExprNode`](src/main/java/ast/AST.java:592) | OperatorExpressionTypeChecker | 赋值表达式，由运算符表达式类型检查器处理 |
| [`ComAssignExprNode`](src/main/java/ast/AST.java:604) | OperatorExpressionTypeChecker | 复合赋值表达式，由运算符表达式类型检查器处理 |

#### 5.4 复杂表达式节点 (Complex Expressions)

| ASTNode类型 | 负责的检查器 | 说明 |
|-------------|--------------|------|
| [`CallExprNode`](src/main/java/ast/AST.java:696) | ComplexExpressionTypeChecker | 函数调用表达式，由复杂表达式类型检查器处理 |
| [`MethodCallExprNode`](src/main/java/ast/AST.java:709) | ComplexExpressionTypeChecker | 方法调用表达式，由复杂表达式类型检查器处理 |
| [`FieldExprNode`](src/main/java/ast/AST.java:722) | ComplexExpressionTypeChecker | 字段访问表达式，由复杂表达式类型检查器处理 |
| [`IndexExprNode`](src/main/java/ast/AST.java:638) | ComplexExpressionTypeChecker | 索引表达式，由复杂表达式类型检查器处理 |
| [`BorrowExprNode`](src/main/java/ast/AST.java:512) | ComplexExpressionTypeChecker | 借用表达式，由复杂表达式类型检查器处理 |
| [`DerefExprNode`](src/main/java/ast/AST.java:524) | ComplexExpressionTypeChecker | 解引用表达式，由复杂表达式类型检查器处理 |
| [`TypeCastExprNode`](src/main/java/ast/AST.java:581) | ComplexExpressionTypeChecker | 类型转换表达式，由复杂表达式类型检查器处理 |
| [`ArrayExprNode`](src/main/java/ast/AST.java:622) | ComplexExpressionTypeChecker | 数组表达式，由复杂表达式类型检查器处理 |
| [`StructExprNode`](src/main/java/ast/AST.java:653) | ComplexExpressionTypeChecker | 结构体表达式，由复杂表达式类型检查器处理 |

#### 5.5 控制流表达式节点 (Control Flow Expressions)

| ASTNode类型 | 负责的检查器 | 说明 |
|-------------|--------------|------|
| [`BlockExprNode`](src/main/java/ast/AST.java:804) | ControlFlowTypeChecker | 块表达式，由控制流类型检查器处理 |
| [`IfExprNode`](src/main/java/ast/AST.java:817) | ControlFlowTypeChecker | if表达式，由控制流类型检查器处理 |
| [`LoopExprNode`](src/main/java/ast/AST.java:833) | ControlFlowTypeChecker | 循环表达式，由控制流类型检查器处理 |
| [`BreakExprNode`](src/main/java/ast/AST.java:753) | ControlFlowTypeChecker | break表达式，由控制流类型检查器处理 |
| [`ContinueExprNode`](src/main/java/ast/AST.java:734) | ControlFlowTypeChecker | continue表达式，由控制流类型检查器处理 |
| [`ReturnExprNode`](src/main/java/ast/AST.java:773) | ControlFlowTypeChecker | return表达式，由控制流类型检查器处理 |

### 6. 特殊节点 (Special Nodes)

| ASTNode类型 | 负责的检查器 | 说明 |
|-------------|--------------|------|
| [`BuiltinFunctionNode`](src/main/java/ast/AST.java:922) | StatementTypeChecker | 内置函数，由StatementTypeChecker处理 |

## TypeChecker协调流程

1. **TypeChecker** 作为主入口点，接收所有ASTNode的访问请求
2. 对于表达式节点，委托给 **ExpressionTypeChecker** 或 **ExpressionTypeCheckerRefactored**
3. 对于语句节点，委托给 **StatementTypeChecker**
4. **ExpressionTypeCheckerRefactored** 将表达式节点进一步分发给专门的检查器：
   - 简单表达式 → **SimpleExpressionTypeChecker**
   - 运算符表达式 → **OperatorExpressionTypeChecker**
   - 复杂表达式 → **ComplexExpressionTypeChecker**
   - 控制流表达式 → **ControlFlowTypeChecker**

## 重构前后对比

### 重构前 (ExpressionTypeChecker)
- 单一大类处理所有表达式类型检查
- 代码量大，职责不够单一
- 难以维护和扩展

### 重构后 (ExpressionTypeCheckerRefactored + 专门检查器)
- 职责分离，每个检查器处理特定类型的表达式
- 代码更清晰，易于维护
- 便于添加新的表达式类型检查逻辑

## 注意事项

1. 某些ASTNode可能被多个检查器处理，例如：
   - [`PathExprNode`](src/main/java/ast/AST.java:447) 在简单表达式和控制流表达式中都有处理
   - [`ExprStmtNode`](src/main/java/ast/AST.java:79) 在表达式和语句检查器中都有处理

2. 重构后的系统保持了向后兼容性，原有的 **ExpressionTypeChecker** 仍然可用

3. **TypeExtractor** 和 **ConstantEvaluator** 作为辅助类，被所有检查器使用

4. 错误处理统一通过 **TypeErrorCollector** 进行收集和报告