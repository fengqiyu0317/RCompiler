import java.util.Vector;

public class StatementParser extends BaseParser {
    private ExpressionParser expressionParser;
    private TypeParser typeParser;
    private PatternParser patternParser;
    
    public StatementParser(TokenStream tokenStream, ErrorReporter errorReporter,
                       ParserConfiguration config, ExpressionParser expressionParser,
                       TypeParser typeParser, PatternParser patternParser) {
        super(tokenStream, errorReporter, config);
        this.expressionParser = expressionParser;
        this.typeParser = typeParser;
        this.patternParser = patternParser;
    }
    
    // Setters for dependency injection
    public void setExpressionParser(ExpressionParser expressionParser) {
        this.expressionParser = expressionParser;
    }
    
    public void setTypeParser(TypeParser typeParser) {
        this.typeParser = typeParser;
    }
    
    public void setPatternParser(PatternParser patternParser) {
        this.patternParser = patternParser;
    }
    
    // Grammar: <statement> ::= <item> | <letstmt> | <exprstmt> | ;
    // Main statement parsing method
    public StmtNode parseStatement() throws ParserException {
        // Handle empty statement
        if (tokenStream.matches(";")) {
            tokenStream.consume(";"); // Consume the semicolon
            return null;
        }
        
        token_t token = tokenStream.current();
        
        // Dispatch to specific parsing methods based on keyword
        if (token.name.equals("let")) {
            return parseLetStatement();
        } else if (token.name.equals("fn")) {
            return parseFunctionStatement();
        } else if (token.name.equals("struct")) {
            return parseStructStatement();
        } else if (token.name.equals("enum")) {
            return parseEnumStatement();
        } else if (token.name.equals("const")) {
            // Check if it's a const function or const item
            if (tokenStream.peek() != null && tokenStream.peek().name.equals("fn")) {
                return parseFunctionStatement(true); // Pass true for const function
            } else {
                return parseConstStatement();
            }
        } else if (token.name.equals("trait")) {
            return parseTraitStatement();
        } else if (token.name.equals("impl")) {
            return parseImplStatement();
        } else {
            // Default to expression Statement
            return parseExpressionStatement();
        }
    }
    
    // Grammar: <letstmt> ::= let <pattern> : <type> (= <expression>)? ;
    // Parse let statement
    public LetStmtNode parseLetStatement() throws ParserException {
        tokenStream.consume("let");
        
        LetStmtNode node = new LetStmtNode();
        node.name = patternParser.parsePattern();
        
        tokenStream.consume(":");
        
        node.type = typeParser.parseType();
        
        if (tokenStream.matches("=")) {
            node.value = expressionParser.parse();
        } else {
            node.value = null;
        }
        
        tokenStream.consume(";");
        
        return node;
    }
    
    // Grammar: <function> ::= (const)? fn <identifier> (<parameters>?) (-> <type>)? ( <blockexpr> | ; )
    // Grammar: <parameters> ::= <selfpara> ,? | (<selfpara> ,)? <parameter> (, <parameter>)* ,?
    // Parse function statement
    public FunctionNode parseFunctionStatement() throws ParserException {
        return parseFunctionStatement(false);
    }
    
    // Parse function statement with optional const parameter
    public FunctionNode parseFunctionStatement(boolean isConst) throws ParserException {
        if (isConst) {
            tokenStream.consume("const");
        }
        
        tokenStream.consume("fn");
        
        FunctionNode node = new FunctionNode();
        node.isConst = isConst;
        node.name = parseIdentifier();
        
        tokenStream.consume("(");
        
        node.selfPara = parseSelfParameter();
        
        if (node.selfPara != null) {
            if (!tokenStream.matches(",")) {
                if (!tokenStream.equals(")")) {
                    errorReporter.reportError("Expected ',' or ')' after self parameter", tokenStream.current());
                }
            }
        }
        
        node.parameters = parseParameterList();
        
        tokenStream.consume(")");
        
        if (tokenStream.matches("->")) {
            node.returnType = typeParser.parseType();
        } else {
            node.returnType = null;
        }
        
        if (tokenStream.equals("{")) {
            node.body = (BlockExprNode) expressionParser.parseBlockExpression();
        } else if (tokenStream.matches(";")) {
            node.body = null;
        } else {
            errorReporter.reportError("Expected '{' or ';' after function signature", tokenStream.current());
        }
        
        return node;
    }
    
