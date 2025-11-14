# 命名空间语义检查实现方法

## 概述

本文档详细说明命名空间语义检查前四个阶段的具体实现方法，基于Rust Reference中的命名空间规范，为RCompiler提供完整的实现指导。

## 第一阶段：多命名空间符号表实现

### 1.1 符号种类定义

首先定义所有支持的符号种类，按照Rust命名空间规范分类：

```java
// Symbol kind enumeration
public enum SymbolKind {
    // Type namespace
    STRUCT("struct"),
    ENUM("enum"),
    TRAIT("trait"),
    BUILTIN_TYPE("builtin_type"),
    SELF_TYPE("self_type"),
    
    // Value namespace
    FUNCTION("function"),
    CONSTANT("constant"),
    STRUCT_CONSTRUCTOR("struct_constructor"),
    ENUM_VARIANT_CONSTRUCTOR("enum_variant_constructor"),
    SELF_CONSTRUCTOR("self_constructor"),
    PARAMETER("parameter"),
    LOCAL_VARIABLE("local_variable"),
    
    // Fields (no namespace)
    FIELD("field");
    
    private final String description;
    
    SymbolKind(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    // Determine which namespace the symbol kind belongs to
    public Namespace getNamespace() {
        switch (this) {
            case STRUCT:
            case ENUM:
            case TRAIT:
            case BUILTIN_TYPE:
            case SELF_TYPE:
                return Namespace.TYPE;
                
            case FUNCTION:
            case CONSTANT:
            case STRUCT_CONSTRUCTOR:
            case ENUM_VARIANT_CONSTRUCTOR:
            case SELF_CONSTRUCTOR:
            case PARAMETER:
            case LOCAL_VARIABLE:
                return Namespace.VALUE;
                
            case FIELD:
                return Namespace.FIELD;
                
                
            default:
                throw new IllegalArgumentException("Unknown symbol kind: " + this);
        }
    }
}

// Namespace enumeration
public enum Namespace {
    TYPE("Type Namespace"),
    VALUE("Value Namespace"),
    FIELD("Field Namespace");
    
    private final String description;
    
    Namespace(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
```

### 1.2 Symbol Table Entry Definition

```java
// Symbol table entry class
public class Symbol {
    private final String name;
    private final SymbolKind kind;
    private final ASTNode declaration;
    private final int scopeLevel;
    private final boolean isMutable;
    private final String typeName; // For fields, store the type name they belong to
    private final List<Symbol> implSymbols; // Symbols defined in impl blocks
    private NamespaceSymbolTable scope; // Scope associated with this symbol (for types, traits, impl blocks)
    
    public Symbol(String name, SymbolKind kind, ASTNode declaration, int scopeLevel, boolean isMutable) {
        this(name, kind, declaration, scopeLevel, isMutable, null, new ArrayList<>());
    }
    
    public Symbol(String name, SymbolKind kind, ASTNode declaration, int scopeLevel, boolean isMutable, String typeName) {
        this(name, kind, declaration, scopeLevel, isMutable, typeName, new ArrayList<>());
    }
    
    public Symbol(String name, SymbolKind kind, ASTNode declaration, int scopeLevel, boolean isMutable, String typeName, List<Symbol> implSymbols) {
        this.name = name;
        this.kind = kind;
        this.declaration = declaration;
        this.scopeLevel = scopeLevel;
        this.isMutable = isMutable;
        this.typeName = typeName;
        this.implSymbols = implSymbols;
        this.scope = null;
    }
    
    // Getter methods
    public String getName() { return name; }
    public SymbolKind getKind() { return kind; }
    public ASTNode getDeclaration() { return declaration; }
    public int getScopeLevel() { return scopeLevel; }
    public boolean isMutable() { return isMutable; }
    public String getTypeName() { return typeName; }
    public List<Symbol> getImplSymbols() { return implSymbols; }
    
    // Add impl symbol
    public void addImplSymbol(Symbol implSymbol) {
        implSymbols.add(implSymbol);
    }
    
    // Get scope (for types that have their own scope)
    public NamespaceSymbolTable getScope() {
        return scope;
    }
    
    // Set scope (for types that have their own scope)
    public void setScope(NamespaceSymbolTable scope) {
        this.scope = scope;
    }
    
    public Namespace getNamespace() {
        return kind.getNamespace();
    }
    
    @Override
    public String toString() {
        return String.format("Symbol{name='%s', kind=%s, scopeLevel=%d}",
                           name, kind, scopeLevel);
    }
}
```

### 1.3 Multi-Namespace Symbol Table Implementation

