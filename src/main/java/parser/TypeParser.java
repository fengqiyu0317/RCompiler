

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
        try {
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
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing type: " + e.getMessage());
        }
    }
    
    // Grammar: <typerefexpr> ::= & (mut)? <type>
    // Parse reference type (& or &mut)
    public TypeRefExprNode parseReferenceType() {
        try {
            tokenStream.consume("&");
            
            TypeRefExprNode node = new TypeRefExprNode();
            node.isMutable = tokenStream.matches("mut");
            node.innerType = parseType();
            
            return node;
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing reference type: " + e.getMessage());
        }
    }
    
    // Grammar: <typearrayexpr> ::= [ <type> ; <expression> ]
    // Parse array type [T; N]
    public TypeArrayExprNode parseArrayType() {
        try {
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
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing array type: " + e.getMessage());
        }
    }
    
    // Grammar: <typeunitexpr> ::= ()
    // Parse unit type ()
    public TypeUnitExprNode parseUnitType() {
        try {
            tokenStream.consume("(");
            tokenStream.consume(")");
            
            return new TypeUnitExprNode();
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing unit type: " + e.getMessage());
        }
    }
    
    // Grammar: <typepathexpr> ::= <pathseg>
    // Parse path type
    public TypePathExprNode parsePathType() {
        try {
            TypePathExprNode node = new TypePathExprNode();
            node.path = parsePathSegment();
            return node;
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing path type: " + e.getMessage());
        }
    }
    
    // Note: parsePathSegment is now implemented in BaseParser and inherited by TypeParser
}