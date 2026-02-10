# RCompiler IR Codegen Visit 函数实现详解

本文档详细描述每种 AST 节点的 visit 函数实现步骤。

---

## 1. FunctionNode

**功能**：将函数定义转换为 `IRFunction`。

**实现步骤**：

```java
@Override
public void visit(FunctionNode node) {
    // 1. 保存当前函数上下文（支持嵌套函数/闭包）
    FunctionContext savedContext = saveFunctionContext();

    // 2. 获取返回类型
    IRType returnType = node.returnType != null
        ? convertType(extractType(node.returnType))
        : IRVoidType.INSTANCE;

    // 3. 创建 IRFunction 并添加到模块
    currentFunction = new IRFunction(node.name.name, returnType);
    module.addFunction(currentFunction);

    // 4. 重置临时变量计数器（每个函数独立编号）
    IRRegister.resetCounter();

    // 5. 创建入口基本块
    IRBasicBlock entryBlock = createBlock("entry");
    setCurrentBlock(entryBlock);

    // 6. 处理 self 参数（如果是方法）
    if (node.selfPara != null) {
        IRType selfType = convertSelfType(node.selfPara);
        IRRegister selfParam = newTemp(selfType, "self");
        currentFunction.addParam(selfParam);

        // 为 self 分配栈空间并存储
        IRRegister selfAddr = newTemp(new IRPtrType(selfType), "self.addr");
        emit(new AllocaInst(selfAddr, selfType));
        emit(new StoreInst(selfParam, selfAddr));
        mapSymbol(node.selfPara.getSymbol(), selfAddr);
    }

    // 7. 处理普通参数
    if (node.parameters != null) {
        for (ParameterNode param : node.parameters) {
            IRType paramType = convertType(param.getParameterType());
            String paramName = getPatternName(param.name);

            // 创建参数寄存器
            IRRegister paramReg = newTemp(paramType, paramName);
            currentFunction.addParam(paramReg);

            // 为参数分配栈空间（便于后续可能的取地址操作）
            IRRegister paramAddr = newTemp(new IRPtrType(paramType), paramName + ".addr");
            emit(new AllocaInst(paramAddr, paramType));
            emit(new StoreInst(paramReg, paramAddr));

            // 记录参数符号到地址的映射
            mapSymbol(getSymbolFromPattern(param.name), paramAddr);
        }
    }

    // 8. 处理函数体
    if (node.body != null) {
        IRValue bodyResult = visitExpr(node.body);

        // 9. 如果函数体没有显式终结，添加返回指令
        if (!currentBlock.isTerminated()) {
            if (returnType instanceof IRVoidType) {
                emit(new ReturnInst());
            } else {
                emit(new ReturnInst(bodyResult));
            }
        }
    } else {
        // 函数声明（无函数体），生成空返回
        if (!currentBlock.isTerminated()) {
            emit(new ReturnInst());
        }
    }

    // 10. 恢复之前的函数上下文
    restoreFunctionContext(savedContext);
}
```

**生成示例**：

源代码：
```rust
fn add(a: i32, b: i32) -> i32 {
    a + b
}
```

生成的 IR：
```llvm
define i32 @add(i32 %a.0, i32 %b.1) {
entry:
    %a.addr.2 = alloca i32
    store i32 %a.0, i32* %a.addr.2
    %b.addr.3 = alloca i32
    store i32 %b.1, i32* %b.addr.3
    %t4 = load i32, i32* %a.addr.2
    %t5 = load i32, i32* %b.addr.3
    %t6 = add i32 %t4, %t5
    ret i32 %t6
}
```

**关键点**：
1. **保存/恢复上下文**：支持嵌套函数和闭包处理，避免计数器和状态被破坏
2. 参数先存入栈空间，便于统一处理（取地址、可变性等）
3. 每个函数重置临时变量计数器
4. 函数体的返回值即为函数返回值
5. 需要检查基本块是否已终结，避免重复添加 `ret`

**嵌套函数/闭包处理示例**：

```rust
fn outer() -> i32 {
    let x = 1;
    let closure = || x + 1;  // 闭包捕获 x
    closure()
}
```

处理流程：
```
1. 进入 outer() → saveFunctionContext() 保存空上下文
2. 处理 outer 的参数和局部变量，生成 %t0, %t1...
3. 遇到闭包 → saveFunctionContext() 保存 outer 的上下文
4. 处理闭包函数，resetCounter()，生成 %t0, %t1...
5. 闭包处理完成 → restoreFunctionContext() 恢复 outer 的上下文
6. 继续处理 outer()，计数器恢复，继续生成 %t2, %t3...
7. outer 处理完成 → restoreFunctionContext() 恢复空上下文
```

---

## 2. StructNode

**功能**：将结构体定义转换为 `IRStructType` 并注册到模块。

**说明**：结构体的类型收集通常在 Pass 1 阶段完成（在 `collectStruct` 方法中），visit 方法主要用于处理可能的附加逻辑。

**实现步骤**：

```java
@Override
public void visit(StructNode node) {
    // 结构体类型已在 Pass 1 (collectStruct) 中收集
    // 此处可以处理额外逻辑，如：
    // - 生成默认构造函数
    // - 生成 Drop 函数（如果需要）

    // 如果结构体尚未注册，则在此注册
    if (module.getStruct(node.name.name) == null) {
        collectStruct(node);
    }
}

/**
 * 收集结构体类型定义（在 Pass 1 调用）
 */
private void collectStruct(StructNode node) {
    // 1. 收集所有字段类型
    List<IRType> fieldTypes = new ArrayList<>();
    List<String> fieldNames = new ArrayList<>();

    if (node.fields != null) {
        for (FieldNode field : node.fields) {
            IRType fieldType = convertType(extractType(field.type));
            fieldTypes.add(fieldType);
            fieldNames.add(field.name.name);
        }
    }

    // 2. 创建 IRStructType（自动计算布局）
    IRStructType structType = new IRStructType(node.name.name, fieldTypes);

    // 3. 可选：保存字段名映射（用于后续字段访问）
    structType.setFieldNames(fieldNames);

    // 4. 注册到模块
    module.addStruct(structType);
}
```

**生成示例**：

源代码：
```rust
struct Point {
    x: i32,
    y: i32,
}
```

生成的 IR 类型定义：
```llvm
%Point = type { i32, i32 }
```

**关键点**：
1. 结构体在 IR 中表示为命名类型 `%StructName`
2. 字段按声明顺序排列，布局由 `IRStructType` 自动计算
3. 字段名到索引的映射需要保存，供后续 `FieldExprNode` 使用

---

## 3. EnumNode

**功能**：将枚举定义转换为 IR 表示。

**说明**：当前编译器的枚举是简单枚举（类似 C 枚举），每个变体只是一个名称，没有关联数据。

**实现步骤**：

```java
@Override
public void visit(EnumNode node) {
    // 简单枚举：用 i32 表示，每个变体对应一个整数值
    Map<String, Integer> variantValues = new LinkedHashMap<>();

    if (node.variants != null) {
        int value = 0;
        for (IdentifierNode variant : node.variants) {
            variantValues.put(variant.name, value);
            value++;
        }
    }

    // 注册枚举信息到模块
    // 枚举类型在 IR 中用 i32 表示
    module.addEnum(node.name.name, IRIntType.I32, variantValues);
}
```

**生成示例**：

源代码：
```rust
enum Color {
    Red,
    Green,
    Blue,
}
```

IR 表示：
```llvm
; Color 用 i32 表示
; Red = 0, Green = 1, Blue = 2

; 使用示例：
%c = alloca i32
store i32 1, i32* %c    ; c = Color::Green
```

**关键点**：
1. 枚举在 IR 中用 `i32` 表示
2. 变体按声明顺序从 0 开始编号
3. 需要在模块中记录变体名到整数值的映射，供后续路径表达式（如 `Color::Red`）使用

---

## 4. ConstItemNode

**功能**：将常量定义转换为全局常量或内联常量。

**说明**：项目中已有 `ConstantEvaluator` 类用于编译期常量表达式求值，支持字面量、算术运算、比较运算、逻辑运算、类型转换等。

**实现步骤**：

