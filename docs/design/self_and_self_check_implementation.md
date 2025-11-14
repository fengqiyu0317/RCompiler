# self与Self语义检查实现方法

## 概述

本文档详细说明self与Self语义检查的具体实现方法，基于Rust Reference中的Paths规范，为RCompiler提供完整的实现指导。本实现将集成到现有的语义分析器中，确保self和Self关键字的使用符合Rust语言规范。

## 实现架构

### 1. 上下文跟踪系统

#### 1.1 上下文类型枚举

```java
// 上下文类型枚举
public enum ContextType {
    GLOBAL,           // 全局上下文（包括普通函数内部）
    IMPL_BLOCK,       // impl块上下文
    TRAIT_BLOCK,      // trait块上下文
    METHOD,           // 方法上下文
    ASSOCIATED_FUNC   // 关联函数上下文
}
```

#### 1.2 上下文信息类

```java
// 上下文信息类
public class ContextInfo {
    private final ContextType type;
    private final ContextInfo parent;     // 父上下文
    
    public ContextInfo(ContextType type, ContextInfo parent) {
        this.type = type;
        this.parent = parent;
    }
    
    // Getter方法
    public ContextType getType() { return type; }
    public ContextInfo getParent() { return parent; }
    
    // 检查当前上下文类型
    public boolean isCurrentImplBlock() {
        return type == ContextType.IMPL_BLOCK;
    }
    
    public boolean isCurrentTraitBlock() {
        return type == ContextType.TRAIT_BLOCK;
    }
    
    public boolean isCurrentMethod() {
        return type == ContextType.METHOD;
    }
    
    public boolean isCurrentAssociatedFunction() {
        return type == ContextType.ASSOCIATED_FUNC;
    }
    
    // 检查是否在可以使用Self的上下文中（仅当前上下文）
    public boolean canUseSelfType() {
        return isCurrentImplBlock() || isCurrentTraitBlock() || isCurrentMethod() || isCurrentAssociatedFunction();
    }
    
    // 检查是否在可以使用self的上下文中（仅当前上下文）
    public boolean canUseSelfValue() {
        return isCurrentMethod();  // 只有在 METHOD 上下文中才能使用 self
    }
}
```

#### 1.3 嵌套上下文处理

在 Rust 中，impl 块或 trait 块的方法内部可能定义嵌套函数（如闭包）。需要注意的是，嵌套函数内部**不能**使用 `Self` 和 `self`，因为它们有自己的作用域，不能访问外部方法的上下文。

因此，我们只需要检查当前上下文，不需要遍历父上下文：

1. **当前上下文检查**：
   - `isCurrentImplBlock()`, `isCurrentTraitBlock()` 等：只检查当前上下文的类型
   - `canUseSelfType()`, `canUseSelfValue()`：只检查当前上下文是否允许使用

2. **移除嵌套上下文检查**：
   - 不再需要 `canUseSelfTypeInNestedContext()` 和 `canUseSelfValueInNestedContext()` 方法
   - 不再需要遍历上下文链

```java
// 检查是否在可以使用Self的上下文中（仅当前上下文）
public boolean canUseSelfType() {
    return isCurrentImplBlock() || isCurrentTraitBlock() || isCurrentMethod() || isCurrentAssociatedFunction();
}

// 检查是否在可以使用self的上下文中（仅当前上下文）
public boolean canUseSelfValue() {
    return isCurrentMethod();  // 只有在 METHOD 上下文中才能使用 self
}
```

这种设计符合 Rust 的语义：
- `Self` 可以在 impl 块、trait 块、这些块中的方法以及关联函数的直接上下文中使用
- `self` 只能在方法的直接上下文中使用
- 嵌套函数（如闭包）不能使用 `Self` 和 `self`

### 2. Self检查器

#### 2.1 SelfChecker类

```java
// self与Self检查器
public class SelfChecker {
    private ContextInfo currentContext;
    
    public SelfChecker() {
        this.currentContext = new ContextInfo(ContextType.GLOBAL, null);
    }
    
    // 进入新上下文
    public void enterContext(ContextType type) {
        currentContext = new ContextInfo(type, currentContext);
    }
    
    // 退出当前上下文
    public void exitContext() {
        if (currentContext.getParent() == null) {
            throw new RuntimeException("Cannot exit from root context - context stack underflow");
        }
        currentContext = currentContext.getParent();
    }
    
    // 错误抛出方法
    private void reportError(SelfErrorType errorType, String message, ASTNode node) {
        throw new SemanticException(errorType.toErrorType(), message, node);
    }
    
    // 获取当前上下文
    public ContextInfo getCurrentContext() {
        return currentContext;
    }
}
```

#### 2.2 错误类型定义

