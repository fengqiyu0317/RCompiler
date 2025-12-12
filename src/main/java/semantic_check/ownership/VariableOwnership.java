/**
 * 变量所有权状态类
 */
public class VariableOwnership {
    private final String varName;
    private final boolean isMutable;
    private boolean isMoved;
    
    public VariableOwnership(String varName, boolean isMutable) {
        this.varName = varName;
        this.isMutable = isMutable;
        this.isMoved = false;
    }
    
    /**
     * 拷贝构造函数
     */
    public VariableOwnership(VariableOwnership other) {
        this.varName = other.varName;
        this.isMutable = other.isMutable;
        this.isMoved = other.isMoved;
    }
    
    public String getVarName() {
        return varName;
    }
    
    public boolean isMutable() {
        return isMutable;
    }
    
    public boolean isMoved() {
        return isMoved;
    }
    
    public void setMoved(boolean moved) {
        isMoved = moved;
    }
}