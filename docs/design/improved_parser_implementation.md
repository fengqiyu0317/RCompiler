# 改进的Parser实现结构

## 当前Parser结构概述

当前的`Parser.java`文件包含约1588行代码，主要结构如下：

```java
public class Parser {
    // 核心字段
    private Vector<token_t> tokens;
    int i = 0;
    private static final int MAX_RECURSION_DEPTH = 100;
    private int recursionDepth = 0;
    private static final Set<String> keywords;
    Vector<StmtNode> statements;
    
    // 主要解析方法（50+个方法）：
    // - 语句解析：parseStmtNode, parseLetStmtNode等
    // - 项解析：parseFunctionNode, parseStructNode等
    // - 表达式解析：parseExprNode, parseExprWithBlockNode等
    // - 类型解析：parseTypeExprNode, parseTypePathExprNode等
    // - 模式解析：parsePatternNode, parseIdPatNode等
    // - 工具方法：isOper, getPrecedence, isIdentifier等
}
```

## 识别的主要问题

1. **单体设计**：单个类包含50+个方法，违反单一职责原则
2. **紧耦合**：所有解析逻辑紧密耦合在一个类中
3. **可测试性差**：难以单独测试各个解析组件
4. **代码重复**：相似解析模式在不同方法中重复
5. **可扩展性有限**：添加新语言特性需要修改核心解析器类
6. **错误处理复杂**：错误处理逻辑分散在整个解析器中

## 解析器间的依赖关系分析

通过分析当前代码，发现不同类型的解析器之间存在大量相互依赖：

### 1. 语句解析器对其他解析器的依赖
- `parseLetStmtNode()` 依赖 `parsePatternNode()`, `parseTypeExprNode()`, `parseExprNode()`
- `parseFunctionNode()` 依赖 `parseIdentifierNode()`, `parseSelfParaNode()`, `parseParameterNode()`, `parseTypeExprNode()`, `parseBlockExprNode()`
- `parseStructNode()` 依赖 `parseIdentifierNode()`, `parseFieldNode()`
- `parseFieldNode()` 依赖 `parseIdentifierNode()`, `parseTypeExprNode()`
- `parseEnumNode()` 依赖 `parseIdentifierNode()`
- `parseConstItemNode()` 依赖 `parseIdentifierNode()`, `parseTypeExprNode()`, `parseExprNode()`
- `parseTraitNode()` 依赖 `parseIdentifierNode()`, `parseAssoItemNode()`
- `parseImplNode()` 依赖 `parseIdentifierNode()`, `parseTypeExprNode()`, `parseAssoItemNode()`
- `parseAssoItemNode()` 依赖 `parseFunctionNode()`, `parseConstItemNode()`

### 2. 表达式解析器对其他解析器的依赖
- `parseExprWithoutBlockNode()` 依赖 `parseExprWithBlockNode()`, `parseLiteralExprNode()`, `parsePathExprNode()`, `parseArrayExprNode()`, `parseGroupExprNode()`, `parseBorrowExprNode()`, `parseDerefExprNode()`, `parseNegaExprNode()`, `parseContinueExprNode()`, `parseBreakExprNode()`, `parseReturnExprNode()`, `parseUnderscoreExprNode()`
- `parseExprWithBlockNode()` 依赖 `parseIfExprNode()`, `parseLoopExprNode()`, `parseBlockExprNode()`
- `parseIfExprNode()` 依赖 `parseGroupExprNode()`, `parseBlockExprNode()`
- `parseLoopExprNode()` 依赖 `parseGroupExprNode()`, `parseBlockExprNode()`
- `parseBlockExprNode()` 依赖 `parseStmtNode()`, `parseExprWithoutBlockNode()`
- `parseArrayExprNode()` 依赖 `parseExprNode()`
- `parseStructExprNode()` 依赖 `parsePathExprSegNode()`, `parseFieldValNode()`
- `parseFieldValNode()` 依赖 `parseIdentifierNode()`, `parseExprNode()`
- `parseCallExprNode()` 依赖 `parseExprNode()`
- `parseMethodCallExprNode()` 依赖 `parseExprNode()`, `parsePathExprSegNode()`
- `parseFieldExprNode()` 依赖 `parseExprNode()`, `parseIdentifierNode()`

