# RCompiler 类型检查使用指南

## 概述

RCompiler现在包含完整的类型检查系统，可以在编译时检测类型错误。本指南详细介绍如何使用和理解类型检查功能，包括类型系统架构、文件结构、类型检查规则、错误处理机制以及高级特性。

## 文件架构

### 整体架构

RCompiler的类型检查系统采用模块化设计，主要分为以下几个核心模块：

```
src/main/java/
├── ast/                          # 抽象语法树(AST)相关
│   ├── AST.java                  # AST节点定义
│   └── ASTPrintException.java     # AST打印异常
├── semantic_check/               # 语义检查模块
│   ├── analyzer/                 # 分析器模块
│   │   ├── TypeChecker.java      # 类型检查器(主要实现)
│   │   ├── SymbolChecker.java    # 符号检查器
│   │   ├── NamespaceAnalyzer.java # 命名空间分析器
│   │   └── SymbolAdder.java     # 符号添加器
│   ├── type/                    # 类型系统模块
│   │   ├── Type.java            # 类型接口
│   │   ├── PrimitiveType.java   # 基本类型实现
│   │   ├── ReferenceType.java   # 引用类型实现
│   │   ├── ArrayType.java       # 数组类型实现
│   │   ├── StructType.java      # 结构体类型实现
│   │   ├── FunctionType.java    # 函数类型实现
│   │   ├── StructConstructorType.java # 结构体构造函数类型
│   │   ├── UnitType.java        # 单元类型实现
│   │   └── NeverType.java      # 永不返回类型实现
│   ├── symbol/                  # 符号系统模块
│   │   ├── Symbol.java          # 符号定义
│   │   ├── SymbolKind.java      # 符号种类枚举
│   │   └── NamespaceSymbolTable.java # 命名空间符号表
│   ├── error/                   # 错误处理模块
│   │   ├── TypeCheckException.java # 类型检查异常
│   │   ├── TypeErrorCollector.java  # 类型错误收集器
│   │   ├── SemanticError.java      # 语义错误
│   │   ├── SemanticException.java  # 语义异常
│   │   └── ErrorType.java         # 错误类型枚举
│   ├── context/                 # 上下文模块
│   │   ├── Context.java         # 上下文定义
│   │   ├── ContextType.java     # 上下文类型枚举
│   │   └── ContextInfo.java     # 上下文信息
│   ├── self/                   # Self语义模块
│   │   ├── SelfChecker.java     # Self检查器
│   │   ├── SelfErrorType.java   # Self错误类型
│   │   └── SelfSemanticAnalyzer.java # Self语义分析器
│   ├── core/                   # 核心模块
│   │   ├── Type.java           # 类型接口(与type/Type.java相同)
│   │   ├── Context.java        # 上下文定义(与context/Context.java相同)
│   │   ├── ContextType.java    # 上下文类型枚举(与context/ContextType.java相同)
│   │   └── Namespace.java      # 命名空间定义
│   └── debug/                  # 调试模块
│       ├── SymbolDebugTest.java  # 符号调试测试
│       └── SymbolDebugVisitor.java # 符号调试访问器
└── Main.java                   # 主程序入口
```

### 核心文件说明

#### 1. AST模块 (`src/main/java/ast/`)

- **AST.java**: 定义了所有AST节点类，包括表达式节点、语句节点、类型节点等
  - `ASTNode`: 所有AST节点的基类
  - `ExprNode`: 表达式节点基类
  - `StmtNode`: 语句节点基类
  - 各种具体节点类：`LiteralExprNode`, `BinaryExprNode`, `FunctionNode`, `StructNode`等

#### 2. 类型检查器 (`src/main/java/semantic_check/analyzer/TypeChecker.java`)

类型检查的核心实现，使用访问者模式遍历AST并进行类型检查：
- 实现了所有AST节点的`visit`方法
- 维护控制流上下文栈
- 提供类型推断和类型比较功能
- 收集和报告类型错误

#### 3. 类型系统 (`src/main/java/semantic_check/type/`)

