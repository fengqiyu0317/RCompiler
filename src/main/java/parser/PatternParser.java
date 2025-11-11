

public class PatternParser extends BaseParser {
    private ExpressionParser expressionParser;
    private TypeParser typeParser;
    private StatementParser statementParser;
    
    public PatternParser(TokenStream tokenStream, ErrorReporter errorReporter, ParserConfiguration config) {
        super(tokenStream, errorReporter, config);
    }
    
    // Setters for dependency injection
    public void setExpressionParser(ExpressionParser expressionParser) {
        this.expressionParser = expressionParser;
    }
    
    public void setTypeParser(TypeParser typeParser) {
        this.typeParser = typeParser;
    }
    
    public void setStatementParser(StatementParser statementParser) {
        this.statementParser = statementParser;
    }
    
    // Grammar: <pattern> ::= <idpat> | <wildpat> | <refpat>
    // Main pattern parsing method
    public PatternNode parsePattern() {
        if (tokenStream.isAtEnd()) {
            errorReporter.reportError("No more tokens to parse in pattern");
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
    
    // Grammar: <refpat> ::= (& | &&) (mut)? <pattern>
    // Parse reference pattern (& or &&)
    public RefPatNode parseReferencePattern() {
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
    
    // Grammar: <wildpat> ::= _
    // Parse wildcard pattern (_)
    public WildPatNode parseWildcardPattern() {
        tokenStream.consume("_");
        return new WildPatNode();
    }
    
    // Grammar: <idpat> ::= (ref)? (mut)? <identifier>
    // Parse identifier pattern
    public IdPatNode parseIdentifierPattern() {
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