# NamespaceAnalyzer 实现文档

## 概述

`NamespaceAnalyzer` 类是一个统一的语义分析组件，结合了命名空间检查和AST访问功能。它继承自 `VisitorBase`，为类似Rust的编程语言编译器提供全面的语义分析。

## 架构

### 类层次结构

```
VisitorBase (抽象类)
    ↓
NamespaceAnalyzer (具体实现)
```

### 核心组件

1. **符号表管理**
   - 全局和当前作用域跟踪
   - 多命名空间支持（类型、值、字段）
   - 层次化作用域管理

2. **上下文管理**
   - 不同的分析上下文（TYPE_CONTEXT、VALUE_CONTEXT、FIELD_CONTEXT、LET_PATTERN_CONTEXT、PARAMETER_PATTERN_CONTEXT、FUNCTION_DECLARATION、TYPE_DECLARATION、FIELD_DECLARATION、CONST_DECLARATION等）
   - 上下文感知的符号解析
   - 新增的上下文类型支持更精确的符号创建和处理

3. **错误处理**
   - 错误处理功能已移除
   - 保留了错误检测逻辑但不再记录错误

## 核心功能

### 1. 初始化

```java
public NamespaceAnalyzer() {
    this.currentContext = Context.VALUE_CONTEXT; // 默认值上下文
}

public void initializeGlobalScope() {
    globalScope = new NamespaceSymbolTable(0);
    currentScope = globalScope;
    addBuiltinTypes(); // 添加 i32, u32, usize, isize, bool, char, str, String
    addBuiltinFunctions(); // 添加 print, println, printInt, printlnInt, getString, getInt, exit
}
```

### 2. 作用域管理

分析器维护层次化作用域结构：

- **全局作用域**：包含内置类型、内置函数和顶级声明
- **局部作用域**：为函数、块和其他嵌套构造创建
- **字段作用域**：用于结构体/枚举字段声明的专用作用域

#### 作用域操作

```java
private void enterScope() {
    currentScope = currentScope.enterScope();
}

private void exitScope() {
    currentScope = currentScope.exitScope();
    if (currentScope == null) {
        throw new RuntimeException("Attempting to exit global scope");
    }
}
```

### 3. 上下文管理

分析器在不同的上下文中操作以正确解析符号：

- **TYPE_CONTEXT**：解析类型标识符
- **VALUE_CONTEXT**：解析值标识符（变量、函数、常量）
- **FIELD_CONTEXT**：解析结构体/枚举字段
- **LET_PATTERN_CONTEXT**：在let语句中声明变量，支持直接在IdentifierNode中进行符号插入
- **PARAMETER_PATTERN_CONTEXT**：声明函数参数，支持直接在IdentifierNode中进行符号插入
- **FUNCTION_DECLARATION**：声明函数，支持直接在IdentifierNode中进行符号插入
- **TYPE_DECLARATION**：声明类型（struct、enum、trait），支持直接在IdentifierNode中进行符号插入
- **FIELD_DECLARATION**：声明字段，支持直接在IdentifierNode中进行符号插入
- **CONST_DECLARATION**：声明常量，支持直接在IdentifierNode中进行符号插入

### 4. 符号解析

#### 类型解析
- 在类型命名空间中查找
- 处理内置类型（i32, u32, usize, isize, bool, char, str, String）、用户定义类型和特殊类型（Self、self）

#### 值解析
- 在值命名空间中查找
- 处理变量、函数、常量、构造函数和内置函数

#### 字段解析
- 在字段命名空间中查找
- 需要类型信息进行正确解析

#### 关联项解析
- 处理trait方法、impl块和枚举变体
- 支持带有`::`语法的路径表达式

### 5. AST节点处理

分析器为所有AST节点类型实现访问方法：

#### 声明节点
- `FunctionNode`：带有参数和返回类型的函数声明，现在使用FUNCTION_DECLARATION上下文处理
- `StructNode`：带有字段定义的结构体声明，现在使用TYPE_DECLARATION上下文处理
- `FieldNode`：带有名称和类型的字段声明，现在使用FIELD_DECLARATION上下文处理并正确处理字段类型
- `EnumNode`：带有变体定义的枚举声明，现在使用TYPE_DECLARATION上下文处理
- `TraitNode`：带有关联项的trait声明，现在使用TYPE_DECLARATION上下文处理
- `ImplNode`：类型和trait的实现块
- `ConstItemNode`：常量声明，现在使用CONST_DECLARATION上下文处理

