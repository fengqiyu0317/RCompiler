# Namespace两阶段处理分析

## 概述

本文档描述了RCompiler中namespace分析的两阶段处理方法的实现，该方法解决了原有实现中符号定义顺序依赖的问题。

## 问题背景

在原有的实现中，NamespaceAnalyzer同时处理符号添加和符号检查，这导致了以下问题：

1. **符号定义顺序问题**：在同一个作用域内，后面定义的符号可以被前面的引用使用，这不符合Rust的语义规则。
2. **复杂的Context切换**：代码中频繁切换Context，使得逻辑难以理解和维护。
3. **职责混合**：符号添加和符号检查的逻辑混合在一起，增加了代码复杂性。

## 两阶段处理架构

### 设计原则

将namespace分析分为两个独立的阶段：

1. **第一阶段（SymbolAdder）**：只负责添加所有符号到符号表
2. **第二阶段（SymbolChecker）**：只负责检查所有符号引用

### 架构优势

1. **清晰的职责分离**：符号添加和符号检查完全分离
2. **正确的语义处理**：确保在同一作用域内，必须先定义后使用
3. **简化的逻辑**：每个阶段只关注自己的任务，减少Context切换
4. **易于维护**：问题定位更容易，代码结构更清晰

### 处理流程

```
AST → SymbolAdder → 完整的符号表 → SymbolChecker → 验证通过的AST
```

## 实现细节

### SymbolAdder类

**职责**：
- 添加所有符号声明到符号表
- 不进行任何符号查找或验证
- 建立完整的符号表结构

**处理节点类型**：
- 函数声明（FunctionNode）
- 结构体声明（StructNode）
- 枚举声明（EnumNode）
- 常量声明（ConstItemNode）
- 特征声明（TraitNode）
- 实现声明（ImplNode）
- Let语句（LetStmtNode）
- 参数声明（ParameterNode）
- 字段声明（FieldNode）

**关键特性**：
- 只处理符号声明，不处理符号引用
- 维护作用域层次结构
- 处理内置类型和函数
- 维护scope栈以支持两阶段分析之间的scope同步

### SymbolChecker类

**职责**：
- 检查所有符号引用的有效性
- 不添加任何新符号到符号表
- 使用SymbolAdder建立的完整符号表

**处理节点类型**：
- 标识符引用（IdentifierNode）
- 类型引用（TypePathExprNode）
- 字段访问（FieldExprNode）
- 方法调用（MethodCallExprNode）
- 结构体表达式（StructExprNode）
- 各种表达式类型

**关键特性**：
- 只进行符号查找和验证
- 报告未定义的符号
- 维护Context状态
- 使用SymbolAdder提供的scope栈确保scope一致性

### NamespaceAnalyzer类

**职责**：
- 作为主控制器，协调两个阶段的执行
- 提供统一的接口给外部调用者
- 管理符号表的生命周期

**关键方法**：
- `initializeGlobalScope()`：初始化全局作用域并同步scope栈
- `analyze(ASTNode root)`：执行两阶段分析
- `getGlobalScope()`：获取全局符号表

## 使用示例

```java
// 创建namespace分析器
NamespaceAnalyzer analyzer = new NamespaceAnalyzer();

// 初始化全局作用域
analyzer.initializeGlobalScope();

// 执行两阶段分析
analyzer.parseTree(root);

// 获取符号表
NamespaceSymbolTable symbolTable = analyzer.getGlobalScope();
```

## 语义规则保证

### 作用域规则
1. **全局作用域**：包含所有顶级声明
2. **局部作用域**：函数、块等创建的子作用域
3. **作用域继承**：内层作用域可以访问外层作用域的符号

### 符号可见性
1. **先定义后使用**：在同一作用域内，符号必须先声明才能使用
2. **作用域查找**：从当前作用域开始，逐层向外查找
3. **命名空间分离**：类型、值、字段分别管理

### 错误检测
1. **未定义符号**：引用了未声明的符号
2. **重复定义**：在同一作用域内重复定义同名符号
3. **类型错误**：类型引用不匹配或不存在
4. **上下文错误**：在不适当的上下文中使用符号

