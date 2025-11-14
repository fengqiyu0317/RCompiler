import java.util.Vector;

public class ExpressionParser extends BaseParser {
    private int recursionDepth = 0;
    private static final int MAX_RECURSION_DEPTH = 100;
    
    private StatementParser statementParser;
    private TypeParser typeParser;
    
    public ExpressionParser(TokenStream tokenStream, ErrorReporter errorReporter, ParserConfiguration config) {
        super(tokenStream, errorReporter, config);
    }
    
    // Setters for dependency injection
    public void setStatementParser(StatementParser statementParser) {
        this.statementParser = statementParser;
    }
    
    public void setTypeParser(TypeParser typeParser) {
        this.typeParser = typeParser;
    }
    
    // Grammar: <expression> ::= <exprwithblock> | <exprwithoutblock>
    // Main expression parsing method
    public ExprNode parse() {
        try {
            return parseExpression(0);
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing expression: " + e.getMessage());
        }
    }
    
    // Parse expression with precedence
    public ExprNode parseExpression(int precedence) {
        try {
            if (recursionDepth > MAX_RECURSION_DEPTH) {
                throw new RuntimeException("Maximum recursion depth exceeded in expression parsing");
            }
            recursionDepth++;
            
            try {
                ExprNode left = parsePrimary();
                
                while (!tokenStream.isAtEnd() &&
                       precedence < getPrecedence(tokenStream.current()) &&
                       !isExpressionEndToken(tokenStream.current()) &&
                       config.isOperatorToken(tokenStream.current().name)) {
                    token_t token = tokenStream.current();
                    left = parseInfixExpression(left, token);
                }
                
                return left;
            } finally {
                recursionDepth--;
            }
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing expression with precedence: " + e.getMessage());
        }
    }
    
    // Grammar: <exprwithblock> ::= <blockexpr> | <ifexpr> | <loopexpr>
    // Grammar: <exprwithoutblock> ::= <literalexpr> | <pathexpr> | <operexpr> | <arrayexpr> | <indexexpr> | <structexpr> | <callexpr> | <methodcallexpr> | <fieldexpr> | <continueexpr> | <breakexpr> | <returnexpr> | <underscoreexpr> | <groupedexpr>
    // Parse primary expressions
    private ExprNode parsePrimary() {
        token_t token = tokenStream.current();
        
        if (token.tokentype == token_t.TokenType_t.INTEGER_LITERAL ||
            token.tokentype == token_t.TokenType_t.CHAR_LITERAL ||
            token.tokentype == token_t.TokenType_t.STRING_LITERAL ||
            token.tokentype == token_t.TokenType_t.RAW_STRING_LITERAL ||
            token.tokentype == token_t.TokenType_t.C_STRING_LITERAL ||
            token.tokentype == token_t.TokenType_t.RAW_C_STRING_LITERAL ||
            token.name.equals("true") || token.name.equals("false")) {
            return parseLiteral();
        } else if (token.name.equals("if") || token.name.equals("while") ||
                   token.name.equals("loop")) {
            return parseExpressionWithBlock();
        } else if (token.name.equals("{")) {
            return parseBlockExpression();
        } else if (token.name.equals("(")) {
            return parseGroupExpression();
        } else if (token.name.equals("&") || token.name.equals("&&") ||
                   token.name.equals("*") || token.name.equals("-") || token.name.equals("!")) {
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
            return null;
        }
    }
    
