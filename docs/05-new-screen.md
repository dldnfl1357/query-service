# 05. 새 화면 만들기 — 코드 자동 생성

쿼리 서비스는 **"1 화면 = 1 폴더"** 라는 규칙이 있습니다.
주문 목록 화면을 만들었던 것처럼, 새 화면을 만들 때도 비슷한 모양의 파일들이 약 10개 필요합니다.

매번 손으로 짜면? 지루하고 실수하기 쉽습니다.
그래서 **YAML 한 장 쓰고 명령어 한 줄** 로 끝낼 수 있게 만들었습니다.

## 만들어야 하는 화면 예시

"상품 상세 화면" 을 만든다고 해봅시다. 이 화면에는 다음 정보가 보입니다.

- **상품 정보** — 이름, 카테고리, 가격 (상품 서비스)
- **리뷰 정보** — 평균 평점, 리뷰 개수 (리뷰 서비스)
- **재고 정보** — 재고 수량, 재고 상태 (재고 서비스)

3개 도메인 데이터를 한 화면에 모아야 합니다. 주문 목록 화면이랑 같은 패턴이죠.

## Step 1. YAML 스펙 한 장 쓰기

`screens-specs/productDetail.yaml` 파일을 만듭니다.

```yaml
screen: productDetail            # 화면 이름 (camelCase)

primaryKey:                      # 화면 한 줄을 식별하는 키
  field: productId
  type: Long

sections:
  - name: product                # 첫 번째 섹션
    root: true                   # 이 도메인이 행을 "만드는" 주체
    queue: query-service.queues.product
    fields:
      - { name: productName,     type: String, length: 256 }
      - { name: productCategory, type: String, length: 64 }
      - { name: productPrice,    type: BigDecimal, precision: 19, scale: 2 }

  - name: review
    matchBy: productId           # productId 로 row 찾아서 update
    queue: query-service.queues.review
    fields:
      - { name: averageRating, type: BigDecimal, precision: 3, scale: 2 }
      - { name: reviewCount,   type: Integer }

  - name: inventory
    matchBy: productId
    queue: query-service.queues.inventory
    fields:
      - { name: stockQuantity, type: Integer }
      - { name: stockStatus,   type: String, length: 32 }

indexes:                          # 검색 빠르게 할 컬럼 지정
  - on: [productCategory]
```

### 스펙 읽는 법

- `screen`: 폴더/클래스 이름의 prefix. `productDetail` → `productDetailView`, `ProductDetailController` 등.
- `primaryKey`: 테이블 한 줄을 구분하는 키. 보통 root 도메인의 ID.
- `sections`: 도메인 한 개 = 섹션 한 개.
  - `root: true` 인 섹션이 **행을 만듭니다(upsert)**. 화면당 root 는 딱 하나.
  - 나머지 섹션은 **기존 행을 부분 업데이트(partial update)** 합니다.
  - `matchBy`: 어느 컬럼을 보고 row 들을 찾을지. 보통 root 의 PK 거나 FK.
- `fields`: 각 도메인이 가져올 컬럼들. 타입은 5가지만 지원: `Long`, `Integer`, `String`, `BigDecimal`, `Instant`.
- `indexes`: 자주 조회/필터링에 쓸 컬럼들에 인덱스를 만들어줌.

### 자동으로 추가되는 것들

스펙에 안 적었지만 코드 생성기가 알아서 넣어주는 것들이 있습니다.

- 각 섹션마다 `xxxSectionUpdatedAt` 컬럼 (순서 안전장치, 04장 참고)
- 모든 이벤트 record 에 `eventId`, `occurredAt`
- `created_at`, `updated_at` 컬럼 (자동 감사용)

## Step 2. 명령어 한 줄

```bash
./gradlew generateScreen --spec=screens-specs/productDetail.yaml
```

이 명령 하나로 다음 파일들이 한꺼번에 만들어집니다.

