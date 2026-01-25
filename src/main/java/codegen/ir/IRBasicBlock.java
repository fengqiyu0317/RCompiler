package codegen.ir;

import codegen.inst.*;

import java.util.ArrayList;
import java.util.List;

/**
 * IR 基本块
 * 包含一系列指令，以终结指令（br/ret）结束
 */
public class IRBasicBlock {
    private final String name;
    private final List<IRInstruction> instructions;
    private IRInstruction terminator;  // br/condbr/ret

    public IRBasicBlock(String name) {
        this.name = name;
        this.instructions = new ArrayList<>();
        this.terminator = null;
    }

    /**
     * 获取基本块名称
     * @return 名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取指令列表（不包括终结指令）
     * @return 指令列表
     */
    public List<IRInstruction> getInstructions() {
        return instructions;
    }

    /**
     * 获取终结指令
     * @return 终结指令（br/condbr/ret）
     */
    public IRInstruction getTerminator() {
        return terminator;
    }

    /**
     * 添加指令到基本块
     * 如果是终结指令，则设置为 terminator
     * @param inst 要添加的指令
     */
    public void addInstruction(IRInstruction inst) {
        if (isTerminatorInst(inst)) {
            if (terminator != null) {
                throw new IllegalStateException("Basic block already has a terminator: " + name);
            }
            terminator = inst;
        } else {
            if (terminator != null) {
                throw new IllegalStateException("Cannot add instruction after terminator in block: " + name);
            }
            instructions.add(inst);
        }
    }

    /**
     * 检查指令是否是终结指令
     */
    private boolean isTerminatorInst(IRInstruction inst) {
        return inst instanceof BranchInst ||
               inst instanceof CondBranchInst ||
               inst instanceof ReturnInst;
    }

    /**
     * 检查基本块是否已终结
     * @return 是否有终结指令
     */
    public boolean isTerminated() {
        return terminator != null;
    }

    /**
     * 获取所有指令（包括终结指令）
     * @return 所有指令的列表
     */
    public List<IRInstruction> getAllInstructions() {
        List<IRInstruction> all = new ArrayList<>(instructions);
        if (terminator != null) {
            all.add(terminator);
        }
        return all;
    }

    /**
     * 获取指令数量（包括终结指令）
     * @return 指令总数
     */
    public int getInstructionCount() {
        return instructions.size() + (terminator != null ? 1 : 0);
    }

    @Override
    public String toString() {
        return "%" + name;
    }
}
