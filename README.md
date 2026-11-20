# PM Acc Manager

Pokemod 多账号快照管理器（root）。
Pokemod 免费版只能存 2 个账号，这个工具在它之外按快照保存，账号数量不限。

## 原理
启动 Pokemod 前，它的账号数据在：
```
/data/data/com.pokemod.app.public/shared_prefs/
├── Hawk2.xml                                  AUTH_TOKEN / REFRESH_TOKEN / EMAIL / TIER / ACCOUNT_ORDER
├── com.pokemod.app.public_preferences.xml     activeAccount / ptcVerifier
├── prefs.xml
└── crypto.KEY_256.xml                         Hawk 密钥
/data/data/com.pokemod.app.public/files/agents/   注入会话
```
本工具对上述文件做整组快照；切换时 force-stop Pokemod → 写回快照 → 修正 owner/selinux → 重新启动。

## 打包
Android Studio：直接 Open 本目录，等待 Gradle Sync，Build > Build Bundle(s)/APK(s) > Build APK(s)。

命令行（需要 JDK 17 + Android SDK）：
```bash
./gradlew assembleDebug
# 或者
./gradlew assembleRelease
```
产物：
```
app/build/outputs/apk/debug/app-debug.apk
```

如需 release 签名，在 `app/build.gradle.kts` 里加 signingConfigs，或用 Android Studio 的 Generate Signed Bundle/APK。

## 使用
1. 安装后首次打开，Magisk 会弹 root 授权，允许。
2. 在 Pokemod 里登录某个账号。
3. 打开本工具 → 点「保存当前账号」→ 起个名字。
4. 要换账号：在 Pokemod 里登录另一个账号，再保存一次。
5. 之后在列表里点「切换」，会自动关闭 Pokemod、写入该账号数据、再打开 Pokemod。

快照保存位置（可用 MT 管理器查看/备份）：
```
/sdcard/Android/data/com.pg.pmacc/files/accounts/<账号名>/
```

## 注意
- 必须 root（Magisk / KernelSU 均可）
- 切换时 Pokemod 会被强制关闭，正在运行的任务会中断
- 如果提示“未获得 root”，检查 Magisk 里是否已授权 com.pg.pmacc
- Pokemod 包名写死在 `MainActivity.kt` 的 `pkg` 字段，换包名改这里

## 用 GitHub Actions 打包（不用本地装 Android Studio）

仓库里已经带好 `.github/workflows/build.yml`，推到 GitHub 后自动构建：

1. 新建一个 GitHub 仓库（Public / Private 都行）
2. 把本目录所有文件（**包括 `.github` 这个隐藏目录**）上传到仓库根目录
   - 网页上传要注意：拖拽文件夹时 `.github` 容易被忽略，可以用 `git push`
   ```bash
   git init
   git add -A
   git commit -m "init"
   git branch -M main
   git remote add origin https://github.com/<你的用户名>/<仓库名>.git
   git push -u origin main
   ```
3. 打开仓库的 **Actions** 标签 → 左侧 `build-apk` → 右上 **Run workflow**
4. 等 3～5 分钟，进入这次运行，页面底部 **Artifacts** 下载 `pmacc-debug-apk`
5. 解压得到 `app-debug.apk`，装到手机上即可（debug 签名，能直接安装）

如果构建在 `Install SDK packages` 报错，把 `build-tools;34.0.0` 换成 runner 已有的版本（日志里会列出可用版本）。

## 登录回调捕获（复刻 Pokemod 的“添加账号”）

Pokemod 能“添加账号”，是因为它在 AndroidManifest 里和 PoGo 抢同一个 deep link：
```xml
<data android:scheme="pokemongo" />
```
浏览器登录完 PoGo 账号后，系统弹出“用哪个应用打开”，选 Pokemod 就把登录回调交给它。

本应用做了同样的事：
1. `CallbackActivity` 注册了 `pokemongo` scheme
2. 在 PoGo 里点登录 → 浏览器完成登录 → 系统弹选择 → 选 **PM Acc Manager**
3. 回调原文会存到 `/sdcard/Android/data/com.pg.pmacc/files/callbacks/callback-<时间>.txt`
4. 列表里点「登录到 PoGo」，会把这份回调原样发给 Pokémon GO（`am start -a VIEW -d <uri> -p com.nianticlabs.pokemongo`），游戏据此完成登录

注意：Pokemod 在收到回调后会做一次 token 交换并保存长期有效的 AUTH_TOKEN / REFRESH_TOKEN；本应用只保存回调原文。如果回调里的授权码是一次性的或已过期，“登录到 PoGo”可能无效，需要重新走一次登录。

## v3（原生界面版）

- 界面改回原生控件（LinearLayout + 按钮），不再使用 WebView —— 之前 WebView 在部分机型上按钮无响应
- 保留登录回调捕获（`CallbackActivity`），保存后自动跳到主界面
- 固定签名 `keystore/pmacc.jks`（alias `pmacc`，密码 `pmacc123`），GitHub 构建的包可直接覆盖安装

功能：
1. 账号快照：Pokemod 登录某账号 → 「保存当前账号」→ 列表点「切换」
2. 登录回调：PoGo 登录 → 浏览器完成 → 系统弹窗选本应用 → 回主界面点「登录到 PoGo」
