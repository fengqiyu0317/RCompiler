// self and Self checker

public class SelfChecker {
    private ContextInfo currentContext;
    
    public SelfChecker() {
        this.currentContext = new ContextInfo(ContextType.GLOBAL, null);
    }
    
    // Enter new context
    public void enterContext(ContextType type) {
        currentContext = new ContextInfo(type, currentContext);
    }
    
    // Exit current context
    public void exitContext() {
        if (currentContext.getParent() == null) {
            throw new RuntimeException("Cannot exit from root context - context stack underflow");
        }
        currentContext = currentContext.getParent();
    }
    
    // Error reporting method
    private void reportError(SelfErrorType errorType, String message, ASTNode node) {
        throw new SemanticException(message, node);
    }
    
    // Get current context
    public ContextInfo getCurrentContext() {
        return currentContext;
    }
    
    // Check self usage
    public void checkSelfUsage(ASTNode node) {
        ContextInfo context = getCurrentContext();
        if (!context.canUseSelfValue()) {
            if (context.isCurrentAssociatedFunction()) {
                reportError(SelfErrorType.SELF_IN_ASSOCIATED_FUNCTION,
                        "self cannot be used in associated functions", node);
            } else {
                reportError(SelfErrorType.SELF_OUTSIDE_METHOD,
                        "self can only be used in method bodies", node);
            }
        }
    }
    
    // Check Self usage
    public void checkSelfTypeUsage(ASTNode node) {
        ContextInfo context = getCurrentContext();
        if (!context.canUseSelfType()) {
            reportError(SelfErrorType.SELF_TYPE_OUTSIDE_CONTEXT,
                    "Self can only be used in impl blocks or trait definitions", node);
        }
    }
}