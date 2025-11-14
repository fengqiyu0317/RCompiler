# Self Test Cases

This document contains 15 test cases for self and Self semantic checking in RCompiler.

## Test Case 1: Valid self usage in instance method

```rust
struct MyStruct {
    value: i32,
}

impl MyStruct {
    fn get_value(&self) -> i32 {
        self.value
    }
}
```

Expected: No errors - self is correctly used in an instance method

## Test Case 2: Invalid self usage in free function

```rust
fn free_function() {
    let x: i32 = self;
}
```

Expected: Error - self cannot be used in free functions (SELF_OUTSIDE_METHOD)

## Test Case 3: Invalid self usage in associated function

```rust
struct MyStruct;

impl MyStruct {
    fn associated_function() {
        let x: MyStruct = self;
    }
}
```

Expected: Error - self cannot be used in associated functions (SELF_IN_ASSOCIATED_FUNCTION)

## Test Case 4: Valid Self usage in impl block

```rust
struct MyStruct {
    value: i32,
}

impl MyStruct {
    fn new() -> Self {
        Self { value: 0 }
    }
    
    fn default() -> Self {
        Self::new()
    }
}
```

Expected: No errors - Self is correctly used in impl block

## Test Case 5: Invalid Self usage in free function

```rust
fn free_function() -> Self {
    Self {}
}
```

Expected: Error - Self cannot be used in free functions (SELF_TYPE_OUTSIDE_CONTEXT)

## Test Case 6: Valid Self usage in struct definition

```rust
struct ListNode {
    value: i32,
    next: Option<Box<Self>>,
}
```

Expected: No errors - Self is correctly used in struct definition

## Test Case 7: Valid self as method parameter

```rust
struct MyStruct;

impl MyStruct {
    fn method1(self) {
        // Method takes ownership of self
    }
    
    fn method2(&self) {
        // Method takes reference to self
    }
    
    fn method3(&mut self) {
        // Method takes mutable reference to self
    }
}
```

Expected: No errors - self is correctly used as method parameters

## Test Case 8: Valid Self usage in trait definition

```rust
trait MyTrait {
    fn new() -> Self;
    fn default_value() -> Self;
    fn process(&self) -> i32;
}
```

Expected: No errors - Self is correctly used in trait definition

## Test Case 9: Invalid Self usage with prefix

```rust
impl MyStruct {
    fn method() -> Self {
        ::Self::new()
    }
}
```

Expected: Error - Self cannot have prefix :: (SELF_TYPE_WITH_PREFIX)

## Test Case 10: Valid nested impl blocks with self and Self

```rust
struct Outer {
    inner: Inner,
}

struct Inner {
    value: i32,
}

impl Outer {
    fn new() -> Self {
        Self { inner: Inner::new() }
    }
    
    fn get_inner_value(&self) -> i32 {
        self.inner.get_value()
    }
}

impl Inner {
    fn new() -> Self {
        Self { value: 42 }
    }
    
    fn get_value(&self) -> i32 {
        self.value
    }
}
```

Expected: No errors - self and Self are correctly used in nested impl blocks

## Test Case 11: Invalid self usage outside impl block

```rust
struct MyStruct;

fn function_using_self() {
    let instance = MyStruct;
    let value = instance.get_value();
}

impl MyStruct {
    fn get_value(&self) -> i32 {
        42
    }
}
```

Expected: Error - self cannot be used outside impl blocks (SELF_OUTSIDE_METHOD)

## Test Case 12: Valid Self usage in return type and expression

```rust
struct MyStruct {
    value: i32,
}

impl MyStruct {
    fn create_with_value(value: i32) -> Self {
        Self { value: value }
    }
    
    fn clone(&self) -> Self {
        Self { value: self.value }
    }
}
```

Expected: No errors - Self is correctly used in return type and expression

## Test Case 13: Mixed valid and invalid self usage

```rust
struct MyStruct {
    value: i32,
}

impl MyStruct {
    fn valid_method(&self) -> i32 {
        self.value
    }
    
    fn invalid_associated() {
        // This should cause an error
        let x: i32 = self.value;
    }
}
```

Expected: Error - self cannot be used in associated function (SELF_IN_ASSOCIATED_FUNCTION)

## Test Case 14: Valid Self usage in complex type expressions

```rust
struct MyStruct<T> {
    value: T,
}

impl<T> MyStruct<T> {
    fn new(value: T) -> Self {
        Self { value }
    }
    
    fn map<U, F>(&self, f: F) -> MyStruct<U> 
    where
        F: FnOnce(&T) -> U,
    {
        MyStruct { value: f(&self.value) }
    }
}
```

Expected: No errors - Self is correctly used in complex type expressions

## Test Case 15: Invalid self usage in trait implementation

```rust
trait MyTrait {
    fn trait_method(&self) -> i32;
}

struct MyStruct;

impl MyTrait for MyStruct {
    fn trait_method() -> i32 {
        // Missing self parameter - this should be caught by parameter checking
        // If we try to use self here, it should cause an error
        self.value  // This should cause an error
    }
}
```

Expected: Error - self cannot be used in method without self parameter (SELF_OUTSIDE_METHOD)