```java
// 常量求值器（复用语义分析阶段的实现）
private ConstantEvaluator constantEvaluator = new ConstantEvaluator(true);

@Override
public void visit(ConstItemNode node) {
    // 1. 获取常量类型
    IRType constType = convertType(extractType(node.type));

    // 2. 使用 ConstantEvaluator 计算常量值
    IRConstant constValue = null;
    if (node.value != null) {
        ConstantValue evaluated = constantEvaluator.evaluate(node.value);
        if (evaluated != null) {
            constValue = convertConstantValue(evaluated, constType);
        }
    }

    // 3. 决定处理方式
    if (constValue != null && isSimpleConstant(constType)) {
        // 简单类型常量：记录到常量表，使用时内联
        module.addConstant(node.name.name, constValue);
    } else if (constValue != null) {
        // 复杂类型常量：生成全局变量
        IRGlobal global = new IRGlobal(constType, node.name.name, true);  // true = isConstant
        global.setInitializer(constValue);
        module.addGlobal(global);
    }

    // 4. 记录符号映射（用于后续引用）
    if (constValue != null) {
        mapConstSymbol(getSymbol(node), constValue);
    }
}

/**
 * 将 ConstantValue 转换为 IRConstant
 */
private IRConstant convertConstantValue(ConstantValue value, IRType targetType) {
    Object val = value.getValue();

    if (val instanceof Number) {
        long longVal = ((Number) val).longValue();
        if (targetType == IRIntType.I32) {
            return IRConstant.i32(longVal);
        } else if (targetType == IRIntType.I64) {
            return IRConstant.i64(longVal);
        } else if (targetType == IRIntType.I1) {
            return IRConstant.i1(longVal != 0);
        }
    } else if (val instanceof Boolean) {
        return IRConstant.i1((Boolean) val);
    }

    // 其他类型
    return new IRConstant(targetType, val);
}

/**
 * 判断是否为简单常量类型（可内联）
 */
private boolean isSimpleConstant(IRType type) {
    return type instanceof IRIntType;
}
```

**生成示例**：

源代码：
```rust
const MAX_SIZE: i32 = 100;
const BUFFER_LEN: usize = 1024;
const FLAG: bool = true;
const COMPUTED: i32 = 10 + 20 * 3;  // 编译期计算为 70
```

IR 表示：
```llvm
; 简单常量内联使用，不生成全局变量
; MAX_SIZE 使用处直接替换为 i32 100
; BUFFER_LEN 使用处直接替换为 i64 1024
; FLAG 使用处直接替换为 i1 1
; COMPUTED 使用处直接替换为 i32 70
```

**关键点**：
1. 复用 `ConstantEvaluator` 进行编译期常量求值
2. 支持的常量表达式：字面量、算术运算（`+`, `-`, `*`, `/`, `%`）、位运算（`&`, `|`, `^`, `<<`, `>>`）、比较运算、逻辑运算、类型转换
3. 简单整数/布尔常量通常内联，不生成全局变量
4. 常量符号引用（如 `const B = A + 1;`）会递归求值

---

## 5. ImplNode

**功能**：处理 `impl` 块，为结构体生成方法。

**实现步骤**：

```java
@Override
public void visit(ImplNode node) {
    // 1. 获取实现的目标类型
    String targetTypeName = getTypeName(node.type);
    IRStructType targetType = module.getStruct(targetTypeName);

    if (targetType == null) {
        throw new RuntimeException("Unknown type in impl: " + targetTypeName);
    }

    // 2. 设置当前 impl 上下文（用于 self 类型解析）
    currentImplType = targetType;

    // 3. 处理 impl 块中的每个方法
    if (node.items != null) {
        for (ImplItemNode item : node.items) {
            if (item instanceof FunctionNode) {
                FunctionNode method = (FunctionNode) item;

                // 生成方法名：TypeName::methodName
                String mangledName = targetTypeName + "::" + method.name.name;

                // 保存原始名称，设置 mangled 名称
                String originalName = method.name.name;
                method.name.name = mangledName;

                // 处理方法
                visit(method);

                // 恢复原始名称
                method.name.name = originalName;
            }
            // TODO: 处理关联常量、关联类型等
        }
    }

    // 4. 清理 impl 上下文
    currentImplType = null;
}

/**
 * 转换 self 参数类型
 */
protected IRType convertSelfType(SelfParaNode selfPara) {
    if (currentImplType == null) {
        throw new RuntimeException("self outside of impl block");
    }

    // self: T       -> T (值传递，实际传指针)
    // &self: &T     -> T* (不可变引用)
    // &mut self     -> T* (可变引用)

    if (selfPara.isRef) {
        return new IRPtrType(currentImplType);
    } else {
        // 值传递的 self 也用指针传递（避免大对象拷贝）
        return new IRPtrType(currentImplType);
    }
}
```

**生成示例**：

源代码：
```rust
struct Point {
    x: i32,
    y: i32,
}

impl Point {
    fn new(x: i32, y: i32) -> Point {
        Point { x, y }
    }

    fn distance(&self) -> i32 {
        self.x * self.x + self.y * self.y
    }
}
```

生成的 IR：
```llvm
%Point = type { i32, i32 }

define %Point @Point::new(i32 %x.0, i32 %y.1) {
entry:
    ; ... 构造 Point 并返回
}

define i32 @Point::distance(%Point* %self.0) {
entry:
    %self.addr.1 = alloca %Point*
    store %Point* %self.0, %Point** %self.addr.1
    ; ... 计算距离
}
```

**关键点**：
1. 方法名使用 `TypeName::methodName` 格式进行名称修饰（name mangling）
2. `self` 参数统一用指针传递
3. `&self` 和 `&mut self` 在 IR 层面都是指针，区别在语义检查阶段处理
4. 需要维护 `currentImplType` 上下文用于解析 `self` 类型

---

## 6. LetStmtNode

**功能**：处理 `let` 语句，为局部变量分配栈空间并可选地初始化。

**实现步骤**：

```java
@Override
public void visit(LetStmtNode node) {
    // 1. 获取变量类型
    IRType varType = convertType(node.getVariableType());

    // 2. 获取变量名
    String varName = getPatternName(node.name);

    // 3. 分配栈空间
    IRRegister addr = newTemp(new IRPtrType(varType), varName + ".addr");
    emit(new AllocaInst(addr, varType));

    // 4. 如果有初始化表达式，生成初始化代码
    if (node.value != null) {
        IRValue initValue = visitExpr(node.value);
        emit(new StoreInst(initValue, addr));
    }

    // 5. 记录符号到地址的映射
    mapSymbol(getSymbolFromPattern(node.name), addr);
}
```

**生成示例**：

源代码：
```rust
let x: i32 = 42;
let y: i32;
let z = x + 1;
```

生成的 IR：
```llvm
; let x: i32 = 42;
%x.addr.0 = alloca i32
store i32 42, i32* %x.addr.0

; let y: i32;
%y.addr.1 = alloca i32
; 无初始化

; let z = x + 1;
%z.addr.2 = alloca i32
%t3 = load i32, i32* %x.addr.0
%t4 = add i32 %t3, 1
store i32 %t4, i32* %z.addr.2
```

**关键点**：
1. 每个局部变量都使用 `alloca` 分配栈空间
2. 变量名映射到地址（指针），使用时需要 `load`
3. 未初始化的变量只分配空间，不生成 `store`

---

## 7. ExprStmtNode

**功能**：处理表达式语句，求值表达式并丢弃结果。

**实现步骤**：

```java
@Override
public void visit(ExprStmtNode node) {
    // 1. 求值表达式
    if (node.expr != null) {
        visitExpr(node.expr);
        // 结果被丢弃（表达式语句不使用返回值）
    }

    // 2. 如果有分号，表示这是一个语句而非块的返回值
    // node.hasSemicolon 可用于区分
}
```

**生成示例**：

源代码：
```rust
foo();          // 函数调用，结果丢弃
x = 10;         // 赋值表达式
println!("hi"); // 宏调用
```

生成的 IR：
```llvm
; foo();
call void @foo()

; x = 10;
store i32 10, i32* %x.addr.0

; println!("hi"); - 展开为函数调用
call void @println(...)
```

**关键点**：
1. 表达式语句的返回值被丢弃
2. 副作用（如赋值、函数调用）仍然生效

---

## 8. LiteralExprNode

**功能**：将字面量转换为 IR 常量。

**实现步骤**：

