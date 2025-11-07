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
```

## 解决 "Error in the HTTP2 framing layer" 错误

### 问题原因
这个错误通常是由于 Git 使用 HTTP/2 协议时与某些网络环境或代理服务器不兼容导致的。

### 解决方案

#### 方案1：禁用 HTTP/2 协议
```bash
# 全局禁用 HTTP/2
git config --global http.version HTTP/1.1

# 或者只对 GitHub 禁用 HTTP/2
git config --global http."https://github.com".version HTTP/1.1

# 验证设置
git config --global --get http.version
```

#### 方案2：使用 SSH 协议替代 HTTPS
```bash
# 将远程 URL 从 HTTPS 改为 SSH
git remote set-url origin git@github.com:fengqiyu0317/RCompiler.git

# 如果还没有 SSH 密钥，生成一个
ssh-keygen -t rsa -b 4096 -C "your-email@example.com"

# 启动 SSH 代理
eval "$(ssh-agent -s)"

# 添加 SSH 密钥到代理
ssh-add ~/.ssh/id_rsa

# 复制公钥到 GitHub
cat ~/.ssh/id_rsa.pub
# 然后在 GitHub 设置中添加这个 SSH 密钥
```

#### 方案3：配置代理设置
```bash
# 如果使用代理，明确配置 HTTP 代理
git config --global http.proxy http://proxy.example.com:8080
git config --global https.proxy https://proxy.example.com:8080

# 或者取消代理设置（如果不使用代理）
git config --global --unset http.proxy
git config --global --unset https.proxy

# 设置代理绕过
git config --global http."https://github.com".proxy ""
```

#### 方案4：更新 Git 版本
```bash
# 检查当前 Git 版本
git --version

# 更新 Git（Ubuntu/Debian）
sudo apt-get update
sudo apt-get install git

# 更新 Git（CentOS/RHEL）
sudo yum update git

# 更新 Git（macOS）
brew upgrade git

# 或者从源码编译最新版本
```

#### 方案5：临时解决方案
```bash
# 临时使用 HTTP/1.1 进行单次操作
git -c http.version=HTTP/1.1 push origin main

# 或者设置环境变量
export GIT_CURL_VERBOSE=1
export GIT_TRACE=1
git push origin main
```

#### 方案6：网络配置调整
```bash
# 增加缓冲区大小
git config --global http.postBuffer 524288000

# 设置超时时间
git config --global http.lowSpeedLimit 0
git config --global http.lowSpeedTime 999999

# 禁用 SSL 验证（不推荐，仅用于测试）
git config --global http.sslVerify false
```

### 完整的故障排除脚本

```bash
#!/bin/bash
# fix_git_http2_error.sh

echo "修复 Git HTTP/2 错误..."

# 方案1：禁用 HTTP/2
echo "尝试方案1：禁用 HTTP/2 协议..."
git config --global http.version HTTP/1.1

# 测试连接
echo "测试 GitHub 连接..."
git ls-remote https://github.com/fengqiyu0317/RCompiler.git

if [ $? -eq 0 ]; then
    echo "方案1成功！现在可以推送代码了。"
    echo "执行推送命令："
    echo "git push origin main"
else
    echo "方案1失败，尝试方案2：使用 SSH 协议..."
    
    # 方案2：切换到 SSH
    git remote set-url origin git@github.com:fengqiyu0317/RCompiler.git
    
    # 测试 SSH 连接
    ssh -T git@github.com
    
    if [ $? -eq 1 ]; then  # GitHub 返回 1 表示成功连接但认证失败
        echo "SSH 连接成功，但需要配置 SSH 密钥。"
        echo "请按照以下步骤配置 SSH："
        echo "1. 生成 SSH 密钥：ssh-keygen -t rsa -b 4096 -C 'your-email@example.com'"
        echo "2. 添加密钥到 SSH 代理：eval \"\$(ssh-agent -s)\" && ssh-add ~/.ssh/id_rsa"
        echo "3. 复制公钥到 GitHub：cat ~/.ssh/id_rsa.pub"
        echo "4. 在 GitHub 设置中添加 SSH 密钥"
    else
        echo "SSH 连接失败，尝试方案3：配置代理..."
        
        # 方案3：重置代理设置
        git config --global --unset http.proxy
        git config --global --unset https.proxy
        
        # 再次测试
        git ls-remote https://github.com/fengqiyu0317/RCompiler.git
        
        if [ $? -eq 0 ]; then
            echo "代理设置修复成功！"
        else
            echo "所有方案都失败了，请检查网络连接或联系系统管理员。"
        fi
    fi
