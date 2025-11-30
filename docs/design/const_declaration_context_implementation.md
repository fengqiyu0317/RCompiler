# Const声明上下文实现文档

## 概述

本文档详细说明了为NamespaceAnalyzer中的ConstItemNode添加const声明上下文的具体实现，确保常量声明遵循与其他声明类型相同的模式，支持递归处理标识符节点。

## 实现目标

1. **统一处理模式**：使const声明处理与function、type等声明保持一致
2. **递归标识符处理**：支持在IdentifierNode中直接创建和注册符号
3. **上下文感知解析**：使用专门的CONST_DECLARATION上下文处理常量声明
4. **符号关联**：建立ConstItemNode、IdentifierNode和Symbol之间的正确关联

## 核心更改

### 1. Context.java更新

添加了新的`CONST_DECLARATION`上下文到Context枚举：

```java
// Context enumeration
public enum Context {
    TYPE_CONTEXT,
    VALUE_CONTEXT,
    FIELD_CONTEXT,
    LET_PATTERN_CONTEXT,  // Special context for let statement patterns
    PARAMETER_PATTERN_CONTEXT,  // Special context for parameter patterns
    FUNCTION_DECLARATION,  // Special context for function declarations
    TYPE_DECLARATION,  // Special context for type declarations (struct, enum, trait)
    FIELD_DECLARATION,  // Special context for field declarations
    CONST_DECLARATION  // Special context for const declarations
}
```

**重要更新**：
- Context枚举现在是一个公共类（public enum Context），而不是私有枚举
- 新增的CONST_DECLARATION上下文使得常量声明处理更加精确和一致
- 所有声明类型现在都有专门的上下文，避免了上下文混淆

### 2. AST.java更新

为ConstItemNode类添加了缺失的symbol字段和相应方法：

```java
// ConstItemNode represents a constant item <constitem>.
// The grammer for constant definition is:
// <constitem> = const <identifier> : <type> (= <expression>)? ;
class ConstItemNode extends ItemNode {
    IdentifierNode name;
    TypeExprNode type;
    ExprNode value; // can be null
    
    // 存储该常量对应的符号
    private Symbol symbol;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
    
    public Symbol getSymbol() {
        return symbol;
    }
    
    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }
}
```

### 3. NamespaceAnalyzer.java更新

#### 3.1 ConstItemNode.visit()方法重构

原来的实现直接创建符号，新的实现使用上下文模式：

```java
// Process constant declaration
@Override
public void visit(ConstItemNode node) {
    // First, recursively process the const name in CONST_DECLARATION context
    Context previousContext = currentContext;
    setContext(Context.CONST_DECLARATION);
    
    try {
        node.name.accept(this);
    } finally {
        // Restore context
        setContext(previousContext);
    }
    
    // Set the const node's symbol to its name's symbol
    // The symbol was created in IdentifierNode's visit method
    if (node.name.getSymbol() != null) {
        node.setSymbol(node.name.getSymbol());
    }
    
    try {
        // Process constant type (if any)
        if (node.type != null) {
            Context typeContext = currentContext;
            setContext(Context.TYPE_CONTEXT);
            
            try {
                node.type.accept(this);
            } finally {
                // Restore context
                setContext(typeContext);
            }
        }
        
        // Process constant value (if any)
        if (node.value != null) {
            node.value.accept(this);
        }
        
    } catch (SemanticException e) {
        throw e;
    }
}
```

#### 3.2 IdentifierNode.visit()方法更新

添加了`CONST_DECLARATION`case处理：

```java
case CONST_DECLARATION:
    // In const declaration context, create and insert the const symbol here
    String constName = identifierName;
    
    // Find the const item node (parent of this identifier)
    ASTNode constItemNode = node.getFather();
    
    // Create const symbol
    Symbol constSymbol = new Symbol(
        constName,
        SymbolKind.CONSTANT,
        constItemNode,
        currentScope.getScopeLevel(),
        false // Constants are immutable
    );
    
    // Add to value namespace
    try {
        currentScope.addValueSymbol(constSymbol);
    } catch (SemanticException e) {
        throw e;
    }
    
    // Set the identifier's symbol to the const symbol
    resolvedSymbol = constSymbol;
    break;
```

