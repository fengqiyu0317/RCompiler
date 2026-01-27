package codegen.value;

import codegen.type.IRType;

/**
 * IR 全局值
 * 表示全局变量或函数，以 @ 为前缀
 */
public class IRGlobal extends IRValue {
    private String stringInitializer;  // 字符串初始值（用于字符串常量）

    public IRGlobal(IRType type, String name) {
        this.type = type;
        this.name = name;
        this.stringInitializer = null;
    }

    public IRGlobal(IRType type, String name, String stringInitializer) {
        this.type = type;
        this.name = name;
        this.stringInitializer = stringInitializer;
    }

    public String getStringInitializer() {
        return stringInitializer;
    }

    public void setStringInitializer(String stringInitializer) {
        this.stringInitializer = stringInitializer;
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
            return "@" + name + " = global " + type + " c\"" + escapeString(stringInitializer) + "\\00\"";
        }
        return "@" + name + " = global " + type;
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