```java
// self与Self相关错误类型
public enum SelfErrorType {
    SELF_OUTSIDE_METHOD("self can only be used in method bodies"),
    SELF_IN_ASSOCIATED_FUNCTION("self cannot be used in associated functions"),
    SELF_NOT_FIRST_PARAMETER("self must be the first parameter of a method"),
    SELF_OUTSIDE_IMPL_OR_TRAIT("self can only be used in impl blocks or trait definitions"),
    
    SELF_TYPE_OUTSIDE_CONTEXT("Self can only be used in impl blocks or trait definitions"),
    SELF_TYPE_IN_FREE_FUNCTION("Self cannot be used in global context or free functions"),
    SELF_TYPE_WITH_PREFIX("Self can only be used as the first segment, without a preceding ::");
    
    private final String description;
    
    SelfErrorType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public ErrorType toErrorType() {
        return ErrorType.SELF_ERROR;
    }
}
```


## 与现有语义分析器的集成

### 1. 修改SemanticAnalyzer类

```java
// 在SemanticAnalyzer类中添加SelfChecker
public class SemanticAnalyzer extends VisitorBase {
    private SelfChecker selfChecker;
    
    // 构造函数中初始化
    public SemanticAnalyzer() {
        // ... 其他初始化
        this.selfChecker = new SelfChecker();
    }
    
    // 获取self检查器
    public SelfChecker getSelfChecker() {
        return selfChecker;
    }
}
```

### 2. 修改visit方法

#### 2.1 ImplNode的visit方法

```java
// 在ImplNode的visit方法中
@Override
public void visit(ImplNode node) {
    // 进入impl块上下文
    selfChecker.enterContext(ContextType.IMPL_BLOCK);
    
    try {
        // 处理impl块内容
        if (node.items != null) {
            for (AssoItemNode item : node.items) {
                item.accept(this);
            }
        }
    } finally {
        // 退出impl块上下文
        selfChecker.exitContext();
    }
}

```

#### 2.2 TraitNode的visit方法

```java
// 在TraitNode的visit方法中
@Override
public void visit(TraitNode node) {
    // 进入trait块上下文
    selfChecker.enterContext(ContextType.TRAIT_BLOCK);
    
    try {
        // 处理trait块内容
        if (node.items != null) {
            for (AssoItemNode item : node.items) {
                item.accept(this);
            }
        }
    } finally {
        // 退出trait块上下文
        selfChecker.exitContext();
    }
}
```

#### 2.3 StructNode的visit方法

```java
// 在StructNode的visit方法中
@Override
public void visit(StructNode node) {
    // 处理结构体字段
    if (node.fields != null) {
        for (FieldNode field : node.fields) {
            field.accept(this);
        }
    }
}
```

#### 2.4 FunctionNode的visit方法

```java
// 在FunctionNode的visit方法中
@Override
public void visit(FunctionNode node) {
    // 检查父上下文是否是impl或trait块
    ContextInfo parentContext = selfChecker.getCurrentContext();
    boolean isInImplOrTrait = parentContext.isCurrentImplBlock() || parentContext.isCurrentTraitBlock();
    
    // 处理函数参数（在进入上下文之前）
    if (node.selfPara != null) {
        node.selfPara.accept(this);
    }
    
    if (node.parameters != null) {
        for (ParameterNode param : node.parameters) {
            param.accept(this);
        }
    }
    
    // 处理函数体（在进入上下文之后）
    if (node.body != null) {
        if (isInImplOrTrait) {
            // 判断是否是实例方法
            boolean isInstanceMethod = node.selfPara != null;
            
            // 进入方法或关联函数上下文
            selfChecker.enterContext(
                isInstanceMethod ? ContextType.METHOD : ContextType.ASSOCIATED_FUNC
            );
            
            try {
                // 处理函数体
                node.body.accept(this);
            } finally {
                // 退出方法或关联函数上下文
                selfChecker.exitContext();
            }
        } else {
            // 处理全局函数，进入全局上下文
            selfChecker.enterContext(ContextType.GLOBAL);
            
            try {
                // 处理函数体
                node.body.accept(this);
            } finally {
                // 退出全局上下文
                selfChecker.exitContext();
            }
        }
    }
}
```

#### 2.5 PathExprSegNode的visit方法

```java
// 在PathExprSegNode的visit方法中
@Override
public void visit(PathExprSegNode node) {
    if (node.patternType == patternSeg_t.SELF) {
        // 检查self的使用
        ContextInfo context = selfChecker.getCurrentContext();
        if (!context.canUseSelfValue()) {
            if (context.isCurrentAssociatedFunction()) {
                selfChecker.reportError(SelfErrorType.SELF_IN_ASSOCIATED_FUNCTION,
                        "self cannot be used in associated functions", node);
            } else {
                selfChecker.reportError(SelfErrorType.SELF_OUTSIDE_METHOD,
                        "self can only be used in method bodies", node);
            }
            return;
        }
    } else if (node.patternType == patternSeg_t.SELF_TYPE) {
        // 检查Self的使用
        ContextInfo context = selfChecker.getCurrentContext();
        if (!context.canUseSelfType()) {
            selfChecker.reportError(SelfErrorType.SELF_TYPE_OUTSIDE_CONTEXT,
                    "Self can only be used in impl blocks or trait definitions", node);
            return;
        }
    } else {
        // 处理普通标识符
        node.name.accept(this);
    }
}
```

#### 2.6 SelfParaNode的visit方法

