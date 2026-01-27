package codegen;

import codegen.ir.IRBasicBlock;
import codegen.ir.IRFunction;
import codegen.ir.IRModule;
import codegen.inst.*;
import codegen.type.*;
import codegen.value.*;

import semantic_check.symbol.SymbolKind;
import semantic_check.type.PrimitiveType;

import java.util.*;

/**
 * IR 代码生成器
 * 将 AST 转换为 IR
 *
 * 注意：各个 visit 函数的具体实现暂时省略，
 * 仅提供框架结构和辅助方法
 */
public class IRGenerator extends VisitorBase {

    // ==================== 内部类 ====================

    /**
     * 循环上下文，记录循环的头部和出口基本块
     */
    private static class LoopContext {
        final IRBasicBlock headerBlock;  // 循环头（continue 目标）
        final IRBasicBlock exitBlock;    // 循环出口（break 目标）
        final IRType resultType;         // 循环结果类型（可为 null 表示无返回值）
        final List<IRValue> breakValues; // break 带出的值
        final List<IRBasicBlock> breakBlocks; // break 所在的基本块

        LoopContext(IRBasicBlock header, IRBasicBlock exit, IRType resultType) {
            this.headerBlock = header;
            this.exitBlock = exit;
            this.resultType = resultType;
            this.breakValues = new ArrayList<>();
            this.breakBlocks = new ArrayList<>();
        }

        void addBreakValue(IRValue value, IRBasicBlock block) {
            breakValues.add(value);
            breakBlocks.add(block);
        }
    }

    /**
     * 函数上下文，用于保存和恢复嵌套函数处理时的状态
     */
    private static class FunctionContext {
        final IRFunction function;
        final IRBasicBlock block;
        final int registerCounter;
        final Map<Symbol, IRValue> capturedSymbols;  // 保存当前符号映射的快照

        FunctionContext(IRFunction function, IRBasicBlock block, int registerCounter,
                        Map<Symbol, IRValue> symbolMap) {
            this.function = function;
            this.block = block;
            this.registerCounter = registerCounter;
            // 保存符号映射的浅拷贝
            this.capturedSymbols = new HashMap<>(symbolMap);
        }
    }

    // ==================== 状态变量 ====================

    private IRModule module;
    private IRFunction currentFunction;
    private IRBasicBlock currentBlock;

    // 符号到 IR 值的映射（变量名 -> 地址）
    private final Map<Symbol, IRValue> symbolMap = new HashMap<>();

    // 常量符号到常量值的映射（用于常量内联）
    private final Map<Symbol, IRConstant> constSymbolMap = new HashMap<>();

    // 循环上下文栈（用于 break/continue）
    private final Deque<LoopContext> loopStack = new ArrayDeque<>();

    // 当前 impl 块的目标类型（用于 self 类型解析）
    private IRStructType currentImplType = null;

    // 常量求值器（复用语义分析阶段的实例）
    private ConstantEvaluator constantEvaluator;

    // 字符串常量计数器
    private int stringConstantCounter = 0;

    // ==================== 入口方法 ====================

    /**
     * 生成 IR 模块
     * @param statements AST 顶层语句列表
     * @return 生成的 IR 模块
     */
    public IRModule generate(List<ASTNode> statements) {
        return generate(statements, new ConstantEvaluator(false));
    }

    /**
     * 生成 IR 模块
     * @param statements AST 顶层语句列表
     * @param constantEvaluator 常量求值器（复用语义分析阶段的实例）
     * @return 生成的 IR 模块
     */
    public IRModule generate(List<ASTNode> statements, ConstantEvaluator constantEvaluator) {
        module = new IRModule();
        this.constantEvaluator = constantEvaluator;

        // Pass 1: 收集类型定义（结构体、枚举）
        for (ASTNode stmt : statements) {
            if (stmt instanceof StructNode) {
                collectStruct((StructNode) stmt);
            } else if (stmt instanceof EnumNode) {
                collectEnum((EnumNode) stmt);
            }
        }

        // Pass 2: 生成函数和全局变量
        for (ASTNode stmt : statements) {
            stmt.accept(this);
        }

        return module;
    }

    // ==================== 类型收集 ====================

    /**
     * 收集结构体类型定义
     */
    private void collectStruct(StructNode node) {
        List<IRType> fieldTypes = new ArrayList<>();
        List<String> fieldNames = new ArrayList<>();
        if (node.fields != null) {
            for (FieldNode field : node.fields) {
                IRType fieldType = convertType(extractType(field.type));
                fieldTypes.add(fieldType);
                fieldNames.add(field.name.name);
            }
        }
        IRStructType structType = new IRStructType(node.name.name, fieldTypes);
        structType.setFieldNames(fieldNames);
        module.addStruct(structType);
    }

    /**
     * 收集枚举类型定义
     */
    private void collectEnum(EnumNode node) {
        Map<String, Integer> variantValues = new LinkedHashMap<>();
        if (node.variants != null) {
            int value = 0;
            for (IdentifierNode variant : node.variants) {
                variantValues.put(variant.name, value);
                value++;
            }
        }
        module.addEnum(node.name.name, IRIntType.I32, variantValues);
    }

    // ==================== Visitor 方法：顶层项 ====================

    @Override
    public void visit(FunctionNode node) {
        // 1. 保存当前函数上下文（支持嵌套函数/闭包）
        FunctionContext savedContext = saveFunctionContext();

        // 2. 获取返回类型
        IRType returnType = node.returnType != null
            ? convertType(extractType(node.returnType))
            : IRVoidType.INSTANCE;

        // 3. 创建 IRFunction 并添加到模块
        // 如果在 impl 块中，生成 mangled 名称：TypeName::methodName
        String funcName = node.name.name;
        if (currentImplType != null) {
            funcName = currentImplType.getName() + "::" + funcName;
        }
        currentFunction = new IRFunction(funcName, returnType);
        module.addFunction(currentFunction);

        // 4. 重置临时变量计数器（每个函数独立编号）
        IRRegister.resetCounter();

        // 5. 创建入口基本块
        IRBasicBlock entryBlock = createBlock("entry");
        setCurrentBlock(entryBlock);

        // 6. 处理 self 参数（如果是方法）
        if (node.selfPara != null) {
            IRType selfType = convertSelfType(node.selfPara);
            IRRegister selfParam = newTemp(selfType, "self");
            currentFunction.addParam(selfParam);

            // 为 self 分配栈空间并存储
            IRRegister selfAddr = newTemp(new IRPtrType(selfType), "self.addr");
            emit(new AllocaInst(selfAddr, selfType));
            emit(new StoreInst(selfParam, selfAddr));
            mapSymbol(node.selfPara.getSymbol(), selfAddr);
        }

        // 7. 处理普通参数
        if (node.parameters != null) {
            for (ParameterNode param : node.parameters) {
                IRType paramType = convertType(param.getParameterType());
                String paramName = getPatternName(param.name);

                // 创建参数寄存器
                IRRegister paramReg = newTemp(paramType, paramName);
                currentFunction.addParam(paramReg);

                // 为参数分配栈空间（便于后续可能的取地址操作）
                IRRegister paramAddr = newTemp(new IRPtrType(paramType), paramName + ".addr");
                emit(new AllocaInst(paramAddr, paramType));
                emit(new StoreInst(paramReg, paramAddr));

                // 记录参数符号到地址的映射
                mapSymbol(getSymbolFromPattern(param.name), paramAddr);
            }
        }

        // 8. 处理函数体
        if (node.body != null) {
            IRValue bodyResult = visitExpr(node.body);

            // 9. 如果函数体没有显式终结，添加返回指令
            if (!currentBlock.isTerminated()) {
                if (returnType instanceof IRVoidType) {
                    emit(new ReturnInst());
                } else {
                    emit(new ReturnInst(bodyResult));
                }
            }
        } else {
            // 函数声明（无函数体），生成空返回
            if (!currentBlock.isTerminated()) {
                emit(new ReturnInst());
            }
        }

        // 10. 恢复之前的函数上下文
        restoreFunctionContext(savedContext);
    }

