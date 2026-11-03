package com.kickstats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.kickstats.entity.League;
import com.kickstats.entity.Match;
import com.kickstats.entity.Season;
import com.kickstats.entity.Stadium;
import com.kickstats.entity.Team;
import com.kickstats.repository.TeamRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// 실제 MySQL을 사용하며, 변경 내용은 테스트 후 롤백된다.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TeamControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private EntityManager em;

    @Test
    void listShowsHomeStadiumWithoutOpenSession() throws Exception {
        Stadium stadium = saveStadium("팀 목록 테스트 경기장");
        saveTeam("팀 목록 테스트 팀", stadium);
        em.flush();
        em.clear();

        mockMvc.perform(get("/teams"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("팀 목록 테스트 팀")))
                .andExpect(content().string(containsString("팀 목록 테스트 경기장")));
    }

    @Test
    void createSavesAllValues() throws Exception {
        Stadium stadium = saveStadium("등록 테스트 경기장");

        mockMvc.perform(post("/teams")
                        .param("name", " 등록 테스트 팀 ").param("city", "테스트시")
                        .param("homeStadiumId", stadium.getStadiumId().toString()).param("foundedYear", "1999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams"));
        em.flush();
        em.clear();

        Team saved = teamRepository.findAllByOrderByNameAsc().stream()
                .filter(t -> t.getName().equals("등록 테스트 팀")).findFirst().orElseThrow();
        assertThat(saved.getCity()).isEqualTo("테스트시");
        assertThat(saved.getHomeStadium().getName()).isEqualTo("등록 테스트 경기장");
        assertThat(saved.getFoundedYear()).isEqualTo((short) 1999);
    }

    @Test
    void createAllowsEmptyOptionalValues() throws Exception {
        mockMvc.perform(post("/teams")
                        .param("name", "선택값 없는 테스트 팀").param("city", "")
                        .param("homeStadiumId", "").param("foundedYear", ""))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void invalidFormKeepsStadiumOptions() throws Exception {
        saveStadium("옵션 테스트 경기장");

        mockMvc.perform(post("/teams").param("name", "").param("foundedYear", "1500"))
                .andExpect(status().isOk())
                .andExpect(view().name("teams/form"))
                .andExpect(model().attributeHasFieldErrors("teamForm", "name", "foundedYear"))
                .andExpect(content().string(containsString("옵션 테스트 경기장")));
    }

    @Test
    void duplicateNameKeepsStadiumOptions() throws Exception {
        saveTeam("중복 테스트 팀", null);
        saveStadium("중복 옵션 테스트 경기장");

        mockMvc.perform(post("/teams").param("name", "중복 테스트 팀"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("이미 등록된 팀 이름입니다.")))
                .andExpect(content().string(containsString("중복 옵션 테스트 경기장")));
    }

    @Test
    void editFormSelectsCurrentStadium() throws Exception {
        Stadium stadium = saveStadium("선택 테스트 경기장");
        Team team = saveTeam("선택 테스트 팀", stadium);

        mockMvc.perform(get("/teams/{id}/edit", team.getTeamId()))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*value=\"" + stadium.getStadiumId()
                        + "\"[^>]*selected[^>]*>.*")));
    }

    @Test
    void updateCanClearStadium() throws Exception {
        Team team = saveTeam("경기장 해제 테스트 팀", saveStadium("해제 테스트 경기장"));

        mockMvc.perform(post("/teams/{id}", team.getTeamId())
                        .param("name", "경기장 해제 테스트 팀").param("homeStadiumId", ""))
                .andExpect(status().is3xxRedirection());
        em.flush();
        em.clear();

        assertThat(em.find(Team.class, team.getTeamId()).getHomeStadium()).isNull();
    }

    @Test
    void createWithMissingStadiumReturnsNotFound() throws Exception {
        mockMvc.perform(post("/teams").param("name", "없는 경기장 테스트 팀")
                        .param("homeStadiumId", String.valueOf(Integer.MAX_VALUE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRemovesTeamWithoutMatches() throws Exception {
        Team team = saveTeam("삭제 테스트 팀", null);

        mockMvc.perform(post("/teams/{id}/delete", team.getTeamId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "팀을 삭제했습니다."));
        em.flush();

        assertThat(teamRepository.existsById(team.getTeamId())).isFalse();
    }

    @Test
    void deleteIsBlockedWhenTeamHasMatches() throws Exception {
        Team home = saveTeam("경기 있는 홈 테스트 팀", null);
        Team away = saveTeam("경기 있는 원정 테스트 팀", null);
        saveMatch(home, away);

        for (Team team : new Team[] {home, away}) {
            mockMvc.perform(post("/teams/{id}/delete", team.getTeamId()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/teams"))
                    .andExpect(flash().attribute("error", "경기 기록이 있는 팀은 삭제할 수 없습니다."));
            assertThat(teamRepository.existsById(team.getTeamId())).isTrue();
        }
    }

    @Test
    void errorFlashMessageIsRendered() throws Exception {
        mockMvc.perform(get("/teams").flashAttr("error", "오류 표시 테스트"))
                .andExpect(content().string(containsString("오류 표시 테스트")))
                .andExpect(content().string(containsString("message-error")));
    }

    private Stadium saveStadium(String name) {
        Stadium stadium = new Stadium();
        stadium.setName(name);
        em.persist(stadium);
        em.flush();
        return stadium;
    }

    private Team saveTeam(String name, Stadium stadium) {
        Team team = new Team();
        team.setName(name);
        team.setHomeStadium(stadium);
        return teamRepository.saveAndFlush(team);
    }

    private void saveMatch(Team home, Team away) {
        League league = new League();
        league.setName("팀 삭제 테스트 리그");
        league.setCountry("대한민국");
        em.persist(league);
        Season season = new Season();
        season.setLeague(league);
        season.setYear("2099");
        em.persist(season);
        Match match = new Match();
        match.setSeason(season);
        match.setHomeTeam(home);
        match.setAwayTeam(away);
        match.setMatchDate(LocalDateTime.of(2099, 3, 1, 14, 0));
        em.persist(match);
        em.flush();
    }
}
