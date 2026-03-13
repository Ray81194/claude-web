# Redis シリアライゼーション検証デモ

Spring Boot + Spring Data Redis を使って、Jackson のシリアライズ/デシリアライズ戦略を検証するプロジェクトです。

## プロジェクト構成

```
redis-serialization-demo/
├── src/main/java/com/example/redisdemo/
│   ├── RedisDemoApplication.java
│   ├── config/
│   │   └── RedisConfig.java
│   └── dto/
│       ├── TransferDto.java           # メインDTO
│       └── type/
│           ├── AccountId.java         # 値オブジェクト（プレーン文字列シリアライズ）
│           ├── Money.java             # 値オブジェクト（@class 付きオブジェクト）
│           ├── OrderStatus.java       # 疑似Enum（コード文字列のみ、@class なし）
│           └── TransferType.java      # 疑似Enum（@class 付きオブジェクト）
└── src/test/java/com/example/redisdemo/
    └── DefaultSerializerOrderStatusTest.java

redis-serialization-approaches.md   # アプローチ比較ドキュメント
```

## Redis への保存形式（JSON）

```json
{
  "@class" : "com.example.redisdemo.dto.TransferDto",
  "fromAccount" : "AC-000001",
  "toAccount" : "AC-000002",
  "amount" : {
    "@class" : "com.example.redisdemo.dto.type.Money",
    "value" : 3000,
    "currency" : "JPY"
  },
  "status" : "01",
  "transferType" : {
    "@class" : "com.example.redisdemo.dto.type.TransferType",
    "code" : "02"
  }
}
```

## 型ごとのシリアライズ戦略

| 型 | JSON 形式 | 戦略 |
|---|---|---|
| `AccountId` | `"AC-000001"` | `@JsonTypeInfo(use=NONE)` + `@JsonValue` / `@JsonCreator` |
| `Money` | `{"@class":"...Money","value":3000,"currency":"JPY"}` | デフォルト（`@class` 自動付与） |
| `OrderStatus` | `"01"` | `@JsonTypeInfo(use=NONE)` + 独自 Serializer/Deserializer |
| `TransferType` | `{"@class":"...TransferType","code":"02"}` | 独自 Serializer（`serializeWithType`）+ Deserializer |

## 技術スタック

- **Java 21**
- **Spring Boot 3.5**
- **Spring Data Redis**
- **Lettuce**（Redis クライアント）
- **Jackson** — `GenericJackson2JsonRedisSerializer`

## 必要環境

- Java 21 以上
- Redis サーバー（localhost:6379）
- Gradle 8 以上

## 実行方法

```bash
# Redis 起動
redis-server --daemonize yes

# テスト実行
gradle test
```

## アプローチ比較

シリアライズ戦略の詳細な比較は [`redis-serialization-approaches.md`](redis-serialization-approaches.md) を参照。
