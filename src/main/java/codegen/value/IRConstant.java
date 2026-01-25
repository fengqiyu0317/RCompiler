package codegen.value;

import codegen.type.IRIntType;
import codegen.type.IRType;

/**
 * IR 常量值
 * 表示编译时已知的常量，如整数、布尔值等
 */
public class IRConstant extends IRValue {
    private final Object value;  // Long, Boolean, String, null

    public IRConstant(IRType type, Object value) {
        this.type = type;
        this.name = null;
        this.value = value;
    }

    /**
     * 获取常量的值
     * @return 常量值对象
     */
    public Object getValue() {
        return value;
    }

    /**
     * 获取常量的整数值
     * @return 整数值
     * @throws IllegalStateException 如果不是整数常量
     */
    public long getIntValue() {
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        throw new IllegalStateException("Not an integer constant: " + value);
    }

    /**
     * 获取常量的布尔值
     * @return 布尔值
     * @throws IllegalStateException 如果不是布尔常量
     */
    public boolean getBoolValue() {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        throw new IllegalStateException("Not a boolean constant: " + value);
    }

    @Override
    public String toString() {
        if (value instanceof Boolean) {
            return ((Boolean) value) ? "1" : "0";
        }
        return String.valueOf(value);
    }

    /**
     * 返回带类型的字符串表示
     * @return 如 "i32 42"
     */
    public String toStringWithType() {
        return type + " " + toString();
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建 i32 整数常量
     */
    public static IRConstant i32(long value) {
        return new IRConstant(IRIntType.I32, value);
    }

    /**
     * 创建 i64 整数常量
     */
    public static IRConstant i64(long value) {
        return new IRConstant(IRIntType.I64, value);
    }

    /**
     * 创建 i1 布尔常量
     */
    public static IRConstant i1(boolean value) {
        return new IRConstant(IRIntType.I1, value);
    }

    /**
     * 创建 i8 字符常量
     */
    public static IRConstant i8(int value) {
        return new IRConstant(IRIntType.I8, (long) value);
    }

    /**
     * 创建零值常量
     */
    public static IRConstant zero(IRType type) {
        if (type instanceof IRIntType) {
            return new IRConstant(type, 0L);
        }
        return new IRConstant(type, null);
    }
}
