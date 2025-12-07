
/**
 * 表达式类型检查上下文
 * 管理类型检查过程中的上下文信息
 */
public class ExpressionTypeContext {
    // Current Self type
    private Type currentSelfType;
    
    // Current function node
    private FunctionNode currentFunction;
    
    public ExpressionTypeContext() {
        this.currentSelfType = null;
        this.currentFunction = null;
    }
    
    /**
     * Get current Self type
     */
    public Type getCurrentSelfType() {
        return currentSelfType;
    }
    
    /**
     * Set current Self type
     */
    public void setCurrentSelfType(Type type) {
        this.currentSelfType = type;
    }
    
    /**
     * Clear current Self type
     */
    public void clearCurrentSelfType() {
        this.currentSelfType = null;
    }
    
    /**
     * Get current function node
     */
    public FunctionNode getCurrentFunction() {
        return currentFunction;
    }
    
    /**
     * Set current function node
     */
    public void setCurrentFunction(FunctionNode function) {
        this.currentFunction = function;
    }
    
    /**
     * Clear current function node
     */
    public void clearCurrentFunction() {
        this.currentFunction = null;
    }
    
    /**
     * Enter function context
     */
    public void enterFunctionContext(FunctionNode node) {
        this.currentFunction = node;
    }
    
    /**
     * Exit function context
     */
    public void exitFunctionContext() {
        this.currentFunction = null;
    }
}