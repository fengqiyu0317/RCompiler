# NamespaceScope使用方法文档

## 概述

本文档详细说明如何使用NamespaceScope功能来跟踪每个AST节点在visit函数结束时的命名空间状态。这个功能为RCompiler提供了强大的命名空间状态跟踪能力，使得语义检查过程更加透明和可调试。

## 重要设计变更

根据最新设计要求，`setNamespaceScopeForNode`操作不应该在`VisitorBase`中实现，因为此时还没有求出NamespaceScope。相反，这个操作应该在每个节点类型的visit函数末尾添加，在namespace check阶段求得NamespaceScope后设置。

## 核心组件

### 1. NamespaceScope类

NamespaceScope类是核心组件，用于表示命名空间状态：

```java
public class NamespaceScope {
    // 当前作用域的符号表引用
    private final NamespaceSymbolTable symbolTable;
    
    // 作用域级别
    private final int scopeLevel;
    
    // 当前类型名称（在类型内部时使用）
    private final String currentTypeName;
    
    // 当前上下文类型
    private final ContextType contextType;
    
    // 父节点的作用域（用于调试）
    private final NamespaceScope parentScope;
    
    // 时间戳（用于调试和跟踪）
    private final long timestamp;
    
    // 构造函数和方法...
}
```

### 2. ASTNode中的集成

每个ASTNode现在都有一个namespacescope属性：

```java
abstract class ASTNode {
    // 现有属性...
    protected ASTNode father;
    
    // 新增：命名空间状态
    private NamespaceScope namespaceScope;
    
    // 获取和设置方法...
    public NamespaceScope getNamespaceScope() {
        return namespaceScope;
    }
    
    public void setNamespaceScope(NamespaceScope namespaceScope) {
        this.namespaceScope = namespaceScope;
    }
    
    public boolean hasNamespaceScope() {
        return namespaceScope != null;
    }
    
    // 调试方法...
    public void debugPrintNamespaceScope() {
        if (namespaceScope != null) {
            System.out.println("Node: " + this.getClass().getSimpleName());
            namespaceScope.debugPrint();
        } else {
            System.out.println("Node: " + this.getClass().getSimpleName() + " has no namespace scope");
        }
    }
}
```

### 3. 在NamespaceCheck实现中的使用

`setNamespaceScopeForNode`方法应该在namespace check的具体实现类中定义：

```java
public class NamespaceCheckVisitor extends VisitorBase {
    // 现有属性和方法...
    
    // 在namespace check阶段设置节点的NamespaceScope
    private void setNamespaceScopeForNode(ASTNode node) {
        if (node != null) {
            // 创建新的命名空间状态
            NamespaceScope newScope = new NamespaceScope(
                currentScope,              // 当前符号表
                currentScope.getScopeLevel(), // 当前作用域级别
                currentTypeName,            // 当前类型名称
                getContextType(),           // 当前上下文类型
                node.getNamespaceScope()    // 父节点的作用域（如果存在）
            );
            
            // 设置到节点
            node.setNamespaceScope(newScope);
        }
    }
    
    // 获取当前上下文类型的辅助方法
    private ContextType getContextType() {
        switch (currentContext) {
            case TYPE_CONTEXT:
                return ContextType.TYPE_CONTEXT;
            case VALUE_CONTEXT:
                return ContextType.VALUE_CONTEXT;
            case FIELD_CONTEXT:
                return ContextType.FIELD_CONTEXT;
            case LET_PATTERN_CONTEXT:
                return ContextType.LET_PATTERN_CONTEXT;
            case PARAMETER_PATTERN_CONTEXT:
                return ContextType.PARAMETER_PATTERN_CONTEXT;
            default:
                return ContextType.VALUE_CONTEXT; // 默认值上下文
        }
    }
}
```

## 在语义分析器中的使用

### 1. 实现抽象方法

在具体的语义分析器中，需要实现VisitorBase中的抽象方法：

```java
public class SemanticAnalyzer extends VisitorBase {
    // 现有属性...
    private NamespaceSymbolTable currentScope;
    private String currentTypeName;
    private ContextType currentContextType;
    
    // 实现抽象方法
    @Override
    protected Object getCurrentSymbolTable() {
        return currentScope;
    }
    
    @Override
    protected int getCurrentScopeLevel() {
        return currentScope.getScopeLevel();
    }
    
    @Override
    protected String getCurrentTypeName() {
        return currentTypeName;
    }
    
    @Override
    protected ContextType getCurrentContextType() {
        return currentContextType;
    }
    
    // 状态保存和恢复的具体实现
    @Override
    protected void saveCurrentNamespaceState() {
        // 保存当前状态到栈中
        namespaceStateStack.push(new NamespaceStateSnapshot(
            currentScope, currentTypeName, currentContextType));
    }
    
    @Override
    protected void restoreNamespaceState() {
        // 从栈中恢复状态
        NamespaceStateSnapshot snapshot = namespaceStateStack.pop();
        currentScope = snapshot.getScope();
        currentTypeName = snapshot.getTypeName();
        currentContextType = snapshot.getContextType();
    }
}
```

### 2. 在visit方法中的使用模式

每个visit方法都遵循相同的模式：

