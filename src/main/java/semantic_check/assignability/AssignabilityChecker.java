/**
 * AssignabilityChecker - 检查表达式是否可以作为赋值目标
 * 用于确定表达式是否可以作为左值（lvalue）使用
 */
public class AssignabilityChecker {
    
    /**
     * 检查表达式是否可以作为赋值目标
     * @param expr 要检查的表达式
     * @return 如果表达式可以作为赋值目标则返回true，否则返回false
     */
    public static boolean isAssignable(ExprNode expr) {
        if (expr == null) {
            return false;
        }
        
        // 首先检查表达式本身的isAssignable属性
        if (!expr.isAssignable()) {
            return false;
        }
        
        // 根据表达式类型进行具体检查
        if (expr instanceof PathExprNode) {
            // 路径表达式（变量访问）通常是可赋值的
            return isPathExprAssignable((PathExprNode) expr);
        } else if (expr instanceof FieldExprNode) {
            // 字段访问表达式通常是可赋值的
            return isFieldExprAssignable((FieldExprNode) expr);
        } else if (expr instanceof IndexExprNode) {
            // 索引表达式（数组访问）通常是可赋值的
            return isIndexExprAssignable((IndexExprNode) expr);
        } else if (expr instanceof DerefExprNode) {
            // 解引用表达式通常是可赋值的
            return isDerefExprAssignable((DerefExprNode) expr);
        }
        
        // 其他类型的表达式通常不可赋值
        return false;
    }
    
    /**
     * 检查路径表达式是否可赋值
     */
    private static boolean isPathExprAssignable(PathExprNode pathExpr) {
        // 获取路径表达式对应的符号
        Symbol symbol = pathExpr.getSymbol();
        if (symbol == null) {
            return false;
        }
        
        // 检查符号是否表示可变变量
        Type symbolType = symbol.getType();
        if (symbolType != null && !symbolType.isMutable()) {
            return false;
        }
        
        // 路径表达式通常是可赋值的（除非是常量或不可变变量）
        return true;
    }
    
    /**
     * 检查字段访问表达式是否可赋值
     */
    private static boolean isFieldExprAssignable(FieldExprNode fieldExpr) {
        // 检查接收者是否可赋值
        if (fieldExpr.receiver != null && !isAssignable(fieldExpr.receiver)) {
            return false;
        }
        
        // 检查字段本身是否可变
        // 这里需要根据结构体类型的字段定义来判断
        Type receiverType = fieldExpr.receiver.getType();
        if (receiverType instanceof StructType) {
            StructType structType = (StructType) receiverType;
            String fieldName = fieldExpr.fieldName != null ? fieldExpr.fieldName.name : null;
            
            if (fieldName != null) {
                Type fieldType = structType.getFieldType(fieldName);
                if (fieldType != null && !fieldType.isMutable()) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * 检查索引表达式是否可赋值
     */
    private static boolean isIndexExprAssignable(IndexExprNode indexExpr) {
        // 检查数组是否可赋值
        if (indexExpr.array != null && !isAssignable(indexExpr.array)) {
            return false;
        }
        
        // 检查数组类型是否可变
        Type arrayType = indexExpr.array.getType();
        if (arrayType instanceof ArrayType) {
            ArrayType arrType = (ArrayType) arrayType;
            if (!arrType.isMutable()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 检查解引用表达式是否可赋值
     */
    private static boolean isDerefExprAssignable(DerefExprNode derefExpr) {
        // 解引用表达式通常是可赋值的，前提是引用本身指向可变数据
        if (derefExpr.innerExpr != null) {
            Type innerType = derefExpr.innerExpr.getType();
            if (innerType instanceof ReferenceType) {
                ReferenceType refType = (ReferenceType) innerType;
                return refType.isMutable();
            }
        }
        
        return false;
    }
}