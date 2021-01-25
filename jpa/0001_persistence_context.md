> 해당 내용은 [자바 ORM 표준 JPA 프로그래밍](https://book.naver.com/bookdb/book_detail.nhn?bid=9252528)의 내용을 정리  

# Persistence Context
영속성 컨텍스트(Persistence Context)는 `엔티티를 영구 저장하는 환경`이라는 뜻  

## 엔티티 생명주기  
Entity에는 아래와 같은 4가지의 상태가 존재한다.  

- **비영속(new/transient)**: 영속성 컨텍스트와 전혀 관계가 없는 상태
- **영속(managed)**: 영속성 컨텍스트에 저장된 상태
- **준영속(detached)**: 영속성 컨텍스트에 저장되었다가 분리된 상태  
- **삭제(removed)**: 삭제된 상태  

![Entity 생명주기](https://user-images.githubusercontent.com/25560203/105708069-3c190c00-5f57-11eb-984e-983ece021813.png)  

**비영속**  

```java
Member member = new Member();
member.setId("member1");
member.setUsername("회원1");
```  

![비영속 엔티티](https://user-images.githubusercontent.com/25560203/105708749-353ec900-5f58-11eb-9c8d-a11b6f9e2391.png)


<br /><br />

**영속**  


```java
em.persist(member);
em.find(Member.class, "member1");
em.createQuery("select m from Member m", ...);
```

![영속 엔티티](https://user-images.githubusercontent.com/25560203/105709530-302e4980-5f59-11eb-9b49-f0f173ee9d1a.png)

<br /><br />  

**준영속**  


```java
em.detach(member);
em.clear();
em.close();
```  

<br /><br />  

**삭제**  

```java
em.remove(member);
```  

## 영속성 컨텍스트 특징  

**엔티티 조회**  

```java
// 엔티티 생성(비영속)
Member member = new Member("member1", "회원1");  
// 엔티티 영속
em.persist(member);
// 엔티티 조회(1)
em.find(Member.class, "member1");
// 엔티티 조회(2)
em.find(Member.class, "member2");
```  

![엔티티 조회](https://user-images.githubusercontent.com/25560203/105711507-fe6ab200-5f5b-11eb-85af-b43f1da0c754.png)

=> 1차 캐시를 통해 성능상 이점을 제공하고, 엔티티의 동일성을 보장한다.  

<br /><br />  

**엔티티 등록**  

```java
EntityManager em = emf.createEntityManager();
EntityTransaction transaction = em.getTransaction();
// 엔티티 매니저는 데이터 변경 시 트랜잭션을 시작해야함  
transaction.begin(); // 트랜잭션 시작  

em.persist(memberA);
em.persist(memberB);
// 여기까지 INSERT SQL을 데이터베이스에 보내지 않음

// 커밋하는 순간 데이터베이스에 INSERT SQL을 보냄
transaction.commit(); // 트랜잭션 커밋
```  

![쓰기 지연, 커밋](https://user-images.githubusercontent.com/25560203/105713644-bc8f3b00-5f5e-11eb-860c-8c746aa46bc2.png)  

<br /><br />  


**엔티티 수정**  

```java
EntityManager em = emf.createEntityManager();
EntityTransaction transaction = em.getTransaction();
transaction.begin(); // 트랜잭션 시작

// 영속 엔티티 조회
Member memberA = em.find(Member.class, "memberA");

// 영속성 엔티티 데이터 수정
memberA.setUsername("updatedUser");
memberA.setAge(10);

transaction.commit(); // 트랜잭션 커밋
```

![변경 감지](https://user-images.githubusercontent.com/25560203/105714305-843c2c80-5f5f-11eb-84fa-d21a0c2fdca5.png)  

1. 트랜잭션 커밋 시 flush()가 먼저 호출
2. 엔티티와 스냅샷을 비교해서 변경된 엔티티를 찾음
3. 변경된 엔티티가 있으면 수정 쿼리를 생성해서 쓰기 지연 SQL 저장소에 보냄
4. 쓰기 지연 저장소의 SQL을 데이터베이스로 보냄
5. 데이터베이스 트랜잭션을 커밋

> JPA 기본전략에 의해 모든 필드를 갱신(아래 쿼리)  
(org.hibernate.annotations.DynamicUpdate를 사용하여 동적인 Update SQL 생성 가능)  

```sql
UPDATE MEMBER
SET
    NAME = ?,
    AGE = ?,
    GRADE = ?,
    ...
WHERE id = ?
```   

<br /><br />

**엔티티 삭제**  

```java
Member memberA = em.find(Member.class, "memberA");
em.remove(memberA);
```  

-> 수정과 마찬가지로 쓰기 지연 SQL 저장소에 등록되고 해당 엔티티는 영속성 컨택스트에서 제거되므로 재사용X  

---  

## Flush  

**플러시(flush())**는 Persistence Context의 변경 내용을 데이터베이스에 반영(플러시는 단순히 데이터베이스에 동기화하는 것)  

1. 변경 감지가 동작해서 영속성 컨텍스트에 있는 모든 엔티티를 스냅샷과 비교해서 수정된 엔티티를 찾는다.  
수정된 엔티티는 수정 쿼리를 만들어 쓰기 지연 SQL 저장소에 등록  
2. 쓰기 지연 SQL 저장소의 쿼리를 데이터베이스에 전송  

### Flush 하는 방법  

1. em.flush() 호출  
2. 트랜잭션 커밋 시 플러시가 자동 호출된다.  
3. JPQL 쿼리 실행 시 플러시 자동 호출  


## 준영속  
준영속 상태의 엔티티는 Persistence Context가 제공하는 기능을 사용할 수 없다.  

### 준영속 상태 만드는 방법  

**1. em.detach(entity)**
특정 엔티티만 준영속 상태로 전환한다.  

```java
Member member = new Member("memberA", "회원A");

// 회원 엔티티 영속 상태
em.persist(member);

// 회원 엔티티를 영속성 컨텍스트에서 분리, 준영속 상태
em.detach(member);

transaction.commit(); //트랜잭션 커밋
```

![detach](https://user-images.githubusercontent.com/25560203/105716339-06c5eb80-5f62-11eb-9f47-0b34eba1dba3.png)  

<br /><br />  

**2. em.clear()**  
영속성 컨텍스트를 완전히 초기화한다. 준영속 상태이므로 상태를 변경해도 변경감지가 이루어지지 않는다.  

```java
Member member = em.find(Member.class, "memberA");

em.clear(); // 영속성 컨텍스트 초기화

// 준영속 상태, 변경 감지X
member.setUsername("updatedName");
```

![clear](https://user-images.githubusercontent.com/25560203/105716854-bef39400-5f62-11eb-9206-3bc7dd521a15.png)  

<br /><br />  

**3. em.close()**  
영속성 컨텍스트를 종료한다.  

```java
EntityManagerFactory emf = Persistence.createEntityManager("jpa");

EntityManager em = emf.createEntityManager();
EntityTransaction transaction = em.getTransaction();

transaction.begin(); // 트랜잭션 시작  

Member memberA = em.find(Member.class, "memberA");
Member memberB = em.find(Member.class, "memberB");

transaction.commit(); //트랜잭션 커밋

em.close(); // 영속성 컨텍스트 종료
```  

![close](https://user-images.githubusercontent.com/25560203/105717724-d2532f00-5f63-11eb-9454-c8decd9e7010.png)  


### 병합  
준영속 상태 -> 영속 상태로 변경하려면 병합(merge)를 사용하면 된다.  

```java

static EntityManagerFactory emf = Persistence.createEntityManager("jpa");

public static void main(String[] args) {
    Member member = createMember("memberA", "회원1");
    
    member.setUsername("updatedName");
    
    mergeMember(member);
}

static Member createMember(String id, String username) {
    // 영속성 컨텍스트1 시작
    EntityManager em1 = emf.createEntityManager();
    EntityTransaction tx1 = em1.getTransaction();
    tx1.begin();
    
    Member member = new Member(id, username);
    
    em1.persist(member);
    tx1.commit;
    em1.close(); // 영속성 컨텍스트1 종료, member는 준영속 상태가 된다.
    
    return member;
}

static Member mergeMember(Member member) {
    // 영속성 컨텍스트2 시작
    EntityManager em2 = emf.createEntityManager();
    EntityTransaction tx2 = em2.getTransaction();
    
    tx2.begin();
    // member는 준영속 상태, mergeMember 객체는 영속 상태
    // i.e member != mergeMember
    Member mergeMember = em2.merge(member);
    tx2.commit();
    
    em2.close();
}
```  

![merge](https://user-images.githubusercontent.com/25560203/105719237-65d92f80-5f65-11eb-9d48-458e9d998867.png)