```java
private IRValue visitLiteral(LiteralExprNode node) {
    switch (node.literalType) {
        case I32:
            return IRConstant.i32(node.value_long);
        case U32:
            return IRConstant.u32(node.value_long);
        case I64:
        case ISIZE:
            return IRConstant.i64(node.value_long);
        case U64:
        case USIZE:
            return IRConstant.u64(node.value_long);
        case BOOL:
            return IRConstant.i1(node.value_bool);
        case CHAR:
            // char 用 i32 表示（Unicode 码点）
            return IRConstant.i32((int) node.value_string.charAt(0));
        case STRING:
        case CSTRING:
            // 字符串生成全局常量
            return createStringConstant(node.value_string);
        default:
            throw new RuntimeException("Unsupported literal type: " + node.literalType);
    }
}

/**
 * 创建字符串常量（全局）
 */
private IRValue createStringConstant(String str) {
    String globalName = ".str." + stringConstantCounter++;
    IRGlobal strGlobal = new IRGlobal(
        new IRArrayType(IRIntType.I8, str.length() + 1),  // +1 for null terminator
        globalName
    );
    strGlobal.setInitializer(str);
    module.addGlobal(strGlobal);
    return strGlobal;
}
```

**生成示例**：

源代码：
```rust
42          // i32
true        // bool
'A'         // char
"hello"     // string
```

IR 表示：
```llvm
i32 42
i1 1
i32 65      ; 'A' 的 ASCII 码

@.str.0 = private constant [6 x i8] c"hello\00"
```

---

## 9. PathExprNode

**功能**：处理路径表达式（变量引用、常量引用、枚举变体等）。

**实现步骤**：

```java
private IRValue visitPath(PathExprNode node) {
    Symbol symbol = node.getSymbol();
    if (symbol == null) {
        throw new RuntimeException("Unresolved symbol: " + node);
    }

    switch (symbol.getKind()) {
        case VARIABLE:
        case PARAMETER:
            // 变量/参数：从地址加载值
            IRValue addr = getSymbolValue(symbol);
            IRType valType = ((IRPtrType) addr.getType()).getPointee();
            IRRegister result = newTemp(valType);
            emit(new LoadInst(result, addr));
            return result;

        case CONSTANT:
            // 常量：返回常量值（内联）
            return getConstValue(symbol);

        case ENUM_VARIANT:
            // 枚举变体：返回对应的整数值
            int variantValue = module.getEnumVariantValue(
                symbol.getEnumName(), symbol.getName()
            );
            return IRConstant.i32(variantValue);

        case FUNCTION:
            // 函数引用：返回函数指针
            return new IRGlobal(getFunctionType(symbol), symbol.getName());

        default:
            throw new RuntimeException("Unsupported symbol kind: " + symbol.getKind());
    }
}
```

**生成示例**：

源代码：
```rust
x               // 变量引用
MAX_SIZE        // 常量引用
Color::Red      // 枚举变体
```

生成的 IR：
```llvm
; x - 变量引用
%t0 = load i32, i32* %x.addr

; MAX_SIZE - 常量内联
i32 100

; Color::Red - 枚举变体
i32 0
```

---

## 10. ArithExprNode

**功能**：处理算术运算表达式。

**说明**：TypeChecker 已经通过 `TypeUtils.findCommonType()` 确定了结果类型并设置到节点上，但没有在 AST 中插入隐式转换节点。因此 IR 生成阶段需要在必要时生成类型转换指令。

**实现步骤**：

```java
private IRValue visitArith(ArithExprNode node) {
    // 1. 求值左右操作数
    IRValue left = visitExpr(node.left);
    IRValue right = visitExpr(node.right);

    // 2. 获取结果类型（TypeChecker 已设置）
    IRType resultType = convertType(node.getType());

    // 3. 必要时进行类型转换（左右操作数类型可能不同）
    left = emitCastIfNeeded(left, resultType);
    right = emitCastIfNeeded(right, resultType);

    // 4. 确定是否为有符号运算
    boolean isSigned = isSignedType(node.getType());

    // 5. 映射运算符
    BinaryOpInst.Op op = mapBinaryOp(node.operator, isSigned);

    // 6. 生成运算指令
    IRRegister result = newTemp(resultType);
    emit(new BinaryOpInst(result, op, left, right));

    return result;
}

/**
 * 必要时生成类型转换指令
 */
private IRValue emitCastIfNeeded(IRValue value, IRType targetType) {
    IRType sourceType = value.getType();

    // 类型相同，无需转换
    if (sourceType.equals(targetType)) {
        return value;
    }

    // 整数类型之间的转换
    if (sourceType instanceof IRIntType && targetType instanceof IRIntType) {
        int sourceBits = ((IRIntType) sourceType).getBits();
        int targetBits = ((IRIntType) targetType).getBits();

        IRRegister result = newTemp(targetType);

        if (targetBits > sourceBits) {
            // 扩展：根据源类型的符号性选择 sext 或 zext
            CastInst.Op castOp = isSignedIRType(sourceType)
                ? CastInst.Op.SEXT   // 有符号扩展
                : CastInst.Op.ZEXT;  // 无符号扩展
            emit(new CastInst(result, castOp, value, targetType));
        } else {
            // 截断
            emit(new CastInst(result, CastInst.Op.TRUNC, value, targetType));
        }

        return result;
    }

    throw new RuntimeException("Unsupported cast: " + sourceType + " -> " + targetType);
}
```

**生成示例**：

源代码：
```rust
let a: i32 = 10;
let b: i64 = 20;
let c = a as i64 + b;  // 需要将 a 转换为 i64
```

生成的 IR：
```llvm
; a 是 i32，b 是 i64
%t0 = load i32, i32* %a.addr
%t1 = sext i32 %t0 to i64       ; 有符号扩展
%t2 = load i64, i64* %b.addr
%t3 = add i64 %t1, %t2          ; 同类型运算
```

**类型转换指令**：
| 转换类型 | 指令 | 说明 |
|---------|------|------|
| 小整数 → 大整数（有符号） | `sext` | 符号扩展 |
| 小整数 → 大整数（无符号） | `zext` | 零扩展 |
| 大整数 → 小整数 | `trunc` | 截断 |

**关键点**：
1. TypeChecker 已确定结果类型，存储在 `node.getType()` 中
2. 左右操作数可能类型不同，需要转换到公共类型
3. 扩展时根据源类型的符号性选择 `sext` 或 `zext`

---

## 11. CompExprNode

**功能**：处理比较运算表达式。

**说明**：与算术运算类似，比较运算的两个操作数可能类型不同，需要先转换到公共类型再比较。

**实现步骤**：

```java
private IRValue visitComp(CompExprNode node) {
    // 1. 求值左右操作数
    IRValue left = visitExpr(node.left);
    IRValue right = visitExpr(node.right);

    // 2. 找到公共类型并转换（比较运算需要同类型操作数）
    IRType leftType = left.getType();
    IRType rightType = right.getType();

    if (!leftType.equals(rightType)) {
        // 找到公共类型
        IRType commonType = findCommonIRType(leftType, rightType);
        left = emitCastIfNeeded(left, commonType);
        right = emitCastIfNeeded(right, commonType);
    }

    // 3. 确定是否为有符号比较
    boolean isSigned = isSignedType(node.left.getType());

    // 4. 映射比较谓词
    CmpInst.Pred pred = mapCmpPred(node.operator, isSigned);

    // 5. 生成比较指令（结果总是 i1）
    IRRegister result = newTemp(IRIntType.I1);
    emit(new CmpInst(result, pred, left, right));

    return result;
}

/**
 * 找到两个 IR 类型的公共类型
 */
private IRType findCommonIRType(IRType type1, IRType type2) {
    if (type1.equals(type2)) {
        return type1;
    }

    // 整数类型：选择较大的类型
    if (type1 instanceof IRIntType && type2 instanceof IRIntType) {
        int bits1 = ((IRIntType) type1).getBits();
        int bits2 = ((IRIntType) type2).getBits();
        return bits1 >= bits2 ? type1 : type2;
    }

    throw new RuntimeException("Cannot find common type: " + type1 + " and " + type2);
}
```

**生成示例**：

源代码：
```rust
let a: i32 = 10;
let b: i64 = 20;
let c = a < b;  // 需要将 a 转换为 i64 再比较
```

生成的 IR：
```llvm
%t0 = load i32, i32* %a.addr
%t1 = sext i32 %t0 to i64       ; 有符号扩展
%t2 = load i64, i64* %b.addr
%t3 = cmp slt i64 %t1, %t2      ; 同类型比较
```

