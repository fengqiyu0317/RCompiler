package codegen.inst;

import codegen.ir.IRBasicBlock;
import codegen.value.IRRegister;
import codegen.value.IRValue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Phi 指令（SSA 形式）
 * 用于在控制流汇合点选择值
 * 形式: %dst = phi <type> [%val1, %label1], [%val2, %label2], ...
 */
public class PhiInst extends IRInstruction {
    private final List<IRValue> values;
    private final List<IRBasicBlock> blocks;

    public PhiInst(IRRegister result) {
        this.result = result;
        this.values = new ArrayList<>();
        this.blocks = new ArrayList<>();
    }

    /**
     * 添加一个来源
     * @param value 来自该基本块的值
     * @param block 来源基本块
     */
    public void addIncoming(IRValue value, IRBasicBlock block) {
        values.add(value);
        blocks.add(block);
    }

    /**
     * 获取来源值列表
     * @return 值列表
     */
    public List<IRValue> getValues() {
        return values;
    }

    /**
     * 获取来源基本块列表
     * @return 基本块列表
     */
    public List<IRBasicBlock> getBlocks() {
        return blocks;
    }

    /**
     * 获取来源数量
     * @return 来源个数
     */
    public int getNumIncoming() {
        return values.size();
    }

    /**
     * 获取指定索引的来源值
     */
    public IRValue getIncomingValue(int index) {
        return values.get(index);
    }

    /**
     * 获取指定索引的来源基本块
     */
    public IRBasicBlock getIncomingBlock(int index) {
        return blocks.get(index);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(result).append(" = phi ").append(result.getType());

        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(" [").append(values.get(i)).append(", %").append(blocks.get(i).getName()).append("]");
        }

        return sb.toString();
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
