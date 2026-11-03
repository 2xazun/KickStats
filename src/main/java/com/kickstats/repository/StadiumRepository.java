package com.kickstats.repository;

import com.kickstats.entity.Stadium;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StadiumRepository extends JpaRepository<Stadium, Integer> {
}
