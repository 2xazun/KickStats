package com.kickstats.repository;

import com.kickstats.entity.AppearanceId;
import com.kickstats.entity.Appearance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppearanceRepository extends JpaRepository<Appearance, AppearanceId> {
}
