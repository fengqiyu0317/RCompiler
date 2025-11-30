# RCompiler 类型检查实现文档

## 概述

本文档描述了RCompiler中类型检查系统的完整实现。该系统基于Rust的类型系统，提供了全面的类型检查功能，包括基本类型、复合类型、函数类型、引用类型等的类型检查。

## 类型系统架构

### 1. 类型接口层次结构

```
Type (接口)
├── PrimitiveType (基本类型)
├── ReferenceType (引用类型)
├── ArrayType (数组类型)
├── StructType (结构体类型)
├── FunctionType (函数类型)
├── StructConstructorType (结构体构造函数类型)
├── UnitType (单元类型)
└── NeverType (永不返回类型)
```

### 2. 类型接口定义

```java
public interface Type {
    // 检查两个类型是否相等
    boolean equals(Type other);
    
    // 获取类型的字符串表示
    String toString();
    
    // 获取基础类型（用于引用类型）
    Type getBaseType();
    
    // 检查是否为数值类型
    boolean isNumeric();
    
    // 检查是否为布尔类型
    boolean isBoolean();
    
    // 检查是否为单元类型
    boolean isUnit();
    
    // 检查是否为永不返回类型
    boolean isNever();
}
```

## 具体类型实现

### 1. PrimitiveType - 基本类型

支持Rust的基本类型：
- 整数类型：i32, u32, isize, usize, int（未确定整数）
- 布尔类型：bool
- 字符类型：char
- 字符串类型：str

```java
public class PrimitiveType implements Type {
    public enum PrimitiveKind {
        INT, I32, U32, ISIZE, USIZE, BOOL, CHAR, STR
    }
    
    private final PrimitiveKind kind;
    
    // 工厂方法
    public static PrimitiveType getIntType() { return new PrimitiveType(PrimitiveKind.INT); }
    public static PrimitiveType getI32Type() { return new PrimitiveType(PrimitiveKind.I32); }
    // ... 其他工厂方法
}
```

### 2. ReferenceType - 引用类型

支持Rust的引用系统：
- 不可变引用：&T
- 可变引用：&mut T
- 双重引用：&&T

```java
public class ReferenceType implements Type {
    private final Type innerType;
    private final boolean isMutable;
    private final boolean isDoubleReference;
    
    public ReferenceType(Type innerType, boolean isMutable, boolean isDoubleReference) {
        this.innerType = innerType;
        this.isMutable = isMutable;
        this.isDoubleReference = isDoubleReference;
    }
}
```

### 3. ArrayType - 数组类型

支持固定大小数组：[T; N]

```java
public class ArrayType implements Type {
    private final Type elementType;
    private final int size;
    
    public ArrayType(Type elementType, int size) {
        this.elementType = elementType;
        this.size = size;
    }
}
```

### 4. StructType - 结构体类型

支持结构体类型和字段访问：

```java
public class StructType implements Type {
    private final String name;
    private final Map<String, Type> fields;
    private final Symbol symbol;
    
    public Type getFieldType(String fieldName) {
        return fields.get(fieldName);
    }
}
```

### 5. FunctionType - 函数类型

支持函数类型和方法类型：

```java
public class FunctionType implements Type {
    private final List<Type> parameterTypes;
    private final Type returnType;
    private final boolean isMethod;
    
    public FunctionType(List<Type> parameterTypes, Type returnType, boolean isMethod) {
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
        this.isMethod = isMethod;
    }
}
```

## TypeChecker 实现

### 1. 基本结构

```java
public class TypeChecker extends VisitorBase {
    private final TypeErrorCollector errorCollector;
    private final boolean throwOnError;
    private Type currentType; // 表示方法中的Self类型
    
    // 控制流上下文栈
    private Stack<ControlFlowContext> contextStack = new Stack<>();
    
    // 控制流上下文类型
    private enum ControlFlowContextType {
        LOOP,      // loop循环
        WHILE,     // while循环
        FUNCTION,   // 函数
    }
}
```

### 2. 表达式类型检查

#### 简单表达式
- **字面量**：根据字面量类型确定类型
- **标识符**：从符号表中获取类型
- **分组表达式**：继承内部表达式的类型

#### 运算符表达式
- **算术表达式**：检查操作数为数值类型，结果为公共类型
- **比较表达式**：检查操作数类型兼容，结果为布尔类型
- **逻辑表达式**：检查操作数为布尔类型，结果为布尔类型
- **赋值表达式**：检查左值为可赋值，检查类型兼容性
- **复合赋值表达式**：检查左值为可赋值，检查操作数类型

#### 复杂表达式
- **函数调用**：检查函数类型，参数数量和类型匹配
- **方法调用**：检查方法类型，self参数和参数类型匹配
- **字段访问**：检查接收器为结构体类型，字段存在
- **索引访问**：检查数组类型，索引为数值类型

#### 引用表达式
- **借用表达式**：创建引用类型
- **解引用表达式**：检查操作数为引用类型，结果为内部类型
- **类型转换**：支持任意类型转换（可扩展）

#### 控制流表达式
- **块表达式**：类型为最后一个表达式的类型，空块为单元类型
- **if表达式**：条件为布尔类型，结果为分支的公共类型
- **loop表达式**：根据break语句确定类型
- **break/continue**：检查在循环内，break值类型匹配
- **return表达式**：检查在函数内，返回值类型匹配

