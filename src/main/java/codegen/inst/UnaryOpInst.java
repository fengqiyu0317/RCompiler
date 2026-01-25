package codegen.inst;

import codegen.value.IRRegister;
import codegen.value.IRValue;

/**
 * 一元运算指令
 * 形式: %dst = <op> <type> %val
 */
public class UnaryOpInst extends IRInstruction {

    /**
     * 一元运算操作符
     */
    public enum Op {
        NEG("neg"),     // 算术取反 (-x)
        NOT("not");     // 按位取反 (!x 或 ~x)

        private final String irName;

        Op(String irName) {
            this.irName = irName;
        }

        public String getIRName() {
            return irName;
        }
    }

    private final Op op;
    private final IRValue operand;

    public UnaryOpInst(IRRegister result, Op op, IRValue operand) {
        this.result = result;
        this.op = op;
        this.operand = operand;
    }

    public Op getOp() {
        return op;
    }

    public IRValue getOperand() {
        return operand;
    }

    @Override
    public String toString() {
        return result + " = " + op.getIRName() + " " +
               operand.getType() + " " + operand;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