### 3. 类型解析器对其他解析器的依赖
- `parseTypeRefExprNode()` 依赖 `parseTypeExprNode()`
- `parseTypeArrayExprNode()` 依赖 `parseTypeExprNode()`, `parseExprNode()`

### 4. 模式解析器对其他解析器的依赖
- `parseRefPatNode()` 依赖 `parsePatternNode()`
- `parseIdPatNode()` 依赖 `parseIdentifierNode()`

## 改进的模块化架构

考虑到这些复杂的依赖关系，我们设计一个更加合理的模块化架构：

### 1. 核心共享组件

```java
// 令牌流管理
public class TokenStream {
    private final Vector<token_t> tokens;
    private int position;
    
    public TokenStream(Vector<token_t> tokens) {
        this.tokens = tokens;
        this.position = 0;
    }
    
    public token_t current() { return tokens.get(position); }
    public token_t peek() { return position + 1 < tokens.size() ? tokens.get(position + 1) : null; }
    public token_t consume() { return tokens.get(position++); }
    public boolean isAtEnd() { return position >= tokens.size(); }
    public boolean matches(String tokenName) {
        if (current().name.equals(tokenName)) {
            position++;
            return true;
        }
        return false;
    }
    
    // 位置管理（用于回溯）
    public void pushPosition() { /* 实现 */ }
    public void popPosition() { /* 实现 */ }
    public void commitPosition() { /* 实现 */ }
}

// 错误报告器
public class ErrorReporter {
    private final List<String> errors;
    
    public ErrorReporter() {
        this.errors = new ArrayList<>();
    }
    
    public void reportError(String message, token_t token) {
        errors.add("Error at token '" + token.name + "': " + message);
    }
    
    public boolean hasErrors() { return !errors.isEmpty(); }
    public List<String> getErrors() { return errors; }
}

// 解析器配置
public class ParserConfiguration {
    private final Set<String> keywords;
    
    public ParserConfiguration() {
        this.keywords = new HashSet<>(Arrays.asList(
            "as", "break", "const", "continue", "crate", "else", "enum", "extern", "false", "fn", "for", "if", "impl", "in", 
            "let", "loop", "match", "mod", "move", "mut", "ref", "return", "self", "Self", "static", "struct", 
            "super", "trait", "true", "type", "unsafe", "use", "where", "while"
        ));
    }
    
    public boolean isKeyword(String tokenName) {
        return keywords.contains(tokenName);
    }
    
    public boolean isIdentifier(token_t token) {
        return token.tokentype == token_t.TokenType_t.IDENTIFIER_OR_KEYWORD && !isKeyword(token.name);
    }
}
```

### 2. 解析器基类和接口

