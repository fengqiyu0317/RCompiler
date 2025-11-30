# NamespaceScope设计文档

## 概述

NamespaceScope类用于表示每个AST节点在visit函数结束时的命名空间状态。这个类将作为ASTNode的一个属性，用于跟踪节点在语义检查过程中的命名空间上下文。

## 设计目标

1. **状态跟踪**：记录节点在visit函数结束时的命名空间状态
2. **上下文保存**：保存节点所在的作用域信息
3. **符号表引用**：关联到当前的符号表状态
4. **调试支持**：提供调试和诊断信息

## 类设计

### NamespaceScope类

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
    
    // 构造函数
    public NamespaceScope(NamespaceSymbolTable symbolTable, int scopeLevel, 
                       String currentTypeName, ContextType contextType, 
                       NamespaceScope parentScope) {
        this.symbolTable = symbolTable;
        this.scopeLevel = scopeLevel;
        this.currentTypeName = currentTypeName;
        this.contextType = contextType;
        this.parentScope = parentScope;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getter方法
    public NamespaceSymbolTable getSymbolTable() { return symbolTable; }
    public int getScopeLevel() { return scopeLevel; }
    public String getCurrentTypeName() { return currentTypeName; }
    public ContextType getContextType() { return contextType; }
    public NamespaceScope getParentScope() { return parentScope; }
    public long getTimestamp() { return timestamp; }
    
    // 调试方法
    @Override
    public String toString() {
        return String.format("NamespaceScope{scopeLevel=%d, typeName='%s', context=%s, timestamp=%d}",
                          scopeLevel, currentTypeName, contextType, timestamp);
    }
    
    // 检查是否在特定上下文中
    public boolean isInImplContext() {
        return contextType == ContextType.IMPL_BLOCK;
    }
    
    public boolean isInTraitContext() {
        return contextType == ContextType.TRAIT_BLOCK;
    }
    
    public boolean isInMethodContext() {
        return contextType == ContextType.METHOD;
    }
    
    public boolean isInAssociatedFunctionContext() {
        return contextType == ContextType.ASSOCIATED_FUNC;
    }
    
    public boolean isInFunctionDeclarationContext() {
        return contextType == ContextType.FUNCTION_DECLARATION;
    }
    
    public boolean isInTypeDeclarationContext() {
        return contextType == ContextType.TYPE_DECLARATION;
    }
    
    public boolean isInConstDeclarationContext() {
        return contextType == ContextType.CONST_DECLARATION;
    }
    
    // 获取作用域路径（用于调试）
    public String getScopePath() {
        StringBuilder path = new StringBuilder();
        NamespaceScope current = this;
        
        while (current != null) {
            if (path.length() > 0) {
                path.append(" -> ");
            }
            path.append("Level").append(current.getScopeLevel());
            if (current.getCurrentTypeName() != null) {
                path.append("(").append(current.getCurrentTypeName()).append(")");
            }
            current = current.getParentScope();
        }
        
        return path.toString();
    }
}
```

## 在ASTNode中的集成

### 修改ASTNode基类

```java
abstract class ASTNode {
    // 现有属性...
    protected ASTNode father;
    
    // 新增：命名空间状态
    private NamespaceScope namespaceScope;
    
    // 现有方法...
    public abstract void accept(VisitorBase visitor);
    public ASTNode getFather() { return father; }
    public void setFather(ASTNode father) { this.father = father; }
    
    // 新增：命名空间状态相关方法
    public NamespaceScope getNamespaceScope() { return namespaceScope; }
    
    public void setNamespaceScope(NamespaceScope namespaceScope) { 
        this.namespaceScope = namespaceScope; 
    }
    
    // 检查是否有命名空间状态
    public boolean hasNamespaceScope() {
        return namespaceScope != null;
    }
}
```

## 重要更新：VisitorBase中不应包含NamespaceScope相关操作

### 设计变更说明

根据最新的设计要求，`VisitorBase`类不应该包含任何与`NamespaceScope`相关的变量和操作，因为在`VisitorBase`执行时还没有求出`NamespaceScope`。相反，`setNamespaceScopeForNode`操作应该在namespace check阶段实现，并在每个节点类型的visit函数末尾调用。

### 修改后的VisitorBase类

```java
public abstract class VisitorBase {
    // 现有属性和方法...
    
    // 注意：VisitorBase中不应该包含NamespaceScope相关的变量和操作
    // 这些操作应该在namespace check阶段的具体实现中完成
    
    // 在每个visit方法开始时保存当前状态
    protected void saveCurrentNamespaceState() {
        // 保存当前状态，以便在visit方法结束时恢复
        // 具体实现取决于子类的需求
    }
    
