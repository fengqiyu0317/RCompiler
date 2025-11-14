# 路径表达式(PathExprNode)的完整处理逻辑

## 问题分析

当前的PathExprNode实现过于简化，只是简单地处理两个段，没有真正的路径解析逻辑。需要实现：

1. **上下文感知的解析**：根据当前上下文在正确的命名空间中查找
2. **双段路径处理**：处理带"::"的两段路径
3. **适当的错误处理**：报告路径解析失败的情况
4. **命名空间区分**：正确区分类型路径和值路径

## 实现方案

### 1. 路径结构分类

PathExprNode的结构可以分为两种情况：
- **单段路径**：只有LSeg，如`my_function`、`MyStruct`、`self`
- **双段路径**：有LSeg和RSeg，如`Trait::method`、`MyStruct::associated_function`

本文档只关注**双段路径**（带"::"的路径）的处理。

### 2. 路径类型判断

现在所有路径都是关联路径（带"::"），不需要路径类型判断。

### 3. 完整路径构建

```java
// 获取路径段文本的辅助方法
private String getSegmentText(PathExprSegNode segment) {
    if (segment.patternType == patternSeg_t.IDENT) {
        return segment.name.name;
    } else if (segment.patternType == patternSeg_t.SELF) {
        return "self";
    } else if (segment.patternType == patternSeg_t.SELF_TYPE) {
        return "Self";
    }
    return "";
}
```

### 4. 路径解析策略

```java
// 解析路径（只处理关联路径）
private Symbol resolvePath(PathExprNode node) {
    String leftSegment = getSegmentText(node.LSeg);
    String rightSegment = getSegmentText(node.RSeg);
    
    // 查找基础类型
    Symbol baseType = currentScope.lookupType(leftSegment);
    if (baseType == null) {
        addError(ErrorType.UNDECLARED_TYPE_IDENTIFIER,
                "Undeclared type: " + leftSegment, node);
        return null;
    }
    
    // 解析关联项
    return resolveAssociatedItem(baseType, rightSegment, node);
}
```

### 5. 具体的路径解析方法

```java
// 解析关联项的通用方法
private Symbol resolveAssociatedItem(Symbol baseType, String itemName, PathExprNode node) {
    // 根据baseType的具体类型进行不同的处理
    switch (baseType.getKind()) {
        case STRUCT:
            return resolveStructAssociatedItem(baseType, itemName, node);
        case ENUM:
            return resolveEnumAssociatedItem(baseType, itemName, node);
        case TRAIT:
            return resolveTraitAssociatedItem(baseType, itemName, node);
        default:
            addError(ErrorType.INVALID_ASSOCIATED_ACCESS,
                    "Type " + baseType.getName() + " does not have associated items", node);
            return null;
    }
}
```

### 6. 结构体的关联项处理

```java
// 解析结构体的关联项
private Symbol resolveStructAssociatedItem(Symbol structType, String itemName, PathExprNode node) {
    // 已知structType是一个struct类型，需要在该struct类型底下查找关联项
    
    // 1. 在struct的impl块中查找关联函数
    Symbol associatedFunction = findStructAssociatedFunction(structType, itemName);
    if (associatedFunction != null) {
        return associatedFunction;
    }
    
    // 2. 在struct的impl块中查找关联常量
    Symbol associatedConst = findStructAssociatedConst(structType, itemName);
    if (associatedConst != null) {
        return associatedConst;
    }
    
    // 3. 检查是否是构造函数（struct名称本身）
    if (itemName.equals(structType.getName())) {
        // 返回struct的构造函数
        return structType;
    }
    
    // 未找到
    addError(ErrorType.UNDECLARED_ASSOCIATED_ITEM,
            "Struct " + structType.getName() + " does not have associated item: " + itemName, node);
    return null;
}

// 在struct的impl块中查找关联函数
private Symbol findStructAssociatedFunction(Symbol structType, String functionName) {
    // 遍历该struct的所有impl块
    for (Symbol implBlock : structType.getImplBlocks()) {
        // 在impl块的作用域中查找函数
        Symbol function = implBlock.getScope().lookupValue(functionName);
        if (function != null && function.getKind() == SymbolKind.FUNCTION) {
            return function;
        }
    }
    return null;
}

// 在struct的impl块中查找关联常量
private Symbol findStructAssociatedConst(Symbol structType, String constName) {
    // 遍历该struct的所有impl块
    for (Symbol implBlock : structType.getImplBlocks()) {
        // 在impl块的作用域中查找常量
        Symbol constSymbol = implBlock.getScope().lookupValue(constName);
        if (constSymbol != null && constSymbol.getKind() == SymbolKind.CONST) {
            return constSymbol;
        }
    }
    return null;
}
```