- **Type.java**: 类型接口，定义了所有类型必须实现的方法
- **PrimitiveType.java**: 基本类型实现（i32, u32, bool等）
- **ReferenceType.java**: 引用类型实现（&T, &mut T）
- **ArrayType.java**: 数组类型实现（[T; N]）
- **StructType.java**: 结构体类型实现
- **FunctionType.java**: 函数类型实现
- **StructConstructorType.java**: 结构体构造函数类型
- **UnitType.java**: 单元类型实现（()）
- **NeverType.java**: 永不返回类型实现（!）

#### 4. 符号系统 (`src/main/java/semantic_check/symbol/`)

- **Symbol.java**: 符号定义，包含符号名称、种类、声明节点等信息
- **SymbolKind.java**: 符号种类枚举（FUNCTION, VARIABLE, STRUCT等）
- **NamespaceSymbolTable.java**: 命名空间符号表，管理符号的查找和作用域

#### 5. 错误处理 (`src/main/java/semantic_check/error/`)

- **TypeCheckException.java**: 类型检查异常定义
- **TypeErrorCollector.java**: 类型错误收集器，收集和管理类型检查错误
- **SemanticError.java**: 语义错误定义
- **SemanticException.java**: 语义异常定义
- **ErrorType.java**: 错误类型枚举

### 模块间交互

#### 1. 类型检查流程

```
源代码 → 词法分析 → 语法分析 → AST构建 → 符号表构建 → 类型检查 → 错误报告
```

详细流程：
1. **AST构建**: 解析器将源代码解析为AST节点
2. **符号表构建**: SymbolChecker遍历AST，构建符号表
3. **类型检查**: TypeChecker遍历AST，使用符号表进行类型检查
4. **错误收集**: TypeErrorCollector收集所有类型错误
5. **错误报告**: 输出类型错误信息

#### 2. 数据流

```
AST节点 → TypeChecker → 类型信息 → 符号表
    ↓
错误信息 → TypeErrorCollector → 错误报告
```

#### 3. 模块依赖关系

```
TypeChecker
    ↓ 依赖
Type系统 (PrimitiveType, ReferenceType, etc.)
    ↓ 依赖
Symbol系统 (Symbol, SymbolKind, NamespaceSymbolTable)
    ↓ 依赖
AST系统 (ASTNode, ExprNode, StmtNode)
    ↓ 依赖
错误系统 (TypeCheckException, TypeErrorCollector)
```

### 关键类和方法

#### 1. TypeChecker核心方法

```java
public class TypeChecker extends VisitorBase {
    // 类型获取和设置
    private Type getType(ExprNode expr) { return expr.getType(); }
    private void setType(ExprNode expr, Type type) { expr.setType(type); }
    
    // 类型检查核心方法
    private boolean isAssignable(ExprNode expr) throws TypeCheckException;
    private Type findCommonType(Type type1, Type type2);
    private boolean isNumericType(Type type);
    
    // 符号类型提取
    private Type extractTypeFromSymbol(Symbol symbol) throws TypeCheckException;
    private Type extractTypeFromTypeNode(TypeExprNode typeNode) throws TypeCheckException;
    
    // 控制流管理
    private void enterLoopContext(LoopExprNode node);
    private void exitLoopContext();
    private ControlFlowContext findNearestLoopContext();
    
    // 错误处理
    private void throwTypeError(TypeCheckException.Type errorType, String message, ASTNode node);
}
```

#### 2. Type接口实现

```java
// 基本类型实现
public class PrimitiveType implements Type {
    public enum PrimitiveKind { INT, I32, U32, ISIZE, USIZE, BOOL, CHAR, STR }
    
    private final PrimitiveKind kind;
    
    // 工厂方法
    public static PrimitiveType getIntType() { return new PrimitiveType(PrimitiveKind.INT); }
    public static PrimitiveType getI32Type() { return new PrimitiveType(PrimitiveKind.I32); }
    
    // 类型检查方法
    public boolean isNumeric() { /* 实现 */ }
    public boolean isBoolean() { /* 实现 */ }
    public boolean equals(Type other) { /* 实现 */ }
}

// 引用类型实现
public class ReferenceType implements Type {
    private final Type innerType;
    private final boolean isMutable;
    private final boolean isDoubleReference;
    
    public Type getInnerType() { return innerType; }
    public boolean isMutable() { return isMutable; }
    public boolean isDoubleReference() { return isDoubleReference; }
}
```

