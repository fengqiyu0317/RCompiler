# ImplNode 的 visit 函数中检查 trait 是否已定义的实现方案

## 问题分析

在 `docs/design/semantic_check_implementation.md` 文件中的 `ImplNode` 的 `visit` 函数（第1347-1404行）没有检查 `node.trait` 引用的 trait 是否已经被定义过。

当前的实现：
```java
// Process impl declaration
@Override
public void visit(ImplNode node) {
    // Process type name
    visit(node.typeName);
    
    // Get the type symbol for this impl
    String typeName = null;
    if (node.typeName instanceof TypePathExprNode) {
        TypePathExprNode typePath = (TypePathExprNode) node.typeName;
        if (typePath.path.LSeg != null && typePath.path.RSeg == null) {
            typeName = getSegmentText(typePath.path.LSeg);
        }
    }
    
    Symbol typeSymbol = null;
    if (typeName != null) {
        typeSymbol = currentScope.lookupType(typeName);
    }
    
    // Process trait name (if any)
    if (node.trait != null) {
        visit(node.trait);
    }
    
    // Create impl block symbol
    // Enter impl scope for associated items
    // Impl blocks also create a new scope for their items
    enterScope();
    
    // Process associated items
    if (node.items != null) {
        for (AssoItemNode item : node.items) {
            visit(item);
            
            // Add to type's implSymbols
            if (item.function != null) {
                String functionName = item.function.name.name;
                Symbol functionSymbol = currentScope.lookupValueInCurrentScope(functionName);
                if (functionSymbol != null && typeSymbol != null) {
                    typeSymbol.addImplSymbol(functionSymbol);
                }
            } else if (item.constant != null) {
                String constName = item.constant.name.name;
                Symbol constSymbol = currentScope.lookupValueInCurrentScope(constName);
                if (constSymbol != null && typeSymbol != null) {
                    typeSymbol.addImplSymbol(constSymbol);
                }
            }
        }
    }
    
    // Exit impl scope
    exitScope();
}
```

问题在于第1367-1370行：
```java
// Process trait name (if any)
if (node.trait != null) {
    visit(node.trait);
}
```

这里只是简单地访问了 trait 节点，但没有检查 trait 是否已经被定义过。

## 解决方案

需要在处理 trait 名称时添加检查，确保引用的 trait 已经被定义，并简化整个 visit 函数。以下是修改后的实现：

```java
// Process impl declaration
@Override
public void visit(ImplNode node) {
    // Process type name
    visit(node.typeName);
    
    // Extract type name and get type symbol
    String typeName = null;
    Symbol typeSymbol = null;
    
    // Only TypePathExprNode can be used as impl target
    if (node.typeName instanceof TypePathExprNode) {
        TypePathExprNode typePath = (TypePathExprNode) node.typeName;
        if (typePath.path.LSeg != null && typePath.path.RSeg == null) {
            typeName = getSegmentText(typePath.path.LSeg);
        }
    }
    
    // Check if type name is valid and defined
    if (typeName == null || typeName.isEmpty()) {
        addError(ErrorType.UNDECLARED_TYPE_IDENTIFIER,
                "Impl block requires a valid type name",
                node.typeName);
    } else {
        typeSymbol = currentScope.lookupType(typeName);
        if (typeSymbol == null) {
            addError(ErrorType.UNDECLARED_TYPE_IDENTIFIER,
                    "Impl target type '" + typeName + "' is not defined",
                    node.typeName);
        }
    }
    
    // Process trait name (if any)
    if (node.trait != null) {
        String traitName = node.trait.name;
        
        // Check if trait is defined
        Symbol traitSymbol = currentScope.lookupType(traitName);
        if (traitSymbol == null) {
            addError(ErrorType.UNDECLARED_TYPE_IDENTIFIER,
                    "Trait '" + traitName + "' is not defined",
                    node.trait);
        } else if (traitSymbol.getKind() != SymbolKind.TRAIT) {
            addError(ErrorType.NAMESPACE_VIOLATION,
                    "'" + traitName + "' is not a trait",
                    node.trait);
        }
        
        visit(node.trait);
    }
    
    // Enter impl scope for associated items
    enterScope();
    
    // Process associated items
    if (node.items != null) {
        for (AssoItemNode item : node.items) {
            visit(item);
            
            // Add to type's implSymbols
            if (item.function != null) {
                String functionName = item.function.name.name;
                Symbol functionSymbol = currentScope.lookupValueInCurrentScope(functionName);
                if (functionSymbol != null && typeSymbol != null) {
                    typeSymbol.addImplSymbol(functionSymbol);
                }
            } else if (item.constant != null) {
                String constName = item.constant.name.name;
                Symbol constSymbol = currentScope.lookupValueInCurrentScope(constName);
                if (constSymbol != null && typeSymbol != null) {
                    typeSymbol.addImplSymbol(constSymbol);
                }
            }
        }
    }
    
    // Exit impl scope
    exitScope();
}
```

