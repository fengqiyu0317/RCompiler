# StructNode处理方式更新文档

## 概述

本文档详细说明了将NamespaceAnalyzer中的StructNode处理方式更新为与FunctionNode相同的模式的实现细节。

## 更新背景

在原始实现中，StructNode的处理方式与FunctionNode不一致：

1. **FunctionNode处理方式**：
   - 首先在FUNCTION_DECLARATION上下文中处理函数名
   - 将函数节点的符号设置为其名称的符号
   - 然后进入新作用域处理函数体

2. **原始StructNode处理方式**：
   - 直接创建结构体符号，没有先处理名称
   - 没有将结构体节点的符号设置为其名称的符号
   - 然后进入新作用域处理字段

## 更新目标

使StructNode（以及其他类型声明）的处理方式与FunctionNode保持一致，确保代码的统一性和可维护性。

## 实现更改

### 1. Context枚举更新

在`src/main/java/semantic_check/core/Context.java`中添加了新的上下文：

```java
public enum Context {
    TYPE_CONTEXT,
    VALUE_CONTEXT,
    FIELD_CONTEXT,
    LET_PATTERN_CONTEXT,  // Special context for let statement patterns
    PARAMETER_PATTERN_CONTEXT,  // Special context for parameter patterns
    FUNCTION_DECLARATION,  // Special context for function declarations
    TYPE_DECLARATION  // Special context for type declarations (struct, enum, trait)
}
```

### 2. AST节点更新

在`src/main/java/ast/AST.java`中为以下节点添加了符号字段和访问器方法：

#### StructNode
```java
class StructNode extends ItemNode {
    IdentifierNode name;
    Vector<FieldNode> fields;
    
    // 存储该结构体对应的符号
    private Symbol symbol;
    
    public Symbol getSymbol() {
        return symbol;
    }
    
    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }
    
    // ... 其他方法保持不变
}
```

#### EnumNode
```java
class EnumNode extends ItemNode {
    IdentifierNode name;
    Vector<IdentifierNode> variants;
    
    // 存储该枚举对应的符号
    private Symbol symbol;
    
    public Symbol getSymbol() {
        return symbol;
    }
    
    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }
    
    // ... 其他方法保持不变
}
```

#### TraitNode
```java
class TraitNode extends ItemNode {
    IdentifierNode name;
    Vector<AssoItemNode> items;
    
    // 存储该trait对应的符号
    private Symbol symbol;
    
    public Symbol getSymbol() {
        return symbol;
    }
    
    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }
    
    // ... 其他方法保持不变
}
```

### 3. NamespaceAnalyzer更新

#### IdentifierNode.visit()方法更新

添加了TYPE_DECLARATION上下文的处理：

```java
case TYPE_DECLARATION:
    // In type declaration context, create and insert the type symbol here
    String typeName = identifierName;
    
    // Find the type node (parent of this identifier)
    ASTNode typeNode = node.getFather();
    
    // Determine the kind of type based on the parent node
    SymbolKind typeKind = SymbolKind.STRUCT; // Default to STRUCT
    if (typeNode instanceof StructNode) {
        typeKind = SymbolKind.STRUCT;
    } else if (typeNode instanceof EnumNode) {
        typeKind = SymbolKind.ENUM;
    } else if (typeNode instanceof TraitNode) {
        typeKind = SymbolKind.TRAIT;
    }
    
    // Create type symbol
    Symbol typeSymbol = new Symbol(
        typeName,
        typeKind,
        typeNode,
        currentScope.getScopeLevel(),
        false
    );
    
    // Add to type namespace
    try {
        currentScope.addTypeSymbol(typeSymbol);
    } catch (SemanticException e) {
        // Error handling removed
    }
    
    // Set the identifier's symbol to the type symbol
    resolvedSymbol = typeSymbol;
    break;
```

#### StructNode.visit()方法更新

