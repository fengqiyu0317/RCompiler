package codegen.inst;

import codegen.type.IRType;
import codegen.value.IRRegister;
import codegen.value.IRValue;

/**
 * 类型转换指令
 * 形式: %dst = <cast-op> <src-type> %val to <dst-type>
 */
public class CastInst extends IRInstruction {

    /**
     * 类型转换操作
     */
    public enum CastOp {
        TRUNC("trunc"),       // 截断（大整数到小整数）
        ZEXT("zext"),         // 零扩展（无符号扩展）
        SEXT("sext"),         // 符号扩展（有符号扩展）
        PTRTOINT("ptrtoint"), // 指针转整数
        INTTOPTR("inttoptr"), // 整数转指针
        BITCAST("bitcast");   // 位转换（相同大小的类型）

        private final String irName;

        CastOp(String irName) {
            this.irName = irName;
        }

        public String getIRName() {
            return irName;
        }
    }

    private final CastOp op;
    private final IRValue operand;
    private final IRType targetType;

    public CastInst(IRRegister result, CastOp op, IRValue operand, IRType targetType) {
        this.result = result;
        this.op = op;
        this.operand = operand;
        this.targetType = targetType;
    }

    public CastOp getOp() {
        return op;
    }

    public IRValue getOperand() {
        return operand;
    }

    public IRType getTargetType() {
        return targetType;
    }

    @Override
    public String toString() {
        return result + " = " + op.getIRName() + " " +
               operand.getType() + " " + operand + " to " + targetType;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