#### 3. Symbol系统

```java
public class Symbol {
    private String name;
    private SymbolKind kind;
    private ASTNode declaration;
    private Type type; // 缓存的类型信息
    
    // getter和setter方法
}

public enum SymbolKind {
    FUNCTION, STRUCT, ENUM, CONSTANT, TRAIT, IMPL,
    PARAMETER, LOCAL_VARIABLE, FIELD, METHOD,
    STRUCT_CONSTRUCTOR, ENUM_VARIANT_CONSTRUCTOR, SELF_PARAMETER
}

public class NamespaceSymbolTable {
    private Map<String, Symbol> symbols;
    private NamespaceSymbolTable parent;
    
    public Symbol lookup(String name) { /* 实现 */ }
    public void addSymbol(Symbol symbol) { /* 实现 */ }
    public void enterScope() { /* 实现 */ }
    public void exitScope() { /* 实现 */ }
}
```

### 设计模式

#### 1. 访问者模式 (Visitor Pattern)

类型检查器使用访问者模式遍历AST：
```java
public class TypeChecker extends VisitorBase {
    public void visit(LiteralExprNode node) throws TypeCheckException {
        // 处理字面量表达式
    }
    
    public void visit(BinaryExprNode node) throws TypeCheckException {
        // 处理二元表达式
    }
    
    // ... 其他visit方法
}
```

#### 2. 工厂模式 (Factory Pattern)

类型创建使用工厂方法：
```java
public class PrimitiveType implements Type {
    public static PrimitiveType getI32Type() { return new PrimitiveType(PrimitiveKind.I32); }
    public static PrimitiveType getBoolType() { return new PrimitiveType(PrimitiveKind.BOOL); }
    // ... 其他工厂方法
}
```

#### 3. 单例模式 (Singleton Pattern)

特殊类型使用单例模式：
```java
public class UnitType implements Type {
    public static final UnitType INSTANCE = new UnitType();
    
    private UnitType() {}
}

public class NeverType implements Type {
    public static final NeverType INSTANCE = new NeverType();
    
    private NeverType() {}
}
```

#### 4. 策略模式 (Strategy Pattern)

不同类型检查策略：
```java
// 类型比较策略
private Type findCommonType(Type type1, Type type2) {
    if (type1 instanceof PrimitiveType && type2 instanceof PrimitiveType) {
        return findCommonPrimitiveType((PrimitiveType)type1, (PrimitiveType)type2);
    } else if (type1 instanceof ArrayType && type2 instanceof ArrayType) {
        return findCommonArrayType((ArrayType)type1, (ArrayType)type2);
    }
    // ... 其他类型比较策略
}
```

### 扩展点

#### 1. 添加新类型

要添加新的类型支持，需要：

1. 在`Type`接口中添加必要的方法
2. 创建新的类型实现类实现`Type`接口
3. 在`TypeChecker`中添加相应的类型检查逻辑
4. 更新`findCommonType`方法以支持新类型
5. 在`extractTypeFromTypeNode`中添加类型解析逻辑

#### 2. 添加新表达式

要添加新的表达式类型，需要：

1. 在`AST.java`中添加新的表达式节点类
2. 在`TypeChecker`中添加对应的`visit`方法
3. 实现类型检查逻辑
4. 更新`isAssignable`方法（如果适用）

#### 3. 扩展错误处理

要添加新的错误类型，需要：

1. 在`TypeCheckException.Type`枚举中添加新的错误类型
2. 在`TypeChecker`中添加相应的错误检测逻辑
3. 更新错误信息格式（如果需要）

## 类型系统架构

### 类型接口层次结构

RCompiler的类型系统基于面向对象设计，所有类型都实现统一的Type接口：

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

### Type接口核心方法

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

## 支持的类型

### 1. 基本类型 (PrimitiveType)

支持Rust的基本类型：