fi

echo "故障排除完成。"
```

### 验证修复结果

```bash
# 测试与 GitHub 的连接
git ls-remote https://github.com/fengqiyu0317/RCompiler.git

# 如果成功，尝试推送
git push origin main

# 查看详细错误信息（如果仍有问题）
GIT_CURL_VERBOSE=1 git push origin main
```

### 预防措施

```bash
# 永久设置 HTTP/1.1（避免未来出现问题）
git config --global http.version HTTP/1.1

# 或者只对 GitHub 设置
git config --global http."https://github.com".version HTTP/1.1

# 查看当前配置
git config --global --list | grep http
```

## 解决 "Verify public key failed" SSH 密钥格式错误

### 问题原因
这个错误通常是由于 SSH 密钥格式不正确或密钥损坏导致的。GitHub 要求使用 OpenSSH 公钥格式的密钥。

### 解决方案

#### 方案1：重新生成正确的 SSH 密钥
```bash
# 删除现有的 SSH 密钥（如果存在）
rm -f ~/.ssh/id_rsa ~/.ssh/id_rsa.pub
rm -f ~/.ssh/id_ed25519 ~/.ssh/id_ed25519.pub

# 生成新的 RSA 密钥（推荐使用 ed25519，更安全）
ssh-keygen -t ed25519 -C "your-email@example.com"

# 或者生成 RSA 密钥（如果需要兼容性）
ssh-keygen -t rsa -b 4096 -C "your-email@example.com"

# 生成时按提示操作，可以设置密码或直接回车跳过
```

#### 方案2：检查并修复现有密钥格式
```bash
# 检查现有公钥格式
ssh-keygen -l -f ~/.ssh/id_rsa.pub

# 如果格式不正确，转换格式
ssh-keygen -e -m PEM -f ~/.ssh/id_rsa.pub > ~/.ssh/id_rsa_pem.pub

# 验证转换后的格式
cat ~/.ssh/id_rsa_pem.pub
```

#### 方案3：确保使用正确的公钥内容
```bash
# 显示正确的公钥内容（应该以 ssh-ed25519 或 ssh-rsa 开头）
cat ~/.ssh/id_ed25519.pub
# 或者
cat ~/.ssh/id_rsa.pub

# 复制完整的公钥内容，包括开头的类型和结尾的邮箱
```

#### 方案4：测试 SSH 连接
```bash
# 测试 SSH 连接到 GitHub
ssh -T git@github.com

# 如果成功，会显示 "Hi fengqiyu0317! You've successfully authenticated..."
# 如果失败，会显示具体的错误信息
```

#### 方案5：重新配置 SSH 代理
```bash
# 停止现有的 SSH 代理
eval "$(ssh-agent -k)" 2>/dev/null

# 启动新的 SSH 代理
eval "$(ssh-agent -s)"

# 添加私钥到代理
ssh-add ~/.ssh/id_ed25519
# 或者
ssh-add ~/.ssh/id_rsa

# 验证密钥已添加
ssh-add -l
```

### 完整的 SSH 密钥修复脚本

```bash
#!/bin/bash
# fix_ssh_key_error.sh

echo "修复 SSH 密钥格式错误..."

# 设置变量
EMAIL="your-email@example.com"
KEY_TYPE="ed25519"  # 可以改为 "rsa"

echo "1. 删除现有密钥..."
rm -f ~/.ssh/id_${KEY_TYPE} ~/.ssh/id_${KEY_TYPE}.pub
rm -f ~/.ssh/id_rsa ~/.ssh/id_rsa.pub  # 也删除 RSA 密钥

