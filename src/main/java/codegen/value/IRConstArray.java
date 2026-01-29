package codegen.value;

import codegen.type.IRArrayType;
import codegen.type.IRType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * IR 常量数组
 * 表示编译期已知的数组常量
 */
public class IRConstArray extends IRValue {
    private final List<IRValue> elements;

    public IRConstArray(IRArrayType type, List<IRValue> elements) {
        this.type = type;
        this.name = null;
        this.elements = new ArrayList<>(elements);
    }

    public List<IRValue> getElements() {
        return elements;
    }

    public IRArrayType getArrayType() {
        return (IRArrayType) type;
    }

    @Override
    public String toString() {
        if (elements.isEmpty()) {
            return "zeroinitializer";
        }
        String body = elements.stream()
            .map(IRConstArray::formatElement)
            .collect(Collectors.joining(", "));
        return "[" + body + "]";
    }

    public String toStringWithType() {
        return type + " " + toString();
    }

    private static String formatElement(IRValue value) {
        if (value instanceof IRConstArray) {
            return ((IRConstArray) value).toStringWithType();
        }
        if (value instanceof IRConstant) {
            return ((IRConstant) value).toStringWithType();
        }
        IRType valueType = value.getType();
        return valueType + " " + value;
    }
}
