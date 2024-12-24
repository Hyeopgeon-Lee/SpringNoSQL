# 🌱 Spring Boot Frameworks 3.x + NoSQL(MongoDB, RedisDB) 실습

> **Java 17 기반 실습 코드**  
> Spring Boot 3.x는 Java 8 지원이 불가하므로, 본 실습은 Java 17 기반으로 작성되었습니다.  
> 공유되는 프로그래밍 코드는 한국폴리텍대학 서울강서캠퍼스 빅데이터과 수업에서 사용된 코드입니다.

---

### 📚 **작성자**
- **한국폴리텍대학 서울강서캠퍼스 빅데이터과**  
- **이협건 교수**  
- ✉️ [hglee67@kopo.ac.kr](mailto:hglee67@kopo.ac.kr)  
- 🔗 [빅데이터학과 입학 상담 오픈채팅방](https://open.kakao.com/o/gEd0JIad)

---

## 🚀 주요 실습 내용

1. **웹크롤링으로 수집된 데이터를 MongoDB에 저장 및 조회, 수정, 삭제 프로그래밍**
2. **RedisDB를 활용한 캐싱 프로그래밍**
3. **음성 명령으로 CGV 영화 순위 정보를 웹크롤링한 뒤 RedisDB에 저장하는 프로그래밍**
4. **쿠버네티스 배포를 위한 Helm 설정 파일 및 Service, Deployment 배포**

---

## 🛠️ 주요 적용 프레임워크

- **Spring Boot Frameworks**
- **Spring Data JPA**
- **QueryDSL**

---

## 📩 문의 및 입학 상담

- 📧 **이메일**: [hglee67@kopo.ac.kr](mailto:hglee67@kopo.ac.kr)  
- 💬 **입학 상담 오픈채팅방**: [바로가기](https://open.kakao.com/o/gEd0JIad)

---

## 💡 **우리 학과 소개**
- 한국폴리텍대학 서울강서캠퍼스 빅데이터과는 **클라우드 컴퓨팅, 인공지능, 빅데이터 기술**을 활용하여 소프트웨어 개발자를 양성하는 학과입니다.  
- 학과에 대한 더 자세한 정보는 [학과 홈페이지](https://www.kopo.ac.kr/kangseo/content.do?menu=1547)를 참고하세요.

---

## 📦 **설치 및 실행 방법**

### 1. 레포지토리 클론
- 아래 명령어를 실행하여 레포지토리를 클론합니다.

```bash
git clone https://github.com/Hyeopgeon-Lee/SpringNoSQL.git
cd SpringNoSQL
```
### 2. MongoDB 및 RedisDB 설정
- application.yml 또는 application.properties 파일에서 MongoDB와 RedisDB 설정 정보를 업데이트합니다.

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/your_database
  redis:
    host: localhost
    port: 6379
```

### 3. 의존성 설치
- Gradle을 사용하여 의존성을 설치합니다.

```bash
./gradlew build
```

### 4. 애플리케이션 실행
- 아래 명령어를 실행하여 애플리케이션을 시작합니다.

```bash
./gradlew bootRun
```

## 📜 쿠버네티스 배포
- Helm Chart와 Service, Deployment 파일을 활용하여 애플리케이션을 쿠버네티스에 배포합니다.
- 상세 설정 내용은 프로젝트의 helm/ 디렉토리를 참고하세요.