echo "2. 生成新的 SSH 密钥..."
ssh-keygen -t ${KEY_TYPE} -C "${EMAIL}" -N "" -f ~/.ssh/id_${KEY_TYPE}

echo "3. 启动 SSH 代理..."
eval "$(ssh-agent -k)" 2>/dev/null
eval "$(ssh-agent -s)"

echo "4. 添加私钥到代理..."
ssh-add ~/.ssh/id_${KEY_TYPE}

echo "5. 显示公钥内容（请复制到 GitHub）："
echo "=========================================="
cat ~/.ssh/id_${KEY_TYPE}.pub
echo "=========================================="

echo "6. 测试 SSH 连接..."
ssh -T git@github.com

echo "修复完成！请将上面的公钥添加到 GitHub："
echo "1. 登录 GitHub"
echo "2. 进入 Settings > SSH and GPG keys"
echo "3. 点击 New SSH key"
echo "4. 粘贴上面显示的公钥内容"
echo "5. 保存后再次运行: ssh -T git@github.com"
```

### 验证和故障排除

#### 检查密钥格式
```bash
# 检查公钥格式（应该显示正确的指纹）
ssh-keygen -l -f ~/.ssh/id_ed25519.pub

# 检查私钥格式
ssh-keygen -y -f ~/.ssh/id_ed25519

# 验证密钥对匹配
ssh-keygen -l -f ~/.ssh/id_ed25519
ssh-keygen -l -f ~/.ssh/id_ed25519.pub
```

#### 调试 SSH 连接
```bash
# 详细模式测试连接
ssh -vT git@github.com

# 检查 SSH 配置
cat ~/.ssh/config

# 检查已知主机
cat ~/.ssh/known_hosts | grep github
```

### GitHub 设置步骤

1. **复制公钥内容**：
   ```bash
   # 复制到剪贴板（如果支持）
   cat ~/.ssh/id_ed25519.pub | pbcopy  # macOS
   cat ~/.ssh/id_ed25519.pub | xclip -sel clip  # Linux
   # 或者手动复制显示的内容
   ```

2. **在 GitHub 中添加密钥**：
   - 登录 GitHub
   - 点击右上角头像 > Settings
   - 左侧菜单选择 "SSH and GPG keys"
   - 点击 "New SSH key"
   - 填写 Title（如 "RCompiler Dev"）
   - 粘贴公钥内容到 Key 字段
   - 点击 "Add SSH key"

3. **验证添加成功**：
   ```bash
   ssh -T git@github.com
   ```

### 常见问题解决

#### 问题1：权限错误
```bash
# 修复 SSH 目录和文件权限
chmod 700 ~/.ssh
chmod 600 ~/.ssh/id_ed25519
chmod 644 ~/.ssh/id_ed25519.pub
chmod 644 ~/.ssh/known_hosts
```

#### 问题2：多个密钥冲突
```bash
# 创建或编辑 SSH 配置文件
cat > ~/.ssh/config << EOF
Host github.com
  HostName github.com
  User git
  IdentityFile ~/.ssh/id_ed25519
  IdentitiesOnly yes
EOF

# 测试配置
ssh -T git@github.com
```

#### 问题3：密钥被拒绝
```bash
# 清理已知主机中的 GitHub 条目
ssh-keygen -R github.com

# 重新测试连接
ssh -T git@github.com
```

### 切换回 HTTPS（如果 SSH 仍有问题）

```bash
# 如果 SSH 仍有问题，可以临时切换回 HTTPS
git remote set-url origin https://github.com/fengqiyu0317/RCompiler.git

