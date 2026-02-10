# RCompiler IR Codegen 框架文档（详细版）

## 1. 目标与范围
**目标**：将已完成语义/类型检查的 AST 转换为统一的 IR（中间表示），为后续优化与后端生成（如 LLVM IR/汇编）提供稳定入口。

**范围**：
- 语句/表达式的 IR 生成（含副作用、短路、控制流）
- 控制流结构的 CFG 组织
- 符号、类型、内存布局的映射
- 运行期支持（内置函数、调用约定、栈帧/寄存器抽象）
- 诊断信息（IR dump/CFG 输出）

**非目标（当前阶段）**：
- 高级优化（GVN、LICM、寄存器分配）
- SSA 必须化
- 完整 ABI（如跨平台调用约定）

---

## 2. IR 设计原则与取舍
1. **结构化 + 基本块化**：以函数为单位构建 CFG，基本块内线性指令。
2. **非 SSA 起步**：初期三地址码风格便于快速落地，后续再引入 SSA 与 Phi。
3. **类型携带**：每条指令、每个值都带 `IRType`，便于类型驱动的 lowering 与后端映射。
4. **控制流明确**：`br/jmp/ret` 作为终结指令，杜绝隐式跳转。
5. **与 AST 解耦**：IR 层为纯数据结构，不依赖 AST 的类层次。
6. **可追溯性**：保留到源码节点的可选映射（source span），便于错误定位与调试。

---

## 3. 总体流程
1. **前置条件**：命名空间分析与类型检查已通过，AST 节点附带类型信息。
2. **全局收集**：扫描顶层项，记录函数、结构体、枚举、常量、内置函数。
3. **类型与布局准备**：
   - 生成 `IRType` 表
   - 计算结构体字段偏移与大小（`SymbolLayout`）
4. **函数级 Codegen**：
   - 创建 entry block
   - 分配参数与局部变量（`alloca`）
   - 生成语句/表达式 IR
5. **CFG 完整性校验**：
   - 每个基本块必须有终结指令
   - 分支目标必须存在
6. **输出**：
   - `IRModule`（全局表）
   - `IRFunction`（CFG + 指令）
   - Dump（文本/图形化）

---

## 4. 模块划分（建议）
### 4.1 ir/
- `IRModule`：全局容器（函数表、全局变量、类型表、内置声明）
- `IRFunction`：函数级 IR（参数、返回类型、基本块、局部表）
- `IRBasicBlock`：基本块（指令序列 + 终结指令）
- `IRInstruction`：指令基类与子类（`Binary/Unary/Call/Load/Store/Branch/Return/GEP/Alloca`）
- `IRType`：IR 类型系统（原生类型 + 复合类型）
- `IRValue`：值对象（临时值、常量、全局符号、局部地址）
- `IRConst`：常量值（整数、布尔、字符、字符串、null）

### 4.2 codegen/
- `IRBuilder`：
  - 管理插入点
  - 创建临时值（%t0, %t1...）
  - 简化指令创建流程
- `IRCodegen`：AST → IR 访问器
- `SymbolLayout`：
  - 变量与字段布局
  - 结构体大小与对齐

### 4.3 lowering/（可选）
- 复杂表达式拆解（短路/结构体构造/数组字面量）
- 语义糖降级（`+=` 等）

---

## 5. 关键数据结构（建议）
### 5.1 IRModule
- `Map<String, IRFunction> functions`
- `Map<String, IRGlobal> globals`
- `Map<String, IRStructType> types`
- `List<IRFunction> builtins`

### 5.2 IRFunction
- `String name`
- `List<IRParameter> params`
- `IRType returnType`
- `List<IRBasicBlock> blocks`
- `Map<String, IRValue> locals`（变量→地址）

### 5.3 IRBasicBlock
- `String label`
- `List<IRInstruction> instructions`
- `IRTerminator terminator`（`br/jmp/ret`）

### 5.4 IRValue
- `IRType type`
- `ValueKind`（TEMP/CONST/GLOBAL/LOCAL/ADDR）
- `String name`（可选，用于 debug）

### 5.5 IRInstruction（建议子类）
- **算术与逻辑**：`BinaryOp`, `UnaryOp`
- **内存**：`Alloca`, `Load`, `Store`, `GEP`
- **控制流**：`CondBr`, `Br`, `Return`
- **调用**：`Call`
- **SSA（后续）**：`Phi`

