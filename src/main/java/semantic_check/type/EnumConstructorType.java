// Enum constructor type implementation

public class EnumConstructorType implements Type {
    private final EnumType enumType;
    
    public EnumConstructorType(EnumType enumType) {
        this.enumType = enumType;
    }
    
    public EnumType getEnumType() {
        return enumType;
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
}