# 使用 Personal Access Token 认证
git remote set-url origin https://YOUR_TOKEN@github.com/fengqiyu0317/RCompiler.git
```

## 解决 "Invalid username or token. Password authentication is not supported" 错误

### 问题原因
这个错误表示 GitHub 不再支持密码认证，必须使用 Personal Access Token (PAT) 或 SSH 密钥进行认证。

### 解决方案

#### 方案1：创建和使用 Personal Access Token (PAT)

##### 1.1 创建 Personal Access Token
```bash
# 在浏览器中访问 GitHub 创建 PAT
# 1. 登录 GitHub
# 2. 点击右上角头像 > Settings
# 3. 左侧菜单选择 "Developer settings"
# 4. 选择 "Personal access tokens" > "Tokens (classic)"
# 5. 点击 "Generate new token" > "Generate new token (classic)"
# 6. 填写 Note（如 "RCompiler Development"）
# 7. 选择 Expiration（推荐选择 30 days 或更短）
# 8. 勾选以下权限：
#    - repo (完整仓库访问权限)
#    - workflow (如果需要 GitHub Actions)
# 9. 点击 "Generate token"
# 10. 复制生成的 token（注意：token 只显示一次，请妥善保存）
```

##### 1.2 使用 PAT 进行 Git 操作
```bash
# 方法1：在命令中直接使用（不推荐，token 会保存在历史记录中）
git remote set-url origin https://YOUR_USERNAME:YOUR_TOKEN@github.com/fengqiyu0317/RCompiler.git

# 方法2：使用 Git 凭据存储（推荐）
git config --global credential.helper store
git push origin main
# 然后输入用户名和粘贴 token 作为密码

# 方法3：使用环境变量（临时）
export GIT_USERNAME="fengqiyu0317"
export GIT_TOKEN="your_personal_access_token"
git push https://$GIT_USERNAME:$GIT_TOKEN@github.com/fengqiyu0317/RCompiler.git main
```

#### 方案2：配置 Git 凭据管理器

##### 2.1 Linux 系统
```bash
# 安装 Git 凭据管理器
sudo apt-get install git-credential-manager  # Ubuntu/Debian
# 或
sudo yum install git-credential-manager      # CentOS/RHEL

# 配置使用凭据管理器
git config --global credential.helper /usr/share/doc/git/contrib/credential/gnome-keyring/git-credential-gnome-keyring

# 或者使用 cache 模式（临时存储）
git config --global credential.helper 'cache --timeout=3600'
```

##### 2.2 macOS 系统
```bash
# macOS 通常自带凭据管理器
git config --global credential.helper osxkeychain

# 如果没有安装，可以通过 Homebrew 安装
brew install git-credential-manager
```

##### 2.3 Windows 系统
```bash
# Windows Git 通常自带凭据管理器
git config --global credential.helper manager

# 或者手动安装 Git Credential Manager
# 下载地址：https://github.com/GitCredentialManager/git-credential-manager/releases
```

#### 方案3：切换到 SSH 认证（推荐）

```bash
# 生成 SSH 密钥（如果还没有）
ssh-keygen -t ed25519 -C "your-email@example.com"

# 启动 SSH 代理
eval "$(ssh-agent -s)"

# 添加私钥到代理
ssh-add ~/.ssh/id_ed25519

# 显示公钥并复制到 GitHub
cat ~/.ssh/id_ed25519.pub

# 将远程 URL 从 HTTPS 改为 SSH
git remote set-url origin git@github.com:fengqiyu0317/RCompiler.git

# 测试 SSH 连接
ssh -T git@github.com

# 推送代码
git push origin main
```

### 完整的认证问题修复脚本

```bash
#!/bin/bash
# fix_git_auth_error.sh

echo "修复 Git 认证错误..."

echo "请选择认证方式："
echo "1. Personal Access Token (PAT)"
echo "2. SSH 密钥"
echo "3. 配置凭据管理器"
read -p "请输入选择 (1-3): " choice

