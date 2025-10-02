public class Parser {
    private Vector<token_t> tokens;

    public Parser(Vector<token_t> tokens) {
        this.tokens = tokens;
    }

    int i = 0;

    public void parse(LetStmtNode node) {
        // consume let
        i++;
        PatternNode pattern = new PatternNode();
        parse(pattern);
        node.variable = pattern;
        // expect colon
        if (i < tokens.size() && tokens.get(i).tokentype.name == ":") {
            i++;
            TypeExprNode type = new TypeExprNode();
            parse(type);
            node.type = type;
        } else {
            assert false : "Expected ':' after pattern in let statement";
        }
        // expect equal
        if (i < tokens.size() && tokens.get(i).tokentype.name == "=") {
            i++;
            ExprStmtNode expr = new ExprStmtNode();
            parse(expr);
            node.value = expr;
        } else {
            node.value = null;
        }
        // expect semicolon
        if (i < tokens.size() && tokens.get(i).tokentype.name == ";") {
            i++;
        } else {
            assert false : "Expected ';' at end of let statement";
        }
    }

    public void parse(FunctionNode node) {
        // consume fn
        i++;
        // expect identifier
        if (i < tokens.size() && token_t.isIdentifier(tokens.get(i))) {
            IdentifierNode name = new IdentifierNode();
            parse(name);
            node.name = name;
        } else {
            assert false : "Expected function name after 'fn'";
        }
        // expect (
        if (i < tokens.size() && tokens.get(i).tokentype.name == "(") {
            i++;
        } else {
            assert false : "Expected '(' after function name";
        }
        // parse parameters
        Vector<ParameterNode> parameters = new Vector<>();
        // try to check if there is a self parameter
        parse(node.selfPara);
        while (i < tokens.size() && tokens.get(i).tokentype.name != ")") {
            ParameterNode param = new ParameterNode();
            parse(param);
            parameters.add(param);
            if (i < tokens.size() && tokens.get(i).tokentype.name == ",") {
                i++;
            } else if (i >= tokens.size() || tokens.get(i).tokentype.name != ")") {
                assert false : "Expected ',' or ')' in parameter list";
            }
        }
        node.parameters = parameters;
        // expect )
        if (i < tokens.size() && tokens.get(i).tokentype.name == ")") {
            i++;
        } else {
            assert false : "Expected ')' at end of parameter list"; 
        }
        // check for return type
        if (i < tokens.size() && tokens.get(i).tokentype.name == "->") {
            i++;
            TypeExprNode returnType = new TypeExprNode();
            parse(returnType);
            node.returnType = returnType;
        } else {
            node.returnType = null;
        }
        // expect {
        if (i < tokens.size() && tokens.get(i).tokentype.name == "{") {
            i++;
            BlockExprNode body = new BlockExprNode();
            parse(body);
            node.body = body;
        } else {
            // if the next token is ";", then it's function declaration without body
            if (i < tokens.size() && tokens.get(i).tokentype.name == ";") {
                i++;
                node.body = null;
            } else {
                assert false : "Expected '{' or ';' after function signature";
            }
        }
    }

    public void parse(SelfParaNode node) {
        // if the token is not self, then node is null
        int start = i; boolean istyped = true;
        if (tokens.get(i).name.equals("&")) {
            node.isReference = true;
            istyped = false;
            i++;
        }
        if (tokens.get(i).name.equals("mut")) {
            node.isMutable = true;
            i++;
        }
        if (tokens.get(i).name.equals("self")) {
            node.isSelf = true;
            i++;
            if (istyped && i < tokens.size() && tokens.get(i).name.equals(":")) {
                i++;
                TypeExprNode type = new TypeExprNode();
                parse(type);
                node.type = type;
            }
        } else {
            i = start;
            node = null;
        }
    }

    public void parse(ParameterNode node) {
        PatternNode pattern = new PatternNode();
        parse(pattern);
        node.pattern = pattern;
        if (i < tokens.size() && tokens.get(i).tokentype.name == ":") {
            i++;
            TypeExprNode type = new TypeExprNode();
            parse(type);
            node.type = type;
        } else {
            assert false : "Expected ':' after parameter pattern";
        }
    }

    public void parse(PatternNode node) {
        if (i >= tokens.size()) {
            assert false : "No more tokens to parse in pattern";
        }
        token_t token = tokens.get(i);
        // check for reference pattern
        if (token.name.equals("&") || token.name.equals("&&")) {
            RefPatNode refNode = new RefPatNode();
            i++;
            if (i < tokens.size() && tokens.get(i).name.equals("mut")) {
                refNode.isMutable = true;
                i++;
            }
            PatternNode innerPattern = new PatternNode();
            parse(innerPattern);
            refNode.innerPattern = innerPattern;
            node = refNode;
            return ;
        } else if (token.name.equals("_")) {
            WildPatNode wildNode = new WildPatNode();
            i++;
            node = wildNode;
            return ;
        } 
        IdPatNode idNode = new IdPatNode();
        // check if the next token is "ref"
        if (tokens.get(i).name.equals("ref")) {
            idNode.isReference = true;
            i++;
        }
        if (i < tokens.size() && tokens.get(i).name.equals("mut")) {
            idNode.isMutable = true;
            i++;
        }
        if (i < tokens.size() && token_t.isIdentifier(tokens.get(i))) {
            IdentifierNode name = new IdentifierNode();
            parse(name);
            idNode.name = name;
            node = idNode;
        } else {
            assert false : "Expected identifier in pattern";
        }
    }

    public void parse(StructNode node) {
        // consume struct
        i++;
        // expect identifier
        if (i < tokens.size() && token_t.isIdentifier(tokens.get(i))) {
            IdentifierNode name = new IdentifierNode();
            parse(name);
            node.name = name;
        } else {
            assert false : "Expected struct name after 'struct'";
        }
        // expect {
        if (i < tokens.size() && tokens.get(i).tokentype.name == "{") {
            i++;
        } else {
            // if the next token is ";", then it's struct declaration without body
            if (i < tokens.size() && tokens.get(i).tokentype.name == ";") {
                i++;
                node.fields = null;
                return;
            } else {
                assert false : "Expected '{' or ';' after struct name";
            }
        }
        // parse fields
        Vector<FieldNode> fields = new Vector<>();
        while (i < tokens.size() && tokens.get(i).tokentype.name != "}") {
            FieldNode field = new FieldNode();
            parse(field);
            fields.add(field);
            if (i < tokens.size() && tokens.get(i).tokentype.name == ",") {
                i++;
            } else if (i >= tokens.size() || tokens.get(i).tokentype.name != "}") {
                assert false : "Expected ',' or '}' in field list";
            }
        }
        node.fields = fields;
        // expect }
        if (i < tokens.size() && tokens.get(i).tokentype.name == "}") {
            i++;
        } else {
            assert false : "Expected '}' at end of field list";
        }
    }

    public void parse(FieldNode node) {
        // expect identifier
        if (i < tokens.size() && token_t.isIdentifier(tokens.get(i))) {
            IdentifierNode name = new IdentifierNode();
            parse(name);
            node.name = name;
        } else {
            assert false : "Expected field name in struct";
        }
        // expect :
        if (i < tokens.size() && tokens.get(i).tokentype.name == ":") {
            i++;
        } else {
            assert false : "Expected ':' after field name in struct";
        }
        TypeExprNode type = new TypeExprNode();
        parse(type);
        node.type = type;
    }

    public void parse(EnumNode node) {
        // consume enum
        i++;
        // expect identifier
        if (i < tokens.size() && token_t.isIdentifier(tokens.get(i))) {
            IdentifierNode name = new IdentifierNode();
            parse(name);
            node.name = name;
        } else {
            assert false : "Expected enum name after 'enum'";
        }
        // expect {
        if (i < tokens.size() && tokens.get(i).tokentype.name == "{") {
            i++;
        } else {
            assert false : "Expected '{' after enum name";
        }
        // parse variants
        Vector<IdentifierNode> variants = new Vector<>();
        while (i < tokens.size() && tokens.get(i).tokentype.name != "}") {
            if (i < tokens.size() && token_t.isIdentifier(tokens.get(i))) {
                IdentifierNode variant = new IdentifierNode();
                parse(variant);
                variants.add(variant);
            } else {
                assert false : "Expected variant name in enum";
            }
            if (i < tokens.size() && tokens.get(i).tokentype.name == ",") {
                i++;
            } else if (i >= tokens.size() || tokens.get(i).tokentype.name != "}") {
                assert false : "Expected ',' or '}' in variant list";
            }
        }
        node.variants = variants;
        // expect }
        if (i < tokens.size() && tokens.get(i).tokentype.name == "}") {
            i++;
        } else {
            assert false : "Expected '}' at end of variant list";
        }
    }

    public void parse(ConstItemNode node) {
        // consume the "const" token
        i++;
        if (i < tokens.size() && token_t.isIdentifier(tokens.get(i))) {
            IdentifierNode name = new IdentifierNode();
            parse(name);
            node.name = name;
        } else {
            assert false : "Expected const name after 'const'";
        }
        if (i < tokens.size() && tokens.get(i).tokentype.name == ":") {
            i++;
            TypeExprNode type = new TypeExprNode();
            parse(type);
            node.type = type;
        } else {
            assert false : "Expected ':' after const name";
        }
        if (i < tokens.size() && tokens.get(i).tokentype.name == "=") {
            i++;
            ExprStmtNode value = new ExprStmtNode();
            parse(value);
            node.value = value;
        }
        if (i < tokens.size() && tokens.get(i).tokentype.name == ";") {
            i++;
        } else {
            assert false : "Expected ';' at end of const item";
        }
    }

    public void parse(StmtNode node) {
        assert i < tokens.size() : "No more tokens to parse";
        token_t token = tokens.get(i);
        if (token.tokentype == token_t.TokenType_t.IDENTIFIER_OR_KEYWORD) {
            // parse based on the specific keyword or identifier
            if (token.name.equals("let")) {
                LetStmtNode letNode = new LetStmtNode();
                // parse pattern
                parse(letNode);   
                node = letNode;             
            } else if(token.name.equals("fn")) {
                FunctionNode funcNode = new FunctionNode();
                parse(funcNode);
                node = funcNode;
            } else if(token.name.equals("struct")) {
                StructNode structNode = new StructNode();
                parse(structNode);
                node = structNode;
            } else if(token.name.equals("enum")) {
                EnumNode enumNode = new EnumNode();
                parse(enumNode);
                node = enumNode;
            } else if(token.name.equals("const")) {
            } else if(token.name.equals("trait")) {
            } else if(token.name.equals("impl")) {
            } else {
                // assume it's an expression statement
                ExprStmtNode exprNode = new ExprStmtNode();
                parse(exprNode);
                node = exprNode;
            }
        }
    }

    Vector<StmtNode> statements = new Vector<>();
    while(i < tokens.size()) {
        StmtNode stmt = new StmtNode();
        parse(stmt);
        statements.add(stmt);
    }
}