```java
// Multi-namespace symbol table class
public class NamespaceSymbolTable {
    // Type namespace
    private final Map<String, Symbol> typeNamespace;
    
    // Value namespace
    private final Map<String, Symbol> valueNamespace;
    
    // Field namespace (organized by type)
    private final Map<String, Map<String, Symbol>> fieldNamespaces;
    
    // Parent scope
    private NamespaceSymbolTable parent;
    
    // Child scopes
    private final List<NamespaceSymbolTable> children;
    
    // Scope level
    private final int scopeLevel;
    
    // Constructor
    public NamespaceSymbolTable(int scopeLevel) {
        this.typeNamespace = new HashMap<>();
        this.valueNamespace = new HashMap<>();
        this.fieldNamespaces = new HashMap<>();
        this.children = new ArrayList<>();
        this.scopeLevel = scopeLevel;
        this.parent = null;
    }
    
    private NamespaceSymbolTable(int scopeLevel, NamespaceSymbolTable parent) {
        this.typeNamespace = new HashMap<>();
        this.valueNamespace = new HashMap<>();
        this.fieldNamespaces = new HashMap<>();
        this.children = new ArrayList<>();
        this.scopeLevel = scopeLevel;
        this.parent = parent;
        parent.children.add(this);
    }
    
    // Create new child scope
    public NamespaceSymbolTable enterScope() {
        return new NamespaceSymbolTable(this.scopeLevel + 1, this);
    }
    
    // Exit current scope, return parent scope
    public NamespaceSymbolTable exitScope() {
        return parent;
    }
    
    // Add type symbol
    public void addTypeSymbol(Symbol symbol) {
        if (symbol.getNamespace() != Namespace.TYPE) {
            throw new IllegalArgumentException("Symbol is not in type namespace: " + symbol);
        }
        
        // Check if already exists
        if (typeNamespace.containsKey(symbol.getName())) {
            throw new SemanticException(
                String.format("Duplicate type declaration: '%s' at scope level %d",
                             symbol.getName(), scopeLevel),
                symbol.getDeclaration()
            );
        }
        
        typeNamespace.put(symbol.getName(), symbol);
    }
    
    // Add value symbol
    public void addValueSymbol(Symbol symbol) {
        if (symbol.getNamespace() != Namespace.VALUE) {
            throw new IllegalArgumentException("Symbol is not in value namespace: " + symbol);
        }
        
        // Special handling for LOCAL_VARIABLE and PARAMETER: check for conflicting CONSTANT in current or parent scopes
        if (symbol.getKind() == SymbolKind.LOCAL_VARIABLE || symbol.getKind() == SymbolKind.PARAMETER) {
            NamespaceSymbolTable scope = this;
            while (scope != null) {
                if (scope.valueNamespace.containsKey(symbol.getName())) {
                    Symbol existingSymbol = scope.valueNamespace.get(symbol.getName());
                    if (existingSymbol.getKind() == SymbolKind.CONSTANT) {
                        throw new SemanticException(
                            String.format("cannot define %s '%s' as it conflicts with constant in scope",
                                        symbol.getKind() == SymbolKind.LOCAL_VARIABLE ? "local variable" : "parameter",
                                        symbol.getName()),
                            symbol.getDeclaration()
                        );
                    }
                }
                scope = scope.parent;
            }
        }
        
        // Check if already exists in current scope
        if (valueNamespace.containsKey(symbol.getName())) {
            throw new SemanticException(
                String.format("Duplicate value declaration: '%s' at scope level %d",
                             symbol.getName(), scopeLevel),
                symbol.getDeclaration()
            );
        }
        
        valueNamespace.put(symbol.getName(), symbol);
    }
    
    // Add field symbol
    public void addFieldSymbol(String typeName, Symbol symbol) {
        if (symbol.getNamespace() != Namespace.FIELD) {
            throw new IllegalArgumentException("Symbol is not a field: " + symbol);
        }
        
        // Get or create field namespace for this type
        Map<String, Symbol> typeFields = fieldNamespaces.computeIfAbsent(
            typeName, k -> new HashMap<>()
        );
        
        // Check if already exists
        if (typeFields.containsKey(symbol.getName())) {
            throw new SemanticException(
                String.format("Duplicate field declaration: '%s' in type '%s'",
                             symbol.getName(), typeName),
                symbol.getDeclaration()
            );
        }
        
        typeFields.put(symbol.getName(), symbol);
    }
    
    // Lookup symbol in type namespace (including parent scopes)
    public Symbol lookupType(String name) {
        Symbol symbol = typeNamespace.get(name);
        if (symbol != null) {
            return symbol;
        }
        
        // If not found in current scope, recursively search parent scopes
        if (parent != null) {
            return parent.lookupType(name);
        }
        
        return null;
    }
    
    // Lookup symbol in value namespace (including parent scopes)
    public Symbol lookupValue(String name) {
        Symbol symbol = valueNamespace.get(name);
        if (symbol != null) {
            return symbol;
        }
        
        // If not found in current scope, recursively search parent scopes
        if (parent != null) {
            return parent.lookupValue(name);
        }
        
        return null;
    }
    
    // Lookup symbol in field namespace
    public Symbol lookupField(String typeName, String fieldName) {
        // First search in current scope
        Map<String, Symbol> typeFields = fieldNamespaces.get(typeName);
        if (typeFields != null) {
            Symbol symbol = typeFields.get(fieldName);
            if (symbol != null) {
                return symbol;
            }
            // If we found typeFields but not the specific field, don't recurse
            // The type exists in this scope but doesn't have this field
            return null;
        }
        
        // If type not found in current scope, recursively search parent scopes
        if (parent != null) {
            return parent.lookupField(typeName, fieldName);
        }
        
        return null;
    }
    
    // Lookup type symbol in current scope (excluding parent scopes)
    public Symbol lookupTypeInCurrentScope(String name) {
        return typeNamespace.get(name);
    }
    
    // Lookup value symbol in current scope (excluding parent scopes)
    public Symbol lookupValueInCurrentScope(String name) {
        return valueNamespace.get(name);
    }
    
    // Get scope level
    public int getScopeLevel() {
        return scopeLevel;
    }
    
    // Get parent scope
    public NamespaceSymbolTable getParent() {
        return parent;
    }
    
    // Get type namespace map (for debugging)
    public Map<String, Symbol> getTypeNamespace() {
        return typeNamespace;
    }
    
    // Get value namespace map (for debugging)
    public Map<String, Symbol> getValueNamespace() {
        return valueNamespace;
    }
    
    // Get field namespaces map (for debugging)
    public Map<String, Map<String, Symbol>> getFieldNamespaces() {
        return fieldNamespaces;
    }
    
    // Get child scopes (for debugging)
    public List<NamespaceSymbolTable> getChildren() {
        return children;
    }
    
    // Debug output
    public void debugPrint() {
        System.out.println("=== Scope Level " + scopeLevel + " ===");
        System.out.println("Type Namespace:");
        for (Symbol symbol : typeNamespace.values()) {
            System.out.println("  " + symbol);
        }
        System.out.println("Value Namespace:");
        for (Symbol symbol : valueNamespace.values()) {
            System.out.println("  " + symbol);
        }
        System.out.println("Field Namespace:");
        for (Map.Entry<String, Map<String, Symbol>> entry : fieldNamespaces.entrySet()) {
            System.out.println("  Type " + entry.getKey() + ":");
            for (Symbol symbol : entry.getValue().values()) {
                System.out.println("    " + symbol);
            }
        }
        System.out.println();
    }
}
```

