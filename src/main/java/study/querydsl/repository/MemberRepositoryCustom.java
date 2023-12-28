package study.querydsl.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;

import java.util.List;

//사용자 정의 리포지토리(동적 쿼리 작성시 JPA는 인터페이스라 동적 쿼리 못쓰니까 만듬
//인터페이스 생성후 해당 인터페이스 상속받는 구현체 구현(이름은 JPA리포지토리 + Impl로 해야함)
//커스텀 인터페이스 상속받아 구현 후 JPA리포지토리에 커스템 인터페이스 상속
public interface MemberRepositoryCustom {
    List<MemberTeamDto> search(MemberSearchCondition condition);
    
    //카운트 쿼리 유무
    Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable);
    Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable);
}
