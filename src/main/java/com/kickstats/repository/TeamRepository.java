package com.kickstats.repository;

import com.kickstats.entity.Team;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Integer> {

    @EntityGraph(attributePaths = "homeStadium")
    List<Team> findAllByOrderByNameAsc();

    boolean existsByName(String name);

    boolean existsByNameAndTeamIdNot(String name, Integer teamId);
}
