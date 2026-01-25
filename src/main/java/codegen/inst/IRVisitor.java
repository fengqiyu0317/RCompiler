package codegen.inst;

/**
 * IR 指令访问者接口
 * 用于遍历和处理 IR 指令
 */
public interface IRVisitor {
    void visit(AllocaInst inst);
    void visit(LoadInst inst);
    void visit(StoreInst inst);
    void visit(BinaryOpInst inst);
    void visit(UnaryOpInst inst);
    void visit(CmpInst inst);
    void visit(CallInst inst);
    void visit(BranchInst inst);
    void visit(CondBranchInst inst);
    void visit(ReturnInst inst);
    void visit(GEPInst inst);
    void visit(CastInst inst);
    void visit(PhiInst inst);
}