- **整数类型**：
  - `i32` - 32位有符号整数
  - `u32` - 32位无符号整数
  - `isize` - 指针大小的有符号整数
  - `usize` - 指针大小的无符号整数
  - `int` - 未确定整数类型（用于类型推断）

- **非数值类型**：
  - `bool` - 布尔类型
  - `char` - 字符类型
  - `str` - 字符串类型

### 2. 引用类型 (ReferenceType)

支持Rust的引用系统：

- **不可变引用**：`&T`
- **可变引用**：`&mut T`
- **双重引用**：`&&T`

引用类型包含以下属性：
- `innerType` - 被引用的类型
- `isMutable` - 是否为可变引用
- `isDoubleReference` - 是否为双重引用

### 3. 数组类型 (ArrayType)

支持固定大小数组：`[T; N]`

数组类型包含：
- `elementType` - 数组元素类型
- `size` - 数组大小（编译时常量）

### 4. 结构体类型 (StructType)

支持结构体类型和字段访问：

结构体类型包含：
- `name` - 结构体名称
- `fields` - 字段名到类型的映射
- `symbol` - 关联的符号表项

### 5. 函数类型 (FunctionType)

支持函数类型和方法类型：

函数类型包含：
- `parameterTypes` - 参数类型列表
- `returnType` - 返回类型
- `isMethod` - 是否为方法（包含self参数）

### 6. 特殊类型

- **单元类型**：`()` - 表示无返回值或空值
- **永不返回类型**：`!` - 表示函数永不返回（如panic、无限循环）

## 类型检查规则

### 1. 表达式类型检查

#### 简单表达式
- **字面量**：根据字面量类型确定类型
  - 整数字面量默认为`int`类型（未确定整数）
  - 布尔字面量为`bool`类型
  - 字符字面量为`char`类型
  - 字符串字面量为`str`类型

- **标识符**：从符号表中获取类型
- **分组表达式**：继承内部表达式的类型

#### 运算符表达式
- **算术表达式** (`+`, `-`, `*`, `/`, `%`)：
  - 操作数必须为数值类型
  - 结果为操作数的公共类型
  - 支持类型推断：`int`类型可以推断为具体数值类型

- **比较表达式** (`==`, `!=`, `<`, `>`, `<=`, `>=`)：
  - 操作数类型必须兼容
  - 结果始终为`bool`类型

- **逻辑表达式** (`&&`, `||`)：
  - 操作数必须为`bool`类型
  - 结果始终为`bool`类型

- **赋值表达式** (`=`)：
  - 左值必须可赋值（路径表达式、字段访问、索引访问、解引用）
  - 左右类型必须兼容
  - 结果为单元类型`()`

- **复合赋值表达式** (`+=`, `-=`, `*=`, `/=`, `%=`)：
  - 左值必须可赋值
  - 操作数必须为数值类型（算术复合赋值）
  - 结果为单元类型`()`

#### 复杂表达式
- **函数调用**：
  - 检查函数存在性
  - 参数数量必须匹配
  - 参数类型必须匹配
  - 结果类型为函数返回类型

- **方法调用**：
  - 检查方法存在性
  - 检查接收器类型
  - 参数数量和类型必须匹配（不包括self参数）
  - 结果类型为方法返回类型

- **字段访问**：
  - 接收器必须为结构体类型
  - 字段必须存在于结构体中
  - 结果类型为字段类型

- **索引访问**：
  - 接收器必须为数组类型
  - 索引必须为数值类型
  - 结果类型为数组元素类型

#### 引用表达式
- **借用表达式** (`&`, `&mut`)：
  - 创建引用类型
  - 结果为引用类型

- **解引用表达式** (`*`)：
  - 操作数必须为引用类型
  - 结果为引用的内部类型

- **类型转换** (`as`)：
  - 支持任意类型转换（可扩展）
  - 结果为目标类型

#### 控制流表达式
- **块表达式**：
  - 类型为最后一个表达式的类型
  - 空块为单元类型`()`

- **if表达式**：
  - 条件必须为`bool`类型
  - 有else分支时，结果为分支的公共类型
  - 无else分支时，结果为单元类型`()`