    @Override
    public void visit(StructNode node) {
        // 结构体类型已在 Pass 1 (collectStruct) 中收集
        // 此处检查是否已注册，如果没有则注册
        if (module.getStruct(node.name.name) == null) {
            collectStruct(node);
        }
    }

    @Override
    public void visit(EnumNode node) {
        // 枚举类型已在 Pass 1 (collectEnum) 中收集
        // 此处检查是否已注册，如果没有则注册
        if (module.getEnum(node.name.name) == null) {
            collectEnum(node);
        }
    }

    @Override
    public void visit(ConstItemNode node) {
        // 1. 获取常量类型
        IRType constType = convertType(extractType(node.type));

        // 2. 使用 ConstantEvaluator 获取常量值
        if (node.value != null) {
            ConstantValue constVal = constantEvaluator.evaluate(node.value);
            if (constVal != null) {
                // 3. 将 ConstantValue 转换为 IRConstant
                IRConstant irConst = convertConstantValue(constVal, constType);

                // 4. 记录常量符号映射（用于后续引用时内联）
                if (irConst != null && node.getSymbol() != null) {
                    constSymbolMap.put(node.getSymbol(), irConst);
                }
            }
        }
    }

    @Override
    public void visit(ImplNode node) {
        // 1. 获取实现的目标类型名
        String targetTypeName = getTypeName(node.typeName);
        IRStructType targetType = module.getStruct(targetTypeName);

        if (targetType == null) {
            throw new RuntimeException("Unknown type in impl: " + targetTypeName);
        }

        // 2. 保存并设置当前 impl 上下文（用于 self 类型解析）
        IRStructType savedImplType = currentImplType;
        currentImplType = targetType;

        // 3. 处理 impl 块中的每个项
        if (node.items != null) {
            for (AssoItemNode item : node.items) {
                if (item.function != null) {
                    // 处理方法（mangled 名称在 visit(FunctionNode) 中自动生成）
                    visit(item.function);
                } else if (item.constant != null) {
                    // 处理关联常量
                    visit(item.constant);
                }
            }
        }

        // 4. 恢复 impl 上下文
        currentImplType = savedImplType;
    }

    // ==================== Visitor 方法：语句 ====================

    @Override
    public void visit(LetStmtNode node) {
        // 1. 获取变量类型
        IRType varType = convertType(node.getVariableType());

        // 2. 获取变量名
        String varName = getPatternName(node.name);

        // 3. 分配栈空间
        IRRegister addr = newTemp(new IRPtrType(varType), varName + ".addr");
        emit(new AllocaInst(addr, varType));

        // 4. 如果有初始化表达式，生成初始化代码
        if (node.value != null) {
            IRValue initValue = visitExpr(node.value);
            emit(new StoreInst(initValue, addr));
        }

        // 5. 记录符号到地址的映射
        mapSymbol(getSymbolFromPattern(node.name), addr);
    }

    @Override
    public void visit(ExprStmtNode node) {
        // 求值表达式，结果被丢弃
        if (node.expr != null) {
            visitExpr(node.expr);
        }
    }

    // ==================== 表达式求值入口 ====================

    /**
     * 求值表达式，返回 IR 值
     * 这是表达式处理的入口方法
     */
    protected IRValue visitExpr(ExprNode expr) {
        if (expr instanceof LiteralExprNode) {
            return visitLiteral((LiteralExprNode) expr);
        } else if (expr instanceof PathExprNode) {
            return visitPath((PathExprNode) expr);
        } else if (expr instanceof ArithExprNode) {
            return visitArith((ArithExprNode) expr);
        } else if (expr instanceof CompExprNode) {
            return visitComp((CompExprNode) expr);
        } else if (expr instanceof LazyExprNode) {
            return visitLazy((LazyExprNode) expr);
        } else if (expr instanceof AssignExprNode) {
            return visitAssign((AssignExprNode) expr);
        } else if (expr instanceof ComAssignExprNode) {
            return visitComAssign((ComAssignExprNode) expr);
        } else if (expr instanceof CallExprNode) {
            return visitCall((CallExprNode) expr);
        } else if (expr instanceof MethodCallExprNode) {
            return visitMethodCall((MethodCallExprNode) expr);
        } else if (expr instanceof FieldExprNode) {
            return visitField((FieldExprNode) expr);
        } else if (expr instanceof IndexExprNode) {
            return visitIndex((IndexExprNode) expr);
        } else if (expr instanceof ArrayExprNode) {
            return visitArrayExpr((ArrayExprNode) expr);
        } else if (expr instanceof StructExprNode) {
            return visitStructExpr((StructExprNode) expr);
        } else if (expr instanceof BorrowExprNode) {
            return visitBorrow((BorrowExprNode) expr);
        } else if (expr instanceof DerefExprNode) {
            return visitDeref((DerefExprNode) expr);
        } else if (expr instanceof NegaExprNode) {
            return visitNega((NegaExprNode) expr);
        } else if (expr instanceof TypeCastExprNode) {
            return visitTypeCast((TypeCastExprNode) expr);
        } else if (expr instanceof BlockExprNode) {
            return visitBlock((BlockExprNode) expr);
        } else if (expr instanceof IfExprNode) {
            return visitIf((IfExprNode) expr);
        } else if (expr instanceof LoopExprNode) {
            return visitLoop((LoopExprNode) expr);
        } else if (expr instanceof BreakExprNode) {
            return visitBreak((BreakExprNode) expr);
        } else if (expr instanceof ContinueExprNode) {
            return visitContinue((ContinueExprNode) expr);
        } else if (expr instanceof ReturnExprNode) {
            return visitReturn((ReturnExprNode) expr);
        }
        // TODO: 其他表达式类型
        throw new UnsupportedOperationException("visitExpr not implemented for: " + expr.getClass());
    }

    // ==================== 表达式处理方法 ====================

    /**
     * 处理字面量表达式
     */
    protected IRValue visitLiteral(LiteralExprNode node) {
        switch (node.literalType) {
            case I32:
            case INT:  // 未确定类型的整数默认为 i32
                return IRConstant.i32(node.value_long);
            case U32:
                return IRConstant.i32(node.value_long);  // u32 也用 i32 表示
            case ISIZE:
                return IRConstant.i64(node.value_long);
            case USIZE:
                return IRConstant.i64(node.value_long);
            case BOOL:
                return IRConstant.i1(node.value_bool);
            case CHAR:
                // char 用 i32 表示（Unicode 码点）
                return IRConstant.i32((int) node.value_string.charAt(0));
            case STRING:
            case CSTRING:
                // 字符串生成全局常量
                return createStringConstant(node.value_string);
            default:
                throw new RuntimeException("Unsupported literal type: " + node.literalType);
        }
    }

