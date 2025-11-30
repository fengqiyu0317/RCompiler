// Never type implementation

public class NeverType implements Type {
    public static final NeverType INSTANCE = new NeverType();
    
    private NeverType() {}
    
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
}