**比较谓词**：
| 运算符 | 有符号 | 无符号 |
|--------|--------|--------|
| `==` | `eq` | `eq` |
| `!=` | `ne` | `ne` |
| `<` | `slt` | `ult` |
| `<=` | `sle` | `ule` |
| `>` | `sgt` | `ugt` |
| `>=` | `sge` | `uge` |

**关键点**：
1. 比较运算的结果类型总是 `i1`（布尔）
2. 操作数需要转换到公共类型后再比较
3. 有符号和无符号比较使用不同的谓词

---

## 12. LazyExprNode

**功能**：处理短路逻辑运算（`&&` 和 `||`）。

**说明**：短路运算需要生成条件分支，因为右操作数可能不被求值。

**实现步骤**：

```java
private IRValue visitLazy(LazyExprNode node) {
    // 1. 求值左操作数
    IRValue leftVal = visitExpr(node.left);

    // 2. 保存当前块（作为 phi 的来源之一）
    IRBasicBlock leftEndBlock = currentBlock;

    // 3. 创建基本块
    IRBasicBlock rightBlock = createBlock("lazy.right");
    IRBasicBlock mergeBlock = createBlock("lazy.merge");

    // 4. 根据运算符生成条件分支
    if (node.operator == oper_t.LOGICAL_AND) {
        // &&: 左为 true 才求值右边，左为 false 直接跳到 merge
        emit(new CondBranchInst(leftVal, rightBlock, mergeBlock));
    } else {
        // ||: 左为 false 才求值右边，左为 true 直接跳到 merge
        emit(new CondBranchInst(leftVal, mergeBlock, rightBlock));
    }

    // 5. 求值右操作数
    setCurrentBlock(rightBlock);
    IRValue rightVal = visitExpr(node.right);
    IRBasicBlock rightEndBlock = currentBlock;  // 保存（可能因嵌套表达式而改变）
    emit(new BranchInst(mergeBlock));

    // 6. 合并结果（使用 phi）
    setCurrentBlock(mergeBlock);
    IRRegister result = newTemp(IRIntType.I1);

    // 创建 phi 节点：根据控制流来源选择值
    PhiInst phi = new PhiInst(result, IRIntType.I1);
    if (node.operator == oper_t.LOGICAL_AND) {
        // &&: 从左边短路来的是 false，从右边来的是右值
        phi.addIncoming(IRConstant.i1(false), leftEndBlock);
        phi.addIncoming(rightVal, rightEndBlock);
    } else {
        // ||: 从左边短路来的是 true，从右边来的是右值
        phi.addIncoming(IRConstant.i1(true), leftEndBlock);
        phi.addIncoming(rightVal, rightEndBlock);
    }
    emit(phi);

    return result;
}
```

**控制流图**：

`a && b` 的控制流：
```
┌──────────────────┐
│ leftEndBlock:    │
│   %t0 = eval(a)  │
│   br %t0 ? right │─── true ──→ ┌─────────────────┐
│          : merge │             │ rightBlock:     │
└────────┬─────────┘             │   %t1 = eval(b) │
         │ false                 │   br merge      │
         │                       └────────┬────────┘
         │                                │
         └───────────┬────────────────────┘
                     ↓
            ┌────────────────────┐
            │ mergeBlock:        │
            │   %t2 = phi i1     │
            │     [false, left]  │ ← 短路：a 为 false
            │     [%t1, right]   │ ← 正常：b 的值
            └────────────────────┘
```

**生成示例**：

源代码：
```rust
a && b
x || y
```

生成的 IR（`a && b`）：
```llvm
entry:
    %t0 = load i1, i1* %a.addr
    br i1 %t0, label %lazy.right, label %lazy.merge

lazy.right:
    %t1 = load i1, i1* %b.addr
    br label %lazy.merge

lazy.merge:
    %t2 = phi i1 [ false, %entry ], [ %t1, %lazy.right ]
```

---

## 13. AssignExprNode

**功能**：处理赋值表达式。

**实现步骤**：

```java
private IRValue visitAssign(AssignExprNode node) {
    // 1. 获取左值地址
    IRValue addr = visitLValue(node.left);

    // 2. 求值右边表达式
    IRValue value = visitExpr(node.right);

    // 3. 生成 store 指令
    emit(new StoreInst(value, addr));

    // 4. 赋值表达式返回 unit 类型
    return null;  // 或返回 IRVoidValue
}

/**
 * 获取左值的地址（不加载值）
 */
private IRValue visitLValue(ExprNode expr) {
    if (expr instanceof PathExprNode) {
        // 变量：直接返回地址
        Symbol symbol = ((PathExprNode) expr).getSymbol();
        return getSymbolValue(symbol);
    } else if (expr instanceof DerefExprNode) {
        // 解引用：求值内部表达式得到地址
        return visitExpr(((DerefExprNode) expr).innerExpr);
    } else if (expr instanceof FieldExprNode) {
        // 字段访问：计算字段地址
        return visitFieldLValue((FieldExprNode) expr);
    } else if (expr instanceof IndexExprNode) {
        // 数组索引：计算元素地址
        return visitIndexLValue((IndexExprNode) expr);
    }
    throw new RuntimeException("Invalid lvalue: " + expr.getClass());
}
```

**生成示例**：

源代码：
```rust
x = 10;
*ptr = 20;
arr[0] = 30;
point.x = 40;
```

生成的 IR：
```llvm
; x = 10
store i32 10, i32* %x.addr

; *ptr = 20
%t0 = load i32*, i32** %ptr.addr
store i32 20, i32* %t0

; arr[0] = 30
%t1 = gep [10 x i32], [10 x i32]* %arr.addr, i32 0, i32 0
store i32 30, i32* %t1

; point.x = 40
%t2 = gep %Point, %Point* %point.addr, i32 0, i32 0
store i32 40, i32* %t2
```

---

## 14. ComAssignExprNode

**功能**：处理复合赋值表达式（`+=`, `-=`, `*=` 等）。

**说明**：与算术运算类似，右操作数可能需要类型转换以匹配左操作数的类型。

**实现步骤**：

```java
private IRValue visitComAssign(ComAssignExprNode node) {
    // 1. 获取左值地址
    IRValue addr = visitLValue(node.left);

    // 2. 加载当前值
    IRType valType = ((IRPtrType) addr.getType()).getPointee();
    IRRegister oldVal = newTemp(valType);
    emit(new LoadInst(oldVal, addr));

    // 3. 求值右边表达式
    IRValue rightVal = visitExpr(node.right);

    // 4. 必要时进行类型转换（右操作数转换为左操作数类型）
    rightVal = emitCastIfNeeded(rightVal, valType);

    // 5. 执行运算
    boolean isSigned = isSignedType(node.left.getType());
    BinaryOpInst.Op op = mapBinaryOp(node.operator, isSigned);
    IRRegister newVal = newTemp(valType);
    emit(new BinaryOpInst(newVal, op, oldVal, rightVal));

    // 6. 存回
    emit(new StoreInst(newVal, addr));

    return null;
}
```

**生成示例**：

源代码：
```rust
let mut x: i64 = 100;
let y: i32 = 5;
x += y;  // y 需要转换为 i64
```

生成的 IR：
```llvm
; x += y (x: i64, y: i32)
%t0 = load i64, i64* %x.addr
%t1 = load i32, i32* %y.addr
%t2 = sext i32 %t1 to i64       ; 将 y 转换为 i64
%t3 = add i64 %t0, %t2
store i64 %t3, i64* %x.addr
```

**关键点**：
1. 复合赋值的结果类型由左操作数决定
2. 右操作数需要转换为左操作数的类型
3. 运算完成后存回原地址

---

## 15. CallExprNode

**功能**：处理函数调用表达式。

**实现步骤**：

```java
private IRValue visitCall(CallExprNode node) {
    // 1. 获取被调用函数
    IRValue callee = visitExpr(node.function);
    IRType returnType = getFunctionReturnType(node.function);

    // 2. 求值所有参数
    List<IRValue> args = new ArrayList<>();
    if (node.arguments != null) {
        for (ExprNode arg : node.arguments) {
            args.add(visitExpr(arg));
        }
    }

    // 3. 生成调用指令
    if (returnType instanceof IRVoidType) {
        // 无返回值
        emit(new CallInst(null, callee, args));
        return null;
    } else {
        // 有返回值
        IRRegister result = newTemp(returnType);
        emit(new CallInst(result, callee, args));
        return result;
    }
}
```

