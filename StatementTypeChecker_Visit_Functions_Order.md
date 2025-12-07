# StatementTypeChecker Visit函数顺序整理

以下是StatementTypeChecker类中各个visit函数按照它们在文件中出现的顺序排列：

## 1. visit(ItemNode node) - 第36行
- **功能**: 访问抽象基类ItemNode
- **实现**: 抛出异常，因为不应该直接访问抽象基类

## 2. visit(FunctionNode node) - 第44行
- **功能**: 访问函数节点
- **实现**: 
  - 保存之前的Self类型
  - 进入函数上下文
  - 设置当前Self类型（如果是方法）
  - 访问函数体
  - 检查返回类型兼容性
  - 恢复之前的Self类型

## 3. visit(LetStmtNode node) - 第165行
- **功能**: 访问let语句节点
- **实现**: 
  - 进行mutability检查
  - 访问值表达式（如果存在）
  - 检查类型兼容性（如果有显式类型）

## 4. visit(ExprStmtNode node) - 第203行
- **功能**: 访问表达式语句节点
- **实现**: 
  - 访问表达式
  - 根据是否有分号设置语句类型（有分号为unit类型，无分号为表达式类型）

## 5. visit(IdentifierNode node) - 第245行
- **功能**: 访问标识符节点
- **实现**: 
  - 获取符号
  - 从符号中提取类型
  - 将类型存储回符号中

## 6. visit(TypePathExprNode node) - 第270行
- **功能**: 访问类型路径表达式节点
- **实现**: 空实现，由TypeExtractor处理

## 7. visit(AssoItemNode node) - 第277行
- **功能**: 访问关联项节点
- **实现**: 访问函数或常量（其中一个不为null）

## 8. visit(ImplNode node) - 第287行
- **功能**: 访问impl节点
- **实现**: 
  - 设置Self类型上下文
  - 访问impl块中的项
  - 恢复之前的Self类型

## 9. visit(ConstItemNode node) - 第328行
- **功能**: 访问常量项节点
- **实现**: 
  - 访问值表达式
  - 检查类型兼容性
  - 确保常量有显式类型

## 10. visit(StructNode node) - 第375行
- **功能**: 访问结构体节点
- **实现**: 空实现，结构体定义不需要类型检查

## 11. visit(EnumNode node) - 第381行
- **功能**: 访问枚举节点
- **实现**: 空实现，枚举定义不需要类型检查

## 12. visit(FieldNode node) - 第387行
- **功能**: 访问字段节点
- **实现**: 抛出异常，不应该直接访问FieldNode

## 13. visit(TraitNode node) - 第395行
- **功能**: 访问trait节点
- **实现**: 空实现，trait定义不需要类型检查

## 14. visit(SelfParaNode node) - 第400行
- **功能**: 访问self参数节点
- **实现**: 空实现，self参数类型由impl块确定

## 15. visit(ParameterNode node) - 第406行
- **功能**: 访问参数节点
- **实现**: 空实现，参数类型已在函数类型创建时处理

## 16. visit(PatternNode node) - 第411行
- **功能**: 访问模式节点
- **实现**: 空实现，PatternNode是抽象类

## 17. visit(IdPatNode node) - 第417行
- **功能**: 访问标识符模式节点
- **实现**: 空实现，标识符模式类型由上下文确定

## 18. visit(WildPatNode node) - 第423行
- **功能**: 访问通配符模式节点
- **实现**: 空实现，通配符模式匹配任何类型

## 19. visit(RefPatNode node) - 第429行
- **功能**: 访问引用模式节点
- **实现**: 访问内部模式（如果存在）

## 20. visit(TypeRefExprNode node) - 第438行
- **功能**: 访问类型引用表达式节点
- **实现**: 空实现，由TypeExtractor处理

## 21. visit(TypeArrayExprNode node) - 第444行
- **功能**: 访问类型数组表达式节点
- **实现**: 访问元素类型和大小（如果存在）

## 22. visit(TypeUnitExprNode node) - 第456行
- **功能**: 访问类型单元表达式节点
- **实现**: 空实现，由TypeExtractor处理

## 23. visit(BuiltinFunctionNode node) - 第462行
- **功能**: 访问内置函数节点
- **实现**: 调用父类的visit方法，由父FunctionNode的visit方法处理

## 总结

StatementTypeChecker中的visit函数按照从具体到抽象、从复杂到简单的顺序排列：
1. 首先是处理主要语句结构的visit函数（FunctionNode, LetStmtNode, ExprStmtNode等）
2. 然后是处理标识符和类型相关的visit函数
3. 最后是处理模式、类型表达式和更抽象节点的visit函数

这种排列方式符合代码的逻辑结构，使得阅读和理解代码更加容易。