### 1.4 Semantic Exception Class

```java
// Semantic exception class
public class SemanticException extends RuntimeException {
    private final ASTNode node;
    
    public SemanticException(String message) {
        this(message, null);
    }
    
    public SemanticException(String message, ASTNode node) {
        super(message);
        this.node = node;
    }
    
    public ASTNode getNode() {
        return node;
    }
}
```

## 第二阶段：声明处理

### 2.1 Rust变量遮蔽支持

Rust语言支持变量遮蔽（Variable Shadowing），允许在同一作用域内使用`let`语句重新声明同名变量。这是Rust的一个重要特性，与重复声明有本质区别：

- **变量遮蔽**：使用`let`关键字创建全新的变量，允许同名，甚至可以改变类型
- **重复声明**：不使用`let`尝试重新声明已存在的符号，这是错误

示例：
```rust
let x = 5;        // 第一个x
let x = x + 1;    // 第二个x，遮蔽第一个，类型相同
let x = "hello";  // 第三个x，遮蔽第二个，类型改变
```

在符号表实现中，值命名空间中的符号遵循以下重复声明检查规则：
- 任何类型的符号都不允许在同一作用域内重复声明
- `LOCAL_VARIABLE`和`PARAMETER`有特殊处理：在添加时，会检查当前及所有父作用域中是否存在同名的`CONSTANT`符号，如果存在则不允许添加
- 不同作用域间的遮蔽通过作用域机制自然实现

### 2.2 语义分析器基础结构

```java
// Semantic analyzer class
public class SemanticAnalyzer extends VisitorBase {
    // Global scope
    private NamespaceSymbolTable globalScope;
    
    // Current scope
    private NamespaceSymbolTable currentScope;
    
    // Error list
    private final List<SemanticError> errors;
    
    // Current context
    private Context currentContext;
    
    // Current type name (for field processing)
    private String currentTypeName;
    
    // Constructor
    public SemanticAnalyzer() {
        this.errors = new ArrayList<>();
        this.currentContext = Context.VALUE_CONTEXT; // Default value context
    }
    
    // Initialize global scope
    public void initializeGlobalScope() {
        globalScope = new NamespaceSymbolTable(0);
        currentScope = globalScope;
        
        // Add builtin types
        addBuiltinTypes();
    }
    
    // Add builtin types
    private void addBuiltinTypes() {
        // Add basic types to type namespace
        addBuiltinType("i32");
        addBuiltinType("u32");
        addBuiltinType("usize");
        addBuiltinType("isize");
        addBuiltinType("bool");
        addBuiltinType("char");
        addBuiltinType("str");  // String slice type
    }
    
    private void addBuiltinType(String typeName) {
        Symbol builtinType = new Symbol(
            typeName,
            SymbolKind.BUILTIN_TYPE,
            null, // Builtin types have no declaration node
            0, // Global scope
            false // Immutable
        );
        globalScope.addTypeSymbol(builtinType);
    }
    
    // Enter new scope
    private void enterScope() {
        currentScope = currentScope.enterScope();
    }
    
    // Exit current scope
    private void exitScope() {
        currentScope = currentScope.exitScope();
        if (currentScope == null) {
            throw new RuntimeException("Attempting to exit global scope");
        }
    }
    
    // Add error
    private void addError(ErrorType type, String message, ASTNode node) {
        errors.add(new SemanticError(type, message, node));
    }
    
    // Check if there are errors
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    // Get error list
    public List<SemanticError> getErrors() {
        return errors;
    }
    
    // Set context
    private void setContext(Context context) {
        this.currentContext = context;
    }
    
    // Context enumeration
    private enum Context {
        TYPE_CONTEXT,
        VALUE_CONTEXT,
        FIELD_CONTEXT,
        LET_PATTERN_CONTEXT,  // Special context for let statement patterns
        PARAMETER_PATTERN_CONTEXT  // Special context for parameter patterns
    }
}
```

### 2.2 处理类型声明

// 类型声明相关的visit函数将在文档底部重新组织

### 2.3 Trait和Impl作用域处理

在Rust中，trait和impl块都会创建新的作用域，但它们的作用域规则与普通块作用域有所不同：

#### Trait作用域特性：
1. **独立作用域**：trait内部的项目在trait自己的作用域内
2. **内部相互可见**：trait内的关联类型、关联常量、关联函数可以相互引用
3. **外部限定访问**：外部需要通过`TraitName::ItemName`的方式访问
4. **实现时可用**：trait中的项目只有在为具体类型实现后才真正可用

#### Impl作用域特性：
1. **独立作用域**：impl块内部有自己的作用域
2. **可以访问trait项目**：在trait impl中可以访问trait定义的项目
3. **类型关联**：impl块的项目与特定类型关联

示例：
```rust
trait MyTrait {
    type Output;
    const DEFAULT: i32;
    fn process(&self) -> Self::Output;
    fn default_impl() -> i32 {
        Self::DEFAULT // 可以访问同一trait内的常量
    }
}

impl MyTrait for i32 {
    type Output = String;
    const DEFAULT: i32 = 42;
    
    fn process(&self) -> Self::Output {
        format!("Value: {}", *self)
    }
}
```

在语义检查实现中，我们需要为trait和impl块创建新的作用域，确保它们的关联项目不会污染外部命名空间。

### 2.4 处理值声明

// 值声明相关的visit函数将在文档底部重新组织
// 注意：let语句创建的LOCAL_VARIABLE通过作用域机制支持遮蔽，这是Rust语言的重要特性

## 第三阶段：名称解析

### 3.1 标识符解析

// 标识符解析相关的visit函数将在文档底部重新组织

