import java.util.Map;
import java.util.HashMap;

// Struct type implementation

public class StructType implements Type {
    private final String name;
    private final Map<String, Type> fields;
    private final Symbol structSymbol;
    
    public StructType(String name, Map<String, Type> fields, Symbol structSymbol) {
        this.name = name;
        this.fields = new HashMap<>(fields);
        this.structSymbol = structSymbol;
    }
    
    public String getName() {
        return name;
    }
    
    public Map<String, Type> getFields() {
        return new HashMap<>(fields);
    }
    
    public Symbol getStructSymbol() {
        return structSymbol;
    }
    
    public Type getFieldType(String fieldName) {
        return fields.get(fieldName);
    }
    
    @Override
    public boolean equals(Type other) {
        if (other instanceof StructType) {
            StructType otherStruct = (StructType) other;
            return name.equals(otherStruct.name);
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
}