    /**
     * 处理路径表达式（变量引用、常量引用、枚举变体等）
     */
    protected IRValue visitPath(PathExprNode node) {
        Symbol symbol = node.getSymbol();
        if (symbol == null) {
            throw new RuntimeException("Unresolved symbol in path expression");
        }

        switch (symbol.getKind()) {
            case LOCAL_VARIABLE:
            case PARAMETER:
                // 变量/参数：从地址加载值
                IRValue addr = getSymbolValue(symbol);
                IRType valType = ((IRPtrType) addr.getType()).getPointee();
                IRRegister result = newTemp(valType);
                emit(new LoadInst(result, addr));
                return result;

            case CONSTANT:
                // 常量：返回常量值（内联）
                IRConstant constVal = getConstSymbolValue(symbol);
                if (constVal == null) {
                    throw new RuntimeException("Constant value not found: " + symbol.getName());
                }
                return constVal;

            case ENUM_VARIANT_CONSTRUCTOR:
                // 枚举变体：返回对应的整数值
                String enumName = symbol.getTypeName();
                IRModule.EnumInfo enumInfo = module.getEnum(enumName);
                if (enumInfo != null) {
                    Integer variantValue = enumInfo.getVariantValue(symbol.getName());
                    if (variantValue != null) {
                        return IRConstant.i32(variantValue);
                    }
                }
                throw new RuntimeException("Enum variant not found: " + symbol.getName());

            case FUNCTION:
                // 函数引用：返回函数指针
                IRFunction func = module.getFunction(symbol.getName());
                if (func != null) {
                    // 构造函数类型
                    IRFunctionType funcType = getFunctionType(func);
                    return new IRGlobal(new IRPtrType(funcType), symbol.getName());
                }
                throw new RuntimeException("Function not found: " + symbol.getName());

            default:
                throw new RuntimeException("Unsupported symbol kind in path: " + symbol.getKind());
        }
    }

    /**
     * 处理算术运算表达式
     */
    protected IRValue visitArith(ArithExprNode node) {
        // 1. 求值左右操作数
        IRValue left = visitExpr(node.left);
        IRValue right = visitExpr(node.right);

        // 2. 获取结果类型（TypeChecker 已设置）
        IRType resultType = convertType(node.getType());

        // 3. 必要时进行类型转换
        left = emitCastIfNeeded(left, resultType);
        right = emitCastIfNeeded(right, resultType);

        // 4. 确定是否为有符号运算
        boolean isSigned = isSignedType(node.getType());

        // 5. 映射运算符
        BinaryOpInst.Op op = mapBinaryOp(node.operator, isSigned);

        // 6. 生成运算指令
        IRRegister result = newTemp(resultType);
        emit(new BinaryOpInst(result, op, left, right));

        return result;
    }

    /**
     * 处理比较运算表达式
     */
    protected IRValue visitComp(CompExprNode node) {
        // 1. 求值左右操作数
        IRValue left = visitExpr(node.left);
        IRValue right = visitExpr(node.right);

        // 2. 找到公共类型并转换
        IRType leftType = left.getType();
        IRType rightType = right.getType();

        if (!leftType.equals(rightType)) {
            IRType commonType = findCommonIRType(leftType, rightType);
            left = emitCastIfNeeded(left, commonType);
            right = emitCastIfNeeded(right, commonType);
        }

        // 3. 确定是否为有符号比较
        boolean isSigned = isSignedType(node.left.getType());

        // 4. 映射比较谓词
        CmpInst.Pred pred = mapCmpPred(node.operator, isSigned);

        // 5. 生成比较指令（结果总是 i1）
        IRRegister result = newTemp(IRIntType.I1);
        emit(new CmpInst(result, pred, left, right));

        return result;
    }

    /**
     * 处理短路逻辑运算表达式（&& 和 ||）
     */
    protected IRValue visitLazy(LazyExprNode node) {
        // 1. 求值左操作数
        IRValue leftVal = visitExpr(node.left);

        // 2. 保存当前块（作为 phi 的来源之一）
        IRBasicBlock leftEndBlock = currentBlock;

        // 3. 创建基本块
        IRBasicBlock rightBlock = createBlock("lazy.right");
        IRBasicBlock mergeBlock = createBlock("lazy.merge");

        // 4. 根据运算符生成条件分支
        if (node.operator == oper_t.LOGICAL_AND) {
            // &&: 左为 true 才求值右边，左为 false 直接跳到 merge
            emit(new CondBranchInst(leftVal, rightBlock, mergeBlock));
        } else {
            // ||: 左为 false 才求值右边，左为 true 直接跳到 merge
            emit(new CondBranchInst(leftVal, mergeBlock, rightBlock));
        }

        // 5. 求值右操作数
        setCurrentBlock(rightBlock);
        IRValue rightVal = visitExpr(node.right);
        IRBasicBlock rightEndBlock = currentBlock;  // 保存（可能因嵌套表达式而改变）
        emit(new BranchInst(mergeBlock));

        // 6. 合并结果（使用 phi）
        setCurrentBlock(mergeBlock);
        IRRegister result = newTemp(IRIntType.I1);

        PhiInst phi = new PhiInst(result);
        if (node.operator == oper_t.LOGICAL_AND) {
            // &&: 从左边短路来的是 false，从右边来的是右值
            phi.addIncoming(IRConstant.i1(false), leftEndBlock);
            phi.addIncoming(rightVal, rightEndBlock);
        } else {
            // ||: 从左边短路来的是 true，从右边来的是右值
            phi.addIncoming(IRConstant.i1(true), leftEndBlock);
            phi.addIncoming(rightVal, rightEndBlock);
        }
        emit(phi);

        return result;
    }

    /**
     * 处理赋值表达式
     */
    protected IRValue visitAssign(AssignExprNode node) {
        // 1. 获取左值地址
        IRValue addr = visitLValue(node.left);

        // 2. 求值右边表达式
        IRValue value = visitExpr(node.right);

        // 3. 生成 store 指令
        emit(new StoreInst(value, addr));

        // 4. 赋值表达式返回 unit 类型
        return null;
    }

    /**
     * 处理复合赋值表达式（+=, -=, *= 等）
     */
    protected IRValue visitComAssign(ComAssignExprNode node) {
        // 1. 获取左值地址
        IRValue addr = visitLValue(node.left);

        // 2. 加载当前值
        IRType valType = ((IRPtrType) addr.getType()).getPointee();
        IRRegister oldVal = newTemp(valType);
        emit(new LoadInst(oldVal, addr));

        // 3. 求值右边表达式
        IRValue rightVal = visitExpr(node.right);

        // 4. 必要时进行类型转换（右操作数转换为左操作数类型）
        rightVal = emitCastIfNeeded(rightVal, valType);

        // 5. 执行运算（将复合赋值运算符映射到基本运算符）
        boolean isSigned = isSignedType(node.left.getType());
        BinaryOpInst.Op op = mapComAssignOp(node.operator, isSigned);
        IRRegister newVal = newTemp(valType);
        emit(new BinaryOpInst(newVal, op, oldVal, rightVal));

        // 6. 存回
        emit(new StoreInst(newVal, addr));

        return null;
    }

