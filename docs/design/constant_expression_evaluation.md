# 常量表达式求值设计文档

## 概述

本文档描述了RCompiler中常量表达式求值的设计和实现。常量表达式求值是编译时计算表达式值的过程，对于数组类型大小、常量声明等场景至关重要。

## 背景

在Rust中，某些表达式必须在编译时就能确定其值，这些表达式被称为常量表达式。主要包括：

1. 数组类型的大小表达式：`[T; N]`中的N必须是常量
2. 常量声明中的初始化表达式：`const X: T = expr;`中的expr必须是常量
3. 其他需要在编译时确定值的上下文

## 架构设计

### 核心组件

#### 1. ConstantValue类

位置：`src/main/java/semantic_check/type/ConstantValue.java`

功能：存储常量表达式的求值结果

```java
public class ConstantValue {
    private final Object value;
    private final Type type;
    
    public ConstantValue(Object value, Type type) {
        this.value = value;
        this.type = type;
    }
    
    public Object getValue() { return value; }
    public Type getType() { return type; }
    
    // 类型检查方法
    public boolean isNumeric() { return type.isNumeric(); }
    public boolean isBoolean() { return type.isBoolean(); }
    public boolean isArray() { return type instanceof ArrayType; }
    // ... 其他类型检查方法
    
    // 类型转换方法
    public long getAsLong() { /* 实现 */ }
    public boolean getAsBoolean() { /* 实现 */ }
    public List<ConstantValue> getAsArray() { /* 实现 */ }
    // ... 其他类型转换方法
}
```

#### 2. ConstantEvaluator类

位置：`src/main/java/semantic_check/analyzer/ConstantEvaluator.java`

功能：实现常量表达式的求值逻辑

```java
public class ConstantEvaluator extends VisitorBase {
    private final TypeErrorCollector errorCollector;
    private final boolean throwOnError;
    private Map<ExprNode, ConstantValue> evaluatedValues = new HashMap<>();
    
    public ConstantValue evaluate(ExprNode expr) {
        // 检查是否已求值
        if (evaluatedValues.containsKey(expr)) {
            return evaluatedValues.get(expr);
        }
        
        // 访问表达式节点进行求值
        expr.accept(this);
        
        return evaluatedValues.get(expr);
    }
    
    // 各种visit方法实现不同类型表达式的求值
    public void visit(LiteralExprNode node) { /* 字面量求值 */ }
    public void visit(PathExprNode node) { /* 常量符号求值 */ }
    public void visit(ArrayExprNode node) { /* 数组表达式求值 */ }
    public void visit(ArithExprNode node) { /* 算术表达式求值 */ }
    // ... 其他表达式类型的求值方法
}
```

#### 3. TypeChecker集成

位置：`src/main/java/semantic_check/analyzer/TypeChecker.java`

功能：在类型检查过程中使用常量表达式求值

```java
public class TypeChecker extends VisitorBase {
    private final ConstantEvaluator constantEvaluator;
    
    // 在需要常量求值的地方使用
    private Type extractTypeFromTypeArrayExpr(TypeArrayExprNode arrayExpr) {
        // 使用常量求值器计算数组大小
        ConstantValue sizeValue = constantEvaluator.evaluate(arrayExpr.size);
        
        if (sizeValue == null) {
            throw new RuntimeException("Array size expression is not a constant");
        }
        
        if (!sizeValue.isNumeric()) {
            throw new RuntimeException("Array size must be numeric");
        }
        
        long arraySize = sizeValue.getAsLong();
        
        // 检查数组大小是否为负数
        if (arraySize < 0) {
            throw new RuntimeException("Array size cannot be negative: " + arraySize);
        }
        
        return new ArrayType(elementType, arraySize);
    }
}
```

#### 4. Symbol类扩展

位置：`src/main/java/semantic_check/symbol/Symbol.java`

功能：为符号添加存储常量值的能力

```java
public class Symbol {
    private Type type;
    private ConstantValue constantValue; // 新增字段
    
    // 新增方法
    public ConstantValue getConstantValue() { return constantValue; }
    public void setConstantValue(ConstantValue constantValue) { this.constantValue = constantValue; }
}
```

## 支持的常量表达式类型

### 1. 字面量

- 整数字面量：`1`, `42`, `0x100`
- 布尔字面量：`true`, `false`
- 字符字面量：`'a'`, `'中'`
- 字符串字面量：`"hello"`, `"世界"`

### 2. 算术表达式

- 基本算术：`+`, `-`, `*`, `/`, `%`
- 位运算：`&`, `|`, `^`, `<<`, `>>`
- 示例：`2 + 3`, `5 * 6`, `1 << 3`

### 3. 比较表达式

- 相等比较：`==`, `!=`
- 大小比较：`<`, `>`, `<=`, `>=`
- 示例：`x == 5`, `y > 10`

### 4. 逻辑表达式

