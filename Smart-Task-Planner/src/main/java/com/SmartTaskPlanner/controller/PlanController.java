package com.SmartTaskPlanner.controller;

import com.SmartTaskPlanner.dto.GoalRequest;
import com.SmartTaskPlanner.model.Plan;
import com.SmartTaskPlanner.repository.PlanRepository;
import com.SmartTaskPlanner.service.PlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class PlanController {

    @Autowired
    private PlanService planService;

    @Autowired
    private PlanRepository planRepository;

    @PostMapping("/plan")
    public ResponseEntity<Plan> generatePlan(@RequestBody GoalRequest request) {
        Plan plan = planService.createPlanFromGoal(request.getGoal());
        return ResponseEntity.ok(plan);
    }

    @GetMapping("/plan/{id}")
    public ResponseEntity<Plan> getPlan(@PathVariable Long id) {
        return planRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
