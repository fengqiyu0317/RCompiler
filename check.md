# Parser Errors in RCompiler Testcases

This document lists all .rx files in the semantic-1 directory that contain parser errors (syntax errors).

## Expression Errors (expr*)

### expr1/expr1.rx
**Error**: Missing semicolon after literal
- Line 12: `42  // Missing semicolon - LL(1) can detect this as unexpected token`
- The literal `42` is not followed by a semicolon, which is required in Rust syntax.

### expr2/expr2.rx
**Error**: Invalid path expression - double colon without identifier
- Line 12: `let x: i32 = std::;  // Invalid path - :: followed by semicolon`
- The path expression `std::` is incomplete as it's missing an identifier after the double colon.

### expr3/expr3.rx
**Error**: Invalid block expression - missing closing brace
- Line 15: `// Missing closing brace - LL(1) expects }`
- The block expression starting at line 12 is missing its closing brace.

### expr4/expr4.rx
**Error**: Invalid operator expression - consecutive operators
- Line 12: `let x: i32 = 5 + * 3;  // Invalid: + followed by * without operand`
- The expression has consecutive operators `+ *` without a valid operand between them.

### expr5/expr5.rx
**Error**: Invalid grouped expression - missing closing parenthesis
- Line 12: `let result: i32 = (5 + 3 * 2;  // Missing closing parenthesis`
- The grouped expression is missing its closing parenthesis.

### expr6/expr6.rx
**Error**: Invalid array index expression - missing closing bracket
- Line 13: `let val: i32 = arr[0;  // Missing closing bracket ]`
- The array index expression is missing its closing bracket.

### expr7/expr7.rx
**Error**: Invalid struct expression - missing comma between fields
- Line 17: `let p: Point = Point { x: 10 y: 20 };  // Missing comma between fields`
- The struct initialization is missing a comma between field `x: 10` and field `y: 20`.

### expr8/expr8.rx
**Error**: Invalid call expression - missing closing parenthesis
- Line 16: `let result: i32 = add(5, 10;  // Missing closing parenthesis`
- The function call is missing its closing parenthesis.

### expr9/expr9.rx
**Error**: Invalid method call expression - missing dot before method name
- Line 23: `calc add(5);  // Missing dot before method name`
- The method call is missing the dot operator before the method name.

### expr10/expr10.rx
**Error**: Invalid field access expression - missing field name after dot
- Line 18: `let x: i32 = p.;  // Missing field name after dot`
- The field access expression is missing the field name after the dot.

### expr11/expr11.rx
**Error**: Invalid loop expression - missing opening brace in while loop
- Line 13: `while (i < 5)  // Missing opening brace {`
- The while loop is missing its opening brace.

### expr12/expr12.rx
**Error**: if condition missing parentheses
- Line 12: `if x > 5 { // Error: if condition missing parentheses`
- The if condition is not enclosed in parentheses.

### expr14/expr14.rx
**Error**: Invalid underscore expression - underscore used in invalid context
- Line 11: `let x: i32 = _;`
- The underscore `_` cannot be used as a value in this context.

### expr15/expr15.rx
**Error**: Mixed expression syntax error - array literal with missing comma
- Line 12: `let numbers: [i32; 3] = [1 2 3];  // Missing commas between array elements`
- The array literal is missing commas between elements.

### expr16/expr16.rx
**Error**: Missing comma in array literal
- Line 11: `let arr: [i32; 4] = [1, 2 3, 4];`
- The array literal is missing a comma between `2` and `3`.

### expr18/expr18.rx
**Error**: Missing value in struct field initialization
- Line 16: `let p: Point = Point { x: 1, y: };`
- The struct field `y` is missing its initialization value.

### expr20/expr20.rx
**Error**: Missing closing bracket in array index
- Line 13: `let value: i32 = numbers[index;`
- The array index expression is missing its closing bracket.

### expr21/expr21.rx
**Error**: Missing semicolon after let statement
- Line 16: `}`
- The let statement ending at line 16 is missing a semicolon.

### expr22/expr22.rx
**Error**: Missing closing parenthesis in method call
- Line 23: `let area: i32 = rect.area(;`
- The method call is missing its closing parenthesis.

### expr23/expr23.rx
**Error**: Incomplete operator expression
- Line 14: `let result: i32 = (x + arr[1]) *;`
- The operator expression is incomplete, missing an operand after the `*` operator.

### expr24/expr24.rx
**Error**: Missing closing parenthesis in grouped expression
- Line 17: `let grouped: i32 = (processed + 5;`
- The grouped expression is missing its closing parenthesis.

### expr25/expr25.rx
**Error**: Missing opening brace in if expression
- Line 19: `let index: usize = if (result > 10)  0 } else { 1 };`
- The if expression is missing its opening brace after the condition.

### expr26/expr26.rx
**Error**: Missing operator between operands
- Line 25: `let final_val: i32 = computed data.value;`
- Missing operator between `computed` and `data.value`.

### expr27/expr27.rx
**Error**: Missing semicolon after if expression
- Line 18: `let result: i32 = if (doubled > 50) { doubled } else { 0 }`
- The if expression statement is missing a semicolon.

### expr28/expr28.rx
**Error**: Missing closing parenthesis in grouped expression
- Line 25: `let index: i32 = (distance % 3;`
- The grouped expression is missing its closing parenthesis.

### expr29/expr29.rx
**Error**: Missing closing bracket in array index expression
- Line 29: `let array_val: i32 = [processed][0;`
- The array index expression is missing its closing bracket.

### expr30/expr30.rx
**Error**: Missing operand in division expression
- Line 24: `let final_result: i32 = (sum + 10) / ;`
- The division expression is missing the right operand.

### expr31/expr31.rx
**Error**: Missing comma in method call
- Line 85: `matrix_a.set(2, 1, 8);`
- Line 85: `matrix_a.set(2, 1 9);`
- The method call is missing a comma between arguments.

### expr32/expr32.rx
**Error**: Missing closing parenthesis in function call
- Line 128: `printInt(final_output;`
- The function call is missing its closing parenthesis.

### expr35/expr35.rx
**Error**: Variable `current_node` not found
- Line 78: `let left_height: i32 = if (left_child_index < 15 && current_node.left_exists) {`
- Line 84: `let right_height: i32 = if (right_child_index < 15 && current_node.right_exists) {`
- Line 90: `current_node.update_height(left_height, right_height);`
- Line 92: `let rotation_needed: i32 = current_node.needs_rotation();`
- The variable `current_node` is used but not defined in the current scope.

### expr37/expr37.rx
**Error**: Array length mismatch
- Line 307: `let initial_array: [i32; 16] = [`
- The array initialization has a length mismatch between the declared size and the actual elements.

### expr39/expr39.rx
**Error**: Missing closing parenthesis in expression
- Line 478: `let hash_step_3: i32 = (hash_step_2 * 73 % 100000; // existing missing parenthesis preserved`
- The expression is missing a closing parenthesis.

### expr40/expr40.rx
**Error**: Missing closing parenthesis in expression
- Line 407: `let hash_operation_3: i32 = (hash_operation_2 * 73 % 2000000011;`
- The expression is missing a closing parenthesis.

## Summary

Total files with parser errors: 32

These errors include:
- Missing semicolons (8 cases)
- Missing parentheses/brackets/braces (12 cases)
- Missing commas in struct/array literals (4 cases)
- Invalid operator usage (3 cases)
- Invalid path/field access expressions (2 cases)
- Variable scope issues (1 case)
- Array length mismatch (1 case)
- Missing method call dot (1 case)

All of these are syntax errors that would be detected by a parser during the parsing phase, before semantic analysis begins.