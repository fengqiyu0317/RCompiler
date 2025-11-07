import java.util.Vector;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

public class Parser {
    private Vector<token_t> tokens;

    public Parser(Vector<token_t> tokens) {
        this.tokens = tokens;
    }

    int i = 0;
    private static final int MAX_RECURSION_DEPTH = 100;
    private int recursionDepth = 0;

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
        LetStmtNode node = new LetStmtNode();
        node.name = parsePatternNode();
        if (i < tokens.size() && tokens.get(i).name.equals(":")) {
            i++;
            node.type = parseTypeExprNode();
        } else {
            throw new ParseException("Expected ':' after pattern in let statement");
        }
        if (i < tokens.size() && tokens.get(i).name.equals("=")) {
            i++;
            node.value = parseExprNode();
        } else {
            node.value = null;
        }
        if (i < tokens.size() && tokens.get(i).name.equals(";")) {
            i++;
        } else {
            throw new ParseException("Expected ';' at end of let statement");
        }
        return node;
    }

    public FunctionNode parseFunctionNode() {
        return parseFunctionNode(false);
    }
    
    public FunctionNode parseFunctionNode(boolean isConst) {
        FunctionNode node = new FunctionNode();
        node.isConst = isConst;
        i++;
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
            node.name = parseIdentifierNode();
        } else {
            throw new ParseException("Expected function name after 'fn'");
        }
        if (i < tokens.size() && tokens.get(i).name.equals("(")) {
            i++;
        } else {
            throw new ParseException("Expected '(' after function name");
        }
        node.selfPara = parseSelfParaNode();
        if (node.selfPara != null) {
            if (i < tokens.size() && tokens.get(i).name.equals(",")) {
                i++;
            } else if (i >= tokens.size() || tokens.get(i).name.equals(")")) {
                // do nothing
            } else {
                throw new ParseException("Expected ',' or ')' after self parameter");
            }
        }
        Vector<ParameterNode> parameters = new Vector<>();
        while (i < tokens.size() && !tokens.get(i).name.equals(")")) {
            parameters.add(parseParameterNode());
            if (i < tokens.size() && tokens.get(i).name.equals(",")) {
                i++;
            } else if (i >= tokens.size() || !tokens.get(i).name.equals(")")) {
                throw new ParseException("Expected ',' or ')' in parameter list");
            }
        }
        node.parameters = parameters;
        if (i < tokens.size() && tokens.get(i).name.equals(")")) {
            i++;
        } else {
            throw new ParseException("Expected ')' at end of parameter list");
        }
        if (i < tokens.size() && tokens.get(i).name.equals("->")) {
            i++;
            node.returnType = parseTypeExprNode();
        } else {
            node.returnType = null;
        }
        if (i < tokens.size() && tokens.get(i).name.equals("{")) {
            node.body = parseBlockExprNode();
        } else {
            if (i < tokens.size() && tokens.get(i).name.equals(";")) {
                i++;
                node.body = null;
            } else {
                throw new ParseException("Expected '{' or ';' after function signature");
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
            throw new ParseException("Expected ':' after parameter pattern");
        }
        return node;
    }

    public PatternNode parsePatternNode() {
        if (i >= tokens.size()) {
            throw new ParseException("No more tokens to parse in pattern");
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
            throw new ParseException("Expected identifier in pattern");
        }
    }

    public StructNode parseStructNode() {
        i++;
        StructNode node = new StructNode();
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
            node.name = parseIdentifierNode();
        } else {
            throw new ParseException("Expected struct name after 'struct'");
        }
        if (i < tokens.size() && tokens.get(i).name.equals("{")) {
            i++;
            Vector<FieldNode> fields = new Vector<>();
            while (i < tokens.size() && !tokens.get(i).name.equals("}")) {
                fields.add(parseFieldNode());
                if (i < tokens.size() && tokens.get(i).name.equals(",")) {
                    i++;
                } else if (i >= tokens.size() || !tokens.get(i).name.equals("}")) {
                    throw new ParseException("Expected ',' or '}' in field list");
                }
            }
            node.fields = fields;
            if (i < tokens.size() && tokens.get(i).name.equals("}")) {
                i++;
            } else {
                throw new ParseException("Expected '}' at end of field list");
            }
        } else if (i < tokens.size() && tokens.get(i).name.equals(";")) {
            i++;
            node.fields = null;
        } else {
            throw new ParseException("Expected '{' or ';' after struct name");
        }
        return node;
    }

    public FieldNode parseFieldNode() {
        FieldNode node = new FieldNode();
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
            node.name = parseIdentifierNode();
        } else {
            throw new ParseException("Expected field name in struct");
        }
        if (i < tokens.size() && tokens.get(i).name.equals(":")) {
            i++;
        } else {
            throw new ParseException("Expected ':' after field name in struct");
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
            throw new ParseException("Expected enum name after 'enum'");
        }
        if (i < tokens.size() && tokens.get(i).name.equals("{")) {
            i++;
            Vector<IdentifierNode> variants = new Vector<>();
            while (i < tokens.size() && !tokens.get(i).name.equals("}")) {
                if (i < tokens.size() && isIdentifier(tokens.get(i))) {
                    variants.add(parseIdentifierNode());
                } else {
                    throw new ParseException("Expected variant name in enum");
                }
                if (i < tokens.size() && tokens.get(i).name.equals(",")) {
                    i++;
                } else if (i >= tokens.size() || !tokens.get(i).name.equals("}")) {
                    throw new ParseException("Expected ',' or '}' in variant list");
                }
            }
            node.variants = variants;
            if (i < tokens.size() && tokens.get(i).name.equals("}")) {
                i++;
            } else {
                throw new ParseException("Expected '}' at end of variant list");
            }
        } else {
            throw new ParseException("Expected '{' after enum name");
        }
        return node;
    }

    public ConstItemNode parseConstItemNode() {
        i++;
        ConstItemNode node = new ConstItemNode();
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
            node.name = parseIdentifierNode();
        } else {
            throw new ParseException("Expected const name after 'const'");
        }
        if (i < tokens.size() && tokens.get(i).name.equals(":")) {
            i++;
            node.type = parseTypeExprNode();
        } else {
            throw new ParseException("Expected ':' after const name");
        }
        if (i < tokens.size() && tokens.get(i).name.equals("=")) {
            i++;
            node.value = parseExprNode();
        } else {
            node.value = null;
        }
        if (i < tokens.size() && tokens.get(i).name.equals(";")) {
            i++;
        } else {
            throw new ParseException("Expected ';' at end of const item");
        }
        return node;
    }

    public TraitNode parseTraitNode() {
        i++;
        TraitNode node = new TraitNode();
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
            node.name = parseIdentifierNode();
        } else {
            throw new ParseException("Expected trait name after 'trait'");
        }
        if (i < tokens.size() && tokens.get(i).name.equals("{")) {
            i++;
        } else {
            throw new ParseException("Expected '{' after trait name");
        }
        Vector<AssoItemNode> items = new Vector<>();
        while (i < tokens.size() && !tokens.get(i).name.equals("}")) {
            AssoItemNode item = parseAssoItemNode();
            items.add(item);
        }
        node.items = items;
        if (i < tokens.size() && tokens.get(i).name.equals("}")) {
            i++;
        } else {
            throw new ParseException("Expected '}' at end of trait body");
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
                node.trait = parseIdentifierNode();
                // consume for
                i++;
                node.typeName = parseTypeExprNode();
            } else {
                // it's an inherent impl
                node.trait = null;
                node.typeName = parseTypeExprNode();
            }
        } else {
            // it's an inherent impl with a non-identifier type
            node.trait = null;
            node.typeName = parseTypeExprNode();
        }
        // expect {
        if (i < tokens.size() && tokens.get(i).name.equals("{")) {
            i++;
        } else {
            throw new ParseException("Expected '{' after impl type");
        }
        Vector<AssoItemNode> items = new Vector<>();
        while (i < tokens.size() && !tokens.get(i).name.equals("}")) {
            AssoItemNode item = parseAssoItemNode();
            // Skip null items (empty statements with just semicolons)
            if (item != null) {
                items.add(item);
            }
        }
        node.items = items;
        if (i < tokens.size() && tokens.get(i).name.equals("}")) {
            i++;
        } else {
            throw new ParseException("Expected '}' at end of impl body");
        }
        return node;
    }

    public AssoItemNode parseAssoItemNode() {
        // It can be a function or a const item
        // the function may be const
        AssoItemNode node = new AssoItemNode();
        if (i < tokens.size() && tokens.get(i).name.equals("fn")) {
            node.function = parseFunctionNode(false);
        } else if (i < tokens.size() && tokens.get(i).name.equals("const")) {
            // It may be a const item or a const function
            if (i + 1 < tokens.size() && tokens.get(i + 1).name.equals("fn")) {
                // consume const
                i++;
                node.function = parseFunctionNode(true);
            } else {
                ConstItemNode constNode = new ConstItemNode();
                node.constant = parseConstItemNode();
            }
        } else if (i < tokens.size() && tokens.get(i).name.equals(";")) {
            // Empty statement, just consume the semicolon and return null
            i++;
            return null;
        } else {
            throw new ParseException("Expected 'fn' or 'const' in associated item");
        }
        return node;
    }

    public ExprStmtNode parseExprStmtNode() {
        if (!(i < tokens.size())) throw new ParseException("No more tokens to parse in expression statement");
        token_t token = tokens.get(i);
        ExprStmtNode node = new ExprStmtNode();
        // check if it's an expression with block
        if (token.name.equals("if") || token.name.equals("while") || token.name.equals("loop") || token.name.equals("{")) {
            node.expr = parseExprWithBlockNode();
            // No semicolon required for expressions with blocks
            return node;
        }
        // otherwise it's an expression without block
        node.expr = parseExprWithoutBlockNode();
        // expect semicolon
        if (i < tokens.size() && tokens.get(i).name.equals(";")) {
            i++;
        } else {
            throw new ParseException("Expected ';' at end of expression statement");
        }
        return node;
    }

    public ExprNode parseExprNode(int precedence) {
        if (!(i < tokens.size())) throw new ParseException("No more tokens to parse in expression");
        token_t token = tokens.get(i);
        if (token.name.equals("if") || token.name.equals("while") || token.name.equals("loop") || token.name.equals("{")) {
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
            throw new ParseException("Expected '(' at start of function argument list");
        }
        while (i < tokens.size() && !tokens.get(i).name.equals(")")) {
            ExprNode arg = new ExprNode();
            args.add(parseExprNode());
            if (i < tokens.size() && tokens.get(i).name.equals(",")) {
                i++;
            } else if (i >= tokens.size() || !tokens.get(i).name.equals(")")) {
                throw new ParseException("Expected ',' or ')' in function argument list");
            }
        }
        // expect )
        if (i < tokens.size() && tokens.get(i).name.equals(")")) {
            i++;
        } else {
            throw new ParseException("Expected ')' at end of function argument list");
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
                throw new ParseException("Unknown operator: " + name);
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
            throw new ParseException("Expected '(' at start of grouped expression");
        }
        GroupExprNode node = new GroupExprNode();
        node.innerExpr = parseExprNode();
        if (i < tokens.size() && tokens.get(i).name.equals(")")) {
            i++;
        } else {
            throw new ParseException("Expected ')' at end of grouped expression");
        }
        return node;
    }

    public ExprWithoutBlockNode parseExprWithoutBlockNode(int precedence) {
        if (recursionDepth > MAX_RECURSION_DEPTH) {
            throw new RuntimeException("Maximum recursion depth exceeded in expression parsing");
        }
        recursionDepth++;
        if (!(i < tokens.size())) throw new ParseException("No more tokens to parse in expression without block");
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
        } else if (token.name.equals("-") || token.name.equals("!")) {
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
                        throw new ParseException("Expected identifier after '.' in field access");
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
                    comAssignNode.right = parseExprNode(opPrecedence);
                    node = comAssignNode;
                } else if (isAssignOper(token)) {
                    AssignExprNode assignNode = new AssignExprNode();
                    assignNode.operator = operator;
                    i++;
                    ExprNode right = new ExprNode();
                    assignNode.left = node;
                    assignNode.right = parseExprNode(opPrecedence);
                    node = assignNode;
                } else if (isComp(token)) {
                    CompExprNode compNode = new CompExprNode();
                    compNode.operator = operator;
                    i++;
                    ExprNode right = new ExprNode();
                    compNode.left = node;
                    compNode.right = parseExprNode(opPrecedence);
                    node = compNode;
                } else if (isArith(token)) {
                    ArithExprNode arithNode = new ArithExprNode();
                    arithNode.operator = operator;
                    i++;
                    ExprNode right = new ExprNode();
                    arithNode.left = node;
                    arithNode.right = parseExprNode(opPrecedence);
                    node = arithNode;
                } else if (isLazy(token)) {
                    LazyExprNode lazyNode = new LazyExprNode();
                    lazyNode.operator = operator;
                    i++;
                    ExprNode right = new ExprNode();
                    lazyNode.left = node;
                    lazyNode.right = parseExprNode(opPrecedence);
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
                    if (((PathExprNode)node).RSeg != null) throw new ParseException("Unexpected state: PathExprNode already has RSeg in path expression");
                    ((PathExprNode)node).RSeg = parsePathExprSegNode();
                    continue;
                }
                throw new ParseException("Expected path segment before '::' in path expression");
            } else if (token.name.equals("[")) {
                IndexExprNode indexNode = new IndexExprNode();
                indexNode.array = node;
                indexNode.index = parseExprNode(precedence);
                node = indexNode;
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
                    throw new ParseException("Expected path expression before '{' in struct expression");
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
                        throw new ParseException("Expected ',' or '}' in field assignment list");
                    }
                }
                node = structNode;
                structNode.fieldValues = fieldValues;
                // expect }
                if (i < tokens.size() && tokens.get(i).name.equals("}")) {
                    i++;
                } else {
                    throw new ParseException("Expected '}' at end of struct expression");
                }
            } else {
                // if it's not a binary operator; for example, if it's a semicolon, comma, etc., we just break the loop
                // but except some characters like ")", "]", "}", which may be the end of the current expression, other characters isn't legal.
                if (token.name.equals(")") || token.name.equals("]") || token.name.equals("}") || token.name.equals(",") || token.name.equals(";")) {
                    break;
                }
                throw new ParseException("Unexpected token '" + token.name + "' in expression");
            }
        }
        recursionDepth--;
        return node;
    }

    public ExprWithoutBlockNode parseExprWithoutBlockNode() {
        recursionDepth = 0;
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
                throw new ParseException("Expected '&' in borrow expression");
            }
        }
        if (i < tokens.size() && tokens.get(i).name.equals("mut")) {
            node.isMutable = true;
            i++;
        } else {
            node.isMutable = false;
        }
        ExprNode innerExpr = new ExprNode();
        node.innerExpr = parseExprNode(130);
        return node;
    }

    public DerefExprNode parseDerefExprNode() {
        // consume *
        i++;
        DerefExprNode node = new DerefExprNode();
        ExprNode innerExpr = new ExprNode();
        node.innerExpr = parseExprNode(130);
        return node;
    }

    public NegaExprNode parseNegaExprNode() {
        // consume - or !
        NegaExprNode node = new NegaExprNode();
        if (tokens.get(i).name.equals("-")) {
            node.isLogical = false;
        } else if (tokens.get(i).name.equals("!")) {
            node.isLogical = true;
        } else {
            throw new ParseException("Expected '-' or '!' in negation expression");
        }
        i++;
        ExprNode innerExpr = new ExprNode();
        node.innerExpr = parseExprNode(130);
        return node;
    }



    public LiteralExprNode parseLiteralExprNode() {
        LiteralExprNode node = new LiteralExprNode();
        token_t token = tokens.get(i);
        if (token.tokentype == token_t.TokenType_t.INTEGER_LITERAL) {
            node.literalType = literal_t.INT;
            try {
                node.value_int = Integer.parseInt(token.name);
            } catch (NumberFormatException e) {
                // Handle integer literals that might be too large or in different formats
                node.value_int = 0; // Default value in case of parsing error
            }
            i++;
        } else if (token.tokentype == token_t.TokenType_t.CHAR_LITERAL) {
            node.literalType = literal_t.CHAR;
            node.value_string = token.name;
            i++;
        } else if (token.tokentype == token_t.TokenType_t.STRING_LITERAL || token.tokentype == token_t.TokenType_t.RAW_STRING_LITERAL) {
            node.literalType = literal_t.STRING;
            node.value_string = token.name;
            i++;
        } else if (token.tokentype == token_t.TokenType_t.C_STRING_LITERAL || token.tokentype == token_t.TokenType_t.RAW_C_STRING_LITERAL) {
            node.literalType = literal_t.CSTRING;
            node.value_string = token.name;
            i++;
        } else if (token.name.equals("true")) {
            node.literalType = literal_t.BOOL;
            node.value_bool = true;
            i++;
        } else if (token.name.equals("false")) {
            node.literalType = literal_t.BOOL;
            node.value_bool = false;
            i++;
        } else {
            throw new ParseException("Expected literal in literal expression");
        }
        return node;
    }

    public PathExprNode parsePathExprNode() {
        PathExprNode node = new PathExprNode();
        if (i < tokens.size()) {
            PathExprSegNode LSeg = new PathExprSegNode();
            node.LSeg = parsePathExprSegNode();
        } else {
            throw new ParseException("Expected path segment in path expression");
        }
        if (i < tokens.size() && tokens.get(i).name.equals("::")) {
            i++;
            if (i < tokens.size()) {
                PathExprSegNode RSeg = new PathExprSegNode();
                node.RSeg = parsePathExprSegNode();
            } else {
                throw new ParseException("Expected path segment after '::' in path expression");
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
            node.name = parseIdentifierNode();
            node.patternType = patternSeg_t.IDENT;
        } else if (i < tokens.size() && tokens.get(i).name.equals("self")) {
            node.patternType = patternSeg_t.SELF;
            i++;
        } else if (i < tokens.size() && tokens.get(i).name.equals("Self")) {
            node.patternType = patternSeg_t.SELF_TYPE;
            i++;
        } else {
            throw new ParseException("Expected identifier, 'self' or 'Self' in path segment");
        }
        return node;
    }

    public ArrayExprNode parseArrayExprNode() {
        // expect [
        ArrayExprNode node = new ArrayExprNode();
        if (i < tokens.size() && tokens.get(i).name.equals("[")) {
            i++;
        } else {
            throw new ParseException("Expected '[' at start of array expression");
        }
        
        // Check for empty array
        if (i < tokens.size() && tokens.get(i).name.equals("]")) {
            i++;
            node.elements = null;
            node.repeatedElement = null;
            node.size = null;
            return node;
        }
        
        // Parse first element
        ExprNode firstElement = parseExprNode();
        
        // Check if it's a repeated element array
        if (i < tokens.size() && tokens.get(i).name.equals(";")) {
            i++;
            node.size = parseExprNode();
            node.repeatedElement = firstElement;
            node.elements = null;
        } else {
            // It's a regular elements array
            Vector<ExprNode> elements = new Vector<>();
            elements.add(firstElement);
            
            while (i < tokens.size() && tokens.get(i).name.equals(",")) {
                i++;
                // Check if there's a trailing comma (next token is ']')
                if (i < tokens.size() && tokens.get(i).name.equals("]")) {
                    break;
                }
                elements.add(parseExprNode());
            }
            
            node.elements = elements;
            node.repeatedElement = null;
            node.size = null;
        }
        
        // expect ]
        if (i < tokens.size() && tokens.get(i).name.equals("]")) {
            i++;
        } else {
            throw new ParseException("Expected ']' at end of array expression");
        }
        return node;
    }

    public IndexExprNode parseIndexExprNode() {
        IndexExprNode node = new IndexExprNode();
        ExprNode array = new ExprNode();
        node.array = parseExprNode();
        // expect [
        if (i < tokens.size() && tokens.get(i).name.equals("[")) {
            i++;
        } else {
            throw new ParseException("Expected '[' at start of index expression");
        }
        ExprNode index = new ExprNode();
        node.index = parseExprNode();
        // expect ]
        if (i < tokens.size() && tokens.get(i).name.equals("]")) {
            i++;
        } else {
            throw new ParseException("Expected ']' at end of index expression");
        }
        return node;
    }

    public StructExprNode parseStructExprNode() {
        StructExprNode node = new StructExprNode();
        node.structName = parsePathExprSegNode();
        // expect {
        if (i < tokens.size() && tokens.get(i).name.equals("{")) {
            i++;
        } else {
            throw new ParseException("Expected '{' at start of struct expression");
        }
        
        // Check for empty struct
        if (i < tokens.size() && tokens.get(i).name.equals("}")) {
            i++;
            node.fieldValues = null;
            return node;
        }
        
        Vector<FieldValNode> fieldValues = new Vector<>();
        while (i < tokens.size() && !tokens.get(i).name.equals("}")) {
            fieldValues.add(parseFieldValNode());
            if (i < tokens.size() && tokens.get(i).name.equals(",")) {
                i++;
            } else if (i >= tokens.size() || !tokens.get(i).name.equals("}")) {
                throw new ParseException("Expected ',' or '}' in field assignment list");
            }
        }
        node.fieldValues = fieldValues;
        
        // expect }
        if (i < tokens.size() && tokens.get(i).name.equals("}")) {
            i++;
        } else {
            throw new ParseException("Expected '}' at end of struct expression");
        }
        return node;
    }

    public FieldValNode parseFieldValNode() {
        FieldValNode node = new FieldValNode();
        // expect identifier
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
            node.fieldName = parseIdentifierNode();
        } else {
            throw new ParseException("Expected field name in struct expression");
        }
        // expect :
        if (i < tokens.size() && tokens.get(i).name.equals(":")) {
            i++;
        } else {
            throw new ParseException("Expected ':' after field name in struct expression");
        }
        ExprNode value = new ExprNode();
        node.value = parseExprNode();
        return node;
    }

    public CallExprNode parseCallExprNode() {
        CallExprNode node = new CallExprNode();
        node.function = parseExprNode();
        // expect (
        if (i < tokens.size() && tokens.get(i).name.equals("(")) {
            i++;
        } else {
            throw new ParseException("Expected '(' at start of call expression");
        }
        
        // Check for empty argument list
        if (i < tokens.size() && tokens.get(i).name.equals(")")) {
            i++;
            node.arguments = null;
            return node;
        }
        
        Vector<ExprNode> arguments = new Vector<>();
        arguments.add(parseExprNode());
        while (i < tokens.size() && tokens.get(i).name.equals(",")) {
            i++;
            arguments.add(parseExprNode());
        }
        node.arguments = arguments;
        
        // expect )
        if (i < tokens.size() && tokens.get(i).name.equals(")")) {
            i++;
        } else {
            throw new ParseException("Expected ')' at end of argument list");
        }
        return node;
    }

    public MethodCallExprNode parseMethodCallExprNode() {
        MethodCallExprNode node = new MethodCallExprNode();
        node.receiver = parseExprNode();
        // expect .
        if (i < tokens.size() && tokens.get(i).name.equals(".")) {
            i++;
        } else {
            throw new ParseException("Expected '.' before method name in method call");
        }
        node.methodName = parsePathExprSegNode();
        // expect (
        if (i < tokens.size() && tokens.get(i).name.equals("(")) {
            i++;
        } else {
            throw new ParseException("Expected '(' at start of method call");
        }
        
        // Check for empty argument list
        if (i < tokens.size() && tokens.get(i).name.equals(")")) {
            i++;
            node.arguments = null;
            return node;
        }
        
        Vector<ExprNode> arguments = new Vector<>();
        arguments.add(parseExprNode());
        while (i < tokens.size() && tokens.get(i).name.equals(",")) {
            i++;
            arguments.add(parseExprNode());
        }
        node.arguments = arguments;
        
        // expect )
        if (i < tokens.size() && tokens.get(i).name.equals(")")) {
            i++;
        } else {
            throw new ParseException("Expected ')' at end of method call argument list");
        }
        return node;
    }

    public FieldExprNode parseFieldExprNode() {
        FieldExprNode node = new FieldExprNode();
        ExprNode receiver = new ExprNode();
        node.receiver = parseExprNode();
        // expect .
        if (i < tokens.size() && tokens.get(i).name.equals(".")) {
            i++;
        } else {
            throw new ParseException("Expected '.' before field name in field expression");
        }
        if (i < tokens.size() && isIdentifier(tokens.get(i))) {
            node.fieldName = parseIdentifierNode();
        } else {
            throw new ParseException("Expected identifier as field name in field expression");
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
        if (i < tokens.size() && !tokens.get(i).name.equals(";") && !tokens.get(i).name.equals("}")) {
            node.value = parseExprNode();
        } else {
            node.value = null;
        }
        return node;
    }

    public ReturnExprNode parseReturnExprNode() {
        // consume "return"
        i++;
        ReturnExprNode node = new ReturnExprNode();
        if (i < tokens.size() && !tokens.get(i).name.equals(";") && !tokens.get(i).name.equals("}")) {
            node.value = parseExprNode();
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
            throw new ParseException("Expected 'if', 'while', 'loop' or '{' in block expression");
        }
        return node;
    }

    public BlockExprNode parseBlockExprNode() {
        BlockExprNode node = new BlockExprNode();
        // expect {
        if (i < tokens.size() && tokens.get(i).name.equals("{")) {
            i++;
        } else {
            throw new RuntimeException("Expected '{' at start of block expression");
        }
        
        Vector<StmtNode> statements = new Vector<>();
        
        // Check for empty block
        if (i < tokens.size() && !tokens.get(i).name.equals("}")) {
            // Parse statements and expressions according to the new syntax:
            // <statements> ::= <statement>+
            //                | <statement>+ <expressionwithoutblock>
            //                | <expressionwithoutblock>
            
            boolean hasExpression = false;
            
            while (i < tokens.size() && !tokens.get(i).name.equals("}")) {
                int startIndex = i;
                try {
                    // Try to parse a statement first
                    // System.out.println("Attempting to parse statement at token: " + tokens.get(i).name);
                    StmtNode stmt = parseStmtNode();
                    if (stmt != null) {
                        statements.add(stmt);
                    }
                } catch (Exception e) {
                    i = startIndex; // Reset index if statement parsing fails
                    // System.out.println("Failed to parse statement: " + e.getMessage());
                    // If statement parsing fails, try to parse an expression
                    try {
                        ExprWithoutBlockNode expr = parseExprWithoutBlockNode();
                        if (expr != null) {
                            // Wrap expression in an expression statement
                            ExprStmtNode exprStmt = new ExprStmtNode();
                            exprStmt.expr = expr;
                            statements.add(exprStmt);
                            hasExpression = true;
                            
                            // After an expression, we should reach the end of the block
                            // according to the syntax rules
                            break;
                        }
                    } catch (Exception exprE) {
                        // If both fail, rethrow the original exception
                        throw e;
                    }
                }
            }
        }
        
        node.statements = statements;
        
        // expect }
        if (i < tokens.size() && tokens.get(i).name.equals("}")) {
            i++;
        } else {
            throw new RuntimeException("Expected '}' at end of block expression");
        }
        return node;
    }

    public IfExprNode parseIfExprNode() {
        IfExprNode node = new IfExprNode();
        // expect if
        if (i < tokens.size() && tokens.get(i).name.equals("if")) {
            i++;
        } else {
            throw new ParseException("Expected 'if' at start of if expression");
        }
        node.condition = parseGroupExprNode();
        node.thenBranch = parseBlockExprNode();
        // check for else or elseif
        if (i < tokens.size() && tokens.get(i).name.equals("else")) {
            i++;
            if (i < tokens.size() && tokens.get(i).name.equals("if")) {
                node.elseifBranch = parseIfExprNode();
                node.elseBranch = null;
            } else {
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
            node.condition = parseGroupExprNode();
        } else if (i < tokens.size() && tokens.get(i).name.equals("loop")) {
            i++;
            node.condition = null;
        } else {
            throw new ParseException("Expected 'while' or 'loop' at start of loop expression");
        }
        node.body = parseBlockExprNode();
        return node;
    }

    public IdentifierNode parseIdentifierNode() {
        if (!(i < tokens.size() && isIdentifier(tokens.get(i)))) throw new ParseException("Expected identifier");
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
            throw new ParseException("Expected '&' at start of reference type");
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
            throw new ParseException("Expected '[' at start of array type");
        }
        TypeExprNode elementType = new TypeExprNode();
        node.elementType = parseTypeExprNode();
        // expect ;
        if (i < tokens.size() && tokens.get(i).name.equals(";")) {
            i++;
        } else {
            throw new ParseException("Expected ';' in array type");
        }
        ExprNode size = new ExprNode();
        node.size = parseExprNode(0);
        // expect ]
        if (i < tokens.size() && tokens.get(i).name.equals("]")) {
            i++;
        } else {
            throw new ParseException("Expected ']' at end of array type");
        }
        return node;
    }
    public TypeUnitExprNode parseTypeUnitExprNode() {
        TypeUnitExprNode node = new TypeUnitExprNode();
        // expect (
        if (i < tokens.size() && tokens.get(i).name.equals("(")) {
            i++;
        } else {
            throw new ParseException("Expected '(' at start of unit type");
        }
        // expect )
        if (i < tokens.size() && tokens.get(i).name.equals(")")) {
            i++;
        } else {
            throw new ParseException("Expected ')' at end of unit type");
        }
        return node;
    }

    public StmtNode parseStmtNode() {
        // 1. Input validation
        if (!(i < tokens.size())) throw new ParseException("No more tokens to parse in statement");
        
        // 2. Get current token
        token_t token = tokens.get(i);
        
        // 3. Empty statement handling
        if (token.name.equals(";")) {
            i++; // consume semicolon
            return null; // return null for empty statement
        }
        
        // 4. Let statement handling
        if (token.name.equals("let")) {
            return parseLetStmtNode();
        }
        // 5. Item type handling
        else if (token.name.equals("fn")) {
            // Function item
            return parseFunctionNode(false);
        }
        else if (token.name.equals("struct")) {
            // Struct item
            return parseStructNode();
        }
        else if (token.name.equals("enum")) {
            // Enum item
            return parseEnumNode();
        }
        else if (token.name.equals("const")) {
            // Const item or const function
            // Need to check next token to determine if it's a const item or const function
            if (i + 1 < tokens.size() && tokens.get(i + 1).name.equals("fn")) {
                // Const function
                i++; // consume const
                return parseFunctionNode(true); // parseFunctionNode() will handle const prefix
            } else {
                // Regular const item
                return parseConstItemNode();
            }
        }
        else if (token.name.equals("trait")) {
            // Trait item
            return parseTraitNode();
        }
        else if (token.name.equals("impl")) {
            // Impl item
            return parseImplNode();
        }
        // 6. Expression statement handling (default case)
        else {
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
            // Skip null statements (empty statements with just semicolons)
            if (stmt != null) {
                // System.out.println(stmt instanceof FunctionNode);
                statements.add(stmt);
            }
        }
    }
}