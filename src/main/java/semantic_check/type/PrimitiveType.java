// Primitive type implementation

public class PrimitiveType implements Type {
    public enum PrimitiveKind {
        INT,     // Undetermined integer (for type inference)
        I32, U32, USIZE, ISIZE, BOOL, CHAR, STR
    }
    
    private final PrimitiveKind kind;
    
    public PrimitiveType(PrimitiveKind kind) {
        this.kind = kind;
    }
    
    public PrimitiveKind getKind() {
        return kind;
    }
    
    @Override
    public boolean equals(Type other) {
        if (other instanceof PrimitiveType) {
            PrimitiveType otherPrimitive = (PrimitiveType) other;
            return kind == otherPrimitive.kind;
        }
        return false;
    }
    
    @Override
    public String toString() {
        switch (kind) {
            case INT: return "int";
            case I32: return "i32";
            case U32: return "u32";
            case USIZE: return "usize";
            case ISIZE: return "isize";
            case BOOL: return "bool";
            case CHAR: return "char";
            case STR: return "str";
            default: return "unknown";
        }
    }
    
    @Override
    public Type getBaseType() {
        return this;
    }
    
    @Override
    public boolean isNumeric() {
        return kind == PrimitiveKind.INT ||
               kind == PrimitiveKind.I32 ||
               kind == PrimitiveKind.U32 ||
               kind == PrimitiveKind.USIZE ||
               kind == PrimitiveKind.ISIZE;
    }
    
    @Override
    public boolean isBoolean() {
        return kind == PrimitiveKind.BOOL;
    }
    
    @Override
    public boolean isUnit() {
        return false;
    }
    
    @Override
    public boolean isNever() {
        return false;
    }
    
    // Factory methods for common primitive types
    public static PrimitiveType getIntType() {
        return new PrimitiveType(PrimitiveKind.INT);
    }
    
    public static PrimitiveType getI32Type() {
        return new PrimitiveType(PrimitiveKind.I32);
    }
    
    public static PrimitiveType getU32Type() {
        return new PrimitiveType(PrimitiveKind.U32);
    }
    
    public static PrimitiveType getUsizeType() {
        return new PrimitiveType(PrimitiveKind.USIZE);
    }
    
    public static PrimitiveType getIsizeType() {
        return new PrimitiveType(PrimitiveKind.ISIZE);
    }
    
    public static PrimitiveType getBoolType() {
        return new PrimitiveType(PrimitiveKind.BOOL);
    }
    
    public static PrimitiveType getCharType() {
        return new PrimitiveType(PrimitiveKind.CHAR);
    }
    
    public static PrimitiveType getStrType() {
        return new PrimitiveType(PrimitiveKind.STR);
    }
}