    // Parse infix expressions
    private ExprNode parseInfixExpression(ExprNode left, token_t operator) {
        try {
            int precedence = getPrecedence(operator);
            
            if (config.isArithmeticOperator(operator.name)) {
                tokenStream.consume(); // Consume the operator
                ExprNode right = parseExpression(precedence);
                ArithExprNode node = new ArithExprNode();
                node.left = left;
                node.right = right;
                node.operator = getOperator(operator.name);
                return node;
            } else if (config.isComparisonOperator(operator.name)) {
                tokenStream.consume(); // Consume the operator
                ExprNode right = parseExpression(precedence);
                CompExprNode node = new CompExprNode();
                node.left = left;
                node.right = right;
                node.operator = getOperator(operator.name);
                return node;
            } else if (config.isAssignmentOperator(operator.name)) {
                tokenStream.consume(); // Consume the operator
                ExprNode right = parseExpression(precedence);
                AssignExprNode node = new AssignExprNode();
                node.left = left;
                node.right = right;
                node.operator = getOperator(operator.name);
                return node;
            } else if (config.isCompoundAssignmentOperator(operator.name)) {
                tokenStream.consume(); // Consume the operator
                ExprNode right = parseExpression(precedence);
                ComAssignExprNode node = new ComAssignExprNode();
                node.left = left;
                node.right = right;
                node.operator = getOperator(operator.name);
                return node;
            } else if (config.isLogicalOperator(operator.name)) {
                tokenStream.consume(); // Consume the operator
                ExprNode right = parseExpression(precedence);
                LazyExprNode node = new LazyExprNode();
                node.left = left;
                node.right = right;
                node.operator = getOperator(operator.name);
                return node;
            } else if (operator.name.equals(".")) {
                tokenStream.consume(); // Consume the operator
                return parseMemberAccess(left);
            } else if (operator.name.equals("(")) {
                return parseFunctionCall(left);
            } else if (operator.name.equals("[")) {
                return parseIndexAccess(left);
            } else if (operator.name.equals("as")) {
                tokenStream.consume(); // Consume the operator
                return parseTypeCast(left);
            } else if (operator.name.equals("::")) {
                tokenStream.consume(); // Consume the operator
                return parsePathExtension(left);
            } else if (operator.name.equals("{")) {
                return parseStructExpression(left);
            }
            
            return left;
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing infix expression: " + e.getMessage());
        }
    }
    
    // Grammar: <literalexpr> ::= <char_literal> | <string_literal> | <raw_string_literal> | <c_string_literal> | <raw_c_string_literal> | <integer_literal> | <boolean_literal>
    // Parse literal expressions
    public LiteralExprNode parseLiteral() {
        try {
            LiteralExprNode node = new LiteralExprNode();
            token_t token = tokenStream.consume(); // Consume the token now
            
            if (token.tokentype == token_t.TokenType_t.INTEGER_LITERAL) {
                // Parse integer literal with type suffix
                String tokenValue = token.name;
                literal_t intType = literal_t.INT; // Default type is i32
                String numericValue = tokenValue;
                
                // Check for type suffix
                if (tokenValue.endsWith("u32")) {
                    intType = literal_t.UINT;
                    numericValue = tokenValue.substring(0, tokenValue.length() - 3);
                } else if (tokenValue.endsWith("i32")) {
                    intType = literal_t.INT;
                    numericValue = tokenValue.substring(0, tokenValue.length() - 3);
                } else if (tokenValue.endsWith("usize")) {
                    intType = literal_t.USIZE;
                    numericValue = tokenValue.substring(0, tokenValue.length() - 5);
                } else if (tokenValue.endsWith("isize")) {
                    intType = literal_t.ISIZE;
                    numericValue = tokenValue.substring(0, tokenValue.length() - 5);
                }
                
                node.literalType = intType;
                
                try {
                    // Always parse as long
                    long value = parseUnsignedLongLong(numericValue);
                    node.value_long = value;
                } catch (NumberFormatException ex) {
                    // Report the error but don't throw an exception to avoid breaking parsing
                    errorReporter.reportError("Integer literal out of range (max: 2^64-1): " + numericValue, token);
                    // Set a default value to continue parsing
                    node.value_long = Long.MAX_VALUE;
                }
            } else if (token.tokentype == token_t.TokenType_t.CHAR_LITERAL) {
                node.literalType = literal_t.CHAR;
                node.value_string = token.name;
            } else if (token.tokentype == token_t.TokenType_t.STRING_LITERAL ||
                       token.tokentype == token_t.TokenType_t.RAW_STRING_LITERAL) {
                node.literalType = literal_t.STRING;
                node.value_string = token.name;
            } else if (token.tokentype == token_t.TokenType_t.C_STRING_LITERAL ||
                       token.tokentype == token_t.TokenType_t.RAW_C_STRING_LITERAL) {
                node.literalType = literal_t.CSTRING;
                node.value_string = token.name;
            } else if (token.name.equals("true")) {
                node.literalType = literal_t.BOOL;
                node.value_bool = true;
            } else if (token.name.equals("false")) {
                node.literalType = literal_t.BOOL;
                node.value_bool = false;
            }
            
            return node;
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing literal: " + e.getMessage());
        }
    }
    
