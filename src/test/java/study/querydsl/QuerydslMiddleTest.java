package study.querydsl;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import java.util.List;

import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
public class QuerydslMiddleTest {

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
    //프로젝션과 결과 반환-기본
    //프로젝션 = select 절에 무엇을 가져올지 지정하는 것
    //프로젝션 대상이 하나면 타입을 명확히 지정, 둘 이상이면 튜풀이나 Dto로 조회
    //튜블 : Querydsl이 여러개를 조회할 때 사용하는 타입

    //튜블 조회
    @Test
    public void simpleProjection() {
        List<String> result = query
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    //튜플은 리포지토리까지는 괜찮지만 그 이상(서비스?)으로 넘어가는 건 좋은 설계가 아니다
    //외부에서 어떤 기술스택을 쓰는지 유출 위험이있다?
    @Test
    public void tupleProjection() {
        List<Tuple> result = query
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple t : result) {
            String username = t.get(member.username);
            Integer age = t.get(member.age);

            System.out.print("username = " + username);
            System.out.println(", age = " + age);
        }
    }

//    ----------------------------------------------------------------------------------------------------------------------------------------------
    //프로젝션 결과반환 - Dto 조회

    @Test
    public void findDtoByJPQL() {

        //조회되는 Member와 MemberDto값이 다름으로 JPQL에서 지원하는 new Operation 사용
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age)  from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    //Querydsl 빈 생성
    //1. 프로퍼티 접근 - Setter(기본 생성자 필요)
    //기본 생성자로 객체 만들고 setter로 값 할당?
    @Test
    public void findDtoBySetter() {
        List<MemberDto> result = query
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    //2. 필드 직접 접근
    //바로 필드에 값을 넣는다? ex) private String username = ?
    @Test
    public void findDtoByField() {
        List<MemberDto> result = query
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    //3. 생성자 사용
    //생성자에 인자값으로 받는 값들 타입을 맞춰야 함
    @Test
    public void findDtoByConstructor() {
        List<MemberDto> result = query
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findUserDtoByfields() {
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = query
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"), //조회하는 테이블 컬럼명과 Dto에 선언된 변수명이 다르면 as를 사용해서 명시해줘야함
//                        member.age
                        //서브 쿼리 사용 시 ExpressionUtils.as(서브쿼리, 별칭)으로 명시
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    public void findUserDtoByConstructor() {
        //생성자로 생성시 필드 타입을 맞춰야한다
        List<UserDto> result = query
                .select(Projections.constructor(UserDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

//    ----------------------------------------------------------------------------------------------------------------------------------------------
    //프로젝션 결과 변환 - QueryProjection
    //빈 생성의 생성자와 다른점 = 빈 생성의 생성자는 컴파일 오류를 못잡고 런타임 오류가 발생한다(실제 유저가 실행시점에 오류 발생)
    //강제성 부여?
    @Test
    public void findDtoByQueryProjection(){
        List<MemberDto> result = query
                .select(new QMemberDto(member.username, member.age))    //타입 잘 맞춰야함 타입 다르면 컴파일 시점에 오류 발생
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto" + memberDto);
        }
    }
}