## 性能考虑

### 时间复杂度
- **符号添加阶段**：O(n)，n为AST节点数
- **符号检查阶段**：O(n)，n为AST节点数
- **总体复杂度**：O(n)，线性时间复杂度

### 空间复杂度
- **符号表大小**：O(n)，n为符号总数
- **作用域栈深度**：O(d)，d为最大嵌套深度
- **总体空间**：O(n+d)，合理的内存使用

## 扩展性

### 新增符号类型
1. 在SymbolAdder中添加新的visit方法
2. 在SymbolChecker中添加对应的检查逻辑
3. 更新SymbolKind枚举（如需要）

### 新增AST节点
1. 在两个类中都添加相应的visit方法
2. 确保符号添加和检查的一致性
3. 维护向后兼容性

## 测试策略

### 单元测试
1. 测试符号添加的正确性
2. 测试符号检查的有效性
3. 测试错误情况的报告

### 集成测试
1. 测试完整的两阶段流程
2. 测试复杂的嵌套结构
3. 测试边界情况

## 总结

## Scope管理机制

### 问题背景
在初始的两阶段实现中，存在一个关键问题：SymbolChecker在第二阶段创建了自己的scope层次结构，而不是使用SymbolAdder在第一阶段创建的scope层次结构。这导致：
1. SymbolChecker无法访问SymbolAdder添加的符号
2. 两阶段之间的scope不一致
3. 符号查找失败，导致语义检查错误

### 解决方案
实现了scope栈同步机制：

1. **SymbolAdder中的scope栈跟踪**：
   - 添加了`scopeStack`字段来跟踪scope层次结构
   - 修改了`enterScope()`和`exitScope()`方法来维护scope栈
   - 提供了`getScopeStack()`方法供SymbolChecker使用

2. **SymbolChecker中的scope同步**：
   - 添加了接受scope栈的构造函数
   - 修改了`enterScope()`和`exitScope()`方法，使其使用scope栈而不是创建新的scope
   - 确保SymbolChecker使用与SymbolAdder相同的scope层次结构

3. **NamespaceAnalyzer中的协调**：
   - 修改了`initializeGlobalScope()`方法，使其在创建SymbolChecker时传递scope栈

### 实现细节

#### SymbolAdder的scope栈管理
```java
// 在SymbolAdder中
private java.util.Stack<NamespaceSymbolTable> scopeStack;

private void enterScope() {
    currentScope = currentScope.enterScope();
    scopeStack.push(currentScope);
}

private void exitScope() {
    currentScope = currentScope.exitScope();
    scopeStack.pop();
}
```

#### SymbolChecker的scope同步
```java
// 在SymbolChecker中
public SymbolChecker(NamespaceSymbolTable globalScope, java.util.Stack<NamespaceSymbolTable> scopeStack) {
    this.globalScope = globalScope;
    this.currentScope = globalScope;
    this.scopeStack = new java.util.Stack<>();
    this.scopeStack.addAll(scopeStack);
    if (!this.scopeStack.isEmpty()) {
        this.currentScope = this.scopeStack.peek();
    }
}

private void enterScope() {
    if (!scopeStack.isEmpty()) {
        currentScope = scopeStack.peek();
        currentScope = currentScope.enterScope();
        scopeStack.push(currentScope);
    }
}
```

### 优势
1. **Scope一致性**：确保两个阶段使用相同的scope层次结构
2. **符号访问**：SymbolChecker可以正确访问SymbolAdder添加的符号
3. **错误减少**：避免了因scope不一致导致的语义检查错误
4. **维护性**：scope管理逻辑更加清晰和可维护

## 总结

两阶段处理方法通过将符号添加和符号检查分离，解决了原有实现中的顺序依赖问题，提供了更清晰、更正确的namespace分析。scope栈同步机制的引入进一步确保了两个阶段之间的scope一致性，使SymbolChecker能够正确访问SymbolAdder添加的符号。这种方法不仅符合Rust的语义规则，还提高了代码的可维护性和扩展性。