## 设计原理

### 1. 上下文驱动处理

使用专门的`CONST_DECLARATION`上下文而不是直接在`ConstItemNode.visit()`中创建符号，有以下优势：

1. **一致性**：与其他声明类型（function、type等）保持相同的处理模式
2. **灵活性**：支持在IdentifierNode中进行上下文感知的符号创建
3. **可扩展性**：未来可以轻松添加更多上下文特定的逻辑

### 2. 递归标识符处理

通过递归调用`node.name.accept(this)`而不是直接访问`node.name.name`，实现了：

1. **符号关联**：自动将创建的符号关联到IdentifierNode
2. **上下文传播**：IdentifierNode能够根据当前上下文执行适当的操作
3. **错误处理**：统一的错误处理机制

### 3. 符号生命周期管理

正确的符号生命周期管理：

1. **创建**：在IdentifierNode.visit()中根据CONST_DECLARATION上下文创建
2. **注册**：将符号添加到当前作用域的值命名空间
3. **关联**：将符号同时关联到IdentifierNode和ConstItemNode
4. **传播**：通过AST节点间的引用关系传播符号信息

## 使用示例

### 基本常量声明

```rust
const MAX_SIZE: i32 = 100;
```

处理流程：
1. `ConstItemNode.visit()`被调用
2. 设置上下文为`CONST_DECLARATION`
3. 递归调用`IdentifierNode.visit()`处理"MAX_SIZE"
4. 在`CONST_DECLARATION`上下文中创建CONSTANT符号
5. 将符号添加到值命名空间
6. 恢复原始上下文
7. 处理类型表达式"i32"
8. 处理值表达式"100"

### 带类型的常量声明

```rust
const DEFAULT_NAME: &str;
```

处理流程：
1. 创建常量符号（名称为"DEFAULT_NAME"）
2. 设置上下文为`TYPE_CONTEXT`处理类型"&str"
3. 恢复上下文

## 错误处理

### 重复声明检测

```rust
const VALUE: i32 = 42;
const VALUE: i32 = 100; // 错误：重复声明
```

当检测到重复声明时：
1. `currentScope.addValueSymbol()`抛出SemanticException
2. 在`IdentifierNode.visit()`中捕获异常
3. 错误处理被移除（根据当前实现）

### 类型错误检测

```rust
const INVALID: undefined_type = 42; // 错误：未定义类型
```

当检测到类型错误时：
1. 在`TYPE_CONTEXT`中处理类型表达式
2. 类型解析失败（在后续的类型检查阶段）
3. 错误处理被移除（根据当前实现）

## 性能考虑

1. **上下文切换开销**：最小化上下文切换，只在必要时进行
2. **符号查找优化**：利用现有的符号表查找机制
3. **内存使用**：避免创建不必要的临时对象

## 未来扩展

1. **常量表达式优化**：支持编译时常量折叠
2. **类型推断**：支持省略类型的常量声明
3. **内联常量**：支持小常量的内联优化

## 总结

通过添加`CONST_DECLARATION`上下文和重构`ConstItemNode.visit()`方法，我们实现了：

1. **统一的声明处理模式**：所有声明类型现在使用相同的上下文驱动模式
2. **递归标识符处理**：支持在IdentifierNode中直接创建和注册符号
3. **正确的符号关联**：建立了AST节点、符号和作用域之间的完整关联关系
4. **一致的错误处理**：使用与其他声明类型相同的错误处理机制

**重要更新**：
- 错误处理现在重新抛出SemanticException，而不是移除错误处理
- 这确保了错误能够正确传播到上层调用者
- 保持了与其他声明类型一致的错误处理方式

这些改进确保了常量声明在NamespaceAnalyzer中得到正确和一致的处理，为后续的类型检查和代码生成阶段提供了准确的符号信息。