```java
// 解析器基类
public abstract class BaseParser {
    protected final TokenStream tokenStream;
    protected final ErrorReporter errorReporter;
    protected final ParserConfiguration config;
    
    public BaseParser(TokenStream tokenStream, ErrorReporter errorReporter, ParserConfiguration config) {
        this.tokenStream = tokenStream;
        this.errorReporter = errorReporter;
        this.config = config;
    }
    
    // 通用的辅助方法
    protected IdentifierNode parseIdentifier() {
        if (!config.isIdentifier(tokenStream.current())) {
            errorReporter.reportError("Expected identifier", tokenStream.current());
        }
        IdentifierNode node = new IdentifierNode();
        node.name = tokenStream.consume().name;
        return node;
    }
    
    protected boolean isOperatorToken(String tokenName) {
        return tokenName.equals("[") || tokenName.equals(".") || tokenName.equals("(") ||
               tokenName.equals("::") || tokenName.equals("&") || tokenName.equals("*") ||
               tokenName.equals("!") || tokenName.equals("-") || tokenName.equals("as") ||
               tokenName.equals("=") || tokenName.equals("+=") || tokenName.equals("-=") ||
               tokenName.equals("*=") || tokenName.equals("/=") || tokenName.equals("%=") ||
               tokenName.equals("&=") || tokenName.equals("|=") || tokenName.equals("^=") ||
               tokenName.equals("<<=") || tokenName.equals(">>=") || tokenName.equals("==") ||
               tokenName.equals("!=") || tokenName.equals(">") || tokenName.equals("<") ||
               tokenName.equals(">=") || tokenName.equals("<=") || tokenName.equals("&&") ||
               tokenName.equals("||") || tokenName.equals("+") || tokenName.equals("-") ||
               tokenName.equals("*") || tokenName.equals("/") || tokenName.equals("%") ||
               tokenName.equals("|") || tokenName.equals("^") ||
               tokenName.equals("<<") || tokenName.equals(">>") || tokenName.equals("->");
    }
    
    protected int getPrecedence(token_t token) {
        // 从原Parser类中提取的优先级逻辑
        if (token.name.equals("*") || token.name.equals("/") || token.name.equals("%")) {
            return 110;
        }
        if (token.name.equals("+") || token.name.equals("-")) {
            return 100;
        }
        // ... 其他优先级规则
        return 0;
    }
    
    protected boolean isArithmeticOperator(String name) {
        return name.equals("+") || name.equals("-") || name.equals("*") || 
               name.equals("/") || name.equals("%") || name.equals("&") || 
               name.equals("|") || name.equals("^") || name.equals("<<") || 
               name.equals(">>");
    }
    
    protected boolean isComparisonOperator(String name) {
        return name.equals("==") || name.equals("!=") || name.equals("<") || 
               name.equals("<=") || name.equals(">") || name.equals(">=");
    }
    
    protected boolean isAssignmentOperator(String name) {
        return name.equals("=") || name.equals("+=") || name.equals("-=") || 
               name.equals("*=") || name.equals("/=") || name.equals("%=") || 
               name.equals("&=") || name.equals("|=") || name.equals("^=") || 
               name.equals("<<=") || name.equals(">>=");
    }
    
    protected oper_t getOperator(String name) {
        // 从原Parser类中提取的操作符映射逻辑
        switch (name) {
            case "+": return oper_t.PLUS;
            case "-": return oper_t.MINUS;
            case "*": return oper_t.MUL;
            // ... 其他操作符映射
            default: throw new ParseException("Unknown operator: " + name);
        }
    }
}
```

### 3. 具体解析器实现