**生成示例**：

源代码：
```rust
foo()
add(1, 2)
println("hello")
```

生成的 IR：
```llvm
call void @foo()

%t0 = call i32 @add(i32 1, i32 2)

call void @println(i8* @.str.0)
```

---

## 16. MethodCallExprNode

**功能**：处理方法调用表达式。

**实现步骤**：

```java
private IRValue visitMethodCall(MethodCallExprNode node) {
    // 1. 获取接收者的地址（作为 self 参数）
    IRValue receiver = visitExpr(node.receiver);
    IRValue selfArg;

    // 如果接收者是值类型，需要取地址
    if (!(receiver.getType() instanceof IRPtrType)) {
        // 创建临时变量存储值，然后取地址
        IRRegister temp = newTemp(new IRPtrType(receiver.getType()));
        emit(new AllocaInst(temp, receiver.getType()));
        emit(new StoreInst(receiver, temp));
        selfArg = temp;
    } else {
        selfArg = receiver;
    }

    // 2. 构造方法名（TypeName::methodName）
    String typeName = getTypeName(node.receiver.getType());
    String methodName = typeName + "::" + node.methodName.name;
    IRGlobal methodFunc = new IRGlobal(getMethodType(node), methodName);

    // 3. 求值其他参数
    List<IRValue> args = new ArrayList<>();
    args.add(selfArg);  // self 作为第一个参数
    if (node.arguments != null) {
        for (ExprNode arg : node.arguments) {
            args.add(visitExpr(arg));
        }
    }

    // 4. 生成调用指令
    IRType returnType = getMethodReturnType(node);
    if (returnType instanceof IRVoidType) {
        emit(new CallInst(null, methodFunc, args));
        return null;
    } else {
        IRRegister result = newTemp(returnType);
        emit(new CallInst(result, methodFunc, args));
        return result;
    }
}
```

**生成示例**：

源代码：
```rust
point.distance()
vec.push(42)
```

生成的 IR：
```llvm
; point.distance()
%t0 = call i32 @Point::distance(%Point* %point.addr)

; vec.push(42)
call void @Vec::push(%Vec* %vec.addr, i32 42)
```

---

## 17. FieldExprNode

**功能**：处理字段访问表达式。

**实现步骤**：

```java
private IRValue visitField(FieldExprNode node) {
    // 1. 获取结构体地址
    IRValue baseAddr = visitExprAsAddr(node.base);

    // 2. 获取字段索引
    IRStructType structType = getStructType(node.base.getType());
    int fieldIndex = structType.getFieldIndex(node.fieldName.name);

    // 3. 生成 GEP 指令计算字段地址
    IRRegister fieldAddr = newTemp(new IRPtrType(structType.getFieldType(fieldIndex)));
    emit(new GEPInst(fieldAddr, baseAddr, Arrays.asList(
        IRConstant.i32(0),      // 解引用指针
        IRConstant.i32(fieldIndex)  // 字段索引
    )));

    // 4. 加载字段值
    IRRegister result = newTemp(structType.getFieldType(fieldIndex));
    emit(new LoadInst(result, fieldAddr));

    return result;
}

/**
 * 获取字段地址（用于左值）
 */
private IRValue visitFieldLValue(FieldExprNode node) {
    IRValue baseAddr = visitExprAsAddr(node.base);
    IRStructType structType = getStructType(node.base.getType());
    int fieldIndex = structType.getFieldIndex(node.fieldName.name);

    IRRegister fieldAddr = newTemp(new IRPtrType(structType.getFieldType(fieldIndex)));
    emit(new GEPInst(fieldAddr, baseAddr, Arrays.asList(
        IRConstant.i32(0),
        IRConstant.i32(fieldIndex)
    )));

    return fieldAddr;
}
```

**生成示例**：

源代码：
```rust
point.x
point.y
```

生成的 IR：
```llvm
; point.x
%t0 = gep %Point, %Point* %point.addr, i32 0, i32 0
%t1 = load i32, i32* %t0

; point.y
%t2 = gep %Point, %Point* %point.addr, i32 0, i32 1
%t3 = load i32, i32* %t2
```

### visitIndexLValue

**功能**：获取数组元素的地址（用于左值赋值）。

**实现步骤**：

```java
private IRValue visitIndexLValue(IndexExprNode node) {
    // 1. 获取数组地址
    IRValue baseAddr = visitExprAsAddr(node.base);

    // 2. 求值索引表达式
    IRValue index = visitExpr(node.index);

    // 3. 生成 GEP 指令计算元素地址
    IRArrayType arrayType = (IRArrayType) ((IRPtrType) baseAddr.getType()).getPointee();
    IRType elemType = arrayType.getElementType();

    IRRegister elemAddr = newTemp(new IRPtrType(elemType));
    emit(new GEPInst(elemAddr, baseAddr, Arrays.asList(
        IRConstant.i32(0),  // 解引用指针
        index               // 数组索引
    )));

    // 4. 返回元素地址（不加载值）
    return elemAddr;
}
```

**生成示例**：

源代码：
```rust
arr[0] = 10;
arr[i] = x;
```

生成的 IR：
```llvm
; arr[0] = 10
%t0 = gep [10 x i32], [10 x i32]* %arr.addr, i32 0, i32 0
store i32 10, i32* %t0

; arr[i] = x
%t1 = load i32, i32* %i.addr
%t2 = gep [10 x i32], [10 x i32]* %arr.addr, i32 0, %t1
%t3 = load i32, i32* %x.addr
store i32 %t3, i32* %t2
```

**与 visitIndexExpr 的区别**：
- `visitIndexExpr`：计算元素地址后**加载值**，用于右值（读取数组元素）
- `visitIndexLValue`：只计算元素地址**不加载**，用于左值（赋值目标）

---

## 18. IndexExprNode

**功能**：处理数组索引表达式。

**实现步骤**：

```java
private IRValue visitIndex(IndexExprNode node) {
    // 1. 获取数组地址
    IRValue baseAddr = visitExprAsAddr(node.base);

    // 2. 求值索引表达式
    IRValue index = visitExpr(node.index);

    // 3. 生成 GEP 指令计算元素地址
    IRArrayType arrayType = (IRArrayType) ((IRPtrType) baseAddr.getType()).getPointee();
    IRType elemType = arrayType.getElementType();

    IRRegister elemAddr = newTemp(new IRPtrType(elemType));
    emit(new GEPInst(elemAddr, baseAddr, Arrays.asList(
        IRConstant.i32(0),  // 解引用指针
        index               // 数组索引
    )));

    // 4. 加载元素值
    IRRegister result = newTemp(elemType);
    emit(new LoadInst(result, elemAddr));

    return result;
}
```

**生成示例**：

源代码：
```rust
arr[0]
arr[i]
matrix[x][y]
```

生成的 IR：
```llvm
; arr[0]
%t0 = gep [10 x i32], [10 x i32]* %arr.addr, i32 0, i32 0
%t1 = load i32, i32* %t0

; arr[i]
%t2 = load i32, i32* %i.addr
%t3 = gep [10 x i32], [10 x i32]* %arr.addr, i32 0, i32 %t2
%t4 = load i32, i32* %t3
```

---

## 19. ArrayExprNode

**功能**：处理数组字面量表达式。

**说明**：数组有两种初始化形式：
- 元素列表：`[1, 2, 3]`
- 重复元素：`[0; 10]`（10个0）

**实现步骤**：

```java
private IRValue visitArrayExpr(ArrayExprNode node) {
    IRArrayType arrayType = convertToIRArrayType(node.getType());
    IRType elemType = arrayType.getElementType();
    int size = arrayType.getSize();

    // 1. 分配数组空间
    IRRegister arrayAddr = newTemp(new IRPtrType(arrayType));
    emit(new AllocaInst(arrayAddr, arrayType));

    if (node.elements != null) {
        // 元素列表形式：[e1, e2, e3, ...]
        for (int i = 0; i < node.elements.size(); i++) {
            IRValue elemVal = visitExpr(node.elements.get(i));

            IRRegister elemAddr = newTemp(new IRPtrType(elemType));
            emit(new GEPInst(elemAddr, arrayAddr, Arrays.asList(
                IRConstant.i32(0),
                IRConstant.i32(i)
            )));

            emit(new StoreInst(elemVal, elemAddr));
        }
    } else if (node.repeatedElement != null && node.size != null) {
        // 重复元素形式：[elem; size]
        IRValue elemVal = visitExpr(node.repeatedElement);
        int arraySize = constantEvaluator.evaluate(node.size).intValue();

        for (int i = 0; i < arraySize; i++) {
            IRRegister elemAddr = newTemp(new IRPtrType(elemType));
            emit(new GEPInst(elemAddr, arrayAddr, Arrays.asList(
                IRConstant.i32(0),
                IRConstant.i32(i)
            )));

            emit(new StoreInst(elemVal, elemAddr));
        }
    }
    // 空数组：不需要初始化

    return arrayAddr;
}
```