### 5.6 IRType
- `PrimitiveType`：i32, u32, bool, char, unit, never
- `PointerType`：指向任意 `IRType`
- `StructType`：字段类型列表与布局
- `ArrayType`：元素类型 + 长度
- `FunctionType`：参数类型列表 + 返回类型

---

## 6. AST 到 IR 的映射规范
### 6.1 表达式
- **字面量** → `IRConst`
- **标识符** → 变量地址 `addr`，使用时生成 `Load`
- **二元运算** → 生成 `BinaryOp`，返回临时值
- **赋值** → 生成 `Store`（若为复合赋值，先 `Load` 再运算再 `Store`）
- **函数调用** → `Call`（返回值为临时值）
- **字段访问** → `GEP` 计算地址 + `Load`
- **数组索引** → `GEP` + `Load`
- **借用表达式** → 直接返回地址（或地址的地址）
- **解引用** → `Load`

### 6.2 语句
- `let`：
  - `Alloca` 分配局部空间
  - 若有初始化值，生成 `Store`
- `if`：
  - 生成条件块、then 块、else 块、merge 块
  - 若表达式有值，merge 处可生成 `Phi`
- `loop`：
  - header → body → backedge
  - break/continue 记录跳转目标
- `return`：
  - 直接生成 `Return` 终结

---

## 7. CFG 构建规则
1. 每个函数必须有 **entry block**。
2. 所有 block 结束必须是 `CondBr` / `Br` / `Return`。
3. `if`：`entry → then/else → merge`。
4. `loop`：`header → body → backedge`，break 跳转到 `exit`。
5. `return`：当前 block 终止，不再产生后继。

---

## 8. 内存模型与调用约定（初期）
1. **局部变量**：使用 `Alloca` 栈分配。
2. **参数传递**：默认值传递，后续可扩展为大对象地址传递。
3. **返回值**：
   - 标量直接返回
   - 结构体可在后续引入 sret
4. **内置函数**：在 `IRModule` 初始化阶段注册（如 `print`, `panic`）。

---

## 9. 错误处理与诊断
1. **不可恢复错误**：类型缺失、符号未解析、非法控制流。
2. **IR Dump**：
   - 文本输出（函数/基本块/指令序列）
   - 可选 CFG 可视化（dot）
3. **Source Mapping**（可选）：指令记录来源 AST 节点用于诊断。

---

## 10. 分阶段实施建议
1. **Phase 1**：IR 基础结构 + 简单表达式/语句
2. **Phase 2**：控制流（if/loop/break/continue）
3. **Phase 3**：结构体/数组/引用
4. **Phase 4**：SSA 与优化（可选）

---

## 11. IR 指令集细化规范

本节给出当前阶段 IR 指令集的最小完备集合与统一格式。指令以“操作码 + 操作数”表示，所有值均带 `IRType`。

### 11.1 通用约定
- **值与地址分离**：变量名默认代表地址（`addr`），使用 `load` 获取值。
- **显式类型**：指令输出值与输入值均显式标注类型。
- **临时值命名**：`%t0`, `%t1`, ...

### 11.2 内存指令
1. **alloca**
  - 语义：在当前栈帧分配地址。
  - 形式：`%dst = alloca <type>`
2. **load**
  - 语义：从地址读取值。
  - 形式：`%dst = load <type>, <type>* %addr`
3. **store**
  - 语义：将值写入地址。
  - 形式：`store <type> %val, <type>* %addr`
4. **gep**（get element pointer）
  - 语义：计算结构体/数组字段地址。
  - 形式：`%dst = gep <base-type>* %base, i32 <index>`

### 11.3 算术与逻辑指令
1. **add/sub/mul/div**
  - 语义：整型运算。
  - 形式：`%dst = add <type> %lhs, %rhs`
2. **cmp**
  - 语义：比较运算，返回 `bool`。
  - 形式：`%dst = cmp <pred> <type> %lhs, %rhs`
  - `pred` 取值：`eq/ne/lt/le/gt/ge`
3. **not**
  - 语义：逻辑非。
  - 形式：`%dst = not bool %val`

### 11.4 控制流指令
1. **br**
  - 语义：无条件跳转。
  - 形式：`br label %target`