```java
// 表达式解析器
public class ExpressionParser extends BaseParser {
    private int recursionDepth = 0;
    private static final int MAX_RECURSION_DEPTH = 100;
    
    public ExpressionParser(TokenStream tokenStream, ErrorReporter errorReporter, ParserConfiguration config) {
        super(tokenStream, errorReporter, config);
    }
    
    // 主表达式解析方法
    public ExprNode parse() {
        return parseExpression(0);
    }
    
    // 带优先级的表达式解析
    public ExprNode parseExpression(int precedence) {
        if (recursionDepth > MAX_RECURSION_DEPTH) {
            throw new RuntimeException("Maximum recursion depth exceeded in expression parsing");
        }
        recursionDepth++;
        
        try {
            ExprNode left = parsePrimary();
            
            while (precedence < getPrecedence(tokenStream.current())) {
                token_t token = tokenStream.consume();
                left = parseInfixExpression(left, token);
            }
            
            return left;
        } finally {
            recursionDepth--;
        }
    }
    
    // 解析基本表达式
    private ExprNode parsePrimary() {
        token_t token = tokenStream.current();
        
        if (token.tokentype == token_t.TokenType_t.INTEGER_LITERAL ||
            token.tokentype == token_t.TokenType_t.STRING_LITERAL ||
            token.name.equals("true") || token.name.equals("false")) {
            return parseLiteral();
        } else if (token.name.equals("if") || token.name.equals("while") || 
                   token.name.equals("loop") || token.name.equals("{")) {
            return parseExpressionWithBlock();
        } else if (token.name.equals("(")) {
            return parseGroupExpression();
        } else if (token.name.equals("&") || token.name.equals("*") || 
                   token.name.equals("-") || token.name.equals("!")) {
            return parsePrefixExpression();
        } else if (config.isIdentifier(token) || token.name.equals("self") || token.name.equals("Self")) {
            return parsePathExpression();
        } else if (token.name.equals("[")) {
            return parseArrayExpression();
        } else if (token.name.equals("continue")) {
            return parseContinueExpression();
        } else if (token.name.equals("break")) {
            return parseBreakExpression();
        } else if (token.name.equals("return")) {
            return parseReturnExpression();
        } else if (token.name.equals("_")) {
            return parseUnderscoreExpression();
        } else {
            errorReporter.reportError("Unexpected token in expression", token);
            return null;
        }
    }
    
    // 解析中缀表达式
    private ExprNode parseInfixExpression(ExprNode left, token_t operator) {
        int precedence = getPrecedence(operator);
        ExprNode right = parseExpression(precedence);
        
        if (isArithmeticOperator(operator.name)) {
            ArithExprNode node = new ArithExprNode();
            node.left = left;
            node.right = right;
            node.operator = getOperator(operator.name);
            return node;
        } else if (isComparisonOperator(operator.name)) {
            CompExprNode node = new CompExprNode();
            node.left = left;
            node.right = right;
            node.operator = getOperator(operator.name);
            return node;
        } else if (isAssignmentOperator(operator.name)) {
            AssignExprNode node = new AssignExprNode();
            node.left = left;
            node.right = right;
            node.operator = getOperator(operator.name);
            return node;
        } else if (operator.name.equals(".")) {
            return parseMemberAccess(left);
        } else if (operator.name.equals("(")) {
            return parseFunctionCall(left);
        } else if (operator.name.equals("[")) {
            return parseIndexAccess(left);
        } else if (operator.name.equals("as")) {
            return parseTypeCast(left);
        } else if (operator.name.equals("::")) {
            return parsePathExtension(left);
        }
        
        return left;
    }
    
    // 具体表达式解析方法
    private LiteralExprNode parseLiteral() { /* 从原Parser类中提取 */ }
    private ExprWithBlockNode parseExpressionWithBlock() { /* 从原Parser类中提取 */ }
    private GroupExprNode parseGroupExpression() { /* 从原Parser类中提取 */ }
    private ExprNode parsePrefixExpression() { /* 从原Parser类中提取 */ }
    private PathExprNode parsePathExpression() { /* 从原Parser类中提取 */ }
    private ArrayExprNode parseArrayExpression() { /* 从原Parser类中提取 */ }
    private ContinueExprNode parseContinueExpression() { /* 从原Parser类中提取 */ }
    private BreakExprNode parseBreakExpression() { /* 从原Parser类中提取 */ }
    private ReturnExprNode parseReturnExpression() { /* 从原Parser类中提取 */ }
    private UnderscoreExprNode parseUnderscoreExpression() { /* 从原Parser类中提取 */ }
    
    private ExprNode parseMemberAccess(ExprNode left) { /* 从原Parser类中提取 */ }
    private CallExprNode parseFunctionCall(ExprNode left) { /* 从原Parser类中提取 */ }
    private IndexExprNode parseIndexAccess(ExprNode left) { /* 从原Parser类中提取 */ }
    private TypeCastExprNode parseTypeCast(ExprNode left) { /* 从原Parser类中提取 */ }
    private ExprNode parsePathExtension(ExprNode left) { /* 从原Parser类中提取 */ }
}

// 语句解析器
public class StatementParser extends BaseParser {
    private final ExpressionParser expressionParser;
    private final TypeParser typeParser;
    private final PatternParser patternParser;
    
    public StatementParser(TokenStream tokenStream, ErrorReporter errorReporter, 
                       ParserConfiguration config, ExpressionParser expressionParser,
                       TypeParser typeParser, PatternParser patternParser) {
        super(tokenStream, errorReporter, config);
        this.expressionParser = expressionParser;
        this.typeParser = typeParser;
        this.patternParser = patternParser;
    }
    
    // 主语句解析方法
    public StmtNode parseStatement() {
        // 处理空语句
        if (tokenStream.matches(";")) {
            return null;
        }
        
        token_t token = tokenStream.current();
        
        // 根据关键字分发到具体解析方法
        if (token.name.equals("let")) {
            return parseLetStatement();
        } else if (token.name.equals("fn")) {
            return parseFunctionStatement();
        } else if (token.name.equals("struct")) {
            return parseStructStatement();
        } else if (token.name.equals("enum")) {
            return parseEnumStatement();
        } else if (token.name.equals("const")) {
            return parseConstStatement();
        } else if (token.name.equals("trait")) {
            return parseTraitStatement();
        } else if (token.name.equals("impl")) {
            return parseImplStatement();
        } else {
            // 默认为表达式语句
            return parseExpressionStatement();
        }
    }
    
    // 具体语句解析方法
    private LetStmtNode parseLetStatement() {
        tokenStream.consume("let");
        
        LetStmtNode node = new LetStmtNode();
        node.name = patternParser.parsePattern();
        
        if (!tokenStream.matches(":")) {
            errorReporter.reportError("Expected ':' after pattern", tokenStream.current());
        }
        
        node.type = typeParser.parseType();
        
        if (tokenStream.matches("=")) {
            node.value = expressionParser.parse();
        } else {
            node.value = null;
        }
        
        if (!tokenStream.matches(";")) {
            errorReporter.reportError("Expected ';' at end of let statement", tokenStream.current());
        }
        
        return node;
    }
    
    private FunctionNode parseFunctionStatement() {
        boolean isConst = false;
        if (tokenStream.current().name.equals("const") && 
            tokenStream.peek().name.equals("fn")) {
            tokenStream.consume("const");
            isConst = true;
        }
        
        tokenStream.consume("fn");
        
        FunctionNode node = new FunctionNode();
        node.isConst = isConst;
        node.name = parseIdentifier();
        
        if (!tokenStream.matches("(")) {
            errorReporter.reportError("Expected '(' after function name", tokenStream.current());
        }
        
        node.selfPara = parseSelfParameter();
        node.parameters = parseParameterList();
        
        if (!tokenStream.matches(")")) {
            errorReporter.reportError("Expected ')' at end of parameter list", tokenStream.current());
        }
        
        if (tokenStream.matches("->")) {
            node.returnType = typeParser.parseType();
        } else {
            node.returnType = null;
        }
        
        if (tokenStream.current().name.equals("{")) {
            node.body = (BlockExprNode) expressionParser.parseExpressionWithBlock();
        } else if (tokenStream.matches(";")) {
            node.body = null;
        } else {
            errorReporter.reportError("Expected '{' or ';' after function signature", tokenStream.current());
        }
        
        return node;
    }
    
    private StructNode parseStructStatement() { /* 从原Parser类中提取 */ }
    private EnumNode parseEnumStatement() { /* 从原Parser类中提取 */ }
    private ConstItemNode parseConstStatement() { /* 从原Parser类中提取 */ }
    private TraitNode parseTraitStatement() { /* 从原Parser类中提取 */ }
    private ImplNode parseImplStatement() { /* 从原Parser类中提取 */ }
    
    private ExprStmtNode parseExpressionStatement() {
        ExprNode expr = expressionParser.parse();
        
        // 检查是否是带块的表达式（不需要分号）
        if (expr instanceof ExprWithBlockNode) {
            ExprStmtNode stmt = new ExprStmtNode();
            stmt.expr = expr;
            return stmt;
        }
        
        // 其他表达式需要分号
        if (!tokenStream.matches(";")) {
            errorReporter.reportError("Expected ';' after expression", tokenStream.current());
        }
        
        ExprStmtNode stmt = new ExprStmtNode();
        stmt.expr = expr;
        return stmt;
    }
    
    // 辅助解析方法
    private SelfParaNode parseSelfParameter() { /* 从原Parser类中提取 */ }
    private Vector<ParameterNode> parseParameterList() { /* 从原Parser类中提取 */ }
    private ParameterNode parseParameter() { /* 从原Parser类中提取 */ }
}

// 类型解析器
public class TypeParser extends BaseParser {
    private final ExpressionParser expressionParser;
    
    public TypeParser(TokenStream tokenStream, ErrorReporter errorReporter, 
                    ParserConfiguration config, ExpressionParser expressionParser) {
        super(tokenStream, errorReporter, config);
        this.expressionParser = expressionParser;
    }
    
    // 主类型解析方法
    public TypeExprNode parseType() {
        token_t token = tokenStream.current();
        
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
    
    // 具体类型解析方法
    private TypeRefExprNode parseReferenceType() {
        tokenStream.consume("&");
        
        TypeRefExprNode node = new TypeRefExprNode();
        node.isMutable = tokenStream.matches("mut");
        node.innerType = parseType();
        
        return node;
    }
    
    private TypeArrayExprNode parseArrayType() {
        tokenStream.consume("[");
        
        TypeArrayExprNode node = new TypeArrayExprNode();
        node.elementType = parseType();
        
        if (!tokenStream.matches(";")) {
            errorReporter.reportError("Expected ';' in array type", tokenStream.current());
        }
        
        node.size = expressionParser.parse();
        
        if (!tokenStream.matches("]")) {
            errorReporter.reportError("Expected ']' at end of array type", tokenStream.current());
        }
        
        return node;
    }
    
    private TypeUnitExprNode parseUnitType() {
        tokenStream.consume("(");
        
        if (!tokenStream.matches(")")) {
            errorReporter.reportError("Expected ')' at end of unit type", tokenStream.current());
        }
        
        return new TypeUnitExprNode();
    }
    
    private TypePathExprNode parsePathType() {
        TypePathExprNode node = new TypePathExprNode();
        node.path = parsePathSegment();
        
        if (tokenStream.matches("::")) {
            // 处理限定路径
            // 这里需要额外的实现来处理复杂的路径类型
        }
        
        return node;
    }
    
    private PathExprSegNode parsePathSegment() {
        token_t token = tokenStream.current();
        PathExprSegNode node = new PathExprSegNode();
        
        if (config.isIdentifier(token)) {
            node.name = parseIdentifier();
            node.patternType = patternSeg_t.IDENT;
        } else if (token.name.equals("self")) {
            tokenStream.consume("self");
            node.patternType = patternSeg_t.SELF;
        } else if (token.name.equals("Self")) {
            tokenStream.consume("Self");
            node.patternType = patternSeg_t.SELF_TYPE;
        } else {
            errorReporter.reportError("Expected type name", token);
        }
        
        return node;
    }
}

// 模式解析器
public class PatternParser extends BaseParser {
    public PatternParser(TokenStream tokenStream, ErrorReporter errorReporter, ParserConfiguration config) {
        super(tokenStream, errorReporter, config);
    }
    
    // 主模式解析方法
    public PatternNode parsePattern() {
        if (tokenStream.isAtEnd()) {
            errorReporter.reportError("No more tokens to parse in pattern", null);
            return null;
        }
        
        token_t token = tokenStream.current();
        
        if (token.name.equals("&") || token.name.equals("&&")) {
            return parseReferencePattern();
        } else if (token.name.equals("_")) {
            return parseWildcardPattern();
        } else {
            return parseIdentifierPattern();
        }
    }
    
    // 具体模式解析方法
    private RefPatNode parseReferencePattern() {
        RefPatNode node = new RefPatNode();
        
        if (tokenStream.matches("&&")) {
            node.isDoubleReference = true;
        } else {
            tokenStream.consume("&");
            node.isDoubleReference = false;
        }
        
        node.isMutable = tokenStream.matches("mut");
        node.innerPattern = parsePattern();
        
        return node;
    }
    
    private WildPatNode parseWildcardPattern() {
        tokenStream.consume("_");
        return new WildPatNode();
    }
    
    private IdPatNode parseIdentifierPattern() {
        IdPatNode node = new IdPatNode();
        
        node.isReference = tokenStream.matches("ref");
        node.isMutable = tokenStream.matches("mut");
        
        if (config.isIdentifier(tokenStream.current())) {
            node.name = parseIdentifier();
        } else {
            errorReporter.reportError("Expected identifier in pattern", tokenStream.current());
        }
        
        return node;
    }
}
```

