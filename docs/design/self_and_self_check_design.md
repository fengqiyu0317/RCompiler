# self与Self语义检查设计

## 概述

本文档设计RCompiler中针对`self`与`Self`关键字的语义检查实现。根据Rust Reference中的Paths规范，`self`和`Self`是两个特殊的关键字，它们在不同的上下文中有特定的含义和使用规则。本设计确保这两个关键字在代码中的使用符合Rust语言规范。

## Rust Reference中self与Self的规范

### 1. `self`关键字

根据Rust Reference，`self`在路径表达式中的规范如下：

#### 1.1 `self`的定义
- 在方法体中，由单个`self`段组成的路径解析为方法的self参数
- `self`只能作为路径段（PathIdentSegment）使用，不能有前缀的`::`

#### 1.2 `self`的使用场景
```rust
struct S {
    b: bool,
}
impl S {
    fn foo(&self){}
    fn baz(&self) -> bool {
        self.foo(); // self作为路径表达式，解析为方法的self参数
        self.b      // self用于字段访问
    }
}
```

### 2. `Self`关键字

根据Rust Reference，`Self`在路径表达式中的规范如下：

#### 2.1 `Self`的定义
- `Self`（大写S）用于引用当前正在实现或定义的类型
- `Self`只能在以下情况中使用：
  1. 在trait定义中，它引用实现trait的类型
  2. 在impl块中，它引用被实现的类型
  3. 在结构体或枚举定义中，它引用被定义的类型

#### 2.2 `Self`的使用限制
- `Self`只能作为第一段使用，不能有前缀的`::`
- `Self`路径不能包含泛型参数
- `Self`的作用域类似于泛型参数

#### 2.3 `Self`的使用示例
```rust
trait T {
    const C: i32;
    // `Self`将是实现`T`的任何类型
    fn new() -> Self;
    fn f(&self) -> i32;
}

struct S;
impl T for S {
    const C: i32 = 9;
    fn new() -> Self {           // `Self`是类型`S`
        S
    }
    fn f(&self) -> i32 {
        Self::C                  // `Self::C`是常量值`9`
    }
}
```

#### 2.4 `Self`的递归引用
结构体可以引用自身，只要不是无限递归：
```rust
struct NonEmptyList {
    head: usize,
    tail: Option<Box<Self>>,  // Self引用结构体自身
}
```

## 当前编译器状态

RCompiler目前具备：
- 完整的词法分析器，将源代码转换为标记
- 解析器，从标记构建抽象语法树(AST)
- 在`src/main/java/ast/AST.java`中定义的AST结构，包括：
  - `PathExprSegNode`类，支持`patternSeg_t.IDENT`、`patternSeg_t.SELF`、`patternSeg_t.SELF_TYPE`
  - `SelfParaNode`类，表示方法中的self参数
- 在`src/main/java/utils/VisitorBase.java`中实现的访问者模式

## 语义检查需求

### 1. `self`使用规则检查

#### 1.1 路径表达式中的`self`
- `self`只能在方法体中使用
- `self`必须解析为方法的self参数
- `self`不能有前缀的`::`
- `self`不能在自由函数中使用
- `self`不能在关联函数中使用

#### 1.2 `self`参数检查
- `self`只能作为方法的第一个参数
- `self`只能在impl块中的函数定义中使用

### 2. `Self`使用规则检查

#### 2.1 路径表达式中的`Self`
- `Self`只能在impl块、trait定义或类型定义中使用
- `Self`不能有前缀的`::`
- `Self`路径不能包含泛型参数
- `Self`不能在自由函数中使用

#### 2.2 类型上下文中的`Self`
- `Self`只能在trait定义、impl块或类型定义中使用
- 在trait定义中，`Self`引用实现trait的类型
- 在impl块中，`Self`引用被实现的类型
- 在类型定义中，`Self`引用被定义的类型

### 3. 上下文跟踪

为了正确检查`self`和`Self`的使用，需要跟踪以下上下文信息：
- 当前是否在impl块中
- 当前是否在trait定义中
- 当前是否在类型定义中
- 当前是否在方法中
- 当前方法是否是实例方法还是关联函数
- 当前处理的节点类型（参数、类型、表达式等）

