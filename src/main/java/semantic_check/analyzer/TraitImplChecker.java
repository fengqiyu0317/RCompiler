// TraitImplChecker class - Checks that impl blocks implement all required trait methods
// This class is responsible for verifying that trait implementations are complete

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

public class TraitImplChecker extends VisitorBase {
    private final TypeErrorCollector errorCollector;
    private final boolean throwOnError;
    private final TypeExtractor typeExtractor;
    
    // Store trait methods and constants for checking
    private Map<String, FunctionType> currentTraitMethods = new HashMap<>();
    private Map<String, Type> currentTraitConstants = new HashMap<>();
    
    // Store impl methods and constants for checking
    private Map<String, FunctionType> currentImplMethods = new HashMap<>();
    private Map<String, Type> currentImplConstants = new HashMap<>();
    
    // Current trait and impl symbols
    private Symbol currentTraitSymbol;
    private Symbol currentTypeSymbol;
    
    public TraitImplChecker(TypeErrorCollector errorCollector, boolean throwOnError, TypeExtractor typeExtractor) {
        this.errorCollector = errorCollector;
        this.throwOnError = throwOnError;
        this.typeExtractor = typeExtractor;
    }
    
    // Visit trait node to collect trait methods and constants
    public void visit(TraitNode node) {
        // Clear previous trait data
        currentTraitMethods.clear();
        currentTraitConstants.clear();
        currentTraitSymbol = node.getSymbol();
        
        // Collect trait methods and constants
        if (node.items != null) {
            for (AssoItemNode item : node.items) {
                if (item.function != null) {
                    collectTraitMethod(item.function);
                } else if (item.constant != null) {
                    collectTraitConstant(item.constant);
                }
            }
        }
    }
    
    // Collect trait method information
    private void collectTraitMethod(FunctionNode function) {
        if (function.name == null) return;
        
        String methodName = function.name.name;
        FunctionType methodType = createFunctionType(function);
        
        currentTraitMethods.put(methodName, methodType);
    }
    
    // Collect trait constant information
    private void collectTraitConstant(ConstItemNode constant) {
        if (constant.name == null) return;
        
        String constantName = constant.name.name;
        Type constantType = null;
        
        if (constant.type != null) {
            constantType = typeExtractor.extractTypeFromTypeNode(constant.type);
        }
        
        currentTraitConstants.put(constantName, constantType);
    }
    
    // Visit impl node to check trait implementation
    public void visit(ImplNode node) {
        // Clear previous impl data
        currentImplMethods.clear();
        currentImplConstants.clear();
        currentTypeSymbol = node.getTypeSymbol();
        currentTraitSymbol = node.getTraitSymbol();
        
        // If this is not a trait impl, skip checking
        if (currentTraitSymbol == null) {
            return;
        }
        
        // First, visit the trait to get required methods and constants
        if (currentTraitSymbol.getDeclaration() instanceof TraitNode) {
            TraitNode traitNode = (TraitNode) currentTraitSymbol.getDeclaration();
            visit(traitNode);
        }
        
        // Collect impl methods and constants
        if (node.items != null) {
            for (AssoItemNode item : node.items) {
                if (item.function != null) {
                    collectImplMethod(item.function);
                } else if (item.constant != null) {
                    collectImplConstant(item.constant);
                }
            }
        }
        
        // Check that all trait methods are implemented
        checkTraitMethods();
        
        // Check that all trait constants are implemented
        checkTraitConstants();
    }
    
    // Collect impl method information
    private void collectImplMethod(FunctionNode function) {
        if (function.name == null) return;
        
        String methodName = function.name.name;
        FunctionType methodType = createFunctionType(function);
        
        currentImplMethods.put(methodName, methodType);
    }
    
    // Collect impl constant information
    private void collectImplConstant(ConstItemNode constant) {
        if (constant.name == null) return;
        
        String constantName = constant.name.name;
        Type constantType = null;
        
        if (constant.type != null) {
            constantType = typeExtractor.extractTypeFromTypeNode(constant.type);
        }
        
        currentImplConstants.put(constantName, constantType);
    }
    
