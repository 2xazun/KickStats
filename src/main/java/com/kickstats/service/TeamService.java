package com.kickstats.service;

import com.kickstats.dto.TeamForm;
import com.kickstats.entity.Stadium;
import com.kickstats.entity.Team;
import com.kickstats.exception.DuplicateNameException;
import com.kickstats.exception.InUseException;
import com.kickstats.exception.NotFoundException;
import com.kickstats.repository.AppearanceRepository;
import com.kickstats.repository.GoalRepository;
import com.kickstats.repository.MatchRepository;
import com.kickstats.repository.StadiumRepository;
import com.kickstats.repository.TeamRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final StadiumRepository stadiumRepository;
    private final MatchRepository matchRepository;
    private final AppearanceRepository appearanceRepository;
    private final GoalRepository goalRepository;

    public List<Team> findAll() {
        return teamRepository.findAllByOrderByNameAsc();
    }

    public TeamForm getForm(Integer teamId) {
        Team team = getTeam(teamId);
        TeamForm form = new TeamForm();
        form.setName(team.getName());
        form.setCity(team.getCity());
        form.setHomeStadiumId(team.getHomeStadium() == null ? null : team.getHomeStadium().getStadiumId());
        form.setFoundedYear(team.getFoundedYear() == null ? null : team.getFoundedYear().intValue());
        return form;
    }

    @Transactional
    public void create(TeamForm form) {
        String name = form.getName().strip();
        if (teamRepository.existsByName(name)) {
            throw new DuplicateNameException("이미 등록된 팀 이름입니다.");
        }
        Team team = new Team();
        apply(team, name, form);
        teamRepository.save(team);
    }

    @Transactional
    public void update(Integer teamId, TeamForm form) {
        Team team = getTeam(teamId);
        String name = form.getName().strip();
        if (teamRepository.existsByNameAndTeamIdNot(name, teamId)) {
            throw new DuplicateNameException("이미 등록된 팀 이름입니다.");
        }
        apply(team, name, form);
    }

    // 계약과 시즌 참가 정보는 DB의 ON DELETE CASCADE로 함께 삭제되지만,
    // 경기 기록이 있는 팀은 FK로 삭제가 막히므로 먼저 확인한다.
    @Transactional
    public void delete(Integer teamId) {
        Team team = getTeam(teamId);
        if (matchRepository.existsByHomeTeamTeamIdOrAwayTeamTeamId(teamId, teamId)
                || appearanceRepository.existsByTeamTeamId(teamId)
                || goalRepository.existsByTeamTeamId(teamId)) {
            throw new InUseException("경기 기록이 있는 팀은 삭제할 수 없습니다.");
        }
        teamRepository.delete(team);
    }

    private Team getTeam(Integer teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("팀을 찾을 수 없습니다: " + teamId));
    }

    private void apply(Team team, String name, TeamForm form) {
        team.setName(name);
        team.setCity(form.getCity() == null || form.getCity().isBlank() ? null : form.getCity().strip());
        team.setHomeStadium(findStadium(form.getHomeStadiumId()));
        team.setFoundedYear(form.getFoundedYear() == null ? null : form.getFoundedYear().shortValue());
    }

    private Stadium findStadium(Integer stadiumId) {
        if (stadiumId == null) {
            return null;
        }
        return stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new NotFoundException("경기장을 찾을 수 없습니다: " + stadiumId));
    }
}
