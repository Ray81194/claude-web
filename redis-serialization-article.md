# Claude を使って技術ドキュメントを育てる：Jackson 設計比較の実例

## この記事について

Claude に技術的な検討ドキュメントを作らせ、会話のなかで段階的に整備していく実例を紹介する。「どんな依頼をしたら何が出てきたか」を具体的に展開する。

---

## 依頼 1：比較ドキュメントを生成する

### 出した指示

```
TransferType のような値オブジェクトを Jackson でシリアライズ/デシリアライズする
方法を3パターン比較してほしい。

- Approach 1: @JsonTypeInfo(use=NONE) + 独自 Serializer/Deserializer
- Approach 2: @JsonTypeInfo 削除 + 独自 Serializer（型付き）+ 独自 Deserializer
- Approach 3: @JsonTypeInfo 削除 + 独自 Serializer（型付き）+ @JsonCreator

各アプローチで JSON 出力例、実装イメージ、メリット・デメリットをまとめてほしい。
最後に比較表と推奨を書いてほしい。
```

### ポイント

- **選択肢を自分で列挙した**。「いくつかパターンを考えて」よりも、検討したいアプローチを明示すると Claude が軸をブラさずにまとめてくれる。
- **出力の構成を指定した**（JSON 出力例・実装イメージ・メリデメ・比較表・推奨）。セクション構成を指定することで、後から読みやすい形になる。

### 出力されたドキュメント構成

各アプローチに以下のセクションが生成された。

```
## Approach N: ～
### JSON 出力例       ← 実際の JSON 形式
### 実装イメージ      ← 概念的なコードスニペット（TransferType で説明）
### 実際のコード      ← 具体実装（OrderStatus / TransferType クラス全体）
### メリット
### デメリット
```

末尾に比較表と推奨セクションも生成された。

#### JSON 出力例（Approach 1）

```json
"transferType": "02"
```

#### JSON 出力例（Approach 2 / 3）

```json
"transferType": {
  "@class": "com.example.TransferType",
  "code": "02"
}
```

#### 比較表（生成されたもの）

| 観点 | Approach 1 | Approach 2 | Approach 3 |
|------|-----------|-----------|-----------|
| JSON サイズ | ◎ コンパクト | △ 肥大 | △ 肥大 |
| 実装のシンプルさ | ◎ シンプル | × 複雑 | ○ 標準的 |
| 保守性 | ◎ 高い | × 低い | ○ 普通 |
| ドメインモデルへの影響 | ○ フィールド側のみ | ◎ なし | △ Jackson 依存が入る |
| Jackson 標準準拠 | ○ 公式 API 使用 | ○ 標準挙動と整合 | ◎ 最も標準的 |
| `@JsonTypeInfo` 依存 | △ 必要 | ◎ 不要 | ◎ 不要 |

---

## 依頼 2：ドキュメントを整理する

### 出した指示

```
実装イメージに追加する形にして。approach3にも追加して変数宣言だけでいい
```

**16文字の指示。**

### Claude が読み取ったこと

この短い指示から Claude は以下を判断した。

1. 「実装イメージに追加する形」＝ 別セクション「実際のコード」を廃止し、「実装イメージ」に統合する
2. 「approach3にも追加」＝ Approach 3 だけコードが薄かったので追記する
3. 「変数宣言だけでいい」＝ Approach 3 は全体実装ではなくフィールド定義とクラス宣言のみ

### 変更前（Approach 1 の構造）

```
### 実装イメージ
  TransferType を使った概念コード（フィールド + Serializer/Deserializer）

### 実際のコード（OrderStatus）
  TransferDto.java のフィールド定義
  OrderStatus.java のクラス全体
```

### 変更後（Approach 1 の構造）

```
### 実装イメージ
  TransferDto.java のフィールド定義    ← 追加
  OrderStatus.java のクラス全体        ← 追加（ファイル名ラベル付き）
```

具体的には以下のようにファイル名をラベルとして付けた形に整理された。

**`TransferDto.java`**
```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@JsonSerialize(using = OrderStatus.Serializer.class)
@JsonDeserialize(using = OrderStatus.Deserializer.class)
private OrderStatus status;
```

