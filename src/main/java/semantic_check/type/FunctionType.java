import java.util.List;
import java.util.ArrayList;

// Function type implementation

public class FunctionType implements Type {
    private final List<Type> parameterTypes;
    private final Type returnType;
    private final boolean isMethod;
    
    public FunctionType(List<Type> parameterTypes, Type returnType, boolean isMethod) {
        this.parameterTypes = new ArrayList<>(parameterTypes);
        this.returnType = returnType;
        this.isMethod = isMethod;
    }
    
    public FunctionType(List<Type> parameterTypes, Type returnType) {
        this(parameterTypes, returnType, false);
    }
    
    public List<Type> getParameterTypes() {
        return new ArrayList<>(parameterTypes);
    }
    
    public Type getReturnType() {
        return returnType;
    }
    
    public boolean isMethod() {
        return isMethod;
    }
    
    @Override
    public boolean equals(Type other) {
        if (other instanceof FunctionType) {
            FunctionType otherFunction = (FunctionType) other;
            if (isMethod != otherFunction.isMethod) {
                return false;
            }
            if (!returnType.equals(otherFunction.returnType)) {
                return false;
            }
            if (parameterTypes.size() != otherFunction.parameterTypes.size()) {
                return false;
            }
            for (int i = 0; i < parameterTypes.size(); i++) {
                if (!parameterTypes.get(i).equals(otherFunction.parameterTypes.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("fn(");
        for (int i = 0; i < parameterTypes.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(parameterTypes.get(i).toString());
        }
        sb.append(") -> ");
        sb.append(returnType.toString());
        return sb.toString();
    }
    
    @Override
    public Type getBaseType() {
        return this;
    }
    
    @Override
    public boolean isNumeric() {
        return false;
    }
    
    @Override
    public boolean isBoolean() {
        return false;
    }
    
    @Override
    public boolean isUnit() {
        return false;
    }
    
    @Override
    public boolean isNever() {
        return false;
    }
}