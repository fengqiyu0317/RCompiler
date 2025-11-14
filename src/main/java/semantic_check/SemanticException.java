// Semantic analysis exception class

public class SemanticException extends RuntimeException {
    private final SelfErrorType errorType;
    private final ASTNode node;
    
    public SemanticException(SelfErrorType errorType, String message, ASTNode node) {
        super(message);
        this.errorType = errorType;
        this.node = node;
    }
    
    public SelfErrorType getErrorType() {
        return errorType;
    }
    
    public ASTNode getNode() {
        return node;
    }
}