### 4. 主解析器类

```java
public class Parser {
    private final TokenStream tokenStream;
    private final ErrorReporter errorReporter;
    private final ParserConfiguration config;
    private final ExpressionParser expressionParser;
    private final TypeParser typeParser;
    private final PatternParser patternParser;
    private final StatementParser statementParser;
    
    public Parser(Vector<token_t> tokens) {
        this.tokenStream = new TokenStream(tokens);
        this.errorReporter = new ErrorReporter();
        this.config = new ParserConfiguration();
        
        // 创建解析器实例，注意依赖关系
        this.expressionParser = new ExpressionParser(tokenStream, errorReporter, config);
        this.typeParser = new TypeParser(tokenStream, errorReporter, config, expressionParser);
        this.patternParser = new PatternParser(tokenStream, errorReporter, config);
        this.statementParser = new StatementParser(tokenStream, errorReporter, config, 
                                             expressionParser, typeParser, patternParser);
    }
    
    // 主入口方法
    public void parse() {
        Vector<StmtNode> statements = new Vector<>();
        
        while (!tokenStream.isAtEnd()) {
            StmtNode stmt = statementParser.parseStatement();
            if (stmt != null) {
                statements.add(stmt);
            }
        }
        
        this.statements = statements;
    }
    
    public Vector<StmtNode> getStatements() {
        return statements;
    }
    
    public boolean hasErrors() {
        return errorReporter.hasErrors();
    }
    
    public List<String> getErrors() {
        return errorReporter.getErrors();
    }
    
    private Vector<StmtNode> statements;
}
```

