package za.co.byteservices.moneycoach.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.byteservices.moneycoach.dto.SafeToSpendResponse;
import za.co.byteservices.moneycoach.service.MoneyCoachService;

import java.math.BigDecimal;

@RestController
public class MoneyCoachController {

    private final MoneyCoachService moneyCoachService;

    public MoneyCoachController(MoneyCoachService moneyCoachService) {
        this.moneyCoachService = moneyCoachService;
    }

    @GetMapping("/api/coach/accounts/{accountId}/safe-to-spend")
    public SafeToSpendResponse getSafeToSpend(
            @PathVariable String accountId,
            @RequestParam(required = false, defaultValue = "0") BigDecimal estimatedBills,
            @RequestParam(required = false, defaultValue = "0") BigDecimal goalSavingAmount
    ) {
        return moneyCoachService.calculateSafeToSpend(
                accountId,
                estimatedBills,
                goalSavingAmount
        );
    }
}
