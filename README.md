## RCompiler

- This project is for Rust language simulation compiler.

### Features:
- lexical analysis
- syntax analysis
- semantic analysis
- intermediate code generation
- optimization
- target code generation 

### Syntax Analysis
- we use AST to represent the syntax tree
- we use recursive descent parsing to parse the source code

#### AST Node Types:
- the types are defined in `AST.java`
- the root type is `ASTNode`
- the highest derived type is `StmtNode`
- the `StmtNode` has three derived types: `Itemnode`, `LetStmtNode`, and `ExprStmtNode`.

##### `ItemNode`:
- represents a top-level item in Rust, such as a function or a struct.
- has derived types for all kinds of items:
    - `FunctionNode`: represents a function item.
    - `StructNode`: represents a struct item.
    - `EnumNode`: represents an enum item.
    - `ConstItemNode`: represents a constant item.
    - `TraitNode`: represents a trait item.
    - `ImplNode`: represents an impl item.
    - `AssoItemNode`: represents an associated item in a trait or impl.

##### `LetStmtNode`:
- represents a let statement.

##### `ExprStmtNode`:
- represents an expression statement.
- there are two kinds of expressions: `ExprWithBlockNode` and `ExprWithoutBlockNode`.
- `ExprWithBlockNode`: represents an expression that contains a block. In this compiler, we only consider block expressions, if expressions and loop expressions.
    - `BlockExprNode`: represents a block expression.
    - `IfExprNode`: represents an if expression.
    - `LoopExprNode`: represents a loop expression.
- `ExprWithoutBlockNode`: represents an expression that does not contain a block. There are many kinds of expressions:
    - `LiteralExprNode`: represents a literal expression.
    - `PathExprNode`: represents a path expression.
    - `OperExprNode`: represents an operator expression.
    - `ArrayExprNode`: represents an array expression.
    - `IndexExprNode`: represents an index expression.
    - `StructExprNode`: represents a struct expression.
    - `CallExprNode`: represents a function call expression.
    - `MethodCallExprNode`: represents a method call expression.
    - `FieldExprNode`: represents a field access expression.
    - `ContinueExprNode`: represents a continue expression.
    - `BreakExprNode`: represents a break expression.
    - `ReturnExprNode`: represents a return expression.
    - `UnderscoreExprNode`: represents an underscore expression.
- Among these types, `OperExprNode` is the most complex one. In this compiler, we consider the following operators:
    - borrowing operators: & &mut
    - dereferencing operator: *
    - negation operator: - !
    - arithmetic operators: + - * / %
    - bitwise operators: & | ^ << >>
    - comparison operators: == != > < >= <=
    - logical operators: && || 
    - assignment operators: = 
    - compound assignment operators: += -= *= /= %= &= |= ^= <<= >>=
    - type casting operator: as
- all these kinds of operator expression are represented by a derive type of `OperExprNode`.
    

##### Other Kinds of Nodes:
- `TypeNode` is used to represent a type in Rust. In RCompiler, we consider four kinds of types:
    - `TypePathExprNode`: represents a type that is a path.
    - `TypeRefExprNode`: represents a reference type.
    - `TypeArrayExprNode`: represents an array type.
    - `TypeUnitExprNode`: represents the unit type `()`.
- `PatternNode` is used to represent a pattern in Rust. In RCompiler, we consider three kinds of patterns:
    - `IdPatNode`: represents an identifier pattern.
    - `WildPatNode`: represents a wildcard pattern `_`.
    - `RefPatNode`: represents a reference pattern, of which the syntax is `(& | &&) mut? pattern`.
- `IdentifierNode` is used to represent an identifier in Rust.

#### Parsing Strategy:
- the parser is implemented in `Parser.java`.
- in `Parser.java`: the class `Parser` reads tokens from the lexer and has a method `parse()` which is the entry point of the parser. It returns a vector of `StmtNode`, which represents the top-level statements in the source code.
- the parser uses recursive descent parsing to parse the source code. Specifically, it has a method for each kind of node in the AST. For example, it has a overloaded method `parse` for each derived type of `ASTNode`, such as `parse(FunctionNode node)`, `parse(StructNode node)`, `parse(LetStmtNode node)`, `parse(ExprStmtNode node)`, etc.
- the parser decides which method to call according to the current tokens and the grammar provided in `AST.java`.