## 架构设计

### 1. 上下文跟踪系统

```java
// 上下文类型枚举
public enum ContextType {
    GLOBAL,           // 全局上下文
    IMPL_BLOCK,       // impl块上下文
    TRAIT_BLOCK,      // trait块上下文
    TYPE_DEFINITION,  // 类型定义上下文
    FUNCTION,         // 函数上下文
    METHOD,           // 方法上下文
    ASSOCIATED_FUNC   // 关联函数上下文
}

// 上下文信息类
public class ContextInfo {
    private final ContextType type;
    private final String typeName;        // 当前类型名称（在impl、trait或类型定义中）
    private final boolean isInstanceMethod; // 是否是实例方法
    private final ContextInfo parent;     // 父上下文
    
    public ContextInfo(ContextType type, String typeName, boolean isInstanceMethod, ContextInfo parent) {
        this.type = type;
        this.typeName = typeName;
        this.isInstanceMethod = isInstanceMethod;
        this.parent = parent;
    }
    
    // Getter方法
    public ContextType getType() { return type; }
    public String getTypeName() { return typeName; }
    public boolean isInstanceMethod() { return isInstanceMethod; }
    public ContextInfo getParent() { return parent; }
    
    // 检查是否在特定上下文中
    public boolean isInImplBlock() {
        return type == ContextType.IMPL_BLOCK || 
               (parent != null && parent.isInImplBlock());
    }
    
    public boolean isInTraitBlock() {
        return type == ContextType.TRAIT_BLOCK || 
               (parent != null && parent.isInTraitBlock());
    }
    
    public boolean isInTypeDefinition() {
        return type == ContextType.TYPE_DEFINITION || 
               (parent != null && parent.isInTypeDefinition());
    }
    
    public boolean isInMethod() {
        return type == ContextType.METHOD || 
               (parent != null && parent.isInMethod());
    }
    
    public boolean isInAssociatedFunction() {
        return type == ContextType.ASSOCIATED_FUNC || 
               (parent != null && parent.isInAssociatedFunction());
    }
    
    // 检查是否在可以使用Self的上下文中
    public boolean canUseSelfType() {
        return isInImplBlock() || isInTraitBlock() || isInTypeDefinition();
    }
    
    // 检查是否在可以使用self的上下文中
    public boolean canUseSelfValue() {
        return isInMethod() && isInstanceMethod();
    }
}
```

### 2. Self检查器

```java
// self与Self检查器
public class SelfChecker {
    private ContextInfo currentContext;
    private final List<SemanticError> errors;
    
    public SelfChecker() {
        this.currentContext = new ContextInfo(ContextType.GLOBAL, null, false, null);
        this.errors = new ArrayList<>();
    }
    
    // 进入新上下文
    public void enterContext(ContextType type, String typeName, boolean isInstanceMethod) {
        currentContext = new ContextInfo(type, typeName, isInstanceMethod, currentContext);
    }
    
    // 退出当前上下文
    public void exitContext() {
        if (currentContext.getParent() != null) {
            currentContext = currentContext.getParent();
        }
    }
    
    // 检查self的使用
    public void checkSelfUsage(PathExprSegNode node, UsageType usageType) {
        // 检查是否在可以使用self的上下文中
        if (!currentContext.canUseSelfValue()) {
            if (currentContext.isInAssociatedFunction()) {
                addError(SelfErrorType.SELF_IN_ASSOCIATED_FUNCTION, 
                        "self cannot be used in associated functions", node);
            } else {
                addError(SelfErrorType.SELF_OUTSIDE_METHOD, 
                        "self can only be used in method bodies", node);
            }
            return;
        }
        
        // 根据使用类型进行额外检查
        switch (usageType) {
            case PARAMETER:
                checkSelfAsParameter(node);
                break;
            case EXPRESSION:
                checkSelfAsExpression(node);
                break;
        }
    }
    
    // 检查Self的使用
    public void checkSelfTypeUsage(PathExprSegNode node, UsageType usageType) {
        // 检查是否在可以使用Self的上下文中
        if (!currentContext.canUseSelfType()) {
            addError(SelfErrorType.SELF_TYPE_OUTSIDE_CONTEXT, 
                    "Self can only be used in impl blocks, trait definitions, or type definitions", node);
            return;
        }
        
        // 根据使用类型进行额外检查
        switch (usageType) {
            case TYPE:
                checkSelfTypeAsType(node);
                break;
            case EXPRESSION:
                checkSelfTypeAsExpression(node);
                break;
        }
    }
    
    // 使用类型枚举
    public enum UsageType {
        PARAMETER,   // 作为参数
        EXPRESSION,  // 作为表达式
        TYPE         // 作为类型
    }
    
    // 错误添加方法
    private void addError(SelfErrorType errorType, String message, ASTNode node) {
        errors.add(new SemanticError(errorType.toErrorType(), message, node));
    }
    
    // 获取错误列表
    public List<SemanticError> getErrors() {
        return errors;
    }
}
```

