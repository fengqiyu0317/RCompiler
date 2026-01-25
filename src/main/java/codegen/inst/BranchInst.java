package codegen.inst;

import codegen.ir.IRBasicBlock;

/**
 * 无条件跳转指令
 * 形式: br label %target
 */
public class BranchInst extends IRInstruction {
    private final IRBasicBlock target;

    public BranchInst(IRBasicBlock target) {
        this.result = null;  // 跳转指令无结果
        this.target = target;
    }

    /**
     * 获取跳转目标基本块
     * @return 目标基本块
     */
    public IRBasicBlock getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return "br label %" + target.getName();
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
