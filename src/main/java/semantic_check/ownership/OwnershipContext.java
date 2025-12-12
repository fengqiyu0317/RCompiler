import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/**
 * 所有权上下文类
 */
public class OwnershipContext {
    private final Map<String, VariableOwnership> variables = new HashMap<>();
    private final Map<String, Map<String, Boolean>> fieldOwnership = new HashMap<>(); // 基础变量 -> 字段名 -> 是否已移动
    final Set<String> mutableBorrows = new HashSet<>();
    final Set<String> immutableBorrows = new HashSet<>();
    // 完整路径的借用状态跟踪
    final Set<ExpressionPath> mutableBorrowPaths = new HashSet<>();
    final Set<ExpressionPath> immutableBorrowPaths = new HashSet<>();
    // 父上下文的引用，用于作用域链查找
    private final OwnershipContext parent;
    
    /**
     * 构造函数，用于创建新上下文（根上下文）
     */
    public OwnershipContext() {
        this.parent = null;
    }
    
    /**
     * 构造函数，用于从父上下文继承
     */
    public OwnershipContext(OwnershipContext parent) {
        this.parent = parent;
        // 不再继承父上下文的变量，而是通过作用域链查找
        // 这样可以正确处理变量遮蔽
    }
    
    /**
     * 声明变量
     */
    public void declareVariable(String varName, boolean isMutable) {
        variables.put(varName, new VariableOwnership(varName, isMutable));
        // 初始化字段所有权映射
        fieldOwnership.put(varName, new HashMap<>());
    }
    
    /**
     * 移动变量
     */
    public void moveVariable(String varName) {
        VariableOwnership ownership = findVariableInScopeChain(varName);
        if (ownership != null) {
            ownership.setMoved(true);
        }
    }
    
    /**
     * 移动字段
     */
    public void moveField(String baseVar, String fieldName) {
        // 确保字段所有权映射存在
        if (!fieldOwnership.containsKey(baseVar)) {
            fieldOwnership.put(baseVar, new HashMap<>());
        }
        // 标记字段为已移动
        fieldOwnership.get(baseVar).put(fieldName, true);
    }
    
    /**
     * 移动表达式路径
     */
    public void moveExpressionPath(ExpressionPath path) {
        if (path instanceof VariablePath) {
            // 简单变量
            moveVariable(path.getBaseVariable());
        } else if (path instanceof FieldAccessPath) {
            // 字段访问
            FieldAccessPath fieldPath = (FieldAccessPath) path;
            moveField(fieldPath.getBaseVariable(), fieldPath.getFieldName());
        } else if (path instanceof IndexAccessPath) {
            // 索引访问不能移动，且错误检查已在OwnershipCheckerCore中完成
            // 这里不需要做任何操作
        } else if (path instanceof DerefAccessPath) {
            // 解引用访问
            DerefAccessPath derefPath = (DerefAccessPath) path;
            moveExpressionPath(derefPath.getPointer());
        }
    }
    
    // FieldAccessPath has been removed, so moveFieldAccessPath method is no longer needed
    // Field access is now handled through VariablePath with the full path
    
    // LegacyFieldAccessPath and moveFieldPath method have been removed
    // Field access is now handled through VariablePath with the full path
    
    /**
     * 检查变量是否已被移动
     */
    public boolean isVariableMoved(String varName) {
        VariableOwnership ownership = findVariableInScopeChain(varName);
        return ownership != null && ownership.isMoved();
    }
    
    /**
     * 检查字段是否已被移动
     */
    public boolean isFieldMoved(String baseVar, String fieldName) {
        // 首先检查基础变量是否已被移动
        if (isVariableMoved(baseVar)) {
            return true;
        }
        
        // 检查字段是否已被移动
        Map<String, Boolean> fields = fieldOwnership.get(baseVar);
        if (fields != null) {
            Boolean moved = fields.get(fieldName);
            return moved != null && moved;
        }
        
        return false;
    }
    
    /**
     * 检查表达式路径是否已被移动
     */
    public boolean isExpressionMoved(ExpressionPath path) {
        if (path instanceof VariablePath) {
            // 简单变量
            return isVariableMoved(path.getBaseVariable());
        } else if (path instanceof FieldAccessPath) {
            // 字段访问
            FieldAccessPath fieldPath = (FieldAccessPath) path;
            return isFieldMoved(fieldPath.getBaseVariable(), fieldPath.getFieldName());
        } else if (path instanceof IndexAccessPath) {
            // 索引访问本身不能移动，且错误检查已在OwnershipCheckerCore中完成
            // 这里总是返回false，因为索引访问不能被移动
            return false;
        } else if (path instanceof DerefAccessPath) {
            // 解引用访问
            DerefAccessPath derefPath = (DerefAccessPath) path;
            return isExpressionMoved(derefPath.getPointer());
        }
        
        return false;
    }
    
