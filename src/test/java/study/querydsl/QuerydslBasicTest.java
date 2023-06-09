package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;
    JPAQueryFactory queryFactory = new JPAQueryFactory(em);

    @BeforeEach
    public void before(){
        queryFactory = new JPAQueryFactory(em);

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

    @Test
    public void startJPQL(){
        // member1를 찾아라
        Member findMember = em.createQuery(" select  m from Member m " +
                        "where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl(){ //두개의 큰 문제
        // QMember m =QMember.member;
        // QMember m = new QMember("m");

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");

    }
    @Test
    public void search(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"),
                        member.age.eq(10)) // ,  = and
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
    @Test
    public void resultFetch(){
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        Member fetchOne = queryFactory
                .selectFrom(member)
                .fetchOne();
        queryFactory
                .selectFrom(member)
                .fetchFirst();

        queryFactory.selectFrom(member)// total count 지원안함
                .fetchResults();
        // 대신에 이렇게 작성해서 해야한다.
        /*int count = entityManager.createQuery(
            "SELECT COUNT(*) FROM Member", Integer.class
        ).getSingleResult();

        * */
        Long totalCount = queryFactory
                .select(Wildcard.count)
                .select(member.count())
                .from(member)
                .fetchOne();
       //count 쿼리로 변경 // fecthCount 지원안함
        long count = queryFactory
                .selectFrom(member)
                .fetchCount();
        /*int count = entityManager.createQuery(
            "SELECT COUNT(*) FROM Member", Integer.class
        ).getSingleResult();*/
    }
    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public  void sort(){
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
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
    @Test
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //0부터 시작(zero index)
                .limit(2) //최대 2건 조회
                .fetch();
        assertThat(result.size()).isEqualTo(2);
    }
    @Test
    public void paging2() {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();
        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }
    @Test
    public void aggregation() {
        List<Tuple> result = queryFactory // query dsl에서 tuple 제공 // tuple로 사용하지 않고 dto로 뽑아서 한다.
                .select(member.count(),
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
        assertThat(tuple.get(member.age.min())).isEqualTo(10);    }
    /*
    * 팀의 이름과 각 팀의 평균 연력을 구해라
    * */
    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory
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
    /**
     * 팀 A에 소속된 모든 회원
     */
    @Test
    public void join()  {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }
    /*
    세타 조인
    회원의 이름이 팀 이름과 같은 회원 조회
    * */
    @Test
    public void theta_join() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory.select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }
    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and
     t.name='teamA'
     */
    @Test
    public void join_on_filtering() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();
        for( Tuple tuple:result){
            System.out.println("t =" +tuple);
        }

    }
    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinUse() throws Exception {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()  // 멤버를 조회할때 연관된 요팀을 한번에 끌고옴
                .where(member.username.eq("member1"))
                .fetchOne();
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());//이미 로딩된 엔티티인지 초기화된 엔티티인지 알려준다.
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }
    /*
    * 서브 쿼리 여러건 처리 , in 사용
    * */
    @Test
    public void subQueryIn() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member) // 서브 쿼리이기때문에 밖에있는 mebmer랑 겹치면 안되기 때문에
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();
       assertThat(result).extracting("age").containsExactly(20,30,40);
    }

    @Test
    public void selectSubquery() throws Exception {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        // 이것도 static import 할수 있다
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();
        for(Tuple tuple: result){
            System.out.println("tuple"+ tuple);
        }
    }
    @Test
    public void constant() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
        for(Tuple tuple : result){
            System.out.println("tuple ="+tuple);
        }
    }
    @Test
    public void concat() throws Exception {
        String result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
    }
    
    @Test
    public void simpleProjection() throws Exception {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();
        for(String s: result){
            System.out.println("s= "+s);
        }
    }
    @Test
    public void tupleProjection() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username=" + username);
            System.out.println("age=" + age);
        }
    }
    @Test
    public void findDtoByJPQL() throws Exception {
        List<MemberDto> result = em.createQuery(
                        "select new study.querydsl.dto.MemberDto(m.username, m.age) " +
                                "from Member m", MemberDto.class)
                .getResultList();
        for(MemberDto memberDto:result){
            System.out.println("memberDto" + memberDto);
        }
    }
    @Test
    public void findDtoBySetter() throws Exception {
        //given
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for(MemberDto memberDto : result){
            System.out.println("memberDto:" + memberDto);
        }
    }
    @Test
    public void findDtoByConstructor() throws Exception {
        //given
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class, // 타입을 맞춰줘야한다.
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for(MemberDto memberDto : result){
            System.out.println("memberDto:" + memberDto);
        }
    }
    @Test
    public void findDtoUserDto() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class, // userdto에서 name으로 적어서 매칭이 안되서 null 값이 나온다.
                        member.username.as("name"),
                        //이름이 없는경우
                        ExpressionUtils.as(JPAExpressions
                                        .select(memberSub.age.max())
                                        .from(memberSub), "age")
                ))
                        //member.age))
                .from(member)
                .fetch();
        for(UserDto userDto : result){
            System.out.println("memberDto:" + userDto);
        }
    }
    @Test
    public void findDtoByQueryProjection() throws Exception {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
        //Constructor 실행은 되는데 런타임 에러가 나옴
        //컴파일 오류로 할수 있다
        //방법은 컴파일러로 타입을 체크할 수 있으므로 가장 안전한 방법이다.
        // 다만 DTO에 QueryDSL 어노테이션을 유지해야 하는 점과 DTO까지 Q 파일을 생성해야 하는 단점이 있다.
    }

    //동적쿼리 해결할때
    @Test
    public void dynamicQuery_BooleanBuilder() throws Exception {
        String usernameParam = "member1"; // 파라미터값에 따라 하나만 반환하고 다양한 검색 조건을 하낟
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }
        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    public void dynamicQuery_WhereParam() throws Exception {
        String usernameParam = "member1"; // 파라미터값에 따라 하나만 반환하고 다양한 검색 조건을 하낟
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return  queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                .fetch();

    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }
    private BooleanExpression allEq(String usernameCond, Integer ageCond){
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }
    // 광고 상태가  isVaild, 날짜가 IN isService 이런씩으로 조합이 가능하다.
