package com.kickstats.repository;

import com.kickstats.entity.League;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeagueRepository extends JpaRepository<League, Integer> {

    boolean existsByName(String name);

    boolean existsByNameAndLeagueIdNot(String name, Integer leagueId);
}
