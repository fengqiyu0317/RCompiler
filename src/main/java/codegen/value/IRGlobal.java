package codegen.value;

import codegen.type.IRPtrType;
import codegen.type.IRType;

/**
 * IR 全局值
 * 表示全局变量或函数，以 @ 为前缀
 */
public class IRGlobal extends IRValue {
    private final IRType valueType;
    private String stringInitializer;  // 字符串初始值（用于字符串常量）
    private IRValue initializer;       // 通用初始化值（数组常量等）

    public IRGlobal(IRType type, String name) {
        this.valueType = type;
        this.type = new IRPtrType(type);
        this.name = name;
        this.stringInitializer = null;
        this.initializer = null;
    }

    public IRGlobal(IRType type, String name, String stringInitializer) {
        this.valueType = type;
        this.type = new IRPtrType(type);
        this.name = name;
        this.stringInitializer = stringInitializer;
        this.initializer = null;
    }

    public IRGlobal(IRType type, String name, IRValue initializer) {
        this.valueType = type;
        this.type = new IRPtrType(type);
        this.name = name;
        this.stringInitializer = null;
        this.initializer = initializer;
    }

    public String getStringInitializer() {
        return stringInitializer;
    }

    public void setStringInitializer(String stringInitializer) {
        this.stringInitializer = stringInitializer;
    }

    public IRValue getInitializer() {
        return initializer;
    }

    public IRType getValueType() {
        return valueType;
    }

    public void setInitializer(IRValue initializer) {
        this.initializer = initializer;
    }

    @Override
    public String toString() {
        return "@" + name;
    }

    /**
     * 生成全局变量定义
     */
    public String toDefinition() {
        if (stringInitializer != null) {
            return "@" + name + " = global " + valueType + " c\"" + escapeString(stringInitializer) + "\\00\"";
        }
        if (initializer != null) {
            return "@" + name + " = constant " + valueType + " " + formatInitializer(initializer);
        }
        return "@" + name + " = global " + valueType;
    }

    private String formatInitializer(IRValue value) {
        if (value instanceof IRConstArray) {
            return ((IRConstArray) value).toString();
        }
        if (value instanceof IRConstant) {
            return ((IRConstant) value).toString();
        }
        return value.toString();
    }

    /**
     * 转义字符串中的特殊字符
     */
    private String escapeString(String str) {
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '"') {
                sb.append("\\\"");
            } else if (c == '\n') {
                sb.append("\\0A");
            } else if (c == '\r') {
                sb.append("\\0D");
            } else if (c == '\t') {
                sb.append("\\09");
            } else if (c < 32 || c > 126) {
                // 非打印字符用十六进制表示
                sb.append(String.format("\\%02X", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
