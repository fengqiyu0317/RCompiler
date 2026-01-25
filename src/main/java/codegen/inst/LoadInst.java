package codegen.inst;

import codegen.value.IRRegister;
import codegen.value.IRValue;

/**
 * 加载指令
 * 从地址读取值
 * 形式: %dst = load <type>, <type>* %addr
 */
public class LoadInst extends IRInstruction {
    private final IRValue address;

    public LoadInst(IRRegister result, IRValue address) {
        this.result = result;
        this.address = address;
    }

    /**
     * 获取加载的源地址
     * @return 地址值
     */
    public IRValue getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return result + " = load " + result.getType() + ", " +
               address.getType() + " " + address;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