    // 在每个visit方法结束时恢复状态
    protected void restoreNamespaceState() {
        // 恢复之前保存的状态
        // 具体实现取决于子类的需求
    }
}
```

### 在NamespaceCheck实现中的使用

`setNamespaceScopeForNode`方法应该在namespace check的具体实现类中定义，例如：

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
            case CONST_DECLARATION:
                return ContextType.CONST_DECLARATION;
            default:
                return ContextType.VALUE_CONTEXT; // 默认值上下文
        }
    }
}
```

## 使用示例

### 在语义分析器中的使用

```java
public class SemanticAnalyzer extends VisitorBase {
    // 现有属性...
    
    @Override
    public void visit(FunctionNode node) {
        // 保存当前状态
        saveCurrentNamespaceState();
        
        try {
            // 处理函数声明...
            
            // 设置函数节点的命名空间状态
            setNamespaceScopeForNode(node);
            
            // 处理函数体...
            if (node.body != null) {
                node.body.accept(this);
            }
            
        } finally {
            // 恢复状态
            restoreNamespaceState();
        }
    }
    
    @Override
    public void visit(BlockExprNode node) {
        // 进入新作用域
        enterScope();
        
        // 保存当前状态
        saveCurrentNamespaceState();
        
        try {
            // 处理块语句...
            if (node.statements != null) {
                for (StmtNode stmt : node.statements) {
                    stmt.accept(this);
                }
            }
            
            // 处理返回值...
            if (node.returnValue != null) {
                node.returnValue.accept(this);
            }
            
            // 设置块节点的命名空间状态
            setNamespaceScopeForNode(node);
            
        } finally {
            // 恢复状态
            restoreNamespaceState();
            
            // 退出作用域
            exitScope();
        }
    }
}
```

## 调试和诊断

### 调试方法

```java
// 在NamespaceScope中添加调试方法
public void debugPrint() {
    System.out.println("=== NamespaceScope Debug Info ===");
    System.out.println("Scope Level: " + scopeLevel);
    System.out.println("Current Type: " + (currentTypeName != null ? currentTypeName : "null"));
    System.out.println("Context Type: " + contextType);
    System.out.println("Scope Path: " + getScopePath());
    System.out.println("Timestamp: " + new Date(timestamp));
    System.out.println("===============================");
}

// 在ASTNode中添加调试方法
public void debugPrintNamespaceScope() {
    if (namespaceScope != null) {
        System.out.println("Node: " + this.getClass().getSimpleName());
        namespaceScope.debugPrint();
    } else {
        System.out.println("Node: " + this.getClass().getSimpleName() + " has no namespace scope");
    }
}
```

## 注意事项

1. **性能考虑**：NamespaceScope对象应该轻量级，避免创建过多对象
2. **内存管理**：在语义检查完成后，可能需要清理命名空间状态以释放内存
3. **线程安全**：如果有多线程访问，需要考虑线程安全问题
4. **序列化**：如果需要持久化，需要考虑序列化支持

## 重要更新：ContextType枚举扩展

### 新增上下文类型

为了支持更精确的语义分析，ContextType枚举需要扩展以包含新的上下文类型：

```java
public enum ContextType {
    TYPE_CONTEXT,
    VALUE_CONTEXT,
    FIELD_CONTEXT,
    LET_PATTERN_CONTEXT,
    PARAMETER_PATTERN_CONTEXT,
    FUNCTION_DECLARATION,  // 新增：函数声明上下文
    TYPE_DECLARATION,     // 新增：类型声明上下文
    FIELD_DECLARATION,     // 新增：字段声明上下文
    CONST_DECLARATION,     // 新增：常量声明上下文
    IMPL_BLOCK,          // 实现块上下文
    TRAIT_BLOCK,         // Trait块上下文
    METHOD,              // 方法上下文
    ASSOCIATED_FUNC       // 关联函数上下文
}
```

### 上下文类型说明

1. **FUNCTION_DECLARATION**：用于函数声明时的命名空间状态跟踪
2. **TYPE_DECLARATION**：用于类型声明（struct、enum、trait）时的命名空间状态跟踪
3. **FIELD_DECLARATION**：用于字段声明时的命名空间状态跟踪
4. **CONST_DECLARATION**：用于常量声明时的命名空间状态跟踪

这些新上下文类型使得NamespaceScope能够更精确地记录不同声明场景下的命名空间状态。

## 总结

NamespaceScope类为每个AST节点提供了命名空间状态的完整跟踪能力，使得语义检查过程更加透明和可调试。通过在ASTNode中添加namespacescope属性，并在VisitorBase中适当设置这个属性，我们可以准确地记录每个节点在visit函数结束时的命名空间状态。

新增的上下文类型支持更精确的语义分析，使得符号创建和处理更加准确，避免了上下文混淆。这些改进为Rust编译器的语义检查提供了更强大的基础设施。