2. **condbr**
  - 语义：条件跳转。
  - 形式：`condbr bool %cond, label %then, label %else`
3. **ret**
  - 语义：函数返回。
  - 形式：`ret <type> %val` 或 `ret void`

### 11.5 调用指令
1. **call**
  - 语义：函数调用。
  - 形式：`%dst = call <ret-type> @func(<args...>)`
  - 若无返回值：`call void @func(<args...>)`

### 11.6 常量与类型
- **整数常量**：`i32 42`
- **布尔常量**：`bool true/false`
- **空值**：`void` 用于无返回值函数

### 11.7 指令选择与 AST 映射（最小表）
| AST 节点 | 指令序列 |
|---|---|
| 字面量 | `IRConst` |
| 变量引用 | `load` |
| 赋值 | `store` |
| 二元算术 | `add/sub/mul/div` |
| 比较 | `cmp` |
| if | `condbr` + blocks |
| return | `ret` |

---

---

## 12. 验收标准
- 能生成完整函数 CFG
- 常见表达式与语句可生成正确 IR
- 控制流块连接正确、无悬空终结
- IR dump 可读且可用于后续后端
- 结构体/数组/引用映射无崩溃（阶段 3 后）

---

## 13. 后续可扩展方向
1. SSA 化与 Phi 插入
2. 常量折叠与简单优化
3. 调试信息（source map）完善
4. 后端对接（LLVM IR 生成）

---

## 14. 简单示例（源代码 → IR 输出示意）

### 14.1 源代码（示例）
```rust
fn add(a: i32, b: i32) -> i32 {
    let c = a + b;
    return c;
}
```

### 14.2 IR 输出（示意）
```
module {
  func @add(i32 %a, i32 %b) -> i32 {
  entry:
    %a.addr = alloca i32
    %b.addr = alloca i32
    %c.addr = alloca i32
    store i32 %a, i32* %a.addr
    store i32 %b, i32* %b.addr
    %t0 = load i32, i32* %a.addr
    %t1 = load i32, i32* %b.addr
    %t2 = add i32 %t0, %t1
    store i32 %t2, i32* %c.addr
    %t3 = load i32, i32* %c.addr
    ret i32 %t3
  }
}
```

### 14.3 说明
- `alloca` 用于局部分配（参数在 entry 中落栈）。
- `load/store` 明确区分地址与值。
- `add` 产生临时值 `%t2`，最终返回 `%t3`。

---

## 15. 测试方式（选用：IR 解释器）

本阶段采用 **IR 解释器** 来验证 IR Codegen 的正确性。核心思路：
1. 由 Codegen 生成 IR。
2. 用 IR 解释器执行 IR。
3. 将执行结果与源程序的预期结果比对。

### 15.1 解释器职责范围
- 支持基础指令：`alloca`、`load`、`store`、`add/sub/mul/div`、`cmp`、`br`、`ret`。
- 支持基本块跳转与条件分支。
- 支持函数调用（仅限同模块函数）。
- 支持基础类型：i32、bool。

### 15.2 最小执行模型（建议）
- **栈帧模型**：每个函数调用创建新的栈帧，保存局部变量地址映射。
- **内存模型**：
  - `alloca` 产生新地址（可用自增 ID 模拟）。
  - `store/load` 通过地址读写。
- **控制流**：维护当前基本块与指令索引，遇到 `br/ret` 切换。

### 15.3 示例测试用例
源代码：
```rust
fn add(a: i32, b: i32) -> i32 {
    let c = a + b;
    return c;
}

fn main() -> i32 {
    return add(2, 3);
}
```

预期：
```
main() 返回 5
```

解释器执行 `main` 的 IR 后，结果应为 `5`。

### 15.4 验收标准
- 解释器执行结果与预期一致。
- 控制流（if/loop/return）在解释器中可正确跳转。
- 对同一源程序多次生成 IR，解释器结果稳定。

---

## 16. Java 接口定义

### 16.1 IR 类型系统