## 依赖关系图

```
┌─────────────────┐
│    Parser       │
└─────────┬───────┘
          │
          ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│StatementParser │    │ExpressionParser │    │  TypeParser    │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          │                      │                      │
          ▼                      ▼                      ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│PatternParser  │    │ExpressionParser │    │ExpressionParser │
└─────────────────┘    │(递归调用)      │    │(用于数组大小)    │
                       └─────────────────┘    └─────────────────┘
```

## 迁移策略

### 第一阶段：创建基础组件
1. 创建`TokenStream`、`ErrorReporter`、`ParserConfiguration`类
2. 创建`BaseParser`抽象基类，包含通用解析逻辑

### 第二阶段：实现具体解析器
1. 实现`PatternParser`（依赖最少）
2. 实现`TypeParser`（依赖PatternParser和ExpressionParser）
3. 实现`ExpressionParser`（依赖TypeParser，递归调用自己）
4. 实现`StatementParser`（依赖所有其他解析器）

### 第三阶段：重构主Parser类
1. 简化主`Parser`类，使用新的解析器组件
2. 更新入口点方法
3. 测试确保功能一致性

### 第四阶段：清理和优化
1. 移除原Parser类中的重复代码
2. 优化各组件之间的接口
3. 添加必要的测试

## 改进效果

