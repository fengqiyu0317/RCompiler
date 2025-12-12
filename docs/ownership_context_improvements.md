# OwnershipContext 改进总结

## 问题描述

原始的 `OwnershipChecker` 实现中，`OwnershipContext` 在进入新作用域时会继承父亲的内容，但在退出作用域时（回退）并没有将内容传回给父亲。这导致以下问题：

1. 子作用域中对变量状态的修改（如移动、借用等）不会反映到父作用域
2. 在父作用域中可能错误地认为变量仍然可用，而实际上它已在子作用域中被移动

## 解决方案

### 1. 修改 exitScope() 方法

在退出作用域时，将当前上下文的变量状态传回给父上下文：

```java
private void exitScope() {
    if (!contextStack.isEmpty()) {
        OwnershipContext currentContext = contextStack.pop();
        
        // 将当前上下文的变量状态传回给父上下文
        if (!contextStack.isEmpty()) {
            OwnershipContext parentContext = contextStack.peek();
            currentContext.mergeToParent(parentContext);
        }
    }
}
```

### 2. 添加 mergeToParent() 方法

在 `OwnershipContext` 类中添加 `mergeToParent` 方法，用于将当前上下文的变量状态合并到父上下文中：

```java
public void mergeToParent(OwnershipContext parentContext) {
    if (parentContext == null) return;
    
    // 合并变量状态
    for (Map.Entry<String, VariableOwnership> entry : variables.entrySet()) {
        String varName = entry.getKey();
        VariableOwnership currentOwnership = entry.getValue();
        
        // 检查父上下文中是否已有该变量
        VariableOwnership parentOwnership = parentContext.variables.get(varName);
        
        if (parentOwnership != null) {
            // 如果父上下文中已有该变量，更新其状态
            // 主要更新移动状态，因为可变性是固定的
            if (currentOwnership.isMoved()) {
                parentOwnership.setMoved(true);
            }
        } else {
            // 如果父上下文中没有该变量，说明这是在子作用域中声明的变量
            // 不需要添加到父上下文中，因为变量的作用域仅限于其声明的作用域
        }
    }
    
    // 注意：借用状态（mutableBorrows 和 immutableBorrows）通常不需要合并
    // 因为借用的生命周期通常不超过其所在的作用域
    // 如果需要支持跨作用域的借用，可以在这里添加相应的逻辑
}
```

### 3. 改进 OwnershipContext 构造函数

修改从父上下文继承的构造函数，使用深拷贝而不是浅拷贝：

```java
public OwnershipContext(OwnershipContext parent) {
    if (parent != null) {
        // 继承父上下文的变量状态
        // 注意：这里需要深拷贝 VariableOwnership 对象，而不是直接复制引用
        // 否则修改子上下文中的变量状态会直接影响父上下文
        for (Map.Entry<String, VariableOwnership> entry : parent.variables.entrySet()) {
            String varName = entry.getKey();
            VariableOwnership parentOwnership = entry.getValue();
            
            // 使用拷贝构造函数创建新的 VariableOwnership 对象
            VariableOwnership childOwnership = new VariableOwnership(parentOwnership);
            
            variables.put(varName, childOwnership);
        }
    }
}
```

### 4. 添加 VariableOwnership 拷贝构造函数

为 `VariableOwnership` 类添加拷贝构造函数：

```java
/**
 * 拷贝构造函数
 */
public VariableOwnership(VariableOwnership other) {
    this.varName = other.varName;
    this.isMutable = other.isMutable;
    this.isMoved = other.isMoved;
}
```

### 5. 改进 IfExprNode 和 LoopExprNode 的访问方法

确保 if 表达式的各个分支和循环体有正确的作用域处理：

```java
@Override
public void visit(IfExprNode node) {
    // 处理条件（在当前作用域中）
    if (node.condition != null) {
        node.condition.accept(this);
    }
    
    // 处理then分支（在新的作用域中）
    if (node.thenBranch != null) {
        enterScope();
        try {
            node.thenBranch.accept(this);
        } finally {
            exitScope();
        }
    }
    
    // 处理else分支（在新的作用域中）
    if (node.elseBranch != null) {
        enterScope();
        try {
            node.elseBranch.accept(this);
        } finally {
            exitScope();
        }
    }
    
    // 处理elseif分支（在新的作用域中）
    if (node.elseifBranch != null) {
        enterScope();
        try {
            node.elseifBranch.accept(this);
        } finally {
            exitScope();
        }
    }
}
```

## 效果

这些修改确保了：

1. **正确的状态传播**：子作用域中的变量状态变化（特别是移动状态）能够正确传播到父作用域
2. **作用域隔离**：子作用域中声明的变量不会影响父作用域
3. **深拷贝保护**：子作用域对变量状态的修改不会意外影响父作用域
4. **正确的借用处理**：借用状态在作用域退出时被正确清理

## 测试用例

创建了一个测试用例 `test_ownership_context.rs` 来验证这些修改，包括：

1. 基本作用域测试
2. if 表达式的作用域测试
3. 循环作用域测试

这些测试用例可以帮助验证所有权检查器在各种作用域场景下的正确行为。