```java
// ir/type/IRType.java
public abstract class IRType {
    public abstract int getSize();      // 字节大小
    public abstract int getAlign();     // 对齐要求
    public abstract String toString();
}

// ir/type/IRIntType.java
public class IRIntType extends IRType {
    public static final IRIntType I1 = new IRIntType(1);
    public static final IRIntType I8 = new IRIntType(8);
    public static final IRIntType I32 = new IRIntType(32);
    public static final IRIntType I64 = new IRIntType(64);

    private final int bits;
    private IRIntType(int bits) { this.bits = bits; }

    public int getBits() { return bits; }
    public int getSize() { return (bits + 7) / 8; }
    public int getAlign() { return getSize(); }
    public String toString() { return "i" + bits; }
}

// ir/type/IRPtrType.java
public class IRPtrType extends IRType {
    private final IRType pointee;
    public IRPtrType(IRType pointee) { this.pointee = pointee; }
    public IRType getPointee() { return pointee; }
    public int getSize() { return 8; }  // 64-bit pointer
    public int getAlign() { return 8; }
    public String toString() { return pointee + "*"; }
}

// ir/type/IRStructType.java
public class IRStructType extends IRType {
    private final String name;
    private final List<IRType> fields;
    private final List<Integer> offsets;
    private int totalSize;

    public IRStructType(String name, List<IRType> fields) {
        this.name = name;
        this.fields = fields;
        this.offsets = new ArrayList<>();
        computeLayout();
    }

    private void computeLayout() {
        int offset = 0;
        for (IRType field : fields) {
            int align = field.getAlign();
            offset = (offset + align - 1) / align * align;
            offsets.add(offset);
            offset += field.getSize();
        }
        totalSize = offset;
    }

    public int getFieldOffset(int index) { return offsets.get(index); }
    public IRType getFieldType(int index) { return fields.get(index); }
    public int getSize() { return totalSize; }
    public int getAlign() {
        if (fields.isEmpty()) return 1;
        int maxAlign = 1;
        for (IRType field : fields) {
            maxAlign = Math.max(maxAlign, field.getAlign());
        }
        return maxAlign;
    }
    public String toString() { return "%" + name; }
}

// ir/type/IRArrayType.java
public class IRArrayType extends IRType {
    private final IRType elementType;
    private final int length;

    public IRArrayType(IRType elementType, int length) {
        this.elementType = elementType;
        this.length = length;
    }

    public IRType getElementType() { return elementType; }
    public int getLength() { return length; }
    public int getSize() { return elementType.getSize() * length; }
    public int getAlign() { return elementType.getAlign(); }
    public String toString() { return "[" + length + " x " + elementType + "]"; }
}

// ir/type/IRVoidType.java
public class IRVoidType extends IRType {
    public static final IRVoidType INSTANCE = new IRVoidType();
    private IRVoidType() {}
    public int getSize() { return 0; }
    public int getAlign() { return 1; }
    public String toString() { return "void"; }
}
```

### 16.2 IR 值系统

```java
// ir/value/IRValue.java
public abstract class IRValue {
    protected IRType type;
    protected String name;

    public IRType getType() { return type; }
    public String getName() { return name; }
    public abstract String toString();
}

// ir/value/IRRegister.java (临时变量)
public class IRRegister extends IRValue {
    private static int counter = 0;

    public IRRegister(IRType type) {
        this.type = type;
        this.name = "t" + (counter++);
    }

    public IRRegister(IRType type, String hint) {
        this.type = type;
        this.name = hint + "." + (counter++);
    }

    public String toString() { return "%" + name; }
    public static void resetCounter() { counter = 0; }
}

// ir/value/IRConstant.java
public class IRConstant extends IRValue {
    private final Object value;  // Long, Boolean, String, null

    public IRConstant(IRType type, Object value) {
        this.type = type;
        this.value = value;
    }

    public Object getValue() { return value; }

    public String toString() {
        if (type instanceof IRIntType) return type + " " + value;
        if (value instanceof Boolean) return "i1 " + (((Boolean)value) ? "1" : "0");
        return type + " " + value;
    }

    // 工厂方法
    public static IRConstant i32(long v) { return new IRConstant(IRIntType.I32, v); }
    public static IRConstant i1(boolean v) { return new IRConstant(IRIntType.I1, v); }
}

// ir/value/IRGlobal.java
public class IRGlobal extends IRValue {
    public IRGlobal(IRType type, String name) {
        this.type = type;
        this.name = name;
    }
    public String toString() { return "@" + name; }
}
```

### 16.3 IR 指令系统

