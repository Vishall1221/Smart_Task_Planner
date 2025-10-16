package com.SmartTaskPlanner.repository;

import com.SmartTaskPlanner.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {}
