package codegen.ir;

import codegen.type.IRStructType;
import codegen.type.IRType;
import codegen.value.IRGlobal;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * IR 模块
 * 表示一个编译单元，包含函数、全局变量和类型定义
 */
public class IRModule {
    private final Map<String, IRFunction> functions;
    private final Map<String, IRGlobal> globals;
    private final Map<String, IRStructType> structs;
    private final Map<String, EnumInfo> enums;

    /**
     * 枚举信息
     */
    public static class EnumInfo {
        private final String name;
        private final IRType underlyingType;
        private final Map<String, Integer> variantValues;

        public EnumInfo(String name, IRType underlyingType, Map<String, Integer> variantValues) {
            this.name = name;
            this.underlyingType = underlyingType;
            this.variantValues = new LinkedHashMap<>(variantValues);
        }

        public String getName() {
            return name;
        }

        public IRType getUnderlyingType() {
            return underlyingType;
        }

        public Map<String, Integer> getVariantValues() {
            return variantValues;
        }

        public Integer getVariantValue(String variantName) {
            return variantValues.get(variantName);
        }
    }

    public IRModule() {
        // 使用 LinkedHashMap 保持插入顺序
        this.functions = new LinkedHashMap<>();
        this.globals = new LinkedHashMap<>();
        this.structs = new LinkedHashMap<>();
        this.enums = new LinkedHashMap<>();
    }

    // ==================== 函数管理 ====================

    /**
     * 添加函数
     * @param func 函数
     */
    public void addFunction(IRFunction func) {
        functions.put(func.getName(), func);
    }

    /**
     * 获取指定名称的函数
     * @param name 函数名
     * @return 函数对象，如果不存在则返回 null
     */
    public IRFunction getFunction(String name) {
        return functions.get(name);
    }

    /**
     * 检查是否存在指定的函数
     * @param name 函数名
     * @return 是否存在
     */
    public boolean hasFunction(String name) {
        return functions.containsKey(name);
    }

    /**
     * 获取所有函数
     * @return 函数集合
     */
    public Collection<IRFunction> getFunctions() {
        return functions.values();
    }

    public int getFunctionCount() {
        return functions.size();
    }

    // ==================== 全局变量管理 ====================

    /**
     * 添加全局变量
     * @param global 全局变量
     */
    public void addGlobal(IRGlobal global) {
        globals.put(global.getName(), global);
    }

    /**
     * 获取指定名称的全局变量
     * @param name 变量名
     * @return 全局变量，如果不存在则返回 null
     */
    public IRGlobal getGlobal(String name) {
        return globals.get(name);
    }

    /**
     * 检查是否存在指定的全局变量
     * @param name 变量名
     * @return 是否存在
     */
    public boolean hasGlobal(String name) {
        return globals.containsKey(name);
    }

    /**
     * 获取所有全局变量
     * @return 全局变量集合
     */
    public Collection<IRGlobal> getGlobals() {
        return globals.values();
    }

    public int getGlobalCount() {
        return globals.size();
    }

    // ==================== 结构体类型管理 ====================

    /**
     * 添加结构体类型
     * @param struct 结构体类型
     */
    public void addStruct(IRStructType struct) {
        structs.put(struct.getName(), struct);
    }

    /**
     * 获取指定名称的结构体类型
     * @param name 结构体名
     * @return 结构体类型，如果不存在则返回 null
     */
    public IRStructType getStruct(String name) {
        return structs.get(name);
    }

    /**
     * 检查是否存在指定的结构体类型
     * @param name 结构体名
     * @return 是否存在
     */
    public boolean hasStruct(String name) {
        return structs.containsKey(name);
    }

    /**
     * 获取所有结构体类型
     * @return 结构体类型集合
     */
    public Collection<IRStructType> getStructs() {
        return structs.values();
    }

    public int getStructCount() {
        return structs.size();
    }

    // ==================== 枚举类型管理 ====================

    /**
     * 添加枚举类型
     * @param name 枚举名
     * @param underlyingType 底层类型（通常是 i32）
     * @param variantValues 变体名到值的映射
     */
    public void addEnum(String name, IRType underlyingType, Map<String, Integer> variantValues) {
        enums.put(name, new EnumInfo(name, underlyingType, variantValues));
    }

    /**
     * 获取指定名称的枚举信息
     * @param name 枚举名
     * @return 枚举信息，如果不存在则返回 null
     */
    public EnumInfo getEnum(String name) {
        return enums.get(name);
    }

    /**
     * 检查是否存在指定的枚举
     * @param name 枚举名
     * @return 是否存在
     */
    public boolean hasEnum(String name) {
        return enums.containsKey(name);
    }

    /**
     * 获取所有枚举信息
     * @return 枚举信息集合
     */
    public Collection<EnumInfo> getEnums() {
        return enums.values();
    }

    public int getEnumCount() {
        return enums.size();
    }
}
