# Windows + CurseForge で DiscGun を動かす手順

> 流れ:GitHub から取得 → `gradlew build` で `.jar` を作る → CurseForge の起動構成(1.21.1 + Fabric)の mods フォルダに入れて起動。
> 詰まったら、出たエラー文をそのままチャットに貼ってください。

## 前提
- **Minecraft Java Edition** を持っていること(統合版/BE では動きません)
- Windows 10 / 11

---

## ① Java 21(JDK)を入れる ※ビルド用
1. https://adoptium.net/temurin/releases/?version=21 を開く
2. **Operating System: Windows / Architecture: x64** を選ぶ
3. **.msi** をダウンロードして実行 → インストール
   - 途中の「Set JAVA_HOME」「Add to PATH」にチェックが付いていればなお良い

確認:コマンドプロンプトで `java -version` → `21` と出ればOK。

> ※ Minecraft を動かす Java は CurseForge が用意してくれるので、JDK 21 は「ビルド用」です。

---

## ② プロジェクトを GitHub から取得
1. https://github.com/Risudayooo/d を開く
2. 緑の **「Code」** → **「Download ZIP」**
3. ダウンロードした zip を**右クリック →「すべて展開」** → `d-main` フォルダができる

---

## ③ ビルドして `.jar` を作る
1. `d-main` フォルダを開く
2. フォルダの**アドレスバー**(上の場所が出ている所)をクリックして **`cmd`** と入力 → Enter
   - そのフォルダでコマンドプロンプトが開きます
   - (Windows 11 なら、フォルダ内で右クリック →「ターミナルで開く」でもOK)
3. 次を入力して Enter:
   ```
   gradlew build
   ```
   - PowerShell の場合は `.\gradlew build`
4. **初回は数分**(Gradle・Minecraft・Fabric を自動ダウンロード)
5. **`BUILD SUCCESSFUL`** と出れば成功
6. できた MOD は **`d-main\build\libs\discgun-0.1.0.jar`**

> ❌ `BUILD FAILED` が出たら、その上のエラー文をチャットに貼ってください。直して push し直します。

---

## ④ CurseForge アプリで起動構成を作る
1. https://www.curseforge.com/download/app からアプリをインストール
2. 左で **Minecraft** を選ぶ
3. **Create Custom Profile**
4. **Game Version: 1.21.1** / **Modloader: Fabric** → Create
   - Fabric Loader が自動で入ります

---

## ⑤ Fabric API を入れる(前提MOD)
- 作ったプロファイルを開く → **Add More Content**
- **Fabric API** を検索 → Install(1.21.1 用が入ります)

---

## ⑥ 自作MODの .jar を入れる
1. プロファイルの **「︙(三点メニュー)」→ Open Folder**(フォルダを開く)
2. 開いた中の **`mods`** フォルダに、③で作った **`discgun-0.1.0.jar`** をコピー
   - 既定の場所はだいたい:
     ```
     C:\Users\<ユーザー名>\curseforge\minecraft\Instances\<プロファイル名>\mods
     ```

---

## ⑦ 起動してテスト
1. CurseForge でそのプロファイルの **Play**
2. クリエイティブで持ち物を開き、**「DiscGun」タブ**から銃とディスクを取り出す
3. 操作:
   - ディスクを右クリック → 持っている銃に装填(テスト用)
   - 銃を持って **左クリック射撃 / 右クリックでディスク固有アクション**
   - **左Alt: ブリンク / 空中スペース: 二段ジャンプ / F: パリィ / R: リロード**

> ⚠️ テクスチャ未同梱なので、アイテムは紫×黒の見た目になります(動作には影響なし)。
> ⚠️ パリィの F はバニラの「オフハンド持ち替え」と被ります。気になる場合は設定でバニラ側を外してください。

---

## コードを直したあとの更新
1. (修正を受け取ったら)GitHub から ZIP を取り直す or `git pull`
2. `gradlew build` で新しい `.jar` を作る
3. mods フォルダの古い `discgun-*.jar` を削除し、新しいものをコピー
4. CurseForge で **Play**

## うまくいかない時
- まず **③のビルド成功**が目標。`BUILD FAILED` のエラー文を貼ってください → コードを直します。
- 起動後にクラッシュしたら、クラッシュレポート(プロファイルの `crash-reports\` 内の最新ファイル)を貼ってください。
