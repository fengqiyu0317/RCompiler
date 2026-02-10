# IRGenerator 结构文档

本文档描述 `IRGenerator.java` 的代码结构和辅助方法分类。

> **更新说明**：本文档已根据最新的代码重组进行更新。标有 ✅ 的部分表示已完成调整。

## 文件结构概览

```
1. package 和 import
2. 类声明和文档注释
3. 内部类                    (行 21)
4. 状态变量                  (行 66)
5. 入口方法                  (行 90)
6. 类型收集（Pass 1）        (行 128)
7. Visitor 方法：顶层项      (行 163)
8. Visitor 方法：语句        (行 332)
9. 表达式求值入口            (行 364)
10. 表达式处理方法           (行 422)
11. 辅助方法：IR 构建        (行 1177)
12. 辅助方法：上下文管理     (行 1219)
13. 辅助方法：左值处理       (行 1291)
14. 辅助方法：类型转换       (行 1372)
15. 辅助方法：符号管理       (行 1573)
16. 辅助方法：运算符映射     (行 1628)
17. 辅助方法：常量处理       (行 1687)
```

---

## 1. 内部类

### LoopContext
循环上下文，用于 break/continue 处理。

```java
private static class LoopContext {
    final IRBasicBlock headerBlock;  // 循环头（continue 目标）
    final IRBasicBlock exitBlock;    // 循环出口（break 目标）
    final IRType resultType;         // 循环结果类型
    final List<IRValue> breakValues; // break 带出的值
    final List<IRBasicBlock> breakBlocks; // break 所在的基本块

    void addBreakValue(IRValue value, IRBasicBlock block);
}
```

### FunctionContext
函数上下文，用于嵌套函数/闭包处理。

```java
private static class FunctionContext {
    final IRFunction function;
    final IRBasicBlock block;
    final int registerCounter;
    final Map<Symbol, IRValue> capturedSymbols;
}
```

---

## 2. 状态变量

| 变量 | 类型 | 作用 |
|------|------|------|
| `module` | `IRModule` | 当前生成的 IR 模块 |
| `currentFunction` | `IRFunction` | 当前正在生成的函数 |
| `currentBlock` | `IRBasicBlock` | 当前基本块 |
| `symbolMap` | `Map<Symbol, IRValue>` | 符号到 IR 值的映射 |
| `constSymbolMap` | `Map<Symbol, IRConstant>` | 常量符号映射 |
| `loopStack` | `Deque<LoopContext>` | 循环上下文栈 |
| `currentImplType` | `IRStructType` | 当前 impl 块的目标类型 |
| `constantEvaluator` | `ConstantEvaluator` | 常量求值器 |
| `stringConstantCounter` | `int` | 字符串常量计数器 |

---

## 3. 辅助方法分类

### 3.1 IR 构建基础方法

| 方法 | 签名 | 作用 |
|------|------|------|
| `newTemp` | `IRRegister newTemp(IRType type)` | 创建临时寄存器 |
| `newTemp` | `IRRegister newTemp(IRType type, String hint)` | 创建带名称提示的临时寄存器 |
| `emit` | `void emit(IRInstruction inst)` | 发射指令到当前基本块 |
| `createBlock` | `IRBasicBlock createBlock(String name)` | 创建新基本块并添加到当前函数 |
| `setCurrentBlock` | `void setCurrentBlock(IRBasicBlock block)` | 设置当前基本块 |

### 3.2 上下文管理 ✅ [已合并]

> **变更**：原"函数上下文管理"和"循环上下文管理"已合并为"上下文管理"

| 方法 | 签名 | 作用 |
|------|------|------|
| `saveFunctionContext` | `FunctionContext saveFunctionContext()` | 保存当前函数上下文 |
| `restoreFunctionContext` | `void restoreFunctionContext(FunctionContext ctx)` | 恢复函数上下文 |
| `pushLoopContext` | `void pushLoopContext(IRBasicBlock header, IRBasicBlock exit, IRType resultType)` | 压入循环上下文 |
| `popLoopContext` | `LoopContext popLoopContext()` | 弹出循环上下文 |
| `getCurrentLoopHeader` | `IRBasicBlock getCurrentLoopHeader()` | 获取循环头基本块（continue 目标） |
| `getCurrentLoopExit` | `IRBasicBlock getCurrentLoopExit()` | 获取循环出口基本块（break 目标） |
| `getCurrentLoopContext` | `LoopContext getCurrentLoopContext()` | 获取当前循环上下文 |