- 逻辑与：`&&`
- 逻辑或：`||`
- 逻辑非：`!`
- 示例：`a && b`, `!c`

### 5. 类型转换

- 显式类型转换：`expr as Type`
- 示例：`x as u32`, `y as isize`

### 6. 分组表达式

- 括号分组：`(expr)`
- 示例：`(2 + 3) * 4`

### 7. 常量符号引用

- 常量标识符：`const MAX: u32 = 100;`中的`MAX`
- 示例：`arr: [i32; MAX]`

## 求值过程

### 1. 表达式遍历

使用访问者模式遍历AST，对每个表达式节点进行求值：

```java
public void visit(ArithExprNode node) {
    // 先求值左操作数
    ConstantValue leftValue = evaluate(node.left);
    // 再求值右操作数
    ConstantValue rightValue = evaluate(node.right);
    
    // 执行算术运算
    ConstantValue result = evaluateArithmeticOperation(node.operator, leftValue, rightValue);
    
    // 缓存结果
    setEvaluatedValue(node, result);
}
```

### 2. 类型检查与转换

在求值过程中进行严格的类型检查：

```java
private ConstantValue evaluateArithmeticOperation(oper_t operator, ConstantValue left, ConstantValue right) {
    // 检查操作数是否为数值类型
    if (!left.isNumeric() || !right.isNumeric()) {
        throw new RuntimeException("Arithmetic operands must be numeric");
    }
    
    // 执行运算并确定结果类型
    long leftLong = left.getAsLong();
    long rightLong = right.getAsLong();
    long result;
    
    switch (operator) {
        case ADD: result = leftLong + rightLong; break;
        case SUB: result = leftLong - rightLong; break;
        // ... 其他运算符
    }
    
    // 确定结果类型
    Type resultType = findCommonType(left.getType(), right.getType());
    return new ConstantValue(result, resultType);
}
```

### 3. 错误处理

提供详细的错误信息和恢复机制：

```java
if (throwOnError) {
    throw new RuntimeException("Array size cannot be negative: " + arraySize);
} else {
    errorCollector.addError("Array size cannot be negative: " + arraySize);
    return null;
}
```

## 使用场景

### 1. 数组类型大小

```rust
let arr: [i32; 5 + 3];  // 大小为8
let matrix: [[i32; 4]; 4];  // 大小为16
let sized: [u8; 1024];  // 大小为1024
```

### 2. 常量声明

```rust
const BUFFER_SIZE: usize = 4096;
const MAX_THREADS: u32 = 8;
const PI: f32 = 3.14159;
const FLAG: bool = true;
```

### 3. 复杂常量表达式

```rust
const PAGE_SIZE: usize = (1 << 12);  // 4096
const MASK: u32 = 0xFF00;
const VALUE: i32 = (BASE + OFFSET) * SCALE;
```

## 限制与注意事项

### 1. 支持的表达式类型

当前实现支持：
- 基本字面量
- 算术运算
- 比较运算
- 逻辑运算
- 类型转换
- 分组表达式
- 常量符号引用

暂不支持：
- 函数调用
- 方法调用
- 字段访问
- 索引访问

### 2. 溢出处理

当前实现不自动处理整数溢出，溢出行为与目标类型相关：

```rust
const MAX_U8: u8 = 255;        // 正常
const OVERFLOW: u8 = 256;      // 溢出，取决于目标类型
```

### 3. 类型推断

常量表达式求值支持基本的类型推断：

```rust
let x = 42;           // 推断为i32
let y = 42u32;        // 明确指定为u32
let z = 3.14;        // 推断为f64
```

## 测试用例

### 1. 基本字面量

```rust
const I: i32 = 42;
const B: bool = true;
const C: char = 'a';
const S: &str = "hello";
```

### 2. 算术表达式

```rust
const A: i32 = 2 + 3;
const B: i32 = 5 * 6;
const C: i32 = 10 % 3;
const D: i32 = 1 << 4;
```

### 3. 数组大小

```rust
const ARR1: [i32; 5];
const ARR2: [i32; 2 + 3];
const ARR3: [i32; 4 * 2];
```

### 4. 类型转换

```rust
const A: i32 = 42;
const B: u32 = A as u32;
const C: isize = A as isize;
```

## 未来扩展

### 1. 更多表达式类型

- 支持更多运算符：幂运算、三元运算符
- 支持更多内置常量函数：`size_of`, `align_of`
- 支持更复杂的数组表达式（如嵌套数组）

### 2. 高级特性

- 常量折叠优化
- 跨 crate 常量引用
- 编译时常量求值缓存

### 3. 错误恢复

- 更精确的错误位置信息
- 错误恢复建议
- 警告级别控制

## 总结

常量表达式求值是RCompiler编译器的重要组成部分，它使得编译器能够在编译时确定表达式的值，从而生成更高效的代码并提供更好的类型检查。当前实现涵盖了基本的常量表达式类型，并提供了良好的扩展性，可以满足大多数常见的常量表达式求值需求。