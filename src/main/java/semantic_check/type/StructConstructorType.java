// Struct constructor type implementation

public class StructConstructorType implements Type {
    private final StructType structType;
    
    public StructConstructorType(StructType structType) {
        this.structType = structType;
    }
    
    public StructType getStructType() {
        return structType;
    }
    
    @Override
    public boolean equals(Type other) {
        if (other instanceof StructConstructorType) {
            StructConstructorType otherConstructor = (StructConstructorType) other;
            return structType.equals(otherConstructor.structType);
        }
        return false;
    }
    
    @Override
    public String toString() {
        return "StructConstructor<" + structType.toString() + ">";
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