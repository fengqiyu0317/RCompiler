package codegen.inst;

import codegen.ir.IRBasicBlock;
import codegen.value.IRValue;

/**
 * 条件跳转指令
 * 形式: br i1 %cond, label %then, label %else
 */
public class CondBranchInst extends IRInstruction {
    private final IRValue condition;
    private final IRBasicBlock thenBlock;
    private final IRBasicBlock elseBlock;

    public CondBranchInst(IRValue condition, IRBasicBlock thenBlock, IRBasicBlock elseBlock) {
        this.result = null;  // 跳转指令无结果
        this.condition = condition;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }

    /**
     * 获取条件值
     * @return 条件值（i1 类型）
     */
    public IRValue getCondition() {
        return condition;
    }

    /**
     * 获取条件为真时的目标基本块
     * @return then 分支目标
     */
    public IRBasicBlock getThenBlock() {
        return thenBlock;
    }

    /**
     * 获取条件为假时的目标基本块
     * @return else 分支目标
     */
    public IRBasicBlock getElseBlock() {
        return elseBlock;
    }

    @Override
    public String toString() {
        return "br i1 " + condition + ", label %" + thenBlock.getName() +
               ", label %" + elseBlock.getName();
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