1. **职责分离**：每个解析器专注于特定类型的解析
2. **依赖管理**：通过构造函数注入明确依赖关系
3. **代码复用**：公共逻辑提取到基类中
4. **易于测试**：可以单独测试每个解析器组件
5. **易于维护**：修改特定解析逻辑只需要修改对应的类
6. **易于扩展**：添加新功能可以通过扩展特定解析器实现

这个改进方案正确处理了解析器之间的复杂依赖关系，通过依赖注入的方式明确了各组件之间的关系，同时保持了原有解析逻辑的核心结构。

## 文件架构

### 目录结构
```
src/main/java/parser/
├── Parser.java                    # 主解析器类（简化版）
├── BaseParser.java               # 解析器基类
├── TokenStream.java              # 令牌流管理
├── ErrorReporter.java            # 错误报告器
├── ParserConfiguration.java      # 解析器配置
├── ExpressionParser.java         # 表达式解析器
├── StatementParser.java         # 语句解析器
├── TypeParser.java              # 类型解析器
├── PatternParser.java            # 模式解析器
└── ParseException.java           # 解析异常类（已存在）
```

### 文件职责说明

#### 1. Parser.java（主解析器）
- **职责**：作为解析系统的入口点，协调各个解析器组件
- **主要方法**：
  - `Parser(Vector<token_t> tokens)`：构造函数，初始化所有解析器组件
  - `parse()`：主解析方法，解析整个令牌流
  - `getStatements()`：获取解析后的语句列表
  - `hasErrors()`：检查是否有解析错误
  - `getErrors()`：获取解析错误列表

#### 2. BaseParser.java（解析器基类）
- **职责**：提供所有解析器的通用功能和工具方法
- **主要方法**：
  - `parseIdentifier()`：解析标识符
  - `isOperatorToken(String)`：判断是否为操作符令牌
  - `getPrecedence(token_t)`：获取操作符优先级
  - `isArithmeticOperator(String)`：判断是否为算术操作符
  - `isComparisonOperator(String)`：判断是否为比较操作符
  - `isAssignmentOperator(String)`：判断是否为赋值操作符
  - `getOperator(String)`：获取操作符枚举值

#### 3. TokenStream.java（令牌流管理）
- **职责**：管理令牌流，提供导航和回溯功能
- **主要方法**：
  - `current()`：获取当前令牌
  - `peek()`：查看下一个令牌
  - `consume()`：消费当前令牌并移动到下一个
  - `isAtEnd()`：检查是否到达令牌流末尾
  - `matches(String)`：检查当前令牌是否匹配并消费
  - `pushPosition()`：保存当前位置（用于回溯）
  - `popPosition()`：恢复到之前保存的位置
  - `commitPosition()`：确认当前位置，清除保存的位置