```java
// ir/inst/IRInstruction.java
public abstract class IRInstruction {
    protected IRValue result;  // 可为 null（如 store, br）

    public IRValue getResult() { return result; }
    public abstract String toString();
    public abstract void accept(IRVisitor visitor);
}

// ir/inst/AllocaInst.java
public class AllocaInst extends IRInstruction {
    private final IRType allocType;

    public AllocaInst(IRRegister result, IRType allocType) {
        this.result = result;
        this.allocType = allocType;
    }

    public IRType getAllocType() { return allocType; }
    public String toString() {
        return result + " = alloca " + allocType;
    }
    public void accept(IRVisitor v) { v.visit(this); }
}

// ir/inst/LoadInst.java
public class LoadInst extends IRInstruction {
    private final IRValue address;

    public LoadInst(IRRegister result, IRValue address) {
        this.result = result;
        this.address = address;
    }

    public IRValue getAddress() { return address; }
    public String toString() {
        return result + " = load " + result.getType() + ", " +
               address.getType() + " " + address;
    }
    public void accept(IRVisitor v) { v.visit(this); }
}

// ir/inst/StoreInst.java
public class StoreInst extends IRInstruction {
    private final IRValue value;
    private final IRValue address;

    public StoreInst(IRValue value, IRValue address) {
        this.result = null;
        this.value = value;
        this.address = address;
    }

    public IRValue getValue() { return value; }
    public IRValue getAddress() { return address; }
    public String toString() {
        return "store " + value.getType() + " " + value + ", " +
               address.getType() + " " + address;
    }
    public void accept(IRVisitor v) { v.visit(this); }
}

// ir/inst/BinaryOpInst.java
public class BinaryOpInst extends IRInstruction {
    public enum Op { ADD, SUB, MUL, DIV, REM, AND, OR, XOR, SHL, SHR }

    private final Op op;
    private final IRValue left, right;

    public BinaryOpInst(IRRegister result, Op op, IRValue left, IRValue right) {
        this.result = result;
        this.op = op;
        this.left = left;
        this.right = right;
    }

    public String toString() {
        return result + " = " + op.name().toLowerCase() + " " +
               left.getType() + " " + left + ", " + right;
    }
    public void accept(IRVisitor v) { v.visit(this); }
}

// ir/inst/CmpInst.java
public class CmpInst extends IRInstruction {
    public enum Pred { EQ, NE, LT, LE, GT, GE }

    private final Pred pred;
    private final IRValue left, right;

    public CmpInst(IRRegister result, Pred pred, IRValue left, IRValue right) {
        this.result = result;
        this.pred = pred;
        this.left = left;
        this.right = right;
    }

    public String toString() {
        return result + " = cmp " + pred.name().toLowerCase() + " " +
               left.getType() + " " + left + ", " + right;
    }
    public void accept(IRVisitor v) { v.visit(this); }
}

// ir/inst/CallInst.java
public class CallInst extends IRInstruction {
    private final IRValue function;
    private final List<IRValue> args;

    public CallInst(IRRegister result, IRValue function, List<IRValue> args) {
        this.result = result;
        this.function = function;
        this.args = args;
    }

    public String toString() {
        String argStr = args.stream()
            .map(a -> a.getType() + " " + a)
            .collect(Collectors.joining(", "));
        if (result != null) {
            return result + " = call " + result.getType() + " " + function + "(" + argStr + ")";
        }
        return "call void " + function + "(" + argStr + ")";
    }
    public void accept(IRVisitor v) { v.visit(this); }
}

// ir/inst/BranchInst.java
public class BranchInst extends IRInstruction {
    private final IRBasicBlock target;

    public BranchInst(IRBasicBlock target) {
        this.result = null;
        this.target = target;
    }

    public IRBasicBlock getTarget() { return target; }
    public String toString() { return "br label %" + target.getName(); }
    public void accept(IRVisitor v) { v.visit(this); }
}

// ir/inst/CondBranchInst.java
public class CondBranchInst extends IRInstruction {
    private final IRValue condition;
    private final IRBasicBlock thenBlock, elseBlock;

    public CondBranchInst(IRValue cond, IRBasicBlock thenB, IRBasicBlock elseB) {
        this.result = null;
        this.condition = cond;
        this.thenBlock = thenB;
        this.elseBlock = elseB;
    }

    public String toString() {
        return "br i1 " + condition + ", label %" + thenBlock.getName() +
               ", label %" + elseBlock.getName();
    }
    public void accept(IRVisitor v) { v.visit(this); }
}

// ir/inst/ReturnInst.java
public class ReturnInst extends IRInstruction {
    private final IRValue value;  // null for void return

    public ReturnInst(IRValue value) {
        this.result = null;
        this.value = value;
    }

    public IRValue getValue() { return value; }
    public String toString() {
        if (value == null) return "ret void";
        return "ret " + value.getType() + " " + value;
    }
    public void accept(IRVisitor v) { v.visit(this); }
}

// ir/inst/GEPInst.java (GetElementPtr)
public class GEPInst extends IRInstruction {
    private final IRValue base;
    private final List<IRValue> indices;

    public GEPInst(IRRegister result, IRValue base, List<IRValue> indices) {
        this.result = result;
        this.base = base;
        this.indices = indices;
    }

    public String toString() {
        String idxStr = indices.stream()
            .map(i -> i.getType() + " " + i)
            .collect(Collectors.joining(", "));
        return result + " = gep " + base.getType() + " " + base + ", " + idxStr;
    }
    public void accept(IRVisitor v) { v.visit(this); }
}
```

