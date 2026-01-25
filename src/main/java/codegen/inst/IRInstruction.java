package codegen.inst;

import codegen.value.IRValue;

/**
 * IR 指令的抽象基类
 * 所有 IR 指令都继承自此类
 */
public abstract class IRInstruction {
    protected IRValue result;  // 指令的结果值，可为 null（如 store, br）

    /**
     * 获取指令的结果值
     * @return 结果值，如果指令无结果则返回 null
     */
    public IRValue getResult() {
        return result;
    }

    /**
     * 检查指令是否有结果值
     * @return 是否有结果
     */
    public boolean hasResult() {
        return result != null;
    }

    /**
     * 获取指令的字符串表示
     * @return IR 文本表示
     */
    public abstract String toString();

    /**
     * 接受访问者
     * @param visitor IR 访问者
     */
    public abstract void accept(IRVisitor visitor);
}