#### 表达式节点
- `IdentifierNode`：基于上下文的标识符解析和符号插入，现在支持在多种声明上下文中直接创建符号
- `PathExprNode`：路径表达式处理，当有两段路径时，不检查RSeg而是将当前Node的Symbol暂时记为LSeg的Symbol
- `CallExprNode`：函数调用表达式
- `MethodCallExprNode`：方法调用表达式
- `FieldExprNode`：字段访问表达式
- `StructExprNode`：结构体构造表达式

#### 语句节点
- `LetStmtNode`：带有模式匹配的变量声明，现在使用LET_PATTERN_CONTEXT上下文处理
- `ExprStmtNode`：表达式语句

#### 模式节点
- `IdPatNode`：用于变量/参数声明的标识符模式
- `ParameterNode`：函数参数声明，现在使用PARAMETER_PATTERN_CONTEXT上下文处理
- `IdentifierNode`：在LET_PATTERN_CONTEXT、PARAMETER_PATTERN_CONTEXT、FUNCTION_DECLARATION、TYPE_DECLARATION、FIELD_DECLARATION和CONST_DECLARATION中直接进行符号插入

### 6. 错误处理

分析器曾经提供全面的错误检测和报告，但错误处理功能已被移除：

#### 错误检测（保留逻辑）
- `UNDECLARED_TYPE_IDENTIFIER`：未定义的类型引用（检测逻辑保留）
- `UNDECLARED_VALUE_IDENTIFIER`：未定义的值引用（检测逻辑保留）
- `UNDECLARED_FIELD`：未定义的字段引用（检测逻辑保留）
- `UNDECLARED_ASSOCIATED_ITEM`：未定义的关联项（检测逻辑保留）
- `DUPLICATE_TYPE_DECLARATION`：重复的类型定义（检测逻辑保留）
- `DUPLICATE_VALUE_DECLARATION`：重复的值定义（检测逻辑保留）
- `DUPLICATE_FIELD_DECLARATION`：重复的字段定义（检测逻辑保留）
- `NAMESPACE_VIOLATION`：上下文违规（检测逻辑保留）

#### 移除的错误处理功能
以下功能已被完全移除：
- `addError()` 方法：用于添加错误到错误列表
- `hasErrors()` 方法：用于检查是否存在错误
- `getErrors()` 方法：用于获取错误列表
- `printErrors()` 方法：用于打印所有错误
- `generateErrorReport()` 方法：用于生成错误报告
- `addTypeError()` 方法：用于添加类型错误
- `addValueError()` 方法：用于添加值错误
- `addFieldError()` 方法：用于添加字段错误
- `errors` 字段：用于存储错误的列表

错误检测逻辑仍然存在，但不再记录或报告错误。所有错误处理调用已被替换为注释。

### 7. 调试和分析工具

#### 符号表检查
```java
public void debugPrintSymbolTable() {
    // 打印层次化符号表结构
}

public String generateSymbolTableStats() {
    // 生成符号使用统计
}
```

## 关键算法

### 1. 符号解析算法

```
function resolveSymbol(identifier, context):
    switch context:
        case TYPE_CONTEXT:
            return lookupInTypeNamespace(identifier)
        case VALUE_CONTEXT:
            return lookupInValueNamespace(identifier)
        case FIELD_CONTEXT:
            return lookupInFieldNamespace(identifier, currentType)
        // ... 其他上下文
```

### 2. 关联项解析算法

```
function resolveAssociatedItem(baseType, itemName):
    // 检查是否是构造函数
    if itemName == baseType.name:
        return baseType
    
    // 处理枚举变体
    if baseType.kind == ENUM:
        return lookupEnumVariant(itemName)
    
    // 处理trait项
    if baseType.kind == TRAIT:
        return lookupTraitItem(itemName)
    
    // 处理impl项
    for implSymbol in baseType.implSymbols:
        if implSymbol.name == itemName:
            return implSymbol
    
    // 未找到
    reportError(UNDECLARED_ASSOCIATED_ITEM)
```

### 3. 作用域管理算法

```
function processNode(node):
    saveCurrentState()
    
    if node.createsNewScope:
        enterScope()
    
    try:
        processNodeChildren(node)
    finally:
        if node.createsNewScope:
            exitScope()
        restoreState()
```

## 使用示例

```java
// 创建并初始化分析器
NamespaceAnalyzer analyzer = new NamespaceAnalyzer();
analyzer.initializeGlobalScope();

// 处理AST
ASTNode root = parseSource(sourceCode);
root.accept(analyzer);

// 错误检查功能已移除
System.out.println("Semantic analysis completed");

// 调试符号表
analyzer.debugPrintSymbolTable();
```

## 集成点

