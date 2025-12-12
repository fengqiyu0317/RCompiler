// Reference type implementation

public class ReferenceType implements Type {
    private final Type innerType;
    private boolean isReferenceMutable;  // Whether the reference itself can be reassigned
    private boolean isValueMutable;      // Whether the referenced value can be modified
    
    public ReferenceType(Type innerType, boolean isReferenceMutable, boolean isValueMutable) {
        this.innerType = innerType;
        this.isReferenceMutable = isReferenceMutable;
        this.isValueMutable = isValueMutable;
    }
    
    // Constructor for backward compatibility - assumes value mutability matches reference mutability
    public ReferenceType(Type innerType, boolean isMutable) {
        this(innerType, true, isMutable);
    }
    
    public Type getInnerType() {
        // 如果innerType是ReferenceType，需要调整其可变性属性
        if (innerType instanceof ReferenceType) {
            ReferenceType innerRefType = (ReferenceType) innerType;
            // 创建一个新的ReferenceType，其isValueMutable等于当前的isValueMutable
            // isReferenceMutable为true，表示这个内部引用本身是可变的
            return new ReferenceType(
                innerRefType.getInnerType(),
                true,  // isReferenceMutable设为true
                this.isValueMutable  // isValueMutable继承自当前引用
            );
        } else {
            // 对于其它类型，需要创建一个新类型，其isMutable等于当前的isValueMutable
            return TypeUtils.createMutableType(innerType, this.isValueMutable);
        }
    }
    

    public boolean isReferenceMutable() {
        return isReferenceMutable;
    }

    public boolean isValueMutable() {
        return isValueMutable;
    }
    
    @Override
    public void setMutability(boolean isReferenceMutable) {
        this.isReferenceMutable = isReferenceMutable;
    }
    
    @Override
    public boolean equals(Type other) {
        if (other instanceof ReferenceType) {
            ReferenceType otherRef = (ReferenceType) other;
            return innerType.equals(otherRef.innerType) &&
                   isReferenceMutable == otherRef.isReferenceMutable &&
                   isValueMutable == otherRef.isValueMutable;
        }
        return false;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("&");
        if (isReferenceMutable) {
            sb.append("mut ");
        }
        sb.append(innerType.toString());
        // Add a notation to indicate value mutability if different from reference mutability
        if (isValueMutable != isReferenceMutable) {
            sb.append(isValueMutable ? " (value mut)" : " (value immut)");
        }
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
    
    @Override
    public boolean isMutable() {
        // For reference types, mutability is determined by the reference itself
        return isReferenceMutable;
    }
    
}