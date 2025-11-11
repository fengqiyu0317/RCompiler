

public class TypeParser extends BaseParser {
    private ExpressionParser expressionParser;
    private StatementParser statementParser;
    
    public TypeParser(TokenStream tokenStream, ErrorReporter errorReporter,
                    ParserConfiguration config, ExpressionParser expressionParser) {
        super(tokenStream, errorReporter, config);
        this.expressionParser = expressionParser;
    }
    
    // Setters for dependency injection
    public void setExpressionParser(ExpressionParser expressionParser) {
        this.expressionParser = expressionParser;
    }
    
    public void setStatementParser(StatementParser statementParser) {
        this.statementParser = statementParser;
    }
    
    // Grammar: <type> ::= <typepathexpr> | <typerefexpr> | <typearrayexpr> | <typeunitexpr>
    // Main type parsing method
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
    
    // Grammar: <typerefexpr> ::= & (mut)? <type>
    // Parse reference type (& or &mut)
    public TypeRefExprNode parseReferenceType() {
        tokenStream.consume("&");
        
        TypeRefExprNode node = new TypeRefExprNode();
        node.isMutable = tokenStream.matches("mut");
        node.innerType = parseType();
        
        return node;
    }
    
    // Grammar: <typearrayexpr> ::= [ <type> ; <expression> ]
    // Parse array type [T; N]
    public TypeArrayExprNode parseArrayType() {
        tokenStream.consume("[");
        
        TypeArrayExprNode node = new TypeArrayExprNode();
        node.elementType = parseType();
        
        tokenStream.consume(";");
        
        if (expressionParser != null) {
            node.size = expressionParser.parse();
        } else {
            errorReporter.reportError("ExpressionParser not available for array size", tokenStream.current());
        }
        
        tokenStream.consume("]");
        
        return node;
    }
    
    // Grammar: <typeunitexpr> ::= ()
    // Parse unit type ()
    public TypeUnitExprNode parseUnitType() {
        tokenStream.consume("(");
        tokenStream.consume(")");
        
        return new TypeUnitExprNode();
    }
    
    // Grammar: <typepathexpr> ::= <pathseg>
    // Parse path type
    public TypePathExprNode parsePathType() {
        TypePathExprNode node = new TypePathExprNode();
        node.path = parsePathSegment();
        return node;
    }
    
    // Note: parsePathSegment is now implemented in BaseParser and inherited by TypeParser
}