package com.kickstats;

import static org.assertj.core.api.Assertions.assertThat;

import com.kickstats.entity.Appearance;
import com.kickstats.entity.AppearanceId;
import com.kickstats.entity.Card;
import com.kickstats.entity.CardType;
import com.kickstats.entity.Contract;
import com.kickstats.entity.Goal;
import com.kickstats.entity.League;
import com.kickstats.entity.Match;
import com.kickstats.entity.Player;
import com.kickstats.entity.Position;
import com.kickstats.entity.Season;
import com.kickstats.entity.Stadium;
import com.kickstats.entity.Team;
import com.kickstats.repository.AppearanceRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

// 실제 MySQL에 저장했다가 다시 읽어 매핑을 확인한다. 트랜잭션은 테스트 후 롤백된다.
@SpringBootTest
@Transactional
class EntityMappingTests {

    @Autowired
    private EntityManager em;

    @Autowired
    private AppearanceRepository appearanceRepository;

    @Test
    void savesAndReadsAllEntities() {
        League league = new League();
        league.setName("매핑 테스트 리그");
        league.setCountry("대한민국");
        em.persist(league);

        Stadium stadium = new Stadium();
        stadium.setName("매핑 테스트 경기장");
        em.persist(stadium);

        Team home = team("매핑 테스트 홈팀", stadium);
        Team away = team("매핑 테스트 원정팀", null);

        Season season = new Season();
        season.setLeague(league);
        season.setYear("2099");
        season.getTeams().add(home);
        season.getTeams().add(away);
        em.persist(season);

        Player scorer = new Player();
        scorer.setName("매핑 테스트 선수");
        scorer.setPosition(Position.FW);
        em.persist(scorer);

        Contract contract = new Contract();
        contract.setPlayer(scorer);
        contract.setTeam(home);
        contract.setBackNumber(200);
        contract.setStartDate(LocalDate.of(2099, 1, 1));
        em.persist(contract);

        Match match = new Match();
        match.setSeason(season);
        match.setHomeTeam(home);
        match.setAwayTeam(away);
        match.setStadium(stadium);
        match.setMatchDate(LocalDateTime.of(2099, 3, 1, 14, 0));
        match.setHomeScore(1);
        match.setAwayScore(0);
        em.persist(match);

        Appearance appearance = new Appearance();
        appearance.setMatch(match);
        appearance.setPlayer(scorer);
        appearance.setTeam(home);
        appearance.setStarter(true);
        appearance.setMinutesPlayed(90);
        em.persist(appearance);

        Goal goal = new Goal();
        goal.setMatch(match);
        goal.setTeam(home);
        goal.setScorer(scorer);
        goal.setMinute(10);
        goal.setPenalty(true);
        em.persist(goal);

        Card card = new Card();
        card.setMatch(match);
        card.setPlayer(scorer);
        card.setCardType(CardType.YELLOW);
        card.setMinute(20);
        em.persist(card);

        em.flush();
        em.clear();

        Season savedSeason = em.find(Season.class, season.getSeasonId());
        assertThat(savedSeason.getTeams()).extracting(Team::getName)
                .containsExactlyInAnyOrder("매핑 테스트 홈팀", "매핑 테스트 원정팀");

        Appearance savedAppearance = appearanceRepository
                .findById(new AppearanceId(match.getMatchId(), scorer.getPlayerId()))
                .orElseThrow();
        assertThat(savedAppearance.isStarter()).isTrue();
        assertThat(savedAppearance.getTeam().getName()).isEqualTo("매핑 테스트 홈팀");
        assertThat(savedAppearance.getPlayer().getPosition()).isEqualTo(Position.FW);

        Goal savedGoal = em.find(Goal.class, goal.getGoalId());
        assertThat(savedGoal.isPenalty()).isTrue();
        assertThat(savedGoal.isOwnGoal()).isFalse();
        assertThat(savedGoal.getAssistPlayer()).isNull();

        assertThat(em.find(Card.class, card.getCardId()).getCardType()).isEqualTo(CardType.YELLOW);
        assertThat(em.find(Contract.class, contract.getContractId()).getEndDate()).isNull();
        assertThat(em.find(Match.class, match.getMatchId()).getHomeScore()).isEqualTo(1);
    }

    @Test
    void readsSeedDataWhenPresent() {
        long players = em.createQuery("select count(p) from Player p", Long.class).getSingleResult();
        if (players == 0) {
            return;
        }
        // 자책골·PK 등 저장된 값을 Entity로 읽을 수 있는지 전체 행을 불러와 확인한다.
        assertThat(em.createQuery("select g from Goal g", Goal.class).getResultList()).isNotEmpty();
        assertThat(em.createQuery("select c from Card c", Card.class).getResultList()).isNotEmpty();
        assertThat(em.createQuery("select a from Appearance a", Appearance.class).getResultList()).isNotEmpty();
        assertThat(em.createQuery("select m from Match m", Match.class).getResultList()).isNotEmpty();
    }

    private Team team(String name, Stadium stadium) {
        Team team = new Team();
        team.setName(name);
        team.setHomeStadium(stadium);
        em.persist(team);
        return team;
    }
}
