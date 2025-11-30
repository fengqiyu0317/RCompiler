// Context information class

public class ContextInfo {
    private final ContextType type;
    private final ContextInfo parent;     // Parent context
    
    public ContextInfo(ContextType type, ContextInfo parent) {
        this.type = type;
        this.parent = parent;
    }
    
    // Getter methods
    public ContextType getType() { return type; }
    public ContextInfo getParent() { return parent; }
    
    // Check current context type
    public boolean isCurrentImplBlock() {
        return type == ContextType.IMPL_BLOCK;
    }
    
    public boolean isCurrentTraitBlock() {
        return type == ContextType.TRAIT_BLOCK;
    }
    
    public boolean isCurrentMethod() {
        return type == ContextType.METHOD;
    }
    
    public boolean isCurrentAssociatedFunction() {
        return type == ContextType.ASSOCIATED_FUNC;
    }
    
    // Check if Self can be used in current context (only current context)
    public boolean canUseSelfType() {
        return isCurrentImplBlock() || isCurrentTraitBlock() || isCurrentMethod() || isCurrentAssociatedFunction();
    }
    
    // Check if self can be used in current context (only current context)
    public boolean canUseSelfValue() {
        return isCurrentMethod();  // self can only be used in METHOD context
    }
}