    // Grammar: <exprwithblock> ::= <blockexpr> | <ifexpr> | <loopexpr>
    // Parse expressions with blocks
    public ExprWithBlockNode parseExpressionWithBlock() {
        try {
            token_t token = tokenStream.current(); // Consume the token now
            
            if (token.name.equals("if")) {
                return parseIfExpression();
            } else if (token.name.equals("while")) {
                return parseLoopExpression(true); // true indicates it's a while loop
            } else if (token.name.equals("loop")) {
                return parseLoopExpression(false); // false indicates it's a loop
            }
            
            errorReporter.reportError("Expected expression with block", token);
            return null;
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing expression with block: " + e.getMessage());
        }
    }
    
    // Grammar: <groupedexpr> ::= ( <expression> )
    // Parse group expressions
    public GroupExprNode parseGroupExpression() {
        try {
            tokenStream.consume("("); // Consume the opening parenthesis
            GroupExprNode node = new GroupExprNode();
            node.innerExpr = parse();
            
            tokenStream.consume(")");
            
            return node;
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing group expression: " + e.getMessage());
        }
    }
    
    // Grammar: <operexpr> ::= <borrowexpr> | <derefexpr> | <negaexpr> | <arithexpr> | <compexpr> | <lazyexpr> | <typecastexpr> | <assignexpr> | <comassignexpr>
    // Grammar: <borrowexpr> ::= (& | &&) (mut)? <expression>
    // Grammar: <derefexpr> ::= * <expression>
    // Grammar: <negaexpr> ::= (! | -) <expression>
    // Parse prefix expressions
    private ExprNode parsePrefixExpression() {
        token_t token = tokenStream.current(); // Consume the token now
        
        if (token.name.equals("&") || token.name.equals("&&")) {
            int precedence = getPrecedence(token);
            tokenStream.consume(); // Consume '&' or '&&'
            return parseBorrowExpression(token.name.equals("&&"), precedence);
        } else if (token.name.equals("*")) {
            tokenStream.consume(); // Consume '*'
            int precedence = getPrecedence(token);
            return parseDerefExpression(precedence);
        } else if (token.name.equals("-") || token.name.equals("!")) {
            tokenStream.consume(); // Consume '-' or '!'
            int precedence = getPrecedence(token);
            return parseNegationExpression(token.name.equals("!"), precedence);
        }
        
        errorReporter.reportError("Expected prefix expression", token);
        return null;
    }
    
    // Grammar: <pathexpr> ::= <pathseg> (:: <pathseg>)?
    // Parse path expressions
    public PathExprNode parsePathExpression() {
        try {
            PathExprNode node = new PathExprNode();
            
            // Create a path segment from the consumed token
            PathExprSegNode seg = new PathExprSegNode();
            seg = parsePathSegment();
            node.LSeg = seg;
            
            if (tokenStream.matches("::")) {
                node.RSeg = parsePathSegment();
            }
            
            return node;
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing path expression: " + e.getMessage());
        }
    }
    
    // Note: parsePathSegment is now implemented in BaseParser and inherited by ExpressionParser
    
