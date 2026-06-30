# 용어 사전

이 문서에 나오는 단어들을 한 줄씩 설명합니다.

| 용어 | 한 줄 설명 |
|---|---|
| **MSA** | Microservice Architecture. 하나의 큰 프로그램 대신 부서별 작은 프로그램들로 쪼개서 만드는 방식. |
| **도메인 서비스** | 회원/상품/주문 등 한 가지 일만 담당하는 작은 서비스. |
| **쿼리 서비스** | 여러 도메인 데이터를 화면용으로 미리 모아두는 조회 전용 서비스. (= 이 프로젝트) |
| **CQRS** | Command-Query Responsibility Segregation. 쓰기와 읽기를 별도 모델로 분리하는 패턴. 쿼리 서비스가 읽기 모델 역할. |
| **Read Model / View** | 조회용으로 미리 만들어둔 테이블. `OrderListView` 같은 게 그 예. |
| **비정규화 (Denormalization)** | 정규화의 반대. 여러 테이블에 흩어진 데이터를 한 테이블에 베껴 모아두기. 조회 속도를 위해. |
| **SQS** | AWS Simple Queue Service. 메시지를 안전하게 전달해주는 우체통. |
| **메시지 큐 (Queue)** | 먼저 들어간 메시지가 먼저 나오는 줄(line). 우체통의 다른 이름. |
| **이벤트 (Event)** | "무언가 일어났다" 라는 알림 메시지. 예: "회원 등급이 바뀌었음". |
| **이벤트 기반 (Event-driven)** | 서비스들이 직접 호출 대신 이벤트로 소통하는 방식. |
| **Producer / Consumer** | 메시지를 보내는 쪽(producer) / 받는 쪽(consumer). |
| **DLQ (Dead Letter Queue)** | 처리 실패가 반복된 메시지를 격리시키는 별도 큐. |
| **Poison pill / Poison message** | DLQ 로 보내야 할 만한, 처리하면 계속 실패하는 메시지. |
| **멱등성 (Idempotency)** | 같은 동작을 여러 번 해도 한 번 한 결과와 똑같음. 중복 메시지 안전장치. |
| **at-least-once** | "최소 한 번은 전달". 두 번 이상 올 수도 있다는 뜻. SQS 의 기본 전달 보장. |
| **at-most-once / exactly-once** | "최대 한 번" / "정확히 한 번". 다른 전달 보장 종류. |
| **last-write-wins** | 여러 업데이트가 충돌할 때 가장 최신 시간 도장 찍힌 게 이김. |
| **Entity** | JPA 에서 "DB 테이블 한 줄을 자바 객체로 표현한 것". |
| **Repository** | DB 와 통신하는 자바 인터페이스. JPA 가 알아서 SQL 만들어줌. |
| **JPA** | Java Persistence API. 자바와 DB 사이를 자동으로 이어주는 표준. |
| **Hibernate** | JPA 표준의 가장 흔한 구현체. 우리도 씀. |
| **DTO** | Data Transfer Object. 화면/API 가 주고받기 위한 데이터 묶음. `OrderListResponse` 같은 것. |
| **Record (자바)** | 자바 14+ 의 불변 데이터 클래스. DTO 만들 때 짧게 쓸 수 있음. |
| **Lombok** | `@Getter`, `@RequiredArgsConstructor` 등으로 반복 코드를 자동 생성해주는 도구. |
| **Spring Boot** | 자바 웹/서비스 만들 때 가장 흔히 쓰는 프레임워크. |
| **Spring Cloud AWS** | Spring Boot 에서 AWS 서비스(SQS 등)를 쉽게 쓰게 해주는 라이브러리. |
| **`@SqsListener`** | SQS 우체통을 자동 감시하다가 편지 오면 메서드를 실행시키는 어노테이션. |
| **`@Transactional`** | "이 메서드 안의 DB 작업들을 한 묶음으로 처리" 표시. 하나라도 실패하면 전부 되돌림. |
| **`@Scheduled`** | "이 메서드를 정해진 시간에 자동 실행" 표시. 새벽 청소 작업에 사용. |
| **가상 스레드 (Virtual Thread)** | 자바 21 의 새 기능. 일꾼(스레드)을 거의 무한정 만들 수 있어 동시 처리에 유리. |
| **upsert** | UPDATE + INSERT. 있으면 업데이트, 없으면 새로 만들기. |
| **partial update** | 한 행의 일부 컬럼만 골라서 업데이트. |
| **PK (Primary Key)** | 한 줄을 구분하는 고유 키. 주문 목록에서는 `orderId`. |
| **FK (Foreign Key)** | 다른 테이블의 PK 를 참조하는 키. 주문 줄 안의 `memberId` 같은 것. |
| **Migration** | DB 스키마(테이블 구조) 변경을 코드로 관리하는 것. `V1__...sql` 같은 파일. |
| **Flyway / Liquibase** | 흔한 마이그레이션 도구. |
| **Terraform** | AWS 같은 인프라를 코드로 정의하는 도구. `infra/sqs.tf` 가 예. |
| **YAML** | 사람이 읽기 좋은 설정 파일 형식. `application.yml`, `screens-specs/*.yaml` 가 예. |
| **Gradle** | 자바 빌드 도구. `./gradlew build` 같은 명령 실행기. |
| **MySQL** | 가장 흔한 관계형 DB. 우리 비정규화 테이블이 여기 들어감. |
| **HikariCP** | 자바에서 DB 연결을 효율적으로 관리하는 풀(pool) 라이브러리. |

다시 [README](README.md) 로.
