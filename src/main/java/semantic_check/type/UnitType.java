// Unit type implementation

public class UnitType implements Type {
    public static final UnitType INSTANCE = new UnitType();
    
    private UnitType() {}
    
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
}