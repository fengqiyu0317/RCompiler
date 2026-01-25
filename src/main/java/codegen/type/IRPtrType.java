package codegen.type;

import java.util.Objects;

/**
 * IR 指针类型
 * 表示指向某个类型的指针
 */
public class IRPtrType extends IRType {
    private final IRType pointee;

    public IRPtrType(IRType pointee) {
        this.pointee = pointee;
    }

    /**
     * 获取指针指向的类型
     * @return 被指向的类型
     */
    public IRType getPointee() {
        return pointee;
    }

    @Override
    public int getSize() {
        return 8;  // 64-bit pointer
    }

    @Override
    public int getAlign() {
        return 8;
    }

    @Override
    public String toString() {
        return pointee + "*";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof IRPtrType)) return false;
        return Objects.equals(pointee, ((IRPtrType) other).pointee);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pointee);
    }
}