    // Grammar: <selfpara> ::= <shortself> | <typedself>
    // Grammar: <shortself> ::= &? (mut)? self
    // Grammar: <typedself> ::= (mut)? self : <type>
    // Parse self parameter
    public SelfParaNode parseSelfParameter() {
        int start = tokenStream.getPosition();
        boolean istyped = true;
        SelfParaNode node = new SelfParaNode();
        
        if (tokenStream.matches("&")) {
            node.isReference = true;
            istyped = false;
        }
        
        if (tokenStream.matches("mut")) {
            node.isMutable = true;
        }

        if (tokenStream.matches("self")) {
            if (istyped && tokenStream.matches(":")) {
                node.type = typeParser.parseType();
            }
            return node;
        } else {
            tokenStream.setPosition(start);
            return null;
        }
    }
    
    // Parse parameter list
    private Vector<ParameterNode> parseParameterList() {
        Vector<ParameterNode> parameters = new Vector<>();
        
        while (!tokenStream.equals(")")) {
            parameters.add(parseParameter());
            // Check for comma between parameters
            if (!tokenStream.matches(",")) {
                // If no comma and not at end of parameter list, it's an error
                if (!tokenStream.equals(")")) {
                    errorReporter.reportError("Expected ',' or ')' in parameter list", tokenStream.current());
                }
                // If we're at the end of the list, break the loop
                break;
            }
        }
        
        return parameters;
    }
    
    // Grammar: <parameter> ::= <pattern> : <type>
    // Parse single parameter
    public ParameterNode parseParameter() throws ParserException {
        ParameterNode node = new ParameterNode();
        node.name = patternParser.parsePattern();
        
        tokenStream.consume(":");
        
        node.type = typeParser.parseType();
        
        return node;
    }
    
    // Grammar: <structitem> ::= struct <identifier> ({ <fields>? } | ;)
    // Grammar: <fields> ::= <field> (, <field>)* ,?
    // Parse struct statement
    public StructNode parseStructStatement() throws ParserException {
        tokenStream.consume("struct");
        
        StructNode node = new StructNode();
        node.name = parseIdentifier();
        
        if (tokenStream.matches("{")) {
            Vector<FieldNode> fields = new Vector<>();
            while (!tokenStream.equals("}")) {
                fields.add(parseFieldNode());
                if (!tokenStream.matches(",")) {
                    if (!tokenStream.equals("}")) {
                        errorReporter.reportError("Expected ',' or '}' in field list", tokenStream.current());
                    }
                }
            }
            node.fields = fields;
            tokenStream.consume("}");
        } else if (tokenStream.matches(";")) {
            node.fields = null;
        } else {
            errorReporter.reportError("Expected '{' or ';' after struct name", tokenStream.current());
        }
        
        return node;
    }
    
    // Grammar: <field> ::= <identifier> : <type> ;
    // Parse field node
    public FieldNode parseFieldNode() throws ParserException {
        FieldNode node = new FieldNode();
        node.name = parseIdentifier();
        
        tokenStream.consume(":");
        
        node.type = typeParser.parseType();
        
        return node;
    }
    
    // Grammar: <enumitem> ::= enum <identifier> { <enum_variants>? }
    // Grammar: <enum_variants> ::= <identifier> (, <identifier>)* ,?
    // Parse enum statement
    public EnumNode parseEnumStatement() throws ParserException {
        tokenStream.consume("enum");
        
        EnumNode node = new EnumNode();
        node.name = parseIdentifier();
        
        tokenStream.consume("{");
        
        Vector<IdentifierNode> variants = new Vector<>();
        while (!tokenStream.equals("}")) {
            variants.add(parseIdentifier());
            if (!tokenStream.matches(",")) {
                if (!tokenStream.equals("}")) {
                    errorReporter.reportError("Expected ',' or '}' in variant list", tokenStream.current());
                }
            }
        }
        node.variants = variants;
        
        tokenStream.consume("}");
        
        return node;
    }
    
    // Grammar: <constitem> ::= const <identifier> : <type> (= <expression>)? ;
    // Parse const statement
    public ConstItemNode parseConstStatement() throws ParserException {
        tokenStream.consume("const");
        
        ConstItemNode node = new ConstItemNode();
        node.name = parseIdentifier();
        
        tokenStream.consume(":");
        
        node.type = typeParser.parseType();
        
        if (tokenStream.matches("=")) {
            node.value = expressionParser.parse();
        } else {
            node.value = null;
        }
        
        tokenStream.consume(";");
        
        return node;
    }
    
