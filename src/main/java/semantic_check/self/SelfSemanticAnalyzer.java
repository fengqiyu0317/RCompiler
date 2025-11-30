// Self semantic analyzer class, extends VisitorBase

public class SelfSemanticAnalyzer extends VisitorBase {
    private SelfChecker selfChecker;
    
    // Initialize in constructor
    public SelfSemanticAnalyzer() {
        this.selfChecker = new SelfChecker();
    }
    
    // Get self checker
    public SelfChecker getSelfChecker() {
        return selfChecker;
    }
    
    // ImplNode visit method
    @Override
    public void visit(ImplNode node) {
        // Enter impl block context
        selfChecker.enterContext(ContextType.IMPL_BLOCK);
        
        try {
            // Process impl block content
            if (node.items != null) {
                for (AssoItemNode item : node.items) {
                    item.accept(this);
                }
            }
        } finally {
            // Exit impl block context
            selfChecker.exitContext();
        }
    }
    
    // TraitNode visit method
    @Override
    public void visit(TraitNode node) {
        // Enter trait block context
        selfChecker.enterContext(ContextType.TRAIT_BLOCK);
        
        try {
            // Process trait block content
            if (node.items != null) {
                for (AssoItemNode item : node.items) {
                    item.accept(this);
                }
            }
        } finally {
            // Exit trait block context
            selfChecker.exitContext();
        }
    }
    
    // FunctionNode visit method
    @Override
    public void visit(FunctionNode node) {
        // Check if parent context is impl or trait block
        ContextInfo parentContext = selfChecker.getCurrentContext();
        boolean isInImplOrTrait = parentContext.isCurrentImplBlock() || parentContext.isCurrentTraitBlock();
        
        // Process function parameters (before entering context)
        if (node.selfPara != null) {
            node.selfPara.accept(this);
        }
        
        if (node.parameters != null) {
            for (ParameterNode param : node.parameters) {
                param.accept(this);
            }
        }
        
        // Process function body (after entering context)
        if (node.body != null) {
            if (isInImplOrTrait) {
                // Determine if it's an instance method
                boolean isInstanceMethod = node.selfPara != null;
                
                // Enter method or associated function context
                selfChecker.enterContext(
                    isInstanceMethod ? ContextType.METHOD : ContextType.ASSOCIATED_FUNC
                );
                
                try {
                    // Process function body
                    node.body.accept(this);
                } finally {
                    // Exit method or associated function context
                    selfChecker.exitContext();
                }
            } else {
                // Process global function, enter global context
                selfChecker.enterContext(ContextType.GLOBAL);
                
                try {
                    // Process function body
                    node.body.accept(this);
                } finally {
                    // Exit global context
                    selfChecker.exitContext();
                }
            }
        }
    }
    
    // PathExprSegNode visit method
    @Override
    public void visit(PathExprSegNode node) {
        if (node.patternType == patternSeg_t.SELF) {
            // Check self usage
            selfChecker.checkSelfUsage(node);
        } else if (node.patternType == patternSeg_t.SELF_TYPE) {
            // Check Self usage
            selfChecker.checkSelfTypeUsage(node);
        } else {
            // Process regular identifier
            if (node.name != null) {
                node.name.accept(this);
            }
        }
    }
    // Other visit methods remain unchanged, using parent class default implementation
    // Only methods that need special handling for self and Self are overridden here
}