### 3.2 类型上下文解析

// 类型上下文解析相关的visit函数将在文档底部重新组织

### 3.3 值上下文解析

// 值上下文解析相关的visit函数将在文档底部重新组织

### 3.4 语句和表达式处理

// 语句和表达式处理相关的visit函数将在文档底部重新组织

## 第四阶段：错误检测

### 4.1 错误类型定义

```java
// Error type enumeration
public enum ErrorType {
    // Type namespace errors
    UNDECLARED_TYPE_IDENTIFIER("Undeclared type identifier"),
    DUPLICATE_TYPE_DECLARATION("Duplicate type declaration"),
    
    // Value namespace errors
    UNDECLARED_VALUE_IDENTIFIER("Undeclared value identifier"),
    DUPLICATE_VALUE_DECLARATION("Duplicate value declaration"),
    
    // Field errors
    UNDECLARED_FIELD("Undeclared field"),
    DUPLICATE_FIELD_DECLARATION("Duplicate field declaration"),
    
    // Namespace violations
    NAMESPACE_VIOLATION("Namespace violation"),
    
    // Associated item errors
    UNDECLARED_ASSOCIATED_ITEM("Undeclared associated item"),
    INVALID_ASSOCIATED_ACCESS("Invalid associated access"),
    
    // Other errors
    GENERAL_SEMANTIC_ERROR("General semantic error");
    
    private final String description;
    
    ErrorType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

// Semantic error class
public class SemanticError {
    private final ErrorType type;
    private final String message;
    private final ASTNode node;
    private final int line;
    private final int column;
    
    public SemanticError(ErrorType type, String message, ASTNode node) {
        this.type = type;
        this.message = message;
        this.node = node;
        
        // Should get line and column numbers from AST node here
        // Simplified implementation: use default values
        this.line = 0;
        this.column = 0;
    }
    
    // Getter methods
    public ErrorType getType() { return type; }
    public String getMessage() { return message; }
    public ASTNode getNode() { return node; }
    public int getLine() { return line; }
    public int getColumn() { return column; }
    
    @Override
    public String toString() {
        return String.format("Semantic Error [%s]: %s (Line: %d, Column: %d)",
                           type.getDescription(), message, line, column);
    }
}
```

### 4.2 错误收集和报告

```java
// Add the following methods in SemanticAnalyzer class

// Add type error
private void addTypeError(String message, ASTNode node) {
    addError(ErrorType.UNDECLARED_TYPE_IDENTIFIER, message, node);
}

// Add value error
private void addValueError(String message, ASTNode node) {
    addError(ErrorType.UNDECLARED_VALUE_IDENTIFIER, message, node);
}

// Add field error
private void addFieldError(String message, ASTNode node) {
    addError(ErrorType.UNDECLARED_FIELD, message, node);
}

// Add duplicate declaration error
private void addDuplicateError(String message, ASTNode node) {
    // Determine error type based on context
    if (currentContext == Context.TYPE_CONTEXT) {
        addError(ErrorType.DUPLICATE_TYPE_DECLARATION, message, node);
    } else if (currentContext == Context.VALUE_CONTEXT) {
        addError(ErrorType.DUPLICATE_VALUE_DECLARATION, message, node);
    } else {
        addError(ErrorType.DUPLICATE_FIELD_DECLARATION, message, node);
    }
}

// Print all errors
public void printErrors() {
    if (hasErrors()) {
        System.err.println("Found " + errors.size() + " semantic errors:");
        for (SemanticError error : errors) {
            System.err.println("  " + error);
        }
    } else {
        System.out.println("Semantic check passed, no errors found.");
    }
}

// Generate error report
public String generateErrorReport() {
    if (!hasErrors()) {
        return "Semantic check passed, no errors found.";
    }
    
    StringBuilder report = new StringBuilder();
    report.append("Found ").append(errors.size()).append(" semantic errors:\n");
    
    // Group errors by type
    Map<ErrorType, List<SemanticError>> errorsByType = new HashMap<>();
    for (SemanticError error : errors) {
        errorsByType.computeIfAbsent(error.getType(), k -> new ArrayList<>()).add(error);
    }
    
    // Output errors by type
    for (Map.Entry<ErrorType, List<SemanticError>> entry : errorsByType.entrySet()) {
        report.append("\n").append(entry.getKey().getDescription()).append(":\n");
        for (SemanticError error : entry.getValue()) {
            report.append("  ").append(error.getMessage()).append("\n");
        }
    }
    
    return report.toString();
}
```

### 4.3 错误恢复策略

```java
// Add the following methods in SemanticAnalyzer class

// Safe symbol lookup (without throwing exceptions)
private Symbol safeLookupType(String name) {
    try {
        return currentScope.lookupType(name);
    } catch (Exception e) {
        return null;
    }
}

// Safe symbol lookup (without throwing exceptions)
private Symbol safeLookupValue(String name) {
    try {
        return currentScope.lookupValue(name);
    } catch (Exception e) {
        return null;
    }
}

// Safe symbol lookup (without throwing exceptions)
private Symbol safeLookupField(String typeName, String fieldName) {
    try {
        return currentScope.lookupField(typeName, fieldName);
    } catch (Exception e) {
        return null;
    }
}

// Safe symbol addition (catch exceptions and convert to errors)
private void safeAddTypeSymbol(Symbol symbol) {
    try {
        currentScope.addTypeSymbol(symbol);
    } catch (SemanticException e) {
        addError(ErrorType.DUPLICATE_TYPE_DECLARATION, e.getMessage(), symbol.getDeclaration());
    }
}

// Safe symbol addition (catch exceptions and convert to errors)
private void safeAddValueSymbol(Symbol symbol) {
    try {
        currentScope.addValueSymbol(symbol);
    } catch (SemanticException e) {
        // Report duplicate declaration errors
        addError(ErrorType.DUPLICATE_VALUE_DECLARATION, e.getMessage(), symbol.getDeclaration());
    }
}

// Safe symbol addition (catch exceptions and convert to errors)
private void safeAddFieldSymbol(String typeName, Symbol symbol) {
    try {
        currentScope.addFieldSymbol(typeName, symbol);
    } catch (SemanticException e) {
        addError(ErrorType.DUPLICATE_FIELD_DECLARATION, e.getMessage(), symbol.getDeclaration());
    }
}
```