```java
@Override
public void visit(FunctionNode node) {
    // 保存当前状态
    saveCurrentNamespaceState();
    
    try {
        // 处理节点内容...
        if (node.name == null) reportNullError("FunctionNode", "name");
        else node.name.accept(this);
        
        if (node.selfPara != null) node.selfPara.accept(this);
        
        if (node.parameters != null) {
            for (int i = 0; i < node.parameters.size(); i++) {
                node.parameters.get(i).accept(this);
            }
        }
        
        if (node.returnType != null) node.returnType.accept(this);
        if (node.body != null) node.body.accept(this);
        
        // 设置此节点的命名空间状态
        setNamespaceScopeForNode(node);
    } finally {
        // 恢复状态
        restoreNamespaceState();
    }
}
```

## 调试和诊断

### 1. 调试单个节点

```java
// 调试特定节点的命名空间状态
if (node.hasNamespaceScope()) {
    node.debugPrintNamespaceScope();
}
```

### 2. 遍历所有节点的命名空间状态

```java
// 创建一个访问者来打印所有节点的命名空间状态
public class NamespaceScopePrinter extends VisitorBase {
    @Override
    public void visit(ASTNode node) {
        if (node.hasNamespaceScope()) {
            node.debugPrintNamespaceScope();
        }
        // 继续遍历子节点...
    }
}

// 使用方法
NamespaceScopePrinter printer = new NamespaceScopePrinter();
astRoot.accept(printer);
```

### 3. 获取作用域路径

```java
// 获取节点的作用域路径
if (node.hasNamespaceScope()) {
    NamespaceScope scope = node.getNamespaceScope();
    System.out.println("Scope path: " + scope.getScopePath());
}
```

## 高级用法

### 1. 命名空间状态比较

```java
// 比较两个节点的命名空间状态
public boolean compareNamespaceScopes(ASTNode node1, ASTNode node2) {
    if (!node1.hasNamespaceScope() || !node2.hasNamespaceScope()) {
        return false;
    }
    
    NamespaceScope scope1 = node1.getNamespaceScope();
    NamespaceScope scope2 = node2.getNamespaceScope();
    
    return scope1.getScopeLevel() == scope2.getScopeLevel() &&
           Objects.equals(scope1.getCurrentTypeName(), scope2.getCurrentTypeName()) &&
           scope1.getContextType() == scope2.getContextType();
}
```

### 2. 命名空间状态过滤

```java
// 查找特定上下文中的所有节点
public List<ASTNode> findNodesInContext(ASTNode root, ContextType targetContext) {
    List<ASTNode> result = new ArrayList<>();
    
    VisitorBase collector = new VisitorBase() {
        @Override
        public void visit(ASTNode node) {
            if (node.hasNamespaceScope()) {
                NamespaceScope scope = node.getNamespaceScope();
                if (scope.getContextType() == targetContext) {
                    result.add(node);
                }
            }
            // 继续遍历...
        }
    };
    
    root.accept(collector);
    return result;
}
```

## 性能考虑

### 1. 内存管理

- NamespaceScope对象是轻量级的，只包含基本类型和引用
- 在语义检查完成后，可以清理命名空间状态以释放内存
- 考虑使用对象池来重用NamespaceScope对象

### 2. 性能优化

```java
// 缓存常用的NamespaceScope配置
private static final Map<ScopeConfig, NamespaceScope> scopeCache = new HashMap<>();

// 在setNamespaceScopeForNode中使用缓存
protected void setNamespaceScopeForNode(ASTNode node) {
    if (node != null) {
        ScopeConfig config = new ScopeConfig(
            getCurrentScopeLevel(), getCurrentTypeName(), getCurrentContextType());
        
        NamespaceScope newScope = scopeCache.computeIfAbsent(config, k -> 
            new NamespaceScope(getCurrentSymbolTable(), k.scopeLevel, 
                           k.typeName, k.contextType, currentNamespaceScope));
        
        node.setNamespaceScope(newScope);
        currentNamespaceScope = newScope;
    }
}
```

## 最佳实践

### 1. 一致性

- 所有visit方法都应遵循相同的模式：保存状态 → 处理 → 设置命名空间 → 恢复状态
- 确保在所有代码路径中都调用setNamespaceScopeForNode
- 在异常情况下也要确保状态恢复（使用try-finally）

### 2. 调试友好

- 为复杂的命名空间状态提供有意义的调试信息
- 使用时间戳来跟踪状态变化的时间顺序
- 提供作用域路径来理解嵌套关系

### 3. 扩展性

- 设计NamespaceScope时考虑未来可能需要的额外信息
- 使用抽象方法而不是具体实现，便于不同类型的分析器
- 提供钩子方法允许子类自定义行为

## 总结

NamespaceScope功能为RCompiler提供了强大的命名空间状态跟踪能力，使得：

1. **状态透明**：每个节点的命名空间状态都可以查询和调试
2. **错误定位**：可以精确定位问题发生的命名空间上下文
3. **分析支持**：支持基于命名空间状态的代码分析和优化
4. **调试辅助**：提供丰富的调试信息来理解语义检查过程

通过正确使用这些功能，可以大大提高Rust编译器的语义检查能力和可维护性。