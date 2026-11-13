# simple-db

JDBC 를 직접 감싸서 MySQL 쿼리를 간단하게 실행하는 유틸리티 `SimpleDb` / `Sql` 구현입니다.
(Codemit · 데브코스 백엔드 14기 16회차 과제: MySQL/JDBC 유틸리티 SimpleDb 구현)

제공된 `SimpleDbTest` 의 **t001 ~ t019 를 테스트·의존성 수정 없이 모두 통과**합니다.

## 기술 스택

- Java 21+ · Gradle (Kotlin DSL)
- MySQL 8 · `mysql-connector-j`
- Lombok, Jackson (`jackson-databind`, `jackson-datatype-jsr310`)
- JUnit 5, AssertJ

## 실행 방법

### 1. 테스트용 MySQL 준비

```bash
docker run --name mysql-1 -p 127.0.0.1:3306:3306 -e TZ=Asia/Seoul -e MYSQL_ROOT_PASSWORD=lldj123414 -d mysql:8.4
docker exec mysql-1 mysqladmin ping -uroot -plldj123414
docker exec mysql-1 mysql -uroot -plldj123414 -e "CREATE DATABASE IF NOT EXISTS simpleDb__test CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
```

### 2. 테스트 실행

```bash
./gradlew test        # Windows: .\gradlew.bat test
```

> Lombok 1.18.42 는 JDK 21 ~ 25 에서 동작합니다. 더 최신 JDK 에서는 `JAVA_HOME` 을 21 ~ 25 로 지정하세요.

## 사용 예

```java
SimpleDb simpleDb = new SimpleDb("localhost", "root", "lldj123414", "simpleDb__test");
simpleDb.setDevMode(true); // 실행되는 SQL 과 파라미터를 출력

// INSERT → 생성된 주키
long newId = simpleDb.genSql()
        .append("INSERT INTO article")
        .append("SET createdDate = NOW(), modifiedDate = NOW()")
        .append(", title = ?", "제목 new")
        .append(", body = ?", "내용 new")
        .insert();

// appendIn → IN (?) 의 ? 를 값 개수만큼 펼침
List<Long> ids = simpleDb.genSql()
        .append("SELECT id FROM article")
        .appendIn("WHERE id IN (?)", 2L, 1L, 3L)
        .appendIn("ORDER BY FIELD (id, ?)", 2L, 1L, 3L)
        .selectLongs(); // [2, 1, 3]

// 결과를 클래스로 매핑
Article article = simpleDb.genSql()
        .append("SELECT * FROM article WHERE id = ?", 1)
        .selectRow(Article.class);

// 트랜잭션
simpleDb.startTransaction();
simpleDb.genSql().append("DELETE FROM article WHERE id = ?", 1).delete();
simpleDb.rollback(); // 또는 commit()
```

## API

### `SimpleDb`

| 메서드 | 설명 |
| --- | --- |
| `new SimpleDb(host, username, password, dbName)` | 접속 정보로 생성 (포트 3306) |
| `setDevMode(boolean)` | `true` 면 실행 SQL·파라미터를 콘솔에 출력 |
| `run(sql, params...)` | DDL/DML 실행, 영향받은 행 수 반환 |
| `genSql()` | 새 `Sql` 생성 |
| `startTransaction()` / `commit()` / `rollback()` | 현재 스레드의 트랜잭션 제어 |
| `close()` | 현재 스레드의 커넥션 종료 |

### `Sql`

| 메서드 | 반환 |
| --- | --- |
| `append(sqlPart, params...)` | `Sql` (체이닝, `?` 바인딩) |
| `appendIn(sqlPart, params...)` | `Sql` (`?` 를 값 개수만큼 `?, ?, ?` 로 펼침) |
| `insert()` | `long` 생성된 주키 |
| `update()` / `delete()` | `int` 영향받은 행 수 |
| `selectRows()` / `selectRow()` | `List<Map<String, Object>>` / `Map<String, Object>` |
| `selectRows(Class)` / `selectRow(Class)` | `List<T>` / `T` |
| `selectDatetime()` | `LocalDateTime` |
| `selectLong()` / `selectLongs()` | `Long` / `List<Long>` |
| `selectString()` | `String` |
| `selectBoolean()` | `Boolean` (`BIT(1)`, `1 = 1` 같은 숫자 결과 모두 지원) |

결과가 없으면 `selectRow*` / 단일 값 조회는 `null` 을 반환합니다.

## 설계

- **스레드별 커넥션**: `ThreadLocal<Connection>` 으로 스레드마다 별도 커넥션을 쓰므로 여러 스레드가 동시에 써도 안전합니다 (t017). `close()` 는 호출한 스레드의 커넥션만 닫고, 다시 사용하면 새 커넥션을 엽니다.
- **트랜잭션**: `startTransaction()` 은 현재 스레드 커넥션의 auto-commit 을 끄고, `commit()` / `rollback()` 후 다시 켭니다. 중첩·스레드 간 트랜잭션은 지원하지 않습니다.
- **`Sql` 은 일회용**: `append` 로 쿼리를 쌓고 한 번 실행합니다. 매번 `genSql()` 로 새로 만드세요.
- **객체 매핑**: 행을 `Map` 으로 읽은 뒤 Jackson(`JavaTimeModule`) 으로 대상 클래스로 변환합니다. `isBlind` 컬럼은 `Article` 에서 `@JsonProperty("isBlind")` 로 매핑합니다.

용어 정의는 [GLOSSARY.md](GLOSSARY.md) 를 참고하세요.

## 구조

```
src/main/java/com/ll/
├── Article.java
└── simpleDb/
    ├── SimpleDb.java
    └── Sql.java
src/test/java/com/ll/simpleDb/
└── SimpleDbTest.java   # 제공 테스트 (수정하지 않음)
```
