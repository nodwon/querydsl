package study.querydsl.repository;

import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;

import java.awt.print.Pageable;
import java.util.List;

public interface MemberRepositoryCustom {
    List<MemberTeamDto> search(MemberSearchCondition condition);
    List<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable);
    List<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable);

}
