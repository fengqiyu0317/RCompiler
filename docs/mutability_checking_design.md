# Mutability检查设计文档

## 概述

本文档描述了RCompiler中mutability（可变性）检查的设计和实现。在Rust中，mutability是一个核心概念，它决定了变量和引用是否可以被修改。正确实现mutability检查对于确保内存安全和防止数据竞争至关重要。

## Rust中的Mutability规则

### 1. 变量绑定

在Rust中，变量绑定默认是不可变的（immutable）：

```rust
let x = 5;        // 不可变绑定
let mut y = 5;     // 可变绑定
x = 10;           // 错误：不能修改不可变变量
y = 10;           // 正确：可以修改可变变量
```

### 2. 引用

引用可以是可变的或不可变的：

```rust
let x = 5;
let y = 5;
let r1 = &x;      // 不可变引用
let r2 = &mut y;   // 可变引用
let r3 = &x;      // 错误：不能从不可变变量创建可变引用
```

### 3. 借用规则

Rust的借用规则确保内存安全：

1. 在任意给定时间，要么只能有一个可变引用，要么只能有多个不可变引用。
2. 引用必须总是有效的。

### 4. 模式匹配中的mutability

在模式匹配中，可以指定绑定的可变性：

```rust
let (x, y) = (5, 5);           // 两个都是不可变
let (mut x, y) = (5, 5);       // x是可变的，y是不可变的
let &(ref x, ref mut y) = &(5, 5); // x是不可变引用，y是可变引用
```

## 现有实现分析

### 1. AST节点中的mutability支持

项目中已经有一些mutability相关的支持：

1. **SelfParaNode**: 包含`isMutable`字段，表示self参数是否可变
2. **IdPatNode**: 包含`isMutable`字段，表示标识符模式是否可变
3. **RefPatNode**: 包含`isMutable`字段，表示引用模式是否可变
4. **BorrowExprNode**: 包含`isMutable`字段，表示借用表达式是否可变
5. **TypeRefExprNode**: 包含`isMutable`字段，表示引用类型是否可变

### 2. 类型系统中的mutability支持

1. **ReferenceType**: 包含`isMutable`字段，表示引用是否可变
2. **Symbol**: 包含`isMutable`字段，表示符号是否可变

### 3. 解析器中的mutability支持

解析器已经能够识别`mut`关键字，并在相应的AST节点中设置`isMutable`标志。

## Mutability检查架构设计

### 1. MutabilityChecker类

创建一个新的`MutabilityChecker`类，负责执行mutability检查：

```java
public class MutabilityChecker extends VisitorBase {
    private final TypeErrorCollector errorCollector;
    private final boolean throwOnError;
    
    // 上下文信息，用于跟踪当前作用域的mutability状态
    private final Stack<MutabilityContext> contextStack = new Stack<>();
    
    public MutabilityChecker(TypeErrorCollector errorCollector, boolean throwOnError) {
        this.errorCollector = errorCollector;
        this.throwOnError = throwOnError;
    }
    
    // 检查方法
    public void checkMutability(ASTNode node) {
        node.accept(this);
    }
    
    // 各种visit方法实现...
}
```

### 2. MutabilityContext类

创建一个`MutabilityContext`类，用于跟踪当前作用域的mutability状态：

```java
public static class MutabilityContext {
    private final Map<String, Boolean> variableMutability = new HashMap<>();
    private final Set<String> mutableBorrows = new HashSet<>();
    private final Set<String> immutableBorrows = new HashSet<>();
    
    // 检查变量是否可变
    public boolean isVariableMutable(String variableName) {
        return variableMutability.getOrDefault(variableName, false);
    }
    
    // 设置变量的可变性
    public void setVariableMutability(String variableName, boolean isMutable) {
        variableMutability.put(variableName, isMutable);
    }
    
    // 添加可变借用
    public void addMutableBorrow(String variableName) {
        mutableBorrows.add(variableName);
    }
    
    // 添加不可变借用
    public void addImmutableBorrow(String variableName) {
        immutableBorrows.add(variableName);
    }
    
    // 检查是否可以创建可变借用
    public boolean canCreateMutableBorrow(String variableName) {
        // 如果变量不可变，则不能创建可变借用
        if (!isVariableMutable(variableName)) {
            return false;
        }
        
        // 如果变量已经有任何借用，则不能创建新的借用
        return !mutableBorrows.contains(variableName) && !immutableBorrows.contains(variableName);
    }
    
    // 检查是否可以创建不可变借用
    public boolean canCreateImmutableBorrow(String variableName) {
        // 如果变量已经有可变借用，则不能创建不可变借用
        return !mutableBorrows.contains(variableName);
    }
    
    // 释放借用
    public void releaseBorrow(String variableName) {
        mutableBorrows.remove(variableName);
        immutableBorrows.remove(variableName);
    }
}
```