- **loop表达式**：
  - 根据break语句确定类型
  - 无break时为`!`类型
  - 多个break时为break值的公共类型

- **break/continue**：
  - 必须在循环内
  - break值类型必须匹配循环类型
  - 结果为`!`类型

- **return表达式**：
  - 必须在函数内
  - 返回值类型必须与函数签名匹配
  - 结果为`!`类型

#### 数组和结构体表达式
- **数组表达式**：
  - 确定元素类型的公共类型
  - 结果为数组类型`[T; N]`

- **结构体表达式**：
  - 检查字段值类型匹配
  - 结果为结构体类型

### 2. 语句类型检查

- **函数声明**：
  - 设置函数上下文
  - 检查返回类型

- **let语句**：
  - 检查初始值类型与声明类型兼容
  - 支持类型推断

- **表达式语句**：
  - 检查表达式类型

- **结构体声明**：
  - 验证字段类型

- **常量声明**：
  - 检查值类型与声明类型兼容

## 错误处理

### 错误类型

类型检查器会报告以下类型的错误：

1. **类型不匹配** (TYPE_MISMATCH)
   - 表达式中类型不兼容
   - 示例：`let x: i32 = true;`

2. **无效操作数类型** (INVALID_OPERAND_TYPE)
   - 运算符的操作数类型不正确
   - 示例：`5 + true;`

3. **无效赋值目标** (INVALID_ASSIGNMENT_TARGET)
   - 赋值表达式的左值不可赋值
   - 示例：`5 = x;`

4. **不是函数** (NOT_A_FUNCTION)
   - 尝试调用非函数类型
   - 示例：`let x = 5; x();`

5. **不是方法** (NOT_A_METHOD)
   - 尝试以方法方式调用非方法函数
   - 示例：`SomeFunction.some_method();`

6. **不是结构体** (NOT_A_STRUCT)
   - 尝试访问非结构体类型的字段
   - 示例：`let x = 5; x.field;`

7. **不是数组** (NOT_AN_ARRAY)
   - 尝试对非数组类型进行索引访问
   - 示例：`let x = 5; x[0];`

8. **不是引用** (NOT_A_REFERENCE)
   - 尝试解引用非引用类型
   - 示例：`let x = 5; *x;`

9. **字段未找到** (FIELD_NOT_FOUND)
   - 访问不存在的结构体字段
   - 示例：`let p = Point { x: 1, y: 2 }; let z = p.z;`

10. **参数数量不匹配** (ARGUMENT_COUNT_MISMATCH)
    - 函数调用参数数量不匹配
    - 示例：`fn foo(x: i32) { } foo(1, 2);`

11. **参数类型不匹配** (ARGUMENT_TYPE_MISMATCH)
    - 函数调用参数类型不匹配
    - 示例：`fn foo(x: i32) { } foo(true);`

12. **无效条件类型** (INVALID_CONDITION_TYPE)
    - if/while条件不是布尔类型
    - 示例：`if 5 { }`

13. **无效索引类型** (INVALID_INDEX_TYPE)
    - 数组索引不是数值类型
    - 示例：`let arr = [1, 2, 3]; arr[true];`

14. **无效数组大小** (INVALID_ARRAY_SIZE)
    - 数组大小不是数值类型
    - 示例：`let arr = [1; true];`

15. **数组元素类型不匹配** (ARRAY_ELEMENT_TYPE_MISMATCH)
    - 数组元素类型不兼容
    - 示例：`let arr = [1, true];`

16. **字段类型不匹配** (FIELD_TYPE_MISMATCH)
    - 结构体字段值类型不匹配
    - 示例：`let p = Point { x: 1, y: true };`

17. **if-else类型不匹配** (IF_ELSE_TYPE_MISMATCH)
    - if和else分支类型不兼容
    - 示例：`let x = if true { 1 } else { true };`

18. **break类型不匹配** (BREAK_TYPE_MISMATCH)
    - loop中break值类型不匹配
    - 示例：`let x = loop { break 1; break true; };`

19. **返回类型不匹配** (RETURN_TYPE_MISMATCH)
    - return语句类型与函数签名不匹配
    - 示例：`fn bar() -> i32 { return true; }`

