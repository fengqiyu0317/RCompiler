package codegen.type;

/**
 * IR 整数类型
 * 支持 i1 (bool), i8, i32, i64 等
 */
public class IRIntType extends IRType {
    public static final IRIntType I1 = new IRIntType(1);
    public static final IRIntType I8 = new IRIntType(8);
    public static final IRIntType I32 = new IRIntType(32);
    public static final IRIntType I64 = new IRIntType(64);

    private final int bits;

    private IRIntType(int bits) {
        this.bits = bits;
    }

    /**
     * 获取指定位数的整数类型
     * @param bits 位数
     * @return 对应的 IRIntType
     */
    public static IRIntType get(int bits) {
        switch (bits) {
            case 1: return I1;
            case 8: return I8;
            case 32: return I32;
            case 64: return I64;
            default: return new IRIntType(bits);
        }
    }

    public int getBits() {
        return bits;
    }

    @Override
    public int getSize() {
        return (bits + 7) / 8;
    }

    @Override
    public int getAlign() {
        return getSize();
    }

    @Override
    public String toString() {
        return "i" + bits;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof IRIntType)) return false;
        return bits == ((IRIntType) other).bits;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(bits);
    }
}
