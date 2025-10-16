package com.SmartTaskPlanner.repository;

import com.SmartTaskPlanner.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {}