20. **break在循环外** (BREAK_OUTSIDE_LOOP)
    - break语句不在循环内
    - 示例：`break;`

21. **continue在循环外** (CONTINUE_OUTSIDE_LOOP)
    - continue语句不在循环内
    - 示例：`continue;`

22. **return在函数外** (RETURN_OUTSIDE_FUNCTION)
    - return语句不在函数内
    - 示例：`return 5;`

23. **未解析符号** (UNRESOLVED_SYMBOL)
    - 无法解析的标识符
    - 示例：`let x = undefined_variable;`

24. **空符号** (NULL_SYMBOL)
    - 符号为空
    - 内部错误

25. **空类型节点** (NULL_TYPE_NODE)
    - 类型节点为空
    - 内部错误

26. **无效表达式** (INVALID_EXPRESSION)
    - 表达式结构无效
    - 内部错误

27. **无效类型提取** (INVALID_TYPE_EXTRACTION)
    - 无法从符号提取类型
    - 内部错误

28. **不支持的符号种类** (UNSUPPORTED_SYMBOL_KIND)
    - 不支持的符号类型
    - 内部错误

29. **无类型注解的空数组** (EMPTY_ARRAY_WITHOUT_TYPE)
    - 空数组需要类型注解
    - 示例：`let arr = [];`

### 错误信息格式

```
Type checking errors:
  [错误信息] at [AST节点类型]
    at [具体节点位置]
```

### 错误收集机制

RCompiler使用`TypeErrorCollector`类来收集和管理类型检查错误：

- 支持收集多个错误而不是在第一个错误时停止
- 可以选择在遇到错误时立即抛出异常
- 提供错误计数和打印功能
- 支持错误清理和重用

### 错误恢复策略

类型检查器采用错误恢复策略，以便在一次编译过程中检测尽可能多的错误：

1. 遇到错误时设置默认类型，继续检查
2. 使用`int`类型作为未确定类型的默认值
3. 跳过无法解析的符号但继续检查其他部分
4. 在表达式类型不确定时使用假设类型继续检查

## 使用示例

### 正确的类型使用

```rust
fn main() {
    // 基本类型使用
    let x: i32 = 5;
    let y: i32 = 10;
    let z = x + y;  // 正确：两个i32相加
    
    if z > 10 {        // 正确：比较两个i32
        println!("z is greater than 10");
    }
    
    // 结构体定义和使用
    struct Point {
        x: i32,
        y: i32,
    }
    
    let p = Point { x: 1, y: 2 };  // 正确：结构体初始化
    let px = p.x;                   // 正确：字段访问
    
    // 数组使用
    let arr = [1, 2, 3];  // 正确：数组初始化
    let first = arr[0];   // 正确：数组索引访问
    
    // 引用使用
    let ref_x = &x;      // 正确：创建不可变引用
    let mut_ref_x = &mut x; // 正确：创建可变引用
    let deref_x = *ref_x;   // 正确：解引用
    
    // 函数调用
    let result = add(x, y); // 正确：函数调用
}

fn add(a: i32, b: i32) -> i32 {
    a + b
}

// 方法定义和调用
impl Point {
    fn new(x: i32, y: i32) -> Self {
        Point { x, y }
    }
    
    fn distance_from_origin(&self) -> f32 {
        ((self.x * self.x + self.y * self.y) as f32).sqrt()
    }
}

fn method_example() {
    let p = Point::new(3, 4);
    let dist = p.distance_from_origin(); // 正确：方法调用
}
```

### 类型错误示例