#### 数组和结构体表达式
- **数组表达式**：确定元素类型的公共类型
- **结构体表达式**：检查字段值类型匹配

### 3. 语句类型检查

- **函数声明**：设置函数上下文，检查返回类型
- **let语句**：检查初始值类型与声明类型兼容
- **表达式语句**：检查表达式类型
- **结构体声明**：验证字段类型
- **常量声明**：检查值类型与声明类型兼容

## 错误处理

### 1. TypeCheckException

定义了各种类型检查错误类型：

```java
public class TypeCheckException extends Exception {
    public enum Type {
        TYPE_MISMATCH,              // 类型不匹配
        INVALID_OPERAND_TYPE,        // 无效操作数类型
        INVALID_ASSIGNMENT_TARGET,    // 无效赋值目标
        NOT_A_FUNCTION,             // 不是函数
        NOT_A_METHOD,               // 不是方法
        NOT_A_STRUCT,               // 不是结构体
        NOT_AN_ARRAY,               // 不是数组
        NOT_A_REFERENCE,            // 不是引用
        FIELD_NOT_FOUND,            // 字段未找到
        ARGUMENT_COUNT_MISMATCH,     // 参数数量不匹配
        ARGUMENT_TYPE_MISMATCH,      // 参数类型不匹配
        INVALID_CONDITION_TYPE,      // 无效条件类型
        INVALID_INDEX_TYPE,          // 无效索引类型
        INVALID_ARRAY_SIZE,          // 无效数组大小
        ARRAY_ELEMENT_TYPE_MISMATCH, // 数组元素类型不匹配
        FIELD_TYPE_MISMATCH,         // 字段类型不匹配
        IF_ELSE_TYPE_MISMATCH,     // if-else类型不匹配
        BREAK_TYPE_MISMATCH,        // break类型不匹配
        RETURN_TYPE_MISMATCH,        // 返回类型不匹配
        BREAK_OUTSIDE_LOOP,         // break在循环外
        CONTINUE_OUTSIDE_LOOP,      // continue在循环外
        RETURN_OUTSIDE_FUNCTION,     // return在函数外
        UNRESOLVED_SYMBOL,          // 未解析符号
        NULL_SYMBOL,                // 空符号
        NULL_TYPE_NODE,             // 空类型节点
        INVALID_EXPRESSION,          // 无效表达式
        INVALID_TYPE_EXTRACTION,     // 无效类型提取
        UNSUPPORTED_SYMBOL_KIND,     // 不支持的符号种类
        EMPTY_ARRAY_WITHOUT_TYPE     // 无类型注解的空数组
    }
}
```

### 2. TypeErrorCollector

收集和管理类型检查错误：

```java
public class TypeErrorCollector {
    private final List<TypeCheckException> errors;
    
    public void addError(TypeCheckException error);
    public void addError(TypeCheckException.Type errorType, String message, ASTNode node);
    public List<TypeCheckException> getErrors();
    public boolean hasErrors();
    public void throwFirstError() throws TypeCheckException;
    public void printErrors();
    public void clearErrors();
    public int getErrorCount();
}
```

## 与主编译流程的集成

类型检查器已集成到Main.java的主编译流程中：

```java
// 执行类型检查
try {
    TypeChecker typeChecker = new TypeChecker(false); // 不抛出错误，收集所有错误
    
    for (StmtNode stmt : parser.getStatements()) {
        stmt.accept(typeChecker);
    }
    
    // 检查类型错误
    if (typeChecker.hasErrors()) {
        System.err.println("Type checking errors:");
        typeChecker.getErrorCollector().printErrors();
    } else {
        System.out.println("Type checking completed successfully.");
    }
    
} catch (TypeCheckException e) {
    System.err.println("Type checking error: " + e.getMessage());
    if (e.getNode() != null) {
        System.err.println("  at node: " + e.getNode().getClass().getSimpleName());
    }
}
```

## 类型推断和类型缓存

### 1. 符号类型缓存

Symbol类已扩展以支持类型缓存：

```java
public class Symbol {
    // 现有字段...
    private Type type; // 缓存的类型信息
    
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
}
```

### 2. 类型推断

系统支持基本的类型推断：
- 未确定整数类型可以根据上下文推断为具体整数类型
- 数组元素类型可以从元素推断
- 变量类型可以从初始值推断

## 控制流分析

TypeChecker实现了控制流分析以支持：
- break/continue语句的作用域检查
- return语句的函数作用域检查
- loop表达式的类型确定（基于break语句）
- if表达式的类型确定（基于分支类型）

## 扩展性

该类型检查系统设计为可扩展的：
- 可以轻松添加新的类型
- 可以扩展类型转换规则
- 可以添加新的表达式类型检查
- 可以增强错误报告

## 测试

类型检查系统可以通过以下方式测试：
1. 创建包含各种类型错误的测试用例
2. 验证错误检测和报告
3. 测试类型推断的正确性
4. 验证复杂表达式的类型检查

## 结论

RCompiler的类型检查系统提供了全面的类型检查功能，支持Rust类型系统的主要特性。该系统与现有的符号表和命名空间分析系统无缝集成，为编译器提供了强大的类型安全保障。