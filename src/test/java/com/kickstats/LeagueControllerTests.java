package com.kickstats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.kickstats.entity.League;
import com.kickstats.entity.Season;
import com.kickstats.repository.LeagueRepository;
import jakarta.persistence.EntityManager;
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
class LeagueControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LeagueRepository leagueRepository;

    @Autowired
    private EntityManager em;

    @Test
    void homeRedirectsToLeagueList() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/leagues"));
    }

    @Test
    void listShowsLeagues() throws Exception {
        saveLeague("컨트롤러 테스트 리그");

        mockMvc.perform(get("/leagues"))
                .andExpect(status().isOk())
                .andExpect(view().name("leagues/list"))
                .andExpect(content().string(containsString("컨트롤러 테스트 리그")));
    }

    @Test
    void newFormRenders() throws Exception {
        mockMvc.perform(get("/leagues/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("리그 등록")));
    }

    @Test
    void createSavesTrimmedNameAndRedirects() throws Exception {
        mockMvc.perform(post("/leagues").param("name", "  새 테스트 리그  ").param("country", "대한민국"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/leagues"));

        assertThat(leagueRepository.existsByName("새 테스트 리그")).isTrue();
    }

    @Test
    void createRejectsBlankValues() throws Exception {
        long before = leagueRepository.count();

        mockMvc.perform(post("/leagues").param("name", "   ").param("country", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("leagues/form"))
                .andExpect(model().attributeHasFieldErrors("leagueForm", "name", "country"))
                .andExpect(content().string(containsString("리그 이름을 입력하세요.")));

        assertThat(leagueRepository.count()).isEqualTo(before);
    }

    @Test
    void createRejectsDuplicateName() throws Exception {
        saveLeague("중복 테스트 리그");

        mockMvc.perform(post("/leagues").param("name", "중복 테스트 리그").param("country", "대한민국"))
                .andExpect(status().isOk())
                .andExpect(view().name("leagues/form"))
                .andExpect(content().string(containsString("이미 등록된 리그 이름입니다.")));
    }

    @Test
    void editFormShowsCurrentValues() throws Exception {
        League league = saveLeague("수정 전 리그");

        mockMvc.perform(get("/leagues/{id}/edit", league.getLeagueId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("수정 전 리그")))
                .andExpect(content().string(containsString("/leagues/" + league.getLeagueId() + "\"")));
    }

    @Test
    void updateChangesValuesAndAllowsKeepingOwnName() throws Exception {
        League league = saveLeague("수정 테스트 리그");

        mockMvc.perform(post("/leagues/{id}", league.getLeagueId())
                        .param("name", "수정 테스트 리그").param("country", "일본"))
                .andExpect(status().is3xxRedirection());

        assertThat(leagueRepository.findById(league.getLeagueId()).orElseThrow().getCountry()).isEqualTo("일본");
    }

    @Test
    void updateRejectsNameOfAnotherLeagueAndKeepsFormAction() throws Exception {
        saveLeague("다른 리그");
        League league = saveLeague("내 리그");

        mockMvc.perform(post("/leagues/{id}", league.getLeagueId())
                        .param("name", "다른 리그").param("country", "대한민국"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("이미 등록된 리그 이름입니다.")))
                .andExpect(content().string(containsString("/leagues/" + league.getLeagueId() + "\"")));
    }

    @Test
    void deleteRemovesLeagueAndCascadesToSeasons() throws Exception {
        League league = saveLeague("삭제 테스트 리그");
        Season season = new Season();
        season.setLeague(league);
        season.setYear("2099");
        em.persist(season);
        em.flush();
        em.clear();

        mockMvc.perform(post("/leagues/{id}/delete", league.getLeagueId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/leagues"));
        em.flush();
        em.clear();

        assertThat(leagueRepository.existsById(league.getLeagueId())).isFalse();
        assertThat(em.find(Season.class, season.getSeasonId())).isNull();
    }

    @Test
    void missingLeagueReturnsNotFound() throws Exception {
        mockMvc.perform(get("/leagues/{id}/edit", Integer.MAX_VALUE)).andExpect(status().isNotFound());
        mockMvc.perform(post("/leagues/{id}/delete", Integer.MAX_VALUE)).andExpect(status().isNotFound());
    }

    private League saveLeague(String name) {
        League league = new League();
        league.setName(name);
        league.setCountry("대한민국");
        return leagueRepository.saveAndFlush(league);
    }
}
