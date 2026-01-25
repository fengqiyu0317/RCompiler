package codegen.inst;

import codegen.value.IRRegister;
import codegen.value.IRValue;

/**
 * 比较指令
 * 形式: %dst = icmp <pred> <type> %lhs, %rhs
 */
public class CmpInst extends IRInstruction {

    /**
     * 比较谓词
     */
    public enum Pred {
        EQ("eq"),       // 等于
        NE("ne"),       // 不等于
        SLT("slt"),     // 有符号小于
        SLE("sle"),     // 有符号小于等于
        SGT("sgt"),     // 有符号大于
        SGE("sge"),     // 有符号大于等于
        ULT("ult"),     // 无符号小于
        ULE("ule"),     // 无符号小于等于
        UGT("ugt"),     // 无符号大于
        UGE("uge");     // 无符号大于等于

        private final String irName;

        Pred(String irName) {
            this.irName = irName;
        }

        public String getIRName() {
            return irName;
        }
    }

    private final Pred pred;
    private final IRValue left;
    private final IRValue right;

    public CmpInst(IRRegister result, Pred pred, IRValue left, IRValue right) {
        this.result = result;
        this.pred = pred;
        this.left = left;
        this.right = right;
    }

    public Pred getPred() {
        return pred;
    }

    public IRValue getLeft() {
        return left;
    }

    public IRValue getRight() {
        return right;
    }

    @Override
    public String toString() {
        return result + " = icmp " + pred.getIRName() + " " +
               left.getType() + " " + left + ", " + right;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
