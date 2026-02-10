# test_misc 栈溢出（大结构体按值返回）修复方针

目标：避免 REIMU 运行期出现 “Store out of bound” 的栈越界，核心手段是对 **返回大结构体** 的函数启用 sret（由调用者提供返回地址）。

## 1. 识别需要 sret 的返回类型
- 当函数返回 `struct` 或大数组（IR 中为 `IRStructType` / `IRArrayType`）时走 sret。
- 其他标量类型保持原有 ABI。

## 2. IRGenerator 中新增 sret 状态
- `currentSretPtr` / `currentSretType`：当前函数的隐藏返回地址。
- `sretReturnTypes`：记录哪些函数使用 sret（函数名 → 返回类型）。

## 3. 生成函数签名时插入隐藏参数
在 `visit(FunctionNode)`：
- 计算 `useSret = needsSret(returnType)`。
- 若 `useSret`：
  - IRFunction 返回类型改为 `void`。
  - 在参数列表最前插入 `sret` 指针参数（指向返回结构体）。
  - 记录 `sretReturnTypes[funcName] = returnType`。

## 4. return 语句改为写回 sret
在 `visitReturn(ReturnExprNode)`：
- 有返回值且 `currentSretPtr != null` 时：
  - `store retVal -> currentSretPtr`
  - `ret void`
- 否则保持原逻辑。

## 5. 函数体结尾补 return 时同步处理
在 `visit(FunctionNode)` 的“函数体未终结时补 return”：
- 如果 `useSret`，先把 `bodyResult` 写入 `currentSretPtr` 再 `ret void`。

## 6. 调用 sret 函数时插入返回缓冲区
在 `visitCall` / `visitMethodCall`：
- 若被调函数在 `sretReturnTypes` 中：
  - 为返回值 `alloca` 一块临时地址（或目标地址）。
  - 把该地址作为第 0 个实参传入。
  - `call void` 后再 `load` 得到返回值。

## 7. let 初始化场景做“直写优化”
在 `visit(LetStmtNode)`：
- 如果初始化表达式是 sret 调用：
  - 直接把 `let` 的变量地址作为 sret 参数传入
  - **避免**先生成临时大对象再拷贝（关键优化）

## 8. 验证步骤
1) 重新跑 `test16–test20`（含 `.in`）确认不再出现 `Store out of bound`。
2) 对比 `.ans` 与输出值是否一致。
3) 关注 sret 改动是否影响普通标量返回函数。

## 9. 回滚点与风险
- sret 只影响 struct/array 返回，不应改变标量函数行为。
- 若出现调用参数错位，优先检查：
  - sret 参数是否插入到参数列表首位
  - 调用处是否同步插入 sret 实参
  - 方法调用是否先插 sret 再 self
