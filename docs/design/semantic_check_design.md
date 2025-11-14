# 基于Rust Reference的命名空间语义检查设计

## 概述

本文档根据Rust Reference中的Namespaces规范，设计RCompiler中与命名空间功能相关的语义检查实现。该设计确保所有声明的名称按照Rust的命名空间规则进行正确管理和检查。

## Rust命名空间规范

根据Rust Reference，Rust中有多个不同的命名空间，每个命名空间包含不同类型的实体：

### 1. 类型命名空间 (Type Namespace)
包含以下实体：
- 结构体(struct)、枚举(enum)、枚举变体声明
- Trait项声明
- 内置类型：布尔值、数值类型、文本类型
- `Self`类型

### 2. 值命名空间 (Value Namespace)
包含以下实体：
- 函数声明
- 常量项声明
- 结构体构造函数
- 枚举变体构造函数
- `Self`构造函数
- 关联常量声明
- 关联函数声明
- 局部绑定——`let`语句、`if let`、`while let`、`for`、`match`分支、函数参数

### 3. 无命名空间的命名实体
以下实体有明确的名称，但不属于任何特定的命名空间：
- 字段(Fields)：结构体、枚举和联合体的字段只能通过字段表达式访问，字段表达式只检查被访问的特定类型的字段名称

## 当前编译器状态

RCompiler目前具备：
- 完整的词法分析器，将源代码转换为标记
- 解析器，从标记构建抽象语法树(AST)
- 在`src/main/java/ast/AST.java`中定义的AST结构
- 在`src/main/java/utils/VisitorBase.java`中实现的访问者模式

## 语义检查需求

### 1. 命名空间分离
确保不同命名空间中的名称不会相互冲突：
- 类型命名空间中的名称与值命名空间中的同名不会冲突
- 字段名称独立于其他命名空间

### 2. 名称解析规则
根据上下文在不同命名空间中查找名称：
- 类型上下文在类型命名空间中查找
- 值上下文在值命名空间中查找
- 字段访问只在特定类型的字段中查找

### 3. 重复声明检测
在同一命名空间内检测重复声明：
- 类型命名空间中的重复类型声明
- 值命名空间中的重复值声明
- 同一结构体中的重复字段声明

## 架构设计

### 1. 多命名空间符号表

```java
// 符号种类枚举
enum SymbolKind {
    // 类型命名空间
    STRUCT,
    ENUM,
    TRAIT,
    BUILTIN_TYPE,
    SELF_TYPE,
    
    // 值命名空间
    FUNCTION,
    CONSTANT,
    STRUCT_CONSTRUCTOR,
    ENUM_VARIANT_CONSTRUCTOR,
    SELF_CONSTRUCTOR,
    ASSOCIATED_CONST,
    ASSOCIATED_FUNCTION,
    LOCAL_VARIABLE,
    PARAMETER,
    
    // 字段(无命名空间)
    FIELD
}

// 符号表条目
class Symbol {
    String name;
    SymbolKind kind;
    ASTNode declaration;
    int scopeLevel;
    boolean isMutable;
    List<Symbol> implSymbols; // Symbols defined in impl blocks
    // 其他元数据
}

// 多命名空间符号表
class NamespaceSymbolTable {
    // 类型命名空间
    Map<String, Symbol> typeNamespace;
    
    // 值命名空间
    Map<String, Symbol> valueNamespace;
    
    // 字段映射(按类型)
    Map<String, Map<String, Symbol>> fieldNamespaces;
    
    NamespaceSymbolTable parent;
    List<NamespaceSymbolTable> children;
    
    // 方法
    void addTypeSymbol(Symbol symbol);
    void addValueSymbol(Symbol symbol);
    void addFieldSymbol(String typeName, Symbol symbol);
    Symbol lookupType(String name);
    Symbol lookupValue(String name);
    Symbol lookupField(String typeName, String fieldName);
    void enterScope();
    void exitScope();
}
```

### 2. 语义分析器

```java
class SemanticAnalyzer extends VisitorBase {
    NamespaceSymbolTable globalScope;
    NamespaceSymbolTable currentScope;
    List<SemanticError> errors;
    
    // 当前上下文(用于确定在哪个命名空间查找)
    enum Context {
        TYPE_CONTEXT,
        VALUE_CONTEXT,
        FIELD_CONTEXT
    }
    Context currentContext;
    
    // 访问方法
    void visit(FunctionNode node);
    void visit(StructNode node);
    void visit(EnumNode node);
    void visit(TraitNode node);
    void visit(ConstItemNode node);
    void visit(LetStmtNode node);
    void visit(IdentifierNode node);
    void visit(PathExprNode node);
    void visit(FieldExprNode node);
    // ... 其他访问方法
}
```

