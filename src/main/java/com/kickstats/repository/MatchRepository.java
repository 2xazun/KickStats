package com.kickstats.repository;

import com.kickstats.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<Match, Integer> {

    boolean existsByHomeTeamTeamIdOrAwayTeamTeamId(Integer homeTeamId, Integer awayTeamId);
}
