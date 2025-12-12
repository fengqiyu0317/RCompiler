// Enum constructor type implementation

public class EnumConstructorType implements Type {
    private final EnumType enumType;
    private boolean isMutable;
    
    public EnumConstructorType(EnumType enumType) {
        this(enumType, false);
    }
    
    public EnumConstructorType(EnumType enumType, boolean isMutable) {
        this.enumType = enumType;
        this.isMutable = isMutable;
    }
    
    public EnumType getEnumType() {
        return enumType;
    }
    
    @Override
    public void setMutability(boolean isMutable) {
        this.isMutable = isMutable;
    }
    
    @Override
    public boolean equals(Type other) {
        if (other instanceof EnumConstructorType) {
            EnumConstructorType otherConstructor = (EnumConstructorType) other;
            return enumType.equals(otherConstructor.enumType);
        }
        return false;
    }
    
    @Override
    public String toString() {
        return "EnumConstructor<" + enumType.toString() + ">";
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
        return isMutable;
    }
    
}