    /**
     * 处理函数调用表达式
     */
    protected IRValue visitCall(CallExprNode node) {
        // 1. 获取返回类型
        IRType returnType = convertType(node.getType());

        // 2. 求值所有参数
        List<IRValue> args = new ArrayList<>();
        if (node.arguments != null) {
            for (ExprNode arg : node.arguments) {
                args.add(visitExpr(arg));
            }
        }

        // 3. 判断是直接调用还是间接调用
        if (node.function instanceof PathExprNode) {
            PathExprNode pathExpr = (PathExprNode) node.function;
            Symbol symbol = pathExpr.getSymbol();
            if (symbol != null && symbol.getKind() == SymbolKind.FUNCTION) {
                // 直接调用：使用函数名
                String funcName = symbol.getName();
                if (returnType instanceof IRVoidType) {
                    emit(new CallInst(funcName, args));
                    return null;
                } else {
                    IRRegister result = newTemp(returnType);
                    emit(new CallInst(result, funcName, args));
                    return result;
                }
            }
        }

        // 4. 间接调用：通过函数指针
        IRValue callee = visitExpr(node.function);
        if (returnType instanceof IRVoidType) {
            emit(new CallInst(callee, args));
            return null;
        } else {
            IRRegister result = newTemp(returnType);
            emit(new CallInst(result, callee, args));
            return result;
        }
    }

    /**
     * 处理方法调用表达式
     */
    protected IRValue visitMethodCall(MethodCallExprNode node) {
        // 1. 获取接收者（作为 self 参数）
        IRValue receiver = visitExprAsAddr(node.receiver);

        // 2. 构造方法名（TypeName::methodName）
        String typeName = getTypeNameFromExpr(node.receiver);
        String methodName = typeName + "::" + node.methodName.name.name;

        // 3. 获取方法的返回类型
        IRType returnType = convertType(node.getType());

        // 4. 求值其他参数
        List<IRValue> args = new ArrayList<>();
        args.add(receiver);  // self 作为第一个参数
        if (node.arguments != null) {
            for (ExprNode arg : node.arguments) {
                args.add(visitExpr(arg));
            }
        }

        // 5. 生成直接调用指令
        if (returnType instanceof IRVoidType) {
            emit(new CallInst(methodName, args));
            return null;
        } else {
            IRRegister result = newTemp(returnType);
            emit(new CallInst(result, methodName, args));
            return result;
        }
    }

    /**
     * 处理字段访问表达式
     */
    protected IRValue visitField(FieldExprNode node) {
        // 1. 获取字段地址
        IRValue fieldAddr = visitFieldLValue(node);

        // 2. 加载字段值
        IRType fieldType = ((IRPtrType) fieldAddr.getType()).getPointee();
        IRRegister result = newTemp(fieldType);
        emit(new LoadInst(result, fieldAddr));

        return result;
    }

    /**
     * 处理数组索引表达式
     */
    protected IRValue visitIndex(IndexExprNode node) {
        // 1. 获取元素地址
        IRValue elemAddr = visitIndexLValue(node);

        // 2. 加载元素值
        IRType elemType = ((IRPtrType) elemAddr.getType()).getPointee();
        IRRegister result = newTemp(elemType);
        emit(new LoadInst(result, elemAddr));

        return result;
    }

    /**
     * 处理数组表达式
     */
    protected IRValue visitArrayExpr(ArrayExprNode node) {
        // 获取数组类型
        IRArrayType arrayType = (IRArrayType) convertType(node.getType());
        IRType elemType = arrayType.getElementType();
        int arraySize = arrayType.getSize();

        // 分配数组空间
        IRRegister arrayAddr = newTemp(new IRPtrType(arrayType), "arr");
        emit(new AllocaInst(arrayAddr, arrayType));

        if (node.elements != null && !node.elements.isEmpty()) {
            // 形式 1: [e1, e2, e3, ...]
            for (int i = 0; i < node.elements.size(); i++) {
                // 计算元素地址
                IRRegister elemAddr = newTemp(new IRPtrType(elemType));
                emit(new GEPInst(elemAddr, arrayAddr, Arrays.asList(
                    IRConstant.i32(0),
                    IRConstant.i32(i)
                )));

                // 求值并存储元素
                IRValue elemVal = visitExpr(node.elements.get(i));
                emit(new StoreInst(elemVal, elemAddr));
            }
        } else if (node.repeatedElement != null && node.size != null) {
            // 形式 2: [expr; size]
            IRValue initVal = visitExpr(node.repeatedElement);

            // 初始化所有元素
            for (int i = 0; i < arraySize; i++) {
                IRRegister elemAddr = newTemp(new IRPtrType(elemType));
                emit(new GEPInst(elemAddr, arrayAddr, Arrays.asList(
                    IRConstant.i32(0),
                    IRConstant.i32(i)
                )));
                emit(new StoreInst(initVal, elemAddr));
            }
        }
        // 空数组不需要初始化

        // 加载数组值（返回数组本身）
        IRRegister result = newTemp(arrayType);
        emit(new LoadInst(result, arrayAddr));
        return result;
    }

    /**
     * 处理结构体表达式
     */
    protected IRValue visitStructExpr(StructExprNode node) {
        // 1. 获取结构体类型
        String structName = node.structName.name.name;
        IRStructType structType = module.getStruct(structName);
        if (structType == null) {
            throw new RuntimeException("Unknown struct type: " + structName);
        }

        // 2. 分配结构体空间
        IRRegister structAddr = newTemp(new IRPtrType(structType), structName.toLowerCase());
        emit(new AllocaInst(structAddr, structType));

        // 3. 初始化各字段
        if (node.fieldValues != null) {
            for (FieldValNode fieldVal : node.fieldValues) {
                String fieldName = fieldVal.fieldName.name;
                int fieldIndex = structType.getFieldIndex(fieldName);
                IRType fieldType = structType.getFieldType(fieldIndex);

                // 计算字段地址
                IRRegister fieldAddr = newTemp(new IRPtrType(fieldType));
                emit(new GEPInst(fieldAddr, structAddr, Arrays.asList(
                    IRConstant.i32(0),
                    IRConstant.i32(fieldIndex)
                )));

                // 求值并存储字段值
                IRValue fieldValue = visitExpr(fieldVal.value);
                emit(new StoreInst(fieldValue, fieldAddr));
            }
        }

        // 4. 加载结构体值
        IRRegister result = newTemp(structType);
        emit(new LoadInst(result, structAddr));
        return result;
    }

    /**
     * 处理借用表达式（& 和 &mut）
     */
    protected IRValue visitBorrow(BorrowExprNode node) {
        // 借用表达式返回内部表达式的地址
        // &x 和 &mut x 在 IR 层面都是获取地址
        IRValue addr = visitExprAsAddr(node.innerExpr);

        // 如果是双重引用 &&x，需要再取一次地址
        if (node.isDoubleReference) {
            IRRegister temp = newTemp(new IRPtrType(addr.getType()));
            emit(new AllocaInst(temp, addr.getType()));
            emit(new StoreInst(addr, temp));
            return temp;
        }

        return addr;
    }

    /**
     * 处理解引用表达式（*）
     */
    protected IRValue visitDeref(DerefExprNode node) {
        // 1. 求值内部表达式（得到指针）
        IRValue ptr = visitExpr(node.innerExpr);

        // 2. 从指针加载值
        IRType pointeeType = ((IRPtrType) ptr.getType()).getPointee();
        IRRegister result = newTemp(pointeeType);
        emit(new LoadInst(result, ptr));

        return result;
    }

