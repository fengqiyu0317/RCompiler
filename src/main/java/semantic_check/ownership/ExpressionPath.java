import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

/**
 * 表达式路径基类，用于表示各种类型的表达式路径
 * 支持变量访问、字段访问、索引访问等复杂表达式
 */
public abstract class ExpressionPath {
    /**
     * 获取基础变量名
     */
    public abstract String getBaseVariable();
    
    /**
     * 获取完整路径字符串表示
     */
    public abstract String getFullPath();
    
    /**
     * 检查是否可以移动（所有权转移）
     */
    public abstract boolean canMove();
    
    /**
     * 检查是否是字段访问
     */
    public boolean isFieldAccess() {
        return false;
    }
    
    /**
     * 检查是否是索引访问
     */
    public boolean isIndexAccess() {
        return false;
    }
    
    /**
     * 检查是否是解引用访问
     */
    public boolean isDerefAccess() {
        return false;
    }
    
    @Override
    public abstract boolean equals(Object obj);
    
    @Override
    public abstract int hashCode();
    
    @Override
    public String toString() {
        return getFullPath();
    }
}

/**
 * 简单变量路径，如 x 或 self
 */
class VariablePath extends ExpressionPath {
    private final String variableName;
    
    public VariablePath(String variableName) {
        this.variableName = variableName;
    }
    
    @Override
    public String getBaseVariable() {
        return variableName;
    }
    
    @Override
    public String getFullPath() {
        return variableName;
    }
    
    @Override
    public boolean canMove() {
        // self变量的可移动性取决于其声明类型
        // 这里简化处理，假设self参数在方法调用中不会转移所有权
        // 实际应该根据方法签名决定
        if ("self".equals(variableName)) {
            return false;
        }
        return true; // 其他变量通常可以移动（除非实现了Copy trait）
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        VariablePath that = (VariablePath) obj;
        return Objects.equals(variableName, that.variableName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(variableName);
    }
}


/**
 * 字段访问路径，如 struct.field 或 obj.field1.field2 或 self.field
 */
class FieldAccessPath extends ExpressionPath {
    private final ExpressionPath object;
    private final String fieldName;
    
    public FieldAccessPath(ExpressionPath object, String fieldName) {
        this.object = object;
        this.fieldName = fieldName;
    }
    
    @Override
    public String getBaseVariable() {
        return object.getBaseVariable();
    }
    
    @Override
    public String getFullPath() {
        return object.getFullPath() + "." + fieldName;
    }
    
    @Override
    public boolean canMove() {
        // 如果是对self的字段访问，通常不应该移动所有权
        if ("self".equals(object.getBaseVariable())) {
            return false;
        }
        return false; // 字段访问不能移动所有权（部分移动需要特殊处理）
    }
    
    @Override
    public boolean isFieldAccess() {
        return true;
    }
    
    public ExpressionPath getObject() {
        return object;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FieldAccessPath that = (FieldAccessPath) obj;
        return Objects.equals(object, that.object) &&
               Objects.equals(fieldName, that.fieldName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(object, fieldName);
    }
}

/**
 * 索引访问路径，如 array[0] 或 vec[1][2] 或 self[0]
 */
class IndexAccessPath extends ExpressionPath {
    private final ExpressionPath array;
    private final ExprNode index;
    
    public IndexAccessPath(ExpressionPath array, ExprNode index) {
        this.array = array;
        this.index = index;
    }
    
    @Override
    public String getBaseVariable() {
        return array.getBaseVariable();
    }
    
    @Override
    public String getFullPath() {
        return array.getFullPath() + "[" + index.toString() + "]";
    }
    
    @Override
    public boolean canMove() {
        // 如果是对self的索引访问，通常不应该移动所有权
        if ("self".equals(array.getBaseVariable())) {
            return false;
        }
        return false; // 索引访问不能移动所有权
    }
    
    @Override
    public boolean isIndexAccess() {
        return true;
    }
    
    public ExpressionPath getArray() {
        return array;
    }
    
    public ExprNode getIndex() {
        return index;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        IndexAccessPath that = (IndexAccessPath) obj;
        return Objects.equals(array, that.array) &&
               Objects.equals(index, that.index);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(array, index);
    }
}

/**
 * 解引用访问路径，如 *ptr 或 *self
 */
class DerefAccessPath extends ExpressionPath {
    private final ExpressionPath pointer;
    
    public DerefAccessPath(ExpressionPath pointer) {
        this.pointer = pointer;
    }
    
    @Override
    public String getBaseVariable() {
        return pointer.getBaseVariable();
    }
    
    @Override
    public String getFullPath() {
        return "*" + pointer.getFullPath();
    }
    
    @Override
    public boolean canMove() {
        // 如果是对self的解引用，通常不应该移动所有权
        if ("self".equals(pointer.getBaseVariable())) {
            return false;
        }
        return pointer.canMove(); // 解引用的可移动性取决于指针
    }
    
    @Override
    public boolean isDerefAccess() {
        return true;
    }
    
    public ExpressionPath getPointer() {
        return pointer;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DerefAccessPath that = (DerefAccessPath) obj;
        return Objects.equals(pointer, that.pointer);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(pointer);
    }
}