### 3. 集成到现有类型检查系统

将mutability检查集成到现有的类型检查系统中：

1. **在StatementTypeChecker中添加mutability检查**：
   - 在`visit(LetStmtNode node)`中检查变量绑定的mutability
   - 在`visit(AssignExprNode node)`中检查赋值操作的mutability

2. **在ExpressionTypeChecker中添加mutability检查**：
   - 在`visit(BorrowExprNode node)`中检查借用表达式的mutability
   - 在`visit(DerefExprNode node)`中检查解引用操作的mutability

3. **在BaseExpressionTypeChecker中添加mutability检查辅助方法**：
   - 添加`checkMutableAccess(ExprNode expr)`方法，检查对可变值的访问
   - 添加`checkMutableAssignment(ExprNode target, ExprNode value)`方法，检查对可变目标的赋值

### 4. 错误报告

定义mutability相关的错误类型和消息：

```java
public class MutabilityError {
    public static final String IMMUTABLE_VARIABLE_ASSIGNMENT = 
        "Cannot assign to immutable variable '%s'";
    
    public static final String MUTABLE_BORROW_OF_IMMUTABLE_VARIABLE = 
        "Cannot create mutable borrow of immutable variable '%s'";
    
    public static final String MULTIPLE_MUTABLE_BORROWS = 
        "Cannot create multiple mutable borrows of variable '%s'";
    
    public static final String MUTABLE_BORROW_WITH_EXISTING_BORROW = 
        "Cannot create mutable borrow of variable '%s' when it already has active borrows";
    
    public static final String IMMUTABLE_BORROW_WITH_MUTABLE_BORROW = 
        "Cannot create immutable borrow of variable '%s' when it already has a mutable borrow";
}
```

## 实现计划

### 阶段1：基础架构
1. 创建`MutabilityChecker`类
2. 创建`MutabilityContext`类
3. 定义mutability错误消息

### 阶段2：语句检查
1. 在`StatementTypeChecker`中集成mutability检查
2. 实现let语句的mutability检查
3. 实现赋值语句的mutability检查

### 阶段3：表达式检查
1. 在`ExpressionTypeChecker`中集成mutability检查
2. 实现借用表达式的mutability检查
3. 实现解引用表达式的mutability检查

### 阶段4：测试和验证
1. 创建mutability检查的测试用例
2. 验证现有测试用例的mutability检查
3. 性能优化和错误处理改进

## 与现有系统的集成点

1. **TypeChecker**: 在主类型检查器中添加mutability检查步骤
2. **StatementTypeChecker**: 在语句类型检查中添加mutability验证
3. **ExpressionTypeChecker**: 在表达式类型检查中添加mutability验证
4. **ErrorCollector**: 扩展错误收集器以支持mutability错误

## 注意事项

1. **性能考虑**: mutability检查不应该显著影响编译性能
2. **错误恢复**: mutability错误不应该阻止其他类型检查的进行
3. **向后兼容**: 新的mutability检查应该与现有的类型检查系统兼容
4. **可扩展性**: 设计应该允许将来添加更复杂的mutability规则

## 结论

通过实现这个mutability检查架构，RCompiler将能够正确验证Rust代码中的mutability规则，提高代码的安全性和可靠性。这个设计充分利用了现有的类型检查系统，并提供了清晰的扩展点，以便将来添加更复杂的mutability检查规则。