    /**
     * 处理取反表达式（! 和 -）
     */
    protected IRValue visitNega(NegaExprNode node) {
        // 1. 求值内部表达式
        IRValue inner = visitExpr(node.innerExpr);
        IRType type = inner.getType();

        IRRegister result = newTemp(type);

        if (node.isLogical) {
            // ! 运算符
            if (type == IRIntType.I1) {
                // bool 类型：逻辑取反，xor x, true
                emit(new BinaryOpInst(result, BinaryOpInst.Op.XOR, inner, IRConstant.i1(true)));
            } else {
                // 整数类型：按位取反，xor x, -1
                emit(new BinaryOpInst(result, BinaryOpInst.Op.XOR, inner, new IRConstant(type, -1)));
            }
        } else {
            // 算术取反 -x：使用 sub 0, x
            IRValue zero;
            if (type == IRIntType.I32) {
                zero = IRConstant.i32(0);
            } else if (type == IRIntType.I64) {
                zero = IRConstant.i64(0);
            } else if (type == IRIntType.I8) {
                zero = IRConstant.i8(0);
            } else {
                zero = new IRConstant(type, 0);
            }
            emit(new BinaryOpInst(result, BinaryOpInst.Op.SUB, zero, inner));
        }

        return result;
    }

    /**
     * 处理类型转换表达式（as）
     */
    protected IRValue visitTypeCast(TypeCastExprNode node) {
        // 1. 求值源表达式
        IRValue source = visitExpr(node.expr);
        IRType sourceType = source.getType();

        // 2. 获取目标类型
        IRType targetType = convertType(extractType(node.type));

        // 3. 如果类型相同，直接返回
        if (sourceType.equals(targetType)) {
            return source;
        }

        IRRegister result = newTemp(targetType);

        // 4. 根据源类型和目标类型选择转换操作
        if (sourceType instanceof IRIntType && targetType instanceof IRIntType) {
            // 整数之间的转换
            int sourceBits = ((IRIntType) sourceType).getBitWidth();
            int targetBits = ((IRIntType) targetType).getBitWidth();

            if (targetBits > sourceBits) {
                // 扩展：根据源类型是否有符号选择 sext 或 zext
                boolean isSigned = isSignedType(node.expr.getType());
                CastInst.Op op = isSigned ? CastInst.Op.SEXT : CastInst.Op.ZEXT;
                emit(new CastInst(result, op, source, targetType));
            } else if (targetBits < sourceBits) {
                // 截断
                emit(new CastInst(result, CastInst.Op.TRUNC, source, targetType));
            } else {
                // 位宽相同但类型不同（如 i32 和 u32），直接使用 bitcast
                emit(new CastInst(result, CastInst.Op.BITCAST, source, targetType));
            }
        } else if (sourceType instanceof IRPtrType && targetType instanceof IRPtrType) {
            // 指针之间的转换：使用 bitcast
            emit(new CastInst(result, CastInst.Op.BITCAST, source, targetType));
        } else if (sourceType instanceof IRPtrType && targetType instanceof IRIntType) {
            // 指针转整数：使用 ptrtoint
            emit(new CastInst(result, CastInst.Op.PTRTOINT, source, targetType));
        } else if (sourceType instanceof IRIntType && targetType instanceof IRPtrType) {
            // 整数转指针：使用 inttoptr
            emit(new CastInst(result, CastInst.Op.INTTOPTR, source, targetType));
        } else {
            throw new RuntimeException("Unsupported type cast: " + sourceType + " -> " + targetType);
        }

        return result;
    }