### 1. 编译器流水线
- 位于解析阶段之后
- 在类型检查和代码生成阶段之前
- 为后续阶段提供符号信息

### 2. 符号表集成
- 与`NamespaceSymbolTable`配合进行符号存储
- 与`Symbol`类集成进行符号表示
- 支持`SymbolKind`枚举进行符号分类

### 3. 错误系统集成（已移除）
- 错误处理功能已从NamespaceAnalyzer中移除
- 错误检测逻辑保留但不再记录错误
- 错误报告功能已被完全移除

## 性能考虑

1. **符号查找优化**
   - 层次化命名空间组织减少查找时间
   - 上下文感知解析最小化不必要的搜索

2. **内存管理**
   - 作用域层次结构允许高效的垃圾回收
   - 符号共享减少内存占用

3. **错误处理移除的影响**
   - 移除错误处理减少了内存使用
   - 简化了代码结构，提高了执行效率

## 未来增强

1. **类型系统集成**
   - 增强的类型检查功能
   - 泛型类型参数支持

2. **高级模式匹配**
   - 更复杂的模式分析
   - 完备性检查

3. **优化支持**
   - 死代码检测
   - 未使用变量警告

4. **IDE集成**
   - 实时语义分析
   - 代码完成支持

## 重构历史

### 2025-11-27: Context枚举扩展
- 扩展了`Context.java`枚举，添加了新的上下文类型以支持更精确的语义分析
- 添加了`FUNCTION_DECLARATION`上下文，用于函数声明时的符号创建
- 添加了`TYPE_DECLARATION`上下文，用于类型声明（struct, enum, trait）时的符号创建
- 添加了`FIELD_DECLARATION`上下文，用于字段声明时的符号处理
- 添加了`CONST_DECLARATION`上下文，用于常量声明时的符号创建
- 这些新上下文使得符号创建和处理更加精确，避免了上下文混淆

### 2025-11-27: AST节点Symbol字段增强
- 为多个AST节点类添加了Symbol字段和相应的getter/setter方法
- 更新了`LetStmtNode`、`FunctionNode`、`ParameterNode`、`StructNode`、`FieldNode`、`EnumNode`、`ConstItemNode`、`TraitNode`和`ImplNode`等类
- 每个节点现在都可以记录对应的符号，建立了AST节点与符号表之间的直接关联
- 这些增强为后续的类型检查阶段提供了更好的符号信息支持

### 2025-11-27: Enum变体构造函数符号处理更新
- 修改了`visit(EnumNode)`方法中ENUM_VARIANT_CONSTRUCTOR符号的创建方式
- 符号名称现在只使用变体名称，不再使用"enum::variant"格式
- 符号声明节点现在指向变体节点(IdentifierNode)，而不是枚举节点(EnumNode)
- 这个更改确保了枚举变体构造函数符号更准确地反映其在代码中的实际定义

### 2025-11-27: FieldNode类型处理修复
- 修复了`visit(FieldNode)`方法中缺少字段类型处理的问题
- 添加了对`node.type`的检查和处理，确保字段类型在TYPE_CONTEXT中被正确解析
- 在处理完字段名称和符号创建后，现在会调用`node.type.accept(this)`来处理类型表达式
- 这个修复确保了字段类型在语义分析阶段得到适当的检查和解析

### 2025-11-27: 简化声明节点查找逻辑
- 移除了`findDeclarationNodeForIdentifier`方法，不再需要该函数
- 在LET_PATTERN_CONTEXT中，直接遍历父节点查找第一个LetStmtNode作为声明节点
- 在PARAMETER_PATTERN_CONTEXT中，同样直接遍历父节点查找第一个ParameterNode作为声明节点
- 如果找不到合适的声明节点，则回退到使用节点本身作为声明节点
- 这些更改简化了代码，使其更加专注于特定需求

### 2025-11-27: IdentifierNode符号插入增强
- 在`IdentifierNode`的`visit`方法中实现了LET_PATTERN_CONTEXT和PARAMETER_PATTERN_CONTEXT的符号插入功能
- 添加了`findDeclarationNode`方法，用于在IdentifierNode中查找声明节点
- 在LET_PATTERN_CONTEXT中，IdentifierNode现在可以直接创建并插入LOCAL_VARIABLE符号到值命名空间
- 在PARAMETER_PATTERN_CONTEXT中，IdentifierNode现在可以直接创建并插入PARAMETER符号到值命名空间
- 这些增强使得符号插入更加灵活，不仅限于通过IdPatNode进行

