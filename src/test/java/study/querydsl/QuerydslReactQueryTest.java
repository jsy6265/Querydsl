package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
public class QuerydslReactQueryTest {


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
    //동적 쿼리

    //BooleanBuilder 사용
    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);

    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        //username, age가 null이 아니면 where문에 조건 추가
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return query
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    //where 다중 파라미터 사용
    @Test
    public void dynamicQuery_whereParam() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);

    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return query
                .selectFrom(member)
                .where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression ageEq(Integer ageCond) {
        if (ageCond == null) {
            return null;    //where에 null이 들어가면 무시된다
        }

        return member.age.eq(ageCond);
    }

    private BooleanExpression usernameEq(String usernameCond) {
        //3항 연산자 사용
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        //BooleanExpression을 사용하면 and문으로 조립이 가능하다
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    //    ----------------------------------------------------------------------------------------------------------------------------------------------
    //수정, 삭제 벌크연산
    @Test
    public void bulkUpdate() {
        long count = query
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        //벌크 연산 문제점 = 쿼리 실행 후 DB에는 값이 변경되었으나 영속성 컨텍스트에는 값이 변경되지 않아 값을 조회하면 변경전의 값이 나온다(영속성 컨텍스트가 우선권을 가진다)
        //fluch, clear하면 영속성 컨테이너에 남아있는 내용이 DB랑 합쳐짐
        em.flush();
        em.clear();

        List<Member> result = query
                .selectFrom(member)
                .fetch();

        for (Member member1 : result) {
            System.out.println("member = " + member1);
        }
    }

    //모든 회원의 나이를 +1
    @Test
    public void bulkAdd() {
        long count = query
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }

    //벌크 삭제 연산
    @Test
    public void bulkDelete() {
        long count = query
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

//    ----------------------------------------------------------------------------------------------------------------------------------------------
    //SQL function 호출하기
    //SQL function은 Dialect에 등록된 내용만 호출할 수 있다.

    @Test
    public void sqlFunction() {
        //Member 테이블의 모든 회원의 이름에 들어가는 'member'를 replace를 사용하여 'M'으로 바꾼다
        List<String> result = query
                .select(Expressions.stringTemplate(
                        "function('replace', {0}, {1}, {2})",
                        member.username, "member", "M"
                ))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void sqlFunction2() {
        //Member 테이블의 모든 회원의 이름에 들어가는 'member'를 replace를 사용하여 'M'으로 바꾼다
        List<String> result = query
                .select(member.username)
                .from(member)
                .where(member.username.eq(member.username.lower()))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
}