## 修改说明

1. **添加 trait 定义检查**：
   - 获取 trait 名称：`String traitName = node.trait.name;`
   - 在类型命名空间中查找 trait：`Symbol traitSymbol = currentScope.lookupType(traitName);`
   - 检查 trait 是否存在：如果 `traitSymbol == null`，则报错
   - 检查符号是否为 trait：如果 `traitSymbol.getKind() != SymbolKind.TRAIT`，则报错

2. **简化类型名称提取**：
   - 直接在 visit 函数中处理类型名称提取
   - 只有 TypePathExprNode 可以作为 impl 目标
   - 其他类型表达式（TypeRefExprNode、TypeArrayExprNode、TypeUnitExprNode）自动导致 typeName 为 null

3. **添加类型名称有效性检查**：
   - 检查 typeName 是否为 null 或空字符串
   - 如果无效，报告错误："Impl block requires a valid type name"

4. **错误处理**：
   - 使用 `addError` 方法报告错误
   - 错误类型为 `UNDECLARED_TYPE_IDENTIFIER`（未声明的类型标识符）
   - 错误类型为 `NAMESPACE_VIOLATION`（命名空间违规）

5. **简化整体结构**：
   - 移除了单独的 extractTypeName 函数
   - 将类型名称提取逻辑直接集成到 visit 函数中
   - 简化了注释和代码结构

## 测试用例

```rust
// 测试用例 1: 正常的 trait 实现
trait MyTrait {
    fn method(&self);
}

struct MyStruct;

impl MyTrait for MyStruct {
    fn method(&self) {
        // 实现
    }
}

// 测试用例 2: 实现未定义的 trait（应该报错）
struct MyStruct2;

impl UndefinedTrait for MyStruct2 {
    fn method(&self) {
        // 实现 - 应该报错：Trait 'UndefinedTrait' is not defined
    }
}

// 测试用例 3: 为非 trait 类型实现（应该报错）
struct MyStruct3;

impl MyStruct3 for MyStruct3 {
    fn method(&self) {
        // 实现 - 应该报错：'MyStruct3' is not a trait
    }
}

// 测试用例 4: 无效的类型名称（应该报错）
impl SomeTrait for () {
    fn method(&self) {
        // 实现 - 应该报错：Impl block requires a valid type name
    }
}

// 测试用例 5: 数组类型作为 impl 目标（应该报错）
impl SomeTrait for [i32] {
    fn method(&self) {
        // 实现 - 应该报错：Impl block requires a valid type name
    }
}

// 测试用例 6: 引用类型作为 impl 目标（应该报错）
impl SomeTrait for &MyStruct {
    fn method(&self) {
        // 实现 - 应该报错：Impl block requires a valid type name
    }
}
```

## 总结

这个修改方案在 `ImplNode` 的 `visit` 函数中添加了对 trait 是否已定义的检查，同时简化了整个函数的结构。通过将类型名称提取逻辑直接集成到 visit 函数中，移除了单独的辅助函数，使代码更加简洁明了。这样可以提前发现并报告错误，提高编译器的错误检测能力。