    // Grammar: <arrayexpr> ::= [ (<elements> | <repeated_element>; <size>)? ]
    // Grammar: <elements> ::= <expression> (, <expression>)* ,?
    // Grammar: <repeated_element> ::= <expression>
    // Grammar: <size> ::= <expression>
    // Parse array expressions
    public ArrayExprNode parseArrayExpression() {
        try {
            tokenStream.consume("["); // Consume the opening bracket
            ArrayExprNode node = new ArrayExprNode();
            
            // Check for empty array
            if (tokenStream.matches("]")) {
                return node;
            }
            
            // Parse first element
            ExprNode firstElement = parse();
            
            // Check if it's a repeated element array
            if (tokenStream.matches(";")) {
                node.size = parse();
                node.repeatedElement = firstElement;
            } else {
                // It's a regular elements array
                Vector<ExprNode> elements = new Vector<>();
                elements.add(firstElement);
                
                while (tokenStream.matches(",")) {
                    if (tokenStream.equals("]")) {
                        break;
                    }
                    elements.add(parse());
                }
                
                node.elements = elements;
            }
            
            tokenStream.consume("]");
            
            return node;
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing array expression: " + e.getMessage());
        }
    }
    
    // Grammar: <continueexpr> ::= continue
    // Parse continue expression
    public ContinueExprNode parseContinueExpression() {
        try {
            tokenStream.consume("continue"); // Consume the continue keyword
            return new ContinueExprNode();
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing continue expression: " + e.getMessage());
        }
    }
    
    // Grammar: <breakexpr> ::= break (<expression>)?
    // Parse break expression
    public BreakExprNode parseBreakExpression() {
        try {
            tokenStream.consume("break"); // Consume the break keyword
            
            BreakExprNode node = new BreakExprNode();
            if (!tokenStream.equals(";") && !tokenStream.equals("}")) {
                node.value = parse();
            }
            
            return node;
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing break expression: " + e.getMessage());
        }
    }
    
    // Grammar: <returnexpr> ::= return (<expression>)?
    // Parse return expression
    public ReturnExprNode parseReturnExpression() {
        try {
            tokenStream.consume("return"); // Consume the return keyword
            
            ReturnExprNode node = new ReturnExprNode();
            if (!tokenStream.equals(";") && !tokenStream.equals("}")) {
                node.value = parse();
            }
            
            return node;
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing return expression: " + e.getMessage());
        }
    }
    
    // Grammar: <underscoreexpr> ::= _
    // Parse underscore expression
    public UnderscoreExprNode parseUnderscoreExpression() {
        try {
            tokenStream.consume("_"); // Consume the underscore
            return new UnderscoreExprNode();
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing underscore expression: " + e.getMessage());
        }
    }
    
    // Grammar: <fieldexpr> ::= <expression> . <identifier>
    // Grammar: <methodcallexpr> ::= <expression> . <pathseg> ( <arguments>? )
    // Grammar: <arguments> ::= <expression> (, <expression>)* ,?
    // Parse member access
    public ExprNode parseMemberAccess(ExprNode left) {
        try {
            // Note: '.' has already been consumed in parseInfixExpression
            PathExprSegNode pathSeg = parsePathSegment();
            
            if (tokenStream.equals("(")) {
                // It's a method call
                MethodCallExprNode methodCallNode = new MethodCallExprNode();
                methodCallNode.receiver = left;
                methodCallNode.methodName = pathSeg;
                methodCallNode.arguments = parseFunctionArguments();
                return methodCallNode;
            } else {
                // It's a field access
                FieldExprNode fieldNode = new FieldExprNode();
                fieldNode.receiver = left;
                if (pathSeg.patternType == patternSeg_t.IDENT) {
                    fieldNode.fieldName = pathSeg.name;
                } else {
                    errorReporter.reportError("Expected identifier after '.' in field access", tokenStream.current());
                }
                return fieldNode;
            }
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing member access: " + e.getMessage());
        }
    }
    
    // Grammar: <callexpr> ::= <expression> ( <arguments>? )
    // Grammar: <arguments> ::= <expression> (, <expression>)* ,?
    // Parse function call
    public CallExprNode parseFunctionCall(ExprNode left) {
        try {
            CallExprNode callNode = new CallExprNode();
            callNode.function = left;
            callNode.arguments = parseFunctionArguments();
            return callNode;
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing function call: " + e.getMessage());
        }
    }
    