**生成示例**：

源代码：
```rust
let arr1 = [1, 2, 3];
let arr2 = [0; 5];
```

生成的 IR：
```llvm
; let arr1 = [1, 2, 3]
%arr1.addr = alloca [3 x i32]
%t0 = gep [3 x i32], [3 x i32]* %arr1.addr, i32 0, i32 0
store i32 1, i32* %t0
%t1 = gep [3 x i32], [3 x i32]* %arr1.addr, i32 0, i32 1
store i32 2, i32* %t1
%t2 = gep [3 x i32], [3 x i32]* %arr1.addr, i32 0, i32 2
store i32 3, i32* %t2

; let arr2 = [0; 5]
%arr2.addr = alloca [5 x i32]
%t3 = gep [5 x i32], [5 x i32]* %arr2.addr, i32 0, i32 0
store i32 0, i32* %t3
%t4 = gep [5 x i32], [5 x i32]* %arr2.addr, i32 0, i32 1
store i32 0, i32* %t4
; ... 重复5次
```

---

## 20. StructExprNode

**功能**：处理结构体字面量表达式。

**实现步骤**：

```java
private IRValue visitStructExpr(StructExprNode node) {
    IRStructType structType = getStructType(node.getType());

    // 1. 分配结构体空间
    IRRegister structAddr = newTemp(new IRPtrType(structType));
    emit(new AllocaInst(structAddr, structType));

    // 2. 初始化各字段
    if (node.fieldValues != null) {
        for (FieldValNode fieldVal : node.fieldValues) {
            String fieldName = fieldVal.fieldName.name;
            int fieldIndex = structType.getFieldIndex(fieldName);
            IRType fieldType = structType.getFieldType(fieldIndex);

            // 求值字段表达式
            IRValue val = visitExpr(fieldVal.value);

            // 计算字段地址
            IRRegister fieldAddr = newTemp(new IRPtrType(fieldType));
            emit(new GEPInst(fieldAddr, structAddr, Arrays.asList(
                IRConstant.i32(0),
                IRConstant.i32(fieldIndex)
            )));

            // 存储字段值
            emit(new StoreInst(val, fieldAddr));
        }
    }

    return structAddr;
}
```

**生成示例**：

源代码：
```rust
struct Point { x: i32, y: i32 }
let p = Point { x: 10, y: 20 };
```

生成的 IR：
```llvm
; let p = Point { x: 10, y: 20 }
%p.addr = alloca %Point
%t0 = gep %Point, %Point* %p.addr, i32 0, i32 0  ; x 字段
store i32 10, i32* %t0
%t1 = gep %Point, %Point* %p.addr, i32 0, i32 1  ; y 字段
store i32 20, i32* %t1
```

---

## 21. BorrowExprNode

**功能**：处理借用表达式（`&` 和 `&mut`）。

**说明**：借用表达式返回内部表达式的地址。

**实现步骤**：

```java
private IRValue visitBorrowExpr(BorrowExprNode node) {
    // 借用就是获取地址，直接返回左值地址
    return visitLValue(node.innerExpr);
}
```

**生成示例**：

源代码：
```rust
let x = 10;
let ptr = &x;
let mut_ptr = &mut x;
```

生成的 IR：
```llvm
; let x = 10
%x.addr = alloca i32
store i32 10, i32* %x.addr

; let ptr = &x
%ptr.addr = alloca i32*
store i32* %x.addr, i32** %ptr.addr

; let mut_ptr = &mut x
%mut_ptr.addr = alloca i32*
store i32* %x.addr, i32** %mut_ptr.addr
```

**注意**：`&` 和 `&mut` 在 IR 层面生成相同的代码，可变性检查在语义分析阶段完成。

---

## 22. DerefExprNode

**功能**：处理解引用表达式（`*ptr`）。

**实现步骤**：

```java
private IRValue visitDerefExpr(DerefExprNode node) {
    // 1. 求值内部表达式得到指针
    IRValue ptr = visitExpr(node.innerExpr);

    // 2. 加载指针指向的值
    IRType pointeeType = ((IRPtrType) ptr.getType()).getPointee();
    IRRegister result = newTemp(pointeeType);
    emit(new LoadInst(result, ptr));

    return result;
}
```

**生成示例**：

源代码：
```rust
let x = 10;
let ptr = &x;
let val = *ptr;
```

生成的 IR：
```llvm
; let val = *ptr
%t0 = load i32*, i32** %ptr.addr   ; 加载指针
%t1 = load i32, i32* %t0           ; 解引用，加载值
%val.addr = alloca i32
store i32 %t1, i32* %val.addr
```

---

## 23. NegaExprNode

**功能**：处理取反表达式（`-x` 和 `!x`）。

**实现步骤**：

```java
private IRValue visitNegaExpr(NegaExprNode node) {
    IRValue val = visitExpr(node.innerExpr);
    IRType type = val.getType();
    IRRegister result = newTemp(type);

    if (node.isLogical) {
        // 逻辑取反：!x
        // 对于布尔值，使用 xor 1
        emit(new BinaryOpInst(result, BinaryOpInst.Op.XOR, val, IRConstant.i1(true)));
    } else {
        // 算术取反：-x
        // 使用 0 - x
        IRValue zero = IRConstant.zero(type);
        emit(new BinaryOpInst(result, BinaryOpInst.Op.SUB, zero, val));
    }

    return result;
}
```

**生成示例**：

源代码：
```rust
let a = -x;
let b = !flag;
```

生成的 IR：
```llvm
; let a = -x
%t0 = load i32, i32* %x.addr
%t1 = sub i32 0, %t0

; let b = !flag
%t2 = load i1, i1* %flag.addr
%t3 = xor i1 %t2, true
```

---

## 24. TypeCastExprNode

**功能**：处理类型转换表达式（`x as i64`）。

**实现步骤**：

```java
private IRValue visitTypeCastExpr(TypeCastExprNode node) {
    IRValue val = visitExpr(node.expr);
    IRType srcType = val.getType();
    IRType dstType = convertToIRType(node.type);

    // 如果类型相同，直接返回
    if (srcType.equals(dstType)) {
        return val;
    }

    IRRegister result = newTemp(dstType);

    // 整数类型之间的转换
    if (srcType instanceof IRIntType && dstType instanceof IRIntType) {
        int srcBits = ((IRIntType) srcType).getBitWidth();
        int dstBits = ((IRIntType) dstType).getBitWidth();

        if (srcBits < dstBits) {
            // 扩展
            boolean isSigned = isSignedType(node.expr.getType());
            if (isSigned) {
                emit(new CastInst(result, CastInst.Op.SEXT, val, dstType));
            } else {
                emit(new CastInst(result, CastInst.Op.ZEXT, val, dstType));
            }
        } else {
            // 截断
            emit(new CastInst(result, CastInst.Op.TRUNC, val, dstType));
        }
    }
    // 指针与整数之间的转换
    else if (srcType instanceof IRPtrType && dstType instanceof IRIntType) {
        emit(new CastInst(result, CastInst.Op.PTRTOINT, val, dstType));
    } else if (srcType instanceof IRIntType && dstType instanceof IRPtrType) {
        emit(new CastInst(result, CastInst.Op.INTTOPTR, val, dstType));
    }
    // 指针之间的转换
    else if (srcType instanceof IRPtrType && dstType instanceof IRPtrType) {
        emit(new CastInst(result, CastInst.Op.BITCAST, val, dstType));
    }

    return result;
}
```

**生成示例**：

源代码：
```rust
let a: i32 = 100;
let b = a as i64;
let c = b as i8;
```

生成的 IR：
```llvm
; let b = a as i64
%t0 = load i32, i32* %a.addr
%t1 = sext i32 %t0 to i64

; let c = b as i8
%t2 = load i64, i64* %b.addr
%t3 = trunc i64 %t2 to i8
```

---