```rust
fn main() {
    // 基本类型错误
    let x: i32 = true;        // 错误：类型不匹配
    let y = 5 + "hello";     // 错误：无效操作数类型
    
    // 结构体相关错误
    struct Point {
        x: i32,
        y: i32,
    }
    
    let p = Point { x: 1, y: "two" };  // 错误：字段类型不匹配
    let z = p.z;                       // 错误：字段未找到
    
    // 数组相关错误
    let arr = [1, true, 3];    // 错误：数组元素类型不匹配
    let empty_arr = [];        // 错误：无类型注解的空数组
    
    // 引用相关错误
    let x = 5;
    let ref_x = &x;
    let invalid_deref = *x;    // 错误：不是引用类型
    
    // 函数调用错误
    fn foo(a: i32) -> i32 { a }
    let result = foo(true);    // 错误：参数类型不匹配
    let result2 = foo(1, 2);   // 错误：参数数量不匹配
    
    // 控制流错误
    if 5 {                     // 错误：无效条件类型
        println!("This won't work");
    }
    
    fn bar() -> i32 {
        return true;           // 错误：返回类型不匹配
    }
    
    break;                     // 错误：break在循环外
}
```

## 类型推断

### 基本类型推断

系统支持基本的类型推断：

```rust
fn main() {
    let x = 5;        // x推断为int（未确定整数）
    let y = 10;       // y推断为int（未确定整数）
    let z = x + y;    // z推断为int（未确定整数）
    
    // 在需要具体类型时，int会被推断为具体类型
    let a: i32 = x;   // x被推断为i32
    let b: u32 = y;   // y被推断为u32
    
    let arr = [1, 2, 3];  // arr推断为[int; 3]
}
```

### 类型推断规则

1. **字面量推断**：
   - 整数字面量默认为`int`类型
   - 布尔字面量为`bool`类型
   - 字符字面量为`char`类型
   - 字符串字面量为`str`类型

2. **表达式推断**：
   - 算术表达式结果为操作数的公共类型
   - 比较表达式结果为`bool`类型
   - 逻辑表达式结果为`bool`类型

3. **变量推断**：
   - let语句中的变量类型从初始值推断
   - 函数参数类型从参数注解推断
   - 函数返回类型从返回注解推断

4. **上下文推断**：
   - `int`类型可以根据上下文推断为具体整数类型
   - 数组元素类型可以从元素推断
   - 函数调用参数类型可以从函数签名推断

### 类型推断限制

当前类型推断系统的限制：

1. 不支持泛型类型推断
2. 不支持复杂的类型推导（如闭包类型）
3. 空数组需要显式类型注解
4. 某些复杂表达式可能需要类型注解

## 控制流分析

### 控制流上下文

TypeChecker实现了控制流分析以支持：

1. **break/continue语句的作用域检查**：
   - 确保break/continue在循环内
   - 支持嵌套循环的正确跳转

2. **return语句的函数作用域检查**：
   - 确保return在函数内
   - 支持嵌套函数的正确返回

3. **loop表达式的类型确定**：
   - 基于break语句确定loop类型
   - 处理多个break语句的类型统一

4. **if表达式的类型确定**：
   - 基于分支类型确定if表达式类型
   - 处理有else和无else的情况

### 控制流上下文栈

TypeChecker使用栈结构管理控制流上下文：

```java
// 控制流上下文类型
private enum ControlFlowContextType {
    LOOP,      // loop循环
    WHILE,     // while循环
    FUNCTION,   // 函数
}

// 控制流上下文类
private static class ControlFlowContext {
    private final ControlFlowContextType type;
    private final ASTNode node;
    private List<Type> breakTypes;
    // ...
}
```

## 类型缓存和符号系统

### 符号类型缓存

Symbol类已扩展以支持类型缓存：

```java
public class Symbol {
    // 现有字段...
    private Type type; // 缓存的类型信息
    
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
}
```

### 类型提取机制

TypeChecker实现了从符号提取类型的机制：

1. **函数类型提取**：
   - 从函数声明提取参数类型和返回类型
   - 区分函数和方法（是否有self参数）

2. **结构体构造函数类型提取**：
   - 从结构体声明提取字段类型
   - 创建结构体构造函数类型

3. **常量类型提取**：
   - 从常量声明提取类型
   - 支持类型注解和值推断

4. **参数类型提取**：
   - 从参数声明提取类型
   - 支持复杂类型表达式

5. **局部变量类型提取**：
   - 从let语句提取类型
   - 支持类型注解和初始值推断

## 集成到编译流程

类型检查自动集成到编译流程中，在命名空间分析之后执行：

