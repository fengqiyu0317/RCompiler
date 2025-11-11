

public abstract class BaseParser {
    protected final TokenStream tokenStream;
    protected final ErrorReporter errorReporter;
    protected final ParserConfiguration config;
    
    public BaseParser(TokenStream tokenStream, ErrorReporter errorReporter, ParserConfiguration config) {
        this.tokenStream = tokenStream;
        this.errorReporter = errorReporter;
        this.config = config;
    }
    
    // Grammar: <identifier> ::= [a-zA-Z_][a-zA-Z0-9_]*
    // Common utility methods
    public IdentifierNode parseIdentifier() throws ParserException {
        if (!config.isIdentifier(tokenStream.current())) {
            errorReporter.reportError("Expected identifier", tokenStream.current());
            return null; // Will not execute because an exception was thrown above
        }
        IdentifierNode node = new IdentifierNode();
        node.name = tokenStream.consume().name;
        return node;
    }
    
    public int getPrecedence(token_t token) {
        // Based on Rust Reference operator precedence table
        // Highest precedence (210): Struct expressions - path followed by {
        if (token.name.equals("{")) {
            return 210;
        }
        
        // High precedence (200): Paths - identifiers, qualified paths
        // These are typically handled as primary expressions, but we need a precedence value
        if (token.name.equals("::")) {
            return 200;
        }
        
        // Method calls (190): .method() expressions
        // Field expressions (180): .field access
        if (token.name.equals(".")) {
            return 190; // Will be handled as both method calls and field access
        }
        
        // Function calls, array indexing (170): () and []
        if (token.name.equals("(") || token.name.equals("[")) {
            return 170;
        }
        
        // Unary operators: -, !, *, borrow (&, &mut) (160)
        // These are handled separately in parsing as prefix operators
        if (token.name.equals("-") || token.name.equals("!") || token.name.equals("*") ||
            token.name.equals("&")) {
            // Note: Need context to determine if * is dereference or multiplication
            // and if - is unary or binary. This is handled in the parser.
            return 160;
        }
        
        // Type cast: as (150)
        if (token.name.equals("as")) {
            return 150;
        }
        
        // Multiplicative operators: *, /, % (140)
        if (token.name.equals("*") || token.name.equals("/") || token.name.equals("%")) {
            return 140;
        }
        
        // Additive operators: +, - (130)
        if (token.name.equals("+") || token.name.equals("-")) {
            return 130;
        }
        
        // Shift operators: <<, >> (120)
        if (token.name.equals("<<") || token.name.equals(">>")) {
            return 120;
        }
        
        // Bitwise AND: & (110)
        if (token.name.equals("&")) {
            return 110;
        }
        
        // Bitwise XOR: ^ (100)
        if (token.name.equals("^")) {
            return 100;
        }
        
        // Bitwise OR: | (90)
        if (token.name.equals("|")) {
            return 90;
        }
        
        // Comparison operators: ==, !=, <, >, <=, >= (80)
        if (token.name.equals("==") || token.name.equals("!=") || token.name.equals("<") ||
            token.name.equals("<=") || token.name.equals(">") || token.name.equals(">=")) {
            return 80;
        }
        
        // Logical AND: && (70)
        if (token.name.equals("&&")) {
            return 70;
        }
        
        // Logical OR: || (60)
        if (token.name.equals("||")) {
            return 60;
        }
        
        // Assignment operators: =, +=, -=, *=, /=, %=, &=, |=, ^=, <<=, >>= (50)
        if (token.name.equals("=") || token.name.equals("+=") || token.name.equals("-=") ||
            token.name.equals("*=") || token.name.equals("/=") || token.name.equals("%=") ||
            token.name.equals("&=") || token.name.equals("|=") || token.name.equals("^=") ||
            token.name.equals("<<=") || token.name.equals(">>=")) {
            return 50;
        }
        
        // return, break (40) - lowest precedence
        if (token.name.equals("return") || token.name.equals("break")) {
            return 40;
        }
        
        return 0; // No precedence for non-operators
    }
    
    public oper_t getOperator(String name) {
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
                throw new ParserException("Unknown operator: " + name);
        }
    }
    
    protected void expect(String tokenName) throws ParserException {
        if (!tokenStream.matches(tokenName)) {
            errorReporter.reportError("Expected '" + tokenName + "'", tokenStream.current());
        }
    }
    
    protected void expect(String tokenName, String errorMessage) throws ParserException {
        if (!tokenStream.matches(tokenName)) {
            errorReporter.reportError(errorMessage, tokenStream.current());
        }
    }
    
    protected boolean checkThenConsume(String tokenName) {
        if (tokenStream.current() != null && tokenStream.equals(tokenName)) {
            tokenStream.consume();
            return true;
        }
        return false;
    }
    
    // Grammar: <pathseg> ::= <identifier> | self | Self
    // Parse path segment
    public PathExprSegNode parsePathSegment() {
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
            errorReporter.reportError("Expected path segment", token);
        }
        
        return node;
    }
}