### 4.4 调试和诊断工具

```java
// Add the following methods in SemanticAnalyzer class

// Print symbol table status
public void debugPrintSymbolTable() {
    System.out.println("=== Symbol Table Status ===");
    debugPrintScope(globalScope, 0);
}

// Recursively print scopes
private void debugPrintScope(NamespaceSymbolTable scope, int indent) {
    // Print indentation
    for (int i = 0; i < indent; i++) {
        System.out.print("  ");
    }
    
    System.out.println("Scope Level " + scope.getScopeLevel() + ":");
    
    // Print type namespace
    for (int i = 0; i < indent; i++) {
        System.out.print("  ");
    }
    System.out.println("  Type Namespace:");
    for (Symbol symbol : scope.getTypeNamespace().values()) {
        for (int i = 0; i < indent + 2; i++) {
            System.out.print("  ");
        }
        System.out.println(symbol);
    }
    
    // Print value namespace
    for (int i = 0; i < indent; i++) {
        System.out.print("  ");
    }
    System.out.println("  Value Namespace:");
    for (Symbol symbol : scope.getValueNamespace().values()) {
        for (int i = 0; i < indent + 2; i++) {
            System.out.print("  ");
        }
        System.out.println(symbol);
    }
    
    // Print field namespace
    for (int i = 0; i < indent; i++) {
        System.out.print("  ");
    }
    System.out.println("  Field Namespace:");
    for (Map.Entry<String, Map<String, Symbol>> entry : scope.getFieldNamespaces().entrySet()) {
        for (int i = 0; i < indent + 2; i++) {
            System.out.print("  ");
        }
        System.out.println("    Type " + entry.getKey() + ":");
        for (Symbol symbol : entry.getValue().values()) {
            for (int i = 0; i < indent + 4; i++) {
                System.out.print("  ");
            }
            System.out.println(symbol);
        }
    }
    
    // Recursively print child scopes
    for (NamespaceSymbolTable child : scope.getChildren()) {
        debugPrintScope(child, indent + 1);
    }
}

// Generate symbol table statistics
public String generateSymbolTableStats() {
    StringBuilder stats = new StringBuilder();
    stats.append("=== Symbol Table Statistics ===\n");
    
    Map<SymbolKind, Integer> symbolCounts = new HashMap<>();
    countSymbols(globalScope, symbolCounts);
    
    for (Map.Entry<SymbolKind, Integer> entry : symbolCounts.entrySet()) {
        stats.append(entry.getKey().getDescription())
             .append(": ")
             .append(entry.getValue())
             .append("\n");
    }
    
    return stats.toString();
}

// Recursively count symbols
private void countSymbols(NamespaceSymbolTable scope, Map<SymbolKind, Integer> counts) {
    // Count symbols in current scope
    for (Symbol symbol : scope.getTypeNamespace().values()) {
        counts.merge(symbol.getKind(), 1, Integer::sum);
    }
    
    for (Symbol symbol : scope.getValueNamespace().values()) {
        counts.merge(symbol.getKind(), 1, Integer::sum);
    }
    
    for (Map<String, Symbol> typeFields : scope.getFieldNamespaces().values()) {
        for (Symbol symbol : typeFields.values()) {
            counts.merge(symbol.getKind(), 1, Integer::sum);
        }
    }
    
    // Recursively count child scopes
    for (NamespaceSymbolTable child : scope.getChildren()) {
        countSymbols(child, counts);
    }
}
```

## Summary

These four phases provide a complete implementation method for namespace semantic checking:

1. **Phase 1** establishes the basic structure of a multi-namespace symbol table, supporting three independent namespaces: type, value, and field
2. **Phase 2** implements processing of various declarations, including type declarations and value declarations
3. **Phase 3** implements context-aware name resolution, ensuring names are looked up in the correct namespace
4. **Phase 4** implements a comprehensive error detection and reporting system, including error classification, collection, and diagnostic tools

This implementation strictly follows the namespace specifications in the Rust Reference, ensuring the compiler can correctly handle Rust's namespace rules.

## Visit函数实现

### 基础节点 (ASTNode)

```java
// Process identifier
@Override
public void visit(IdentifierNode node) {
    String identifierName = node.name;
    
    switch (currentContext) {
        case TYPE_CONTEXT:
            // Lookup in type namespace
            Symbol typeSymbol = currentScope.lookupType(identifierName);
            if (typeSymbol == null) {
                addError(ErrorType.UNDECLARED_TYPE_IDENTIFIER,
                        String.format("Undeclared type identifier: '%s'", identifierName),
                        node);
            }
            break;
            
        case VALUE_CONTEXT:
            // Lookup in value namespace
            Symbol valueSymbol = currentScope.lookupValue(identifierName);
            if (valueSymbol == null) {
                addError(ErrorType.UNDECLARED_VALUE_IDENTIFIER,
                        String.format("Undeclared value identifier: '%s'", identifierName),
                        node);
            }
            break;
            
        case FIELD_CONTEXT:
            // Field context requires special handling, processed in FieldExprNode
            addError(ErrorType.NAMESPACE_VIOLATION,
                    "Identifier in field context should be processed through FieldExprNode",
                    node);
            break;
    }
}
```

### 语句节点 (StmtNode及其子类)

#### LetStmtNode

```java
// Process let statement
@Override
public void visit(LetStmtNode node) {
    // First process value expression (if any)
    if (node.value != null) {
        node.value.accept(this);
    }
    
    // Then process pattern in let pattern context
    Context previousContext = currentContext;
    setContext(Context.LET_PATTERN_CONTEXT);
    
    try {
        node.name.accept(this);
    } finally {
        // Restore context
        setContext(previousContext);
    }
}
```

