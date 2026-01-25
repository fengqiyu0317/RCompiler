package codegen.inst;

import codegen.value.IRRegister;
import codegen.value.IRValue;

/**
 * 二元运算指令
 * 形式: %dst = <op> <type> %lhs, %rhs
 */
public class BinaryOpInst extends IRInstruction {

    /**
     * 二元运算操作符
     */
    public enum Op {
        ADD("add"),
        SUB("sub"),
        MUL("mul"),
        SDIV("sdiv"),   // 有符号除法
        UDIV("udiv"),   // 无符号除法
        SREM("srem"),   // 有符号取余
        UREM("urem"),   // 无符号取余
        AND("and"),
        OR("or"),
        XOR("xor"),
        SHL("shl"),     // 左移
        LSHR("lshr"),   // 逻辑右移（无符号）
        ASHR("ashr");   // 算术右移（有符号）

        private final String irName;

        Op(String irName) {
            this.irName = irName;
        }

        public String getIRName() {
            return irName;
        }
    }

    private final Op op;
    private final IRValue left;
    private final IRValue right;

    public BinaryOpInst(IRRegister result, Op op, IRValue left, IRValue right) {
        this.result = result;
        this.op = op;
        this.left = left;
        this.right = right;
    }

    public Op getOp() {
        return op;
    }

    public IRValue getLeft() {
        return left;
    }

    public IRValue getRight() {
        return right;
    }

    @Override
    public String toString() {
        return result + " = " + op.getIRName() + " " +
               left.getType() + " " + left + ", " + right;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
