// Semantic error class

public class SemanticError {
    private final ErrorType type;
    private final String message;
    private final ASTNode node;
    private final int line;
    private final int column;
    
    public SemanticError(ErrorType type, String message, ASTNode node) {
        this.type = type;
        this.message = message;
        this.node = node;
        
        // Should get line and column numbers from AST node here
        // Simplified implementation: use default values
        this.line = 0;
        this.column = 0;
    }
    
    // Getter methods
    public ErrorType getType() { return type; }
    public String getMessage() { return message; }
    public ASTNode getNode() { return node; }
    public int getLine() { return line; }
    public int getColumn() { return column; }
    
    @Override
    public String toString() {
        return String.format("Semantic Error [%s]: %s (Line: %d, Column: %d)",
                           type.getDescription(), message, line, column);
    }
}