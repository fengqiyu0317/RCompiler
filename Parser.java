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


    public void parse(LetStmtNode node) {
        // consume let
        i++;
        PatternNode pattern = new PatternNode();
        parse(pattern);
        node.name = pattern;
        // expect colon
        if (i < tokens.size() && tokens.get(i).name == ":") {
            i++;
            TypeExprNode type = new TypeExprNode();
            parse(type);
            node.type = type;
        } else {
            assert false : "Expected ':' after pattern in let statement";
        }
        // expect equal
        if (i < tokens.size() && tokens.get(i).name == "=") {
            i++;
            ExprNode expr = new ExprNode();
            parse(expr);
            node.value = expr;
        } else {
            node.value = null;
        }
        // expect semicolon
        if (i < tokens.size() && tokens.get(i).name == ";") {
            i++;
        } else {
            assert false : "Expected ';' at end of let statement";
        }
    }

    public void parse(FunctionNode node) {
        // consume fn
        i++;
        // expect identifier
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
            IdentifierNode name = new IdentifierNode();
            parse(name);
            node.name = name;
        } else {
            assert false : "Expected function name after 'fn'";
        }
        // expect (
        if (i < tokens.size() && tokens.get(i).name == "(") {
            i++;
        } else {
            assert false : "Expected '(' after function name";
        }
        // parse parameters
        Vector<ParameterNode> parameters = new Vector<>();
        // try to check if there is a self parameter
        parse(node.selfPara);
        while (i < tokens.size() && tokens.get(i).name != ")") {
            ParameterNode param = new ParameterNode();
            parse(param);
            parameters.add(param);
            if (i < tokens.size() && tokens.get(i).name == ",") {
                i++;
            } else if (i >= tokens.size() || tokens.get(i).name != ")") {
                assert false : "Expected ',' or ')' in parameter list";
            }
        }
        node.parameters = parameters;
        // expect )
        if (i < tokens.size() && tokens.get(i).name == ")") {
            i++;
        } else {
            assert false : "Expected ')' at end of parameter list"; 
        }
        // check for return type
        if (i < tokens.size() && tokens.get(i).name == "->") {
            i++;
            TypeExprNode returnType = new TypeExprNode();
            parse(returnType);
            node.returnType = returnType;
        } else {
            node.returnType = null;
        }
        // expect {
        if (i < tokens.size() && tokens.get(i).name == "{") {
            i++;
            BlockExprNode body = new BlockExprNode();
            parse(body);
            node.body = body;
        } else {
            // if the next token is ";", then it's function declaration without body
            if (i < tokens.size() && tokens.get(i).name == ";") {
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
        PatternNode name = new PatternNode();
        parse(name);
        node.name = name;
        if (i < tokens.size() && tokens.get(i).name == ":") {
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
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
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
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
            IdentifierNode name = new IdentifierNode();
            parse(name);
            node.name = name;
        } else {
            assert false : "Expected struct name after 'struct'";
        }
        // expect {
        if (i < tokens.size() && tokens.get(i).name == "{") {
            i++;
        } else {
            // if the next token is ";", then it's struct declaration without body
            if (i < tokens.size() && tokens.get(i).name == ";") {
                i++;
                node.fields = null;
                return;
            } else {
                assert false : "Expected '{' or ';' after struct name";
            }
        }
        // parse fields
        Vector<FieldNode> fields = new Vector<>();
        while (i < tokens.size() && tokens.get(i).name != "}") {
            FieldNode field = new FieldNode();
            parse(field);
            fields.add(field);
            if (i < tokens.size() && tokens.get(i).name == ",") {
                i++;
            } else if (i >= tokens.size() || tokens.get(i).name != "}") {
                assert false : "Expected ',' or '}' in field list";
            }
        }
        node.fields = fields;
        // expect }
        if (i < tokens.size() && tokens.get(i).name == "}") {
            i++;
        } else {
            assert false : "Expected '}' at end of field list";
        }
    }

    public void parse(FieldNode node) {
        // expect identifier
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
            IdentifierNode name = new IdentifierNode();
            parse(name);
            node.name = name;
        } else {
            assert false : "Expected field name in struct";
        }
        // expect :
        if (i < tokens.size() && tokens.get(i).name == ":") {
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
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
            IdentifierNode name = new IdentifierNode();
            parse(name);
            node.name = name;
        } else {
            assert false : "Expected enum name after 'enum'";
        }
        // expect {
        if (i < tokens.size() && tokens.get(i).name == "{") {
            i++;
        } else {
            assert false : "Expected '{' after enum name";
        }
        // parse variants
        Vector<IdentifierNode> variants = new Vector<>();
        while (i < tokens.size() && tokens.get(i).name != "}") {
            if (i < tokens.size() && isIdentifier(tokens.get(i))) {
                IdentifierNode variant = new IdentifierNode();
                parse(variant);
                variants.add(variant);
            } else {
                assert false : "Expected variant name in enum";
            }
            if (i < tokens.size() && tokens.get(i).name == ",") {
                i++;
            } else if (i >= tokens.size() || tokens.get(i).name != "}") {
                assert false : "Expected ',' or '}' in variant list";
            }
        }
        node.variants = variants;
        // expect }
        if (i < tokens.size() && tokens.get(i).name == "}") {
            i++;
        } else {
            assert false : "Expected '}' at end of variant list";
        }
    }

    public void parse(ConstItemNode node) {
        // consume the "const" token
        i++;
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
            IdentifierNode name = new IdentifierNode();
            parse(name);
            node.name = name;
        } else {
            assert false : "Expected const name after 'const'";
        }
        if (i < tokens.size() && tokens.get(i).name == ":") {
            i++;
            TypeExprNode type = new TypeExprNode();
            parse(type);
            node.type = type;
        } else {
            assert false : "Expected ':' after const name";
        }
        if (i < tokens.size() && tokens.get(i).name == "=") {
            i++;
            ExprNode value = new ExprNode();
            parse(value);
            node.value = value;
        }
        if (i < tokens.size() && tokens.get(i).name == ";") {
            i++;
        } else {
            assert false : "Expected ';' at end of const item";
        }
    }

    public void parse(TraitNode node) {
        // consume the "trait" token
        i++;
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
            IdentifierNode name = new IdentifierNode();
            parse(name);
            node.name = name;
        } else {
            assert false : "Expected trait name after 'trait'";
        }
        if (i < tokens.size() && tokens.get(i).name == "{") {
            i++;
        } else {
            assert false : "Expected '{' after trait name";
        }
        Vector<AssoItemNode> items = new Vector<>();
        while (i < tokens.size() && tokens.get(i).name != "}") {
            AssoItemNode item = new AssoItemNode();
            parse(item);
            items.add(item);
        }
        node.items = items;
        if (i < tokens.size() && tokens.get(i).name == "}") {
            i++;
        } else {
            assert false : "Expected '}' at end of trait body";
        }
    }

    public void parse(ImplNode node) {
        // consume the "impl" token
        i++;
        // check if it's an inherent impl or a trait impl
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
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
        if (i < tokens.size() && tokens.get(i).name == "{") {
            i++;
        } else {
            assert false : "Expected '{' after impl type";
        }
        Vector<AssoItemNode> items = new Vector<>();
        while (i < tokens.size() && tokens.get(i).name != "}") {
            AssoItemNode item = new AssoItemNode();
            parse(item);
            items.add(item);
        }
        node.items = items;
        if (i < tokens.size() && tokens.get(i).name == "}") {
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
            node.function = funcNode;
        } else if (i < tokens.size() && tokens.get(i).name.equals("const")) {
            // It may be a const item or a const function
            if (i + 1 < tokens.size() && tokens.get(i + 1).name.equals("fn")) {
                FunctionNode funcNode = new FunctionNode();
                funcNode.isConst = true;
                // consume const
                i++;
                parse(funcNode);
                node.function = funcNode;
            } else {
                ConstItemNode constNode = new ConstItemNode();
                parse(constNode);
                node.constant = constNode;
            }
        } else {
            assert false : "Expected 'fn' or 'const' in associated item";
        }
    }

    public void parse(ExprStmtNode node) {
        assert i < tokens.size() : "No more tokens to parse in expression statement";
        token_t token = tokens.get(i);
        // check if it's an expression with block
        if (token.name.equals("if") || token.name.equals("while") || token.name.equals("loop") || token.name == "{") {
            ExprWithBlockNode blockNode = new ExprWithBlockNode();
            parse(blockNode);
            node.expr = blockNode;
            return ;
        }
        // otherwise it's an expression without block
        ExprWithoutBlockNode exprNode = new ExprWithoutBlockNode();
        parse(exprNode, 0);
        node.expr = exprNode;
        // expect semicolon
        if (i < tokens.size() && tokens.get(i).name == ";") {
            i++;
        } else {
            assert false : "Expected ';' at end of expression statement";
        }
    }

    public void parse(ExprNode node, int precedence) {
        // we need to check if it's an expression with block or without block
        assert i < tokens.size() : "No more tokens to parse in expression";
        token_t token = tokens.get(i);
        if (token.name.equals("if") || token.name.equals("while") || token.name.equals("loop") || token.name == "{") {
            ExprWithBlockNode blockNode = new ExprWithBlockNode();
            parse(blockNode);
            node = blockNode;
        } else {
            ExprWithoutBlockNode exprNode = new ExprWithoutBlockNode();
            parse(exprNode, precedence);
            node = exprNode;
        }
    }

    public void parse(ExprNode node) {
        parse(node, 0);
    }

    // we write a function: for tokens (arg1, arg2, ..., argn), the function return a vector of ExprNode
    public Vector<ExprNode> parseFunctionArgs() {
        Vector<ExprNode> args = new Vector<>();
        // expect (
        if (i < tokens.size() && tokens.get(i).name == "(") {
            i++;
        } else {
            assert false : "Expected '(' at start of function argument list";
        }
        while (i < tokens.size() && tokens.get(i).name != ")") {
            ExprNode arg = new ExprNode();
            parse(arg);
            args.add(arg);
            if (i < tokens.size() && tokens.get(i).name == ",") {
                i++;
            } else if (i >= tokens.size() || tokens.get(i).name != ")") {
                assert false : "Expected ',' or ')' in function argument list";
            }
        }
        // expect )
        if (i < tokens.size() && tokens.get(i).name == ")") {
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

    public void parse(GroupExprNode node) {
        // expect (
        if (i < tokens.size() && tokens.get(i).name == "(") {
            i++;
        } else {
            assert false : "Expected '(' at start of grouped expression";
        }
        ExprNode innerExpr = new ExprNode();
        parse(innerExpr, 0);
        node.innerExpr = innerExpr;
        // expect )
        if (i < tokens.size() && tokens.get(i).name == ")") {
            i++;
        } else {
            assert false : "Expected ')' at end of grouped expression";
        }
    }

    public void parse(ExprWithoutBlockNode node, int precedence) {
        assert i < tokens.size() : "No more tokens to parse in expression without block";
        token_t token = tokens.get(i);

        if (token.tokentype == token_t.TokenType_t.INTEGER_LITERAL || token.tokentype == token_t.TokenType_t.CHAR_LITERAL || token.tokentype == token_t.TokenType_t.STRING_LITERAL || token.tokentype == token_t.TokenType_t.RAW_STRING_LITERAL || token.tokentype == token_t.TokenType_t.C_STRING_LITERAL || token.tokentype == token_t.TokenType_t.RAW_C_STRING_LITERAL || token.name.equals("true") || token.name.equals("false")) {
            LiteralExprNode litNode = new LiteralExprNode();
            parse(litNode);
            node = litNode;
        } else if (isIdentifier(token) || token.name.equals("self") || token.name.equals("Self")) {
            PathExprNode path = new PathExprNode();
            parse(path.LSeg);
            node = path;
        } else if (token.name.equals("[")) {
            ArrayExprNode arrayNode = new ArrayExprNode();
            parse(arrayNode);
            node = arrayNode;
        } else if (token.name.equals("(")) {
            GroupExprNode groupNode = new GroupExprNode();
            parse(groupNode);
            node = groupNode;
        } else if (token.name.equals("&") || token.name.equals("&&")) {
            BorrowExprNode borrowNode = new BorrowExprNode();
            parse(borrowNode);
            node = borrowNode;
        } else if (token.name.equals("*")) {
            DerefExprNode derefNode = new DerefExprNode();
            parse(derefNode);
            node = derefNode;
        } else if (token.name.equals("-") || token.name.equals("~")) {
            NegaExprNode negaNode = new NegaExprNode();
            parse(negaNode);
            node = negaNode;
        } else if (token.name.equals("continue")) {
            ContinueExprNode contNode = new ContinueExprNode();
            parse(contNode);
            node = contNode;
        } else if (token.name.equals("break")) {
            BreakExprNode breakNode = new BreakExprNode();
            parse(breakNode);
            node = breakNode;
        } else if (token.name.equals("return")) {
            ReturnExprNode retNode = new ReturnExprNode();
            parse(retNode);
            node = retNode;
        } else if (token.name.equals("_")) {
            UnderscoreExprNode underNode = new UnderscoreExprNode();
            parse(underNode);
            node = underNode;
        } 
        while (i < tokens.size()) {
            token = tokens.get(i);
            if (token.name.equals(".")) {
                // for a field access, the next token must be an identifier; but for a method call, the next token is a PathExprSegNode.
                // the next expression may be a field access or a method call; the difference between them is whether there is a '(' after the next token
                // for the field access, we use the FieldExprNode; for the method call, we use the MethodCallExprNode
                i++;
                PathExprSegNode pathSeg = new PathExprSegNode();
                parse(pathSeg);
                if (i < tokens.size() && tokens.get(i).name == "(") {
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
                    parse(right, opPrecedence);
                    comAssignNode.left = node;
                    comAssignNode.right = right;
                    node = comAssignNode;
                } else if (isAssignOper(token)) {
                    AssignExprNode assignNode = new AssignExprNode();
                    assignNode.operator = operator;
                    i++;
                    ExprNode right = new ExprNode();
                    parse(right, opPrecedence);
                    assignNode.left = node;
                    assignNode.right = right;
                    node = assignNode;
                } else if (isComp(token)) {
                    CompExprNode compNode = new CompExprNode();
                    compNode.operator = operator;
                    i++;
                    ExprNode right = new ExprNode();
                    parse(right, opPrecedence);
                    compNode.left = node;
                    compNode.right = right;
                    node = compNode;
                } else if (isArith(token)) {
                    ArithExprNode arithNode = new ArithExprNode();
                    arithNode.operator = operator;
                    i++;
                    ExprNode right = new ExprNode();
                    parse(right, opPrecedence);
                    arithNode.left = node;
                    arithNode.right = right;
                    node = arithNode;
                } else if (isLazy(token)) {
                    LazyExprNode lazyNode = new LazyExprNode();
                    lazyNode.operator = operator;
                    i++;
                    ExprNode right = new ExprNode();
                    parse(right, opPrecedence);
                    lazyNode.left = node;
                    lazyNode.right = right;
                    node = lazyNode;
                }
            } else if (token.name.equals("as")) {
                if (precedence >= 120) {
                    break;
                }
                TypeCastExprNode typeCastNode = new TypeCastExprNode();
                i++;
                TypeExprNode targetType = new TypeExprNode();
                parse(targetType);
                typeCastNode.expr = node;
                typeCastNode.type = targetType;
                node = typeCastNode;
            } else if (token.name.equals("::")) {
                if (precedence >= 170) {
                    break;
                }
                i++;
                PathExprSegNode pathSeg = new PathExprSegNode();
                parse(pathSeg);
                if (node instanceof PathExprNode) {
                    assert ((PathExprNode)node).RSeg == null : "Unexpected state: PathExprNode already has RSeg in path expression";
                    ((PathExprNode)node).RSeg = pathSeg;
                    continue;
                }
                assert false : "Expected path segment before '::' in path expression";
            } else if (token.name.equals("[")) {
                // consume [
                i++;
                IndexExprNode indexNode = new IndexExprNode();
                indexNode.array = node;
                parse(indexNode.index, precedence);
                node = indexNode;
                // expect ]
                if (i < tokens.size() && tokens.get(i).name == "]") {
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
                    if (tokens.get(i).name == "}") {
                        break;
                    }
                    FieldValNode fieldVal = new FieldValNode();
                    parse(fieldVal);
                    fieldValues.add(fieldVal);
                    if (i < tokens.size() && tokens.get(i).name == ",") {
                        i++;
                    } else if (i < tokens.size() && tokens.get(i).name == "}") {
                        break;
                    } else {
                        assert false : "Expected ',' or '}' in field assignment list";
                    }
                }
                node = structNode;
                structNode.fieldValues = fieldValues;
                // expect }
                if (i < tokens.size() && tokens.get(i).name == "}") {
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
        node = node;
    }

    public void parse(ExprWithoutBlockNode node) {
        parse(node, 0);
    }


    public void parse(BorrowExprNode node) {
        if (tokens.get(i).name.equals("&")) {
            node.isDouble = false;
        } else if (tokens.get(i).name.equals("&&")) {
            node.isDouble = true;
        } else {
            assert false : "Expected '&' or '&&' in borrow expression";
        }
        i++;
        if (tokens.get(i).name.equals("mut")) {
            node.isMutable = true;
            i++;
        } else {
            node.isMutable = false;
        }
        ExprNode innerExpr = new ExprNode();
        parse(innerExpr, 130);
        node.innerExpr = innerExpr;
    }

    public void parse(DerefExprNode node) {
        // consume *
        i++;
        ExprNode innerExpr = new ExprNode();
        parse(innerExpr, 130);
        node.innerExpr = innerExpr;
    }

    public void parse(NegaExprNode node) {
        // consume - or ~
        if (tokens.get(i).name.equals("-")) {
            node.isLogical = false;
        } else if (tokens.get(i).name.equals("~")) {
            node.isLogical = true;
        } else {
            assert false : "Expected '-' or '~' in negation expression";
        }
        i++;
        ExprNode innerExpr = new ExprNode();
        parse(innerExpr, 130);
        node.innerExpr = innerExpr;
    }



    public void parse(LiteralExprNode node) {
        if (i < tokens.size()) {
            token_t token = tokens.get(i);
            if (token.tokentype == token_t.TokenType_t.INTEGER_LITERAL) {
                node.literalType = literal_t.INT;
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
        if (i < tokens.size() && tokens.get(i).name == "::") {
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
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
            IdentifierNode name = new IdentifierNode();
            parse(name);
            node.name = name;
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
    }

    public void parse(ArrayExprNode node) {
        // expect [
        if (i < tokens.size() && tokens.get(i).name == "[") {
            i++;
        } else {
            assert false : "Expected '[' at start of array expression";
        }
        Vector<ExprNode> elements = new Vector<>();
        boolean isList = true, isFirst = true;
        while (i < tokens.size() && tokens.get(i).name != "]") {
            ExprNode element = new ExprNode();
            parse(element);
            elements.add(element);
            if (i < tokens.size() && tokens.get(i).name == ",") {
                i++;
                isFirst = false;
            } else if (i < tokens.size() && tokens.get(i).name == ";") {
                if (!isFirst) {
                    assert false : "Unexpected ';' in array expression";
                }
                isList = false;
                i++;
                ExprNode size = new ExprNode();
                parse(size);
                node.size = size;
                node.repeatedElement = element;
                break;
            } else if (i >= tokens.size() || tokens.get(i).name != "]") {
                assert false : "Expected ',' or ']' in array expression";
            }
        }
        if (isList) {
            node.elements = elements;
            node.repeatedElement = null;
            node.size = null;
        } else {
            node.elements = null;
        }
    }

    public void parse(IndexExprNode node) {
        ExprNode array = new ExprNode();
        parse(array);
        node.array = array;
        // expect [
        if (i < tokens.size() && tokens.get(i).name == "[") {
            i++;
        } else {
            assert false : "Expected '[' at start of index expression";
        }
        ExprNode index = new ExprNode();
        parse(index);
        node.index = index;
        // expect ]
        if (i < tokens.size() && tokens.get(i).name == "]") {
            i++;
        } else {
            assert false : "Expected ']' at end of index expression";
        }
    }

    public void parse(StructExprNode node) {
        PathExprSegNode structName = new PathExprSegNode();
        parse(structName);
        node.structName = structName;
        // expect {
        if (i < tokens.size() && tokens.get(i).name == "{") {
            i++;
        } else {
            assert false : "Expected '{' at start of struct expression";
        }
        Vector<FieldValNode> fieldValues = new Vector<>();
        while (i < tokens.size() && tokens.get(i).name != "}") {
            FieldValNode fieldVal = new FieldValNode();
            parse(fieldVal);
            fieldValues.add(fieldVal);
            if (i < tokens.size() && tokens.get(i).name == ",") {
                i++;
            } else if (i >= tokens.size() || tokens.get(i).name != "}") {
                assert false : "Expected ',' or '}' in field assignment list";
            }
        }
        node.fieldValues = fieldValues;
        // expect }
        if (i < tokens.size() && tokens.get(i).name == "}") {
            i++;
        } else {
            assert false : "Expected '}' at end of struct expression";
        }
    }

    public void parse(FieldValNode node) {
        // expect identifier
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
            IdentifierNode name = new IdentifierNode();
            parse(name);
            node.fieldName = name;
        } else {
            assert false : "Expected field name in struct expression";
        }
        // expect :
        if (i < tokens.size() && tokens.get(i).name == ":") {
            i++;
        } else {
            assert false : "Expected ':' after field name in struct expression";
        }
        ExprNode value = new ExprNode();
        parse(value);
        node.value = value;
    }

    public void parse(CallExprNode node) {
        ExprNode function = new ExprNode();
        parse(function);
        node.function = function;
        // expect (
        if (i < tokens.size() && tokens.get(i).name == "(") {
            i++;
        } else {
            assert false : "Expected '(' at start of call expression";
        }
        Vector<ExprNode> arguments = new Vector<>();
        while (i < tokens.size() && tokens.get(i).name != ")") {
            ExprNode argument = new ExprNode();
            parse(argument);
            arguments.add(argument);
            if (i < tokens.size() && tokens.get(i).name == ",") {
                i++;
            } else if (i >= tokens.size() || tokens.get(i).name != ")") {
                assert false : "Expected ',' or ')' in argument list";
            }
        }
        node.arguments = arguments;
        // expect )
        if (i < tokens.size() && tokens.get(i).name == ")") {
            i++;
        } else {
            assert false : "Expected ')' at end of argument list";
        }
    }

    public void parse(MethodCallExprNode node) {
        ExprNode receiver = new ExprNode();
        parse(receiver);
        node.receiver = receiver;
        // expect .
        if (i < tokens.size() && tokens.get(i).name == ".") {
            i++;
        } else {
            assert false : "Expected '.' before method name in method call";
        }
        PathExprSegNode methodName = new PathExprSegNode();
        parse(methodName);
        node.methodName = methodName;
        // expect (
        if (i < tokens.size() && tokens.get(i).name == "(") {
            i++;
        } else {
            assert false : "Expected '(' at start of method call";
        }
        Vector<ExprNode> arguments = new Vector<>();
        while (i < tokens.size() && tokens.get(i).name != ")") {
            ExprNode argument = new ExprNode();
            parse(argument);
            arguments.add(argument);
            if (i < tokens.size() && tokens.get(i).name == ",") {
                i++;
            } else if (i >= tokens.size() || tokens.get(i).name != ")") {
                assert false : "Expected ',' or ')' in method call argument list";
            }
        }
        node.arguments = arguments;
        // expect )
        if (i < tokens.size() && tokens.get(i).name == ")") {
            i++;
        } else {
            assert false : "Expected ')' at end of method call argument list";
        }
    }

    public void parse(FieldExprNode node) {
        ExprNode receiver = new ExprNode();
        parse(receiver);
        node.receiver = receiver;
        // expect .
        if (i < tokens.size() && tokens.get(i).name == ".") {
            i++;
        } else {
            assert false : "Expected '.' before field name in field expression";
        }
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
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
        if (i < tokens.size() && tokens.get(i).name != ";") {
            ExprNode value = new ExprNode();
            parse(value, 10);
            node.value = value;
        } else {
            node.value = null;
        }
    }

    public void parse(ReturnExprNode node) {
        // consume "return"
        i++;
        if (i < tokens.size() && tokens.get(i).name != ";") {
            ExprNode value = new ExprNode();
            parse(value, 10);
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
        } else if (i < tokens.size() && tokens.get(i).name == "{") {
            BlockExprNode blockNode = new BlockExprNode();
            parse(blockNode);
            node = blockNode;
        } else {
            assert false : "Expected 'if', 'while', 'loop' or '{' in block expression";
        }
    }

    public void parse(BlockExprNode node) {
        // expect {
        if (i < tokens.size() && tokens.get(i).name == "{") {
            i++;
        } else {
            assert false : "Expected '{' at start of block expression";
        }
        Vector<StmtNode> statements = new Vector<>();
        while (i < tokens.size() && tokens.get(i).name != "}") {
            StmtNode stmt = new StmtNode();
            parse(stmt);
            statements.add(stmt);
        }
        node.statements = statements;
        // expect }
        if (i < tokens.size() && tokens.get(i).name == "}") {
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
        ExprNode condition = new ExprNode();
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
            ExprNode condition = new ExprNode();
            parse(condition);
            node.condition = condition;
        } else if (i < tokens.size() && tokens.get(i).name.equals("loop")) {
            i++;
            node.condition = null;
        } else {
            assert false : "Expected 'while' or 'loop' at start of loop expression";
        }
        BlockExprNode body = new BlockExprNode();
        parse(body);
        node.body = body;
    }

    public void parse(IdentifierNode node) {
        assert i < tokens.size() && isIdentifier(tokens.get(i)) : "Expected identifier";
        node.name = tokens.get(i).name;
        i++;
    }

    // now we parse type expressions. 
    // there are four types of type expressions:
    // 1. typepath (for TypePathExprNode): a PathExprSegNode
    // 2. reference type (for TypeRefExprNode): & or & mut followed by a type expression
    // 3. array type (for TypeArrayExprNode): [type; size] 
    // 4. unit type (for TypeUnitExprNode): ()
    public void parse(TypeExprNode node) {
        if (i < tokens.size() && tokens.get(i).name.equals("&")) {
            TypeRefExprNode refNode = new TypeRefExprNode();
            parse(refNode);
            node = refNode;
        } else if (i < tokens.size() && tokens.get(i).name.equals("[")) {
            TypeArrayExprNode arrayNode = new TypeArrayExprNode();
            parse(arrayNode);
            node = arrayNode;
        } else if (i < tokens.size() && tokens.get(i).name.equals("(")) {
            // it's a unit type
            TypeUnitExprNode unitNode = new TypeUnitExprNode();
            parse(unitNode);
            node = unitNode;
        } else {
            // it's a type path
            TypePathExprNode pathNode = new TypePathExprNode();
            parse(pathNode);
            node = pathNode;
        }
    }
    public void parse(TypePathExprNode node) {
        PathExprSegNode seg = new PathExprSegNode();
        parse(seg);
        node.path = seg;
    }
    public void parse(TypeRefExprNode node) {
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
        parse(innerType);
        node.innerType = innerType;
    }
    public void parse(TypeArrayExprNode node) {
        // expect [
        if (i < tokens.size() && tokens.get(i).name.equals("[")) {
            i++;
        } else {
            assert false : "Expected '[' at start of array type";
        }
        TypeExprNode elementType = new TypeExprNode();
        parse(elementType);
        node.elementType = elementType;
        // expect ;
        if (i < tokens.size() && tokens.get(i).name.equals(";")) {
            i++;
        } else {
            assert false : "Expected ';' in array type";
        }
        ExprNode size = new ExprNode();
        parse(size, 0);
        node.size = size;
        // expect ]
        if (i < tokens.size() && tokens.get(i).name.equals("]")) {
            i++;
        } else {
            assert false : "Expected ']' at end of array type";
        }
    }
    public void parse(TypeUnitExprNode node) {
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
                funcNode.isConst = false;
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
                // it may be a const item or a const function
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
            } else if(token.name.equals("trait")) {
                TraitNode traitNode = new TraitNode();
                parse(traitNode);
                node = traitNode;
            } else if(token.name.equals("impl")) {
                ImplNode implNode = new ImplNode();
                parse(implNode);
                node = implNode;
            } else {
                ExprStmtNode exprNode = new ExprStmtNode();
                parse(exprNode);
                node = exprNode;
            }
        } else {
            // if it's ";", it's an empty statement
            if (token.name == ";") {
                // consume ";"
                i++;
            } else {
                ExprStmtNode exprNode = new ExprStmtNode();
                parse(exprNode);
                node = exprNode;
                // consume ";"
                if (i < tokens.size() && tokens.get(i).name == ";") {
                    i++;
                } else {
                    assert false : "Expected ';' at end of expression statement";
                }
            }
        }
    }

    // the entry point of the parser
    public void parse() {
        // the program is comprised of a series of statements
        Vector<StmtNode> statements = new Vector<>();
        while(i < tokens.size()) {
            StmtNode stmt = new StmtNode();
            parse(stmt);
            statements.add(stmt);
        }
    }
}