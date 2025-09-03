# 🍃 SpringNoSQL — Spring Boot 3.2.3 + MongoDB & Redis 실습

<p align="left">
  <img alt="java" src="https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white">
  <img alt="spring-boot" src="https://img.shields.io/badge/Spring%20Boot-3.2.3-6DB33F?logo=springboot&logoColor=white">
  <img alt="gradle" src="https://img.shields.io/badge/Gradle-8.5-02303A?logo=gradle&logoColor=white">
  <img alt="mongodb" src="https://img.shields.io/badge/MongoDB-7.x-47A248?logo=mongodb&logoColor=white">
  <img alt="redis" src="https://img.shields.io/badge/Redis-7.x-DC382D?logo=redis&logoColor=white">
  <img alt="license" src="https://img.shields.io/badge/License-Apache--2.0-blue">
</p>

**교육·실습용 NoSQL 프로젝트**로, Spring Boot 3.x(Java 17)에서 **MongoDB + Redis**를 함께 다루며
- 멜론 Top100 크롤링 → **MongoDB** 저장/조회/수정/삭제/필드추가/집계
- **Redis** 자료구조(String/JSON/List/Hash/Set/ZSet) 입·출력 및 TTL 실습
- CGV 영화 순위 크롤링 → **Redis 캐시(1시간)** 반환
- **RedisCacheManager** 기반 일일 멜론 차트 **캐싱(@Cacheable/@CacheEvict)**

을 한 프로젝트에 담았습니다. 공통 응답 스펙(`CommonResponse<T>`)과 Bean Validation도 포함되어 있습니다.

> ⚠️ 실습 코드는 교육용으로 보안/예외처리가 최소화되어 있습니다. 운영용 전환 시 **비밀키/접속정보 외부화, 입력값 검증, 예외 처리, 권한, 로그 보안**을 보강하세요.

---

## ✨ 주요 기능 요약

### MongoDB (멜론 차트)
- 멜론 Top100 수집(`Jsoup`) → `MELON_yyyyMMdd` 컬렉션에 저장
- 가수별 곡 수 집계, 가수명으로 곡 목록 조회
- 필드 업데이트/추가(`nickname`, `member` 등), 다건 insert, 문서 삭제
- **일일 캐시 키**: `melonKeyGen` → `"MELON_yyyyMMdd"` (KST), 캐시명: `melonSongs` (기본 TTL 3시간)

### Redis 실습
- Key:Value(`String`/`JSON`) 저장/조회 (**TTL 2일**)
- `List` / `List(JSON)` 저장/조회 (**TTL 5시간**)
- `Hash` 저장/조회 (**TTL 100분**)
- `Set(JSON)` / `ZSet(JSON)` 저장/조회 (**TTL 5시간**)

### 영화 순위
- `http://www.cgv.co.kr/movies/` 크롤링 → Redis List 캐시(**키: `CGV_yyyyMMdd`, TTL: 1시간**)
- **음성 명령** 키워드(`영화, 영하, 연하, 연화`)가 포함될 때만 데이터 반환 (크롬 음성인식 예제 페이지 제공)

---

## 🧱 기술 스택
- **Spring Boot**: Web, Validation
- **MongoDB**: Spring Data MongoDB, `MongoTemplate`
- **Redis**: Spring Data Redis(Lettuce), `RedisTemplate`, `RedisCacheManager`
- **크롤링**: Jsoup
- **Lombok**, **Jakarta Validation**, **Gradle 8.5**

---

## 📁 프로젝트 구조
```
SpringNoSQL/
├─ src/main/java/kopo/poly
│  ├─ SpringNoSqlApplication.java
│  ├─ config/
│  │  ├─ CacheConfig.java          # RedisCacheManager(값 JSON 직렬화, 기본 TTL 3시간), melonKeyGen
│  │  └─ RedisConfiguration.java   # Lettuce 연결팩토리, RedisTemplate
│  ├─ controller/
│  │  ├─ MelonController.java      # /melon/v1/*
│  │  ├─ RedisController.java      # /redis/v1/*
│  │  └─ MovieController.java      # /speechcommand (CGV 순위 반환)
│  ├─ controller/response/CommonResponse.java
│  ├─ dto/ (MelonDTO, RedisDTO, MovieDTO, MongoDTO, MsgDTO)
│  ├─ service/impl/ (MelonService, MyRedisService, MovieService, MongoService)
│  └─ persistance/
│     ├─ mongodb/ (IMelonMapper, IMongoMapper, impl/*, AbstractMongoDBComon)
│     └─ redis/   (IMyRedisMapper, IMovieMapper, impl/*)
├─ src/main/resources
│  ├─ application.properties
│  └─ static/html/
│     ├─ mongoTest.html
│     ├─ melon/*.html              # 수집/조회/수정/필드추가 데모
│     ├─ redis/*.html              # String/List/Hash/Set/ZSet 데모
│     └─ tts/*.html                # 음성인식 → /speechcommand 데모
├─ k8s/ (deployment.yaml, service.yaml)
├─ helm/ (mongodb-values.yaml, redisdb-values.yaml)
├─ Dockerfile
└─ LICENSE (Apache-2.0)
```

