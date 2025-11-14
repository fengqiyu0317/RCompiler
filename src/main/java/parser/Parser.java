import java.util.Vector;
import java.util.List;

public class Parser {
    private final TokenStream tokenStream;
    private final ErrorReporter errorReporter;
    private final ParserConfiguration config;
    private final ExpressionParser expressionParser;
    private final TypeParser typeParser;
    private final PatternParser patternParser;
    private final StatementParser statementParser;
    
    public Parser(Vector<token_t> tokens) {
        this.tokenStream = new TokenStream(tokens);
        this.errorReporter = new ErrorReporter();
        this.config = new ParserConfiguration();
        
        // Create parser instances with circular dependencies
        // First create basic instances
        this.expressionParser = new ExpressionParser(tokenStream, errorReporter, config);
        this.typeParser = new TypeParser(tokenStream, errorReporter, config, expressionParser);
        this.patternParser = new PatternParser(tokenStream, errorReporter, config);
        this.statementParser = new StatementParser(tokenStream, errorReporter, config,
                                             expressionParser, typeParser, patternParser);
        
        // Now set up the circular dependencies
        this.expressionParser.setStatementParser(statementParser);
        this.expressionParser.setTypeParser(typeParser);
        this.typeParser.setStatementParser(statementParser);
        this.patternParser.setExpressionParser(expressionParser);
        this.patternParser.setTypeParser(typeParser);
        this.patternParser.setStatementParser(statementParser);
    }
    
    // Grammar: <program> ::= <statement>*
    // Main entry method
    public void parse() throws ParserException {
        try {
            Vector<StmtNode> statements = new Vector<>();
            
            while (!tokenStream.isAtEnd()) {
                try {
                    StmtNode stmt = statementParser.parseStatement();
                    if (stmt != null) {
                        statements.add(stmt);
                    }
                } catch (ParserException e) {
                    System.err.println("Error parsing statement: " + e.getMessage());
                    throw e;
                } catch (Exception e) {
                    throw new ParserException("Error parsing program: " + e.getMessage());
                }
            }
            
            this.statements = statements;
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error parsing program: " + e.getMessage());
        }
    }
    
    
    public Vector<StmtNode> getStatements() {
        return statements;
    }
    
    public boolean hasErrors() {
        return errorReporter.hasErrors();
    }
    
    public List<String> getErrors() {
        return errorReporter.getErrors();
    }
    
    private Vector<StmtNode> statements;
    
    // Accessor methods for direct access to parsers
    // This provides a clean API for advanced users who need more control
    
    public ExpressionParser getExpressionParser() {
        return expressionParser;
    }
    
    public StatementParser getStatementParser() {
        return statementParser;
    }
    
    public TypeParser getTypeParser() {
        return typeParser;
    }
    
    public PatternParser getPatternParser() {
        return patternParser;
    }
    
    public TokenStream getTokenStream() {
        return tokenStream;
    }
    
    public ErrorReporter getErrorReporter() {
        return errorReporter;
    }
    
    public ParserConfiguration getConfig() {
        return config;
    }
}