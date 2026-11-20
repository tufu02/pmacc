# PM Acc Manager v2（可覆盖安装的版本）

Pokemod 多账号快照 + Pokémon GO 登录回调捕获。

## 这版为“以后直接改 APK”做了什么
1. **固定签名**：`keystore/pmacc.jks`（alias `pmacc`，密码 `pmacc123`）已包含在工程里，
   `app/build.gradle.kts` 的 debug / release 都用它签名。
   → GitHub 构建出来的 APK 和本地修改后重新签名的 APK **签名一致，可以直接覆盖安装**，不需要卸载。
2. **UI 全部在 `assets/home.html`**：没有任何 layout XML。
   → 以后改界面只需要替换这个 HTML 文件，不用重新编译资源。
3. **所有业务逻辑在 `Bridge.kt`**：网页通过 `window.N.xxx()` 调用。
   → 改逻辑只需要改 dex（apktool 反编译 smali 后重新编译），不用碰 AndroidManifest / res。
4. **Manifest 已经预注册好**：
   - `HomeActivity`（LAUNCHER）
   - `CallbackActivity`（`pokemongo://` 和 `com.nianticlabs.pokemongo://` 回调）
   - `PanelActivity`（隐藏入口，可 `am start -n com.pg.pmacc/.PanelActivity` 打开同一面板）

## 打包
GitHub Actions（推荐）：推送到仓库 → Actions 自动构建 → Artifacts 下载 `pmacc-apk`。

本地：Android Studio 打开工程直接 Run，或用 `gradle assembleDebug`。

## 使用
1. 装好后 Magisk 授权 root（账号快照需要）
2. **登录回调**：Pokémon GO 点登录 → 浏览器完成 → 系统弹选择时选本应用 → 回到本应用列表点「登录 PoGo」
3. **账号快照**：在 Pokemod 登录某账号 → 本应用里「保存当前」→ 以后点「切换」即可

数据目录：
```
/sdcard/Android/data/com.pg.pmacc/files/callbacks/   登录回调
/sdcard/Android/data/com.pg.pmacc/files/accounts/    Pokemod 账号快照
```

## 本地改完 APK 后怎么签名（和我这边一致）
```bash
apksigner sign --ks keystore/pmacc.jks --ks-pass pass:pmacc123 \
  --key-pass pass:pmacc123 --ks-key-alias pmacc --out signed.apk unsigned.apk
```
这样产出的包可以直接覆盖安装。