---

## ⚙️ 빠른 시작

### 1) 필수 요건
- **JDK 17**, **Gradle 8.5+**
- **MongoDB 7.x**, **Redis 7.x** (로컬/원격 중 택1)

### 2) 환경설정 (`src/main/resources/application.properties`)
```properties
server.port=11000

# MongoDB
spring.data.mongodb.host=<MONGO_HOST>
spring.data.mongodb.port=27017
spring.data.mongodb.database=myDB
spring.data.mongodb.username=poly
spring.data.mongodb.password=1234

# Redis
spring.data.redis.host=<REDIS_HOST>
spring.data.redis.port=6379
spring.data.redis.username=poly
spring.data.redis.password=1234

# (선택) Redis Cache prefix/TTL
spring.cache.type=redis
spring.cache.redis.time-to-live=10m              # CacheConfig에서 기본 3시간으로 재정의됨
spring.cache.redis.use-key-prefix=true
spring.cache.redis.key-prefix=melon:
spring.cache.redis.cache-null-values=false
```

> ✋ **보안 주의**: 계정/비밀번호는 실습용입니다. 운영환경에서는 환경변수·Vault 등을 사용하세요.

### 3) 빌드 & 실행
```bash
# 의존성 설치 및 실행
./gradlew bootRun

# 또는 JAR 패키징 후 실행
./gradlew clean build
java -jar build/libs/*.jar
```

### 4) (옵션) 도커 이미지
```bash
./gradlew clean build
docker build -t hyeopgeonlee/nosql-jdk17:0.0.1 .
docker run --rm -p 11000:11000 --name nosql-app \
  -e SPRING_DATA_MONGODB_HOST=<MONGO_HOST> \
  -e SPRING_DATA_MONGODB_USERNAME=poly -e SPRING_DATA_MONGODB_PASSWORD=poly1234 \
  -e SPRING_DATA_REDIS_HOST=<REDIS_HOST> \
  -e SPRING_DATA_REDIS_USERNAME=poly -e SPRING_DATA_REDIS_PASSWORD=poly1234 \
  hyeopgeonlee/nosql-jdk17:0.0.1
```

---

## 🔌 REST API 요약

### 공통 응답 포맷
```json
{
  "httpStatus": "OK",
  "message": "SUCCESS",
  "data": { ... }   // 혹은 배열
}
```

### 1) 멜론(MongoDB) — `/melon/v1/*`
| 기능 | 메서드 | 경로 | 요청 예시(JSON) |
|---|---|---|---|
| 멜론 Top100 수집 → Mongo 저장 | POST | `/melon/v1/collectMelonSong` | `{}` |
| 오늘 수집된 곡 목록 | POST | `/melon/v1/getSongList` | `{}` |
| 가수별 곡 수 집계 | POST | `/melon/v1/getSingerSongCnt` | `{}` |
| 가수명으로 곡 검색 | POST | `/melon/v1/getSingerSong` | `{"singer":"아이유"}` |
| 컬렉션 드랍 | POST | `/melon/v1/dropCollection` | `{}` |
| insertMany 예제 | POST | `/melon/v1/insertManyField` | `{}` |
| 특정 필드 수정 | POST | `/melon/v1/updateField` | `{"singer":"방탄소년단","updateSinger":"BTS"}` |
| 필드 추가(단건) | POST | `/melon/v1/updateAddField` | `{"singer":"아이유","nickname":"IU"}` |
| 필드 추가(List) | POST | `/melon/v1/updateAddListField` | `{"singer":"방탄소년단","member":["RM","진","슈가"]}` |
| 수정 + 필드추가 | POST | `/melon/v1/updateFieldAndAddField` | `{"singer":"방탄소년단","updateSinger":"BTS","addFieldValue":"K-POP"}` |
| 문서 삭제 | POST | `/melon/v1/deleteDocument` | `{"singer":"BTS"}` |

> 🧊 **캐시**: `getSongList`는 `@Cacheable(cacheNames="melonSongs", keyGenerator="melonKeyGen", sync=true)`로 일일 캐싱됩니다. `collectMelonSong` 등 쓰기 작업 시 `@CacheEvict`로 무효화됩니다. 기본 TTL **3시간** (CacheConfig).

### 2) Redis — `/redis/v1/*`
`RedisDTO` 필드: `{ "name", "email", "addr", "text", "order" }`