### 3. 错误类型定义

```java
// self与Self相关错误类型
public enum SelfErrorType {
    SELF_OUTSIDE_METHOD("self can only be used in method bodies"),
    SELF_IN_ASSOCIATED_FUNCTION("self cannot be used in associated functions"),
    SELF_NOT_FIRST_PARAMETER("self must be the first parameter of a method"),
    SELF_OUTSIDE_IMPL_OR_TRAIT("self can only be used in impl blocks or trait definitions"),
    
    SELF_TYPE_OUTSIDE_CONTEXT("Self can only be used in impl blocks, trait definitions, or type definitions"),
    SELF_TYPE_IN_FREE_FUNCTION("Self cannot be used in free functions"),
    SELF_TYPE_WITH_GENERIC_ARGS("Self path cannot include generic arguments"),
    SELF_TYPE_WITH_PREFIX("Self can only be used as the first segment, without a preceding ::"),
    
    SELF_RECURSIVE_TYPE("Type definition cannot be infinitely recursive");
    
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

## 实现计划

### 第一阶段：上下文跟踪实现
1. 实现ContextInfo类，支持上下文嵌套和查询
2. 实现SelfChecker类，支持上下文进入和退出
3. 在语义分析器中集成上下文跟踪

### 第二阶段：self检查实现
1. 实现self作为参数的检查
2. 实现self作为表达式的检查
3. 实现self在不同上下文中的使用规则检查

### 第三阶段：Self检查实现
1. 实现Self作为类型的检查
2. 实现Self作为表达式的检查
3. 实现Self在不同上下文中的使用规则检查
4. 实现Self递归引用检查

### 第四阶段：路径检查实现
1. 实现self和Self不能有前缀`::`的检查
2. 实现Self不能包含泛型参数的检查
3. 实现路径解析规则

### 第五阶段：集成与测试
1. 将self与Self检查器集成到主语义分析流程
2. 添加特定测试用例验证实现
3. 完善错误报告和恢复机制

## 详细实现

### 1. self检查规则

#### 1.1 self作为参数检查

```java
private void checkSelfAsParameter(PathExprSegNode node) {
    // 检查是否在方法中
    if (!currentContext.isInMethod()) {
        addError(SelfErrorType.SELF_OUTSIDE_IMPL_OR_TRAIT, 
                "self can only be used as a parameter in methods", node);
        return;
    }
    
    // 检查是否是实例方法
    if (!currentContext.isInstanceMethod()) {
        addError(SelfErrorType.SELF_IN_ASSOCIATED_FUNCTION, 
                "self cannot be used in associated functions", node);
        return;
    }
    
    // 在完整的实现中，还需要检查self是否是第一个参数
    // 这需要在FunctionNode的visit方法中额外处理
}
```

#### 1.2 self作为表达式检查

```java
private void checkSelfAsExpression(PathExprSegNode node) {
    // 检查是否在可以使用self的上下文中
    if (!currentContext.canUseSelfValue()) {
        if (currentContext.isInAssociatedFunction()) {
            addError(SelfErrorType.SELF_IN_ASSOCIATED_FUNCTION, 
                    "self cannot be used in associated functions", node);
        } else {
            addError(SelfErrorType.SELF_OUTSIDE_METHOD, 
                    "self can only be used in method bodies", node);
        }
        return;
    }
}
```

### 2. Self检查规则

#### 2.1 Self作为类型检查

```java
private void checkSelfTypeAsType(PathExprSegNode node) {
    // 检查是否在可以使用Self的上下文中
    if (!currentContext.canUseSelfType()) {
        addError(SelfErrorType.SELF_TYPE_OUTSIDE_CONTEXT, 
                "Self can only be used in impl blocks, trait definitions, or type definitions", node);
        return;
    }
    
    // 检查Self是否在类型定义中递归使用
    if (currentContext.isInTypeDefinition()) {
        checkRecursiveSelfUsage(node);
    }
}
```

#### 2.2 Self作为表达式检查

```java
private void checkSelfTypeAsExpression(PathExprSegNode node) {
    // 检查是否在可以使用Self的上下文中
    if (!currentContext.canUseSelfType()) {
        addError(SelfErrorType.SELF_TYPE_OUTSIDE_CONTEXT, 
                "Self can only be used in impl blocks, trait definitions, or type definitions", node);
        return;
    }
    
    // 检查Self是否作为路径的第一段使用
    if (!isFirstSegmentInPath(node)) {
        addError(SelfErrorType.SELF_TYPE_WITH_PREFIX, 
                "Self can only be used as the first segment, without a preceding ::", node);
        return;
    }
}
```

### 3. 路径检查规则

#### 3.1 路径前缀检查

```java
// 检查self或Self是否是路径的第一段
private boolean isFirstSegmentInPath(PathExprSegNode node) {
    // 需要检查PathExprNode的结构
    // 如果node是PathExprNode的LSeg，则是第一段
    // 如果node是PathExprNode的RSeg，则不是第一段
    if (node.getParent() instanceof PathExprNode) {
        PathExprNode pathNode = (PathExprNode) node.getParent();
        return pathNode.LSeg == node;
    }
    return false;
}
```

#### 3.2 泛型参数检查

```java
// 检查Self是否包含泛型参数
private void checkSelfGenericArguments(PathExprSegNode node) {
    // 在完整的实现中，需要检查Self后面是否有::<...>语法
    // 根据Rust Reference，Self路径不能包含泛型参数
    if (hasGenericArguments(node)) {
        addError(SelfErrorType.SELF_TYPE_WITH_GENERIC_ARGS, 
                "Self path cannot include generic arguments", node);
    }
}
```

### 4. 语义分析器集成

```java
// 在SemanticAnalyzer类中添加SelfChecker
public class SemanticAnalyzer extends VisitorBase {
    private SelfChecker selfChecker;
    