#### ExprStmtNode

```java
// Process expression statement
@Override
public void visit(ExprStmtNode node) {
    // Set value context
    Context previousContext = currentContext;
    setContext(Context.VALUE_CONTEXT);
    
    try {
        // Process expression
        node.expr.accept(this);
    } finally {
        // Restore context
        setContext(previousContext);
    }
}
```

#### FunctionNode

```java
// Process function declaration
@Override
public void visit(FunctionNode node) {
    String functionName = node.name.name;
    
    try {
        // Create function symbol
        Symbol functionSymbol = new Symbol(
            functionName,
            SymbolKind.FUNCTION,
            node,
            currentScope.getScopeLevel(),
            false
        );
        
        // Add to value namespace
        currentScope.addValueSymbol(functionSymbol);
        
        // Enter function scope
        enterScope();
        
        // Process self parameter (if any)
        if (node.selfPara != null) {
            // Create a special symbol for self
            Symbol selfSymbol = new Symbol(
                "self",
                SymbolKind.PARAMETER, // Treat self as a parameter
                node.selfPara,
                currentScope.getScopeLevel(),
                false // self is immutable by default
            );
            
            try {
                // Add to value namespace
                currentScope.addValueSymbol(selfSymbol);
            } catch (SemanticException e) {
                addError(ErrorType.DUPLICATE_VALUE_DECLARATION, e.getMessage(), node.selfPara);
            }
        }
        
        // Process parameters
        if (node.parameters != null) {
            for (ParameterNode param : node.parameters) {
                param.accept(this);
            }
        }
        
        // Process return type (if any)
        if (node.returnType != null) {
            Context previousContext = currentContext;
            setContext(Context.TYPE_CONTEXT);
            
            try {
                node.returnType.accept(this);
            } finally {
                // Restore context
                setContext(previousContext);
            }
        }
        
        // Process function body
        if (node.body != null) {
            node.body.accept(this);
        }
        
        // Exit function scope
        exitScope();
        
    } catch (SemanticException e) {
        addError(ErrorType.DUPLICATE_VALUE_DECLARATION, e.getMessage(), node);
    }
}
```

#### StructNode

```java
// Process struct declaration
@Override
public void visit(StructNode node) {
    String structName = node.name.name;
    
    try {
        // Create struct symbol
        Symbol structSymbol = new Symbol(
            structName,
            SymbolKind.STRUCT,
            node,
            currentScope.getScopeLevel(),
            false
        );
        
        // Add to type namespace
        currentScope.addTypeSymbol(structSymbol);
        
        // Enter struct scope for fields
        enterScope();
        
        // Set the struct's scope to the current scope
        structSymbol.setScope(currentScope);
        
        // Set current type name for field processing
        String previousTypeName = currentTypeName;
        currentTypeName = structName;
        
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
            structName,
            SymbolKind.STRUCT_CONSTRUCTOR,
            node,
            currentScope.getScopeLevel(),
            false
        );
        
        // Add to value namespace
        currentScope.addValueSymbol(constructorSymbol);
        
    } catch (SemanticException e) {
        addError(ErrorType.DUPLICATE_TYPE_DECLARATION, e.getMessage(), node);
    }
}
```

#### FieldNode

```java
// Process field declaration
@Override
public void visit(FieldNode node) {
    String fieldName = node.name.name;
    
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
        currentScope.addFieldSymbol(currentTypeName, fieldSymbol);
        
    } catch (SemanticException e) {
        addError(ErrorType.DUPLICATE_FIELD_DECLARATION, e.getMessage(), node);
    }
}
```

#### EnumNode

```java
// Process enum declaration
@Override
public void visit(EnumNode node) {
    String enumName = node.name.name;
    
    try {
        // Create enum symbol
        Symbol enumSymbol = new Symbol(
            enumName,
            SymbolKind.ENUM,
            node,
            currentScope.getScopeLevel(),
            false
        );
        
        // Add to type namespace
        currentScope.addTypeSymbol(enumSymbol);
        
        // Enter enum scope for variants
        enterScope();
        
        // Set the enum's scope to the current scope
        enumSymbol.setScope(currentScope);
        
        // Process enum variants
        if (node.variants != null) {
            for (IdentifierNode variant : node.variants) {
                // ENUM_VARIANT is removed from type namespace, only constructor remains in value namespace
                
                // Create enum variant constructor symbol
                Symbol variantConstructorSymbol = new Symbol(
                    variant.name,
                    SymbolKind.ENUM_VARIANT_CONSTRUCTOR,
                    variant,
                    currentScope.getScopeLevel(),
                    false
                );
                
                // Add to value namespace
                currentScope.addValueSymbol(variantConstructorSymbol);
            }
        }
        
        // Exit enum scope
        exitScope();
        
    } catch (SemanticException e) {
        addError(ErrorType.DUPLICATE_TYPE_DECLARATION, e.getMessage(), node);
    }
}
```

#### ConstItemNode

```java
// Process constant declaration
@Override
public void visit(ConstItemNode node) {
    String constName = node.name.name;
    
    try {
        // Create constant symbol
        Symbol constSymbol = new Symbol(
            constName,
            SymbolKind.CONSTANT,
            node,
            currentScope.getScopeLevel(),
            false // Constants are immutable
        );
        
        // Add to value namespace
        currentScope.addValueSymbol(constSymbol);
        
        // Process constant type (if any)
        if (node.type != null) {
            Context previousContext = currentContext;
            setContext(Context.TYPE_CONTEXT);
            
            try {
                node.type.accept(this);
            } finally {
                // Restore context
                setContext(previousContext);
            }
        }
        
        // Process constant value (if any)
        if (node.value != null) {
            node.value.accept(this);
        }
        
    } catch (SemanticException e) {
        addError(ErrorType.DUPLICATE_VALUE_DECLARATION, e.getMessage(), node);
    }
}
```