### 3.3 左值处理 ✅ [已移动]

> **变更**：从文件末尾移动到上下文管理之后

| 方法 | 签名 | 作用 |
|------|------|------|
| `visitLValue` | `IRValue visitLValue(ExprNode expr)` | 获取左值的地址（不加载值） |
| `visitExprAsAddr` | `IRValue visitExprAsAddr(ExprNode expr)` | 获取表达式的地址 |
| `visitFieldLValue` | `IRValue visitFieldLValue(FieldExprNode node)` | 获取结构体字段的地址 |
| `visitIndexLValue` | `IRValue visitIndexLValue(IndexExprNode node)` | 获取数组元素的地址 |

### 3.4 类型转换 ✅ [已合并]

> **变更**：原分散在多处的类型相关方法已全部合并到此区域

| 方法 | 签名 | 作用 |
|------|------|------|
| `convertType` | `IRType convertType(Type astType)` | AST 类型转 IR 类型 |
| `extractType` | `Type extractType(TypeExprNode typeExpr)` | 从类型表达式提取类型（TODO） |
| `getTypeNameFromExpr` | `String getTypeNameFromExpr(ExprNode expr)` | 从表达式获取类型名（用于方法调用） |
| `getStructType` | `IRStructType getStructType(Type type)` | 从 AST 类型获取 IR 结构体类型 |
| `emitCastIfNeeded` | `IRValue emitCastIfNeeded(IRValue value, IRType targetType)` | 必要时生成类型转换指令 |
| `findCommonIRType` | `IRType findCommonIRType(IRType type1, IRType type2)` | 找两个类型的公共类型 |
| `isSignedType` | `boolean isSignedType(Type type)` | 判断 AST 类型是否有符号 |
| `getFunctionType` | `IRFunctionType getFunctionType(IRFunction func)` | 从 IRFunction 获取函数类型 |
| `convertSelfType` | `IRType convertSelfType(SelfParaNode selfPara)` | 转换 self 参数类型 |
| `getTypeName` | `String getTypeName(TypeExprNode typeExpr)` | 从类型表达式获取类型名 |

### 3.5 符号管理 ✅ [已合并]

> **变更**：原分散在多处的符号相关方法已全部合并到此区域

| 方法 | 签名 | 作用 |
|------|------|------|
| `mapSymbol` | `void mapSymbol(Symbol symbol, IRValue value)` | 映射符号到 IR 值 |
| `getSymbolValue` | `IRValue getSymbolValue(Symbol symbol)` | 获取符号对应的 IR 值 |
| `getPatternName` | `String getPatternName(PatternNode pattern)` | 从模式获取变量名 |
| `getSymbol` | `Symbol getSymbol(ASTNode node)` | 从节点获取符号（TODO） |
| `getSymbolFromPattern` | `Symbol getSymbolFromPattern(PatternNode pattern)` | 从模式获取符号 |
| `getConstSymbolValue` | `IRConstant getConstSymbolValue(Symbol symbol)` | 获取常量符号的值 |

### 3.6 运算符映射 ✅ [已合并]

> **变更**：`mapComAssignOp` 已移动到此区域，消除了重复的区域标题

| 方法 | 签名 | 作用 |
|------|------|------|
| `mapBinaryOp` | `BinaryOpInst.Op mapBinaryOp(oper_t op, boolean isSigned)` | 映射 AST 二元运算符到 IR |
| `mapCmpPred` | `CmpInst.Pred mapCmpPred(oper_t op, boolean isSigned)` | 映射 AST 比较运算符到 IR |
| `mapComAssignOp` | `BinaryOpInst.Op mapComAssignOp(oper_t op, boolean isSigned)` | 映射复合赋值运算符到基本运算 |

### 3.7 常量处理

| 方法 | 签名 | 作用 |
|------|------|------|
| `createStringConstant` | `IRValue createStringConstant(String str)` | 创建字符串全局常量 |
| `convertConstantValue` | `IRConstant convertConstantValue(ConstantValue value, IRType targetType)` | 将 ConstantValue 转换为 IRConstant |

### 3.8 类型收集（Pass 1）

| 方法 | 签名 | 作用 |
|------|------|------|
| `collectStruct` | `void collectStruct(StructNode node)` | 收集结构体类型定义 |
| `collectEnum` | `void collectEnum(EnumNode node)` | 收集枚举类型定义 |