**`OrderStatus.java`**
```java
public class OrderStatus {

    public static final OrderStatus PENDING   = new OrderStatus("01", "保留");
    public static final OrderStatus CONFIRMED = new OrderStatus("02", "確定");

    private static final Map<String, OrderStatus> CODE_MAP = Map.of(
            "01", PENDING,
            "02", CONFIRMED
    );

    private final String code;
    private final String label;

    private OrderStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode()  { return code; }
    public String getLabel() { return label; }

    public static OrderStatus of(String code) {
        OrderStatus status = CODE_MAP.get(code);
        if (status == null) throw new IllegalArgumentException("Unknown code: " + code);
        return status;
    }

    public static class Serializer extends JsonSerializer<OrderStatus> {
        @Override
        public void serialize(OrderStatus value, JsonGenerator gen,
                              SerializerProvider provider) throws IOException {
            gen.writeString(value.getCode());
        }
    }

    public static class Deserializer extends JsonDeserializer<OrderStatus> {
        @Override
        public OrderStatus deserialize(JsonParser p,
                                       DeserializationContext ctxt) throws IOException {
            return OrderStatus.of(p.getValueAsString());
        }
    }
}
```

### Approach 3 に追加された変数宣言

「変数宣言だけでいい」という指示通り、実装本体ではなく宣言のみが追加された。

**`TransferDto.java`**
```java
@JsonSerialize(using = TransferType.Serializer.class)
private TransferType transferType;
```

**`TransferType.java`**
```java
@JsonSerialize(using = TransferType.Serializer.class)
public class TransferType {
    // ...
}
```

Approach 2 と比べると `@JsonDeserialize` アノテーションがない。これにより「Approach 3 は独自 Deserializer が不要」という差分が宣言から視覚的に読み取れるようになった。

### この依頼で学べること

- **省略語・文脈依存の指示でも伝わる**。「実装イメージに追加する形」という曖昧な表現でも、直前のドキュメント構造を把握した上で正しく解釈される。
- **制約を付けると出力を絞れる**。「変数宣言だけでいい」と明示したことで、Approach 3 だけ過剰に実装されることを防いだ。

---

## 依頼 3：記事化する

### 出した指示（1回目）

```
今までの内容を記事にしたい。
どのようなプロンプトでどのようなコードとなったかの流れをまとめてほしい。
```

### 出た出力

プロンプトと出力の対応を中心にした技術記事が生成された。構成は以下。

```
# Jackson 値オブジェクトのシリアライゼーション設計：プロンプト駆動での比較ドキュメント作成

## はじめに（背景説明）
## 背景：何が問題だったか
## Step 1：3つのアプローチの洗い出し
  ### プロンプト（引用）
  ### 生成されたドキュメント構成（表）
## Step 2：実装イメージとコードの統合
  ### プロンプト（引用）
  ### 変更の意図
  ### 統合後の構成（コードスニペット）
## 最終的な比較まとめ（表）
## 推奨と結論
## ドキュメント整備の流れまとめ（ASCII フロー図）
```

### 出した指示（2回目・本記事）

```
claudeの使い方の観点で再作成して。どんな依頼で出力がどうなったかを展開したい
```

### 1回目との違い

| 観点 | 1回目 | 2回目（本記事） |
|------|-------|----------------|
| 主語 | 技術内容（Jackson 設計） | Claude の使い方 |
| 読者 | Jackson を調べている人 | Claude の使い方を知りたい人 |
| 展開の軸 | 技術的な比較 | 依頼→出力→気づき の繰り返し |
| コード量 | 要所のみ | 変更前後を並べて展開 |

「観点を変えて再作成」という指示で、同じ素材から全く異なる切り口の記事が生成された。

---

## まとめ：依頼のパターンと効果

| 依頼パターン | 効果 | 本記事での例 |
|-------------|------|-------------|
| 選択肢を自分で列挙して比較を頼む | 軸がブレない。比較表が充実する | Approach 1〜3 を事前に定義 |
| 出力の構成を指定する | セクション構成が安定する | JSON 出力例・実装イメージ・メリデメ・比較表・推奨 |
| 短い指示で差分変更を頼む | 既存構造を壊さず必要箇所だけ修正される | 「実装イメージに追加する形にして」 |
| 制約を付けて出力を絞る | 過剰な生成を防げる | 「変数宣言だけでいい」 |
| 観点を指定して再作成を頼む | 同じ素材から別の記事が生成できる | 「claudeの使い方の観点で再作成して」 |
