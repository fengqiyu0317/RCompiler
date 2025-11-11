# Parser Design Document

## Overview

This document provides a comprehensive design overview of the RCompiler parser, which is responsible for converting a sequence of tokens produced by the tokenizer into an Abstract Syntax Tree (AST) representing the structure of the source code.

## Table of Contents

1. [Architecture](#architecture)
2. [Parser Class Structure](#parser-class-structure)
3. [Parsing Strategy](#parsing-strategy)
4. [AST Node Hierarchy](#ast-node-hierarchy)
5. [Expression Parsing](#expression-parsing)
6. [Statement Parsing](#statement-parsing)
7. [Item Parsing](#item-parsing)
8. [Type System](#type-system)
9. [Error Handling](#error-handling)
10. [Performance Considerations](#performance-considerations)
11. [Future Enhancements](#future-enhancements)

## Architecture

### Core Components

The parser consists of several key components:

1. **Parser Class** (`src/main/java/parser/Parser.java`): The main parsing engine
2. **AST Nodes** (`src/main/java/ast/AST.java`): Node definitions for the AST
3. **Type Definitions** (`src/main/java/types/`): Enums for literals, operators, and pattern segments
4. **Exception Handling** (`src/main/java/parser/ParseException.java`): Custom exception for parsing errors

### Design Principles

The parser follows these design principles:

- **Recursive Descent**: Uses a top-down recursive descent parsing approach
- **Predictive Parsing**: Implements LL(1) parsing where possible, with backtracking for ambiguous constructs
- **Operator Precedence**: Implements proper operator precedence for expression parsing
- **Modularity**: Separates parsing logic into distinct methods for each grammar construct
- **Error Recovery**: Provides meaningful error messages with location information

## Parser Class Structure

### Core Fields

```java
public class Parser {
    private Vector<token_t> tokens;  // Input token stream
    int i = 0;                       // Current position in token stream
    private static final int MAX_RECURSION_DEPTH = 100;  // Recursion limit
    private int recursionDepth = 0;    // Current recursion depth
    private static final Set<String> keywords;  // Rust keywords set
    Vector<StmtNode> statements;      // Parsed statements
}
```

### Key Methods

The parser is organized into several categories of methods:

1. **Entry Point**:
   - `parse()`: Main entry point that parses the entire token stream

2. **Statement Parsing**:
   - `parseStmtNode()`: Parses statements and determines their type
   - `parseLetStmtNode()`: Parses let statements
   - `parseExprStmtNode()`: Parses expression statements

3. **Item Parsing**:
   - `parseFunctionNode()`: Parses function definitions
   - `parseStructNode()`: Parses struct definitions
   - `parseEnumNode()`: Parses enum definitions
   - `parseConstItemNode()`: Parses constant items
   - `parseTraitNode()`: Parses trait definitions
   - `parseImplNode()`: Parses implementation blocks

4. **Expression Parsing**:
   - `parseExprNode()`: Parses expressions with precedence handling
   - `parseExprWithBlockNode()`: Parses expressions containing blocks
   - `parseExprWithoutBlockNode()`: Parses expressions without blocks

5. **Type Parsing**:
   - `parseTypeExprNode()`: Parses type expressions
   - `parseTypePathExprNode()`: Parses path types
   - `parseTypeRefExprNode()`: Parses reference types
   - `parseTypeArrayExprNode()`: Parses array types
   - `parseTypeUnitExprNode()`: Parses unit types

6. **Pattern Parsing**:
   - `parsePatternNode()`: Parses patterns
   - `parseIdPatNode()`: Parses identifier patterns
   - `parseWildPatNode()`: Parses wildcard patterns
   - `parseRefPatNode()`: Parses reference patterns

## Parsing Strategy

### Recursive Descent with Backtracking

The parser uses a recursive descent approach with selective backtracking for ambiguous constructs. The main challenge is distinguishing between expressions with blocks (`ExprWithBlockNode`) and expressions without blocks (`ExprWithoutBlockNode`).

### Expression Disambiguation Strategy

The parser implements a sophisticated strategy to handle the ambiguity between expressions with and without blocks:

1. **Initial Attempt**: First attempts to parse as `ExprWithBlockNode` when encountering tokens like `if`, `while`, `loop`, or `{`
2. **Lookahead Check**: After parsing, checks the next token to determine if the expression is part of a larger expression
3. **Backtracking if Necessary**: If the next token indicates the expression is part of a larger construct, backtracks and re-parses as `ExprWithoutBlockNode`

### Operator Precedence Parsing

The parser implements operator precedence parsing using the following precedence levels (higher number = higher precedence):

1. **170**: Path extension (`::`)
2. **130**: Unary operators (`&`, `*`, `!`, `-`)
3. **120**: Type casting (`as`)
4. **110**: Multiplication, division, modulo (`*`, `/`, `%`)
5. **100**: Addition, subtraction (`+`, `-`)
6. **90**: Bit shifts (`<<`, `>>`)
7. **80**: Bitwise AND (`&`)
8. **70**: Bitwise XOR (`^`)
9. **60**: Bitwise OR (`|`)
10. **50**: Comparison operators (`==`, `!=`, `<`, `<=`, `>`, `>=`)
11. **40**: Logical AND (`&&`)
12. **30**: Logical OR (`||`)
13. **20**: Assignment operators (`=`, `+=`, `-=`, etc.)

## AST Node Hierarchy

The AST is defined in `src/main/java/ast/AST.java` with a hierarchical structure:

### Base Node
- `ASTNode`: Abstract base class for all AST nodes

### Statement Nodes
- `StmtNode`: Base class for all statements
- `LetStmtNode`: Represents let statements
- `ExprStmtNode`: Represents expression statements

### Item Nodes
- `ItemNode`: Base class for all items (extends StmtNode)
- `FunctionNode`: Represents function definitions
- `StructNode`: Represents struct definitions
- `EnumNode`: Represents enum definitions
- `ConstItemNode`: Represents constant items
- `TraitNode`: Represents trait definitions
- `ImplNode`: Represents implementation blocks

### Expression Nodes
- `ExprNode`: Base class for all expressions
- `ExprWithBlockNode`: Base class for expressions containing blocks
  - `IfExprNode`: Represents if expressions
  - `LoopExprNode`: Represents loop expressions
  - `BlockExprNode`: Represents block expressions
- `ExprWithoutBlockNode`: Base class for expressions without blocks
  - `LiteralExprNode`: Represents literal values
  - `PathExprNode`: Represents path expressions
  - `OperExprNode`: Base class for operator expressions
    - `ArithExprNode`: Arithmetic operations
    - `CompExprNode`: Comparison operations
    - `LazyExprNode`: Logical operations
    - `AssignExprNode`: Assignment operations
    - `ComAssignExprNode`: Compound assignment operations
    - `TypeCastExprNode`: Type casting
    - `BorrowExprNode`: Borrow operations
    - `DerefExprNode`: Dereference operations
    - `NegaExprNode`: Negation operations
  - `ArrayExprNode`: Array expressions
  - `IndexExprNode`: Index expressions
  - `StructExprNode`: Struct expressions
  - `CallExprNode`: Function call expressions
  - `MethodCallExprNode`: Method call expressions
  - `FieldExprNode`: Field access expressions
  - `ContinueExprNode`: Continue expressions
  - `BreakExprNode`: Break expressions
  - `ReturnExprNode`: Return expressions
  - `UnderscoreExprNode`: Underscore expressions
  - `GroupExprNode`: Grouped expressions

### Type Nodes
- `TypeExprNode`: Base class for type expressions
- `TypePathExprNode`: Path types
- `TypeRefExprNode`: Reference types
- `TypeArrayExprNode`: Array types
- `TypeUnitExprNode`: Unit types

### Pattern Nodes
- `PatternNode`: Base class for patterns
- `IdPatNode`: Identifier patterns
- `WildPatNode`: Wildcard patterns
- `RefPatNode`: Reference patterns

### Supporting Nodes
- `IdentifierNode`: Represents identifiers
- `ParameterNode`: Function parameters
- `SelfParaNode`: Self parameters
- `FieldNode`: Struct fields
- `FieldValNode`: Struct field values
- `AssoItemNode`: Associated items in traits and impls

## Expression Parsing

### Expression Categories

The parser distinguishes between two main categories of expressions:

1. **Expressions with Blocks** (`ExprWithBlockNode`):
   - If expressions (`if`)
   - Loop expressions (`while`, `loop`)
   - Block expressions (`{ ... }`)

2. **Expressions without Blocks** (`ExprWithoutBlockNode`):
   - Literals, identifiers, paths
   - Operator expressions (arithmetic, comparison, logical, etc.)
   - Function calls, method calls
   - Array expressions, struct expressions
   - Index expressions, field access
   - Control flow expressions (`return`, `break`, `continue`)

### Expression Parsing Algorithm

The core expression parsing algorithm works as follows:

1. **Parse Primary Expression**: Parse the leftmost part of the expression
2. **Parse Postfix Operations**: Handle postfix operations (field access, method calls, indexing)
3. **Parse Infix Operations**: Handle infix operations with proper precedence
4. **Handle Type Casting**: Process `as` expressions
5. **Handle Assignment**: Process assignment operations

### Special Cases

#### Block Expression Disambiguation

The parser uses a sophisticated approach to handle the ambiguity between block expressions and expressions without blocks:

```java
public ExprNode parseExprNode(int precedence) {
    int startPos = i; // Save starting position for backtracking
    token_t token = tokens.get(i);
    
    // Check if it could be an ExprWithBlockNode
    if (token.name.equals("if") || token.name.equals("while") || 
        token.name.equals("loop") || token.name.equals("{")) {
        // Try to parse as ExprWithBlockNode
        ExprWithBlockNode withBlockNode = parseExprWithBlockNode();
        
        // Check the next token to determine if this is part of a larger expression
        if (i < tokens.size()) {
            token_t nextToken = tokens.get(i);
            
            // If the next token is an operator, this block expression is part of a larger expression
            if (isOperatorToken(nextToken.name)) {
                // Backtrack and parse as ExprWithoutBlockNode
                i = startPos;
                return parseExprWithoutBlockNode(precedence);
            }
        }
        
        // Confirm it's an ExprWithBlockNode
        return withBlockNode;
    }
    
    // Default to parsing as ExprWithoutBlockNode
    return parseExprWithoutBlockNode(precedence);
}
```

#### Recursive Depth Protection

The parser includes protection against excessive recursion:

```java
private static final int MAX_RECURSION_DEPTH = 100;
private int recursionDepth = 0;

public ExprWithoutBlockNode parseExprWithoutBlockNode(int precedence) {
    if (recursionDepth > MAX_RECURSION_DEPTH) {
        throw new RuntimeException("Maximum recursion depth exceeded in expression parsing");
    }
    recursionDepth++;
    
    try {
        // Parsing logic...
    } finally {
        recursionDepth--;
    }
}
```

## Statement Parsing

### Statement Types

The parser handles several types of statements:

1. **Item Statements**: Function, struct, enum, const, trait, and impl definitions
2. **Let Statements**: Variable bindings with optional type annotations and initial values
3. **Expression Statements**: Expressions followed by semicolons (or not, for block expressions)
4. **Empty Statements**: Just a semicolon

### Statement Parsing Algorithm

The main statement parsing algorithm:

```java
public StmtNode parseStmtNode() {
    if (!(i < tokens.size())) throw new ParseException("No more tokens to parse in statement");
    
    token_t token = tokens.get(i);
    
    // Empty statement handling
    if (token.name.equals(";")) {
        i++; // consume semicolon
        return null; // return null for empty statement
    }
    
    // Let statement handling
    if (token.name.equals("let")) {
        return parseLetStmtNode();
    }
    
    // Item type handling
    if (token.name.equals("fn")) {
        return parseFunctionNode(false);
    }
    // ... other item types
    
    // Expression statement handling (default case)
    return parseExprStmtNode();
}
```

## Item Parsing

### Item Types

The parser handles several types of items:

1. **Functions**: Including regular functions and const functions
2. **Structs**: With optional fields or unit structs
3. **Enums**: With variant lists
4. **Constants**: With type annotations and optional initial values
5. **Traits**: With associated items
6. **Implementations**: Both inherent implementations and trait implementations

### Special Cases

#### Const Function vs. Const Item

The parser distinguishes between const functions and const items:

```java
if (token.name.equals("const")) {
    // Need to check next token to determine if it's a const item or const function
    if (i + 1 < tokens.size() && tokens.get(i + 1).name.equals("fn")) {
        // Const function
        i++; // consume const
        return parseFunctionNode(true);
    } else {
        // Regular const item
        return parseConstItemNode();
    }
}
```

#### Impl Type Distinction

The parser distinguishes between inherent implementations and trait implementations:

```java
// Check if it's an inherent impl or a trait impl
if (i < tokens.size() && isIdentifier(tokens.get(i))) {
    // Look ahead to see if there is a "for" token
    if (i + 1 < tokens.size() && tokens.get(i + 1).name.equals("for")) {
        // It's a trait impl
        node.trait = parseIdentifierNode();
        i++; // consume for
        node.typeName = parseTypeExprNode();
    } else {
        // It's an inherent impl
        node.trait = null;
        node.typeName = parseTypeExprNode();
    }
}
```

## Type System

### Type Categories

The parser handles several categories of types:

1. **Path Types**: Simple type names like `i32`, `String`, etc.
2. **Reference Types**: References like `&T` or `&mut T`
3. **Array Types**: Arrays with specified size like `[T; N]`
4. **Unit Types**: The empty tuple type `()`

### Type Parsing Algorithm

The type parsing algorithm:

```java
public TypeExprNode parseTypeExprNode() {
    if (i < tokens.size() && tokens.get(i).name.equals("&")) {
        return parseTypeRefExprNode();
    } else if (i < tokens.size() && tokens.get(i).name.equals("[")) {
        return parseTypeArrayExprNode();
    } else if (i < tokens.size() && tokens.get(i).name.equals("(")) {
        // it's a unit type
        return parseTypeUnitExprNode();
    } else {
        // it's a type path
        return parseTypePathExprNode();
    }
}
```

## Error Handling

### Exception Types

The parser uses custom exceptions for error handling:

1. **ParseException**: General parsing errors with descriptive messages
2. **TokenizerException**: Errors from the tokenizer phase

### Error Reporting

The parser provides detailed error messages including:

- Expected tokens or constructs
- Actual tokens encountered
- Location information (when available from the tokenizer)

### Error Recovery

The parser implements limited error recovery:

- **Synchronization Points**: Uses statement boundaries as synchronization points
- **Panic Mode**: In case of severe errors, the parser can enter panic mode to skip to the next synchronization point

## Performance Considerations

### Optimization Strategies

The parser implements several optimization strategies:

1. **Minimal Backtracking**: Only backtracks when absolutely necessary
2. **Operator Precedence Table**: Uses a lookup table for operator precedence
3. **Keyword Set**: Uses a HashSet for efficient keyword checking
4. **Recursion Depth Limiting**: Prevents stack overflow in pathological cases

### Memory Management

The parser manages memory efficiently by:

- Reusing AST node objects where possible
- Using Vector collections with appropriate initial capacities
- Limiting the size of recursive call stacks

## Future Enhancements

### Potential Improvements

1. **Better Error Recovery**: Implement more sophisticated error recovery strategies
2. **Parse Tree Caching**: Cache parsed subtrees for incremental parsing
3. **Syntax Extensions**: Support for additional Rust language features
4. **Performance Profiling**: Add performance profiling to identify bottlenecks
5. **Parallel Parsing**: Explore possibilities for parallel parsing of independent code units

### Language Extensions

The parser could be extended to support:

1. **Macros**: Support for macro definitions and expansion
2. **Attributes**: Support for attribute parsing
3. **Generics**: Support for generic type parameters
4. **Lifetimes**: Support for lifetime annotations
5. **Modules**: Support for module declarations and use statements

## Conclusion

The RCompiler parser implements a sophisticated recursive descent parser with operator precedence parsing and sophisticated expression disambiguation. It handles the full range of Rust language constructs currently supported by the compiler and provides a solid foundation for future extensions.

The parser's design emphasizes modularity, maintainability, and performance, making it well-suited for both educational purposes and practical compiler development.

## Improved Parser Architecture

### Current Issues with Monolithic Design

The current parser implementation has several structural issues that impact maintainability and extensibility:

1. **Single Large Class**: The `Parser` class contains over 1500 lines of code, violating the Single Responsibility Principle
2. **Tight Coupling**: All parsing logic is tightly coupled within a single class, making it difficult to modify or extend specific parsing components
3. **Poor Testability**: The monolithic structure makes it difficult to test individual parsing components in isolation
4. **Code Duplication**: Similar parsing patterns are repeated across different methods
5. **Limited Extensibility**: Adding new language features requires modifying the core parser class
6. **Complex Error Handling**: Error handling logic is scattered throughout the parser

### Proposed Modular Architecture

To address these issues, we propose a modular parser architecture based on the following principles:

1. **Separation of Concerns**: Different parsing responsibilities are separated into distinct classes
2. **Interface-Based Design**: Use interfaces to define contracts between components
3. **Factory Pattern**: Use factories to create and configure parser components
4. **Strategy Pattern**: Use different parsing strategies for different language constructs
5. **Visitor Pattern**: Use visitors for AST processing and transformation

#### Core Architecture Components

```
┌─────────────────────────────────────────────────────────────┐
│                    ParserFacade                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │  TokenStream    │  │  ParseContext   │  │ ErrorReporter│ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   ParserFactory                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │ExpressionParser │  │ StatementParser │  │  TypeParser  │ │
│  │    Factory      │  │    Factory      │  │   Factory    │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                Specialized Parsers                          │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │ExpressionParser │  │ StatementParser │  │  TypeParser  │ │
│  │                 │  │                 │  │              │ │
│  │ • PrattParser   │  │ • LetParser     │  │ • PathParser │ │
│  │ • LiteralParser │  │ • ItemParser    │  │ • RefParser  │ │
│  │ • PathParser    │  │ • ExprParser    │  │ • ArrayParser│ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

#### Core Infrastructure

**1. ParseContext**
```java
public class ParseContext {
    private final TokenStream tokenStream;
    private final ErrorReporter errorReporter;
    private final SymbolTable symbolTable;
    private final ParserConfiguration config;
    
    // Provides shared state and services to all parser components
}
```

**2. TokenStream**
```java
public class TokenStream {
    private final List<token_t> tokens;
    private int position;
    private final Stack<Integer> positionStack;
    
    // Manages token navigation with backtracking support
}
```

**3. ErrorReporter**
```java
public class ErrorReporter {
    private final List<ParseError> errors;
    private final ErrorRecoveryStrategy recoveryStrategy;
    
    // Centralized error handling and recovery
}
```

#### Expression Parsing with Pratt Parser

The new architecture uses a Pratt Parser (Top-Down Operator Precedence Parser) for expression parsing:

```java
public class PrattExpressionParser implements ExpressionParser {
    private final Map<String, PrefixParselet> prefixParselets;
    private final Map<String, InfixParselet> infixParselets;
    
    public ExprNode parse(int precedence) {
        token_t token = tokenStream.consume();
        PrefixParselet prefix = prefixParselets.get(token.name);
        
        if (prefix == null) {
            errorReporter.reportError("Expected expression", token);
            return null;
        }
        
        ExprNode left = prefix.parse(this, token);
        
        while (precedence < getPrecedence()) {
            token = tokenStream.peek();
            InfixParselet infix = infixParselets.get(token.name);
            
            if (infix == null) break;
            
            tokenStream.consume();
            left = infix.parse(this, left, token);
        }
        
        return left;
    }
}
```

#### Parser Factory Pattern

```java
public class ParserFactory {
    public ExpressionParser createExpressionParser(ParseContext context) {
        PrattExpressionParser parser = new PrattExpressionParser(context);
        
        // Register literal parselets
        parser.registerPrefixParselet(TokenType.INTEGER_LITERAL, new LiteralParselet());
        parser.registerPrefixParselet(TokenType.STRING_LITERAL, new LiteralParselet());
        
        // Register prefix operators
        parser.registerPrefixParselet("-", new PrefixOperatorParselet(Precedence.PREFIX));
        parser.registerPrefixParselet("!", new PrefixOperatorParselet(Precedence.PREFIX));
        parser.registerPrefixParselet("&", new PrefixOperatorParselet(Precedence.PREFIX));
        
        // Register infix operators
        parser.registerInfixParselet("+", new BinaryOperatorParselet(Precedence.ADDITIVE));
        parser.registerInfixParselet("*", new BinaryOperatorParselet(Precedence.MULTIPLICATIVE));
        
        return parser;
    }
}
```

#### Specialized Parsers

**1. Statement Parser**
```java
public class StatementParser {
    private final Map<String, StatementParselet> statementParselets;
    
    public StmtNode parseStatement() {
        token_t token = tokenStream.peek();
        StatementParselet parselet = statementParselets.get(token.name);
        
        if (parselet != null) {
            return parselet.parse(context);
        }
        
        // Default to expression statement
        return parseExpressionStatement();
    }
}
```

**2. Type Parser**
```java
public class TypeParser {
    public TypeExprNode parseType() {
        token_t token = tokenStream.peek();
        
        if (token.name.equals("&")) {
            return parseReferenceType();
        } else if (token.name.equals("[")) {
            return parseArrayType();
        } else if (token.name.equals("(")) {
            return parseUnitType();
        } else {
            return parsePathType();
        }
    }
}
```

### Benefits of the New Architecture

1. **Improved Maintainability**: Each parser component has a single responsibility
2. **Better Testability**: Individual components can be tested in isolation
3. **Enhanced Extensibility**: New language features can be added by creating new parselets
4. **Reduced Coupling**: Components interact through well-defined interfaces
5. **Code Reusability**: Common parsing patterns can be reused across different components
6. **Better Error Handling**: Centralized error reporting and recovery
7. **Easier Debugging**: Issues can be isolated to specific components

### Migration Strategy

To migrate from the current monolithic parser to the new modular architecture:

1. **Phase 1**: Create the core infrastructure (ParseContext, TokenStream, ErrorReporter)
2. **Phase 2**: Implement the Pratt expression parser alongside the current expression parser
3. **Phase 3**: Gradually migrate statement parsing to the new architecture
4. **Phase 4**: Migrate type and pattern parsing
5. **Phase 5**: Remove the old monolithic parser code

### Example: Improved Expression Parsing

With the new architecture, expression parsing becomes much more modular:

```java
// Register parselets for different expression types
parser.registerPrefixParselet("if", new IfExpressionParselet());
parser.registerPrefixParselet("while", new WhileExpressionParselet());
parser.registerPrefixParselet("{", new BlockExpressionParselet());
parser.registerPrefixParselet("(", new GroupExpressionParselet());

// Register infix operators with proper precedence
parser.registerInfixParselet(".", new MemberAccessParselet(Precedence.MEMBER));
parser.registerInfixParselet("(", new FunctionCallParselet(Precedence.CALL));
parser.registerInfixParselet("[", new IndexAccessParselet(Precedence.CALL));
parser.registerInfixParselet("as", new TypeCastParselet(Precedence.TYPE_CAST));
```

This approach makes it easy to add new expression types or modify existing ones without affecting other parts of the parser.

### Conclusion

The proposed modular architecture addresses the current parser's structural issues while maintaining compatibility with the existing AST structure. The design follows established compiler construction patterns and provides a solid foundation for future extensions to the Rust language support in RCompiler.