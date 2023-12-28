package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory query;


    @BeforeEach //테스트 케이스 실행전 돌아가는 메서드
    public void before() {
        query = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

//    ----------------------------------------------------------------------------------------------------------------------------------------------

    //JPQL vs Querydsl
    @Test
    public void startJPQL() {
        //member1을 찾아라

        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void StartQuerydsl() {

        QMember m = new QMember("m"); //m은 별칭 별로 안중요함

        Member findMember = query.
                select(m).
                from(m).
                where(m.username.eq("member1")). //파라미터 바인딩 자동으로 해줌, 컴파일 시점에서 오타있으면 오휴 발생
                        fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    //    ----------------------------------------------------------------------------------------------------------------------------------------------
    //기본 Q-Type 활용
    @Test
    public void qTypeQuery() {
        //Q-Type 선언 방법
        //1. 기본 생성자
        QMember m = QMember.member;

        Member findMember = query.
                select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();

        //2. static import
        Member findMember2 = query.
                select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        //같은 테이블 join시 엘리어스(별칭) 따로 선언
        QMember m1 = new QMember("m1");
        QMember m2 = new QMember("m2");

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    //검색 조건 쿼리
    @Test
    public void search() {
        // 검색 조건 종류
        // eq, ne, eq().not 같은지 검사
        //isNotNull null값인지 검사
        //in, notIn, between 범위 검사
        //goe = 크거나 같다, go = 크다, loe = 작거나 같다, lt = 작다
        //like, contains = %member%, startWith = member%

        Member findMember = query
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() {
        Member findMember = query
                .selectFrom(member)
                .where(member.username.eq("member1"), member.age.eq(10)) //.and()말고 , 조건으로도 가능
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

//    ----------------------------------------------------------------------------------------------------------------------------------------------
    //결과 조회

    @Test
    public void resultFetch() {
        List<Member> fetch = query
                .selectFrom(member)
                .fetch();

        Member fetchOne = query
                .selectFrom(member)
                .fetchOne();

        Member fetchFirst = query
                .selectFrom(member)
                .fetchFirst();

        QueryResults<Member> results = query
                .selectFrom(member)
                .fetchResults();

        results.getTotal();

        List<Member> content = results.getResults();

        long total = query
                .selectFrom(member)
                .fetchCount();
    }

//    ----------------------------------------------------------------------------------------------------------------------------------------------
    //정렬

    //1. 회원 나이 내림차순(desc)
    //2. 회원 이름 오름차순(asc)
    //단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = query
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

//    ----------------------------------------------------------------------------------------------------------------------------------------------
    //페이징

    @Test
    public void paging1() {
        List<Member> result = query
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //몇번째 부터?
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    //전체 조회수가 필요한  경우
    @Test
    public void paging2() {
        QueryResults<Member> result = query
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //몇번째 부터?
                .limit(2)
                .fetchResults();

        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(result.getLimit()).isEqualTo(2);
        assertThat(result.getOffset()).isEqualTo(1);
        assertThat(result.getResults().size()).isEqualTo(2);
    }

    //    ----------------------------------------------------------------------------------------------------------------------------------------------
    //집합
    @Test
    public void aggregation() {
        //Tuple querydsl Tuple = 여려개의 타입이 있을때 꺼내 오는거?
        List<Tuple> result = query
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    //팀이름과 각팀의 평균 연령을 구해라
    @Test
    public void group() {
        List<Tuple> result = query
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    //    ----------------------------------------------------------------------------------------------------------------------------------------------
    //기본 조인
    @Test
    public void join() {
        List<Member> result = query
                .selectFrom(member)
//                .join(member.team, team)
                .leftJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result).extracting("username")
                .containsExactly("member1", "member2");
    }

    //연관관계 없는 테이블 조인
    //두 테이블을 일단 모두 합치고 값을 찾는다?
    //회원의 이름이 팀 이름과 같은 회원 조회
    @Test
    public void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = query
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result).extracting("username")
                .containsExactly("teamA", "teamB");
    }

    //join -on
    //1. 조인 대상 필터링
    //2. 연관관계 없는 엔티티 외부조인

    //회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
    //JPQL : select m , t from Member m left join m.team t on t.name = 'teamA'
    @Test
    public void join_on_filtering() {
        List<Tuple> result = query
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple t : result) {
            System.out.println("tuple = " + t);
        }
    }

    //연관관계 없는 엔티티 조인
    //회원의 이름이 팀 이름과 같은 대상 외부 조인
    @Test
    public void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamA", 60));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = query
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple t : result) {
            System.out.println("tuple = " + t);
        }
    }

    //페치 조인
    //연관된 엔티티를 한번의 쿼리로 가져온다? Lazy로 연결된 엔티티는 그냥 조회하면 안가져오지만 페치조인을 사용하면 가져온다
    //주로 성능 최적화에서 사용

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() {
        em.flush();
        em.clear();

        Member member1 = query
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(member1.getTeam());//로딩된 엔티티인지 초기화가 안된엔티티인지 검사
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    public void fetchJoinUse() {
        em.flush();
        em.clear();

        Member member1 = query
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(member1.getTeam());//로딩된 엔티티인지 초기화가 안된엔티티인지 검사
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

//    ----------------------------------------------------------------------------------------------------------------------------------------------
    //서브쿼리

    @Test
    public void subQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = query
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    //나이가 평균 이상인 회원
    @Test
    public void subQueryGoe() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = query
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    //나이가 10 이상인 회원
    @Test
    public void subQueryIn() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = query
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    @Test
    public void selectSubQuery(){
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = query
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for(Tuple t : result){
            System.out.println("tuple = " + t);
        }
    }

//    ----------------------------------------------------------------------------------------------------------------------------------------------
    //case 문
    @Test
    public void basicCase(){
        List<String> reuslt = query
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for(String s : reuslt){
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase(){
        List<String> result = query
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for(String s : result){
            System.out.println("s = " + s);
        }
    }

//    ----------------------------------------------------------------------------------------------------------------------------------------------
    //상수, 문자 더하기

    @Test
    public void constant(){
        List<Tuple> result = query
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for(Tuple t : result){
            System.out.println("tuple = " + t);
        }
    }

    @Test
    public void concat(){
        //{username}_{age}
        List<String> result = query
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .fetch();

        for(String s : result){
            System.out.println("s = " + s);
        }
    }
}
