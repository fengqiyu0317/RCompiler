package codegen.value;

import codegen.type.IRType;

/**
 * IR 全局值
 * 表示全局变量或函数，以 @ 为前缀
 */
public class IRGlobal extends IRValue {

    public IRGlobal(IRType type, String name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public String toString() {
        return "@" + name;
    }
}
