### How to parse `StmtNode`
- The grammar rule for `StmtNode` is:
  ```
  <stmt> = <letstmt> | <exprstmt> | <item>
  ```
- There are three kinds of statements: let statements, expression statements and item statements.
- The strategy to parse a statement:
  - first we check the first token of the statement:
    - if it's "let", it's a let statement;
    - if it's "fn", it's a function item;
    - if it's "struct", it's a struct item;
    - if it's "enum", it's an enum item;
    - if it's "const", it's a constant item;
    - if it's "trait", it's a trait item;
    - if it's "impl", it's an impl item;
    - otherwise, it's an expression statement.
  - then we call the corresponding parse method to parse the statement.
- The parse methods for each kind of statement are implemented in `Parser.java`.