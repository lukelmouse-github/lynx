# 🚀 Flatten Demo 快速构建指南

## ⚡ 最快的方式（30秒）

### 使用一键构建脚本

```bash
# 进入项目根目录
cd /Users/luke/Projects/Github/lynx

# 运行构建脚本
./build-flatten-demo.sh build

# 完成！输出文件在：
# /Users/luke/Projects/Github/lynx/explorer/showcase/menu/dist/flatten.lynx.bundle
```

---

## 📋 常用命令

### 构建生产版本（推荐）

```bash
./build-flatten-demo.sh build
```

**输出**: `dist/flatten.lynx.bundle` (生产优化版本)

### 启动开发服务器（实时编译）

```bash
./build-flatten-demo.sh dev
```

**特点**:
- 自动监听文件变化
- 实时重新编译
- 支持热更新
- 运行在 `http://localhost:5173`

### 构建开发版本（带源码映射）

```bash
./build-flatten-demo.sh dev-build
```

**特点**:
- 包含源码映射
- 便于调试
- 文件较大

### 清除缓存

```bash
./build-flatten-demo.sh clean
```

**清除内容**:
- `dist/` 目录
- `.vite` 缓存

### 显示帮助

```bash
./build-flatten-demo.sh help
```

---

## 🎯 完整工作流

### 第1步：修改代码

编辑文件：
- `/Users/luke/Projects/Github/lynx/explorer/showcase/menu/flatten/src/App.tsx`
- `/Users/luke/Projects/Github/lynx/explorer/showcase/menu/sub-menu/flatten.tsx`

### 第2步：构建

```bash
cd /Users/luke/Projects/Github/lynx
./build-flatten-demo.sh build
```

### 第3步：运行

在Lynx Explorer中打开：
```
file://lynx?local://showcase/flatten/main.lynx.bundle
```

### 第4步：验证

检查修改是否生效

---

## 🔄 开发工作流（推荐）

### 终端1：启动开发服务器

```bash
cd /Users/luke/Projects/Github/lynx
./build-flatten-demo.sh dev
```

### 终端2：编辑代码

```bash
# 使用你喜欢的编辑器修改代码
# 开发服务器会自动重新编译
```

### 设备上：刷新查看

```
在Lynx Explorer中按F5或下拉刷新
```

---

## 📊 构建时间

| 场景 | 时间 | 说明 |
|------|------|------|
| 第一次构建 | 30-60秒 | 包括依赖安装 |
| 后续构建 | 5-10秒 | 增量构建 |
| 开发模式 | 2-5秒 | 实时编译 |

---

## ✅ 验证构建成功

### 检查1：查看输出文件

```bash
ls -lh /Users/luke/Projects/Github/lynx/explorer/showcase/menu/dist/flatten.lynx.bundle
```

**应该看到**:
```
-rw-r--r--  1 user  group  150K  Dec  4 10:30  flatten.lynx.bundle
```

### 检查2：查看文件大小

```bash
# 生产版本：通常 50-200KB
# 开发版本：通常 200-500KB
```

### 检查3：在设备上验证

1. 打开Lynx Explorer
2. 输入URL: `file://lynx?local://showcase/flatten/main.lynx.bundle`
3. 点击打开
4. 应该直接进入演示页面（无需二级菜单）

---

## 🐛 常见问题

### Q: 脚本找不到？

```bash
# 检查脚本是否存在
ls -l /Users/luke/Projects/Github/lynx/build-flatten-demo.sh

# 如果不存在，重新创建
# 或者手动运行命令
cd /Users/luke/Projects/Github/lynx/explorer/showcase/menu/flatten
pnpm run build
```

### Q: 权限被拒绝？

```bash
# 添加执行权限
chmod +x /Users/luke/Projects/Github/lynx/build-flatten-demo.sh

# 再次运行
./build-flatten-demo.sh build
```

### Q: pnpm 找不到？

```bash
# 安装 pnpm
npm install -g pnpm

# 验证安装
pnpm --version
```

### Q: 构建失败？

```bash
# 清除缓存重试
./build-flatten-demo.sh clean

# 重新构建
./build-flatten-demo.sh build

# 如果还是失败，查看详细日志
cd /Users/luke/Projects/Github/lynx/explorer/showcase/menu/flatten
pnpm run build 2>&1 | tee build.log
```

### Q: 在设备上看不到修改？

```bash
# 1. 清除Lynx Explorer缓存
# 在设备上：设置 → 应用 → Lynx Explorer → 存储 → 清除缓存

# 2. 重新构建
./build-flatten-demo.sh build

# 3. 重新打开Lynx Explorer
```

---

## 💡 高级用法

### 只构建flatten，不构建其他demo

```bash
cd /Users/luke/Projects/Github/lynx/explorer/showcase/menu/flatten
pnpm run build
```

### 构建所有showcase demo

```bash
cd /Users/luke/Projects/Github/lynx/explorer/showcase
pnpm run build
```

### 查看详细构建日志

```bash
cd /Users/luke/Projects/Github/lynx/explorer/showcase/menu/flatten
pnpm run build --verbose 2>&1 | tee build.log
```

### 监听文件变化自动构建

```bash
cd /Users/luke/Projects/Github/lynx/explorer/showcase/menu/flatten
pnpm run build --watch
```

---

## 📁 重要文件位置

```
/Users/luke/Projects/Github/lynx/
├── build-flatten-demo.sh              ← 一键构建脚本
├── explorer/showcase/
│   ├── RUN_DEMO.md                    ← 详细运行指南
│   ├── menu/
│   │   ├── flatten/
│   │   │   ├── src/App.tsx            ← 演示页面代码
│   │   │   ├── package.json
│   │   │   └── dist/
│   │   │       └── flatten.lynx.bundle ← 构建输出
│   │   └── sub-menu/flatten.tsx       ← 菜单入口
│   └── dist/
│       └── flatten.lynx.bundle        ← 最终输出
```

---

## 🎯 一句话总结

```bash
# 修改代码后，只需要运行这一个命令
./build-flatten-demo.sh build

# 然后在Lynx Explorer中打开即可
# 不需要重新编译Lynx引擎！
```

---

## 📞 需要帮助？

1. 查看详细指南：`RUN_DEMO.md`
2. 查看脚本帮助：`./build-flatten-demo.sh help`
3. 查看构建日志：`pnpm run build 2>&1 | tee build.log`

---

**现在就开始构建吧！** 🚀

```bash
cd /Users/luke/Projects/Github/lynx
./build-flatten-demo.sh build
