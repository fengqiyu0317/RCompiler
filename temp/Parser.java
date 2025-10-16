import java.util.Vector;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

public class Parser {
    private Vector<token_t> tokens;

    public Parser(Vector<token_t> tokens) {
        this.tokens = tokens;
    }

    int i = 0;

    // we need a list to store all the keywords in Rust
    private static final Set<String> keywords = new HashSet<>(Arrays.asList(
        "as", "break", "const", "continue", "crate", "else", "enum", "extern", "false", "fn", "for", "if", "impl", "in", 
        "let", "loop", "match", "mod", "move", "mut", "ref", "return", "self", "Self", "static", "struct", 
        "super", "trait", "true", "type", "unsafe", "use", "where", "while"
    ));
    public boolean isIdentifier(token_t token) {
        // an identifier is a token whose type is IDENTIFIER_OR_KEYWORD and its name is not a keyword
        return token.tokentype == token_t.TokenType_t.IDENTIFIER_OR_KEYWORD && !keywords.contains(token.name);
    }


    public LetStmtNode parseLetStmtNode() {
        i++;
        PatternNode pattern = parsePatternNode();
        LetStmtNode node = new LetStmtNode();
        node.name = pattern;
        if (i < tokens.size() && tokens.get(i).name.equals(":")) {
            i++;
            TypeExprNode type = parseTypeExprNode();
            node.type = type;
        } else {
            assert false : "Expected ':' after pattern in let statement";
        }
        if (i < tokens.size() && tokens.get(i).name.equals("=")) {
            i++;
            ExprNode expr = parseExprNode();
            node.value = expr;
        } else {
            node.value = null;
        }
        if (i < tokens.size() && tokens.get(i).name.equals(";")) {
            i++;
        } else {
            assert false : "Expected ';' at end of let statement";
        }
        return node;
    }

