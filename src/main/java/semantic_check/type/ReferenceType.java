// Reference type implementation

public class ReferenceType implements Type {
    private final Type innerType;
    private final boolean isMutable;
    
    public ReferenceType(Type innerType, boolean isMutable) {
        this.innerType = innerType;
        this.isMutable = isMutable;
    }
    
    public Type getInnerType() {
        return innerType;
    }
    
    public boolean isMutable() {
        return isMutable;
    }
    
    @Override
    public boolean equals(Type other) {
        if (other instanceof ReferenceType) {
            ReferenceType otherRef = (ReferenceType) other;
            return innerType.equals(otherRef.innerType);
        }
        return false;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("&");
        if (isMutable) {
            sb.append("mut ");
        }
        sb.append(innerType.toString());
        return sb.toString();
    }
    
    @Override
    public Type getBaseType() {
        return innerType.getBaseType();
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