case $choice in
    1)
        echo "=== Personal Access Token 配置 ==="
        echo "请按照以下步骤创建 PAT："
        echo "1. 访问 https://github.com/settings/tokens"
        echo "2. 点击 'Generate new token (classic)'"
        echo "3. 勾选 'repo' 权限"
        echo "4. 复制生成的 token"
        echo ""
        read -p "请输入您的 GitHub 用户名: " username
        read -p "请输入您的 Personal Access Token: " token
        
        # 配置远程 URL 使用 token
        git remote set-url origin https://$username:$token@github.com/fengqiyu0317/RCompiler.git
        
        # 配置凭据存储
        git config --global credential.helper store
        
        echo "PAT 配置完成！"
        ;;
    2)
        echo "=== SSH 密钥配置 ==="
        
        # 生成 SSH 密钥
        ssh-keygen -t ed25519 -C "your-email@example.com" -N "" -f ~/.ssh/id_ed25519
        
        # 启动 SSH 代理
        eval "$(ssh-agent -s)"
        ssh-add ~/.ssh/id_ed25519
        
        echo "请复制以下公钥到 GitHub："
        echo "=========================================="
        cat ~/.ssh/id_ed25519.pub
        echo "=========================================="
        echo ""
        echo "步骤："
        echo "1. 访问 https://github.com/settings/keys"
        echo "2. 点击 'New SSH key'"
        echo "3. 粘贴上面的公钥内容"
        echo ""
        read -p "按回车键继续..."
        
        # 切换到 SSH URL
        git remote set-url origin git@github.com:fengqiyu0317/RCompiler.git
        
        # 测试连接
        ssh -T git@github.com
        
        echo "SSH 配置完成！"
        ;;
    3)
        echo "=== 凭据管理器配置 ==="
        
        # 检测操作系统
        if [[ "$OSTYPE" == "linux-gnu"* ]]; then
            echo "检测到 Linux 系统"
            git config --global credential.helper 'cache --timeout=3600'
        elif [[ "$OSTYPE" == "darwin"* ]]; then
            echo "检测到 macOS 系统"
            git config --global credential.helper osxkeychain
        elif [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]]; then
            echo "检测到 Windows 系统"
            git config --global credential.helper manager
        else
            echo "未知系统，使用通用配置"
            git config --global credential.helper store
        fi
        
        echo "凭据管理器配置完成！"
        echo "下次执行 git push 时，请输入用户名和 Personal Access Token"
        ;;
    *)
        echo "无效选择"
        exit 1
        ;;
esac

echo "认证配置完成，现在可以尝试推送代码："
echo "git push origin main"
```

### 验证和测试

#### 测试 PAT 认证
```bash
# 测试远程连接
git remote -v

# 尝试推送（会提示输入用户名和密码）
git push origin main

# 验证凭据是否保存
git config --global --get credential.helper
```

#### 测试 SSH 认证
```bash
# 测试 SSH 连接
ssh -T git@github.com

# 检查远程 URL
git remote get-url origin

# 尝试推送
git push origin main
```

### 常见问题解决

#### 问题1：Token 权限不足
```bash
# 确保创建 PAT 时勾选了以下权限：
# - repo (完整仓库访问权限)
# - workflow (如果需要 GitHub Actions)
# - write:packages (如果需要包管理)

# 如果权限不足，需要重新创建 PAT
```

#### 问题2：Token 过期
```bash
# PAT 有过期时间，过期后需要重新创建
# 建议设置较短的过期时间（如 30 天）并定期更新

# 查看当前配置的远程 URL
git remote get-url origin

# 更新为新的 token
git remote set-url origin https://username:NEW_TOKEN@github.com/fengqiyu0317/RCompiler.git
```

#### 问题3：凭据缓存问题
```bash
# 清除缓存的凭据
git config --global --unset credential.helper

# 或者清除特定 URL 的凭据
git config --global --unset credential."https://github.com".helper

# 重新配置凭据管理器
git config --global credential.helper store
```

### 安全建议

1. **不要在命令行中直接使用 token**：避免 token 被保存在命令历史中
2. **定期轮换 token**：建议每 30-90 天更新一次 PAT
3. **使用最小权限原则**：只授予必要的权限
4. **使用 SSH 密钥**：对于频繁使用，SSH 密钥比 PAT 更安全
5. **不要共享 token**：Personal Access Token 应该保密

### 临时解决方案（紧急情况）

```bash
# 如果急需推送代码，可以使用临时环境变量
export GIT_ASKPASS="echo -n 'username: '; read username; echo \$username"
export GIT_USERNAME="fengqiyu0317"
export GIT_PASSWORD="your_token_here"

# 或者使用 Git 的 askpass helper
echo 'echo "your_token_here"' > /tmp/git_askpass
chmod +x /tmp/git_askpass
GIT_ASKPASS=/tmp/git_askpass git push origin main
rm /tmp/git_askpass
```

## 查询和管理 SSH 密钥

### 查看本地 SSH 密钥

#### 1. 列出所有 SSH 密钥文件
```bash
# 查看 .ssh 目录中的所有文件
ls -la ~/.ssh/