```java
// 在SelfParaNode的visit方法中
@Override
public void visit(SelfParaNode node) {
    // 只需要访问self参数的类型
    if (node.type != null) {
        node.type.accept(this);
    }
}
```

## 测试用例实现

### 1. 正确使用测试用例

```java
// 测试正确的self使用
@Test
public void testValidSelfUsage() {
    String code = "
    struct S {
        b: bool,
    }
    impl S {
        fn foo(&self) {}
        fn baz(&self) -> bool {
            self.foo();
            self.b
        }
    }
    ";
    
    SemanticAnalyzer analyzer = new SemanticAnalyzer();
    // ... 解析代码并运行语义分析
    
    // 如果没有抛出异常，则测试通过
    // 正确使用self和Self不应该抛出异常
}

// 测试正确的Self使用
@Test
public void testValidSelfTypeUsage() {
    String code = "
    trait T {
        const C: i32;
        fn new() -> Self;
        fn f(&self) -> i32;
    }
    
    struct S;
    impl T for S {
        const C: i32 = 9;
        fn new() -> Self {
            S
        }
        fn f(&self) -> i32 {
            Self::C
        }
    }
    ";
    
    SemanticAnalyzer analyzer = new SemanticAnalyzer();
    // ... 解析代码并运行语义分析
    
    // 如果没有抛出异常，则测试通过
    // 正确使用self和Self不应该抛出异常
}

```

### 2. 错误使用测试用例

```java
// 测试错误的self使用
@Test
public void testInvalidSelfUsage() {
    String code = "
    fn free_function(x: i32) {
        // 在全局上下文中，不能使用self
        let y = self;
    }
    ";
    
    SemanticAnalyzer analyzer = new SemanticAnalyzer();
    // ... 解析代码并运行语义分析
    
    // 应该抛出SemanticException
    assertThrows(SemanticException.class, () -> {
        analyzer.analyze(code);
    });
}

// 测试错误的Self使用
@Test
public void testInvalidSelfTypeUsage() {
    String code = "
    fn free_function() -> Self {
        // 在全局上下文中，不能使用Self
        Self {}
    }
    ";
    
    SemanticAnalyzer analyzer = new SemanticAnalyzer();
    // ... 解析代码并运行语义分析
    
    // 应该抛出SemanticException
    assertThrows(SemanticException.class, () -> {
        analyzer.analyze(code);
    });
}
```

## 实现步骤

### 第一阶段：基础结构实现
1. 实现ContextInfo类，支持上下文嵌套和查询
2. 实现SelfChecker类，支持上下文进入和退出
3. 实现SelfErrorType枚举，定义所有self与Self相关错误
4. 在ErrorType枚举中添加SELF_ERROR类型

### 第二阶段：self检查实现
1. 实现self作为参数的检查逻辑
2. 实现self作为表达式的检查逻辑
3. 实现self在不同上下文中的使用规则检查
4. 在SelfParaNode的visit方法中集成self参数检查

### 第三阶段：Self检查实现
1. 实现Self作为类型的检查逻辑
2. 实现Self作为表达式的检查逻辑
3. 实现Self在不同上下文中的使用规则检查
4. 实现Self递归引用检查

### 第四阶段：路径检查实现
1. 实现self和Self不能有前缀`::`的检查
2. 实现路径解析规则

### 第五阶段：集成与测试
1. 将SelfChecker集成到主语义分析流程
2. 修改所有相关的visit方法以支持上下文跟踪
3. 添加特定测试用例验证实现
4. 完善错误报告和恢复机制

## 与现有代码的兼容性

本实现与现有的语义分析器完全兼容：

1. **不修改现有符号表结构**：self和Self检查使用独立的上下文跟踪系统
2. **不修改现有AST结构**：利用现有的PathExprSegNode和SelfParaNode
3. **不修改现有访问者模式**：扩展现有的visit方法，不改变接口
4. **不修改现有错误处理**：扩展现有错误类型，保持一致的错误报告机制

## 性能考虑

1. **上下文跟踪开销**：上下文跟踪使用链表结构，进入和退出上下文是O(1)操作
2. **检查开销**：self和Self检查只在遇到相关关键字时进行，不影响其他代码
3. **内存开销**：上下文信息只在需要时创建，不会显著增加内存使用

## 未来扩展

实现基本self与Self检查后，可以扩展以支持：

1. **更复杂的self参数类型检查**：如`Box<Self>`、`Rc<Self>`等
2. **生命周期相关的self与Self使用检查**：结合生命周期分析
3. **泛型impl块中的Self使用检查**：支持泛型参数和约束
4. **更精确的错误定位和修复建议**：提供更友好的错误信息
5. **Self在trait约束中的使用检查**：支持更复杂的trait系统

## 结论

本实现基于Rust Reference的Paths规范，为RCompiler提供了全面的self与Self语义检查实现。通过精确的上下文跟踪和规则检查，可以确保编译器正确处理Rust中这两个特殊关键字的使用，提高代码质量和开发体验。实现与现有代码完全兼容，不会影响现有功能，同时为未来扩展提供了良好的基础。