import java.util.ArrayList;
import java.util.List;

// Symbol table entry class
public class Symbol {
    private final String name;
    private final SymbolKind kind;
    private final ASTNode declaration;
    private final int scopeLevel;
    private final boolean isMutable;
    private final String typeName; // For fields, store the type name they belong to
    private final List<Symbol> implSymbols; // Symbols defined in impl blocks
    private Type type; // Cached type for this symbol
    private ConstantValue constantValue; // Cached constant value for this symbol
    
    public Symbol(String name, SymbolKind kind, ASTNode declaration, int scopeLevel, boolean isMutable) {
        this(name, kind, declaration, scopeLevel, isMutable, null, new ArrayList<>());
    }
    
    public Symbol(String name, SymbolKind kind, ASTNode declaration, int scopeLevel, boolean isMutable, String typeName) {
        this(name, kind, declaration, scopeLevel, isMutable, typeName, new ArrayList<>());
    }
    
    public Symbol(String name, SymbolKind kind, ASTNode declaration, int scopeLevel, boolean isMutable, String typeName, List<Symbol> implSymbols) {
        this.name = name;
        this.kind = kind;
        this.declaration = declaration;
        this.scopeLevel = scopeLevel;
        this.isMutable = isMutable;
        this.typeName = typeName;
        this.implSymbols = implSymbols;
    }
    
    // Getter methods
    public String getName() { return name; }
    public SymbolKind getKind() { return kind; }
    public ASTNode getDeclaration() { return declaration; }
    public int getScopeLevel() { return scopeLevel; }
    public boolean isMutable() { return isMutable; }
    public String getTypeName() { return typeName; }
    public List<Symbol> getImplSymbols() { return implSymbols; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    
    public ConstantValue getConstantValue() { return constantValue; }
    public void setConstantValue(ConstantValue constantValue) { this.constantValue = constantValue; }
    
    // Add impl symbol
    public void addImplSymbol(Symbol implSymbol) {
        implSymbols.add(implSymbol);
    }
    
    public Namespace getNamespace() {
        return kind.getNamespace();
    }
    
    @Override
    public String toString() {
        return String.format("Symbol{name='%s', kind=%s, scopeLevel=%d}",
                           name, kind, scopeLevel);
    }
}