// Never type implementation

public class NeverType implements Type {
    public static final NeverType INSTANCE = new NeverType();
    public static final NeverType MUTABLE_INSTANCE = new NeverType(true);
    
    private boolean isMutable;
    
    private NeverType() {
        this(false);
    }
    
    private NeverType(boolean isMutable) {
        this.isMutable = isMutable;
    }
    
    @Override
    public boolean equals(Type other) {
        return other instanceof NeverType;
    }
    
    @Override
    public String toString() {
        return "!";
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
        return true;
    }
    
    public boolean isMutable() {
        return isMutable;
    }
    
    @Override
    public void setMutability(boolean isMutable) {
        this.isMutable = isMutable;
    }
    
}