---

## 4. 当前代码组织结构 ✅ [已完成重组]

```java
package codegen;

import ...;

/**
 * IR 代码生成器
 * 将 AST 转换为 IR
 */
public class IRGenerator extends VisitorBase {

    // ==================== 内部类 ====================              // 行 21

    private static class LoopContext { ... }
    private static class FunctionContext { ... }

    // ==================== 状态变量 ====================             // 行 66

    private IRModule module;
    private IRFunction currentFunction;
    private IRBasicBlock currentBlock;
    private final Map<Symbol, IRValue> symbolMap = new HashMap<>();
    private final Map<Symbol, IRConstant> constSymbolMap = new HashMap<>();
    private final Deque<LoopContext> loopStack = new ArrayDeque<>();
    private IRStructType currentImplType = null;
    private ConstantEvaluator constantEvaluator;
    private int stringConstantCounter = 0;

    // ==================== 入口方法 ====================             // 行 90

    public IRModule generate(List<ASTNode> statements) { ... }
    public IRModule generate(List<ASTNode> statements, ConstantEvaluator eval) { ... }

    // ==================== 类型收集（Pass 1） ====================   // 行 128

    private void collectStruct(StructNode node) { ... }
    private void collectEnum(EnumNode node) { ... }

    // ==================== Visitor 方法：顶层项 ====================  // 行 163

    @Override public void visit(FunctionNode node) { ... }
    @Override public void visit(StructNode node) { ... }
    @Override public void visit(EnumNode node) { ... }
    @Override public void visit(ConstItemNode node) { ... }
    @Override public void visit(ImplNode node) { ... }

    // ==================== Visitor 方法：语句 ====================    // 行 332

    @Override public void visit(LetStmtNode node) { ... }
    @Override public void visit(ExprStmtNode node) { ... }

    // ==================== 表达式求值入口 ====================        // 行 364

    protected IRValue visitExpr(ExprNode expr) { ... }

    // ==================== 表达式处理方法 ====================        // 行 422

    // 字面量和路径
    protected IRValue visitLiteral(LiteralExprNode node) { ... }
    protected IRValue visitPath(PathExprNode node) { ... }

    // 运算表达式
    protected IRValue visitArith(ArithExprNode node) { ... }
    protected IRValue visitComp(CompExprNode node) { ... }
    protected IRValue visitLazy(LazyExprNode node) { ... }
    protected IRValue visitNega(NegaExprNode node) { ... }

    // 赋值表达式
    protected IRValue visitAssign(AssignExprNode node) { ... }
    protected IRValue visitComAssign(ComAssignExprNode node) { ... }

    // 调用表达式
    protected IRValue visitCall(CallExprNode node) { ... }
    protected IRValue visitMethodCall(MethodCallExprNode node) { ... }

    // 访问表达式
    protected IRValue visitField(FieldExprNode node) { ... }
    protected IRValue visitIndex(IndexExprNode node) { ... }

    // 构造表达式
    protected IRValue visitArrayExpr(ArrayExprNode node) { ... }
    protected IRValue visitStructExpr(StructExprNode node) { ... }

    // 指针表达式
    protected IRValue visitBorrow(BorrowExprNode node) { ... }
    protected IRValue visitDeref(DerefExprNode node) { ... }

    // 类型转换
    protected IRValue visitTypeCast(TypeCastExprNode node) { ... }

    // 控制流表达式
    protected IRValue visitBlock(BlockExprNode node) { ... }
    protected IRValue visitIf(IfExprNode node) { ... }
    protected IRValue visitLoop(LoopExprNode node) { ... }
    protected IRValue visitBreak(BreakExprNode node) { ... }
    protected IRValue visitContinue(ContinueExprNode node) { ... }
    protected IRValue visitReturn(ReturnExprNode node) { ... }

    // ==================== 辅助方法：IR 构建 ====================     // 行 1177

    protected IRRegister newTemp(IRType type) { ... }
    protected IRRegister newTemp(IRType type, String hint) { ... }
    protected void emit(IRInstruction inst) { ... }
    protected IRBasicBlock createBlock(String name) { ... }
    protected void setCurrentBlock(IRBasicBlock block) { ... }

    // ==================== 辅助方法：上下文管理 ====================   // 行 1219 ✅ [已合并]

    protected FunctionContext saveFunctionContext() { ... }
    protected void restoreFunctionContext(FunctionContext context) { ... }
    protected void pushLoopContext(IRBasicBlock header, IRBasicBlock exit, IRType resultType) { ... }
    protected LoopContext popLoopContext() { ... }
    protected IRBasicBlock getCurrentLoopHeader() { ... }
    protected IRBasicBlock getCurrentLoopExit() { ... }
    protected LoopContext getCurrentLoopContext() { ... }

    // ==================== 辅助方法：左值处理 ====================     // 行 1291 ✅ [已移动]

    protected IRValue visitLValue(ExprNode expr) { ... }
    protected IRValue visitExprAsAddr(ExprNode expr) { ... }
    protected IRValue visitFieldLValue(FieldExprNode node) { ... }
    protected IRValue visitIndexLValue(IndexExprNode node) { ... }

    // ==================== 辅助方法：类型转换 ====================     // 行 1372 ✅ [已合并]

    protected IRType convertType(Type astType) { ... }
    protected Type extractType(TypeExprNode typeExpr) { ... }
    protected String getTypeNameFromExpr(ExprNode expr) { ... }
    protected IRStructType getStructType(Type type) { ... }
    protected IRValue emitCastIfNeeded(IRValue value, IRType targetType) { ... }
    protected IRType findCommonIRType(IRType type1, IRType type2) { ... }
    protected boolean isSignedType(Type type) { ... }
    protected IRFunctionType getFunctionType(IRFunction func) { ... }
    protected IRType convertSelfType(SelfParaNode selfPara) { ... }
    protected String getTypeName(TypeExprNode typeExpr) { ... }

    // ==================== 辅助方法：符号管理 ====================     // 行 1573 ✅ [已合并]

    protected void mapSymbol(Symbol symbol, IRValue value) { ... }
    protected IRValue getSymbolValue(Symbol symbol) { ... }
    protected String getPatternName(PatternNode pattern) { ... }
    protected Symbol getSymbol(ASTNode node) { ... }
    protected Symbol getSymbolFromPattern(PatternNode pattern) { ... }
    protected IRConstant getConstSymbolValue(Symbol symbol) { ... }

    // ==================== 辅助方法：运算符映射 ====================   // 行 1628 ✅ [已合并]

    protected BinaryOpInst.Op mapBinaryOp(oper_t op, boolean isSigned) { ... }
    protected CmpInst.Pred mapCmpPred(oper_t op, boolean isSigned) { ... }
    protected BinaryOpInst.Op mapComAssignOp(oper_t op, boolean isSigned) { ... }

    // ==================== 辅助方法：常量处理 ====================     // 行 1687

    protected IRValue createStringConstant(String str) { ... }
    protected IRConstant convertConstantValue(ConstantValue value, IRType targetType) { ... }
}
```

