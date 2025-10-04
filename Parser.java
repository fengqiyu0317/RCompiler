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

    public void parse(TraitNode node) {
        // consume the "trait" token
        i++;
        if (i < tokens.size() && token_t.isIdentifier(tokens.get(i))) {
            IdentifierNode name = new IdentifierNode();
            parse(name);
            node.name = name;
        } else {
            assert false : "Expected trait name after 'trait'";
        }
        if (i < tokens.size() && tokens.get(i).tokentype.name == "{") {
            i++;
        } else {
            assert false : "Expected '{' after trait name";
        }
        Vector<AssoItemNode> items = new Vector<>();
        while (i < tokens.size() && tokens.get(i).tokentype.name != "}") {
            AssoItemNode item = new AssoItemNode();
            parse(item);
            items.add(item);
        }
        node.items = items;
        if (i < tokens.size() && tokens.get(i).tokentype.name == "}") {
            i++;
        } else {
            assert false : "Expected '}' at end of trait body";
        }
    }

    public void parse(ImplNode node) {
        // consume the "impl" token
        i++;
        // check if it's an inherent impl or a trait impl
        if (i < tokens.size() && token_t.isIdentifier(tokens.get(i))) {
            // it's may be a trait name or a type name
            // we need to look ahead to see if there is a "for" token
            // we just need to check the (i + 1)th token
            if (i + 1 < tokens.size() && tokens.get(i + 1).name.equals("for")) {
                // it's a trait impl
                IdentifierNode trait = new IdentifierNode();
                parse(trait);
                node.trait = trait;
                // consume for
                i++;
            }
        }
        TypeExprNode typeName = new TypeExprNode();
        parse(typeName);
        node.typeName = typeName;
        // expect {
        if (i < tokens.size() && tokens.get(i).tokentype.name == "{") {
            i++;
        } else {
            assert false : "Expected '{' after impl type";
        }
        Vector<AssoItemNode> items = new Vector<>();
        while (i < tokens.size() && tokens.get(i).tokentype.name != "}") {
            AssoItemNode item = new AssoItemNode();
            parse(item);
            items.add(item);
        }
        node.items = items;
        if (i < tokens.size() && tokens.get(i).tokentype.name == "}") {
            i++;
        } else {
            assert false : "Expected '}' at end of impl body";
        }
    }

    public void parse(AssoItemNode node) {
        // It can be a function or a const item
        // the function may be const
        if (i < tokens.size() && tokens.get(i).name.equals("fn")) {
            FunctionNode funcNode = new FunctionNode();
            funcNode.isConst = false;
            parse(funcNode);
            node = funcNode;
        } else if (i < tokens.size() && tokens.get(i).name.equals("const")) {
            // It may be a const item or a const function
            if (i + 1 < tokens.size() && tokens.get(i + 1).name.equals("fn")) {
                FunctionNode funcNode = new FunctionNode();
                funcNode.isConst = true;
                // consume const
                i++;
                parse(funcNode);
                node = funcNode;
            } else {
                ConstItemNode constNode = new ConstItemNode();
                parse(constNode);
                node = constNode;
            }
        } else {
            assert false : "Expected 'fn' or 'const' in associated item";
        }
    }


    public void parse(LiteralExprNode node) {
        if (i < tokens.size()) {
            token_t token = tokens.get(i);
            if (token.tokentype == token_t.TokenType_t.INTEGER_LITERAL) {
                node.literalType = literal_t.INTEGER;
                node.value_int = Integer.parseInt(token.name);
                i++;
            } else if (token.name.equals("true") || token.name.equals("false")) {
                node.literalType = literal_t.BOOL;
                if (token.name.equals("true")) {
                    node.value_bool = true;
                } else if (token.name.equals("false")) {
                    node.value_bool = false;
                }
                i++;
            } else if (token.tokentype == token_t.TokenType_t.CHAR_LITERAL) {
                node.literalType = literal_t.CHAR;
                node.value_string = token.name;
                i++;
            } else if (token.tokentype == token_t.TokenType_t.STRING_LITERAL || token.tokentype == token_t.TokenType_t.RAW_STRING_LITERAL || token.tokentype == token_t.TokenType_t.C_STRING_LITERAL || token.tokentype == token_t.TokenType_t.RAW_C_STRING_LITERAL) {
                node.literalType = literal_t.STRING;
                node.value_string = token.name;
                if (token.tokentype == token_t.TokenType_t.C_STRING_LITERAL || token.tokentype == token_t.TokenType_t.RAW_C_STRING_LITERAL) {
                    node.value_string += "\0"; // add null terminator for C-style strings
                }
                i++;
            } else {
                assert false : "Expected literal in literal expression";
            }
        } else {
            assert false : "No more tokens to parse in literal expression";
        }
    }

    public void parse(PathExprNode node) {
        if (i < tokens.size()) {
            PathExprSegNode LSeg = new PathExprSegNode();
            parse(LSeg);
            node.LSeg = LSeg;
        } else {
            assert false : "Expected path segment in path expression";
        }
        if (i < tokens.size() && tokens.get(i).tokentype.name == "::") {
            i++;
            if (i < tokens.size()) {
                PathExprSegNode RSeg = new PathExprSegNode();
                parse(RSeg);
                node.RSeg = RSeg;
            } else {
                assert false : "Expected path segment after '::' in path expression";
            }
        } else {
            node.RSeg = null;
        }
    }

    public void parse(PathExprSegNode node) {
        if (i < tokens.size() && token_t.isIdentifier(tokens.get(i))) {
            IdentifierNode name = new IdentifierNode();
            parse(name);
            node.name = name;
            node.patternType = patternSeg_t.IDENTIFIER;
        } else if (i < tokens.size() && tokens.get(i).tokentype.name.equals("self")) {
            node.patternType = patternSeg_t.SELF;
            i++;
        } else if (i < tokens.size() && tokens.get(i).tokentype.name.equals("Self")) {
            node.patternType = patternSeg_t.SELF_TYPE;
            i++;
        } else {
            assert false : "Expected identifier, 'self' or 'Self' in path segment";
        }
    }

    public void parse(ArrayExprNode node) {
        // expect [
        if (i < tokens.size() && tokens.get(i).tokentype.name == "[") {
            i++;
        } else {
            assert false : "Expected '[' at start of array expression";
        }
        Vector<ExprStmtNode> elements = new Vector<>();
        boolean isList = true, isFirst = true;
        while (i < tokens.size() && tokens.get(i).tokentype.name != "]") {
            ExprStmtNode element = new ExprStmtNode();
            parse(element);
            elements.add(element);
            if (i < tokens.size() && tokens.get(i).tokentype.name == ",") {
                i++;
                isFirst = false;
            } else if (i < tokens.size() && tokens.get(i).tokentype.name == ";") {
                if (!isFirst) {
                    assert false : "Unexpected ';' in array expression";
                }
                isList = false;
                i++;
                ExprStmtNode size = new ExprStmtNode();
                parse(size);
                node.size = size;
                node.repeatedElement = element;
                break;
            } else if (i >= tokens.size() || tokens.get(i).tokentype.name != "]") {
                assert false : "Expected ',' or ']' in array expression";
            }
        }
        node.isList = isList;
        if (isList) {
            node.elements = elements;
            node.repeatedElement = null;
            node.size = null;
        } else {
            node.elements = null;
        }
    }

    public void parse(IndexExprNode node) {
        ExprStmtNode array = new ExprStmtNode();
        parse(array);
        node.array = array;
        // expect [
        if (i < tokens.size() && tokens.get(i).tokentype.name == "[") {
            i++;
        } else {
            assert false : "Expected '[' at start of index expression";
        }
        ExprStmtNode index = new ExprStmtNode();
        parse(index);
        node.index = index;
        // expect ]
        if (i < tokens.size() && tokens.get(i).tokentype.name == "]") {
            i++;
        } else {
            assert false : "Expected ']' at end of index expression";
        }
    }

    public void parse(StructExprNode node) {
        PathExprNode structName = new PathExprNode();
        parse(structName);
        node.structName = structName;
        // expect {
        if (i < tokens.size() && tokens.get(i).tokentype.name == "{") {
            i++;
        } else {
            assert false : "Expected '{' at start of struct expression";
        }
        Vector<FieldValNode> fieldAssignments = new Vector<>();
        while (i < tokens.size() && tokens.get(i).tokentype.name != "}") {
            FieldValNode fieldVal = new FieldValNode();
            parse(fieldVal);
            fieldAssignments.add(fieldVal);
            if (i < tokens.size() && tokens.get(i).tokentype.name == ",") {
                i++;
            } else if (i >= tokens.size() || tokens.get(i).tokentype.name != "}") {
                assert false : "Expected ',' or '}' in field assignment list";
            }
        }
        node.fieldAssignments = fieldAssignments;
        // expect }
        if (i < tokens.size() && tokens.get(i).tokentype.name == "}") {
            i++;
        } else {
            assert false : "Expected '}' at end of struct expression";
        }
    }

    public void parse(CallExprNode node) {
        ExprStmtNode function = new ExprStmtNode();
        parse(function);
        node.function = function;
        // expect (
        if (i < tokens.size() && tokens.get(i).tokentype.name == "(") {
            i++;
        } else {
            assert false : "Expected '(' at start of call expression";
        }
        Vector<ExprStmtNode> arguments = new Vector<>();
        while (i < tokens.size() && tokens.get(i).tokentype.name != ")") {
            ExprStmtNode argument = new ExprStmtNode();
            parse(argument);
            arguments.add(argument);
            if (i < tokens.size() && tokens.get(i).tokentype.name == ",") {
                i++;
            } else if (i >= tokens.size() || tokens.get(i).tokentype.name != ")") {
                assert false : "Expected ',' or ')' in argument list";
            }
        }
        node.arguments = arguments;
        // expect )
        if (i < tokens.size() && tokens.get(i).tokentype.name == ")") {
            i++;
        } else {
            assert false : "Expected ')' at end of argument list";
        }
    }

    public void parse(MethodCallExprNode node) {
        ExprStmtNode receiver = new ExprStmtNode();
        parse(receiver);
        node.receiver = receiver;
        // expect .
        if (i < tokens.size() && tokens.get(i).tokentype.name == ".") {
            i++;
        } else {
            assert false : "Expected '.' before method name in method call";
        }
        PathExprSegNode methodName = new PathExprSegNode();
        parse(methodName);
        node.methodName = methodName;
        // expect (
        if (i < tokens.size() && tokens.get(i).tokentype.name == "(") {
            i++;
        } else {
            assert false : "Expected '(' at start of method call";
        }
        Vector<ExprStmtNode> arguments = new Vector<>();
        while (i < tokens.size() && tokens.get(i).tokentype.name != ")") {
            ExprStmtNode argument = new ExprStmtNode();
            parse(argument);
            arguments.add(argument);
            if (i < tokens.size() && tokens.get(i).tokentype.name == ",") {
                i++;
            } else if (i >= tokens.size() || tokens.get(i).tokentype.name != ")") {
                assert false : "Expected ',' or ')' in method call argument list";
            }
        }
        node.arguments = arguments;
        // expect )
        if (i < tokens.size() && tokens.get(i).tokentype.name == ")") {
            i++;
        } else {
            assert false : "Expected ')' at end of method call argument list";
        }
    }

    public void parse(FieldExprNode node) {
        ExprStmtNode receiver = new ExprStmtNode();
        parse(receiver);
        node.receiver = receiver;
        // expect .
        if (i < tokens.size() && tokens.get(i).tokentype.name == ".") {
            i++;
        } else {
            assert false : "Expected '.' before field name in field expression";
        }
        if (i < tokens.size() && token_t.isIdentifier(tokens.get(i))) {
            IdentifierNode fieldName = new IdentifierNode();
            parse(fieldName);
            node.fieldName = fieldName;
        } else {
            assert false : "Expected identifier as field name in field expression";
        }
    }

    public void parse(ContinueExprNode node) {
        // consume "continue"
        i++;
    }

    public void parse(BreakExprNode node) {
        // consume "break"
        i++;
        if (i < tokens.size() && tokens.get(i).tokentype.name != ";") {
            ExprStmtNode value = new ExprStmtNode();
            parse(value);
            node.value = value;
        } else {
            node.value = null;
        }
    }

    public void parse(ReturnExprNode node) {
        // consume "return"
        i++;
        if (i < tokens.size() && tokens.get(i).tokentype.name != ";") {
            ExprStmtNode value = new ExprStmtNode();
            parse(value);
            node.value = value;
        } else {
            node.value = null;
        }
    }

    public void parse(UnderscoreExprNode node) {
        // consume "_"
        i++;
    }





    public void parse(ExprWithBlockNode node) {
        // if the expression is an if-expression
        if (i < tokens.size() && tokens.get(i).name.equals("if")) {
            IfExprNode ifNode = new IfExprNode();
            parse(ifNode);
            node = ifNode;
        } else if (i < tokens.size() && tokens.get(i).name.equals("while")) {
            LoopExprNode loopNode = new LoopExprNode();
            parse(loopNode);
            node = loopNode;
        } else if (i < tokens.size() && tokens.get(i).name.equals("loop")) {
            LoopExprNode loopNode = new LoopExprNode();
            parse(loopNode);
            node = loopNode;
        } else if (i < tokens.size() && tokens.get(i).tokentype.name == "{") {
            BlockExprNode blockNode = new BlockExprNode();
            parse(blockNode);
            node = blockNode;
        } else {
            assert false : "Expected 'if', 'while', 'loop' or '{' in block expression";
        }
    }

    public void parse(BlockExprNode node) {
        // expect {
        if (i < tokens.size() && tokens.get(i).tokentype.name == "{") {
            i++;
        } else {
            assert false : "Expected '{' at start of block expression";
        }
        Vector<StmtNode> statements = new Vector<>();
        while (i < tokens.size() && tokens.get(i).tokentype.name != "}") {
            StmtNode stmt = new StmtNode();
            parse(stmt);
            statements.add(stmt);
        }
        node.statements = statements;
        // expect }
        if (i < tokens.size() && tokens.get(i).tokentype.name == "}") {
            i++;
        } else {
            assert false : "Expected '}' at end of block expression";
        }
    }

    public void parse(IfExprNode node) {
        // expect if
        if (i < tokens.size() && tokens.get(i).name.equals("if")) {
            i++;
        } else {
            assert false : "Expected 'if' at start of if expression";
        }
        ExprStmtNode condition = new ExprStmtNode();
        parse(condition);
        node.condition = condition;
        BlockExprNode thenBranch = new BlockExprNode();
        parse(thenBranch);
        node.thenBranch = thenBranch;
        // check for else or elseif
        if (i < tokens.size() && tokens.get(i).name.equals("else")) {
            i++;
            if (i < tokens.size() && tokens.get(i).name.equals("if")) {
                IfExprNode elseifBranch = new IfExprNode();
                parse(elseifBranch);
                node.elseifBranch = elseifBranch;
                node.elseBranch = null;
            } else {
                BlockExprNode elseBranch = new BlockExprNode();
                parse(elseBranch);
                node.elseBranch = elseBranch;
                node.elseifBranch = null;
            }
        } else {
            node.elseBranch = null;
            node.elseifBranch = null;
        }
    }

    public void parse(LoopExprNode node) {
        // expect while or loop
        if (i < tokens.size() && tokens.get(i).name.equals("while")) {
            i++;
            ExprStmtNode condition = new ExprStmtNode();
            parse(condition);
            node.contidion = condition;
        } else if (i < tokens.size() && tokens.get(i).name.equals("loop")) {
            i++;
            node.contidion = null;
        } else {
            assert false : "Expected 'while' or 'loop' at start of loop expression";
        }
        BlockExprNode body = new BlockExprNode();
        parse(body);
        node.body = body;
    }

    // public void parse()

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