package codegen;

import codegen.ir.IRBasicBlock;
import codegen.ir.IRFunction;
import codegen.ir.IRModule;
import codegen.inst.*;
import codegen.type.IRStructType;
import codegen.type.IRType;
import codegen.value.IRRegister;

import java.io.PrintStream;
import java.util.stream.Collectors;

/**
 * IR 打印器
 * 将 IR 模块输出为可读的文本格式
 */
public class IRPrinter {
    private final PrintStream out;
    private int indentLevel = 0;

    public IRPrinter() {
        this(System.out);
    }

    public IRPrinter(PrintStream out) {
        this.out = out;
    }

    /**
     * 打印整个 IR 模块
     * @param module IR 模块
     */
    public void print(IRModule module) {
        // 打印结构体类型定义
        for (IRStructType struct : module.getStructs()) {
            printStructType(struct);
        }
        if (module.getStructCount() > 0) {
            out.println();
        }

        // 打印全局变量
        for (var global : module.getGlobals()) {
            out.println(global + " = global " + global.getType());
        }
        if (module.getGlobalCount() > 0) {
            out.println();
        }

        // 打印函数
        boolean first = true;
        for (IRFunction func : module.getFunctions()) {
            if (!first) {
                out.println();
            }
            printFunction(func);
            first = false;
        }
    }

    /**
     * 打印结构体类型定义
     */
    private void printStructType(IRStructType struct) {
        StringBuilder sb = new StringBuilder();
        sb.append("%").append(struct.getName()).append(" = type { ");

        String fields = struct.getFields().stream()
            .map(IRType::toString)
            .collect(Collectors.joining(", "));
        sb.append(fields);

        sb.append(" }");
        out.println(sb);
    }

    /**
     * 打印函数
     */
    public void printFunction(IRFunction func) {
        // 函数签名
        StringBuilder sig = new StringBuilder();
        sig.append("define ").append(func.getReturnType()).append(" @").append(func.getName()).append("(");

        String params = func.getParams().stream()
            .map(p -> p.getType() + " " + p)
            .collect(Collectors.joining(", "));
        sig.append(params).append(") {");

        out.println(sig);

        // 基本块
        for (IRBasicBlock block : func.getBlocks()) {
            printBasicBlock(block);
        }

        out.println("}");
    }

    /**
     * 打印基本块
     */
    private void printBasicBlock(IRBasicBlock block) {
        out.println(block.getName() + ":");

        // 普通指令
        for (IRInstruction inst : block.getInstructions()) {
            out.println("    " + inst);
        }

        // 终结指令
        if (block.getTerminator() != null) {
            out.println("    " + block.getTerminator());
        }
    }

    /**
     * 将 IR 模块转换为字符串
     * @param module IR 模块
     * @return IR 文本
     */
    public static String toString(IRModule module) {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        new IRPrinter(ps).print(module);
        return baos.toString();
    }
}