    // Parse function arguments
    public Vector<ExprNode> parseFunctionArguments() {
        try {
            Vector<ExprNode> args = new Vector<>();

            tokenStream.consume("(");
            // Check for empty argument list
            if (tokenStream.matches(")")) {
                return args;
            }
            
            // Parse first argument
            args.add(parse());
            
            // Parse remaining arguments
            while (tokenStream.matches(",")) {
                // Check for empty argument (consecutive commas or trailing comma)
                if (tokenStream.equals(")")) {
                    break;
                }
                args.add(parse());
            }
            
            tokenStream.consume(")");
            
            return args;
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing function arguments: " + e.getMessage());
        }
    }
    
    // Grammar: <indexexpr> ::= <expression> [ <expression> ]
    // Parse index access
    public IndexExprNode parseIndexAccess(ExprNode left) {
        try {
            IndexExprNode indexNode = new IndexExprNode();
            indexNode.array = left;
            
            tokenStream.consume("[");
            indexNode.index = parse();
            tokenStream.consume("]");
            
            return indexNode;
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing index access: " + e.getMessage());
        }
    }
    
    // Grammar: <typecastexpr> ::= <expression> as <type>
    // Parse type cast
    private TypeCastExprNode parseTypeCast(ExprNode left) {
        try {
            TypeCastExprNode typeCastNode = new TypeCastExprNode();
            typeCastNode.expr = left;
            
            if (typeParser != null) {
                typeCastNode.type = typeParser.parseType();
            } else {
                errorReporter.reportError("TypeParser not available for type casting", tokenStream.current());
            }
            
            return typeCastNode;
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing type cast: " + e.getMessage());
        }
    }
    
    // Parse path extension
    private ExprNode parsePathExtension(ExprNode left) {
        try {
            if (left instanceof PathExprNode) {
                PathExprNode pathNode = (PathExprNode) left;
                if (pathNode.RSeg != null) {
                    errorReporter.reportError("Unexpected state: PathExprNode already has RSeg", tokenStream.current());
                }
                pathNode.RSeg = parsePathSegment();
                return pathNode;
            }
            
            errorReporter.reportError("Expected path expression before '::'", tokenStream.current());
            return left;
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing path extension: " + e.getMessage());
        }
    }
    
    // Grammar: <borrowexpr> ::= (& | &&) (mut)? <expression>
    // Parse borrow expression
    public BorrowExprNode parseBorrowExpression(boolean isDoubleReference, int precedence) {
        try {
            BorrowExprNode node = new BorrowExprNode();
            
            node.isDoubleReference = isDoubleReference;
            
            node.isMutable = tokenStream.matches("mut");
            node.innerExpr = parseExpression(precedence);
            
            return node;
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing borrow expression: " + e.getMessage());
        }
    }
    
    // Grammar: <derefexpr> ::= * <expression>
    // Parse dereference expression
    public DerefExprNode parseDerefExpression(int precedence) {
        try {
            DerefExprNode node = new DerefExprNode();
            node.innerExpr = parseExpression(precedence);
            
            return node;
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing deref expression: " + e.getMessage());
        }
    }
    
    // Grammar: <negaexpr> ::= (! | -) <expression>
    // Parse negation expression
    public NegaExprNode parseNegationExpression(boolean isLogical, int precedence) {
        try {
            // Note: '-' or '!' has already been consumed in parsePrefixExpression
            NegaExprNode node = new NegaExprNode();
            
            // We need to determine which operator was used
            // Since we don't have access to the previous token, we'll need to modify the approach
            // For now, we'll assume it's a logical negation if the next token suggests it
            // This might need additional context from the calling method
            
            node.isLogical = isLogical;
            node.innerExpr = parseExpression(precedence);
            
            return node;
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing negation expression: " + e.getMessage());
        }
    }
    
