/**
 * Mutability检查相关的错误消息
 */
public class MutabilityError {
    // 变量赋值错误
    public static final String IMMUTABLE_VARIABLE_ASSIGNMENT = 
        "Cannot assign to immutable variable '%s'";
    
    // 借用错误
    public static final String MUTABLE_BORROW_OF_IMMUTABLE_VARIABLE = 
        "Cannot create mutable borrow of immutable variable '%s'";
    
    public static final String MULTIPLE_MUTABLE_BORROWS = 
        "Cannot create multiple mutable borrows of variable '%s'";
    
    public static final String MUTABLE_BORROW_WITH_EXISTING_BORROW = 
        "Cannot create mutable borrow of variable '%s' when it already has active borrows";
    
    public static final String IMMUTABLE_BORROW_WITH_MUTABLE_BORROW = 
        "Cannot create immutable borrow of variable '%s' when it already has a mutable borrow";
    
    // 字段访问错误
    public static final String IMMUTABLE_FIELD_ACCESS = 
        "Cannot access field of immutable variable '%s'";
    
    public static final String MUTABLE_FIELD_ACCESS_ON_IMMUTABLE = 
        "Cannot mutably access field of immutable variable '%s'";
    
    // 数组索引错误
    public static final String IMMUTABLE_ARRAY_INDEX = 
        "Cannot index into immutable array '%s'";
    
    public static final String MUTABLE_ARRAY_INDEX_ON_IMMUTABLE = 
        "Cannot mutably index into immutable array '%s'";
    
    // 解引用错误
    public static final String IMMUTABLE_DEREFERENCE = 
        "Cannot dereference immutable reference '%s'";
    
    public static final String MUTABLE_DEREFERENCE_ON_IMMUTABLE = 
        "Cannot mutably dereference immutable reference '%s'";
    
    // 函数参数错误
    public static final String IMMUTABLE_PARAM_MUTATION = 
        "Cannot mutate immutable parameter '%s'";
    
    // 通用错误
    public static final String IMMUTABLE_EXPRESSION_MUTATION = 
        "Cannot mutate immutable expression";
    
    public static final String MUTABILITY_CHECK_FAILED = 
        "Mutability check failed: %s";
    
    /**
     * 格式化错误消息
     */
    public static String format(String template, String... args) {
        String result = template;
        for (int i = 0; i < args.length; i++) {
            result = result.replaceFirst("%s", args[i]);
        }
        return result;
    }
}