| 기능 | 메서드 | 경로 | 요청 예시 | TTL |
|---|---|---|---|---|
| String 저장 | POST | `/redis/v1/saveString` | `{"text":"Hello Redis"}` | 2일 |
| String(JSON) 저장 | POST | `/redis/v1/saveStringJSON` | `{"name":"Kim","email":"k@ex.com","addr":"Seoul"}` | 2일 |
| List 저장 | POST | `/redis/v1/saveList` | `[{"text":"A"},{"text":"B"}]` | 5시간 |
| List(JSON) 저장 | POST | `/redis/v1/saveListJSON` | `[{"name":"A"},{"name":"B"}]` | 5시간 |
| Hash 저장 | POST | `/redis/v1/saveHash` | `{"name":"Lee","email":"lee@ex.com","addr":"KR"}` | 100분 |
| Set(JSON) 저장 | POST | `/redis/v1/saveSetJSON` | `[{"name":"A"},{"name":"B"}]` | 5시간 |
| ZSet(JSON) 저장 | POST | `/redis/v1/saveZSetJSON` | `[{"name":"A","order":1.0},{"name":"B","order":2.0}]` | 5시간 |

> 각 API는 내부적으로 키를 고정 사용합니다. 예) `myRedis_String`, `myRedis_List_JSON`, `myRedis_ZSet_JSON` 등.

### 3) 영화 순위(크롤링) — `/speechcommand`
- **POST** `/speechcommand`  
  요청: `{"speechCommand":"영화 순위 알려줘"}`  
  동작: 문구에 `영화/영하/연하/연화`가 포함되면 CGV 순위를 반환합니다.  
  캐시 키: `CGV_yyyyMMdd` (TTL 1시간, Redis List)

---

## 🧪 빠른 점검(Quick Demo)

- **정적 데모 페이지**
  - MongoDB: `GET /html/mongoTest.html`
  - Melon: `GET /html/melon/melonCollect.html`, `melonSearch.html`, `melonSinger.html`, `melonUpdateField.html` 등
  - Redis: `GET /html/redis/saveString.html`, `saveList.html`, `saveHash.html`, `saveZSetJSON.html` 등
  - 음성인식(TTS 데모): `GET /html/tts/tts.html` → 브라우저에서 음성 명령 → `/speechcommand` 호출

- **cURL 예시**
```bash
# 오늘 멜론 수집 → 저장
curl -X POST http://localhost:11000/melon/v1/collectMelonSong -H "Content-Type: application/json" -d '{}'

# 가수별 곡 수
curl -X POST http://localhost:11000/melon/v1/getSingerSongCnt -H "Content-Type: application/json" -d '{}'

# Redis ZSet(JSON) 저장
curl -X POST http://localhost:11000/redis/v1/saveZSetJSON -H "Content-Type: application/json" \
  -d '[{"name":"Alpha","order":1.0},{"name":"Beta","order":2.0}]'

# 영화 순위(음성 명령)
curl -X POST http://localhost:11000/speechcommand -H "Content-Type: application/json" \
  -d '{"speechCommand":"영화 순위 알려줘"}'
```

---

## 🧊 캐시 & TTL 정리

- **Spring Cache (RedisCacheManager)**  
  - 캐시명: `melonSongs` / 키: `"MELON_yyyyMMdd"` (KST 기준)  
  - 기본 TTL: **3시간** (코드 `CacheConfig`에서 설정, 값은 JSON 직렬화)

- **RedisTemplate TTL**  
  - String / String(JSON): **2일**  
  - List / List(JSON): **5시간**  
  - Hash: **100분**  
  - Set(JSON) / ZSet(JSON): **5시간**  
  - 영화 순위(CG V): **1시간** (`CGV_yyyyMMdd`)

---

## ☸️ Kubernetes & Helm

### 애플리케이션 배포 (예시)
```bash
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
# Service(NodePort): 11000 → nodePort: 31200
```

### MongoDB / Redis (Bitnami 차트 예시)
```bash
# MongoDB
helm repo add bitnami https://charts.bitnami.com/bitnami
helm install mongo bitnami/mongodb -f helm/mongodb-values.yaml

# Redis
helm install redis bitnami/redis -f helm/redisdb-values.yaml
```

> values 파일에는 실습용 암호(`poly1234`)와 저장소/pool 설정이 포함되어 있습니다. 운영에서는 적합한 자원/보안을 적용하세요.

---

## 🧑‍💻 개발 팁
- **로그 확인**: `logging.level.org.springframework.cache=TRACE`, `logging.level.org.springframework.data.redis=INFO`
- **검증**: DTO에 `jakarta.validation` 적용(`@Valid`, `@NotBlank`) → `CommonResponse.getErrors(...)`로 에러 응답
- **크롤링**: 대상 사이트 구조 변경에 민감합니다. 선택자 실패 시 예외/빈 리스트가 반환될 수 있습니다.

---

## 📜 라이선스 & 문의
- 라이선스: **Apache-2.0** (`/LICENSE`)
- 문의: 한국폴리텍대학 서울강서캠퍼스 **빅데이터소프트웨어과** · **이협건 교수** (hglee67@kopo.ac.kr)  
  입학 상담 오픈채팅: <https://open.kakao.com/o/gEd0JIad>