    // Grammar: <ifexpr> ::= if <expression except structexpr> <blockexpr> (else (<ifexpr> | <blockexpr>))?
    // Parse if expression
    public IfExprNode parseIfExpression() {
        try {
            tokenStream.consume("if"); // Consume the 'if' keyword
            IfExprNode node = new IfExprNode();
            node.condition = parseGroupExpression();
            node.thenBranch = parseBlockExpression();
            
            if (tokenStream.matches("else")) {
                if (tokenStream.equals("if")) {
                    node.elseifBranch = parseIfExpression();
                } else {
                    node.elseBranch = parseBlockExpression();
                }
            }
            
            return node;
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing if expression: " + e.getMessage());
        }
    }
    
    // Grammar: <loopexpr> ::= <infinite_loop> | <conditional_loop>
    // Grammar: <infinite_loop> ::= loop <blockexpr>
    // Grammar: <conditional_loop> ::= while <expression except structexpr> <blockexpr>
    public LoopExprNode parseLoopExpression(boolean isWhileLoop) {
        try {
            tokenStream.consume(isWhileLoop ? "while" : "loop"); // Consume 'while' or 'loop'
            LoopExprNode node = new LoopExprNode();
            
            if (isWhileLoop) {
                // It's a while loop, so there should be a condition
                node.condition = parseGroupExpression();
            } else {
                // It's a loop, so no condition
                node.condition = null;
            }
            
            node.body = parseBlockExpression();
            
            return node;
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing loop expression: " + e.getMessage());
        }
    }
    
    // Grammar: <blockexpr> ::= { <statements>? }
    // Grammar: <statements> ::= <statement>+ | <statement>+ <expressionwithoutblock> | <expressionwithoutblock>
    // Parse block expression
    public BlockExprNode parseBlockExpression() {
        try {
            tokenStream.consume("{"); // Consume the opening brace
            
            BlockExprNode node = new BlockExprNode();
            Vector<StmtNode> statements = new Vector<>();
            
            if (statementParser != null) {
                // Check for empty block
                if (tokenStream.equals("}")) {
                    node.statements = statements;
                    tokenStream.consume("}");
                    return node;
                }
                
                // Parse statements and possibly a trailing expression
                boolean hasExpression = false;
                
                // First, try to parse statements
                while (!tokenStream.equals("}")) {
                    // Save current position to backtrack if needed
                    int savedPosition = tokenStream.getPosition();
                    
                    try {
                        // Try to parse a statement
                        StmtNode stmt = statementParser.parseStatement();
                        if (stmt != null) {
                            statements.add(stmt);
                            continue;
                        }
                    } catch (Exception e) {
                        // Statement parsing failed, backtrack
                        tokenStream.setPosition(savedPosition);
                    }
                    
                    // If we're here, either statement parsing failed or we're at the end
                    // Try to parse an expression without block as the final element
                    try {
                        ExprNode expr = parseExpressionWithoutBlock();
                        if (expr != null) {
                            // Wrap the expression in an expression statement
                            ExprStmtNode exprStmt = new ExprStmtNode();
                            exprStmt.expr = expr;
                            statements.add(exprStmt);
                            hasExpression = true;
                            break;
                        } else {
                            throw new ParserException("Expected statement or expression in block");
                        }
                    } catch (Exception e) {
                        // Expression parsing also failed
                        throw new ParserException("Failed to parse statement or expression in block: " + e.getMessage());
                    }
                }
                
                node.statements = statements;
            } else {
                errorReporter.reportError("StatementParser not available for block expression", tokenStream.current());
            }
            
            tokenStream.consume("}");
            
            return node;
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing block expression: " + e.getMessage());
        }
    }
    
    // Helper method to parse expression without block
    private ExprNode parseExpressionWithoutBlock() {
        // Save the current state to determine if we should parse as ExprWithoutBlock
        int startPos = tokenStream.getPosition();
        token_t token = tokenStream.current();
        
        // Check if this could be an expression with block
        // if (token.name.equals("if") || token.name.equals("while") ||
        //     token.name.equals("loop") || token.name.equals("{")) {
            
        //     // Try to parse as expression with block
        //     ExprWithBlockNode withBlockNode = parseExpressionWithBlock();
            
        //     // Check the next token to see if this is part of a larger expression
        //     if (!tokenStream.isAtEnd()) {
        //         token_t nextToken = tokenStream.current();
                
        //         // If the next token has a non-zero precedence, this might be part of a larger expression
        //         if (getPrecedence(nextToken) > 0) {
        //             // Backtrack and parse as expression without block
        //             tokenStream.setPosition(startPos);
        //             return parseExpression(0);
        //         }
        //     }
            
        //     // This is a standalone expression with block
        //     return withBlockNode;
        // }
        
        // Parse as a regular expression without block
        return parseExpression(0);
    }
    