#### 4. ErrorReporter.java（错误报告器）
- **职责**：集中管理解析错误，提供统一的错误报告接口
- **主要方法**：
  - `reportError(String, token_t)`：报告错误
  - `hasErrors()`：检查是否有错误
  - `getErrors()`：获取所有错误信息

#### 5. ParserConfiguration.java（解析器配置）
- **职责**：管理解析器配置，如关键字集合等
- **主要方法**：
  - `isKeyword(String)`：判断是否为关键字
  - `isIdentifier(token_t)`：判断是否为标识符

#### 6. ExpressionParser.java（表达式解析器）
- **职责**：解析各种类型的表达式，处理操作符优先级
- **主要方法**：
  - `parse()`：解析表达式（入口方法）
  - `parseExpression(int)`：带优先级的表达式解析
  - `parsePrimary()`：解析基本表达式
  - `parseInfixExpression(ExprNode, token_t)`：解析中缀表达式
  - 各种具体表达式解析方法：
    - `parseLiteral()`：字面量
    - `parseExpressionWithBlock()`：带块的表达式
    - `parseGroupExpression()`：分组表达式
    - `parsePrefixExpression()`：前缀表达式
    - `parsePathExpression()`：路径表达式
    - `parseArrayExpression()`：数组表达式
    - `parseMemberAccess(ExprNode)`：成员访问
    - `parseFunctionCall(ExprNode)`：函数调用
    - `parseIndexAccess(ExprNode)`：索引访问
    - `parseTypeCast(ExprNode)`：类型转换

#### 7. StatementParser.java（语句解析器）
- **职责**：解析各种类型的语句
- **主要方法**：
  - `parseStatement()`：解析语句（入口方法）
  - 各种具体语句解析方法：
    - `parseLetStatement()`：let语句
    - `parseFunctionStatement()`：函数定义
    - `parseStructStatement()`：结构体定义
    - `parseEnumStatement()`：枚举定义
    - `parseConstStatement()`：常量定义
    - `parseTraitStatement()`：trait定义
    - `parseImplStatement()`：impl块
    - `parseExpressionStatement()`：表达式语句
  - 辅助解析方法：
    - `parseSelfParameter()`：self参数
    - `parseParameterList()`：参数列表
    - `parseParameter()`：单个参数

#### 8. TypeParser.java（类型解析器）
- **职责**：解析各种类型表达式
- **主要方法**：
  - `parseType()`：解析类型（入口方法）
  - 各种具体类型解析方法：
    - `parseReferenceType()`：引用类型
    - `parseArrayType()`：数组类型
    - `parseUnitType()`：单元类型
    - `parsePathType()`：路径类型
    - `parsePathSegment()`：路径段

#### 9. PatternParser.java（模式解析器）
- **职责**：解析各种模式
- **主要方法**：
  - `parsePattern()`：解析模式（入口方法）
  - 各种具体模式解析方法：
    - `parseReferencePattern()`：引用模式
    - `parseWildcardPattern()`：通配符模式
    - `parseIdentifierPattern()`：标识符模式

### 文件间的依赖关系

```
Parser.java
├── TokenStream.java
├── ErrorReporter.java
├── ParserConfiguration.java
└── StatementParser.java
    ├── ExpressionParser.java
    │   ├── BaseParser.java
    │   └── TypeParser.java (递归调用)
    ├── TypeParser.java
    │   ├── BaseParser.java
    │   └── ExpressionParser.java (用于数组大小)
    └── PatternParser.java
        └── BaseParser.java
```

### 数据流图

```
令牌流 → TokenStream → Parser
                          ↓
                    StatementParser
                          ↓
                    ExpressionParser ← TypeParser
                          ↓
                    AST节点结构
```

### 接口设计原则

1. **单一职责**：每个类专注于特定类型的解析
2. **依赖注入**：通过构造函数注入依赖，明确组件关系
3. **最小接口**：每个类只暴露必要的方法
4. **错误处理**：统一的错误报告机制
5. **可测试性**：每个组件可以独立测试

这个文件架构设计确保了代码的模块化、可维护性和可扩展性，同时正确处理了解析器之间的复杂依赖关系。