## 25. BlockExprNode

**功能**：处理块表达式。

**说明**：块表达式包含一系列语句，可能有返回值。

**实现步骤**：

```java
private IRValue visitBlockExpr(BlockExprNode node) {
    // 1. 处理块内的所有语句
    if (node.statements != null) {
        for (StmtNode stmt : node.statements) {
            visitStmt(stmt);
        }
    }

    // 2. 处理返回值表达式
    if (node.returnValue != null) {
        return visitExpr(node.returnValue);
    }

    // 无返回值，返回 void
    return IRConstant.voidValue();
}
```

**生成示例**：

源代码：
```rust
let result = {
    let a = 1;
    let b = 2;
    a + b
};
```

生成的 IR：
```llvm
; 块表达式
%a.addr = alloca i32
store i32 1, i32* %a.addr
%b.addr = alloca i32
store i32 2, i32* %b.addr
%t0 = load i32, i32* %a.addr
%t1 = load i32, i32* %b.addr
%t2 = add i32 %t0, %t1
; %t2 是块的返回值
%result.addr = alloca i32
store i32 %t2, i32* %result.addr
```

---

## 26. IfExprNode

**功能**：处理 if 表达式。

**说明**：if 表达式可能有返回值，需要使用 phi 节点合并分支结果。

**实现步骤**：

```java
private IRValue visitIfExpr(IfExprNode node) {
    IRType resultType = convertToIRType(node.getType());
    boolean hasValue = !(resultType instanceof IRVoidType);

    // 1. 求值条件
    IRValue cond = visitExpr(node.condition);

    // 2. 创建基本块
    IRBasicBlock thenBlock = new IRBasicBlock("if.then");
    IRBasicBlock elseBlock = new IRBasicBlock("if.else");
    IRBasicBlock mergeBlock = new IRBasicBlock("if.merge");

    // 3. 条件跳转
    emit(new BranchInst(cond, thenBlock,
        (node.elseBranch != null || node.elseifBranch != null) ? elseBlock : mergeBlock));

    // 4. 处理 then 分支
    setCurrentBlock(thenBlock);
    IRValue thenVal = visitBlockExpr(node.thenBranch);
    IRBasicBlock thenEndBlock = currentBlock;
    emit(new JumpInst(mergeBlock));

    // 5. 处理 else/elseif 分支
    IRValue elseVal = null;
    IRBasicBlock elseEndBlock = null;

    if (node.elseifBranch != null) {
        setCurrentBlock(elseBlock);
        elseVal = visitIfExpr(node.elseifBranch);
        elseEndBlock = currentBlock;
        emit(new JumpInst(mergeBlock));
    } else if (node.elseBranch != null) {
        setCurrentBlock(elseBlock);
        elseVal = visitBlockExpr((BlockExprNode) node.elseBranch);
        elseEndBlock = currentBlock;
        emit(new JumpInst(mergeBlock));
    }

    // 6. 合并块
    setCurrentBlock(mergeBlock);

    // 7. 如果有返回值，生成 phi 节点
    if (hasValue && thenVal != null) {
        IRRegister result = newTemp(resultType);
        PhiInst phi = new PhiInst(result, resultType);
        phi.addIncoming(thenVal, thenEndBlock);
        if (elseVal != null) {
            phi.addIncoming(elseVal, elseEndBlock);
        }
        emit(phi);
        return result;
    }

    return IRConstant.voidValue();
}
```

**生成示例**：

源代码：
```rust
let max = if a > b { a } else { b };
```

生成的 IR：
```llvm
; 条件判断
%t0 = load i32, i32* %a.addr
%t1 = load i32, i32* %b.addr
%t2 = icmp sgt i32 %t0, %t1
br i1 %t2, label %if.then, label %if.else

if.then:
    %t3 = load i32, i32* %a.addr
    br label %if.merge

if.else:
    %t4 = load i32, i32* %b.addr
    br label %if.merge

if.merge:
    %t5 = phi i32 [%t3, %if.then], [%t4, %if.else]
    %max.addr = alloca i32
    store i32 %t5, i32* %max.addr
```

---

## 27. LoopExprNode

**功能**：处理循环表达式（`loop` 和 `while`）。

**说明**：需要维护循环上下文以支持 `break` 和 `continue`。

**实现步骤**：

```java
private IRValue visitLoopExpr(LoopExprNode node) {
    IRType resultType = convertToIRType(node.getType());
    boolean hasValue = !(resultType instanceof IRVoidType);

    // 1. 创建基本块
    IRBasicBlock condBlock = new IRBasicBlock("loop.cond");
    IRBasicBlock bodyBlock = new IRBasicBlock("loop.body");
    IRBasicBlock exitBlock = new IRBasicBlock("loop.exit");

    // 2. 保存循环上下文（用于 break/continue）
    LoopContext loopCtx = new LoopContext(condBlock, exitBlock, hasValue ? resultType : null);
    pushLoopContext(node, loopCtx);

    // 3. 跳转到条件块
    emit(new JumpInst(condBlock));

    // 4. 条件块
    setCurrentBlock(condBlock);
    if (node.isInfinite) {
        // 无限循环：直接跳转到循环体
        emit(new JumpInst(bodyBlock));
    } else {
        // 条件循环：求值条件
        IRValue cond = visitExpr(node.condition);
        emit(new BranchInst(cond, bodyBlock, exitBlock));
    }

    // 5. 循环体
    setCurrentBlock(bodyBlock);
    visitBlockExpr(node.body);
    emit(new JumpInst(condBlock));

    // 6. 退出块
    setCurrentBlock(exitBlock);
    popLoopContext();

    // 7. 如果有返回值（来自 break），生成 phi 节点
    if (hasValue && !loopCtx.breakValues.isEmpty()) {
        IRRegister result = newTemp(resultType);
        PhiInst phi = new PhiInst(result, resultType);
        for (var entry : loopCtx.breakValues) {
            phi.addIncoming(entry.value, entry.block);
        }
        emit(phi);
        return result;
    }

    return IRConstant.voidValue();
}
```

**辅助类**：

```java
private static class LoopContext {
    IRBasicBlock continueTarget;  // continue 跳转目标
    IRBasicBlock breakTarget;     // break 跳转目标
    IRType resultType;            // 循环返回值类型（可为 null）
    List<BreakValue> breakValues; // break 带出的值

    static class BreakValue {
        IRValue value;
        IRBasicBlock block;
    }
}

private Deque<LoopContext> loopStack = new ArrayDeque<>();
private Map<ASTNode, LoopContext> loopContextMap = new HashMap<>();

private void pushLoopContext(ASTNode node, LoopContext ctx) {
    loopStack.push(ctx);
    loopContextMap.put(node, ctx);
}

private void popLoopContext() {
    loopStack.pop();
}

private LoopContext getLoopContext(ASTNode targetNode) {
    return loopContextMap.get(targetNode);
}
```

**生成示例**：

源代码：
```rust
// 无限循环
loop {
    if done { break; }
}

// while 循环
while i < 10 {
    i = i + 1;
}
```

生成的 IR：
```llvm
; loop { if done { break; } }
    br label %loop.cond
loop.cond:
    br label %loop.body
loop.body:
    %t0 = load i1, i1* %done.addr
    br i1 %t0, label %loop.exit, label %loop.cont
loop.cont:
    br label %loop.cond
loop.exit:
    ; 循环结束

; while i < 10 { i = i + 1; }
    br label %loop.cond
loop.cond:
    %t1 = load i32, i32* %i.addr
    %t2 = icmp slt i32 %t1, 10
    br i1 %t2, label %loop.body, label %loop.exit
loop.body:
    %t3 = load i32, i32* %i.addr
    %t4 = add i32 %t3, 1
    store i32 %t4, i32* %i.addr
    br label %loop.cond
loop.exit:
    ; 循环结束
```

---

## 28. BreakExprNode

**功能**：处理 break 表达式。

**说明**：break 可以带值跳出循环。

**实现步骤**：

```java
private IRValue visitBreakExpr(BreakExprNode node) {
    // 1. 获取目标循环的上下文
    LoopContext loopCtx = getLoopContext(node.getTargetNode());

    // 2. 如果有返回值，记录 break 带出的值
    if (node.value != null && loopCtx.resultType != null) {
        IRValue val = visitExpr(node.value);
        loopCtx.breakValues.add(new LoopContext.BreakValue(val, currentBlock));
    }

    // 3. 跳转到循环出口
    emit(new JumpInst(loopCtx.breakTarget));

    // 4. 创建新的不可达块（break 后的代码不会执行）
    IRBasicBlock unreachable = new IRBasicBlock("unreachable");
    setCurrentBlock(unreachable);

    return IRConstant.voidValue();
}
```

