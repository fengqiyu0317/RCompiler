# StatementTypeChecker Visit函数重组计划

## 当前函数分析

根据功能，可以将当前的visit函数分为以下几类：

### 1. 顶层定义和容器节点
- `visit(ItemNode node)` - 抽象基类
- `visit(FunctionNode node)` - 函数定义
- `visit(ImplNode node)` - impl块
- `visit(AssoItemNode node)` - 关联项
- `visit(ConstItemNode node)` - 常量项
- `visit(StructNode node)` - 结构体定义
- `visit(EnumNode node)` - 枚举定义
- `visit(TraitNode node)` - trait定义
- `visit(BuiltinFunctionNode node)` - 内置函数

### 2. 语句节点
- `visit(LetStmtNode node)` - let语句
- `visit(ExprStmtNode node)` - 表达式语句

### 3. 表达式和标识符节点
- `visit(IdentifierNode node)` - 标识符
- `visit(TypePathExprNode node)` - 类型路径表达式
- `visit(TypeRefExprNode node)` - 类型引用表达式
- `visit(TypeArrayExprNode node)` - 类型数组表达式
- `visit(TypeUnitExprNode node)` - 类型单元表达式

### 4. 模式相关节点
- `visit(PatternNode node)` - 模式基类
- `visit(IdPatNode node)` - 标识符模式
- `visit(WildPatNode node)` - 通配符模式
- `visit(RefPatNode node)` - 引用模式

### 5. 参数和字段节点
- `visit(SelfParaNode node)` - self参数
- `visit(ParameterNode node)` - 参数
- `visit(FieldNode node)` - 字段

## 建议的新排序方案

按照从高层到底层、从具体到抽象的原则，建议按以下顺序重新排列：

### 1. 顶层定义和容器节点（按重要性排序）
1. `visit(ItemNode node)` - 抽象基类，放在最前
2. `visit(FunctionNode node)` - 最核心的函数定义
3. `visit(ImplNode node)` - impl块，包含方法
4. `visit(AssoItemNode node)` - impl块中的关联项
5. `visit(ConstItemNode node)` - 常量定义
6. `visit(StructNode node)` - 结构体定义
7. `visit(EnumNode node)` - 枚举定义
8. `visit(TraitNode node)` - trait定义
9. `visit(BuiltinFunctionNode node)` - 内置函数

### 2. 语句节点（按使用频率排序）
10. `visit(LetStmtNode node)` - 变量声明
11. `visit(ExprStmtNode node)` - 表达式语句

### 3. 表达式和标识符节点（按复杂度排序）
12. `visit(IdentifierNode node)` - 基础标识符
13. `visit(TypePathExprNode node)` - 类型路径
14. `visit(TypeRefExprNode node)` - 类型引用
15. `visit(TypeArrayExprNode node)` - 类型数组
16. `visit(TypeUnitExprNode node)` - 单元类型

### 4. 模式相关节点（从抽象到具体）
17. `visit(PatternNode node)` - 模式基类
18. `visit(IdPatNode node)` - 标识符模式
19. `visit(WildPatNode node)` - 通配符模式
20. `visit(RefPatNode node)` - 引用模式

### 5. 参数和字段节点（按作用域排序）
21. `visit(SelfParaNode node)` - self参数
22. `visit(ParameterNode node)` - 普通参数
23. `visit(FieldNode node)` - 字段

## 排序原则

1. **从高层到底层**：先处理顶层定义，再处理语句，然后是表达式，最后是细节
2. **从具体到抽象**：先处理具体实现，再处理抽象基类
3. **按重要性排序**：核心功能放在前面
4. **按关联性分组**：相关功能放在一起
5. **保持一致性**：同类节点按照相似的逻辑排序

这种排序方式使代码更容易理解和维护，因为：
- 相关功能聚集在一起
- 重要的核心功能放在前面
- 从整体到细节的逻辑流程清晰