### 3. 错误报告系统

```java
class SemanticError {
    ErrorType type;
    String message;
    int line;
    int column;
    String sourceFile;
    String namespace; // 错误发生的命名空间
}

enum ErrorType {
    UNDECLARED_TYPE_IDENTIFIER,      // 类型命名空间中未声明的标识符
    UNDECLARED_VALUE_IDENTIFIER,     // 值命名空间中未声明的标识符
    UNDECLARED_FIELD,                // 未声明的字段
    DUPLICATE_TYPE_DECLARATION,      // 类型命名空间中的重复声明
    DUPLICATE_VALUE_DECLARATION,     // 值命名空间中的重复声明
    DUPLICATE_FIELD_DECLARATION,     // 重复字段声明
    NAMESPACE_VIOLATION,             // 命名空间违规
    // ... 其他错误类型
}
```

## 实现计划

### 第一阶段：多命名空间符号表实现
1. 实现NamespaceSymbolTable类，支持类型、值和字段三个命名空间
2. 实现分层作用域管理
3. 实现各命名空间的符号查找和插入

### 第二阶段：声明处理
1. 处理类型命名空间声明(结构体、枚举、trait)
2. 处理值命名空间声明(函数、常量、局部变量)
3. 处理字段声明(结构体字段)

### 第三阶段：名称解析
1. 根据上下文在正确的命名空间中查找名称
2. 处理类型上下文中的名称解析
3. 处理值上下文中的名称解析
4. 处理字段访问中的名称解析

### 第四阶段：错误检测
1. 检测各命名空间中的未声明标识符
2. 检测各命名空间中的重复声明
3. 检测字段访问错误
4. 生成特定命名空间的错误消息

### 第五阶段：集成与测试
1. 将语义分析器集成到主编译流程
2. 使用现有测试用例验证实现
3. 添加特定命名空间的测试用例

## 详细实现

### 1. 命名空间分离实现

每个作用域将维护三个独立的命名空间：
- 类型命名空间：存储所有类型相关符号
- 值命名空间：存储所有值相关符号
- 字段命名空间：按类型组织存储字段符号

### 2. 上下文感知的名称解析

语义分析器将跟踪当前上下文：
- 在类型表达式中，在类型命名空间查找
- 在值表达式中，在值命名空间查找
- 在字段访问中，在特定类型的字段中查找

### 3. 字段特殊处理

字段不属于任何全局命名空间，只能通过字段表达式访问：
- 字段名称只在特定类型的上下文中有效
- 字段访问需要先解析接收器类型，然后在该类型的字段中查找

### 4. 重复声明检测

在每个命名空间内独立检测重复声明：
- 同一作用域中不能有同名的类型
- 同一作用域中不能有同名的值
- 同一结构体中不能有同名的字段

## 与现有代码的集成

语义分析器将集成到`Main.java`的主编译流程中：

```java
// 解析后
Parser parser = new Parser(new Vector<token_t>(tokenizer.tokens));
parser.parse();

// 执行命名空间语义分析
SemanticAnalyzer analyzer = new SemanticAnalyzer();
for (StmtNode stmt : parser.getStatements()) {
    analyzer.visit(stmt);
}

// 报告语义错误
if (analyzer.hasErrors()) {
    for (SemanticError error : analyzer.getErrors()) {
        System.err.println(error);
    }
    // 以错误代码退出
}
```

## 测试策略

使用现有测试用例并添加新的测试用例：
1. 验证类型和值命名空间的分离
2. 验证字段访问的正确性
3. 验证重复声明的检测
4. 验证跨命名空间的名称不冲突

## 示例场景

根据Rust Reference中的示例：

```rust
struct Foo { x: u32 }  // Foo在类型命名空间

fn example(f: Foo) {    // Foo在类型命名空间中查找
    let x: Foo;         // Foo在类型命名空间中查找
    // 字段访问x只在Foo类型的字段中查找
}
```

## 未来扩展

实现基本命名空间检查后，可以扩展以支持：
- 宏命名空间
- 生命周期命名空间
- 标签命名空间
- 更复杂的名称解析规则

## 结论

本设计基于Rust Reference的命名空间规范，为RCompiler提供了全面的命名空间语义检查实现。通过分离类型、值和字段命名空间，并实现上下文感知的名称解析，可以确保编译器正确处理Rust的命名空间规则。