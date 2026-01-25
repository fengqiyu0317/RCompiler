package codegen.inst;

import codegen.value.IRValue;

/**
 * 返回指令
 * 形式: ret <type> %val
 *       ret void
 */
public class ReturnInst extends IRInstruction {
    private final IRValue value;  // null for void return

    /**
     * 创建有返回值的返回指令
     */
    public ReturnInst(IRValue value) {
        this.result = null;  // ret 指令本身无结果
        this.value = value;
    }

    /**
     * 创建无返回值的返回指令（void）
     */
    public ReturnInst() {
        this.result = null;
        this.value = null;
    }

    /**
     * 获取返回值
     * @return 返回值，如果是 void 返回则为 null
     */
    public IRValue getValue() {
        return value;
    }

    /**
     * 检查是否是 void 返回
     * @return 是否无返回值
     */
    public boolean isVoidReturn() {
        return value == null;
    }

    @Override
    public String toString() {
        if (value == null) {
            return "ret void";
        }
        return "ret " + value.getType() + " " + value;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
