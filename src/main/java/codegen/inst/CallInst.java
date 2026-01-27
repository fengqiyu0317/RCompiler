package codegen.inst;

import codegen.type.IRType;
import codegen.type.IRVoidType;
import codegen.value.IRRegister;
import codegen.value.IRValue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 函数调用指令
 * 直接调用形式: %dst = call <ret-type> @func(<args...>)
 * 间接调用形式: %dst = call <ret-type> %fptr(<args...>)
 */
public class CallInst extends IRInstruction {
    private final String functionName;  // 直接调用时的函数名（以 @ 开头）
    private final IRValue functionPtr;  // 间接调用时的函数指针
    private final List<IRValue> args;
    private final IRType returnType;

    /**
     * 创建直接调用指令（有返回值）
     */
    public CallInst(IRRegister result, String functionName, List<IRValue> args) {
        this.result = result;
        this.functionName = functionName;
        this.functionPtr = null;
        this.args = new ArrayList<>(args);
        this.returnType = result.getType();
    }

    /**
     * 创建直接调用指令（无返回值）
     */
    public CallInst(String functionName, List<IRValue> args) {
        this.result = null;
        this.functionName = functionName;
        this.functionPtr = null;
        this.args = new ArrayList<>(args);
        this.returnType = IRVoidType.INSTANCE;
    }

    /**
     * 创建间接调用指令（有返回值，通过函数指针）
     */
    public CallInst(IRRegister result, IRValue functionPtr, List<IRValue> args) {
        this.result = result;
        this.functionName = null;
        this.functionPtr = functionPtr;
        this.args = new ArrayList<>(args);
        this.returnType = result.getType();
    }

    /**
     * 创建间接调用指令（无返回值，通过函数指针）
     */
    public CallInst(IRValue functionPtr, List<IRValue> args) {
        this.result = null;
        this.functionName = null;
        this.functionPtr = functionPtr;
        this.args = new ArrayList<>(args);
        this.returnType = IRVoidType.INSTANCE;
    }

    public boolean isDirect() {
        return functionName != null;
    }

    public String getFunctionName() {
        return functionName;
    }

    public IRValue getFunctionPtr() {
        return functionPtr;
    }

    public List<IRValue> getArgs() {
        return args;
    }

    public IRType getReturnType() {
        return returnType;
    }

    @Override
    public String toString() {
        String argStr = args.stream()
            .map(a -> a.getType() + " " + a)
            .collect(Collectors.joining(", "));

        String callee = isDirect() ? "@" + functionName : functionPtr.toString();

        if (result != null) {
            return result + " = call " + returnType + " " + callee + "(" + argStr + ")";
        }
        return "call void " + callee + "(" + argStr + ")";
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
