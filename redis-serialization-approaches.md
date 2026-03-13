# Redis シリアライゼーション アプローチ比較

## 概要

`TransferType` のような値オブジェクトを Jackson でシリアライズ/デシリアライズする際の3つのアプローチを比較する。

---

## Approach 1: `@JsonTypeInfo(use=NONE)` + 独自 Serializer/Deserializer（元の設計）

### JSON 出力例
```json
"transferType": "02"
```

### 実装イメージ
```java
// DTO フィールド側
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@JsonSerialize(using = TransferType.Serializer.class)
@JsonDeserialize(using = TransferType.Deserializer.class)
private TransferType transferType;

// TransferType 内
public static class Serializer extends JsonSerializer<TransferType> {
    @Override
    public void serialize(TransferType value, JsonGenerator gen, SerializerProvider p) throws IOException {
        gen.writeString(value.getCode());
    }

    @Override
    public void serializeWithType(TransferType value, JsonGenerator gen, SerializerProvider p, TypeSerializer typeSer) throws IOException {
        serialize(value, gen, p); // 型情報を抑制してプレーン文字列で出力
    }
}

public static class Deserializer extends JsonDeserializer<TransferType> {
    @Override
    public TransferType deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        return TransferType.of(p.getText());
    }
}
```

### メリット
- JSON がコンパクト（プレーン文字列 `"02"`）
- Serializer/Deserializer の実装がシンプル
- `@JsonTypeInfo(use=NONE)` は Jackson の公式メカニズムで意図が明確
- フィールド側で「この型は独自処理する」と宣言できる

### デメリット
- `@JsonTypeInfo(use=NONE)` を知らない人には非直感的に見える
- `serializeWithType` が `serialize` を呼ぶだけで、なぜ型情報を抑制するのか読み取りにくい
- アノテーションがフィールド側に分散する

---

## Approach 2: `@JsonTypeInfo` 削除 + 独自 Serializer（型付き）+ 独自 Deserializer（現在の実装）

### JSON 出力例
```json
"transferType": {
  "@class": "com.example.TransferType",
  "code": "02"
}
```

### 実装イメージ
```java
// TransferType クラス側
@JsonSerialize(using = TransferType.Serializer.class)
@JsonDeserialize(using = TransferType.Deserializer.class)
public class TransferType {

    public static class Serializer extends JsonSerializer<TransferType> {
        @Override
        public void serialize(TransferType value, JsonGenerator gen, SerializerProvider p) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("code", value.getCode());
            gen.writeEndObject();
        }

        @Override
        public void serializeWithType(TransferType value, JsonGenerator gen, SerializerProvider p, TypeSerializer typeSer) throws IOException {
            WritableTypeId typeId = typeSer.typeId(value, JsonToken.START_OBJECT);
            typeSer.writeTypePrefix(gen, typeId);
            gen.writeStringField("code", value.getCode());
            typeSer.writeTypeSuffix(gen, typeId);
        }
    }

    public static class Deserializer extends JsonDeserializer<TransferType> {
        @Override
        public TransferType deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            // パーサー状態によって分岐が必要（複雑）
            if (p.currentToken() == JsonToken.START_OBJECT) {
                p.nextToken(); // FIELD_NAME
            }
            String code = null;
            while (p.currentToken() != JsonToken.END_OBJECT) {
                if ("code".equals(p.currentName())) {
                    p.nextToken();
                    code = p.getText();
                }
                p.nextToken();
            }
            return TransferType.of(code);
        }
    }
}
```

### メリット
- `@JsonTypeInfo` アノテーションが不要
- 型情報が JSON に自然な形で入り Jackson の標準挙動と整合する
- `TransferType` クラスが自己完結（フィールド側への指示不要）

### デメリット
- JSON サイズが増える（`@class` フィールドが追加）
- `Deserializer.deserialize` がパーサー状態（`START_OBJECT` か `FIELD_NAME` か）を意識した複雑な実装になる
- パーサー状態の深い知識が必要で**バグリスクが高く保守性が低い**

---

## Approach 3: `@JsonTypeInfo` 削除 + 独自 Serializer（型付き）+ `@JsonCreator`

### JSON 出力例
```json
"transferType": {
  "@class": "com.example.TransferType",
  "code": "02"
}
```

### 実装イメージ
```java
// TransferType クラス側
@JsonSerialize(using = TransferType.Serializer.class)
public class TransferType {

    // @JsonCreator でデシリアライズを標準化
    @JsonCreator
    public static TransferType of(@JsonProperty("code") String code) {
        return new TransferType(code);
    }

    public static class Serializer extends JsonSerializer<TransferType> {
        @Override
        public void serialize(TransferType value, JsonGenerator gen, SerializerProvider p) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("code", value.getCode());
            gen.writeEndObject();
        }

        @Override
        public void serializeWithType(TransferType value, JsonGenerator gen, SerializerProvider p, TypeSerializer typeSer) throws IOException {
            WritableTypeId typeId = typeSer.typeId(value, JsonToken.START_OBJECT);
            typeSer.writeTypePrefix(gen, typeId);
            gen.writeStringField("code", value.getCode());
            typeSer.writeTypeSuffix(gen, typeId);
        }
    }
}
```

### メリット
- 独自 Deserializer クラスが不要
- `@JsonCreator` は Jackson の標準的な慣用句で可読性が高い
- Approach 2 のパーサー状態の複雑さがない

### デメリット
- JSON サイズが増える（Approach 2 と同じ）
- `of(String)` メソッドに `@JsonCreator`/`@JsonProperty` という Jackson アノテーションが混入し、ドメインモデルに関心の分離が崩れる
- 独自 Serializer と `serializeWithType` は依然必要

---

## 比較まとめ

| 観点 | Approach 1 | Approach 2 | Approach 3 |
|------|-----------|-----------|-----------|
| JSON サイズ | ◎ コンパクト (`"02"`) | △ 肥大 (`@class` 付き) | △ 肥大 (`@class` 付き) |
| 実装のシンプルさ | ◎ シンプル | × 複雑（パーサー状態） | ○ 標準的 |
| 保守性 | ◎ 高い | × 低い | ○ 普通 |
| ドメインモデルへの影響 | ○ フィールド側のみ | ◎ なし | △ Jackson 依存が入る |
| Jackson 標準準拠 | ○ 公式 API 使用 | ○ 標準挙動と整合 | ◎ 最も標準的 |
| `@JsonTypeInfo` 依存 | △ 必要 | ◎ 不要 | ◎ 不要 |

---

## 推奨: **Approach 1**

### 理由

1. **意図が最も明確** — `@JsonTypeInfo(use=NONE)` は「この型に Jackson デフォルト型付けを使わない」という公式メカニズム
2. **実装がシンプル** — Serializer/Deserializer ともに文字列の読み書きだけで済む
3. **JSON がコンパクト** — `"02"` だけで `@class` が不要になりデータ効率が良い
4. **保守性が高い** — パーサー状態の知識が不要で誰でも読める

### 次善策

`@JsonTypeInfo` を絶対に使いたくない場合は **Approach 3** を選ぶ。
ただし、ドメインモデルへの Jackson 依存と JSON 肥大化というコストが伴う。

Approach 2 は Approach 3 に比べて明確な優位点がなく、パーサー状態の複雑さによる保守リスクが高いため **非推奨**。
