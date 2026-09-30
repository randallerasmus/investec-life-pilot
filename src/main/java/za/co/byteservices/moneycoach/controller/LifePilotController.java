package za.co.byteservices.moneycoach.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.byteservices.moneycoach.dto.BalanceForecastRequest;
import za.co.byteservices.moneycoach.dto.BalanceForecastResponse;
import za.co.byteservices.moneycoach.dto.LifePilotScenarioRequest;
import za.co.byteservices.moneycoach.dto.LifePilotScenarioResponse;
import za.co.byteservices.moneycoach.service.BalanceForecastService;
import za.co.byteservices.moneycoach.service.LifePilotScenarioService;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
public class LifePilotController {

    private final LifePilotScenarioService scenarioService;
    private final BalanceForecastService balanceForecastService;

    public LifePilotController(LifePilotScenarioService scenarioService,
                               BalanceForecastService balanceForecastService) {
        this.scenarioService = scenarioService;
        this.balanceForecastService = balanceForecastService;
    }

    @PostMapping("/api/lifepilot/scenarios")
    public LifePilotScenarioResponse simulateScenario(@Valid @RequestBody LifePilotScenarioRequest request) {
        return scenarioService.simulate(request);
    }

    /**
     * Projects the account balance forward. A forecast is a read, so the same
     * projection is reachable by query parameters for quick inspection and by a
     * request body when a caller needs the full set of options.
     */
    @GetMapping("/api/lifepilot/accounts/{accountId}/forecast")
    public BalanceForecastResponse forecastBalance(
            @PathVariable String accountId,
            @RequestParam(required = false) Integer horizonDays,
            @RequestParam(required = false) BigDecimal minimumBalanceThreshold,
            @RequestParam(required = false) BigDecimal expectedMonthlyIncome,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nextIncomeDate,
            @RequestParam(required = false) Boolean includeDiscretionarySpend) {

        BalanceForecastRequest request = new BalanceForecastRequest(
                horizonDays,
                minimumBalanceThreshold,
                expectedMonthlyIncome,
                nextIncomeDate,
                includeDiscretionarySpend
        );

        return balanceForecastService.forecast(accountId, request);
    }

    @PostMapping("/api/lifepilot/accounts/{accountId}/forecast")
    public BalanceForecastResponse forecastBalance(@PathVariable String accountId,
                                                   @Valid @RequestBody(required = false) BalanceForecastRequest request) {
        return balanceForecastService.forecast(accountId, request);
    }
}
