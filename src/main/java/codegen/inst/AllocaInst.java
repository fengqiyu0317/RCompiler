package codegen.inst;

import codegen.type.IRType;
import codegen.value.IRRegister;

/**
 * 栈分配指令
 * 在当前栈帧分配指定类型的空间
 * 形式: %dst = alloca <type>
 */
public class AllocaInst extends IRInstruction {
    private final IRType allocType;

    public AllocaInst(IRRegister result, IRType allocType) {
        this.result = result;
        this.allocType = allocType;
    }

    /**
     * 获取分配的类型
     * @return 被分配空间的类型
     */
    public IRType getAllocType() {
        return allocType;
    }

    @Override
    public String toString() {
        return result + " = alloca " + allocType;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
