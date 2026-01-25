package codegen.type;

import java.util.Objects;

/**
 * IR 数组类型
 * 表示固定长度的数组
 */
public class IRArrayType extends IRType {
    private final IRType elementType;
    private final int length;

    public IRArrayType(IRType elementType, int length) {
        this.elementType = elementType;
        this.length = length;
    }

    /**
     * 获取数组元素类型
     * @return 元素类型
     */
    public IRType getElementType() {
        return elementType;
    }

    /**
     * 获取数组长度
     * @return 数组元素个数
     */
    public int getLength() {
        return length;
    }

    @Override
    public int getSize() {
        return elementType.getSize() * length;
    }

    @Override
    public int getAlign() {
        return elementType.getAlign();
    }

    @Override
    public String toString() {
        return "[" + length + " x " + elementType + "]";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof IRArrayType)) return false;
        IRArrayType that = (IRArrayType) other;
        return length == that.length && Objects.equals(elementType, that.elementType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementType, length);
    }
}