    /**
     * 处理块表达式
     */
    protected IRValue visitBlock(BlockExprNode node) {
        // 1. 先扫描块中的类型和函数定义，保存可能被覆盖的旧值
        Map<String, IRStructType> savedStructs = new HashMap<>();
        Map<String, IRModule.EnumInfo> savedEnums = new HashMap<>();
        Map<String, IRFunction> savedFunctions = new HashMap<>();

        if (node.statements != null) {
            for (StmtNode stmt : node.statements) {
                if (stmt instanceof StructNode) {
                    StructNode structNode = (StructNode) stmt;
                    String name = structNode.name.name;
                    // 保存旧值（可能为 null）
                    savedStructs.put(name, module.getStruct(name));
                    collectStruct(structNode);
                } else if (stmt instanceof EnumNode) {
                    EnumNode enumNode = (EnumNode) stmt;
                    String name = enumNode.name.name;
                    // 保存旧值（可能为 null）
                    savedEnums.put(name, module.getEnum(name));
                    collectEnum(enumNode);
                } else if (stmt instanceof FunctionNode) {
                    FunctionNode funcNode = (FunctionNode) stmt;
                    String name = funcNode.name.name;
                    // 保存旧值（可能为 null）
                    savedFunctions.put(name, module.getFunction(name));
                    // 函数会在 visit 时添加到 module
                }
            }
        }

        // 2. 处理块中的所有语句
        IRValue result = null;
        if (node.statements != null) {
            for (StmtNode stmt : node.statements) {
                stmt.accept(this);
                // 如果当前块已终结（遇到 return/break/continue），停止处理后续语句
                if (currentBlock.isTerminated()) {
                    break;
                }
            }
        }

        // 3. 处理返回值表达式（如果有）
        if (!currentBlock.isTerminated() && node.returnValue != null) {
            result = visitExpr(node.returnValue);
        }

        // 4. 恢复 module 状态
        for (Map.Entry<String, IRStructType> entry : savedStructs.entrySet()) {
            if (entry.getValue() == null) {
                module.removeStruct(entry.getKey());
            } else {
                module.setStruct(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<String, IRModule.EnumInfo> entry : savedEnums.entrySet()) {
            if (entry.getValue() == null) {
                module.removeEnum(entry.getKey());
            } else {
                module.setEnum(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<String, IRFunction> entry : savedFunctions.entrySet()) {
            if (entry.getValue() == null) {
                module.removeFunction(entry.getKey());
            } else {
                module.setFunction(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * 处理 if 表达式
     */
    protected IRValue visitIf(IfExprNode node) {
        // 1. 求值条件
        IRValue cond = visitExpr(node.condition);

        // 2. 创建基本块
        IRBasicBlock thenBlock = createBlock("if.then");
        IRBasicBlock elseBlock = createBlock("if.else");
        IRBasicBlock mergeBlock = createBlock("if.merge");

        // 3. 生成条件分支
        emit(new CondBranchInst(cond, thenBlock, elseBlock));

        // 4. 处理 then 分支
        setCurrentBlock(thenBlock);
        IRValue thenVal = visitBlock(node.thenBranch);
        IRBasicBlock thenEndBlock = currentBlock;  // 保存（可能因嵌套而改变）
        if (!currentBlock.isTerminated()) {
            emit(new BranchInst(mergeBlock));
        }

        // 5. 处理 else 分支
        setCurrentBlock(elseBlock);
        IRValue elseVal = null;
        IRBasicBlock elseEndBlock = elseBlock;

        if (node.elseifBranch != null) {
            // else if 分支
            elseVal = visitIf(node.elseifBranch);
            elseEndBlock = currentBlock;
        } else if (node.elseBranch != null) {
            // else 分支
            elseVal = visitBlock((BlockExprNode) node.elseBranch);
            elseEndBlock = currentBlock;
        }

        if (!currentBlock.isTerminated()) {
            emit(new BranchInst(mergeBlock));
        }

        // 6. 合并块
        setCurrentBlock(mergeBlock);

        // 7. 如果 if 表达式有值，使用 phi 合并
        IRType resultType = convertType(node.getType());
        if (!(resultType instanceof IRVoidType) && thenVal != null) {
            IRRegister result = newTemp(resultType);
            PhiInst phi = new PhiInst(result);

            // 只有当分支没有提前终结时才添加 incoming
            if (!thenEndBlock.isTerminated() || thenEndBlock.getTerminator() instanceof BranchInst) {
                phi.addIncoming(thenVal, thenEndBlock);
            }
            if (elseVal != null && (!elseEndBlock.isTerminated() || elseEndBlock.getTerminator() instanceof BranchInst)) {
                phi.addIncoming(elseVal, elseEndBlock);
            }

            emit(phi);
            return result;
        }

        return null;
    }

    /**
     * 处理循环表达式（loop 和 while）
     */
    protected IRValue visitLoop(LoopExprNode node) {
        // 1. 获取循环结果类型
        IRType resultType = convertType(node.getType());
        boolean hasValue = !(resultType instanceof IRVoidType);

        // 2. 创建基本块
        IRBasicBlock headerBlock = createBlock("loop.header");
        IRBasicBlock bodyBlock = createBlock("loop.body");
        IRBasicBlock exitBlock = createBlock("loop.exit");

        // 3. 跳转到循环头
        emit(new BranchInst(headerBlock));

        // 4. 循环头：检查条件（while）或直接进入循环体（loop）
        setCurrentBlock(headerBlock);

        if (node.isInfinite) {
            // loop：无条件进入循环体
            emit(new BranchInst(bodyBlock));
        } else {
            // while：检查条件
            IRValue cond = visitExpr(node.condition);
            emit(new CondBranchInst(cond, bodyBlock, exitBlock));
        }

        // 5. 压入循环上下文（用于 break/continue）
        pushLoopContext(headerBlock, exitBlock, hasValue ? resultType : null);

        // 6. 处理循环体
        setCurrentBlock(bodyBlock);
        visitBlock(node.body);

        // 7. 循环体结束后跳回循环头
        if (!currentBlock.isTerminated()) {
            emit(new BranchInst(headerBlock));
        }

        // 8. 弹出循环上下文并获取 break 值
        LoopContext loopCtx = popLoopContext();

        // 9. 设置当前块为出口块
        setCurrentBlock(exitBlock);

        // 10. 如果循环有返回值，使用 phi 合并所有 break 的值
        if (hasValue && !loopCtx.breakValues.isEmpty()) {
            IRRegister result = newTemp(resultType);
            PhiInst phi = new PhiInst(result);

            for (int i = 0; i < loopCtx.breakValues.size(); i++) {
                phi.addIncoming(loopCtx.breakValues.get(i), loopCtx.breakBlocks.get(i));
            }

            emit(phi);
            return result;
        }

        return null;
    }

    /**
     * 处理 break 表达式
     */
    protected IRValue visitBreak(BreakExprNode node) {
        // 1. 获取当前循环上下文
        LoopContext loopCtx = getCurrentLoopContext();

        // 2. 如果 break 带有值，求值并记录
        if (node.value != null) {
            IRValue breakVal = visitExpr(node.value);
            loopCtx.addBreakValue(breakVal, currentBlock);
        }

        // 3. 跳转到循环出口
        emit(new BranchInst(loopCtx.exitBlock));

        // break 是终结指令，不返回值（控制流已转移）
        return null;
    }

    /**
     * 处理 continue 表达式
     */
    protected IRValue visitContinue(ContinueExprNode node) {
        // 跳转到循环头
        IRBasicBlock header = getCurrentLoopHeader();
        emit(new BranchInst(header));

        // continue 是终结指令，不返回值（控制流已转移）
        return null;
    }

    /**
     * 处理 return 表达式
     */
    protected IRValue visitReturn(ReturnExprNode node) {
        // 1. 如果有返回值，求值
        if (node.value != null) {
            IRValue retVal = visitExpr(node.value);
            emit(new ReturnInst(retVal));
        } else {
            // 无返回值（void）
            emit(new ReturnInst());
        }

        // return 是终结指令，不返回值（控制流已转移）
        return null;
    }

    // ==================== 辅助方法：IR 构建 ====================

    /**
     * 创建新的临时变量
     */
    protected IRRegister newTemp(IRType type) {
        return new IRRegister(type);
    }

    /**
     * 创建带提示名的临时变量
     */
    protected IRRegister newTemp(IRType type, String hint) {
        return new IRRegister(type, hint);
    }

    /**
     * 发射指令到当前基本块
     */
    protected void emit(IRInstruction inst) {
        if (currentBlock == null) {
            throw new IllegalStateException("No current block to emit instruction");
        }
        currentBlock.addInstruction(inst);
    }

    /**
     * 创建新的基本块并添加到当前函数
     */
    protected IRBasicBlock createBlock(String name) {
        IRBasicBlock block = new IRBasicBlock(name);
        currentFunction.addBlock(block);
        return block;
    }

    /**
     * 设置当前基本块
     */
    protected void setCurrentBlock(IRBasicBlock block) {
        currentBlock = block;
    }

    // ==================== 辅助方法：上下文管理 ====================

    /**
     * 保存当前函数上下文（进入嵌套函数/闭包前调用）
     * @return 保存的上下文，用于后续恢复
     */
    protected FunctionContext saveFunctionContext() {
        return new FunctionContext(
            currentFunction,
            currentBlock,
            IRRegister.getCounter(),
            symbolMap
        );
    }

    /**
     * 恢复函数上下文（退出嵌套函数/闭包后调用）
     * @param context 之前保存的上下文
     */
    protected void restoreFunctionContext(FunctionContext context) {
        currentFunction = context.function;
        currentBlock = context.block;
        IRRegister.setCounter(context.registerCounter);
        // 恢复符号映射
        symbolMap.clear();
        symbolMap.putAll(context.capturedSymbols);
    }

    /**
     * 压入循环上下文
     */
    protected void pushLoopContext(IRBasicBlock header, IRBasicBlock exit, IRType resultType) {
        loopStack.push(new LoopContext(header, exit, resultType));
    }

    /**
     * 弹出循环上下文
     */
    protected LoopContext popLoopContext() {
        return loopStack.pop();
    }

    /**
     * 获取当前循环的头部基本块（continue 目标）
     */
    protected IRBasicBlock getCurrentLoopHeader() {
        if (loopStack.isEmpty()) {
            throw new IllegalStateException("Not inside a loop");
        }
        return loopStack.peek().headerBlock;
    }

    /**
     * 获取当前循环的出口基本块（break 目标）
     */
    protected IRBasicBlock getCurrentLoopExit() {
        if (loopStack.isEmpty()) {
            throw new IllegalStateException("Not inside a loop");
        }
        return loopStack.peek().exitBlock;
    }

    /**
     * 获取当前循环上下文
     */
    protected LoopContext getCurrentLoopContext() {
        if (loopStack.isEmpty()) {
            throw new IllegalStateException("Not inside a loop");
        }
        return loopStack.peek();
    }

    // ==================== 辅助方法：左值处理 ====================

    /**
     * 获取左值的地址（不加载值）
     */
    protected IRValue visitLValue(ExprNode expr) {
        if (expr instanceof PathExprNode) {
            // 变量：直接返回地址
            Symbol symbol = ((PathExprNode) expr).getSymbol();
            return getSymbolValue(symbol);
        } else if (expr instanceof DerefExprNode) {
            // 解引用：求值内部表达式得到地址
            return visitExpr(((DerefExprNode) expr).innerExpr);
        } else if (expr instanceof FieldExprNode) {
            // 字段访问：计算字段地址
            return visitFieldLValue((FieldExprNode) expr);
        } else if (expr instanceof IndexExprNode) {
            // 数组索引：计算元素地址
            return visitIndexLValue((IndexExprNode) expr);
        }
        throw new RuntimeException("Invalid lvalue: " + expr.getClass());
    }

    /**
     * 获取表达式的地址（用于需要地址的场景，如方法调用的 self）
     */
    protected IRValue visitExprAsAddr(ExprNode expr) {
        // 如果是左值表达式，直接使用 visitLValue
        if (expr instanceof PathExprNode || expr instanceof FieldExprNode ||
            expr instanceof IndexExprNode || expr instanceof DerefExprNode) {
            return visitLValue(expr);
        }
        // 其他情况：先求值，再存到临时变量取地址
        IRValue value = visitExpr(expr);
        IRRegister temp = newTemp(new IRPtrType(value.getType()));
        emit(new AllocaInst(temp, value.getType()));
        emit(new StoreInst(value, temp));
        return temp;
    }

    /**
     * 获取字段的地址（用于左值）
     */
    protected IRValue visitFieldLValue(FieldExprNode node) {
        IRValue baseAddr = visitExprAsAddr(node.receiver);
        IRStructType structType = getStructType(node.receiver.getType());
        int fieldIndex = structType.getFieldIndex(node.fieldName.name);

        IRRegister fieldAddr = newTemp(new IRPtrType(structType.getFieldType(fieldIndex)));
        emit(new GEPInst(fieldAddr, baseAddr, Arrays.asList(
            IRConstant.i32(0),
            IRConstant.i32(fieldIndex)
        )));

        return fieldAddr;
    }

    /**
     * 获取数组元素的地址（用于左值）
     */
    protected IRValue visitIndexLValue(IndexExprNode node) {
        IRValue baseAddr = visitExprAsAddr(node.array);
        IRValue index = visitExpr(node.index);

        IRArrayType arrayType = (IRArrayType) ((IRPtrType) baseAddr.getType()).getPointee();
        IRType elemType = arrayType.getElementType();

        IRRegister elemAddr = newTemp(new IRPtrType(elemType));
        emit(new GEPInst(elemAddr, baseAddr, Arrays.asList(
            IRConstant.i32(0),
            index
        )));

        return elemAddr;
    }

    // ==================== 辅助方法：类型转换 ====================

    /**
     * 将 AST 类型转换为 IR 类型
     */
    protected IRType convertType(Type astType) {
        if (astType instanceof PrimitiveType) {
            PrimitiveType pt = (PrimitiveType) astType;
            switch (pt.getKind()) {
                case I32:
                case U32:
                    return IRIntType.I32;
                case BOOL:
                    return IRIntType.I1;
                case CHAR:
                    return IRIntType.I32;  // char 用 i32 表示
                case USIZE:
                case ISIZE:
                    return IRIntType.I64;
                default:
                    throw new RuntimeException("Unsupported primitive type: " + pt);
            }
        } else if (astType instanceof ReferenceType) {
            ReferenceType rt = (ReferenceType) astType;
            return new IRPtrType(convertType(rt.getInnerType()));
        } else if (astType instanceof ArrayType) {
            ArrayType at = (ArrayType) astType;
            return new IRArrayType(convertType(at.getElementType()), at.getSize());
        } else if (astType instanceof StructType) {
            StructType st = (StructType) astType;
            IRStructType irStruct = module.getStruct(st.getName());
            if (irStruct == null) {
                throw new RuntimeException("Unknown struct type: " + st.getName());
            }
            return irStruct;
        } else if (astType instanceof EnumType) {
            // 枚举类型在 IR 中用 i32 表示
            return IRIntType.I32;
        } else if (astType instanceof UnitType) {
            return IRVoidType.INSTANCE;
        } else if (astType instanceof NeverType) {
            return IRVoidType.INSTANCE;  // never 类型在 IR 层面用 void 表示
        }
        throw new RuntimeException("Unknown type: " + astType);
    }

    /**
     * 从 TypeExprNode 提取 Type
     * 简单实现：处理基本类型
     */
    protected Type extractType(TypeExprNode typeExpr) {
        if (typeExpr instanceof TypePathExprNode) {
            TypePathExprNode pathType = (TypePathExprNode) typeExpr;
            if (pathType.path != null && pathType.path.name != null) {
                String typeName = pathType.path.name.name;
                switch (typeName) {
                    case "i32":
                        return PrimitiveType.getI32Type();
                    case "u32":
                        return PrimitiveType.getU32Type();
                    case "isize":
                        return PrimitiveType.getIsizeType();
                    case "usize":
                        return PrimitiveType.getUsizeType();
                    case "bool":
                        return PrimitiveType.getBoolType();
                    case "char":
                        return PrimitiveType.getCharType();
                    case "str":
                        return PrimitiveType.getStrType();
                    case "String":
                        return PrimitiveType.getStringType();
                    default:
                        // 可能是自定义类型（结构体、枚举等）
                        throw new RuntimeException("Unknown type: " + typeName);
                }
            }
        }
        throw new RuntimeException("Cannot extract type from: " + typeExpr.getClass());
    }
    /**
     * 从表达式获取类型名（用于方法调用）
     */
    protected String getTypeNameFromExpr(ExprNode expr) {
        IRStructType structType = getStructType(expr.getType());
        if (structType != null) {
            return structType.getName();
        }
        return expr.getType().toString();
    }

    /**
     * 从 AST 类型获取 IR 结构体类型
     */
    protected IRStructType getStructType(Type type) {
        while (type instanceof ReferenceType) {
            type = ((ReferenceType) type).getInnerType();
        }
        if (type instanceof StructType) {
            return module.getStruct(((StructType) type).getName());
        }
        throw new RuntimeException("Cannot get struct type from: " + type);
    }


    /**
     * 必要时生成类型转换指令
     */
    protected IRValue emitCastIfNeeded(IRValue value, IRType targetType) {
        IRType sourceType = value.getType();

        // 类型相同，无需转换
        if (sourceType.equals(targetType)) {
            return value;
        }

        // 整数类型之间的转换
        if (sourceType instanceof IRIntType && targetType instanceof IRIntType) {
            int sourceBits = ((IRIntType) sourceType).getBitWidth();
            int targetBits = ((IRIntType) targetType).getBitWidth();

            IRRegister result = newTemp(targetType);

            if (targetBits > sourceBits) {
                // 扩展：根据源类型选择 sext 或 zext
                // 这里简化处理，默认使用有符号扩展
                emit(new CastInst(result, CastInst.Op.SEXT, value, targetType));
            } else {
                // 截断
                emit(new CastInst(result, CastInst.Op.TRUNC, value, targetType));
            }

            return result;
        }

        throw new RuntimeException("Unsupported cast: " + sourceType + " -> " + targetType);
    }

    /**
     * 找到两个 IR 类型的公共类型
     */
    protected IRType findCommonIRType(IRType type1, IRType type2) {
        if (type1.equals(type2)) {
            return type1;
        }

        // 整数类型：选择较大的类型
        if (type1 instanceof IRIntType && type2 instanceof IRIntType) {
            int bits1 = ((IRIntType) type1).getBitWidth();
            int bits2 = ((IRIntType) type2).getBitWidth();
            return bits1 >= bits2 ? type1 : type2;
        }

        throw new RuntimeException("Cannot find common type: " + type1 + " and " + type2);
    }

    /**
     * 判断 AST 类型是否为有符号类型
     */
    protected boolean isSignedType(Type type) {
        if (type instanceof PrimitiveType) {
            PrimitiveType pt = (PrimitiveType) type;
            switch (pt.getKind()) {
                case I32:
                case ISIZE:
                    return true;
                case U32:
                case USIZE:
                case BOOL:
                case CHAR:
                    return false;
                default:
                    return true;  // 默认有符号
            }
        }
        return true;
    }


    /**
     * 从 IRFunction 获取函数类型
     */
    protected IRFunctionType getFunctionType(IRFunction func) {
        List<IRType> paramTypes = new ArrayList<>();
        for (IRRegister param : func.getParams()) {
            paramTypes.add(param.getType());
        }
        return new IRFunctionType(func.getReturnType(), paramTypes);
    }
    /**
     * 转换 self 参数类型
     */
    protected IRType convertSelfType(SelfParaNode selfPara) {
        // 如果有显式类型，使用显式类型
        if (selfPara.type != null) {
            return convertType(extractType(selfPara.type));
        }

        // 否则从当前 impl 上下文推断
        if (currentImplType == null) {
            throw new RuntimeException("self outside of impl block");
        }

        // self / &self / &mut self 都用指针传递
        return new IRPtrType(currentImplType);
    }
    /**
     * 从 TypeExprNode 获取类型名
     */
    protected String getTypeName(TypeExprNode typeExpr) {
        if (typeExpr instanceof TypePathExprNode) {
            TypePathExprNode pathType = (TypePathExprNode) typeExpr;
            if (pathType.path != null) {
                switch (pathType.path.patternType) {
                    case IDENT:
                        if (pathType.path.name != null) {
                            return pathType.path.name.name;
                        }
                        break;
                    case SELF:
                        return "self";
                    case SELF_TYPE:
                        // Self 类型：返回当前 impl 块的目标类型名
                        if (currentImplType != null) {
                            return currentImplType.getName();
                        }
                        return "Self";
                }
            }
        }
        throw new RuntimeException("Cannot get type name from: " + typeExpr.getClass());
    }


    // ==================== 辅助方法：符号管理 ====================

    /**
     * 映射符号到 IR 值
     */
    protected void mapSymbol(Symbol symbol, IRValue value) {
        symbolMap.put(symbol, value);
    }

    /**
     * 获取符号对应的 IR 值
     */
    protected IRValue getSymbolValue(Symbol symbol) {
        IRValue value = symbolMap.get(symbol);
        if (value == null) {
            throw new RuntimeException("Unknown symbol: " + symbol);
        }
        return value;
    }

    /**
     * 从 PatternNode 获取变量名
     */
    protected String getPatternName(PatternNode pattern) {
        if (pattern instanceof IdPatNode) {
            return ((IdPatNode) pattern).name.name;
        }
        return "tmp";
    }

    /**
     * 从 PatternNode 获取符号
     */
    protected Symbol getSymbolFromPattern(PatternNode pattern) {
        if (pattern instanceof IdPatNode) {
            return ((IdPatNode) pattern).name.getSymbol();
        }
        throw new RuntimeException("Cannot get symbol from pattern: " + pattern.getClass());
    }

    /**
     * 获取常量符号的值
     */
    protected IRConstant getConstSymbolValue(Symbol symbol) {
        return constSymbolMap.get(symbol);
    }

    // ==================== 辅助方法：运算符映射 ====================

    /**
     * 将 AST 运算符映射到 IR 二元运算
     */
    protected BinaryOpInst.Op mapBinaryOp(oper_t op, boolean isSigned) {
        switch (op) {
            case ADD: return BinaryOpInst.Op.ADD;
            case SUB: return BinaryOpInst.Op.SUB;
            case MUL: return BinaryOpInst.Op.MUL;
            case DIV: return isSigned ? BinaryOpInst.Op.SDIV : BinaryOpInst.Op.UDIV;
            case MOD: return isSigned ? BinaryOpInst.Op.SREM : BinaryOpInst.Op.UREM;
            case BITAND: return BinaryOpInst.Op.AND;
            case BITOR: return BinaryOpInst.Op.OR;
            case BITXOR: return BinaryOpInst.Op.XOR;
            case SHL: return BinaryOpInst.Op.SHL;
            case SHR: return isSigned ? BinaryOpInst.Op.ASHR : BinaryOpInst.Op.LSHR;
            default:
                throw new RuntimeException("Unknown binary operator: " + op);
        }
    }

    /**
     * 将 AST 比较运算符映射到 IR 比较谓词
     */
    protected CmpInst.Pred mapCmpPred(oper_t op, boolean isSigned) {
        switch (op) {
            case EQ: return CmpInst.Pred.EQ;
            case NE: return CmpInst.Pred.NE;
            case LT: return isSigned ? CmpInst.Pred.SLT : CmpInst.Pred.ULT;
            case LE: return isSigned ? CmpInst.Pred.SLE : CmpInst.Pred.ULE;
            case GT: return isSigned ? CmpInst.Pred.SGT : CmpInst.Pred.UGT;
            case GE: return isSigned ? CmpInst.Pred.SGE : CmpInst.Pred.UGE;
            default:
                throw new RuntimeException("Unknown comparison operator: " + op);
        }
    }

    /**
     * 将复合赋值运算符映射到基本二元运算
     */
    protected BinaryOpInst.Op mapComAssignOp(oper_t op, boolean isSigned) {
        switch (op) {
            case PLUS_ASSIGN: return BinaryOpInst.Op.ADD;
            case MINUS_ASSIGN: return BinaryOpInst.Op.SUB;
            case MUL_ASSIGN: return BinaryOpInst.Op.MUL;
            case DIV_ASSIGN: return isSigned ? BinaryOpInst.Op.SDIV : BinaryOpInst.Op.UDIV;
            case MOD_ASSIGN: return isSigned ? BinaryOpInst.Op.SREM : BinaryOpInst.Op.UREM;
            case AND_ASSIGN: return BinaryOpInst.Op.AND;
            case OR_ASSIGN: return BinaryOpInst.Op.OR;
            case XOR_ASSIGN: return BinaryOpInst.Op.XOR;
            case SHL_ASSIGN: return BinaryOpInst.Op.SHL;
            case SHR_ASSIGN: return isSigned ? BinaryOpInst.Op.ASHR : BinaryOpInst.Op.LSHR;
            default:
                throw new RuntimeException("Unknown compound assignment operator: " + op);
        }
    }


    // ==================== 辅助方法：常量处理 ====================

    /**
     * 创建字符串常量（全局）
     */
    protected IRValue createStringConstant(String str) {
        String globalName = ".str." + stringConstantCounter++;
        IRGlobal strGlobal = new IRGlobal(
            new IRArrayType(IRIntType.I8, str.length() + 1),  // +1 for null terminator
            globalName,
            str  // 字符串内容
        );
        module.addGlobal(strGlobal);
        return strGlobal;
    }

    /**
     * 将 ConstantValue 转换为 IRConstant
     */
    protected IRConstant convertConstantValue(ConstantValue value, IRType targetType) {
        Object val = value.getValue();

        if (val instanceof Number) {
            long longVal = ((Number) val).longValue();
            if (targetType == IRIntType.I32) {
                return IRConstant.i32(longVal);
            } else if (targetType == IRIntType.I64) {
                return IRConstant.i64(longVal);
            } else if (targetType == IRIntType.I8) {
                return IRConstant.i8((int) longVal);
            } else if (targetType == IRIntType.I1) {
                return IRConstant.i1(longVal != 0);
            }
            return new IRConstant(targetType, longVal);
        } else if (val instanceof Boolean) {
            return IRConstant.i1((Boolean) val);
        }

        // 其他类型
        return new IRConstant(targetType, val);
    }

}