    // Check that all trait methods are implemented
    private void checkTraitMethods() {
        for (Map.Entry<String, FunctionType> entry : currentTraitMethods.entrySet()) {
            String methodName = entry.getKey();
            FunctionType traitMethodType = entry.getValue();
            
            if (!currentImplMethods.containsKey(methodName)) {
                // Method not implemented
                reportError("Missing implementation of trait method '" + methodName + 
                          "' for type '" + currentTypeSymbol.getName() + 
                          "' from trait '" + currentTraitSymbol.getName() + "'");
                continue;
            }
            
            // Check method signatures match
            FunctionType implMethodType = currentImplMethods.get(methodName);
            if (!methodSignaturesMatch(traitMethodType, implMethodType)) {
                reportError("Method signature mismatch for '" + methodName + 
                          "' in impl for type '" + currentTypeSymbol.getName() + 
                          "' from trait '" + currentTraitSymbol.getName() + "'");
            }
        }
    }
    
    // Check that all trait constants are implemented
    private void checkTraitConstants() {
        for (Map.Entry<String, Type> entry : currentTraitConstants.entrySet()) {
            String constantName = entry.getKey();
            Type traitConstantType = entry.getValue();
            
            if (!currentImplConstants.containsKey(constantName)) {
                // Constant not implemented
                reportError("Missing implementation of trait constant '" + constantName + 
                          "' for type '" + currentTypeSymbol.getName() + 
                          "' from trait '" + currentTraitSymbol.getName() + "'");
                continue;
            }
            
            // Check constant types match
            Type implConstantType = currentImplConstants.get(constantName);
            if (traitConstantType != null && implConstantType != null && 
                !TypeUtils.isTypeCompatible(implConstantType, traitConstantType)) {
                reportError("Constant type mismatch for '" + constantName + 
                          "' in impl for type '" + currentTypeSymbol.getName() + 
                          "' from trait '" + currentTraitSymbol.getName() + "'");
            }
        }
    }
    
    // Create FunctionType from FunctionNode
    private FunctionType createFunctionType(FunctionNode function) {
        // Extract parameter types
        Type[] paramTypes = new Type[0];
        if (function.parameters != null && !function.parameters.isEmpty()) {
            paramTypes = new Type[function.parameters.size()];
            for (int i = 0; i < function.parameters.size(); i++) {
                ParameterNode param = function.parameters.get(i);
                if (param.type != null) {
                    paramTypes[i] = typeExtractor.extractTypeFromTypeNode(param.type);
                }
            }
        }
        
        // Extract return type
        Type returnType = UnitType.INSTANCE;
        if (function.returnType != null) {
            returnType = typeExtractor.extractTypeFromTypeNode(function.returnType);
        }
        
        List<Type> paramTypeList = Arrays.asList(paramTypes);
        return new FunctionType(paramTypeList, returnType);
    }
    
    // Check if two method signatures match
    private boolean methodSignaturesMatch(FunctionType traitMethod, FunctionType implMethod) {
        // Check parameter count
        if (traitMethod.getParameterTypes().size() != implMethod.getParameterTypes().size()) {
            return false;
        }
        
        // Check parameter types
        List<Type> traitParams = traitMethod.getParameterTypes();
        List<Type> implParams = implMethod.getParameterTypes();
        for (int i = 0; i < traitParams.size(); i++) {
            if (!TypeUtils.isTypeCompatible(implParams.get(i), traitParams.get(i))) {
                return false;
            }
        }
        
        // Check return type
        if (!TypeUtils.isTypeCompatible(implMethod.getReturnType(), traitMethod.getReturnType())) {
            return false;
        }
        
        return true;
    }
    
    // Report an error
    private void reportError(String message) {
        RuntimeException error = new RuntimeException(message);
        if (throwOnError) {
            throw error;
        } else {
            errorCollector.addError(error.getMessage());
        }
    }
}