// Primitive type implementation

public class PrimitiveType implements Type {
    public enum PrimitiveKind {
        INT,     // Undetermined integer (for type inference)
        I32, U32, USIZE, ISIZE, BOOL, CHAR, STR, STRING
    }
    
    private final PrimitiveKind kind;
    private boolean isMutable;
    private Long value; // 存储int类型的值，用于溢出检测
    
    public PrimitiveType(PrimitiveKind kind) {
        this(kind, false);
    }
    
    public PrimitiveType(PrimitiveKind kind, boolean isMutable) {
        this.kind = kind;
        this.isMutable = isMutable;
        this.value = null;
    }
    
    // 添加构造函数重载以支持值记录
    public PrimitiveType(PrimitiveKind kind, Long value) {
        this(kind, false, value);
    }
    
    public PrimitiveType(PrimitiveKind kind, boolean isMutable, Long value) {
        this.kind = kind;
        this.isMutable = isMutable;
        this.value = value;
    }
    
    public PrimitiveKind getKind() {
        return kind;
    }
    
    // 获取值
    public Long getValue() {
        return value;
    }
    
    // 设置值
    public void setValue(Long value) {
        this.value = value;
    }
    
    // 检查是否有值
    public boolean hasValue() {
        return value != null;
    }
    
    @Override
    public void setMutability(boolean isMutable) {
        this.isMutable = isMutable;
    }
    
    @Override
    public boolean equals(Type other) {
        // PrimitiveType 不等于 UnderscoreType
        if (other instanceof UnderscoreType) {
            return false;
        }
        
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
            case STRING: return "String";
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
    
    @Override
    public boolean isMutable() {
        return isMutable;
    }
    
    // Factory methods for common primitive types
    public static PrimitiveType getIntType() {
        return new PrimitiveType(PrimitiveKind.INT);
    }
    
    public static PrimitiveType getIntType(boolean isMutable) {
        return new PrimitiveType(PrimitiveKind.INT, isMutable);
    }
    
    // 添加支持值的工厂方法
    public static PrimitiveType getIntType(long value) {
        return new PrimitiveType(PrimitiveKind.INT, value);
    }
    
    public static PrimitiveType getIntType(boolean isMutable, long value) {
        return new PrimitiveType(PrimitiveKind.INT, isMutable, value);
    }
    
    public static PrimitiveType getI32Type(long value) {
        return new PrimitiveType(PrimitiveKind.I32, value);
    }
    
    public static PrimitiveType getI32Type(boolean isMutable, long value) {
        return new PrimitiveType(PrimitiveKind.I32, isMutable, value);
    }
    
    public static PrimitiveType getU32Type(long value) {
        return new PrimitiveType(PrimitiveKind.U32, value);
    }
    
    public static PrimitiveType getU32Type(boolean isMutable, long value) {
        return new PrimitiveType(PrimitiveKind.U32, isMutable, value);
    }
    
    public static PrimitiveType getUsizeType(long value) {
        return new PrimitiveType(PrimitiveKind.USIZE, value);
    }
    
    public static PrimitiveType getUsizeType(boolean isMutable, long value) {
        return new PrimitiveType(PrimitiveKind.USIZE, isMutable, value);
    }
    
    public static PrimitiveType getIsizeType(long value) {
        return new PrimitiveType(PrimitiveKind.ISIZE, value);
    }
    
    public static PrimitiveType getIsizeType(boolean isMutable, long value) {
        return new PrimitiveType(PrimitiveKind.ISIZE, isMutable, value);
    }
    
    public static PrimitiveType getI32Type() {
        return new PrimitiveType(PrimitiveKind.I32);
    }
    
    public static PrimitiveType getI32Type(boolean isMutable) {
        return new PrimitiveType(PrimitiveKind.I32, isMutable);
    }
    
    public static PrimitiveType getU32Type() {
        return new PrimitiveType(PrimitiveKind.U32);
    }
    
    public static PrimitiveType getU32Type(boolean isMutable) {
        return new PrimitiveType(PrimitiveKind.U32, isMutable);
    }
    
    public static PrimitiveType getUsizeType() {
        return new PrimitiveType(PrimitiveKind.USIZE);
    }
    
    public static PrimitiveType getUsizeType(boolean isMutable) {
        return new PrimitiveType(PrimitiveKind.USIZE, isMutable);
    }
    
    public static PrimitiveType getIsizeType() {
        return new PrimitiveType(PrimitiveKind.ISIZE);
    }
    
    public static PrimitiveType getIsizeType(boolean isMutable) {
        return new PrimitiveType(PrimitiveKind.ISIZE, isMutable);
    }
    
    public static PrimitiveType getBoolType() {
        return new PrimitiveType(PrimitiveKind.BOOL);
    }
    
    public static PrimitiveType getBoolType(boolean isMutable) {
        return new PrimitiveType(PrimitiveKind.BOOL, isMutable);
    }
    
    public static PrimitiveType getCharType() {
        return new PrimitiveType(PrimitiveKind.CHAR);
    }
    
    public static PrimitiveType getCharType(boolean isMutable) {
        return new PrimitiveType(PrimitiveKind.CHAR, isMutable);
    }
    
    public static PrimitiveType getStrType() {
        return new PrimitiveType(PrimitiveKind.STR);
    }
    
    public static PrimitiveType getStrType(boolean isMutable) {
        return new PrimitiveType(PrimitiveKind.STR, isMutable);
    }
    
    public static PrimitiveType getStringType() {
        return new PrimitiveType(PrimitiveKind.STRING);
    }
    
    public static PrimitiveType getStringType(boolean isMutable) {
        return new PrimitiveType(PrimitiveKind.STRING, isMutable);
    }
    
}