    // 构造函数中初始化
    public SemanticAnalyzer() {
        // ... 其他初始化
        this.selfChecker = new SelfChecker();
    }
    
    // 在ImplNode的visit方法中
    @Override
    public void visit(ImplNode node) {
        // 进入impl块上下文
        String typeName = getTypeName(node.typeName);
        selfChecker.enterContext(ContextType.IMPL_BLOCK, typeName, false);
        
        try {
            // 处理impl块内容
            if (node.items != null) {
                for (AssoItemNode item : node.items) {
                    visit(item);
                }
            }
        } finally {
            // 退出impl块上下文
            selfChecker.exitContext();
        }
    }
    
    // 在TraitNode的visit方法中
    @Override
    public void visit(TraitNode node) {
        // 进入trait块上下文
        String traitName = node.name.name;
        selfChecker.enterContext(ContextType.TRAIT_BLOCK, traitName, false);
        
        try {
            // 处理trait块内容
            if (node.items != null) {
                for (AssoItemNode item : node.items) {
                    visit(item);
                }
            }
        } finally {
            // 退出trait块上下文
            selfChecker.exitContext();
        }
    }
    
    // 在StructNode的visit方法中
    @Override
    public void visit(StructNode node) {
        // 进入类型定义上下文
        String structName = node.name.name;
        selfChecker.enterContext(ContextType.TYPE_DEFINITION, structName, false);
        
        try {
            // 处理结构体字段
            if (node.fields != null) {
                for (FieldNode field : node.fields) {
                    visit(field);
                }
            }
        } finally {
            // 退出类型定义上下文
            selfChecker.exitContext();
        }
    }
    