//    private BooleanExpression isServiceable(String usernameCond, Integer ageCond){
//        return isVaild(usernameCond).and(DatateBetweenIn(ageCond));
//    }

    @Test
    @Commit
    public void bulkUpdate() throws Exception {
        //member 1 = 10 -> 비회원
        //member 2 = 20 -> 비회원
        //member 3 = 30 -> 유지
        //member 4  = 40 -> 유지
        // 영속성 컨텍스에 올라가있는데  영속성 컨텍스트에 있는 엔티티를 무시하고 실행되기 때문에 배치 쿼리를
        //실행하고 나면 영속성 컨텍스트를 초기화 하는 것이 안전하다.
        queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();
        em.flush();
        em.clear();
        //e데이터를 맞추기위해 초기화 시키면 영속속 컨텍ㅅ트와 같아짐
        List<Member> result = queryFactory.selectFrom(member).fetch();
        for(Member member1 : result){
            System.out.println("member1="+member1);
        }

    }
    @Test
    public void bulkAdd() throws Exception {
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1)) // multiply(2) 곱하기
                .execute();
    }
    @Test
    public void bulkDelete() throws Exception {
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }
    @Test
    public void sqlFunction() throws Exception {
        List<String> result = queryFactory
                .select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})",
                        member.username, "member", "M"))
                .from(member)
                .fetch();
        for(String s: result){
            System.out.println("s=" +s);
        }

    }

    @Test
    public void sqlFunction2() throws Exception {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
               // .where(member.username.eq(Expressions.stringTemplate("function('lower' , {0})", member.username)))
                .where(member.username.eq(member.username.lower()))
                .fetch();

        for(String s: result){
            System.out.println("s=" +s);
        }

    }
}