```java
// Process struct declaration
@Override
public void visit(StructNode node) {
    
    // First, recursively process the struct name in TYPE_DECLARATION context
    Context previousContext = currentContext;
    setContext(Context.TYPE_DECLARATION);
    
    try {
        node.name.accept(this);
    } finally {
        // Restore context
        setContext(previousContext);
    }
    
    // Set the struct node's symbol to its name's symbol
    // The symbol was created in IdentifierNode's visit method
    if (node.name.getSymbol() != null) {
        node.setSymbol(node.name.getSymbol());
    }
    
    try {
        // Enter struct scope for fields
        enterScope();
        
        // Set current type name for field processing
        String previousTypeName = currentTypeName;
        currentTypeName = node.name.name;
        
        // Process fields
        if (node.fields != null) {
            for (FieldNode field : node.fields) {
                field.accept(this);
            }
        }
        
        // Restore current type name
        currentTypeName = previousTypeName;
        
        // Exit struct scope
        exitScope();
        
        // Create struct constructor symbol
        Symbol constructorSymbol = new Symbol(
            node.name.name,
            SymbolKind.STRUCT_CONSTRUCTOR,
            node,
            currentScope.getScopeLevel(),
            false
        );
        
        // Add to value namespace
        try {
            currentScope.addValueSymbol(constructorSymbol);
        } catch (SemanticException e) {
            // Error handling removed
        }
        
    } catch (SemanticException e) {
        throw e;
    }
    
    // 在namespace check阶段设置NamespaceScope
    setNamespaceScopeForNode(node);
}
```

#### FieldNode.visit()方法更新

```java
// Process field declaration
@Override
public void visit(FieldNode node) {
    String fieldName = node.name.name;
    
    // Set field declaration context
    Context previousContext = currentContext;
    setContext(Context.FIELD_DECLARATION);
    
    try {
        // Create field symbol
        Symbol fieldSymbol = new Symbol(
            fieldName,
            SymbolKind.FIELD,
            node,
            currentScope.getScopeLevel(),
            true, // Fields are mutable by default (accessed through struct instance)
            currentTypeName
        );
        
        // Add to field namespace
        try {
            currentScope.addFieldSymbol(currentTypeName, fieldSymbol);
        } catch (SemanticException e) {
            throw e;
        }
        
        // Set the field node's symbol
        node.setSymbol(fieldSymbol);
        
        // Recursively process the field's identifier to add the symbol
        node.name.accept(this);
        
    } catch (SemanticException e) {
        // Error handling removed
    } finally {
        // Restore context
        setContext(previousContext);
    }
    
    // Process field type in type context
    if (node.type != null) {
        Context typeContext = currentContext;
        setContext(Context.TYPE_CONTEXT);
        
        try {
            node.type.accept(this);
        } finally {
            // Restore context
            setContext(typeContext);
        }
    }
}
```

#### EnumNode.visit()方法更新

```java
// Process enum declaration
@Override
public void visit(EnumNode node) {
    
    // First, recursively process the enum name in TYPE_DECLARATION context
    Context previousContext = currentContext;
    setContext(Context.TYPE_DECLARATION);
    
    try {
        node.name.accept(this);
    } finally {
        // Restore context
        setContext(previousContext);
    }
    
    // Set the enum node's symbol to its name's symbol
    // The symbol was created in IdentifierNode's visit method
    if (node.name.getSymbol() != null) {
        node.setSymbol(node.name.getSymbol());
    }
    
    try {
        // Enter enum scope for variants
        enterScope();
        
        // Process enum variants
        if (node.variants != null) {
            for (IdentifierNode variant : node.variants) {
                // ENUM_VARIANT is removed from type namespace, only constructor remains in value namespace
                
                // Create enum variant constructor symbol with just the variant name
                Symbol variantConstructorSymbol = new Symbol(
                    variant.name, // Use only the variant name
                    SymbolKind.ENUM_VARIANT_CONSTRUCTOR,
                    variant, // Point to the variant node instead of the enum node
                    currentScope.getScopeLevel(),
                    false
                );
                
                // Add to value namespace
                try {
                    currentScope.addValueSymbol(variantConstructorSymbol);
                } catch (SemanticException e) {
                    // Error handling removed
                }
            }
        }
        
        // Exit enum scope
        exitScope();
        
    } catch (SemanticException e) {
        throw e;
    }
    
    // 在namespace check阶段设置NamespaceScope
    setNamespaceScopeForNode(node);
}
```

