package codegen;

/**
 * Target machine configuration for IR codegen.
 * Keep these values in one place so we can switch targets easily.
 */
public final class TargetConfig {
    // Pointer width in bits for the current target.
    public static final int POINTER_BITS = 32;

    // Rust usize/isize follow pointer width.
    public static final int USIZE_BITS = POINTER_BITS;
    public static final int ISIZE_BITS = POINTER_BITS;

    private TargetConfig() {}

    public static int pointerBytes() {
        return (POINTER_BITS + 7) / 8;
    }
}
