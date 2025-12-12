import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

// Trait type implementation

public class TraitType implements Type {
    private final String name;
    private final Map<String, FunctionType> methods;
    private final Map<String, Type> constants;
    private final Symbol traitSymbol;
    private boolean isMutable;
    
    public TraitType(String name, Symbol traitSymbol) {
        this(name, traitSymbol, false);
    }
    
    public TraitType(String name, Symbol traitSymbol, boolean isMutable) {
        this.name = name;
        this.methods = new HashMap<>();
        this.constants = new HashMap<>();
        this.traitSymbol = traitSymbol;
        this.isMutable = isMutable;
    }
    
    public String getName() {
        return name;
    }
    
    public Map<String, FunctionType> getMethods() {
        return new HashMap<>(methods);
    }
    
    public Map<String, Type> getConstants() {
        return new HashMap<>(constants);
    }
    
    public Symbol getTraitSymbol() {
        return traitSymbol;
    }
    
    @Override
    public void setMutability(boolean isMutable) {
        this.isMutable = isMutable;
    }
    
    public void addMethod(String methodName, FunctionType methodType) {
        methods.put(methodName, methodType);
    }
    
    public void addConstant(String constantName, Type constantType) {
        constants.put(constantName, constantType);
    }
    
    public boolean hasMethod(String methodName) {
        return methods.containsKey(methodName);
    }
    
    public boolean hasConstant(String constantName) {
        return constants.containsKey(constantName);
    }
    
    public FunctionType getMethodType(String methodName) {
        if (methods.containsKey(methodName)) {
            return methods.get(methodName);
        }
        return null;
    }
    
    public Type getConstantType(String constantName) {
        if (constants.containsKey(constantName)) {
            return constants.get(constantName);
        }
        return null;
    }
    
    @Override
    public boolean equals(Type other) {
        if (other instanceof TraitType) {
            TraitType otherTrait = (TraitType) other;
            return name.equals(otherTrait.name);
        }
        return false;
    }
    
    @Override
    public String toString() {
        return name;
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