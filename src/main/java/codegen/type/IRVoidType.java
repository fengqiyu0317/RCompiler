package codegen.type;

/**
 * IR void 类型
 * 用于无返回值的函数
 */
public class IRVoidType extends IRType {
    public static final IRVoidType INSTANCE = new IRVoidType();

    private IRVoidType() {}

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public int getAlign() {
        return 1;
    }

    @Override
    public String toString() {
        return "void";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof IRVoidType;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
