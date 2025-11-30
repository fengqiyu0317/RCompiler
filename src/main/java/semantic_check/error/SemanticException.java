// Semantic analysis exception class

public class SemanticException extends RuntimeException {
    private final ASTNode node;
    
    public SemanticException(String message, ASTNode node) {
        super(message);
        this.node = node;
    }
    
    public ASTNode getNode() {
        return node;
    }
}