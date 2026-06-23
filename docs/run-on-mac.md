# Mac で DiscGun をビルドして遊ぶ手順(初心者向け)

> 目標:このMODを自分の Minecraft(Java版 1.21.1)で動かす。
> 詰まったら、ターミナルに出たエラー文をそのままチャットに貼ってください。

## 前提
- **Minecraft Java Edition** を持っていること(統合版/BE では動きません)
- ネットにつながる Mac

---

## ① Java 21 を入れる
ビルドに必要です。

1. https://adoptium.net/temurin/releases/?version=21 を開く
2. **Operating System: macOS** を選ぶ
3. Mac のチップを選ぶ
   - Apple のメニュー  →「このMacについて」→ チップが **M1/M2/M3/M4** なら **aarch64**、**Intel** なら **x64**
4. **.pkg** をダウンロードして、ダブルクリックでインストール

確認:ターミナルで `java -version` と打って `21` と出ればOK。

---

## ② プロジェクトをダウンロード
1. https://github.com/Risudayooo/d を開く
2. 緑の **「Code」** ボタン → **「Download ZIP」**
3. ダウンロードした zip をダブルクリックで展開 → **`d-main`** フォルダができる

---

## ③ ビルドして MOD ファイル(.jar)を作る
1. **ターミナル** を開く(アプリ → ユーティリティ → ターミナル)
2. `cd ` と入力(cd のあとにスペース)→ さっきの **`d-main` フォルダをターミナルにドラッグ&ドロップ** → Enter
3. 次を入力して Enter:
   ```
   ./gradlew build
   ```
4. **初回は数分**かかります(Gradle・Minecraft・Fabric を自動ダウンロード)
5. 最後に **`BUILD SUCCESSFUL`** と出れば成功
6. できた MOD は **`d-main/build/libs/discgun-0.1.0.jar`**

> ❌ `BUILD FAILED` が出たら、その上に出ているエラー文をチャットに貼ってください。直します。

---

## ④ Fabric を Minecraft に入れる
このMODは「Fabric」という土台の上で動きます。

1. https://fabricmc.net/use/installer/ から **インストーラー(universal .jar)** をダウンロード
2. ダブルクリックで起動(開けない時は右クリック →「開く」)
3. **Minecraft Version: 1.21.1** を選んで **Install**
4. これで公式ランチャーに「fabric-loader 1.21.1」のプロファイルが増えます

---

## ⑤ 前提MOD「Fabric API」を入れる
1. https://modrinth.com/mod/fabric-api を開く
2. **1.21.1** 対応・**Fabric** 版の .jar をダウンロード
3. mods フォルダに入れる。場所(Mac):
   ```
   ~/Library/Application Support/minecraft/mods
   ```
   - 無ければフォルダを作る。Finder で「移動」→「フォルダへ移動」に上のパスを貼ると開けます

---

## ⑥ 自作MODを入れる
③で作った **`discgun-0.1.0.jar`** を、⑤と同じ **mods フォルダ**にコピー。

---

## ⑦ 起動してテスト
1. Minecraft ランチャーで **Fabric(1.21.1)** のプロファイルを選んで起動
2. クリエイティブで持ち物を開き、**「DiscGun」タブ**から銃とディスクを取り出す
3. 動作:
   - ディスクを右クリック → 持っている銃に装填(テスト用)
   - 銃を持って **左クリックで射撃 / 右クリックでディスク固有アクション**
   - **左Alt: ブリンク / 空中スペース: 二段ジャンプ / F: パリィ / R: リロード**

> ⚠️ テクスチャは未同梱なので、アイテムは紫×黒の「欠けた見た目」になります(動作には影響なし)。
> ⚠️ パリィの F はバニラの「オフハンド持ち替え」と被ります。気になる場合は設定でバニラ側のキーを外してください。

---

## うまくいかない時
- まずは **③のビルド**を通すのが第一目標です。`BUILD FAILED` のエラー文を貼ってもらえれば、こちらでコードを直して push します。
- 起動後にクラッシュした場合は、クラッシュレポート(`~/Library/Application Support/minecraft/crash-reports/` の最新ファイル)を貼ってください。
