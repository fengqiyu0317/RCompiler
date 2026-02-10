# RCompiler Rust IR 语法参考文档

## 目录

1. [概述](#概述)
2. [核心概念](#核心概念)
3. [IR类型系统](#ir类型系统)
4. [IR值与地址](#ir值与地址)
5. [IR指令集](#ir指令集)
6. [控制流结构](#控制流结构)
7. [函数调用约定](#函数调用约定)
8. [内存模型](#内存模型)
9. [IR代码示例](#ir代码示例)
10. [AST到IR的映射](#ast到ir的映射)

---

## 概述

RCompiler IR (Intermediate Representation) 是一种低级、类型化的中间表示，用于在Rust源代码和目标代码之间进行转换。IR设计为三地址码风格，具有明确的控制流和内存操作，便于后续优化和代码生成。

### 设计原则

- **类型携带**：每条指令和值都带有类型信息
- **控制流明确**：使用显式的分支和跳转指令
- **内存操作明确**：区分值和地址，使用load/store进行内存访问
- **结构化CFG**：以函数为单位构建控制流图(CFG)
- **可追溯性**：保留到源码节点的映射关系

---

## 核心概念

### 模块结构

```
module {
  // 全局声明
  [全局变量声明]
  [函数声明]
  
  // 函数定义
  func @function_name(params) -> return_type {
    // 基本块
    block_label:
      // 指令序列
      // 终结指令
  }
}
```

### 基本块

基本块是IR的基本执行单元，包含一系列指令和一个终结指令：

```
block_label:
  %t0 = add i32 %a, %b
  %t1 = icmp eq i32 %t0, 0
  condbr bool %t1, label %then, label %else
```

### 值命名约定

- **临时值**：`%t0`, `%t1`, `%t2`, ...
- **局部变量地址**：`%var_name.addr`
- **参数**：`%param_name`
- **全局符号**：`@global_name`
- **基本块标签**：`%label_name`

---

## IR类型系统

### 原生类型

| 类型 | 描述 | 示例 |
|------|------|------|
| `i1` | 布尔类型 | `bool true`, `bool false` |
| `i8` | 8位整数 | `i8 42` |
| `i16` | 16位整数 | `i16 1024` |
| `i32` | 32位整数 | `i32 12345` |
| `i64` | 64位整数 | `i64 123456789` |
| `u8`, `u16`, `u32`, `u64` | 无符号整数 | `u32 42` |
| `char` | 字符类型 | `char 'a'` |
| `unit` | 单元类型 | `unit` |
| `never` | 永不返回类型 | `never` |

### 复合类型

#### 指针类型
```
<type>*  // 指向type的指针
i32*     // 指向i32的指针
```

#### 数组类型
```
[<type>; <size>]  // 固定大小数组
[i32; 10]         // 包含10个i32的数组
```

#### 结构体类型
```
struct <name> {
  <field_name>: <type>,
  ...
}

struct Point {
  x: i32,
  y: i32
}
```

#### 函数类型
```
(<param_types>) -> <return_type>
(i32, i32) -> i32
() -> unit
```

---

## IR值与地址

### 值类别

1. **临时值**：指令计算的结果
   ```
   %t0 = add i32 %a, %b
   ```

2. **常量**：编译时已知的值
   ```
   i32 42
   bool true
   char 'a'
   ```

3. **全局符号**：全局变量或函数
   ```
   @global_var
   @function_name
   ```

4. **局部地址**：局部变量的栈地址
   ```
   %x.addr = alloca i32
   ```

### 地址与值的区别

IR明确区分地址和值：
- 变量名默认表示地址
- 使用`load`从地址读取值
- 使用`store`将值写入地址

```
%addr = alloca i32        // 分配地址
store i32 42, i32* %addr  // 存储值到地址
%val = load i32, i32* %addr // 从地址加载值
```

---

## IR指令集

### 内存指令

#### `alloca` - 栈分配
```
<dst> = alloca <type>
```
在当前栈帧分配指定类型的空间，返回地址。

**示例**：
```
%x.addr = alloca i32
%arr.addr = alloca [i32; 10]
```

#### `load` - 加载
```
<dst> = load <type>, <type>* <addr>
```
从指定地址加载值。

**示例**：
```
%val = load i32, i32* %x.addr
%elem = load i32, i32* %ptr
```

#### `store` - 存储
```
store <type> <val>, <type>* <addr>
```
将值存储到指定地址。

**示例**：
```
store i32 42, i32* %x.addr
store i32 %result, i32* %out.addr
```

#### `gep` - 计算元素指针
```
<dst> = gep <base_type>* <base>, <index_type> <index>
```
计算结构体或数组元素的地址。

**示例**：
```
// 数组元素地址
%elem_ptr = gep [i32; 10]* %arr, i32 3

// 结构体字段地址
%field_ptr = gep struct.Point* %pt, i32 0, i32 1
```

### 算术与逻辑指令

#### 二元算术指令
```
<dst> = <op> <type> <lhs>, <rhs>
```
支持的操作：`add`, `sub`, `mul`, `div`, `rem`, `shl`, `shr`, `and`, `or`, `xor`

**示例**：
```
%sum = add i32 %a, %b
%diff = sub i32 %x, %y
%prod = mul i32 %a, %b
%quot = div i32 %a, %b
%rem = rem i32 %a, %b
%shifted = shl i32 %val, i32 2
%and_result = and i32 %a, %b
```

#### 一元指令
```
<dst> = <op> <type> <operand>
```
支持的操作：`neg`, `not`

**示例**：
```
%neg_val = neg i32 %x
%not_val = not bool %cond
```

#### 比较指令
```
<dst> = icmp <pred> <type> <lhs>, <rhs>
```
比较谓词：`eq`, `ne`, `ugt`, `uge`, `ult`, `ule`, `sgt`, `sge`, `slt`, `sle`

**示例**：
```
%is_eq = icmp eq i32 %a, %b
%is_gt = icmp sgt i32 %x, %y
%is_zero = icmp eq i32 %val, i32 0
```

### 类型转换指令

#### `cast` - 类型转换
```
<dst> = cast <src_type> <val> to <dst_type>
```

**示例**：
```
%i32_val = cast i8 %byte to i32
%bool_val = cast i32 %num to bool
%char_val = cast i32 %code to char
```

### 控制流指令

#### `br` - 无条件跳转
```
br label <target>
```

**示例**：
```
br label %next_block
```

#### `condbr` - 条件跳转
```
condbr <cond_type> <cond>, label <true_target>, label <false_target>
```

**示例**：
```
condbr bool %is_positive, label %positive, label %negative
```

#### `ret` - 返回
```
ret <type> <val>    // 有返回值
ret void            // 无返回值
```

**示例**：
```
ret i32 %result
ret void
```

### 函数调用指令

#### `call` - 函数调用
```
<dst> = call <return_type> @func_name(<arg_types> <args>)
```

**示例**：
```
%result = call i32 @add(i32 %a, i32 %b)
call void @print(i32 %value)
%len = call i32 @strlen(i8* %str)
```

---

## 控制流结构

### if-else 结构

```
// 入口块
entry:
  %cond = icmp eq i32 %x, i32 0
  condbr bool %cond, label %then, label %else

// then分支
then:
  %then_val = add i32 %x, i32 1
  br label %merge

// else分支
else:
  %else_val = sub i32 %x, i32 1
  br label %merge

// 合并块
merge:
  %result = phi i32 [%then_val, %then], [%else_val, %else]
  ret i32 %result
```

### loop 结构

```
entry:
  br label %loop_header

loop_header:
  %i = load i32, i32* %i.addr
  %cond = icmp slt i32 %i, i32 10
  condbr bool %cond, label %loop_body, label %loop_exit

loop_body:
  // 循环体
  %i_next = add i32 %i, i32 1
  store i32 %i_next, i32* %i.addr
  br label %loop_header

loop_exit:
  ret void
```

### break 和 continue

```
entry:
  br label %loop_header

loop_header:
  %cond = call bool @check_condition()
  condbr bool %cond, label %loop_body, label %loop_exit

loop_body:
  %should_break = call bool @should_break()
  condbr bool %should_break, label %loop_exit, label %continue_check

continue_check:
  %should_continue = call bool @should_continue()
  condbr bool %should_continue, label %loop_header, label %loop_body

loop_exit:
  ret void
```

---

## 函数调用约定

### 函数定义

```
func @function_name(<param_type> %param_name, ...) -> <return_type> {
entry:
  // 函数体
}
```

**示例**：
```
func @add(i32 %a, i32 %b) -> i32 {
entry:
  %a.addr = alloca i32
  %b.addr = alloca i32
  store i32 %a, i32* %a.addr
  store i32 %b, i32* %b.addr
  
  %a_val = load i32, i32* %a.addr
  %b_val = load i32, i32* %b.addr
  %result = add i32 %a_val, %b_val
  
  ret i32 %result
}
```

### 内置函数

内置函数在模块初始化阶段注册：

```
func @print(i32 %value) -> void
func @panic() -> never
func @malloc(i32 %size) -> i8*
func @free(i8* %ptr) -> void
```

---

## 内存模型

### 栈分配

局部变量使用`alloca`在栈上分配：

```
%local_var = alloca i32
%local_array = alloca [i32; 10]
%local_struct = alloca struct.Point
```

### 堆分配

通过内置函数进行堆分配：

```
%size = mul i32 10, i32 4  // 10个i32的大小
%ptr = call i8* @malloc(i32 %size)
%array_ptr = cast i8* %ptr to [i32; 10]*
```

### 内存布局

#### 结构体布局
```
struct Point {
  x: i32,  // 偏移 0
  y: i32   // 偏移 4
}

// 访问字段
%pt = alloca struct.Point
%x_ptr = gep struct.Point* %pt, i32 0, i32 0  // x字段地址
%y_ptr = gep struct.Point* %pt, i32 0, i32 1  // y字段地址
```

#### 数组布局
```
// 数组元素访问
%arr = alloca [i32; 10]
%elem_ptr = gep [i32; 10]* %arr, i32 3  // 第3个元素地址
%elem = load i32, i32* %elem_ptr        // 加载元素值
```

---

## IR代码示例

### 简单函数

#### 源代码
```rust
fn add(a: i32, b: i32) -> i32 {
    return a + b;
}
```

#### IR代码
```
func @add(i32 %a, i32 %b) -> i32 {
entry:
  %a.addr = alloca i32
  %b.addr = alloca i32
  store i32 %a, i32* %a.addr
  store i32 %b, i32* %b.addr
  
  %a_val = load i32, i32* %a.addr
  %b_val = load i32, i32* %b.addr
  %result = add i32 %a_val, %b_val
  
  ret i32 %result
}
```

### 条件分支

#### 源代码
```rust
fn abs(x: i32) -> i32 {
    if x >= 0 {
        return x;
    } else {
        return -x;
    }
}
```

#### IR代码
```
func @abs(i32 %x) -> i32 {
entry:
  %x.addr = alloca i32
  store i32 %x, i32* %x.addr
  
  %x_val = load i32, i32* %x.addr
  %is_positive = icmp sge i32 %x_val, i32 0
  condbr bool %is_positive, label %then, label %else

then:
  %then_val = load i32, i32* %x.addr
  br label %merge

else:
  %else_val = load i32, i32* %x.addr
  %neg_val = neg i32 %else_val
  br label %merge

merge:
  %result = phi i32 [%then_val, %then], [%neg_val, %else]
  ret i32 %result
}
```

### 循环

#### 源代码
```rust
fn sum(n: i32) -> i32 {
    let mut i = 0;
    let mut total = 0;
    while i < n {
        total = total + i;
        i = i + 1;
    }
    return total;
}
```

#### IR代码
```
func @sum(i32 %n) -> i32 {
entry:
  %n.addr = alloca i32
  %i.addr = alloca i32
  %total.addr = alloca i32
  
  store i32 %n, i32* %n.addr
  store i32 0, i32* %i.addr
  store i32 0, i32* %total.addr
  
  br label %loop_header

loop_header:
  %i_val = load i32, i32* %i.addr
  %n_val = load i32, i32* %n.addr
  %cond = icmp slt i32 %i_val, %n_val
  condbr bool %cond, label %loop_body, label %loop_exit

loop_body:
  %i_val2 = load i32, i32* %i.addr
  %total_val = load i32, i32* %total.addr
  %new_total = add i32 %total_val, %i_val2
  store i32 %new_total, i32* %total.addr
  
  %i_val3 = load i32, i32* %i.addr
  %new_i = add i32 %i_val3, i32 1
  store i32 %new_i, i32* %i.addr
  
  br label %loop_header

loop_exit:
  %result = load i32, i32* %total.addr
  ret i32 %result
}
```

### 结构体操作

#### 源代码
```rust
struct Point {
    x: i32,
    y: i32
}

fn distance(p1: Point, p2: Point) -> i32 {
    let dx = p2.x - p1.x;
    let dy = p2.y - p1.y;
    return dx * dx + dy * dy;
}
```

#### IR代码
```
func @distance(struct.Point %p1, struct.Point %p2) -> i32 {
entry:
  %p1.addr = alloca struct.Point
  %p2.addr = alloca struct.Point
  %dx.addr = alloca i32
  %dy.addr = alloca i32
  
  store struct.Point %p1, struct.Point* %p1.addr
  store struct.Point %p2, struct.Point* %p2.addr
  
  // p2.x - p1.x
  %p2_x_ptr = gep struct.Point* %p2.addr, i32 0, i32 0
  %p2_x = load i32, i32* %p2_x_ptr
  %p1_x_ptr = gep struct.Point* %p1.addr, i32 0, i32 0
  %p1_x = load i32, i32* %p1_x_ptr
  %dx = sub i32 %p2_x, %p1_x
  store i32 %dx, i32* %dx.addr
  
  // p2.y - p1.y
  %p2_y_ptr = gep struct.Point* %p2.addr, i32 0, i32 1
  %p2_y = load i32, i32* %p2_y_ptr
  %p1_y_ptr = gep struct.Point* %p1.addr, i32 0, i32 1
  %p1_y = load i32, i32* %p1_y_ptr
  %dy = sub i32 %p2_y, %p1_y
  store i32 %dy, i32* %dy.addr
  
  // dx * dx + dy * dy
  %dx_val = load i32, i32* %dx.addr
  %dx_sq = mul i32 %dx_val, %dx_val
  %dy_val = load i32, i32* %dy.addr
  %dy_sq = mul i32 %dy_val, %dy_val
  %result = add i32 %dx_sq, %dy_sq
  
  ret i32 %result
}
```

---

## AST到IR的映射

### 表达式映射

| AST节点 | IR指令序列 |
|---------|-----------|
| 字面量 | `IRConst` |
| 变量引用 | `load` |
| 赋值 | `store` |
| 二元算术 | `add/sub/mul/div/rem` |
| 比较 | `icmp` |
| 逻辑运算 | `and/or/not` |
| 类型转换 | `cast` |
| 函数调用 | `call` |
| 字段访问 | `gep` + `load` |
| 数组索引 | `gep` + `load` |
| 借用表达式 | 返回地址 |
| 解引用 | `load` |

### 语句映射

| AST语句 | IR结构 |
|---------|--------|
| let声明 | `alloca` + `store` |
| 表达式语句 | 表达式指令序列 |
| if表达式 | 条件分支 + phi |
| loop表达式 | 循环结构 |
| return | `ret` |
| break | 跳转到循环出口 |
| continue | 跳转到循环头部 |

### 控制流映射

| Rust控制流 | IR控制流 |
|------------|----------|
| if-else | `condbr` + 多个基本块 |
| match | `condbr` 或 `switch` (未来) |
| loop | 基本块循环结构 |
| while | 条件检查 + 循环结构 |
| for | 转换为while结构 |
| break | 跳转到循环出口块 |
| continue | 跳转到循环头部块 |

---

## 附录

### 保留字和特殊符号

- `%` - 临时值和局部标签前缀
- `@` - 全局符号前缀
- `label` - 基本块标签标识符
- `func` - 函数定义关键字
- `module` - 模块定义关键字

### 类型后缀

- `i` - 有符号整数 (i1, i8, i16, i32, i64)
- `u` - 无符号整数 (u8, u16, u32, u64)
- `bool` - 布尔类型
- `char` - 字符类型
- `void` - 无返回值类型
- `unit` - 单元类型
- `never` - 永不返回类型

### 比较谓词

- `eq` - 等于
- `ne` - 不等于
- `sgt/sge` - 有符号大于/大于等于
- `slt/sle` - 有符号小于/小于等于
- `ugt/uge` - 无符号大于/大于等于
- `ult/ule` - 无符号小于/小于等于

---

*本文档描述了RCompiler IR的语法规范，随着编译器的发展可能会进行更新和扩展。*