### 16.4 IR 程序结构

```java
// ir/IRBasicBlock.java
public class IRBasicBlock {
    private final String name;
    private final List<IRInstruction> instructions = new ArrayList<>();
    private IRInstruction terminator;  // br/ret

    public IRBasicBlock(String name) { this.name = name; }

    public String getName() { return name; }
    public List<IRInstruction> getInstructions() { return instructions; }
    public IRInstruction getTerminator() { return terminator; }

    public void addInstruction(IRInstruction inst) {
        if (inst instanceof BranchInst || inst instanceof CondBranchInst ||
            inst instanceof ReturnInst) {
            terminator = inst;
        } else {
            instructions.add(inst);
        }
    }

    public boolean isTerminated() { return terminator != null; }
}

// ir/IRFunction.java
public class IRFunction {
    private final String name;
    private final IRType returnType;
    private final List<IRRegister> params = new ArrayList<>();
    private final List<IRBasicBlock> blocks = new ArrayList<>();
    private final Map<Symbol, IRValue> locals = new HashMap<>();

    public IRFunction(String name, IRType returnType) {
        this.name = name;
        this.returnType = returnType;
    }

    public void addParam(IRRegister param) { params.add(param); }
    public void addBlock(IRBasicBlock block) { blocks.add(block); }
    public IRBasicBlock getEntryBlock() { return blocks.isEmpty() ? null : blocks.get(0); }

    public void mapLocal(Symbol sym, IRValue addr) { locals.put(sym, addr); }
    public IRValue getLocal(Symbol sym) { return locals.get(sym); }

    // Getters...
}

// ir/IRModule.java
public class IRModule {
    private final Map<String, IRFunction> functions = new LinkedHashMap<>();
    private final Map<String, IRGlobal> globals = new LinkedHashMap<>();
    private final Map<String, IRStructType> structs = new LinkedHashMap<>();

    public void addFunction(IRFunction func) { functions.put(func.getName(), func); }
    public void addGlobal(IRGlobal global) { globals.put(global.getName(), global); }
    public void addStruct(IRStructType struct) { structs.put(struct.getName(), struct); }

    public IRFunction getFunction(String name) { return functions.get(name); }
    public Collection<IRFunction> getFunctions() { return functions.values(); }
    // ...
}
```

### 16.5 IR 生成器核心

