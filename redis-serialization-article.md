# Jackson 値オブジェクトのシリアライゼーション設計：プロンプト駆動での比較ドキュメント作成

## はじめに

Spring Boot + Redis 環境で `TransferType` のような値オブジェクトを Jackson でシリアライズ／デシリアライズする際、どの設計を採用するかで実装の複雑さや保守性が大きく変わる。本記事では、3つのアプローチを比較検討した過程と、AI との対話を通じてドキュメントを整備した流れをまとめる。

---

## 背景：何が問題だったか

Redis にオブジェクトをキャッシュする際、Jackson のデフォルト型付け（`@class` 埋め込み）が有効な環境では、値オブジェクトの扱いに工夫が必要になる。

- `TransferType` のような **コードと名称のペアを持つ値オブジェクト** は、JSON 上はコード文字列 `"02"` だけで十分
- しかし Jackson の標準挙動では `{"@class": "...", "code": "02"}` という肥大した形式になる
- Serializer / Deserializer をカスタム実装するにしても、どこにアノテーションを置くか、どこまで実装するかで複数の選択肢がある

---

## Step 1：3つのアプローチの洗い出し

### プロンプト

> `TransferType` のような値オブジェクトを Jackson でシリアライズ/デシリアライズする方法を3パターン比較してほしい。
> - Approach 1: `@JsonTypeInfo(use=NONE)` + 独自 Serializer/Deserializer
> - Approach 2: `@JsonTypeInfo` 削除 + 独自 Serializer（型付き）+ 独自 Deserializer（現在の実装）
> - Approach 3: `@JsonTypeInfo` 削除 + 独自 Serializer（型付き）+ `@JsonCreator`

### 生成されたドキュメント構成

各アプローチについて以下の軸で整理したドキュメントが生成された。

| セクション | 内容 |
|-----------|------|
| JSON 出力例 | 実際の JSON 形式 |
| 実装イメージ | 概念的なコードスニペット |
| 実際のコード | `OrderStatus` / `TransferType` を用いた具体実装 |
| メリット／デメリット | 設計上の観点からの評価 |

#### Approach 1 の JSON 出力例
```json
"transferType": "02"
```

#### Approach 2 / 3 の JSON 出力例
```json
"transferType": {
  "@class": "com.example.TransferType",
  "code": "02"
}
```

---

## Step 2：実装イメージとコードの統合

### プロンプト

> 実装イメージに追加する形にして。approach3 にも追加して変数宣言だけでいい

### 変更の意図

初版では「実装イメージ」（概念コード）と「実際のコード」（具体実装）が別セクションに分かれており、同じ情報が二か所に散らばっていた。このプロンプトにより：

1. **「実際のコード」セクションを廃止**し、「実装イメージ」に統合
2. 各アプローチで **`TransferDto.java`（フィールド定義）** と **型クラス（`OrderStatus.java` / `TransferType.java`）** をセットで示す構成に変更
3. Approach 3 には実装が存在しなかったため、**変数宣言のみ**を追加

### 統合後の構成（Approach 1 の例）

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

    // ... コンストラクタ、getter、of() ...

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

Approach 3 では `@JsonDeserialize` が不要な点が Approach 2 との差分として宣言から読み取れるようになった。

---

## 最終的な比較まとめ

| 観点 | Approach 1 | Approach 2 | Approach 3 |
|------|-----------|-----------|-----------|
| JSON サイズ | ◎ コンパクト (`"02"`) | △ 肥大 (`@class` 付き) | △ 肥大 (`@class` 付き) |
| 実装のシンプルさ | ◎ シンプル | × 複雑（パーサー状態） | ○ 標準的 |
| 保守性 | ◎ 高い | × 低い | ○ 普通 |
| ドメインモデルへの影響 | ○ フィールド側のみ | ◎ なし | △ Jackson 依存が入る |
| Jackson 標準準拠 | ○ 公式 API 使用 | ○ 標準挙動と整合 | ◎ 最も標準的 |
| `@JsonTypeInfo` 依存 | △ 必要 | ◎ 不要 | ◎ 不要 |

---

## 推奨と結論

**Approach 1 を推奨。**

- `@JsonTypeInfo(use=NONE)` は Jackson 公式メカニズムであり意図が明確
- Serializer / Deserializer の実装が文字列の読み書きのみでシンプル
- JSON がコンパクトで `@class` フィールドが不要

`@JsonTypeInfo` を使いたくない場合は **Approach 3** が次善策。ただし `of()` メソッドへの Jackson アノテーション混入（関心の分離の崩れ）と JSON 肥大化のコストを受け入れる必要がある。

**Approach 2 は非推奨。** Approach 3 と比べて明確な優位点がなく、パーサー状態を意識した Deserializer の複雑さが保守リスクを高める。

---

## ドキュメント整備の流れまとめ

```
[Step 1] 3アプローチの比較ドキュメント生成
         ↓
         概念コード（実装イメージ）と具体コード（実際のコード）が別セクションに分離
         ↓
[Step 2] 「実装イメージに追加する形にして」
         + 「approach3 にも追加して変数宣言だけでいい」
         ↓
         実際のコードを実装イメージに統合
         Approach 3 に DTO フィールド + クラス宣言を追加
         セクション数を削減してドキュメントをコンパクト化
```

シンプルな指示でも「どのファイルに何を書くか」という構造的な変更を正確に反映できた。
