# RCompiler Makefile
# 用于编译和运行Rust编译器项目

# 项目配置
JAVA_HOME ?= /usr/lib/jvm/default-java
JAVAC = javac
JAVA = java
JAVAC_FLAGS = -d $(TARGET_DIR) -cp $(TARGET_DIR) -encoding UTF-8
JAVA_FLAGS = -cp $(TARGET_DIR) -Dfile.encoding=UTF-8

# 目录配置
SRC_DIR = src/main/java
TEST_DIR = src/test/java
TARGET_DIR = target/classes
RESOURCES_DIR = src/main/resources

# 源文件查找
SRC_FILES = $(shell find $(SRC_DIR) -name "*.java")
TEST_FILES = $(shell find $(TEST_DIR) -name "*.java")

# 主类和测试类
MAIN_CLASS = Main
TEST_RUNNER = TestRunner
ERROR_TEST_RUNNER = ErrorTestRunner
BATCH_TEST_RUNNER = BatchTestRunner

# 默认目标
.PHONY: all compile run test clean rebuild help

# 全部编译
all: compile

# 创建目标目录
$(TARGET_DIR):
	mkdir -p $(TARGET_DIR)

# 编译主程序
compile: $(TARGET_DIR) $(SRC_FILES)
	@echo "编译主程序..."
	$(JAVAC) $(JAVAC_FLAGS) $(SRC_FILES)
	@echo "编译完成！"

# 编译测试
compile-test: compile $(TEST_FILES)
	@echo "编译测试程序..."
	$(JAVAC) $(JAVAC_FLAGS) $(TEST_FILES)
	@echo "测试编译完成！"

# 运行主程序
run: compile
	@echo "运行主程序..."
	$(JAVA) $(JAVA_FLAGS) $(MAIN_CLASS)

# 运行单个测试
test: compile-test
	@echo "运行测试..."
	$(JAVA) $(JAVA_FLAGS) $(TEST_RUNNER)

# 运行错误测试
test-error: compile-test
	@echo "运行错误测试..."
	$(JAVA) $(JAVA_FLAGS) $(ERROR_TEST_RUNNER)

# 运行批量测试
test-batch: compile-test
	@echo "运行批量测试..."
	$(JAVA) $(JAVA_FLAGS) $(BATCH_TEST_RUNNER)

# 运行所有测试
test-all: test test-error test-batch

# 清理编译产物
clean:
	@echo "清理编译产物..."
	rm -rf $(TARGET_DIR)
	@echo "清理完成！"

# 重新编译
rebuild: clean compile

# 运行特定测试用例
# 使用方法: make test-case TESTCASE=path/to/test.rs
test-case: compile
	@echo "运行测试用例: $(TESTCASE)"
	$(JAVA) $(JAVA_FLAGS) $(MAIN_CLASS) $(TESTCASE)

# 调试模式运行
debug: compile
	@echo "调试模式运行..."
	$(JAVA) $(JAVA_FLAGS) -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 $(MAIN_CLASS)

# 显示项目信息
info:
	@echo "=== RCompiler 项目信息 ==="
	@echo "源代码目录: $(SRC_DIR)"
	@echo "测试目录: $(TEST_DIR)"
	@echo "输出目录: $(TARGET_DIR)"
	@echo "主类: $(MAIN_CLASS)"
	@echo "Java编译器: $(JAVAC)"
	@echo "Java运行时: $(JAVA)"
	@echo "源文件数量: $(words $(SRC_FILES))"
	@echo "测试文件数量: $(words $(TEST_FILES))"

# 显示帮助
help:
	@echo "=== RCompiler Makefile 使用说明 ==="
	@echo ""
	@echo "基本命令:"
	@echo "  make          - 编译项目 (等同于 make compile)"
	@echo "  make compile  - 编译所有Java源文件"
	@echo "  make run      - 编译并运行主程序"
	@echo "  make test     - 编译并运行基本测试"
	@echo ""
	@echo "测试命令:"
	@echo "  make test-error    - 运行错误测试"
	@echo "  make test-batch    - 运行批量测试"
	@echo "  make test-all      - 运行所有测试"
	@echo "  make test-case TESTCASE=path/to/test.rs - 运行特定测试用例"
	@echo ""
	@echo "维护命令:"
	@echo "  make clean    - 清理编译产物"
	@echo "  make rebuild  - 清理后重新编译"
	@echo "  make debug    - 调试模式运行程序"
	@echo ""
	@echo "信息命令:"
	@echo "  make info     - 显示项目信息"
	@echo "  make help     - 显示此帮助信息"
	@echo ""
	@echo "示例用法:"
	@echo "  make run                    # 编译并运行主程序"
	@echo "  make test-case TESTCASE=tests/inputs/test.rs  # 运行特定测试"
	@echo "  make rebuild                # 完全重新编译项目"

# 检查Java环境
check-java:
	@echo "检查Java环境..."
	@which $(JAVAC) > /dev/null || (echo "错误: 未找到javac编译器" && exit 1)
	@which $(JAVA) > /dev/null || (echo "错误: 未找到java运行时" && exit 1)
	@$(JAVAC) -version
	@$(JAVA) -version
	@echo "Java环境检查完成！"

# 安装依赖（如果需要）
install-deps:
	@echo "检查项目依赖..."
	@echo "当前项目使用标准Java库，无需额外安装依赖"

# 创建发布包
dist: clean compile
	@echo "创建发布包..."
	mkdir -p dist
	cp -r $(TARGET_DIR) dist/
	cp -r docs dist/
	cp README.md dist/
	cp structure.md dist/
	tar -czf rcompiler-$(shell date +%Y%m%d).tar.gz dist/
	@echo "发布包创建完成: rcompiler-$(shell date +%Y%m%d).tar.gz"

# 统计代码行数
stats:
	@echo "=== 代码统计 ==="
	@echo "Java源代码行数:"
	@find $(SRC_DIR) -name "*.java" -exec wc -l {} + | tail -1
	@echo "测试代码行数:"
	@find $(TEST_DIR) -name "*.java" -exec wc -l {} + | tail -1
	@echo "文档行数:"
	@find docs -name "*.md" -exec wc -l {} + | tail -1