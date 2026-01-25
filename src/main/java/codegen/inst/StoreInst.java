package codegen.inst;

import codegen.value.IRValue;

/**
 * 存储指令
 * 将值写入地址
 * 形式: store <type> %val, <type>* %addr
 */
public class StoreInst extends IRInstruction {
    private final IRValue value;
    private final IRValue address;

    public StoreInst(IRValue value, IRValue address) {
        this.result = null;  // store 指令无结果
        this.value = value;
        this.address = address;
    }

    /**
     * 获取要存储的值
     * @return 被存储的值
     */
    public IRValue getValue() {
        return value;
    }

    /**
     * 获取存储的目标地址
     * @return 目标地址
     */
    public IRValue getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "store " + value.getType() + " " + value + ", " +
               address.getType() + " " + address;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