# 常见的 SSH 密钥文件包括：
# id_rsa, id_rsa.pub (RSA 密钥)
# id_ed25519, id_ed25519.pub (Ed25519 密钥)
# id_ecdsa, id_ecdsa.pub (ECDSA 密钥)
# known_hosts (已知主机)
# config (SSH 配置文件)
```

#### 2. 查看公钥内容
```bash
# 查看 Ed25519 公钥（推荐）
cat ~/.ssh/id_ed25519.pub

# 查看 RSA 公钥
cat ~/.ssh/id_rsa.pub

# 查看 ECDSA 公钥
cat ~/.ssh/id_ecdsa.pub

# 如果不确定使用哪种密钥，可以查看所有公钥
find ~/.ssh/ -name "*.pub" -exec echo "=== {} ===" \; -exec cat {} \;
```

#### 3. 查看私钥信息
```bash
# 查看私钥指纹信息
ssh-keygen -l -f ~/.ssh/id_ed25519

# 查看私钥详细信息
ssh-keygen -y -f ~/.ssh/id_ed25519

# 验证私钥和公钥是否匹配
ssh-keygen -l -f ~/.ssh/id_ed25519
ssh-keygen -l -f ~/.ssh/id_ed25519.pub
```

#### 4. 检查 SSH 代理中的密钥
```bash
# 查看 SSH 代理中加载的密钥
ssh-add -l

# 查看所有密钥（包括代理中的）
ssh-add -L

# 如果代理未运行，启动它
eval "$(ssh-agent -s)"

# 添加私钥到代理
ssh-add ~/.ssh/id_ed25519
```

### 查询 GitHub 账户中的 SSH 密钥

#### 1. 通过网页查看
```bash
# 在浏览器中访问以下 URL 查看您的 SSH 密钥：
# https://github.com/settings/keys

# 或者直接访问：
# https://github.com/settings/ssh/new
```

#### 2. 通过 GitHub API 查询
```bash
# 使用 curl 查询您的 SSH 密钥（需要 Personal Access Token）
curl -H "Authorization: token YOUR_TOKEN" https://api.github.com/user/keys

# 使用 jq 格式化输出（如果安装了 jq）
curl -H "Authorization: token YOUR_TOKEN" https://api.github.com/user/keys | jq '.'
```

### SSH 密钥管理脚本

```bash
#!/bin/bash
# manage_ssh_keys.sh

echo "=== SSH 密钥管理工具 ==="
echo ""

echo "1. 查看本地 SSH 密钥"
echo "2. 查看公钥内容"
echo "3. 验证密钥对"
echo "4. 测试 SSH 连接"
echo "5. 查看代理中的密钥"
echo "6. 生成新密钥"
echo ""
read -p "请选择操作 (1-6): " choice

