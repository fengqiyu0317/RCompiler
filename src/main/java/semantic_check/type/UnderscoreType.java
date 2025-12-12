// Underscore type for underscore expressions
public class UnderscoreType implements Type {
    public static final UnderscoreType INSTANCE = new UnderscoreType();
    
    private UnderscoreType() {
        // Private constructor for singleton
    }
    
    @Override
    public boolean equals(Type other) {
        // UnderscoreType 不等于任何其他类型，包括自己
        return false;
    }
    
    @Override
    public String toString() {
        return "_";
    }
    
    @Override
    public Type getBaseType() {
        return this;
    }
    
    @Override
    public boolean isNumeric() {
        return false;
    }
    
    @Override
    public boolean isBoolean() {
        return false;
    }
    
    @Override
    public boolean isUnit() {
        return false;
    }
    
    @Override
    public boolean isNever() {
        return false;
    }
    
    @Override
    public boolean isMutable() {
        return false;
    }
    
    @Override
    public void setMutability(boolean isMutable) {
        // UnderscoreType 不可变
    }
}