import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

// Enum type implementation

public class EnumType implements Type {
    private final String name;
    private final Map<String, List<Type>> variants;
    private final Symbol enumSymbol;
    
    public EnumType(String name, Map<String, List<Type>> variants, Symbol enumSymbol) {
        this.name = name;
        this.variants = new HashMap<>(variants);
        this.enumSymbol = enumSymbol;
    }
    
    public EnumType(String name, Symbol enumSymbol) {
        this.name = name;
        this.variants = new HashMap<>();
        this.enumSymbol = enumSymbol;
    }
    
    public String getName() {
        return name;
    }
    
    public Map<String, List<Type>> getVariants() {
        return new HashMap<>(variants);
    }
    
    public Symbol getEnumSymbol() {
        return enumSymbol;
    }
    
    public void addVariant(String variantName, List<Type> variantTypes) {
        variants.put(variantName, new ArrayList<>(variantTypes));
    }
    
    public void addVariant(String variantName) {
        variants.put(variantName, new ArrayList<>());
    }
    
    public boolean hasVariant(String variantName) {
        return variants.containsKey(variantName);
    }
    
    public List<Type> getVariantTypes(String variantName) {
        if (variants.containsKey(variantName)) {
            return new ArrayList<>(variants.get(variantName));
        }
        return null;
    }
    
    @Override
    public boolean equals(Type other) {
        if (other instanceof EnumType) {
            EnumType otherEnum = (EnumType) other;
            return name.equals(otherEnum.name);
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