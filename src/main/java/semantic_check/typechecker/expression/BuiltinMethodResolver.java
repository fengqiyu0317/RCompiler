

/**
 * 内置方法解析器
 * 负责查找和管理内置方法
 */
public class BuiltinMethodResolver {
    
    /**
     * Lookup builtin method
     * @param methodName Method name
     * @param receiverType Receiver type
     * @return Method symbol if found, otherwise null
     */
    public static Symbol lookupBuiltinMethod(String methodName, Type receiverType) {
        // Check if this is a valid builtin method for the given receiver type
        boolean isValidMethod = false;
        
        // 检查String和str方法
        if (TypeUtils.isStringOrStrType(receiverType)) {
            isValidMethod = methodName.equals("to_string") ||
                         methodName.equals("as_str") ||
                         methodName.equals("as_mut_str") ||
                         methodName.equals("len") ||
                         methodName.equals("append");
        }
        // Check array methods
        else if (TypeUtils.isArrayType(receiverType)) {
            isValidMethod = methodName.equals("len");
        }
        // 检查u32和usize方法
        else if (TypeUtils.isU32OrUsizeType(receiverType)) {
            isValidMethod = methodName.equals("to_string");
        }
        
        if (!isValidMethod) {
            return null; // 不是此类型的有效内置方法
        }
        
        // 创建BuiltinFunctionNode来表示内置方法
        BuiltinFunctionNode builtinNode = new BuiltinFunctionNode(methodName, BuiltinFunctionNode.BuiltinType.METHOD);
        
        // 根据方法名配置内置方法
        switch (methodName) {
            case "to_string":
                builtinNode.configureToString();
                break;
            case "as_str":
                builtinNode.configureAsStr();
                break;
            case "as_mut_str":
                builtinNode.configureAsMutStr();
                break;
            case "len":
                builtinNode.configureLen();
                break;
            case "append":
                builtinNode.configureAppend();
                break;
            default:
                return null; // Unknown builtin method
        }
        
        // Set symbol for builtin method node
        Symbol builtinMethod = new Symbol(
            methodName,
            SymbolKind.FUNCTION, // 方法被视为函数
            builtinNode, // 使用BuiltinFunctionNode作为声明节点
            0, // 全局作用域
            false // 不可变
        );
        
        // Set symbol in builtin method node
        builtinNode.setSymbol(builtinMethod);
        
        return builtinMethod;
    }
    
    /**
     * 检查类型是否有指定的内置方法
     * @param receiverType 接收者类型
     * @param methodName 方法名
     * @return 如果有则返回true
     */
    public static boolean hasBuiltinMethod(Type receiverType, String methodName) {
        return lookupBuiltinMethod(methodName, receiverType) != null;
    }
    
    /**
     * 获取类型的所有内置方法名
     * @param receiverType 接收者类型
     * @return 方法名列表
     */
    public static String[] getAllBuiltinMethods(Type receiverType) {
        if (TypeUtils.isStringOrStrType(receiverType)) {
            return new String[]{"to_string", "as_str", "as_mut_str", "len", "append"};
        } else if (TypeUtils.isArrayType(receiverType)) {
            return new String[]{"len"};
        } else if (TypeUtils.isU32OrUsizeType(receiverType)) {
            return new String[]{"to_string"};
        }
        
        return new String[0];
    }
}