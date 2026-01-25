package codegen.inst;

import codegen.value.IRRegister;
import codegen.value.IRValue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 获取元素指针指令（GetElementPtr）
 * 用于计算结构体字段或数组元素的地址
 * 形式: %dst = getelementptr <type>, <type>* %base, <indices...>
 */
public class GEPInst extends IRInstruction {
    private final IRValue base;
    private final List<IRValue> indices;

    public GEPInst(IRRegister result, IRValue base, List<IRValue> indices) {
        this.result = result;
        this.base = base;
        this.indices = new ArrayList<>(indices);
    }

    /**
     * 创建单索引的 GEP 指令
     */
    public GEPInst(IRRegister result, IRValue base, IRValue index) {
        this.result = result;
        this.base = base;
        this.indices = new ArrayList<>();
        this.indices.add(index);
    }

    /**
     * 创建双索引的 GEP 指令（常用于结构体字段访问）
     */
    public GEPInst(IRRegister result, IRValue base, IRValue index1, IRValue index2) {
        this.result = result;
        this.base = base;
        this.indices = new ArrayList<>();
        this.indices.add(index1);
        this.indices.add(index2);
    }

    /**
     * 获取基地址
     * @return 基地址值
     */
    public IRValue getBase() {
        return base;
    }

    /**
     * 获取索引列表
     * @return 索引列表
     */
    public List<IRValue> getIndices() {
        return indices;
    }

    @Override
    public String toString() {
        String idxStr = indices.stream()
            .map(i -> i.getType() + " " + i)
            .collect(Collectors.joining(", "));
        return result + " = getelementptr " + base.getType() + ", " +
               base.getType() + " " + base + ", " + idxStr;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