#### TraitNode

```java
// Process trait declaration
@Override
public void visit(TraitNode node) {
    String traitName = node.name.name;
    
    try {
        // Create trait symbol
        Symbol traitSymbol = new Symbol(
            traitName,
            SymbolKind.TRAIT,
            node,
            currentScope.getScopeLevel(),
            false
        );
        
        // Add to type namespace
        currentScope.addTypeSymbol(traitSymbol);
        
        // Enter trait scope for associated items
        // Trait creates a new scope for its associated items
        enterScope();
        
        // Set the trait's scope to the current scope
        traitSymbol.setScope(currentScope);
        
        // Process associated items in trait
        if (node.items != null) {
            for (AssoItemNode item : node.items) {
                item.accept(this);
            }
        }
        
        // Exit trait scope
        exitScope();
        
    } catch (SemanticException e) {
        addError(ErrorType.DUPLICATE_TYPE_DECLARATION, e.getMessage(), node);
    }
}
```

#### ImplNode

```java
// Process impl declaration
@Override
public void visit(ImplNode node) {
    // Process type name
    node.typeName.accept(this);
    
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
        
        node.trait.accept(this);
    }
    
    // Enter impl scope for associated items
    enterScope();
    
    // Process associated items
    if (node.items != null) {
        for (AssoItemNode item : node.items) {
            item.accept(this);
        }
    }
    
    // After processing all items, add all symbols in current scope to type's implSymbols
    if (typeSymbol != null) {
        for (Symbol symbol : currentScope.getValueNamespace().values()) {
            // Only add value namespace symbols (functions, constants) to implSymbols
            if (symbol.getKind() == SymbolKind.FUNCTION ||
                symbol.getKind() == SymbolKind.CONSTANT) {
                typeSymbol.addImplSymbol(symbol);
            }
        }
    }
    
    // Exit impl scope
    exitScope();
}
```

### 表达式节点 (ExprNode及其子类)

#### BlockExprNode

```java
// Process block expression
@Override
public void visit(BlockExprNode node) {
    // Enter new scope
    enterScope();
    
    try {
        // Process statements in block
        if (node.statements != null) {
            for (StmtNode stmt : node.statements) {
                stmt.accept(this);
            }
        }
    } finally {
        // Exit scope
        exitScope();
    }
}
```

#### PathExprNode

```java
// Process path expression
@Override
public void visit(PathExprNode node) {
    if (node.RSeg != null) {
        // 处理双段路径（带"::"）
        resolvePath(node);
    } else {
        // 处理单段路径（不带"::"）- 直接递归处理子节点
        node.LSeg.accept(this);
    }
}

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

// 解析路径（只处理关联路径）
private Symbol resolvePath(PathExprNode node) {
    String leftSegment = getSegmentText(node.LSeg);
    String rightSegment = getSegmentText(node.RSeg);
    
    // 处理左段的特殊情况
    Symbol baseType;
    if (leftSegment.equals("self")) {
        // self不能作为关联路径的左段
        addError(ErrorType.NAMESPACE_VIOLATION,
                "'self' cannot be used in associated path", node);
        return null;
    } else if (leftSegment.equals("Self")) {
        // Self表示当前类型，使用currentTypeName
        if (currentTypeName == null || currentTypeName.isEmpty()) {
            addError(ErrorType.NAMESPACE_VIOLATION,
                    "'Self' is not available in this context", node);
            return null;
        }
        baseType = currentScope.lookupType(currentTypeName);
        if (baseType == null) {
            addError(ErrorType.UNDECLARED_TYPE_IDENTIFIER,
                    "Current type '" + currentTypeName + "' is not defined", node);
            return null;
        }
    } else {
        // 查找基础类型
        baseType = currentScope.lookupType(leftSegment);
        if (baseType == null) {
            addError(ErrorType.UNDECLARED_TYPE_IDENTIFIER,
                    "Undeclared type: " + leftSegment, node);
            return null;
        }
    }
    
    // 解析关联项
    return resolveAssociatedItem(baseType, rightSegment, node);
}

// 解析关联项的通用方法
private Symbol resolveAssociatedItem(Symbol baseType, String itemName, PathExprNode node) {
    // 首先检查是否是构造函数（类型名称本身）
    if (itemName.equals(baseType.getName())) {
        return baseType;
    }
    
    // 对于枚举类型，特殊处理：在枚举类型的作用域中查找枚举变体
    if (baseType.getKind() == SymbolKind.ENUM) {
        Symbol enumVariant = baseType.getScope().lookupValue(itemName);
        if (enumVariant != null && enumVariant.getKind() == SymbolKind.ENUM_VARIANT_CONSTRUCTOR) {
            return enumVariant;
        }
    }
    
    // 对于trait类型，在trait的作用域中查找关联项
    if (baseType.getKind() == SymbolKind.TRAIT) {
        Symbol traitItem = baseType.getScope().lookupValue(itemName);
        if (traitItem != null &&
            (traitItem.getKind() == SymbolKind.FUNCTION || traitItem.getKind() == SymbolKind.CONSTANT)) {
            return traitItem;
        }
    }
    
    // 对于所有类型（struct, enum, trait），在implSymbols中查找关联项
    for (Symbol implSymbol : baseType.getImplSymbols()) {
        if (implSymbol.getName().equals(itemName) &&
            (implSymbol.getKind() == SymbolKind.FUNCTION || implSymbol.getKind() == SymbolKind.CONSTANT)) {
            return implSymbol;
        }
    }
    
    // 未找到
    addError(ErrorType.UNDECLARED_ASSOCIATED_ITEM,
            "Type " + baseType.getName() + " does not have associated item: " + itemName, node);
    return null;
}
```

#### PathExprSegNode