    public FunctionNode parseFunctionNode() {
        i++;
        FunctionNode node = new FunctionNode();
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
            node.name = parseIdentifierNode();
        } else {
            assert false : "Expected function name after 'fn'";
        }
        if (i < tokens.size() && tokens.get(i).name.equals("(")) {
            i++;
        } else {
            assert false : "Expected '(' after function name";
        }
        node.selfPara = parseSelfParaNode();
        Vector<ParameterNode> parameters = new Vector<>();
        while (i < tokens.size() && !tokens.get(i).name.equals(")")) {
            parameters.add(parseParameterNode());
            if (i < tokens.size() && tokens.get(i).name.equals(",")) {
                i++;
            } else if (i >= tokens.size() || !tokens.get(i).name.equals(")")) {
                assert false : "Expected ',' or ')' in parameter list";
            }
        }
        node.parameters = parameters;
        if (i < tokens.size() && tokens.get(i).name.equals(")")) {
            i++;
        } else {
            assert false : "Expected ')' at end of parameter list"; 
        }
        if (i < tokens.size() && tokens.get(i).name.equals("->")) {
            i++;
            node.returnType = parseTypeExprNode();
        } else {
            node.returnType = null;
        }
        if (i < tokens.size() && tokens.get(i).name.equals("{")) {
            i++;
            node.body = parseBlockExprNode();
        } else {
            if (i < tokens.size() && tokens.get(i).name.equals(";")) {
                i++;
                node.body = null;
            } else {
                assert false : "Expected '{' or ';' after function signature";
            }
        }
        return node;
    }

    public SelfParaNode parseSelfParaNode() {
        int start = i; boolean istyped = true;
        SelfParaNode node = new SelfParaNode();
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
            i++;
            if (istyped && i < tokens.size() && tokens.get(i).name.equals(":")) {
                i++;
                node.type = parseTypeExprNode();
            }
            return node;
        } else {
            i = start;
            return null;
        }
    }

    public ParameterNode parseParameterNode() {
        ParameterNode node = new ParameterNode();
        node.name = parsePatternNode();
        if (i < tokens.size() && tokens.get(i).name.equals(":")) {
            i++;
            node.type = parseTypeExprNode();
        } else {
            assert false : "Expected ':' after parameter pattern";
        }
        return node;
    }

    public PatternNode parsePatternNode() {
        if (i >= tokens.size()) {
            assert false : "No more tokens to parse in pattern";
        }
        token_t token = tokens.get(i);
        if (token.name.equals("&") || token.name.equals("&&")) {
            RefPatNode refNode = new RefPatNode();
            i++;
            if (i < tokens.size() && tokens.get(i).name.equals("mut")) {
                refNode.isMutable = true;
                i++;
            }
            refNode.innerPattern = parsePatternNode();
            return refNode;
        } else if (token.name.equals("_")) {
            WildPatNode wildNode = new WildPatNode();
            i++;
            return wildNode;
        } 
        IdPatNode idNode = new IdPatNode();
        if (tokens.get(i).name.equals("ref")) {
            idNode.isReference = true;
            i++;
        }
        if (i < tokens.size() && tokens.get(i).name.equals("mut")) {
            idNode.isMutable = true;
            i++;
        }
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
            idNode.name = parseIdentifierNode();
            return idNode;
        } else {
            assert false : "Expected identifier in pattern";
        }
        return idNode;
    }

    public StructNode parseStructNode() {
        i++;
        StructNode node = new StructNode();
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
            node.name = parseIdentifierNode();
        } else {
            assert false : "Expected struct name after 'struct'";
        }
        if (i < tokens.size() && tokens.get(i).name.equals("{")) {
            i++;
        } else {
            if (i < tokens.size() && tokens.get(i).name.equals(";")) {
                i++;
                node.fields = null;
                return node;
            } else {
                assert false : "Expected '{' or ';' after struct name";
            }
        }
        Vector<FieldNode> fields = new Vector<>();
        while (i < tokens.size() && !tokens.get(i).name.equals("}")) {
            fields.add(parseFieldNode());
            if (i < tokens.size() && tokens.get(i).name.equals(",")) {
                i++;
            } else if (i >= tokens.size() || !tokens.get(i).name.equals("}")) {
                assert false : "Expected ',' or '}' in field list";
            }
        }
        node.fields = fields;
        if (i < tokens.size() && tokens.get(i).name.equals("}")) {
            i++;
        } else {
            assert false : "Expected '}' at end of field list";
        }
        return node;
    }

    public FieldNode parseFieldNode() {
        FieldNode node = new FieldNode();
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
            node.name = parseIdentifierNode();
        } else {
            assert false : "Expected field name in struct";
        }
        if (i < tokens.size() && tokens.get(i).name.equals(":")) {
            i++;
        } else {
            assert false : "Expected ':' after field name in struct";
        }
        node.type = parseTypeExprNode();
        return node;
    }

    public EnumNode parseEnumNode() {
        i++;
        EnumNode node = new EnumNode();
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
            node.name = parseIdentifierNode();
        } else {
            assert false : "Expected enum name after 'enum'";
        }
        if (i < tokens.size() && tokens.get(i).name.equals("{")) {
            i++;
        } else {
            assert false : "Expected '{' after enum name";
        }
        Vector<IdentifierNode> variants = new Vector<>();
        while (i < tokens.size() && !tokens.get(i).name.equals("}")) {
            if (i < tokens.size() && isIdentifier(tokens.get(i))) {
                variants.add(parseIdentifierNode());
            } else {
                assert false : "Expected variant name in enum";
            }
            if (i < tokens.size() && tokens.get(i).name.equals(",")) {
                i++;
            } else if (i >= tokens.size() || !tokens.get(i).name.equals("}")) {
                assert false : "Expected ',' or '}' in variant list";
            }
        }
        node.variants = variants;
        if (i < tokens.size() && tokens.get(i).name.equals("}")) {
            i++;
        } else {
            assert false : "Expected '}' at end of variant list";
        }
        return node;
    }

    public ConstItemNode parseConstItemNode() {
        i++;
        ConstItemNode node = new ConstItemNode();
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
            node.name = parseIdentifierNode();
        } else {
            assert false : "Expected const name after 'const'";
        }
        if (i < tokens.size() && tokens.get(i).name.equals(":")) {
            i++;
            node.type = parseTypeExprNode();
        } else {
            assert false : "Expected ':' after const name";
        }
        if (i < tokens.size() && tokens.get(i).name.equals("=")) {
            i++;
            node.value = parseExprNode();
        }
        if (i < tokens.size() && tokens.get(i).name.equals(";")) {
            i++;
        } else {
            assert false : "Expected ';' at end of const item";
        }
        return node;
    }

    public TraitNode parseTraitNode() {
        i++;
        TraitNode node = new TraitNode();
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
            node.name = parseIdentifierNode();
        } else {
            assert false : "Expected trait name after 'trait'";
        }
        if (i < tokens.size() && tokens.get(i).name.equals("{")) {
            i++;
        } else {
            assert false : "Expected '{' after trait name";
        }
        Vector<AssoItemNode> items = new Vector<>();
        while (i < tokens.size() && !tokens.get(i).name.equals("}")) {
            items.add(parseAssoItemNode());
        }
        node.items = items;
        if (i < tokens.size() && tokens.get(i).name.equals("}")) {
            i++;
        } else {
            assert false : "Expected '}' at end of trait body";
        }
        return node;
    }

    public ImplNode parseImplNode() {
        // consume the "impl" token
        i++;
        ImplNode node = new ImplNode();
        // check if it's an inherent impl or a trait impl
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
            // it's may be a trait name or a type name
            // we need to look ahead to see if there is a "for" token
            // we just need to check the (i + 1)th token
            if (i + 1 < tokens.size() && tokens.get(i + 1).name.equals("for")) {
                // it's a trait impl
                IdentifierNode trait = new IdentifierNode();
                node.trait = parseIdentifierNode();
                // consume for
                i++;
            }
        }
        TypeExprNode typeName = new TypeExprNode();
        node.typeName = parseTypeExprNode();
        // expect {
        if (i < tokens.size() && tokens.get(i).name.equals("{")) {
            i++;
        } else {
            assert false : "Expected '{' after impl type";
        }
        Vector<AssoItemNode> items = new Vector<>();
        while (i < tokens.size() && !tokens.get(i).name.equals("}")) {
            AssoItemNode item = new AssoItemNode();
            items.add(parseAssoItemNode());
        }
        node.items = items;
        if (i < tokens.size() && tokens.get(i).name.equals("}")) {
            i++;
        } else {
            assert false : "Expected '}' at end of impl body";
        }
        return node;
    }

    public AssoItemNode parseAssoItemNode() {
        // It can be a function or a const item
        // the function may be const
        AssoItemNode node = new AssoItemNode();
        if (i < tokens.size() && tokens.get(i).name.equals("fn")) {
            FunctionNode funcNode = new FunctionNode();
            funcNode.isConst = false;
            node.function = parseFunctionNode();
        } else if (i < tokens.size() && tokens.get(i).name.equals("const")) {
            // It may be a const item or a const function
            if (i + 1 < tokens.size() && tokens.get(i + 1).name.equals("fn")) {
                FunctionNode funcNode = new FunctionNode();
                funcNode.isConst = true;
                // consume const
                i++;
                node.function = parseFunctionNode();
            } else {
                ConstItemNode constNode = new ConstItemNode();
                node.constant = parseConstItemNode();
            }
        } else {
            assert false : "Expected 'fn' or 'const' in associated item";
        }
        return node;
    }

    public ExprStmtNode parseExprStmtNode() {
        assert i < tokens.size() : "No more tokens to parse in expression statement";
        token_t token = tokens.get(i);
        ExprStmtNode node = new ExprStmtNode();
        // check if it's an expression with block
        if (token.name.equals("if") || token.name.equals("while") || token.name.equals("loop") || token.name == "{") {
            ExprWithBlockNode blockNode = new ExprWithBlockNode();
            node.expr = parseExprWithBlockNode();
            return node;
        }
        // otherwise it's an expression without block
        ExprWithoutBlockNode exprNode = new ExprWithoutBlockNode();
        node.expr = parseExprWithoutBlockNode();
        // expect semicolon
        if (i < tokens.size() && tokens.get(i).name.equals(";")) {
            i++;
        } else {
            assert false : "Expected ';' at end of expression statement";
        }
        return node;
    }

    public ExprNode parseExprNode(int precedence) {
        assert i < tokens.size() : "No more tokens to parse in expression";
        token_t token = tokens.get(i);
        if (token.name.equals("if") || token.name.equals("while") || token.name.equals("loop") || token.name == "{") {
            return parseExprWithBlockNode();
        } else {
            return parseExprWithoutBlockNode(precedence);
        }
    }

    public ExprNode parseExprNode() {
        return parseExprNode(0);
    }

    // we write a function: for tokens (arg1, arg2, ..., argn), the function return a vector of ExprNode
    public Vector<ExprNode> parseFunctionArgs() {
        Vector<ExprNode> args = new Vector<>();
        // expect (
        if (i < tokens.size() && tokens.get(i).name.equals("(")) {
            i++;
        } else {
            assert false : "Expected '(' at start of function argument list";
        }
        while (i < tokens.size() && !tokens.get(i).name.equals(")")) {
            ExprNode arg = new ExprNode();
            args.add(parseExprWithoutBlockNode());
            if (i < tokens.size() && tokens.get(i).name.equals(",")) {
                i++;
            } else if (i >= tokens.size() || !tokens.get(i).name.equals(")")) {
                assert false : "Expected ',' or ')' in function argument list";
            }
        }
        // expect )
        if (i < tokens.size() && tokens.get(i).name.equals(")")) {
            i++;
        } else {
            assert false : "Expected ')' at end of function argument list";
        }
        return args;
    }


    // we need use the function "isOper" to check if a token is a binary operator; there are arith, comp, lazy, assign, comassign operators
    public boolean isOper(token_t token) {
        // arith operators: +, -, *, /, %, &, |, ^, <<, >>
        if (token.name.equals("+") || token.name.equals("-") || token.name.equals("*") || token.name.equals("/") || token.name.equals("%") || token.name.equals("&") || token.name.equals("|") || token.name.equals("^") || token.name.equals("<<") || token.name.equals(">>")) {
            return true;
        }
        // comp operators: ==, !=, <, <=, >, >=
        if (token.name.equals("==") || token.name.equals("!=") || token.name.equals("<") || token.name.equals("<=") || token.name.equals(">") || token.name.equals(">=")) {
            return true;
        }
        // lazy operators: &&, ||
        if (token.name.equals("&&") || token.name.equals("||")) {
            return true;
        }
        // assign operators: =
        if (token.name.equals("=")) {
            return true;
        }
        // comassign operators: +=, -=, *=, /=, %=, |=, ^=, <<=, >>=, &=
        if (token.name.equals("+=") || token.name.equals("-=") || token.name.equals("*=") || token.name.equals("/=") || token.name.equals("%=") || token.name.equals("|=") || token.name.equals("^=") || token.name.equals("<<=") || token.name.equals(">>=") || token.name.equals("&=")) {
            return true;
        }
        return false;
    }
    public boolean isArith(token_t token) {
        // arith operators: +, -, *, /, %, &, |, ^, <<, >>
        return token.name.equals("+") || token.name.equals("-") || token.name.equals("*") || token.name.equals("/") || token.name.equals("%") || token.name.equals("&") || token.name.equals("|") || token.name.equals("^") || token.name.equals("<<") || token.name.equals(">>");
    }
    public boolean isComp(token_t token) {
        // comp operators: ==, !=, <, <=, >, >=
        return token.name.equals("==") || token.name.equals("!=") || token.name.equals("<") || token.name.equals("<=") || token.name.equals(">") || token.name.equals(">=");
    }
    public boolean isLazy(token_t token) {
        // lazy operators: &&, ||
        return token.name.equals("&&") || token.name.equals("||");
    }
    public boolean isAssignOper(token_t token) {
        // assign operators: =
        return token.name.equals("=");
    }
    public boolean isComAssignOper(token_t token) {
        // comassign operators: +=, -=, *=, /=, %=, |=, ^=, <<=, >>=, &=
        return token.name.equals("+=") || token.name.equals("-=") || token.name.equals("*=") || token.name.equals("/=") || token.name.equals("%=") || token.name.equals("|=") || token.name.equals("^=") || token.name.equals("<<=") || token.name.equals(">>=") || token.name.equals("&=");
    }
    public oper_t getOper(String name) {
        switch (name) {
            case "+":
                return oper_t.PLUS;
            case "-":
                return oper_t.MINUS;
            case "*":
                return oper_t.MUL;
            case "/":
                return oper_t.DIV;
            case "%":
                return oper_t.MOD;
            case "&":
                return oper_t.AND;
            case "|":
                return oper_t.OR;
            case "^":
                return oper_t.XOR;
            case "<<":
                return oper_t.SHL;
            case ">>":
                return oper_t.SHR;
            case "==":
                return oper_t.EQ;
            case "!=":
                return oper_t.NEQ;
            case "<":
                return oper_t.LT;
            case "<=":
                return oper_t.LTE;
            case ">":
                return oper_t.GT;
            case ">=":
                return oper_t.GTE;
            case "&&":
                return oper_t.LOGICAL_AND;
            case "||":
                return oper_t.LOGICAL_OR;
            case "=":
                return oper_t.ASSIGN;
            case "+=":
                return oper_t.PLUS_ASSIGN;
            case "-=":
                return oper_t.MINUS_ASSIGN;
            case "*=":
                return oper_t.MUL_ASSIGN;
            case "/=":
                return oper_t.DIV_ASSIGN;
            case "%=":
                return oper_t.MOD_ASSIGN;
            case "&=":
                return oper_t.AND_ASSIGN;
            case "|=":
                return oper_t.OR_ASSIGN;
            case "^=":
                return oper_t.XOR_ASSIGN;
            case "<<=":
                return oper_t.SHL_ASSIGN;
            case ">>=":
                return oper_t.SHR_ASSIGN;
            default:
                assert false : "Unknown operator: " + name;
                return null; // to satisfy the compiler
        }
    }
    // we also need a function "getPrecedence" to get the precedence of a binary operator; there are 10 levels of precedence for the operators we support; the higher the number, the higher the precedence; 
    // the precedence levels are as follows:
    // 1. * / %
    // 2. + -
    // 3. << >>
    // 4. &
    // 5. ^
    // 6. |
    // 7. == != < <= > >=
    // 8. &&
    // 9. ||
    // 10. = += -= *= /= %= &= |= ^= <<= >>=
    // we define the precedence of "*" is 110; 
    public int getPrecedence(token_t token) {
        if (token.name.equals("*") || token.name.equals("/") || token.name.equals("%")) {
            return 110;
        }
        if (token.name.equals("+") || token.name.equals("-")) {
            return 100;
        }
        if (token.name.equals("<<") || token.name.equals(">>")) {
            return 90;
        }
        if (token.name.equals("&")) {
            return 80;
        }
        if (token.name.equals("^")) {
            return 70;
        }
        if (token.name.equals("|")) {
            return 60;
        }
        if (token.name.equals("==") || token.name.equals("!=") || token.name.equals("<") || token.name.equals("<=") || token.name.equals(">") || token.name.equals(">=")) {
            return 50;
        }
        if (token.name.equals("&&")) {
            return 40;
        }
        if (token.name.equals("||")) {
            return 30;
        }
        if (token.name.equals("=") || token.name.equals("+=") || token.name.equals("-=") || token.name.equals("*=") || token.name.equals("/=") || token.name.equals("%=") || token.name.equals("&=") || token.name.equals("|=") || token.name.equals("^=") || token.name.equals("<<=") || token.name.equals(">>=")) {
            return 20;
        }
        return 0;
    }

    public GroupExprNode parseGroupExprNode() {
        if (i < tokens.size() && tokens.get(i).name.equals("(")) {
            i++;
        } else {
            assert false : "Expected '(' at start of grouped expression";
        }
        GroupExprNode node = new GroupExprNode();
        node.innerExpr = parseExprNode(0);
        if (i < tokens.size() && tokens.get(i).name.equals(")")) {
            i++;
        } else {
            assert false : "Expected ')' at end of grouped expression";
        }
        return node;
    }

    public ExprWithoutBlockNode parseExprWithoutBlockNode(int precedence) {
        assert i < tokens.size() : "No more tokens to parse in expression without block";
        token_t token = tokens.get(i);
        ExprWithoutBlockNode node = new ExprWithoutBlockNode();

        if (token.tokentype == token_t.TokenType_t.INTEGER_LITERAL || token.tokentype == token_t.TokenType_t.CHAR_LITERAL || token.tokentype == token_t.TokenType_t.STRING_LITERAL || token.tokentype == token_t.TokenType_t.RAW_STRING_LITERAL || token.tokentype == token_t.TokenType_t.C_STRING_LITERAL || token.tokentype == token_t.TokenType_t.RAW_C_STRING_LITERAL || token.name.equals("true") || token.name.equals("false")) {
            LiteralExprNode litNode = new LiteralExprNode();
            node = parseLiteralExprNode();
        } else if (isIdentifier(token) || token.name.equals("self") || token.name.equals("Self")) {
            PathExprNode path = new PathExprNode();
            path.LSeg = new PathExprSegNode();
            path.LSeg = parsePathExprSegNode();
            node = path;
        } else if (token.name.equals("[")) {
            ArrayExprNode arrayNode = new ArrayExprNode();
            node = parseArrayExprNode();
        } else if (token.name.equals("(")) {
            GroupExprNode groupNode = new GroupExprNode();
            node = parseGroupExprNode();
        } else if (token.name.equals("&") || token.name.equals("&&")) {
            BorrowExprNode borrowNode = new BorrowExprNode();
            node = parseBorrowExprNode();
        } else if (token.name.equals("*")) {
            DerefExprNode derefNode = new DerefExprNode();
            node = parseDerefExprNode();
        } else if (token.name.equals("-") || token.name.equals("~")) {
            NegaExprNode negaNode = new NegaExprNode();
            node = parseNegaExprNode();
        } else if (token.name.equals("continue")) {
            ContinueExprNode contNode = new ContinueExprNode();
            node = parseContinueExprNode();
        } else if (token.name.equals("break")) {
            BreakExprNode breakNode = new BreakExprNode();
            node = parseBreakExprNode();
        } else if (token.name.equals("return")) {
            ReturnExprNode retNode = new ReturnExprNode();
            node = parseReturnExprNode();
        } else if (token.name.equals("_")) {
            UnderscoreExprNode underNode = new UnderscoreExprNode();
            node = parseUnderscoreExprNode();
        }
        while (i < tokens.size()) {
            token = tokens.get(i);
            if (token.name.equals(".")) {
                // for a field access, the next token must be an identifier; but for a method call, the next token is a PathExprSegNode.
                // the next expression may be a field access or a method call; the difference between them is whether there is a '(' after the next token
                // for the field access, we use the FieldExprNode; for the method call, we use the MethodCallExprNode
                i++;
                PathExprSegNode pathSeg = new PathExprSegNode();
                pathSeg = parsePathExprSegNode();
                if (i < tokens.size() && tokens.get(i).name.equals("(")) {
                    // it's a method call
                    MethodCallExprNode methodCallNode = new MethodCallExprNode();
                    methodCallNode.receiver = node;
                    methodCallNode.methodName = pathSeg;
                    Vector<ExprNode> arguments = parseFunctionArgs();
                    methodCallNode.arguments = arguments;
                    node = methodCallNode;
                } else {
                    // it's a field access
                    FieldExprNode fieldNode = new FieldExprNode();
                    fieldNode.receiver = node;
                    if (pathSeg.patternType == patternSeg_t.IDENT) {
                        fieldNode.fieldName = pathSeg.name;
                    } else {
                        assert false : "Expected identifier after '.' in field access";
                    }
                    node = fieldNode;
                }
            } else if (isOper(token)) {
                int opPrecedence = getPrecedence(token);
                if (opPrecedence <= precedence) {
                    break;
                }
                // get the operator
                oper_t operator = getOper(token.name);
                // check the type of operator
                if (isComAssignOper(token)) {
                    ComAssignExprNode comAssignNode = new ComAssignExprNode();
                    comAssignNode.operator = operator;
                    i++;
                    ExprNode right = new ExprNode();
                    comAssignNode.left = node;
                    comAssignNode.right = parseExprWithoutBlockNode(opPrecedence);
                    node = comAssignNode;
                } else if (isAssignOper(token)) {
                    AssignExprNode assignNode = new AssignExprNode();
                    assignNode.operator = operator;
                    i++;
                    ExprNode right = new ExprNode();
                    assignNode.left = node;
                    assignNode.right = parseExprWithoutBlockNode(opPrecedence);
                    node = assignNode;
                } else if (isComp(token)) {
                    CompExprNode compNode = new CompExprNode();
                    compNode.operator = operator;
                    i++;
                    ExprNode right = new ExprNode();
                    compNode.left = node;
                    compNode.right = parseExprWithoutBlockNode(opPrecedence);
                    node = compNode;
                } else if (isArith(token)) {
                    ArithExprNode arithNode = new ArithExprNode();
                    arithNode.operator = operator;
                    i++;
                    ExprNode right = new ExprNode();
                    arithNode.left = node;
                    arithNode.right = parseExprWithoutBlockNode(opPrecedence);
                    node = arithNode;
                } else if (isLazy(token)) {
                    LazyExprNode lazyNode = new LazyExprNode();
                    lazyNode.operator = operator;
                    i++;
                    ExprNode right = new ExprNode();
                    lazyNode.left = node;
                    lazyNode.right = parseExprWithoutBlockNode(opPrecedence);
                    node = lazyNode;
                }
            } else if (token.name.equals("as")) {
                if (precedence >= 120) {
                    break;
                }
                TypeCastExprNode typeCastNode = new TypeCastExprNode();
                i++;
                TypeExprNode targetType = new TypeExprNode();
                typeCastNode.expr = node;
                typeCastNode.type = parseTypeExprNode();
                node = typeCastNode;
            } else if (token.name.equals("::")) {
                if (precedence >= 170) {
                    break;
                }
                i++;
                PathExprSegNode pathSeg = new PathExprSegNode();
                if (node instanceof PathExprNode) {
                    assert ((PathExprNode)node).RSeg == null : "Unexpected state: PathExprNode already has RSeg in path expression";
                    ((PathExprNode)node).RSeg = parsePathExprSegNode();
                    continue;
                }
                assert false : "Expected path segment before '::' in path expression";
            } else if (token.name.equals("[")) {
                // consume [
                i++;
                IndexExprNode indexNode = new IndexExprNode();
                indexNode.array = node;
                indexNode.index = parseExprWithoutBlockNode(precedence);
                node = indexNode;
                // expect ]
                if (i < tokens.size() && tokens.get(i).name.equals("]")) {
                    i++;
                } else {
                    assert false : "Expected ']' at end of index expression";
                }
            } else if (token.name.equals("(")) {
                // it's a function call
                CallExprNode callNode = new CallExprNode();
                callNode.function = node;
                Vector<ExprNode> arguments = parseFunctionArgs();
                callNode.arguments = arguments;
                node = callNode;
            } else if (token.name.equals("{")) {
                // it's a struct expression
                StructExprNode structNode = new StructExprNode();
                if (node instanceof PathExprNode) {
                    structNode.structName = ((PathExprNode)node).LSeg;
                } else {
                    assert false : "Expected path expression before '{' in struct expression";
                }
                i++;
                Vector<FieldValNode> fieldValues = new Vector<>();
                while (i < tokens.size()) {
                    if (tokens.get(i).name.equals("}")) {
                        break;
                    }
                    FieldValNode fieldVal = new FieldValNode();
                    fieldValues.add(parseFieldValNode());
                    if (i < tokens.size() && tokens.get(i).name.equals(",")) {
                        i++;
                    } else if (i < tokens.size() && tokens.get(i).name.equals("}")) {
                        break;
                    } else {
                        assert false : "Expected ',' or '}' in field assignment list";
                    }
                }
                node = structNode;
                structNode.fieldValues = fieldValues;
                // expect }
                if (i < tokens.size() && tokens.get(i).name.equals("}")) {
                    i++;
                } else {
                    assert false : "Expected '}' at end of struct expression";
                }
            } else {
                // if it's not a binary operator; for example, if it's a semicolon, comma, etc., we just break the loop
                // but except some characters like ")", "]", "}", which may be the end of the current expression, other characters isn't legal.
                if (token.name.equals(")") || token.name.equals("]") || token.name.equals("}") || token.name.equals(",") || token.name.equals(";")) {
                    break;
                }
                assert false : "Unexpected token '" + token.name + "' in expression";
            }
        }
        return node;
    }

    public ExprWithoutBlockNode parseExprWithoutBlockNode() {
        return parseExprWithoutBlockNode(0);
    }


    public BorrowExprNode parseBorrowExprNode() {
        BorrowExprNode node = new BorrowExprNode();
        if (tokens.get(i).name.equals("&&")) {
            node.isDoubleReference = true;
            i++;
        } else {
            node.isDoubleReference = false;
            if (tokens.get(i).name.equals("&")) {
                i++;
            } else {
                assert false : "Expected '&' in borrow expression";
            }
        }
        if (i < tokens.size() && tokens.get(i).name.equals("mut")) {
            node.isMutable = true;
            i++;
        } else {
            node.isMutable = false;
        }
        ExprNode innerExpr = new ExprNode();
        node.innerExpr = parseExprWithoutBlockNode(130);
        return node;
    }

    public DerefExprNode parseDerefExprNode() {
        // consume *
        i++;
        DerefExprNode node = new DerefExprNode();
        ExprNode innerExpr = new ExprNode();
        node.innerExpr = parseExprWithoutBlockNode(130);
        return node;
    }

    public NegaExprNode parseNegaExprNode() {
        // consume - or ~
        NegaExprNode node = new NegaExprNode();
        if (tokens.get(i).name.equals("-")) {
            node.isLogical = false;
        } else if (tokens.get(i).name.equals("~")) {
            node.isLogical = true;
        } else {
            assert false : "Expected '-' or '~' in negation expression";
        }
        i++;
        ExprNode innerExpr = new ExprNode();
        node.innerExpr = parseExprWithoutBlockNode(130);
        return node;
    }



    public LiteralExprNode parseLiteralExprNode() {
        LiteralExprNode node = new LiteralExprNode();
        token_t token = tokens.get(i);
        if (token.tokentype == token_t.TokenType_t.INTEGER_LITERAL) {
            node.literalType = literal_t.INTEGER;
            node.value_string = token.name;
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
        } else if (token.name.equals("true")) {
            node.literalType = literal_t.BOOLEAN;
            node.value_string = "true";
            i++;
        } else if (token.name.equals("false")) {
            node.literalType = literal_t.BOOLEAN;
            node.value_string = "false";
            i++;
        } else {
            assert false : "Expected literal in literal expression";
        }
        return node;
    }

    public PathExprNode parsePathExprNode() {
        PathExprNode node = new PathExprNode();
        if (i < tokens.size()) {
            PathExprSegNode LSeg = new PathExprSegNode();
            node.LSeg = parsePathExprSegNode();
        } else {
            assert false : "Expected path segment in path expression";
        }
        if (i < tokens.size() && tokens.get(i).name.equals("::")) {
            i++;
            if (i < tokens.size()) {
                PathExprSegNode RSeg = new PathExprSegNode();
                node.RSeg = parsePathExprSegNode();
            } else {
                assert false : "Expected path segment after '::' in path expression";
            }
        } else {
            node.RSeg = null;
        }
        return node;
    }

    public PathExprSegNode parsePathExprSegNode() {
        PathExprSegNode node = new PathExprSegNode();
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
            // System.out.println(tokens.get(i).name);
            IdentifierNode name = new IdentifierNode();
            node.name = parseIdentifierNode();
            node.patternType = patternSeg_t.IDENT;
        } else if (i < tokens.size() && tokens.get(i).name.equals("self")) {
            node.patternType = patternSeg_t.SELF;
            i++;
        } else if (i < tokens.size() && tokens.get(i).name.equals("Self")) {
            node.patternType = patternSeg_t.SELF_TYPE;
            i++;
        } else {
            assert false : "Expected identifier, 'self' or 'Self' in path segment";
        }
        return node;
    }

    public ArrayExprNode parseArrayExprNode() {
        // expect [
        ArrayExprNode node = new ArrayExprNode();
        if (i < tokens.size() && tokens.get(i).name.equals("[")) {
            i++;
        } else {
            assert false : "Expected '[' at start of array expression";
        }
        Vector<ExprNode> elements = new Vector<>();
        boolean isList = true, isFirst = true;
        while (i < tokens.size() && !tokens.get(i).name.equals("]")) {
            ExprNode element = new ExprNode();
            elements.add(parseExprWithoutBlockNode());
                if (i < tokens.size() && tokens.get(i).name.equals(",")) {
                i++;
                isFirst = false;
                } else if (i < tokens.size() && tokens.get(i).name.equals(";")) {
                if (!isFirst) {
                    assert false : "Unexpected ';' in array expression";
                }
                isList = false;
                i++;
                ExprNode size = new ExprNode();
                node.size = parseExprWithoutBlockNode();
                node.repeatedElement = element;
                break;
                } else if (i >= tokens.size() || !tokens.get(i).name.equals("]")) {
                assert false : "Expected ',' or ']' in array expression";
            }
        }
        // expect ]
        if (i < tokens.size() && tokens.get(i).name.equals("]")) {
            i++;
        } else {
            assert false : "Expected ']' at end of array expression";
        }
        if (isList) {
            node.elements = elements;
            node.repeatedElement = null;
            node.size = null;
        } else {
            node.elements = null;
        }
        return node;
    }

    public IndexExprNode parseIndexExprNode() {
        IndexExprNode node = new IndexExprNode();
        ExprNode array = new ExprNode();
        node.array = parseExprWithoutBlockNode();
        // expect [
        if (i < tokens.size() && tokens.get(i).name.equals("[")) {
            i++;
        } else {
            assert false : "Expected '[' at start of index expression";
        }
        ExprNode index = new ExprNode();
        node.index = parseExprWithoutBlockNode();
        // expect ]
        if (i < tokens.size() && tokens.get(i).name.equals("]")) {
            i++;
        } else {
            assert false : "Expected ']' at end of index expression";
        }
        return node;
    }

    public StructExprNode parseStructExprNode() {
        StructExprNode node = new StructExprNode();
        PathExprSegNode structName = new PathExprSegNode();
        node.structName = parsePathExprSegNode();
        // expect {
        if (i < tokens.size() && tokens.get(i).name.equals("{")) {
            i++;
        } else {
            assert false : "Expected '{' at start of struct expression";
        }
        Vector<FieldValNode> fieldValues = new Vector<>();
        while (i < tokens.size() && !tokens.get(i).name.equals("}")) {
            FieldValNode fieldVal = new FieldValNode();
            fieldValues.add(parseFieldValNode());
                if (i < tokens.size() && tokens.get(i).name.equals(",")) {
                i++;
                } else if (i >= tokens.size() || !tokens.get(i).name.equals("}")) {
                assert false : "Expected ',' or '}' in field assignment list";
            }
        }
        node.fieldValues = fieldValues;
        // expect }
        if (i < tokens.size() && tokens.get(i).name.equals("}")) {
            i++;
        } else {
            assert false : "Expected '}' at end of struct expression";
        }
        return node;
    }

    public FieldValNode parseFieldValNode() {
        FieldValNode node = new FieldValNode();
        // expect identifier
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
            IdentifierNode name = new IdentifierNode();
            node.fieldName = parseIdentifierNode();
        } else {
            assert false : "Expected field name in struct expression";
        }
        // expect :
        if (i < tokens.size() && tokens.get(i).name.equals(":")) {
            i++;
        } else {
            assert false : "Expected ':' after field name in struct expression";
        }
        ExprNode value = new ExprNode();
        node.value = parseExprWithoutBlockNode();
        return node;
    }

    public CallExprNode parseCallExprNode() {
        CallExprNode node = new CallExprNode();
        ExprNode function = new ExprNode();
        node.function = parseExprWithoutBlockNode();
        // expect (
        if (i < tokens.size() && tokens.get(i).name.equals("(")) {
            i++;
        } else {
            assert false : "Expected '(' at start of call expression";
        }
        Vector<ExprNode> arguments = new Vector<>();
        while (i < tokens.size() && !tokens.get(i).name.equals(")")) {
            ExprNode argument = new ExprNode();
            arguments.add(parseExprWithoutBlockNode());
                if (i < tokens.size() && tokens.get(i).name.equals(",")) {
                i++;
            } else if (i >= tokens.size() || !tokens.get(i).name.equals(")")) {
                assert false : "Expected ',' or ')' in argument list";
            }
        }
        node.arguments = arguments;
        // expect )
        if (i < tokens.size() && tokens.get(i).name.equals(")")) {
            i++;
        } else {
            assert false : "Expected ')' at end of argument list";
        }
        return node;
    }

    public MethodCallExprNode parseMethodCallExprNode() {
        MethodCallExprNode node = new MethodCallExprNode();
        ExprNode receiver = new ExprNode();
        node.receiver = parseExprWithoutBlockNode();
        // expect .
        if (i < tokens.size() && tokens.get(i).name.equals(".")) {
            i++;
        } else {
            assert false : "Expected '.' before method name in method call";
        }
        PathExprSegNode methodName = new PathExprSegNode();
        node.methodName = parsePathExprSegNode();
        // expect (
        if (i < tokens.size() && tokens.get(i).name.equals("(")) {
            i++;
        } else {
            assert false : "Expected '(' at start of method call";
        }
        Vector<ExprNode> arguments = new Vector<>();
        while (i < tokens.size() && !tokens.get(i).name.equals(")")) {
            ExprNode argument = new ExprNode();
            arguments.add(parseExprWithoutBlockNode());
                if (i < tokens.size() && tokens.get(i).name.equals(",")) {
                i++;
            } else if (i >= tokens.size() || !tokens.get(i).name.equals(")")) {
                assert false : "Expected ',' or ')' in method call argument list";
            }
        }
        node.arguments = arguments;
        // expect )
        if (i < tokens.size() && tokens.get(i).name.equals(")")) {
            i++;
        } else {
            assert false : "Expected ')' at end of method call argument list";
        }
        return node;
    }

    public FieldExprNode parseFieldExprNode() {
        FieldExprNode node = new FieldExprNode();
        ExprNode receiver = new ExprNode();
        node.receiver = parseExprWithoutBlockNode();
        // expect .
        if (i < tokens.size() && tokens.get(i).name.equals(".")) {
            i++;
        } else {
            assert false : "Expected '.' before field name in field expression";
        }
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
            IdentifierNode fieldName = new IdentifierNode();
            node.fieldName = parseIdentifierNode();
        } else {
            assert false : "Expected identifier as field name in field expression";
        }
        return node;
    }

    public ContinueExprNode parseContinueExprNode() {
        // consume "continue"
        i++;
        ContinueExprNode node = new ContinueExprNode();
        return node;
    }

    public BreakExprNode parseBreakExprNode() {
        // consume "break"
        i++;
        BreakExprNode node = new BreakExprNode();
        if (i < tokens.size() && !tokens.get(i).name.equals(";")) {
            ExprNode value = new ExprNode();
            node.value = parseExprWithoutBlockNode(10);
        } else {
            node.value = null;
        }
        return node;
    }

    public ReturnExprNode parseReturnExprNode() {
        // consume "return"
        i++;
        ReturnExprNode node = new ReturnExprNode();
        if (i < tokens.size() && !tokens.get(i).name.equals(";")) {
            ExprNode value = new ExprNode();
            node.value = parseExprWithoutBlockNode(10);
        } else {
            node.value = null;
        }
        return node;
    }

    public UnderscoreExprNode parseUnderscoreExprNode() {
        // consume "_"
        i++;
        UnderscoreExprNode node = new UnderscoreExprNode();
        return node;
    }





    public ExprWithBlockNode parseExprWithBlockNode() {
        ExprWithBlockNode node = new ExprWithBlockNode();
        // if the expression is an if-expression
        if (i < tokens.size() && tokens.get(i).name.equals("if")) {
            IfExprNode ifNode = new IfExprNode();
            node = parseIfExprNode();
        } else if (i < tokens.size() && tokens.get(i).name.equals("while")) {
            LoopExprNode loopNode = new LoopExprNode();
            node = parseLoopExprNode();
        } else if (i < tokens.size() && tokens.get(i).name.equals("loop")) {
            LoopExprNode loopNode = new LoopExprNode();
            node = parseLoopExprNode();
        } else if (i < tokens.size() && tokens.get(i).name.equals("{")) {
            BlockExprNode blockNode = new BlockExprNode();
            node = parseBlockExprNode();
        } else {
            assert false : "Expected 'if', 'while', 'loop' or '{' in block expression";
        }
        return node;
    }

    public BlockExprNode parseBlockExprNode() {
        BlockExprNode node = new BlockExprNode();
        // expect {
        if (i < tokens.size() && tokens.get(i).name.equals("{")) {
            i++;
        } else {
            assert false : "Expected '{' at start of block expression";
        }
        Vector<StmtNode> statements = new Vector<>();
        while (i < tokens.size() && !tokens.get(i).name.equals("}")) {
            StmtNode stmt = new StmtNode();
            statements.add(parseStmtNode());
        }
        node.statements = statements;
        // expect }
        if (i < tokens.size() && tokens.get(i).name.equals("}")) {
            i++;
        } else {
            assert false : "Expected '}' at end of block expression";
        }
        return node;
    }

    public IfExprNode parseIfExprNode() {
        IfExprNode node = new IfExprNode();
        // expect if
        if (i < tokens.size() && tokens.get(i).name.equals("if")) {
            i++;
        } else {
            assert false : "Expected 'if' at start of if expression";
        }
        ExprNode condition = new ExprNode();
        node.condition = parseExprWithoutBlockNode();
        BlockExprNode thenBranch = new BlockExprNode();
        node.thenBranch = parseBlockExprNode();
        // check for else or elseif
        if (i < tokens.size() && tokens.get(i).name.equals("else")) {
            i++;
            if (i < tokens.size() && tokens.get(i).name.equals("if")) {
                IfExprNode elseifBranch = new IfExprNode();
                node.elseifBranch = parseIfExprNode();
                node.elseBranch = null;
            } else {
                BlockExprNode elseBranch = new BlockExprNode();
                node.elseBranch = parseBlockExprNode();
                node.elseifBranch = null;
            }
        } else {
            node.elseBranch = null;
            node.elseifBranch = null;
        }
        return node;
    }

    public LoopExprNode parseLoopExprNode() {
        LoopExprNode node = new LoopExprNode();
        // expect while or loop
        if (i < tokens.size() && tokens.get(i).name.equals("while")) {
            i++;
            ExprNode condition = new ExprNode();
            node.condition = parseExprWithoutBlockNode();
        } else if (i < tokens.size() && tokens.get(i).name.equals("loop")) {
            i++;
            node.condition = null;
        } else {
            assert false : "Expected 'while' or 'loop' at start of loop expression";
        }
        BlockExprNode body = new BlockExprNode();
        node.body = parseBlockExprNode();
        return node;
    }

    public IdentifierNode parseIdentifierNode() {
        assert i < tokens.size() && isIdentifier(tokens.get(i)) : "Expected identifier";
        IdentifierNode node = new IdentifierNode();
        node.name = tokens.get(i).name;
        i++;
        return node;
    }

    // now we parse type expressions. 
    // there are four types of type expressions:
    // 1. typepath (for TypePathExprNode): a PathExprSegNode
    // 2. reference type (for TypeRefExprNode): & or & mut followed by a type expression
    // 3. array type (for TypeArrayExprNode): [type; size] 
    // 4. unit type (for TypeUnitExprNode): ()
    public TypeExprNode parseTypeExprNode() {
        TypeExprNode node = new TypeExprNode();
        if (i < tokens.size() && tokens.get(i).name.equals("&")) {
            TypeRefExprNode refNode = new TypeRefExprNode();
            node = parseTypeRefExprNode();
        } else if (i < tokens.size() && tokens.get(i).name.equals("[")) {
            TypeArrayExprNode arrayNode = new TypeArrayExprNode();
            node = parseTypeArrayExprNode();
        } else if (i < tokens.size() && tokens.get(i).name.equals("(")) {
            // it's a unit type
            TypeUnitExprNode unitNode = new TypeUnitExprNode();
            node = parseTypeUnitExprNode();
        } else {
            // it's a type path
            TypePathExprNode pathNode = new TypePathExprNode();
            node = parseTypePathExprNode();
        }
        return node;
    }
    public TypePathExprNode parseTypePathExprNode() {
        TypePathExprNode node = new TypePathExprNode();
        PathExprSegNode seg = new PathExprSegNode();
        node.path = parsePathExprSegNode();
        return node;
    }
    public TypeRefExprNode parseTypeRefExprNode() {
        TypeRefExprNode node = new TypeRefExprNode();
        // expect &
        if (i < tokens.size() && tokens.get(i).name.equals("&")) {
            i++;
        } else {
            assert false : "Expected '&' at start of reference type";
        }
        if (i < tokens.size() && tokens.get(i).name.equals("mut")) {
            node.isMutable = true;
            i++;
        } else {
            node.isMutable = false;
        }
        TypeExprNode innerType = new TypeExprNode();
        node.innerType = parseTypeExprNode();
        return node;
    }
    public TypeArrayExprNode parseTypeArrayExprNode() {
        TypeArrayExprNode node = new TypeArrayExprNode();
        // expect [
        if (i < tokens.size() && tokens.get(i).name.equals("[")) {
            i++;
        } else {
            assert false : "Expected '[' at start of array type";
        }
        TypeExprNode elementType = new TypeExprNode();
        node.elementType = parseTypeExprNode();
        // expect ;
        if (i < tokens.size() && tokens.get(i).name.equals(";")) {
            i++;
        } else {
            assert false : "Expected ';' in array type";
        }
        ExprNode size = new ExprNode();
        node.size = parseExprWithoutBlockNode(0);
        // expect ]
        if (i < tokens.size() && tokens.get(i).name.equals("]")) {
            i++;
        } else {
            assert false : "Expected ']' at end of array type";
        }
        return node;
    }
    public TypeUnitExprNode parseTypeUnitExprNode() {
        TypeUnitExprNode node = new TypeUnitExprNode();
        // expect (
        if (i < tokens.size() && tokens.get(i).name.equals("(")) {
            i++;
        } else {
            assert false : "Expected '(' at start of unit type";
        }
        // expect )
        if (i < tokens.size() && tokens.get(i).name.equals(")")) {
            i++;
        } else {
            assert false : "Expected ')' at end of unit type";
        }
        return node;
    }

    public StmtNode parseStmtNode() {
        assert i < tokens.size() : "No more tokens to parse in statement";
        token_t token = tokens.get(i);
        
        if (token.name.equals("let")) {
            return parseLetStmtNode();
        } else if (token.name.equals("fn")) {
            return parseFunctionNode();
        } else if (token.name.equals("struct")) {
            return parseStructNode();
        } else if (token.name.equals("enum")) {
            return parseEnumNode();
        } else if (token.name.equals("const")) {
            return parseConstItemNode();
        } else if (token.name.equals("trait")) {
            return parseTraitNode();
        } else if (token.name.equals("impl")) {
            return parseImplNode();
        } else {
            // Default to expression statement
            return parseExprStmtNode();
        }
    }

    // the entry point of the parser
    Vector<StmtNode> statements;
    public void parse() {
        // the program is comprised of a series of statements
        statements = new Vector<>();
        while(i < tokens.size()) {
            StmtNode stmt = parseStmtNode();
            System.out.println(stmt instanceof FunctionNode);
            statements.add(stmt);
        }
    }
}