# Git 仓库更新指令

## 将当前项目文件更新到 GitHub 仓库 https://github.com/fengqiyu0317/RCompiler

### 基本设置和初始化

```bash
# 检查当前 Git 状态
git status

# 如果还没有初始化 Git 仓库，先初始化
git init

# 添加远程仓库（如果还没有添加）
git remote add origin https://github.com/fengqiyu0317/RCompiler.git

# 如果远程仓库已存在，先删除再添加（确保使用正确的 URL）
git remote remove origin
git remote add origin https://github.com/fengqiyu0317/RCompiler.git
```

### 添加和提交所有文件

```bash
# 添加所有文件到暂存区
git add .

# 或者更精确地添加特定文件
git add *.java *.md *.sh *.rs

# 提交文件
git commit -m "Update RCompiler project files"

# 如果需要设置提交者信息
git config user.name "fengqiyu0317"
git config user.email "your-email@example.com"
```

### 推送到远程仓库

```bash
# 推送到主分支（通常是 main 或 master）
git push -u origin main

# 或者如果默认分支是 master
git push -u origin master

# 如果遇到分支名问题，可以先查看远程分支
git remote show origin

# 然后推送到正确的分支
git push -u origin main  # 或 master
```

### 处理可能的冲突和问题

```bash
# 如果远程仓库有新的提交，先拉取
git pull origin main

# 如果有冲突，解决冲突后再次提交
git add .
git commit -m "Resolve merge conflicts"

# 强制推送（谨慎使用，会覆盖远程更改）
git push -f origin main
```

### 完整的自动化脚本

```bash
#!/bin/bash
# update_to_github.sh

echo "开始更新 RCompiler 项目到 GitHub..."

# 设置变量
REPO_URL="https://github.com/fengqiyu0317/RCompiler.git"
BRANCH="main"
COMMIT_MSG="Update RCompiler project files - $(date)"

# 检查 Git 是否已初始化
if [ ! -d ".git" ]; then
    echo "初始化 Git 仓库..."
    git init
fi

# 配置用户信息（如果需要）
git config user.name "fengqiyu0317" 2>/dev/null
git config user.email "fengqiyu0317@example.com" 2>/dev/null

# 添加远程仓库
echo "配置远程仓库..."
git remote remove origin 2>/dev/null
git remote add origin $REPO_URL

# 添加所有文件
echo "添加文件到暂存区..."
git add .

# 提交更改
echo "提交更改..."
git commit -m "$COMMIT_MSG"

# 推送到远程仓库
echo "推送到 GitHub..."
git push -u origin $BRANCH

echo "更新完成！"
```

### 验证更新结果

```bash
# 检查远程仓库状态
git remote -v

# 查看提交历史
git log --oneline -10

# 检查分支状态
git branch -a

# 验证远程同步
git status
```

### 常用 Git 命令参考

```bash
# 查看文件状态
git status

# 查看差异
git diff

# 查看提交历史
git log --oneline --graph

# 查看远程信息
git remote show origin

# 同步远程仓库
git fetch origin

# 创建新分支
git checkout -b new-feature

# 合并分支
git merge new-feature

# 删除分支
git branch -d new-feature
```

### 注意事项

1. **认证问题**：如果使用 HTTPS，可能需要配置 GitHub Personal Access Token
2. **分支名称**：确认远程仓库的主分支是 main 还是 master
3. **文件大小**：GitHub 有文件大小限制，大文件可能需要使用 Git LFS
4. **敏感信息**：确保不提交包含密码、API 密钥等敏感信息的文件

### GitHub 认证设置

```bash
# 如果使用 Personal Access Token
git remote set-url origin https://YOUR_TOKEN@github.com/fengqiyu0317/RCompiler.git

# 或者配置 Git 凭据存储
git config --global credential.helper store

# 使用 SSH 密钥（推荐）
git remote set-url origin git@github.com:fengqiyu0317/RCompiler.git