// Unit type implementation

public class UnitType implements Type {
    public static final UnitType INSTANCE = new UnitType();
    public static final UnitType MUTABLE_INSTANCE = new UnitType(true);
    
    private boolean isMutable;
    
    private UnitType() {
        this(false);
    }
    
    private UnitType(boolean isMutable) {
        this.isMutable = isMutable;
    }
    
    @Override
    public boolean equals(Type other) {
        return other instanceof UnitType;
    }
    
    @Override
    public String toString() {
        return "()";
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
        return true;
    }
    
    @Override
    public boolean isNever() {
        return false;
    }
    
    public boolean isMutable() {
        return isMutable;
    }
    
    @Override
    public void setMutability(boolean isMutable) {
        this.isMutable = isMutable;
    }
    
}