    // Grammar: <structexpr> ::= <pathseg> { <fieldvals>? }
    // Grammar: <fieldvals> ::= <fieldval> (, <fieldval>)* ,?
    // Grammar: <fieldval> ::= <identifier> : <expression>
    // Parse struct expression
    public StructExprNode parseStructExpression(ExprNode left) {
        try {
            StructExprNode node = new StructExprNode();
            
            // The left expression should be a path expression representing the struct name
            if (left instanceof PathExprNode) {
                PathExprNode pathExpr = (PathExprNode) left;
                // For struct expressions, we only use the left segment of the path
                node.structName = pathExpr.LSeg;
            } else {
                errorReporter.reportError("Expected path expression before '{' in struct expression", tokenStream.current());
                return null;
            }
            
            tokenStream.consume("{"); // Consume the opening brace
            
            // Check for empty struct
            if (tokenStream.matches("}")) {
                return node;
            }
            
            // Parse field values
            Vector<FieldValNode> fieldValues = new Vector<>();
            
            // Parse first field value
            fieldValues.add(parseFieldValue());
            
            // Parse remaining field values
            while (tokenStream.matches(",")) {
                // Check for empty field (consecutive commas or trailing comma)
                if (tokenStream.equals("}")) {
                    break;
                }
                fieldValues.add(parseFieldValue());
            }
            
            node.fieldValues = fieldValues;
            
            // Consume the closing brace
            tokenStream.consume("}");
            
            return node;
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing struct expression: " + e.getMessage());
        }
    }
    
    // Parse field value for struct expression
    private FieldValNode parseFieldValue() {
        try {
            FieldValNode node = new FieldValNode();
            
            // Parse field name (identifier)
            token_t token = tokenStream.current();
            if (!config.isIdentifier(token)) {
                errorReporter.reportError("Expected identifier in struct field", token);
                return null;
            }
            
            node.fieldName = new IdentifierNode();
            node.fieldName.name = token.name;
            tokenStream.consume();
            
            // Consume colon
            tokenStream.consume(":");
            
            // Parse field value expression
            node.value = parse();
            
            return node;
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing field value: " + e.getMessage());
        }
    }
    
    // Check if the current token should end expression parsing
    private boolean isExpressionEndToken(token_t token) {
        return token.name.equals(";") ||
               token.name.equals("}") ||
               token.name.equals(",") ||
               token.name.equals(")") ||
               token.name.equals("]");
    }
    
    // Parse unsigned long long value from string
    private long parseUnsignedLongLong(String numericValue) throws NumberFormatException {
        // Handle hexadecimal, octal, and binary literals
        int radix = 10;
        String value = numericValue;
        
        if (value.startsWith("0x")) {
            radix = 16;
            value = value.substring(2);
        } else if (value.startsWith("0b")) {
            radix = 2;
            value = value.substring(2);
        } else if (value.startsWith("0o")) {
            radix = 8;
            value = value.substring(2);
        } 
        
        // Parse using the appropriate radix
        if (radix == 10) {
            // For decimal, we need to check if it fits in unsigned long long range
            java.math.BigInteger bigInt = new java.math.BigInteger(value);
            java.math.BigInteger maxULL = new java.math.BigInteger("4294967295"); // 2^32 - 1
            
            if (bigInt.compareTo(maxULL) > 0) {
                throw new NumberFormatException("Value exceeds unsigned long long range");
            }
            
            return bigInt.longValue();
        } else {
            // For other radices, we can use Long.parseLong with unsigned check
            long result = Long.parseLong(value, radix);
            return result;
        }
    }
    
    // Parse group expression (helper for if conditions)
}