1. 词法分析
2. 语法分析
3. 父关系设置
4. Self语义检查
5. 命名空间语义检查
6. **类型检查** ← 新增
7. 后续编译阶段

### 集成代码示例

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

## 扩展性

类型检查系统设计为可扩展的：

### 添加新类型支持

1. 创建新的Type实现类
2. 实现Type接口的所有方法
3. 在TypeChecker中添加相应的类型检查逻辑
4. 更新类型提取和推断逻辑

### 扩展类型转换规则

1. 在TypeChecker中添加类型转换检查
2. 实现自定义类型转换逻辑
3. 更新类型推断规则

### 增强错误报告

1. 添加新的TypeCheckException.Type枚举值
2. 在TypeChecker中添加相应的错误检测
3. 扩展TypeErrorCollector的错误处理

### 添加更复杂的类型推断

1. 实现Hindley-Milner类型推断算法
2. 添加类型变量和类型统一
3. 支持多态类型和泛型

## 测试

### 测试类型检查功能

类型检查系统可以通过以下方式测试：

1. **创建包含各种类型错误的测试用例**：
   - 基本类型错误
   - 复合类型错误
   - 控制流错误
   - 函数调用错误

2. **验证错误检测和报告**：
   - 检查错误类型是否正确
   - 验证错误位置是否准确
   - 确认错误信息是否清晰

3. **测试类型推断的正确性**：
   - 验证基本类型推断
   - 测试复杂表达式推断
   - 检查上下文相关推断

4. **验证复杂表达式的类型检查**：
   - 嵌套函数调用
   - 复杂控制流
   - 混合类型操作

### 测试示例

```rust
// 测试文件：test_type_check.rs

// 测试基本类型错误
fn test_basic_type_errors() {
    let x: i32 = true;  // 应该报错：TYPE_MISMATCH
    let y = 5 + "hello"; // 应该报错：INVALID_OPERAND_TYPE
}

// 测试结构体错误
struct Point {
    x: i32,
    y: i32,
}

fn test_struct_errors() {
    let p = Point { x: 1, y: "two" };  // 应该报错：FIELD_TYPE_MISMATCH
    let z = p.z;                       // 应该报错：FIELD_NOT_FOUND
}

// 测试函数调用错误
fn foo(a: i32) -> i32 { a }

fn test_function_errors() {
    let result = foo(true);    // 应该报错：ARGUMENT_TYPE_MISMATCH
    let result2 = foo(1, 2);   // 应该报错：ARGUMENT_COUNT_MISMATCH
}

// 测试控制流错误
fn test_control_flow_errors() {
    if 5 {                     // 应该报错：INVALID_CONDITION_TYPE
        println!("This won't work");
    }
    
    break;                     // 应该报错：BREAK_OUTSIDE_LOOP
}

fn test_return_errors() -> i32 {
    return true;               // 应该报错：RETURN_TYPE_MISMATCH
}
```

## 性能考虑

### 类型检查性能优化

1. **类型缓存**：
   - 缓存符号的类型信息
   - 避免重复类型计算

2. **延迟类型检查**：
   - 只在需要时进行类型检查
   - 避免不必要的类型计算

3. **错误收集优化**：
   - 使用高效的错误收集机制
   - 避免错误收集过程中的性能损失

4. **内存管理**：
   - 合理管理类型对象生命周期
   - 避免内存泄漏

## 未来发展方向

### 短期目标

1. **增强类型推断**：
   - 支持更复杂的类型推断
   - 改进类型推断错误报告

2. **扩展类型系统**：
   - 添加更多Rust类型支持
   - 支持自定义类型

3. **改进错误报告**：
   - 提供更详细的错误信息
   - 添加错误修复建议

### 长期目标

1. **泛型支持**：
   - 实现泛型类型系统
   - 支持泛型函数和结构体

2. **trait系统**：
   - 实现trait类型检查
   - 支持trait对象

3. **生命周期检查**：
   - 实现生命周期分析
   - 支持借用检查

## 详细文档

更多技术细节请参考：
- [类型检查实现文档](design/type_checking_implementation.md)
- [语义检查设计文档](design/semantic_check_design.md)