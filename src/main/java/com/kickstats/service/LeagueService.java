package com.kickstats.service;

import com.kickstats.dto.LeagueForm;
import com.kickstats.entity.League;
import com.kickstats.exception.DuplicateNameException;
import com.kickstats.exception.NotFoundException;
import com.kickstats.repository.LeagueRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeagueService {

    private final LeagueRepository leagueRepository;

    public List<League> findAll() {
        return leagueRepository.findAll(Sort.by("name"));
    }

    public LeagueForm getForm(Integer leagueId) {
        League league = getLeague(leagueId);
        LeagueForm form = new LeagueForm();
        form.setName(league.getName());
        form.setCountry(league.getCountry());
        return form;
    }

    @Transactional
    public void create(LeagueForm form) {
        String name = form.getName().strip();
        if (leagueRepository.existsByName(name)) {
            throw new DuplicateNameException("이미 등록된 리그 이름입니다.");
        }
        League league = new League();
        apply(league, name, form);
        leagueRepository.save(league);
    }

    @Transactional
    public void update(Integer leagueId, LeagueForm form) {
        League league = getLeague(leagueId);
        String name = form.getName().strip();
        if (leagueRepository.existsByNameAndLeagueIdNot(name, leagueId)) {
            throw new DuplicateNameException("이미 등록된 리그 이름입니다.");
        }
        apply(league, name, form);
    }

    // DB의 ON DELETE CASCADE로 시즌과 경기 기록도 함께 삭제된다.
    @Transactional
    public void delete(Integer leagueId) {
        leagueRepository.delete(getLeague(leagueId));
    }

    private League getLeague(Integer leagueId) {
        return leagueRepository.findById(leagueId)
                .orElseThrow(() -> new NotFoundException("리그를 찾을 수 없습니다: " + leagueId));
    }

    private void apply(League league, String name, LeagueForm form) {
        league.setName(name);
        league.setCountry(form.getCountry().strip());
    }
}