**生成示例**：

源代码：
```rust
let result = loop {
    if found {
        break 42;
    }
};
```

生成的 IR：
```llvm
loop.body:
    %t0 = load i1, i1* %found.addr
    br i1 %t0, label %break.block, label %loop.cont

break.block:
    br label %loop.exit    ; break 42

loop.exit:
    %t1 = phi i32 [42, %break.block]
    %result.addr = alloca i32
    store i32 %t1, i32* %result.addr
```

---

## 29. ContinueExprNode

**功能**：处理 continue 表达式。

**实现步骤**：

```java
private IRValue visitContinueExpr(ContinueExprNode node) {
    // 1. 获取目标循环的上下文
    LoopContext loopCtx = getLoopContext(node.getTargetNode());

    // 2. 跳转到循环条件块
    emit(new JumpInst(loopCtx.continueTarget));

    // 3. 创建新的不可达块
    IRBasicBlock unreachable = new IRBasicBlock("unreachable");
    setCurrentBlock(unreachable);

    return IRConstant.voidValue();
}
```

**生成示例**：

源代码：
```rust
while i < 10 {
    if skip {
        continue;
    }
    process(i);
}
```

生成的 IR：
```llvm
loop.body:
    %t0 = load i1, i1* %skip.addr
    br i1 %t0, label %continue.block, label %process.block

continue.block:
    br label %loop.cond    ; continue

process.block:
    %t1 = load i32, i32* %i.addr
    call void @process(i32 %t1)
    br label %loop.cond
```

---

## 30. ReturnExprNode

**功能**：处理 return 表达式。

**实现步骤**：

```java
private IRValue visitReturnExpr(ReturnExprNode node) {
    if (node.value != null) {
        // 有返回值
        IRValue val = visitExpr(node.value);
        emit(new RetInst(val));
    } else {
        // 无返回值
        emit(new RetInst());
    }

    // 创建新的不可达块
    IRBasicBlock unreachable = new IRBasicBlock("unreachable");
    setCurrentBlock(unreachable);

    return IRConstant.voidValue();
}
```

**生成示例**：

源代码：
```rust
fn add(a: i32, b: i32) -> i32 {
    return a + b;
}

fn print_hello() {
    println("Hello");
    return;
}
```

生成的 IR：
```llvm
; return a + b
%t0 = load i32, i32* %a.addr
%t1 = load i32, i32* %b.addr
%t2 = add i32 %t0, %t1
ret i32 %t2

; return (void)
call void @println(i8* @str.hello)
ret void
```

---

## 31. GroupExprNode（待实现）

**功能**：处理括号表达式 `(expr)`。

**说明**：括号表达式只是改变优先级，不产生额外的 IR 代码，直接求值内部表达式即可。

**实现步骤**：

```java
private IRValue visitGroup(GroupExprNode node) {
    // 括号表达式直接求值内部表达式
    return visitExpr(node.innerExpr);
}
```

**生成示例**：

源代码：
```rust
let x = (1 + 2) * 3;
```

生成的 IR：
```llvm
; (1 + 2) * 3
%t0 = add i32 1, 2
%t1 = mul i32 %t0, 3
```

**关键点**：
1. 括号表达式不产生额外指令
2. 直接递归处理内部表达式

---

## 32. UnderscoreExprNode（待实现）

**功能**：处理下划线表达式 `_`。

**说明**：下划线表达式通常用于模式匹配中的占位符，在表达式上下文中较少使用。如果出现在赋值左侧，表示丢弃值。

**实现步骤**：

```java
private IRValue visitUnderscore(UnderscoreExprNode node) {
    // 下划线表达式作为右值时，通常是错误
    // 作为左值时（如 let _ = expr;），表示丢弃值
    // 在 IR 生成阶段，可以返回一个特殊值或抛出异常
    throw new RuntimeException("Underscore expression not supported as rvalue");
}
```

**生成示例**：

源代码：
```rust
let _ = compute();  // 丢弃返回值
```

生成的 IR：
```llvm
; let _ = compute()
call i32 @compute()   ; 调用函数，结果被丢弃
```

**关键点**：
1. `let _ = expr;` 只需要求值表达式，不需要存储结果
2. 在 `LetStmtNode` 处理时检测下划线模式并特殊处理

---

## 33. TraitNode（待实现/可选）

**功能**：处理 trait 定义。

**说明**：当前编译器可能不完全支持 trait，或者 trait 只用于类型检查而不生成 IR 代码。

**实现步骤**：

```java
@Override
public void visit(TraitNode node) {
    // Trait 定义通常不直接生成 IR 代码
    // Trait 方法的默认实现可能需要生成代码

    if (node.items != null) {
        for (AssoItemNode item : node.items) {
            if (item.function != null && item.function.body != null) {
                // 有默认实现的方法，生成代码
                // 方法名格式：TraitName::methodName
                String mangledName = node.name.name + "::" + item.function.name.name;
                // ... 生成函数代码
            }
        }
    }
}
```

**关键点**：
1. Trait 本身不生成 IR 类型定义
2. 有默认实现的方法需要生成代码
3. Trait 方法的调用通过 vtable 或静态分发

---

## 34. BuiltinFunctionNode（内部使用）

**功能**：表示内置函数的占位节点。

**说明**：`BuiltinFunctionNode` 是在符号表初始化时创建的，用于表示内置函数（如 `print`、`getInt` 等）。这些函数的实现在 `builtin.ll` 中，IR 生成器只需要生成调用指令。

**实现步骤**：

```java
// 不需要单独的 visit 方法
// 内置函数通过 CallExprNode 调用时，直接生成 call 指令
// 函数名从符号表获取
```

**生成示例**：

源代码：
```rust
printlnInt(42);
let n = getInt();
```

生成的 IR：
```llvm
call void @printlnInt(i32 42)
%t0 = call i32 @getInt()
```

---

## 实现状态总结

| 节点类型 | 状态 | 说明 |
|---------|------|------|
| FunctionNode | ✅ 已实现 | |
| StructNode | ✅ 已实现 | |
| EnumNode | ✅ 已实现 | |
| ConstItemNode | ✅ 已实现 | |
| ImplNode | ✅ 已实现 | |
| LetStmtNode | ✅ 已实现 | |
| ExprStmtNode | ✅ 已实现 | |
| LiteralExprNode | ✅ 已实现 | visitLiteral |
| PathExprNode | ✅ 已实现 | visitPath |
| ArithExprNode | ✅ 已实现 | visitArith |
| CompExprNode | ✅ 已实现 | visitComp |
| LazyExprNode | ✅ 已实现 | visitLazy |
| AssignExprNode | ✅ 已实现 | visitAssign |
| ComAssignExprNode | ✅ 已实现 | visitComAssign |
| CallExprNode | ✅ 已实现 | visitCall |
| MethodCallExprNode | ✅ 已实现 | visitMethodCall |
| FieldExprNode | ✅ 已实现 | visitField |
| IndexExprNode | ✅ 已实现 | visitIndex |
| ArrayExprNode | ✅ 已实现 | visitArrayExpr |
| StructExprNode | ✅ 已实现 | visitStructExpr |
| BorrowExprNode | ✅ 已实现 | visitBorrow |
| DerefExprNode | ✅ 已实现 | visitDeref |
| NegaExprNode | ✅ 已实现 | visitNega |
| TypeCastExprNode | ✅ 已实现 | visitTypeCast |
| BlockExprNode | ✅ 已实现 | visitBlock |
| IfExprNode | ✅ 已实现 | visitIf |
| LoopExprNode | ✅ 已实现 | visitLoop |
| BreakExprNode | ✅ 已实现 | visitBreak |
| ContinueExprNode | ✅ 已实现 | visitContinue |
| ReturnExprNode | ✅ 已实现 | visitReturn |
| **GroupExprNode** | ❌ 待实现 | 括号表达式 |
| **UnderscoreExprNode** | ❌ 待实现 | 下划线表达式 |
| TraitNode | ⚠️ 可选 | 可能不需要生成 IR |
| BuiltinFunctionNode | ✅ 内部使用 | 通过 CallExprNode 处理 |

