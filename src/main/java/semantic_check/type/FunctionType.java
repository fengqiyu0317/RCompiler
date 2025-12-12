import java.util.List;
import java.util.ArrayList;

// Function type implementation

public class FunctionType implements Type {
    private final List<Type> parameterTypes;
    private final Type returnType;
    private final boolean isMethod;
    private boolean isMutable;
    private final Type selfType;
    
    public FunctionType(List<Type> parameterTypes, Type returnType, boolean isMethod) {
        this(parameterTypes, returnType, isMethod, false, null);
    }
    
    public FunctionType(List<Type> parameterTypes, Type returnType) {
        this(parameterTypes, returnType, false, false, null);
    }
    
    public FunctionType(List<Type> parameterTypes, Type returnType, boolean isMethod, boolean isMutable) {
        this(parameterTypes, returnType, isMethod, isMutable, null);
    }
    
    public FunctionType(List<Type> parameterTypes, Type returnType, boolean isMethod, boolean isMutable, Type selfType) {
        this.parameterTypes = new ArrayList<>(parameterTypes);
        this.returnType = returnType;
        this.isMethod = isMethod;
        this.isMutable = isMutable;
        this.selfType = selfType;
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
    
    public Type getSelfType() {
        return selfType;
    }
    
    @Override
    public void setMutability(boolean isMutable) {
        this.isMutable = isMutable;
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
    
    @Override
    public boolean isMutable() {
        return isMutable;
    }
    
}