    // FieldAccessPath has been removed, so isFieldAccessPathMoved method is no longer needed
    // Field access is now handled through VariablePath with the full path
    
    // LegacyFieldAccessPath and isFieldMoved method have been removed
    // Field access is now handled through VariablePath with the full path
    
    /**
     * 检查变量是否可变
     */
    public boolean isVariableMutable(String varName) {
        VariableOwnership ownership = findVariableInScopeChain(varName);
        return ownership != null && ownership.isMutable();
    }
    
    /**
     * 在作用域链中查找变量
     * 从当前作用域开始，逐级向上查找，直到找到第一个匹配的变量名
     */
    private VariableOwnership findVariableInScopeChain(String varName) {
        // 先在当前作用域中查找
        VariableOwnership ownership = variables.get(varName);
        if (ownership != null) {
            return ownership;
        }
        
        // 如果当前作用域中没有，则向父作用域查找
        if (parent != null) {
            return parent.findVariableInScopeChain(varName);
        }
        
        // 没有找到
        return null;
    }
    
    /**
     * 获取变量所在的作用域
     * 从当前作用域开始，逐级向上查找，直到找到第一个匹配的变量名
     * 返回包含该变量的作用域，如果没有找到则返回null
     */
    private OwnershipContext findScopeContainingVariable(String varName) {
        // 先在当前作用域中查找
        if (variables.containsKey(varName)) {
            return this;
        }
        
        // 如果当前作用域中没有，则向父作用域查找
        if (parent != null) {
            return parent.findScopeContainingVariable(varName);
        }
        
        // 没有找到
        return null;
    }
    
    /**
     * 添加可变借用
     */
    public void addMutableBorrow(String varName) {
        mutableBorrows.add(varName);
    }
    
    /**
     * 添加不可变借用
     */
    public void addImmutableBorrow(String varName) {
        immutableBorrows.add(varName);
    }
    
    /**
     * 添加可变借用路径
     */
    public void addMutableBorrowPath(ExpressionPath path) {
        mutableBorrowPaths.add(path);
    }
    
    /**
     * 添加不可变借用路径
     */
    public void addImmutableBorrowPath(ExpressionPath path) {
        immutableBorrowPaths.add(path);
    }
    
    /**
     * 检查是否可以创建可变借用
     */
    public boolean canCreateMutableBorrow(String varName) {
        // 如果变量不可变，则不能创建可变借用
        if (!isVariableMutable(varName)) {
            return false;
        }
        
        // 如果变量已经有任何借用，则不能创建新的可变借用
        return !mutableBorrows.contains(varName) && !immutableBorrows.contains(varName);
    }
    
    /**
     * 检查是否可以创建可变借用路径
     */
    public boolean canCreateMutableBorrow(ExpressionPath path) {
        String baseVar = path.getBaseVariable();
        
        // 如果变量不可变，则不能创建可变借用
        if (!isVariableMutable(baseVar)) {
            return false;
        }
        
        // 检查路径本身是否已经被借用
        return !mutableBorrowPaths.contains(path) && !immutableBorrowPaths.contains(path);
    }
    
    /**
     * 检查是否可以创建不可变借用
     */
    public boolean canCreateImmutableBorrow(String varName) {
        // 如果变量已经有可变借用，则不能创建不可变借用
        return !mutableBorrows.contains(varName);
    }
    
    /**
     * 检查是否可以创建不可变借用路径
     */
    public boolean canCreateImmutableBorrow(ExpressionPath path) {
        // 检查路径本身是否已经有可变借用
        return !mutableBorrowPaths.contains(path);
    }
    
    /**
     * 检查变量是否被借用（包括可变和不可变借用）
     */
    public boolean isVariableBorrowed(String varName) {
        return mutableBorrows.contains(varName) || immutableBorrows.contains(varName);
    }
    
    /**
     * 释放借用
     */
    public void releaseBorrow(String varName) {
        mutableBorrows.remove(varName);
        immutableBorrows.remove(varName);
    }
    
    /**
     * 释放借用路径
     */
    public void releaseBorrowPath(ExpressionPath path) {
        mutableBorrowPaths.remove(path);
        immutableBorrowPaths.remove(path);
    }
    
    // 移除了mergeToParent方法，因为通过作用域链，变量的状态变化已经实时反映到父作用域
    // 不需要在退出作用域时合并状态
}