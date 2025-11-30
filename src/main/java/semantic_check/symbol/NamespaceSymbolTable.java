import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Multi-namespace symbol table class
public class NamespaceSymbolTable {
    // Type namespace
    private final Map<String, Symbol> typeNamespace;
    
    // Value namespace
    private final Map<String, Symbol> valueNamespace;
    
    // Field namespace (organized by type)
    private final Map<String, Map<String, Symbol>> fieldNamespaces;
    
    // Parent scope
    private NamespaceSymbolTable parent;
    
    // Child scopes
    private final List<NamespaceSymbolTable> children;
    
    // Scope level
    private final int scopeLevel;
    
    // Constructor
    public NamespaceSymbolTable(int scopeLevel) {
        this.typeNamespace = new HashMap<>();
        this.valueNamespace = new HashMap<>();
        this.fieldNamespaces = new HashMap<>();
        this.children = new ArrayList<>();
        this.scopeLevel = scopeLevel;
        this.parent = null;
    }
    
    private NamespaceSymbolTable(int scopeLevel, NamespaceSymbolTable parent) {
        this.typeNamespace = new HashMap<>();
        this.valueNamespace = new HashMap<>();
        this.fieldNamespaces = new HashMap<>();
        this.children = new ArrayList<>();
        this.scopeLevel = scopeLevel;
        this.parent = parent;
        parent.children.add(this);
    }
    
    // Create new child scope
    public NamespaceSymbolTable enterScope() {
        return new NamespaceSymbolTable(this.scopeLevel + 1, this);
    }
    
    // Exit current scope, return parent scope
    public NamespaceSymbolTable exitScope() {
        return parent;
    }
    
    // Add type symbol
    public void addTypeSymbol(Symbol symbol) {
        if (symbol.getNamespace() != Namespace.TYPE) {
            throw new SemanticException(
                "Symbol is not in type namespace: " + symbol,
                symbol.getDeclaration()
            );
        }
        
        // Check if already exists
        if (typeNamespace.containsKey(symbol.getName())) {
            throw new SemanticException(
                String.format("Duplicate type declaration: '%s' at scope level %d",
                             symbol.getName(), scopeLevel),
                symbol.getDeclaration()
            );
        }
        
        typeNamespace.put(symbol.getName(), symbol);
    }
    
    // Add value symbol
    public void addValueSymbol(Symbol symbol) {
        if (symbol.getNamespace() != Namespace.VALUE) {
            throw new SemanticException(
                "Symbol is not in value namespace: " + symbol,
                symbol.getDeclaration()
            );
        }
        
        // Special handling for LOCAL_VARIABLE and PARAMETER: check for conflicting CONSTANT in current or parent scopes
        if (symbol.getKind() == SymbolKind.LOCAL_VARIABLE || symbol.getKind() == SymbolKind.PARAMETER) {
            NamespaceSymbolTable scope = this;
            while (scope != null) {
                if (scope.valueNamespace.containsKey(symbol.getName())) {
                    Symbol existingSymbol = scope.valueNamespace.get(symbol.getName());
                    if (existingSymbol.getKind() == SymbolKind.CONSTANT) {
                        throw new SemanticException(
                            String.format("cannot define %s '%s' as it conflicts with constant in scope",
                                        symbol.getKind() == SymbolKind.LOCAL_VARIABLE ? "local variable" : "parameter",
                                        symbol.getName()),
                            symbol.getDeclaration()
                        );
                    }
                }
                scope = scope.parent;
            }
        }
        
        // Check if already exists in current scope
        if (valueNamespace.containsKey(symbol.getName()) && symbol.getKind() != SymbolKind.LOCAL_VARIABLE) {
            throw new SemanticException(
                String.format("Duplicate value declaration: '%s' at scope level %d",
                             symbol.getName(), scopeLevel),
                symbol.getDeclaration()
            );
        }
        
        valueNamespace.put(symbol.getName(), symbol);
    }
    