```java
// codegen/IRGenerator.java
public class IRGenerator extends VisitorBase {
    private IRModule module;
    private IRFunction currentFunction;
    private IRBasicBlock currentBlock;
    private final Map<Symbol, IRValue> symbolMap = new HashMap<>();

    // 循环上下文栈（用于 break/continue）
    private final Deque<LoopContext> loopStack = new ArrayDeque<>();

    public IRModule generate(List<ASTNode> statements) {
        module = new IRModule();

        // Pass 1: 收集类型定义
        for (ASTNode stmt : statements) {
            if (stmt instanceof StructNode) collectStruct((StructNode) stmt);
        }

        // Pass 2: 生成函数
        for (ASTNode stmt : statements) {
            stmt.accept(this);
        }

        return module;
    }

    // === 辅助方法 ===

    private IRRegister newTemp(IRType type) {
        return new IRRegister(type);
    }

    private IRRegister newTemp(IRType type, String hint) {
        return new IRRegister(type, hint);
    }

    private void emit(IRInstruction inst) {
        currentBlock.addInstruction(inst);
    }

    private IRBasicBlock createBlock(String name) {
        IRBasicBlock block = new IRBasicBlock(name);
        currentFunction.addBlock(block);
        return block;
    }

    private void setCurrentBlock(IRBasicBlock block) {
        currentBlock = block;
    }

    private IRType convertType(Type astType) {
        if (astType instanceof PrimitiveType) {
            PrimitiveType pt = (PrimitiveType) astType;
            switch (pt.getKind()) {
                case I32: case U32: return IRIntType.I32;
                case BOOL: return IRIntType.I1;
                case USIZE: case ISIZE: return IRIntType.I64;
                default: throw new RuntimeException("Unsupported: " + pt);
            }
        } else if (astType instanceof ReferenceType) {
            return new IRPtrType(convertType(((ReferenceType) astType).getInnerType()));
        } else if (astType instanceof ArrayType) {
            ArrayType at = (ArrayType) astType;
            return new IRArrayType(convertType(at.getElementType()), at.getSize());
        } else if (astType instanceof StructType) {
            return module.getStruct(((StructType) astType).getName());
        } else if (astType instanceof UnitType) {
            return IRVoidType.INSTANCE;
        }
        throw new RuntimeException("Unknown type: " + astType);
    }

    // === Visitor 实现 ===

    @Override
    public void visit(FunctionNode node) {
        IRType retType = node.returnType != null ?
            convertType(extractType(node.returnType)) : IRVoidType.INSTANCE;

        currentFunction = new IRFunction(node.name.name, retType);
        module.addFunction(currentFunction);

        // Entry block
        IRBasicBlock entry = createBlock("entry");
        setCurrentBlock(entry);

        // 参数处理
        if (node.parameters != null) {
            for (ParameterNode param : node.parameters) {
                IRType paramType = convertType(param.getParameterType());
                IRRegister paramReg = newTemp(paramType, getPatternName(param.name));
                currentFunction.addParam(paramReg);

                // alloca + store
                IRRegister addr = newTemp(new IRPtrType(paramType));
                emit(new AllocaInst(addr, paramType));
                emit(new StoreInst(paramReg, addr));
                symbolMap.put(getSymbol(param), addr);
            }
        }

        // 函数体
        if (node.body != null) {
            IRValue bodyVal = visitExpr(node.body);
            if (!currentBlock.isTerminated()) {
                emit(new ReturnInst(retType instanceof IRVoidType ? null : bodyVal));
            }
        }
    }

    @Override
    public void visit(LetStmtNode node) {
        IRType varType = convertType(node.getVariableType());
        IRRegister addr = newTemp(new IRPtrType(varType), getPatternName(node.name));
        emit(new AllocaInst(addr, varType));

        if (node.value != null) {
            IRValue val = visitExpr(node.value);
            emit(new StoreInst(val, addr));
        }

        symbolMap.put(getSymbol(node), addr);
    }

    // 表达式求值，返回 IRValue
    private IRValue visitExpr(ExprNode node) {
        if (node instanceof LiteralExprNode) {
            return visitLiteral((LiteralExprNode) node);
        } else if (node instanceof PathExprNode) {
            return visitPath((PathExprNode) node);
        } else if (node instanceof ArithExprNode) {
            return visitArith((ArithExprNode) node);
        } else if (node instanceof AssignExprNode) {
            return visitAssign((AssignExprNode) node);
        } else if (node instanceof IfExprNode) {
            return visitIf((IfExprNode) node);
        } else if (node instanceof BlockExprNode) {
            return visitBlock((BlockExprNode) node);
        }
        // ... 其他表达式类型
        throw new RuntimeException("Unsupported expr: " + node.getClass());
    }

    private IRValue visitLiteral(LiteralExprNode node) {
        switch (node.literalType) {
            case INT_I32: return IRConstant.i32(node.value_long);
            case BOOL: return IRConstant.i1(node.value_bool);
            default: throw new RuntimeException("Unsupported literal");
        }
    }

    private IRValue visitPath(PathExprNode node) {
        Symbol sym = node.getSymbol();
        IRValue addr = symbolMap.get(sym);
        if (addr == null) throw new RuntimeException("Unknown symbol: " + sym);

        IRType valType = ((IRPtrType) addr.getType()).getPointee();
        IRRegister result = newTemp(valType);
        emit(new LoadInst(result, addr));
        return result;
    }

    private IRValue visitArith(ArithExprNode node) {
        IRValue left = visitExpr(node.left);
        IRValue right = visitExpr(node.right);

        BinaryOpInst.Op op = mapOp(node.operator);
        IRRegister result = newTemp(left.getType());
        emit(new BinaryOpInst(result, op, left, right));
        return result;
    }

    private IRValue visitIf(IfExprNode node) {
        IRValue cond = visitExpr(node.condition);

        IRBasicBlock thenBlock = createBlock("if.then");
        IRBasicBlock elseBlock = createBlock("if.else");
        IRBasicBlock mergeBlock = createBlock("if.merge");

        emit(new CondBranchInst(cond, thenBlock, elseBlock));

        // Then
        setCurrentBlock(thenBlock);
        IRValue thenVal = visitExpr(node.thenBranch);
        if (!currentBlock.isTerminated()) emit(new BranchInst(mergeBlock));

        // Else
        setCurrentBlock(elseBlock);
        IRValue elseVal = node.elseBranch != null ?
            visitExpr(node.elseBranch) : null;
        if (!currentBlock.isTerminated()) emit(new BranchInst(mergeBlock));

        setCurrentBlock(mergeBlock);

        // 如果 if 有返回值，需要 phi（简化版暂不实现）
        return thenVal;
    }

    // ... 更多 visitor 方法
}
```

