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
 * 形式: %dst = call <ret-type> @func(<args...>)
 *       call void @func(<args...>)
 */
public class CallInst extends IRInstruction {
    private final IRValue function;
    private final List<IRValue> args;
    private final IRType returnType;

    /**
     * 创建有返回值的调用指令
     */
    public CallInst(IRRegister result, IRValue function, List<IRValue> args) {
        this.result = result;
        this.function = function;
        this.args = new ArrayList<>(args);
        this.returnType = result.getType();
    }

    /**
     * 创建无返回值的调用指令
     */
    public CallInst(IRValue function, List<IRValue> args) {
        this.result = null;
        this.function = function;
        this.args = new ArrayList<>(args);
        this.returnType = IRVoidType.INSTANCE;
    }

    public IRValue getFunction() {
        return function;
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

        if (result != null) {
            return result + " = call " + returnType + " " + function + "(" + argStr + ")";
        }
        return "call void " + function + "(" + argStr + ")";
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