    // Grammar: <traititem> ::= trait <identifier> { <asso_item>* }
    // Grammar: <asso_item> ::= <function> | <constitem>
    // Parse trait statement
    public TraitNode parseTraitStatement() throws ParserException {
        tokenStream.consume("trait");
        
        TraitNode node = new TraitNode();
        node.name = parseIdentifier();
        
        tokenStream.consume("{");
        
        Vector<AssoItemNode> items = new Vector<>();
        while (!tokenStream.equals("}")) {
            AssoItemNode item = parseAssoItemNode();
            if (item != null) {
                items.add(item);
            }
        }
        node.items = items;
        
        tokenStream.consume("}");
        
        return node;
    }
    
    // Grammar: <implitem> ::= <inherentimplitem> | <traitimplitem>
    // Grammar: <inherentimplitem> ::= impl <type> { <asso_item>* }
    // Grammar: <traitimplitem> ::= impl <identifier> for <type> { <asso_item>* }
    // Parse impl statement
    public ImplNode parseImplStatement() throws ParserException {
        tokenStream.consume("impl");
        
        ImplNode node = new ImplNode();
        
        // Check if it's an inherent impl or a trait impl
        if (config.isIdentifier(tokenStream.current())) {
            // Look ahead to see if there is a "for" token
            if (tokenStream.peek() != null && tokenStream.peek().name.equals("for")) {
                // It's a trait impl
                node.trait = parseIdentifier();
                tokenStream.consume("for");
                node.typeName = typeParser.parseType();
            } else {
                // It's an inherent impl
                node.trait = null;
                node.typeName = typeParser.parseType();
            }
        } else {
            // It's an inherent impl with a non-identifier type
            node.trait = null;
            node.typeName = typeParser.parseType();
        }
        
        tokenStream.consume("{");
        
        Vector<AssoItemNode> items = new Vector<>();
        while (!tokenStream.equals("}")) {
            AssoItemNode item = parseAssoItemNode();
            if (item != null) {
                items.add(item);
            }
        }
        node.items = items;
        
        tokenStream.consume("}");
        
        return node;
    }
    
    // Grammar: <asso_item> ::= <function> | <constitem>
    // Parse associated item
    public AssoItemNode parseAssoItemNode() throws ParserException {
        AssoItemNode node = new AssoItemNode();
        
        if (tokenStream.equals("fn")) {
            node.function = parseFunctionStatement();
        } else if (tokenStream.equals("const")) {
            // It may be a const item or a const function
            if (tokenStream.peek() != null && tokenStream.peek().name.equals("fn")) {
                node.function = parseFunctionStatement(true); // Pass true for const function
            } else {
                node.constant = parseConstStatement();
            }
        } else if (tokenStream.matches(";")) {
            // Empty statement, just consume the semicolon and return null
            return null;
        } else {
            errorReporter.reportError("Expected 'fn' or 'const' in associated item", tokenStream.current());
        }
        
        return node;
    }
    
    // Grammar: <exprstmt> ::= <exprwithblock> ;? | <exprwithoutblock> ;
    // Parse expression statement
    public ExprStmtNode parseExpressionStatement() throws ParserException {
        // First, determine if we're dealing with an expression with block or without block
        token_t currentToken = tokenStream.current();
        
        // Check if the current token indicates an expression with block
        if (currentToken.name.equals("if") ||
            currentToken.name.equals("while") ||
            currentToken.name.equals("loop")) {
            // Parse as expression with block (if, while, loop)
            ExprWithBlockNode expr = expressionParser.parseExpressionWithBlock();
            
            ExprStmtNode stmt = new ExprStmtNode();
            stmt.expr = expr;
            // Expressions with blocks don't require semicolons, but they can have them
            tokenStream.matches(";");
            return stmt;
        } else if (currentToken.name.equals("{")) {
            // Parse as block expression specifically
            BlockExprNode expr = expressionParser.parseBlockExpression();
            
            ExprStmtNode stmt = new ExprStmtNode();
            stmt.expr = expr;
            // Block expressions don't require semicolons, but they can have them
            tokenStream.matches(";");
            return stmt;
        } else {
            // Parse as expression without block
            ExprNode expr = expressionParser.parse();
            
            // Expressions without blocks require semicolons
            tokenStream.consume(";");
            
            ExprStmtNode stmt = new ExprStmtNode();
            stmt.expr = expr;
            return stmt;
        }
    }
}