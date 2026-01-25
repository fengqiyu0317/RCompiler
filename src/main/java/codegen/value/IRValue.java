package codegen.value;

import codegen.type.IRType;

/**
 * IR 值的抽象基类
 * 表示 IR 中的所有值（寄存器、常量、全局变量等）
 */
public abstract class IRValue {
    protected IRType type;
    protected String name;

    /**
     * 获取值的类型
     * @return IR 类型
     */
    public IRType getType() {
        return type;
    }

    /**
     * 获取值的名称
     * @return 名称字符串
     */
    public String getName() {
        return name;
    }

    /**
     * 获取值的字符串表示
     * @return IR 文本表示
     */
    public abstract String toString();
}
