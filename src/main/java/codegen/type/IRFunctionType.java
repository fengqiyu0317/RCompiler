package codegen.type;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * IR 函数类型
 * 表示函数的签名，包括返回类型和参数类型列表
 */
public class IRFunctionType extends IRType {
    private final IRType returnType;
    private final List<IRType> paramTypes;

    public IRFunctionType(IRType returnType, List<IRType> paramTypes) {
        this.returnType = returnType;
        this.paramTypes = new ArrayList<>(paramTypes);
    }

    /**
     * 获取返回类型
     * @return 返回类型
     */
    public IRType getReturnType() {
        return returnType;
    }

    /**
     * 获取参数类型列表
     * @return 参数类型列表
     */
    public List<IRType> getParamTypes() {
        return paramTypes;
    }

    /**
     * 获取参数数量
     * @return 参数数量
     */
    public int getParamCount() {
        return paramTypes.size();
    }

    /**
     * 获取指定位置的参数类型
     * @param index 参数索引
     * @return 参数类型
     */
    public IRType getParamType(int index) {
        return paramTypes.get(index);
    }

    @Override
    public int getSize() {
        // 函数类型本身没有大小，函数指针的大小在 IRPtrType 中处理
        return 0;
    }

    @Override
    public int getAlign() {
        return 1;
    }

    @Override
    public String toString() {
        String params = paramTypes.stream()
            .map(IRType::toString)
            .collect(Collectors.joining(", "));
        return returnType + " (" + params + ")";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof IRFunctionType)) return false;
        IRFunctionType that = (IRFunctionType) other;
        return Objects.equals(returnType, that.returnType) &&
               Objects.equals(paramTypes, that.paramTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(returnType, paramTypes);
    }
}