```java
// Process path expression segment
@Override
public void visit(PathExprSegNode node) {
    if (node.patternType == patternSeg_t.IDENT) {
        // Regular identifier
        node.name.accept(this);
    }
    // self and Self are handled elsewhere (e.g., in FunctionNode)
    // No need to process them here
}
```

#### CallExprNode

```java
// Process function call expression
@Override
public void visit(CallExprNode node) {
    // Set value context
    Context previousContext = currentContext;
    setContext(Context.VALUE_CONTEXT);
    
    try {
        // Process function expression
        node.function.accept(this);
        
        // Process arguments
        if (node.arguments != null) {
            for (ExprNode arg : node.arguments) {
                arg.accept(this);
            }
        }
    } finally {
        // Restore context
        setContext(previousContext);
    }
}
```

#### MethodCallExprNode

```java
// Process method call expression
@Override
public void visit(MethodCallExprNode node) {
    // Process receiver
    node.receiver.accept(this);
    
    // Set value context
    Context previousContext = currentContext;
    setContext(Context.VALUE_CONTEXT);
    
    try {
        // Process method name
        node.methodName.accept(this);
        
        // Process arguments
        if (node.arguments != null) {
            for (ExprNode arg : node.arguments) {
                arg.accept(this);
            }
        }
    } finally {
        // Restore context
        setContext(previousContext);
    }
}
```

#### FieldExprNode

```java
// Process field access expression
@Override
public void visit(FieldExprNode node) {
    // Process receiver expression
    node.receiver.accept(this);
    
    // Set field context
    Context previousContext = currentContext;
    setContext(Context.FIELD_CONTEXT);
    
    try {
        // Process field name
        node.fieldName.accept(this);
    } finally {
        // Restore context
        setContext(previousContext);
    }
}
```

### 模式节点 (PatternNode及其子类)

#### IdPatNode

```java
// Process identifier pattern
@Override
public void visit(IdPatNode node) {
    if (currentContext == Context.LET_PATTERN_CONTEXT) {
        String varName = node.name.name;
        
        // Create local variable symbol
        Symbol varSymbol = new Symbol(
            varName,
            SymbolKind.LOCAL_VARIABLE,
            node,
            currentScope.getScopeLevel(),
            node.isMutable
        );
        
        try {
            // Add to value namespace
            currentScope.addValueSymbol(varSymbol);
        } catch (SemanticException e) {
            // Report duplicate declaration error
            addError(ErrorType.DUPLICATE_VALUE_DECLARATION, e.getMessage(), node);
        }
    } else if (currentContext == Context.PARAMETER_PATTERN_CONTEXT) {
        String paramName = node.name.name;
        
        // Create parameter symbol
        Symbol paramSymbol = new Symbol(
            paramName,
            SymbolKind.PARAMETER,
            node,
            currentScope.getScopeLevel(),
            false // Parameters are immutable by default
        );
        
        try {
            // Add to value namespace
            currentScope.addValueSymbol(paramSymbol);
        } catch (SemanticException e) {
            // Report duplicate declaration error
            addError(ErrorType.DUPLICATE_VALUE_DECLARATION, e.getMessage(), node);
        }
    } else {
        // In other contexts, just process the identifier name
        node.name.accept(this);
    }
}
```

### 类型节点 (TypeExprNode及其子类)

#### TypePathExprNode

```java
// Process type path expression
@Override
public void visit(TypePathExprNode node) {
    // Set type context
    Context previousContext = currentContext;
    setContext(Context.TYPE_CONTEXT);
    
    try {
        // Process path
        node.path.accept(this);
    } finally {
        // Restore context
        setContext(previousContext);
    }
}
```

#### TypeRefExprNode

```java
// Process type reference expression
@Override
public void visit(TypeRefExprNode node) {
    // Process inner type
    node.innerType.accept(this);
}
```

#### TypeArrayExprNode

```java
// Process type array expression
@Override
public void visit(TypeArrayExprNode node) {
    // Process element type
    node.elementType.accept(this);
    
    // Process size expression
    node.size.accept(this);
}
```

#### TypeUnitExprNode

```java
// Process type unit expression
@Override
public void visit(TypeUnitExprNode node) {
    // Unit type doesn't require semantic checking
}
```

### 其他节点

#### ParameterNode

```java
// Process parameter
@Override
public void visit(ParameterNode node) {
    // Process pattern in parameter pattern context
    if (node.pattern != null) {
        Context previousContext = currentContext;
        setContext(Context.PARAMETER_PATTERN_CONTEXT);
        
        try {
            node.pattern.accept(this);
        } finally {
            // Restore context
            setContext(previousContext);
        }
    }
    
    // Process parameter type in type context
    if (node.type != null) {
        Context previousContext = currentContext;
        setContext(Context.TYPE_CONTEXT);
        
        try {
            node.type.accept(this);
        } finally {
            // Restore context
            setContext(previousContext);
        }
    }
}
```

#### resolveField

```java
// Process field name (in field context)
private void resolveField(IdentifierNode fieldNode, ExprNode receiver) {
    String fieldName = fieldNode.name;
    
    // Need to determine receiver type here
    // In complete implementation, type analysis is needed to determine receiver type
    // Simplified implementation: assume receiver is an identifier whose name is the type name
    if (receiver instanceof IdentifierNode) {
        IdentifierNode receiverId = (IdentifierNode) receiver;
        String receiverTypeName = receiverId.name;
        
        // Lookup in field namespace
        Symbol fieldSymbol = currentScope.lookupField(receiverTypeName, fieldName);
        if (fieldSymbol == null) {
            addError(ErrorType.UNDECLARED_FIELD,
                    String.format("Type '%s' has no field '%s'", receiverTypeName, fieldName),
                    fieldNode);
        }
    } else {
        // More complex receiver expressions require more complex type analysis
        addError(ErrorType.UNDECLARED_FIELD,
                String.format("Cannot determine receiver type, unable to resolve field '%s'", fieldName),
                fieldNode);
    }
}
```