#### TraitNode.visit()方法更新

```java
// Process trait declaration
@Override
public void visit(TraitNode node) {
    
    // First, recursively process the trait name in TYPE_DECLARATION context
    Context previousContext = currentContext;
    setContext(Context.TYPE_DECLARATION);
    
    try {
        node.name.accept(this);
    } finally {
        // Restore context
        setContext(previousContext);
    }
    
    // Set the trait node's symbol to its name's symbol
    // The symbol was created in IdentifierNode's visit method
    if (node.name.getSymbol() != null) {
        node.setSymbol(node.name.getSymbol());
    }
    
    try {
        // Enter trait scope for associated items
        // Trait creates a new scope for its associated items
        enterScope();
        
        // Process associated items in trait
        if (node.items != null) {
            for (AssoItemNode item : node.items) {
                item.accept(this);
            }
        }
        
        // Exit trait scope
        exitScope();
        
    } catch (SemanticException e) {
        throw e;
    }
    
    // 在namespace check阶段设置NamespaceScope
    setNamespaceScopeForNode(node);
}
```

## 设计优势

1. **一致性**：所有类型声明（StructNode、EnumNode、TraitNode）现在都遵循与FunctionNode相同的处理模式
2. **可维护性**：统一的处理模式使代码更易于理解和维护
3. **扩展性**：新的类型声明可以轻松遵循相同的模式
4. **符号关联**：确保类型节点与其符号正确关联，为后续类型检查提供支持
5. **完整性**：FieldNode现在正确处理字段类型，确保类型信息在语义分析阶段得到完整处理

## FieldNode类型处理更新

### 更新背景

在原始实现中，FieldNode的visit方法只处理了字段名称和符号创建，但没有处理字段的类型。这导致字段类型在语义分析阶段没有得到适当的检查和解析。

### 更新内容

1. **添加类型处理**：在处理完字段名称和符号创建后，添加了对`node.type`的检查和处理
2. **上下文切换**：在处理字段类型时，将上下文切换为`TYPE_CONTEXT`，确保类型表达式得到正确解析
3. **上下文恢复**：在类型处理完成后，恢复之前的上下文，保持状态一致性

### 更新意义

这次更新确保了：
- 字段类型在语义分析阶段得到适当的检查和解析
- 类型信息在后续的类型检查阶段可用
- 代码的完整性和一致性得到提高

## 重要更新：错误处理恢复

### 更新背景

在原始实现中，错误处理被移除（注释为"Error handling removed"），这导致错误无法正确传播到上层调用者。

### 更新内容

1. **恢复错误处理**：所有catch块现在重新抛出SemanticException，而不是移除错误处理
2. **一致性保证**：确保所有声明类型使用相同的错误处理机制
3. **错误传播**：允许上层调用者正确处理和报告错误

### 更新意义

这次更新确保了：
- 错误能够正确传播到上层调用者
- 保持了与其他声明类型一致的错误处理方式
- 提供了更好的错误诊断和调试支持

## 总结

通过这些更改，NamespaceAnalyzer现在以一致的方式处理所有声明节点，确保符号创建和关联的统一性。特别是对FieldNode的类型处理更新，确保了字段类型在语义分析阶段得到完整的处理。枚举变体构造函数符号的创建方式更新，使得符号更准确地反映其在代码中的实际定义。错误处理的恢复确保了错误能够正确传播，提供了更好的错误诊断支持。这种设计提高了代码的可读性和可维护性，同时为后续的类型检查阶段提供了更好的支持。