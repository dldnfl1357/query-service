# 03. 주문 목록 예제 따라가기

여기서는 **실제 코드** 가 어떻게 짜여 있는지, 데이터 한 건이 어떻게 흘러가는지 따라가 봅니다.

## 폴더 구조 먼저 보기

```
src/main/java/com/example/queryservice/
├── QueryServiceApplication.java        ← 프로그램 시작점
├── config/                              ← 전체 설정
│   ├── VirtualThreadConfig.java        (가상 스레드 켜기)
│   ├── JpaConfig.java                  (DB 사용 설정)
│   └── QueueProperties.java            (우체통 이름들)
├── common/idempotency/                  ← 중복 메시지 막기 (04장에서)
└── screens/orderlist/                   ← 주문 목록 화면 = 1 폴더
    ├── entity/OrderListView.java        (테이블)
    ├── repository/OrderListViewRepository.java (테이블 사용 코드)
    ├── event/                           (편지 양식들)
    │   ├── OrderChangedEvent.java
    │   ├── MemberChangedEvent.java
    │   ├── ProductChangedEvent.java
    │   └── DeliveryChangedEvent.java
    ├── handler/OrderListSqsHandlers.java (우체통에서 편지 꺼내기)
    ├── service/
    │   ├── OrderListWriteService.java   (테이블에 적기)
    │   └── OrderListQueryService.java   (테이블에서 읽기)
    └── api/
        ├── OrderListController.java     (화면용 창구)
        └── OrderListResponse.java       (화면에 줄 데이터 양식)
```

## 등장인물 5명

### 1) Entity — 테이블 모양

`OrderListView.java` 가 `order_list_view` 테이블 한 줄을 나타냅니다.

```java
@Entity
public class OrderListView {
    @Id private Long orderId;        // 주문 번호 (한 줄 식별자)

    private String orderNumber;       // 주문 정보
    private String orderStatus;
    private BigDecimal orderAmount;
    private Instant orderedAt;
    private Instant orderSectionUpdatedAt;  // ← 주문 정보가 마지막에 업데이트된 시각

    private Long memberId;            // 회원 정보
    private String memberName;
    private String memberEmail;
    private String memberGrade;
    private Instant memberSectionUpdatedAt;

    private Long productId;           // 상품 정보
    private String productName;
    private ...
    private Instant productSectionUpdatedAt;

    private String deliveryStatus;    // 배송 정보
    private ...
    private Instant deliverySectionUpdatedAt;
}
```

**핵심:** 각 도메인 영역(주문/회원/상품/배송) 마지막에 `XxxSectionUpdatedAt` 이 하나씩 있습니다.
"이 영역은 몇 시 몇 분 데이터인지" 적어두는 도장입니다. 왜 필요한지는 잠시 후에 나옵니다.

### 2) Event — 우체통에 들어오는 편지

`MemberChangedEvent.java`:

```java
public record MemberChangedEvent(
    String eventId,        // 편지 고유 번호
    Long memberId,         // 어느 회원?
    String memberName,
    String memberEmail,
    String memberGrade,
    Instant occurredAt     // 언제 발생?
) {}
```

회원 서비스가 보낼 때 이 모양의 JSON 으로 보냅니다.

### 3) Handler — 우체통 지키는 사람

`OrderListSqsHandlers.java`:

```java
@SqsListener("${query-service.queues.member}")  // member-changed 우체통 감시
public void onMemberChanged(MemberChangedEvent event) {
    log.info("회원 변경 메시지 도착 memberId={}", event.memberId());
    writeService.applyMember(event);             // 처리는 다음 사람에게
}
```

`@SqsListener` 가 우체통을 계속 지켜보다가 편지가 오면 이 메서드를 실행합니다.
얇게 만들어요. 실제 일은 `writeService` 에게 넘깁니다.

### 4) WriteService — 테이블에 적는 사람

`OrderListWriteService.applyMember()`:

