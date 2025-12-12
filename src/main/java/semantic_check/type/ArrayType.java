// Array type implementation

public class ArrayType implements Type {
    private final Type elementType;
    private final long size;
    private boolean isMutable;
    
    public ArrayType(Type elementType, long size) {
        this(elementType, size, false);
    }
    
    public ArrayType(Type elementType, long size, boolean isMutable) {
        this.elementType = elementType;
        this.size = size;
        this.isMutable = isMutable;
    }
    
    public Type getElementType() {
        return elementType;
    }
    
    public long getSize() {
        return size;
    }
    
    @Override
    public void setMutability(boolean isMutable) {
        this.isMutable = isMutable;
    }
    
    @Override
    public boolean equals(Type other) {
        if (other instanceof ArrayType) {
            ArrayType otherArray = (ArrayType) other;
            return elementType.equals(otherArray.elementType) && size == otherArray.size;
        }
        return false;
    }
    
    @Override
    public String toString() {
        return "[" + elementType.toString() + "; " + size + "]";
    }
    
    @Override
    public Type getBaseType() {
        return elementType.getBaseType();
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