### 7. 枚举的关联项处理

```java
// 解析枚举的关联项
private Symbol resolveEnumAssociatedItem(Symbol enumType, String itemName, PathExprNode node) {
    // 枚举只有变体，没有关联常量和关联函数
    
    // 在枚举类型的作用域中查找枚举变体（枚举变体在值命名空间中）
    Symbol enumVariant = enumType.getScope().lookupValue(itemName);
    if (enumVariant != null && enumVariant.getKind() == SymbolKind.ENUM_VARIANT_CONSTRUCTOR) {
        return enumVariant;
    }
    
    // 未找到
    addError(ErrorType.UNDECLARED_ASSOCIATED_ITEM,
            "Enum " + enumType.getName() + " does not have variant: " + itemName, node);
    return null;
}
```

### 8. Trait的关联项处理

```java
// 解析Trait的关联项
private Symbol resolveTraitAssociatedItem(Symbol traitType, String itemName, PathExprNode node) {
    // Trait中没有关联类型，只有关联函数和关联常量
    
    // 1. 在Trait中查找关联函数
    Symbol associatedFunction = findTraitAssociatedFunction(traitType, itemName);
    if (associatedFunction != null) {
        return associatedFunction;
    }
    
    // 2. 在Trait中查找关联常量
    Symbol associatedConst = findTraitAssociatedConst(traitType, itemName);
    if (associatedConst != null) {
        return associatedConst;
    }
    
    // 未找到
    addError(ErrorType.UNDECLARED_ASSOCIATED_ITEM,
            "Trait " + traitType.getName() + " does not have associated item: " + itemName, node);
    return null;
}

// 在Trait中查找关联函数
private Symbol findTraitAssociatedFunction(Symbol traitType, String functionName) {
    // 在Trait的作用域中查找函数
    Symbol function = traitType.getScope().lookupValue(functionName);
    if (function != null && function.getKind() == SymbolKind.FUNCTION) {
        return function;
    }
    return null;
}

// 在Trait中查找关联常量
private Symbol findTraitAssociatedConst(Symbol traitType, String constName) {
    // 在Trait的作用域中查找常量
    Symbol constSymbol = traitType.getScope().lookupValue(constName);
    if (constSymbol != null && constSymbol.getKind() == SymbolKind.CONST) {
        return constSymbol;
    }
    return null;
}
```

### 9. 完整的visit方法

```java
// Process path expression
@Override
public void visit(PathExprNode node) {
    Symbol resolvedSymbol;
    PathType pathType;
    
    if (node.RSeg != null) {
        // 处理双段路径（带"::"）
        pathType = determinePathType(node);
        resolvedSymbol = resolvePath(node, pathType);
    } else {
        // 处理单段路径（不带"::"）
        String segmentText = getSegmentText(node.LSeg);
        
        // 根据上下文判断路径类型并直接解析
        if (currentContext == Context.TYPE_CONTEXT) {
            // 直接在当前作用域查找类型
            resolvedSymbol = currentScope.lookupType(segmentText);
            if (resolvedSymbol == null) {
                addError(ErrorType.UNDECLARED_TYPE_IDENTIFIER,
                        "Undeclared type: " + segmentText, node);
            }
        } else {
            // 直接在当前作用域查找值
            resolvedSymbol = currentScope.lookupValue(segmentText);
            if (resolvedSymbol == null) {
                addError(ErrorType.UNDECLARED_VALUE_IDENTIFIER,
                        "Undeclared value: " + segmentText, node);
            }
        }
    }
}
```

## 关键点

1. **双段路径处理**：只处理带"::"的两段路径，不处理单段路径
2. **类型区分**：根据baseType的具体类型（Struct、Enum、Trait）进行不同的关联项处理
3. **impl块查找**：在struct和enum的impl块中查找关联函数和关联常量
4. **构造函数处理**：特殊处理struct名称本身作为构造函数的情况
5. **枚举变体处理**：特殊处理枚举变体的查找
6. **错误处理**：为每种解析失败提供明确的错误信息

这种方法专注于处理Rust中带"::"的两段路径表达式，包括关联函数调用、关联常量访问、枚举变体等各种场景。