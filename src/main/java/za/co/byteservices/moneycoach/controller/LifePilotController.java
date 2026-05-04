package za.co.byteservices.moneycoach.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import za.co.byteservices.moneycoach.dto.LifePilotScenarioRequest;
import za.co.byteservices.moneycoach.dto.LifePilotScenarioResponse;
import za.co.byteservices.moneycoach.service.LifePilotScenarioService;

@RestController
public class LifePilotController {

    private final LifePilotScenarioService scenarioService;

    public LifePilotController(LifePilotScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @PostMapping("/api/lifepilot/scenarios")
    public LifePilotScenarioResponse simulateScenario(@RequestBody LifePilotScenarioRequest request) {
        return scenarioService.simulate(request);
    }
}
