package codegen.ir;

import codegen.type.IRType;
import codegen.value.IRRegister;
import codegen.value.IRValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * IR 函数
 * 包含参数、返回类型、基本块列表和局部变量映射
 */
public class IRFunction {
    private final String name;
    private final IRType returnType;
    private final List<IRRegister> params;
    private final List<IRBasicBlock> blocks;
    private final Map<String, IRValue> locals;  // 变量名 -> 地址

    public IRFunction(String name, IRType returnType) {
        this.name = name;
        this.returnType = returnType;
        this.params = new ArrayList<>();
        this.blocks = new ArrayList<>();
        this.locals = new HashMap<>();
    }

    // ==================== 基本信息 ====================

    public String getName() {
        return name;
    }

    public IRType getReturnType() {
        return returnType;
    }

    // ==================== 参数管理 ====================

    /**
     * 添加参数
     * @param param 参数寄存器
     */
    public void addParam(IRRegister param) {
        params.add(param);
    }

    public List<IRRegister> getParams() {
        return params;
    }

    public int getParamCount() {
        return params.size();
    }

    public IRRegister getParam(int index) {
        return params.get(index);
    }

    // ==================== 基本块管理 ====================

    /**
     * 添加基本块
     * @param block 基本块
     */
    public void addBlock(IRBasicBlock block) {
        blocks.add(block);
    }

    public List<IRBasicBlock> getBlocks() {
        return blocks;
    }

    /**
     * 获取入口基本块
     * @return 第一个基本块，如果没有则返回 null
     */
    public IRBasicBlock getEntryBlock() {
        return blocks.isEmpty() ? null : blocks.get(0);
    }

    /**
     * 根据名称查找基本块
     * @param name 基本块名称
     * @return 找到的基本块，如果不存在则返回 null
     */
    public IRBasicBlock getBlock(String name) {
        for (IRBasicBlock block : blocks) {
            if (block.getName().equals(name)) {
                return block;
            }
        }
        return null;
    }

    public int getBlockCount() {
        return blocks.size();
    }

    // ==================== 局部变量管理 ====================

    /**
     * 映射局部变量到其地址
     * @param name 变量名
     * @param addr 变量地址（alloca 的结果）
     */
    public void mapLocal(String name, IRValue addr) {
        locals.put(name, addr);
    }

    /**
     * 获取局部变量的地址
     * @param name 变量名
     * @return 变量地址，如果不存在则返回 null
     */
    public IRValue getLocal(String name) {
        return locals.get(name);
    }

    /**
     * 检查是否存在指定的局部变量
     * @param name 变量名
     * @return 是否存在
     */
    public boolean hasLocal(String name) {
        return locals.containsKey(name);
    }

    // ==================== 工具方法 ====================

    /**
     * 获取函数的总指令数
     * @return 所有基本块的指令总数
     */
    public int getTotalInstructionCount() {
        int count = 0;
        for (IRBasicBlock block : blocks) {
            count += block.getInstructionCount();
        }
        return count;
    }

    @Override
    public String toString() {
        return "@" + name;
    }
}