```java
@Transactional
public void applyMember(MemberChangedEvent event) {
    // (A) 중복 편지인지 확인
    if (!idempotency.tryClaim(event.eventId(), CONSUMER)) {
        log.debug("이미 처리한 편지 무시");
        return;
    }
    // (B) 회원 ID 가 같고, 우리 도장보다 새로운 편지일 때만 업데이트
    int updated = repository.updateMemberSection(
        event.memberId(),
        event.memberName(),
        event.memberEmail(),
        event.memberGrade(),
        event.occurredAt()
    );
}
```

(A) 와 (B) 가 핵심 안전장치입니다.

- **(A) 중복 막기 (멱등성):** 같은 편지가 두 번 와도 한 번만 처리. 자세한 건 [04장](04-safety.md)에서.
- **(B) 순서 막기:** 편지가 늦게 도착했더라도, 우리가 이미 더 새 데이터를 가지고 있으면 안 덮어씀.

### 5) Repository 의 partial update — "내 영역만" 업데이트

```java
@Modifying
@Query("""
    update OrderListView v
       set v.memberName = :name,
           v.memberEmail = :email,
           v.memberGrade = :grade,
           v.memberSectionUpdatedAt = :updatedAt
     where v.memberId = :memberId
       and (v.memberSectionUpdatedAt is null
            or v.memberSectionUpdatedAt < :updatedAt)
""")
int updateMemberSection(...);
```

이 SQL 한 줄에 두 가지 비밀이 있습니다.

1. **`set` 절에 회원 컬럼만 적음.** 상품/배송 정보는 안 건드림. 다른 도메인 데이터를 실수로 덮어쓰지 않음.
2. **`where ... or < :updatedAt`.** 새 도장 시각이 더 늦을 때만 업데이트. 이게 "순서 보장 없이도 정확함" 의 비결.

## 시간 순서로 한 번 따라가기

홍길동(memberId=42) 등급이 골드로 바뀐 상황을 끝까지 따라가 봅시다.

```
시각 10:00:00.000
회원 서비스 ──→ member-changed 우체통
  편지: {
    eventId: "abc123",
    memberId: 42,
    memberName: "홍길동",
    memberGrade: "GOLD",
    occurredAt: "10:00:00.000"
  }

시각 10:00:00.500
쿼리 서비스의 @SqsListener 가 편지 발견
  → onMemberChanged(event) 호출
  → writeService.applyMember(event) 호출

시각 10:00:00.501
(A) idempotency.tryClaim("abc123", "orderList") → true
   → "이 편지 처음 봤어, 진행"

시각 10:00:00.510
(B) repository.updateMemberSection(...)
   → SQL: UPDATE order_list_view
          SET member_name='홍길동', member_grade='GOLD',
              member_section_updated_at='10:00:00.000'
          WHERE member_id=42
            AND (member_section_updated_at IS NULL
                 OR member_section_updated_at < '10:00:00.000')
   → 홍길동의 모든 주문 줄(예: 3건)이 한 번에 GOLD 로 갱신됨

시각 10:00:00.520
SQS 에게 "처리 끝났어" 보고 (ack)
   → 우체통에서 이 편지 제거됨
```

## 조회 흐름

이제 누군가 화면에서 주문 목록을 봅니다.

```
브라우저 ─GET /api/order-list?page=0&size=50─→ Spring
                                                  ↓
                                  OrderListController
                                                  ↓
                                  OrderListQueryService.findAll(...)
                                                  ↓
                                  repository.findAll(pageable)
                                                  ↓
                                  SELECT * FROM order_list_view LIMIT 50
                                                  ↓
                                  결과 → OrderListResponse 로 변환 → JSON 응답
```

**다른 서비스 호출 0회.** 그래서 빠릅니다.

## 한 줄 요약

화면 1개 = 폴더 1개. 폴더 안에는 항상 entity / repository / event / handler / service / api 가 있고,
각자 정해진 역할만 합니다.

다음: [04. 안전장치 — DLQ와 멱등성](04-safety.md)
