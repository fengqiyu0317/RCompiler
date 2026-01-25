package codegen.value;

import codegen.type.IRType;

/**
 * IR 寄存器（临时变量）
 * 表示 IR 中的临时值，如 %t0, %t1, %x.0 等
 */
public class IRRegister extends IRValue {
    private static int counter = 0;

    public IRRegister(IRType type) {
        this.type = type;
        this.name = "t" + (counter++);
    }

    public IRRegister(IRType type, String hint) {
        this.type = type;
        this.name = hint + "." + (counter++);
    }

    /**
     * 重置计数器（用于新函数开始时）
     */
    public static void resetCounter() {
        counter = 0;
    }

    /**
     * 获取当前计数器值
     * @return 当前计数
     */
    public static int getCounter() {
        return counter;
    }

    /**
     * 设置计数器值（用于恢复嵌套函数处理后的状态）
     * @param value 要设置的计数值
     */
    public static void setCounter(int value) {
        counter = value;
    }

    @Override
    public String toString() {
        return "%" + name;
    }
}