    // 在FunctionNode的visit方法中
    @Override
    public void visit(FunctionNode node) {
        // 判断是否是实例方法
        boolean isInstanceMethod = node.selfPara != null;
        
        // 进入函数上下文
        selfChecker.enterContext(
            isInstanceMethod ? ContextType.METHOD : ContextType.ASSOCIATED_FUNC,
            currentContext.getTypeName(),
            isInstanceMethod
        );
        
        try {
            // 处理函数内容
            // ... 现有代码
        } finally {
            // 退出函数上下文
            selfChecker.exitContext();
        }
    }
    
    // 在PathExprSegNode的visit方法中
    @Override
    public void visit(PathExprSegNode node) {
        if (node.patternType == patternSeg_t.SELF) {
            // 检查self的使用
            selfChecker.checkSelfUsage(node, determineUsageType());
        } else if (node.patternType == patternSeg_t.SELF_TYPE) {
            // 检查Self的使用
            selfChecker.checkSelfTypeUsage(node, determineUsageType());
        } else {
            // 处理普通标识符
            visit(node.name);
        }
    }
    
    // 辅助方法：确定使用类型
    private UsageType determineUsageType() {
        // 根据当前上下文确定使用类型
        // 这需要更复杂的上下文分析
        return UsageType.EXPRESSION; // 简化实现
    }
}
```

## 测试策略

### 1. 正确使用测试用例

```rust
// 正确的self使用
impl MyStruct {
    fn method(&self) -> i32 {  // self作为参数
        self.field  // self作为表达式
    }
    
    fn method_mut(mut self) -> Self {  // self作为参数和Self作为返回类型
        self
    }
}

// 正确的Self使用
impl MyStruct {
    fn new() -> Self {  // Self作为返回类型
        Self { field: 42 }  // Self作为构造函数
    }
    
    fn associated() -> Self {  // Self作为返回类型
        Self::default()  // Self作为关联函数调用
    }
}

// 正确的Self在类型定义中使用
struct NonEmptyList {
    head: usize,
    tail: Option<Box<Self>>,  // Self引用结构体自身
}
```

### 2. 错误使用测试用例

```rust
// 错误的self使用
fn free_function(x: i32) {  // 自由函数
    let y = self;  // 错误：self不能在自由函数中使用
}

impl MyStruct {
    fn associated() {  // 关联函数
        let y = self;  // 错误：self不能在关联函数中使用
    }
}

// 错误的Self使用
fn free_function() -> Self {  // 错误：Self不能在自由函数中使用
    Self {}
}

impl MyStruct {
    fn method(&self) {
        let x: Self<i32> = Self {};  // 错误：Self不能包含泛型参数
        let y = ::Self::method();     // 错误：Self不能有前缀::
    }
}
```

## 与现有代码的集成

self与Self检查器将集成到现有的语义分析流程中：

1. 在`SemanticAnalyzer`类中添加`SelfChecker`实例
2. 在各个`visit`方法中添加上下文跟踪
3. 在`PathExprSegNode`的`visit`方法中添加self与Self检查
4. 将检查结果合并到主错误报告中

## 未来扩展

实现基本self与Self检查后，可以扩展以支持：
1. 更复杂的self参数类型检查（如`Box<Self>`、`Rc<Self>`等）
2. 生命周期相关的self与Self使用检查
3. 泛型impl块中的Self使用检查
4. 更精确的错误定位和修复建议
5. Self在trait约束中的使用检查

## 结论

本设计基于Rust Reference的Paths规范，为RCompiler提供了全面的self与Self语义检查实现。通过精确的上下文跟踪和规则检查，可以确保编译器正确处理Rust中这两个特殊关键字的使用，提高代码质量和开发体验。