case $choice in
    1)
        echo "=== 本地 SSH 密钥文件 ==="
        if [ -d ~/.ssh ]; then
            ls -la ~/.ssh/
        else
            echo "SSH 目录不存在：~/.ssh/"
        fi
        ;;
    2)
        echo "=== 公钥内容 ==="
        for key in ~/.ssh/*.pub; do
            if [ -f "$key" ]; then
                echo "--- $(basename $key) ---"
                cat "$key"
                echo ""
            fi
        done
        ;;
    3)
        echo "=== 验证密钥对 ==="
        for key in ~/.ssh/id_*; do
            if [ -f "$key" ] && [[ ! "$key" =~ \.pub$ ]]; then
                echo "--- $(basename $key) ---"
                if [ -f "${key}.pub" ]; then
                    echo "私钥指纹："
                    ssh-keygen -l -f "$key"
                    echo "公钥指纹："
                    ssh-keygen -l -f "${key}.pub"
                    if ssh-keygen -l -f "$key" 2>/dev/null | grep -q "$(ssh-keygen -l -f "${key}.pub" 2>/dev/null | cut -d' ' -f2)"; then
                        echo "✓ 密钥对匹配"
                    else
                        echo "✗ 密钥对不匹配"
                    fi
                else
                    echo "缺少对应的公钥文件：${key}.pub"
                fi
                echo ""
            fi
        done
        ;;
    4)
        echo "=== 测试 SSH 连接 ==="
        echo "测试连接到 GitHub..."
        ssh -T git@github.com
        ;;
    5)
        echo "=== SSH 代理中的密钥 ==="
        if pgrep -q ssh-agent; then
            echo "SSH 代理正在运行"
            echo "代理中的密钥："
            ssh-add -l
        else
            echo "SSH 代理未运行"
            echo "启动 SSH 代理..."
            eval "$(ssh-agent -s)"
            echo "代理已启动，但没有加载密钥"
        fi
        ;;
    6)
        echo "=== 生成新密钥 ==="
        echo "请选择密钥类型："
        echo "1. Ed25519 (推荐，更安全)"
        echo "2. RSA (兼容性更好)"
        echo "3. ECDSA"
        read -p "请选择 (1-3): " key_type
        
        case $key_type in
            1) type="ed25519" ;;
            2) type="rsa" ;;
            3) type="ecdsa" ;;
            *) type="ed25519" ;;
        esac
        
        read -p "请输入邮箱地址: " email
        
        if [ "$type" = "rsa" ]; then
            ssh-keygen -t rsa -b 4096 -C "$email"
        else
            ssh-keygen -t $type -C "$email"
        fi
        
        echo "密钥生成完成！"
        echo "公钥内容："
        cat ~/.ssh/id_${type}.pub
        ;;
    *)
        echo "无效选择"
        ;;
esac
```

### 快速查询命令

```bash
# 一键查看所有 SSH 密钥信息
echo "=== SSH 密钥概览 ===" && \
echo "密钥文件：" && ls -la ~/.ssh/ 2>/dev/null || echo "SSH 目录不存在" && \
echo "" && \
echo "公钥内容：" && find ~/.ssh/ -name "*.pub" -exec echo "--- {} ---" \; -exec cat {} \; 2>/dev/null && \
echo "" && \
echo "代理状态：" && ssh-add -l 2>/dev/null || echo "SSH 代理未运行或无密钥"
```

### 检查密钥是否已添加到 GitHub

```bash
#!/bin/bash
# check_github_keys.sh

echo "=== 检查 GitHub 中的 SSH 密钥 ==="

# 获取本地公钥指纹
local_keys=()
for key_file in ~/.ssh/id_*.pub; do
    if [ -f "$key_file" ]; then
        fingerprint=$(ssh-keygen -l -f "$key_file" 2>/dev/null | cut -d' ' -f2)
        key_name=$(basename "$key_file")
        local_keys+=("$key_name:$fingerprint")
    fi
done

echo "本地 SSH 密钥："
for key in "${local_keys[@]}"; do
    echo "  $key"
done

echo ""
echo "请访问以下 URL 查看您的 GitHub SSH 密钥："
echo "https://github.com/settings/keys"
echo ""
echo "或者使用 GitHub API 查询（需要 Personal Access Token）："
echo "curl -H \"Authorization: token YOUR_TOKEN\" https://api.github.com/user/keys"
```

### 常见问题解决

#### 问题1：没有 SSH 密钥
```bash
# 如果没有 SSH 密钥，生成一个
ssh-keygen -t ed25519 -C "your-email@example.com"

# 然后添加到 SSH 代理
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_ed25519
```

#### 问题2：权限问题
```bash
# 修复 SSH 目录和文件权限
chmod 700 ~/.ssh
chmod 600 ~/.ssh/id_*
chmod 644 ~/.ssh/*.pub
chmod 644 ~/.ssh/known_hosts
chmod 600 ~/.ssh/config
```

#### 问题3：多个密钥冲突
```bash
# 创建或编辑 SSH 配置文件
cat > ~/.ssh/config << EOF
Host github.com
  HostName github.com
  User git
  IdentityFile ~/.ssh/id_ed25519
  IdentitiesOnly yes
EOF

# 测试配置
ssh -T git@github.com
```