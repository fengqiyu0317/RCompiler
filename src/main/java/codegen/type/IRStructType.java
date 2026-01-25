package codegen.type;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * IR 结构体类型
 * 包含字段类型列表和内存布局信息
 */
public class IRStructType extends IRType {
    private final String name;
    private final List<IRType> fields;
    private final List<Integer> offsets;
    private List<String> fieldNames;  // 字段名列表（可选）
    private int totalSize;

    public IRStructType(String name, List<IRType> fields) {
        this.name = name;
        this.fields = new ArrayList<>(fields);
        this.offsets = new ArrayList<>();
        this.fieldNames = null;
        computeLayout();
    }

    /**
     * 计算结构体的内存布局
     * 包括每个字段的偏移量和总大小
     */
    private void computeLayout() {
        int offset = 0;
        for (IRType field : fields) {
            int align = field.getAlign();
            // 对齐到字段要求的边界
            offset = (offset + align - 1) / align * align;
            offsets.add(offset);
            offset += field.getSize();
        }
        // 结构体总大小需要对齐到结构体的对齐要求
        int structAlign = getAlign();
        totalSize = (offset + structAlign - 1) / structAlign * structAlign;
    }

    public String getName() {
        return name;
    }

    public int getFieldCount() {
        return fields.size();
    }

    /**
     * 获取指定字段的偏移量
     * @param index 字段索引
     * @return 字段在结构体中的字节偏移
     */
    public int getFieldOffset(int index) {
        return offsets.get(index);
    }

    /**
     * 获取指定字段的类型
     * @param index 字段索引
     * @return 字段类型
     */
    public IRType getFieldType(int index) {
        return fields.get(index);
    }

    public List<IRType> getFields() {
        return fields;
    }

    /**
     * 设置字段名列表
     * @param names 字段名列表
     */
    public void setFieldNames(List<String> names) {
        this.fieldNames = new ArrayList<>(names);
    }

    /**
     * 获取字段名列表
     * @return 字段名列表，如果未设置则返回 null
     */
    public List<String> getFieldNames() {
        return fieldNames;
    }

    /**
     * 根据字段名获取字段索引
     * @param name 字段名
     * @return 字段索引，如果未找到则返回 -1
     */
    public int getFieldIndex(String name) {
        if (fieldNames == null) return -1;
        return fieldNames.indexOf(name);
    }

    @Override
    public int getSize() {
        return totalSize;
    }

    @Override
    public int getAlign() {
        if (fields.isEmpty()) return 1;
        int maxAlign = 1;
        for (IRType field : fields) {
            maxAlign = Math.max(maxAlign, field.getAlign());
        }
        return maxAlign;
    }

    @Override
    public String toString() {
        return "%" + name;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof IRStructType)) return false;
        return Objects.equals(name, ((IRStructType) other).name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