### 16.6 IR 打印器

```java
// codegen/IRPrinter.java
public class IRPrinter {
    private final PrintStream out;

    public IRPrinter() { this(System.out); }
    public IRPrinter(PrintStream out) { this.out = out; }

    public void print(IRModule module) {
        // 结构体定义
        for (IRStructType struct : module.getStructs()) {
            out.println(struct.getName() + " = type { ... }");
        }
        out.println();

        // 函数
        for (IRFunction func : module.getFunctions()) {
            printFunction(func);
            out.println();
        }
    }

    private void printFunction(IRFunction func) {
        String params = func.getParams().stream()
            .map(p -> p.getType() + " " + p)
            .collect(Collectors.joining(", "));

        out.println("define " + func.getReturnType() + " @" +
                    func.getName() + "(" + params + ") {");

        for (IRBasicBlock block : func.getBlocks()) {
            out.println(block.getName() + ":");
            for (IRInstruction inst : block.getInstructions()) {
                out.println("    " + inst);
            }
            if (block.getTerminator() != null) {
                out.println("    " + block.getTerminator());
            }
        }

        out.println("}");
    }
}
```

---

## 17. 文件结构

```
src/main/java/
├── codegen/
│   ├── ir/
│   │   ├── IRModule.java
│   │   ├── IRFunction.java
│   │   └── IRBasicBlock.java
│   ├── type/
│   │   ├── IRType.java
│   │   ├── IRIntType.java
│   │   ├── IRPtrType.java
│   │   ├── IRArrayType.java
│   │   ├── IRStructType.java
│   │   └── IRVoidType.java
│   ├── value/
│   │   ├── IRValue.java
│   │   ├── IRRegister.java
│   │   ├── IRConstant.java
│   │   └── IRGlobal.java
│   ├── inst/
│   │   ├── IRInstruction.java
│   │   ├── AllocaInst.java
│   │   ├── LoadInst.java
│   │   ├── StoreInst.java
│   │   ├── BinaryOpInst.java
│   │   ├── CmpInst.java
│   │   ├── CallInst.java
│   │   ├── BranchInst.java
│   │   ├── CondBranchInst.java
│   │   ├── ReturnInst.java
│   │   └── GEPInst.java
│   ├── IRGenerator.java
│   ├── IRPrinter.java
│   └── IRInterpreter.java  // 测试用解释器
```

---

## 18. Visit 函数实现详解

Visit 函数的详细实现已移至单独文档：[ir_codegen_visit_functions.md](ir_codegen_visit_functions.md)