    // Add field symbol
    public void addFieldSymbol(String typeName, Symbol symbol) {
        if (symbol.getNamespace() != Namespace.FIELD) {
            throw new SemanticException(
                "Symbol is not a field: " + symbol,
                symbol.getDeclaration()
            );
        }
        
        // Get or create field namespace for this type
        Map<String, Symbol> typeFields = fieldNamespaces.computeIfAbsent(
            typeName, k -> new HashMap<>()
        );
        
        // Check if already exists
        if (typeFields.containsKey(symbol.getName())) {
            throw new SemanticException(
                String.format("Duplicate field declaration: '%s' in type '%s'",
                             symbol.getName(), typeName),
                symbol.getDeclaration()
            );
        }
        
        typeFields.put(symbol.getName(), symbol);
    }
    
    // Lookup symbol in type namespace (including parent scopes)
    public Symbol lookupType(String name) {
        Symbol symbol = typeNamespace.get(name);
        if (symbol != null) {
            return symbol;
        }
        
        // If not found in current scope, recursively search parent scopes
        if (parent != null) {
            return parent.lookupType(name);
        }
        
        return null;
    }
    
    // Lookup symbol in value namespace (including parent scopes)
    public Symbol lookupValue(String name) {
        Symbol symbol = valueNamespace.get(name);
        if (symbol != null) {
            return symbol;
        }
        
        // If not found in current scope, recursively search parent scopes
        if (parent != null) {
            return parent.lookupValue(name);
        }
        
        return null;
    }
    
    // Lookup symbol in field namespace
    public Symbol lookupField(String typeName, String fieldName) {
        // First search in current scope
        Map<String, Symbol> typeFields = fieldNamespaces.get(typeName);
        if (typeFields != null) {
            Symbol symbol = typeFields.get(fieldName);
            if (symbol != null) {
                return symbol;
            }
            // If we found typeFields but not the specific field, don't recurse
            // The type exists in this scope but doesn't have this field
            return null;
        }
        
        // If type not found in current scope, recursively search parent scopes
        if (parent != null) {
            return parent.lookupField(typeName, fieldName);
        }
        
        return null;
    }
    
    // Lookup type symbol in current scope (excluding parent scopes)
    public Symbol lookupTypeInCurrentScope(String name) {
        return typeNamespace.get(name);
    }
    
    // Lookup value symbol in current scope (excluding parent scopes)
    public Symbol lookupValueInCurrentScope(String name) {
        return valueNamespace.get(name);
    }
    
    // Get scope level
    public int getScopeLevel() {
        return scopeLevel;
    }
    
    // Get parent scope
    public NamespaceSymbolTable getParent() {
        return parent;
    }
    
    // Get type namespace map (for debugging)
    public Map<String, Symbol> getTypeNamespace() {
        return typeNamespace;
    }
    
    // Get value namespace map (for debugging)
    public Map<String, Symbol> getValueNamespace() {
        return valueNamespace;
    }
    
    // Get field namespaces map (for debugging)
    public Map<String, Map<String, Symbol>> getFieldNamespaces() {
        return fieldNamespaces;
    }
    
    // Get child scopes (for debugging)
    public List<NamespaceSymbolTable> getChildren() {
        return children;
    }
    
    // Debug output
    public void debugPrint() {
        System.out.println("=== Scope Level " + scopeLevel + " ===");
        System.out.println("Type Namespace:");
        for (Symbol symbol : typeNamespace.values()) {
            System.out.println("  " + symbol);
        }
        System.out.println("Value Namespace:");
        for (Symbol symbol : valueNamespace.values()) {
            System.out.println("  " + symbol);
        }
        System.out.println("Field Namespace:");
        for (Map.Entry<String, Map<String, Symbol>> entry : fieldNamespaces.entrySet()) {
            System.out.println("  Type " + entry.getKey() + ":");
            for (Symbol symbol : entry.getValue().values()) {
                System.out.println("    " + symbol);
            }
        }
        System.out.println();
    }
}