```
src/main/java/com/example/queryservice/screens/productDetail/
  entity/ProductDetailView.java
  repository/ProductDetailViewRepository.java
  service/ProductDetailWriteService.java
  service/ProductDetailQueryService.java
  handler/ProductDetailSqsHandlers.java
  event/ProductChangedEvent.java
  event/ReviewChangedEvent.java
  event/InventoryChangedEvent.java
  api/ProductDetailController.java
  api/ProductDetailResponse.java

src/main/resources/db/migration/
  V3__init_product_detail_view.sql   ← 번호는 자동 계산
```

생성된 코드는 **주문 목록과 똑같은 패턴** 입니다. 한 번 익혀두면 모든 화면이 같은 구조라 읽기 쉽습니다.

## Step 3. 손으로 마무리할 3가지

자동화하지 않은 게 3개 있습니다. **일부러** 사람이 직접 결정해야 하기 때문입니다.

### (1) 새 큐 이름을 application.yml 에 추가

```yaml
query-service:
  queues:
    order: order-changed
    member: member-changed
    product: product-changed
    delivery: delivery-changed
    review: review-changed       # ← 추가
    inventory: inventory-changed # ← 추가
```

### (2) 새 도메인 큐를 Terraform 에 추가

`infra/sqs.tf`:

```hcl
locals {
  domains = ["order", "member", "product", "delivery", "review", "inventory"]
  #                                                     ^^^^^^^  ^^^^^^^^^
}
```

이 한 줄에 도메인을 추가하면 main queue + DLQ + 알람이 자동으로 다 만들어집니다.
`terraform apply` 한 번 실행.

### (3) 마이그레이션 SQL 검토 후 DB 에 적용

생성된 `V3__init_product_detail_view.sql` 을 열어서 컬럼 타입/길이가 적절한지 한 번 보고,
운영 DB 에 마이그레이션 도구(Flyway 등)로 적용합니다.

## Step 4. 빌드/테스트

```bash
./gradlew build
```

컴파일 에러 없이 통과하면 새 화면이 준비된 겁니다.

---

## 작은 팁

### 화면 한 줄에 들어갈 데이터를 먼저 그려보기

YAML 을 쓰기 전에 **화면에 보일 한 줄을 종이에 그려보면** 컬럼이 자동으로 정해집니다.

```
[상품 상세 화면 한 줄]
┌──────────┬──────────┬────────┬──────┬──────┬──────┬──────┬──────┐
│ 상품ID    │ 상품명   │ 카테고리│ 가격 │ 평점 │ 리뷰수│ 재고 │ 상태 │
└──────────┴──────────┴────────┴──────┴──────┴──────┴──────┴──────┘
            └─── product ───┘  └ review ─┘  └─ inventory ─┘
                  섹션 1            섹션 2          섹션 3
```

그 다음에 "각 컬럼이 어느 부서(도메인)에서 오는지" 만 표시하면 YAML 이 거의 다 써집니다.

### root 섹션 고르기

"어느 도메인 변경이 이 화면에 새 줄을 만드는가?" 가 root 입니다.
주문 목록은 "새 주문이 생길 때" 새 줄이 생기니까 → `order` 가 root.
상품 상세는 "새 상품이 등록될 때" 새 줄이 생기니까 → `product` 가 root.

### 새 도메인은 새 큐, 새 핸들러

기존에 없던 도메인을 추가하려면 새 SQS 큐가 필요합니다.
**한 큐에 여러 도메인 메시지를 섞지 않습니다.** 그래야 한 도메인이 폭주해도 다른 도메인이 안 막힙니다.

---

## 한 줄 요약

1. 화면 한 줄에 보일 데이터를 종이에 그리기
2. 그대로 YAML 로 옮겨 적기
3. `./gradlew generateScreen --spec=...` 실행
4. 큐 이름 + Terraform + 마이그레이션 SQL 만 손으로 마무리

다음: [용어 사전](glossary.md)
