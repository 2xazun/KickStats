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

import com.kickstats.entity.Stadium;
import com.kickstats.entity.Team;
import com.kickstats.repository.StadiumRepository;
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
class StadiumControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StadiumRepository stadiumRepository;

    @Autowired
    private EntityManager em;

    @Test
    void listShowsCityAndFormattedCapacity() throws Exception {
        saveStadium("목록 테스트 경기장", "테스트시", 12345);
        saveStadium("정보 없는 테스트 경기장", null, null);

        mockMvc.perform(get("/stadiums"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("목록 테스트 경기장")))
                .andExpect(content().string(containsString("12,345석")))
                .andExpect(content().string(containsString("정보 없는 테스트 경기장")));
    }

    @Test
    void createStoresBlankOptionalValuesAsNull() throws Exception {
        mockMvc.perform(post("/stadiums")
                        .param("name", "  새 테스트 경기장 ").param("city", "  ").param("capacity", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/stadiums"));
        em.flush();

        Stadium saved = stadiumRepository.findAll().stream()
                .filter(s -> s.getName().equals("새 테스트 경기장")).findFirst().orElseThrow();
        assertThat(saved.getCity()).isNull();
        assertThat(saved.getCapacity()).isNull();
    }

    @Test
    void createRejectsInvalidValues() throws Exception {
        mockMvc.perform(post("/stadiums").param("name", "").param("capacity", "-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("stadiums/form"))
                .andExpect(model().attributeHasFieldErrors("stadiumForm", "name", "capacity"))
                .andExpect(content().string(containsString("수용 인원은 0 이상으로 입력하세요.")));
    }

    @Test
    void createShowsKoreanMessageForNonNumericCapacity() throws Exception {
        mockMvc.perform(post("/stadiums").param("name", "숫자 테스트 경기장").param("capacity", "abc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("수용 인원은 숫자로 입력하세요.")));
    }

    @Test
    void createRejectsDuplicateName() throws Exception {
        saveStadium("중복 테스트 경기장", null, null);

        mockMvc.perform(post("/stadiums").param("name", "중복 테스트 경기장"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("이미 등록된 경기장 이름입니다.")));
    }

    @Test
    void updateChangesValues() throws Exception {
        Stadium stadium = saveStadium("수정 테스트 경기장", "이전시", 100);

        mockMvc.perform(post("/stadiums/{id}", stadium.getStadiumId())
                        .param("name", "수정 테스트 경기장").param("city", "새시").param("capacity", "200"))
                .andExpect(status().is3xxRedirection());

        Stadium saved = stadiumRepository.findById(stadium.getStadiumId()).orElseThrow();
        assertThat(saved.getCity()).isEqualTo("새시");
        assertThat(saved.getCapacity()).isEqualTo(200);
    }

    @Test
    void deleteClearsHomeStadiumOfTeam() throws Exception {
        Stadium stadium = saveStadium("삭제 테스트 경기장", null, null);
        Team team = new Team();
        team.setName("경기장 삭제 테스트 팀");
        team.setHomeStadium(stadium);
        em.persist(team);
        em.flush();
        em.clear();

        mockMvc.perform(post("/stadiums/{id}/delete", stadium.getStadiumId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/stadiums"));
        em.flush();
        em.clear();

        assertThat(stadiumRepository.existsById(stadium.getStadiumId())).isFalse();
        assertThat(em.find(Team.class, team.getTeamId()).getHomeStadium()).isNull();
    }

    @Test
    void missingStadiumReturnsNotFound() throws Exception {
        mockMvc.perform(get("/stadiums/{id}/edit", Integer.MAX_VALUE)).andExpect(status().isNotFound());
    }

    private Stadium saveStadium(String name, String city, Integer capacity) {
        Stadium stadium = new Stadium();
        stadium.setName(name);
        stadium.setCity(city);
        stadium.setCapacity(capacity);
        return stadiumRepository.saveAndFlush(stadium);
    }
}