---

## 5. 重组变更摘要 ✅

以下是本次代码重组的主要变更：

| 变更项 | 原状态 | 新状态 |
|--------|--------|--------|
| 内部类位置 | 分散在文件中 | 移到类顶部 (行 21) |
| `stringConstantCounter` | 在文件中间 | 移到状态变量区域 (行 66) |
| 表达式求值入口 | 在辅助方法之后 | 移到 Visitor 方法之后 (行 364) |
| 表达式处理方法 | 在辅助方法之后 | 移到表达式求值入口之后 (行 422) |
| 函数上下文管理 + 循环上下文管理 | 两个独立区域 | 合并为"上下文管理" (行 1219) |
| 左值处理 | 在文件末尾 | 移到上下文管理之后 (行 1291) |
| 类型转换相关方法 | 分散在 5+ 个位置 | 全部合并到一个区域 (行 1372) |
| 符号管理相关方法 | 分散在 3+ 个位置 | 全部合并到一个区域 (行 1573) |
| 运算符映射 | 有重复的区域标题 | 合并为单一区域 (行 1628) |
| `mapComAssignOp` | 在左值处理之后 | 移到运算符映射区域 |

### 已消除的问题

1. ✅ `stringConstantCounter` 已移到状态变量区域
2. ✅ `mapComAssignOp` 已与其他运算符映射方法放在一起
3. ✅ 类型相关方法已合并到一个区域
4. ✅ 符号管理方法已合并到一个区域
5. ✅ 左值处理方法位置已调整，不再有其他方法插入其中
6. ✅ 消除了重复的区域标题（运算符映射、类型转换）
