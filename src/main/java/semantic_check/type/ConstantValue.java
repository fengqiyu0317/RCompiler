// Constant value class for storing evaluated constant expressions
// This class represents a value that can be determined at compile time

import java.util.List;
import java.util.ArrayList;

public class ConstantValue {
    private final Object value;
    private final Type type;
    
    public ConstantValue(Object value, Type type) {
        this.value = value;
        this.type = type;
    }
    
    public Object getValue() {
        return value;
    }
    
    public Type getType() {
        return type;
    }
    
    public boolean isNumeric() {
        return type.isNumeric();
    }
    
    public boolean isBoolean() {
        return type.isBoolean();
    }
    
    public boolean isString() {
        return type.toString().equals("str");
    }
    
    public boolean isChar() {
        return type.toString().equals("char");
    }
    
    public boolean isArray() {
        return type instanceof ArrayType;
    }
    
    // Get as long (for numeric values)
    public long getAsLong() {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        throw new RuntimeException("Cannot convert non-numeric value to long");
    }
    
    // Get as boolean
    public boolean getAsBoolean() {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        throw new RuntimeException("Cannot convert non-boolean value to boolean");
    }
    
    // Get as string
    public String getAsString() {
        if (value instanceof String) {
            return (String) value;
        }
        throw new RuntimeException("Cannot convert non-string value to string");
    }
    
    // Get as char
    public char getAsChar() {
        if (value instanceof String && ((String) value).length() == 1) {
            return ((String) value).charAt(0);
        }
        throw new RuntimeException("Cannot convert value to char");
    }
    
    // Get as array (for array values)
    @SuppressWarnings("unchecked")
    public List<ConstantValue> getAsArray() {
        if (value instanceof List) {
            return (List<ConstantValue>) value;
        }
        throw new RuntimeException("Cannot convert non-array value to array");
    }
    
    @Override
    public String toString() {
        if (isArray()) {
            StringBuilder sb = new StringBuilder();
            sb.append("ConstantValue{value=[");
            List<ConstantValue> arrayValues = getAsArray();
            for (int i = 0; i < arrayValues.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(arrayValues.get(i).getValue());
            }
            sb.append("], type=").append(type).append("}");
            return sb.toString();
        }
        return "ConstantValue{" +
               "value=" + value +
               ", type=" + type +
               '}';
    }
}