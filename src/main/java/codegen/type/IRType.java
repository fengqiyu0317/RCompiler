package codegen.type;

/**
 * IR 类型的抽象基类
 * 所有 IR 类型都继承自此类
 */
public abstract class IRType {
    /**
     * 获取类型的字节大小
     * @return 字节数
     */
    public abstract int getSize();

    /**
     * 获取类型的对齐要求
     * @return 对齐字节数
     */
    public abstract int getAlign();

    /**
     * 获取类型的字符串表示
     * @return 类型的 IR 文本表示
     */
    public abstract String toString();

    /**
     * 检查两个类型是否相等
     * @param other 另一个类型
     * @return 是否相等
     */
    public abstract boolean equals(Object other);

    @Override
    public abstract int hashCode();
}