### 2025-11-15: 移除错误处理功能
- 从`NamespaceAnalyzer`类中完全移除了错误处理相关功能
- 移除了`private final List<SemanticError> errors`字段
- 移除了所有错误相关的方法：`addError()`, `hasErrors()`, `getErrors()`, `printErrors()`, `generateErrorReport()`, `addTypeError()`, `addValueError()`, `addFieldError()`
- 移除了构造函数中的错误列表初始化
- 将所有错误处理调用替换为注释，保留了错误检测逻辑
- 这次重构简化了代码结构，减少了内存使用，提高了执行效率

### 2025-11-15: 移除未使用的addDuplicateError函数
- 从`NamespaceAnalyzer`类中移除了未使用的`addDuplicateError`函数
- 从备份文件`semantic_check_backup/SemanticAnalyzer.java`中也移除了相同的函数
- 该函数没有被代码中的任何地方调用，因此移除它不会影响功能
- 这次清理使代码更加简洁和易于维护

### 2025-11-15: 类重命名
- 将`SemanticAnalyzer`重命名为`NamespaceAnalyzer`，以更好地反映其主要职责
- 删除了`NamespaceSemanticAnalyzer`类，该类只是继承`SemanticAnalyzer`而没有添加任何功能
- 更新了`Main.java`以直接使用新的`NamespaceAnalyzer`类

这次重构简化了类层次结构，通过消除不必要的继承层使代码更易于维护。

### 2025-11-28: 内置函数支持
- 在`NamespaceAnalyzer.java`中添加了`addBuiltinFunctions()`方法，用于添加内置函数符号
- 添加了`String`类型到内置类型列表中
- 新增的内置函数包括：`print`, `println`, `printInt`, `printlnInt`, `getString`, `getInt`, `exit`
- 这些内置项使用现有的`FUNCTION`符号类型，确保与语言规范一致
- 更新了`initializeGlobalScope()`方法，在初始化时调用所有内置项添加方法

### 2025-11-28: 内置函数AST节点支持
- 在`AST.java`中添加了`BuiltinFunctionNode`类，用于表示内置函数的AST节点
- `BuiltinFunctionNode`包含函数名称，并继承自`FunctionNode`类，确保与普通函数的一致性
- 在`VisitorBase.java`中添加了`visit(BuiltinFunctionNode)`方法，支持访问内置函数节点
- 修改了`NamespaceAnalyzer.java`中的`addBuiltinFunction()`方法，现在创建`BuiltinFunctionNode`实例而不是使用null
- 内置函数符号现在指向相应的`BuiltinFunctionNode`，而不是null值
- 这个更改确保了所有符号都有有效的AST节点引用，提高了符号表的完整性
- 通过继承自`FunctionNode`，内置函数现在可以享受与普通函数相同的处理逻辑，提高了代码的一致性和可维护性

### 2025-11-28: PathExprNode两段路径处理更新
- 修改了`visit(PathExprNode)`方法中对两段路径的处理逻辑
- 当PathExprNode有两段（LSeg和RSeg都不为null）时，不再调用`resolvePath(node)`方法
- 改为直接处理LSeg，并将当前Node的Symbol设置为LSeg的Symbol
- 不再对RSeg进行检查或解析，符合新的语义分析需求
- 这个更改简化了两段路径的处理，避免了不必要的关联项解析

### 2025-11-28: Scope管理机制修复
- 修复了SymbolChecker和SymbolAdder之间的scope管理不一致问题
- 在SymbolAdder中添加了scope栈跟踪机制，用于记录scope层次结构
- 修改了SymbolAdder的`enterScope()`和`exitScope()`方法，使其维护scope栈
- 在SymbolAdder中添加了`getScopeStack()`方法，供SymbolChecker使用
- 修改了SymbolChecker的构造函数，添加了接受scope栈的重载版本
- 更新了SymbolChecker的`enterScope()`和`exitScope()`方法，使其使用scope栈而不是创建新的scope
- 修改了NamespaceAnalyzer的`initializeGlobalScope()`方法，使其在创建SymbolChecker时传递scope栈
- 这些修复确保了两个阶段使用相同的scope层次结构，解决了SymbolChecker无法访问SymbolAdder添加符号的问题

## 结论

统一的`NamespaceAnalyzer`类为编译器中的语义分析提供了全面的基础。其模块化设计、高效算法和广泛的错误报告使其适用于开发和生产环境。将先前分离的组件合并到单个类中提高了可维护性，同时保留了所有现有功能。新增的内置函数支持使编译器能够处理更多标准库功能，提高了与Rust语言规范的兼容性。PathExprNode的两段路径处理更新进一步简化了语义分析流程，提高了处理效率。