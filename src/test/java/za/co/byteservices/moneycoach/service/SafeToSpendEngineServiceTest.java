package za.co.byteservices.moneycoach.service;

import org.junit.jupiter.api.Test;
import za.co.byteservices.moneycoach.dto.AdvancedSafeToSpendRequest;
import za.co.byteservices.moneycoach.dto.AdvancedSafeToSpendResponse;
import za.co.byteservices.moneycoach.dto.InvestecBalanceResponse;
import za.co.byteservices.moneycoach.dto.InvestecTransactionResponse;
import za.co.byteservices.moneycoach.model.MoneyCoachRiskLevel;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SafeToSpendEngineServiceTest {

    private final InvestecAccountService investecAccountService = mock(InvestecAccountService.class);
    private final SafeToSpendEngineService service = new SafeToSpendEngineService(investecAccountService);

    @Test
    void calculatesAdvancedSafeToSpendFromBalanceAndTransactions() throws Exception {
        when(investecAccountService.getBalance("acc-123")).thenReturn(balance("acc-123", "12500.00", "ZAR"));
        when(investecAccountService.getTransactions(eq("acc-123"), any(), any()))
                .thenReturn(transactions(
                        transaction("acc-123", "Rent debit order", "-4500.00", LocalDate.now().minusDays(10)),
                        transaction("acc-123", "Groceries", "-1800.00", LocalDate.now().minusDays(7)),
                        transaction("acc-123", "Fuel", "-900.00", LocalDate.now().minusDays(5)),
                        transaction("acc-123", "Salary", "22000.00", LocalDate.now().minusDays(15))
                ));

        AdvancedSafeToSpendResponse response = service.calculateAdvancedSafeToSpend("acc-123", new AdvancedSafeToSpendRequest(
                LocalDate.now().plusDays(10),
                new BigDecimal("1000.00"),
                new BigDecimal("2500.00"),
                "Golf clubs"
        ));

        assertThat(response.getCurrentBalance()).isEqualByComparingTo("12500.00");
        assertThat(response.getEstimatedRecurringExpenses()).isGreaterThan(BigDecimal.ZERO);
        assertThat(response.getDaysUntilPayday()).isEqualTo(10);
        assertThat(response.getPlannedPurchaseAmount()).isEqualByComparingTo("2500.00");
        assertThat(response.getAffordabilityStatus()).isIn("AFFORDABLE", "TIGHT");
        assertThat(response.getRiskLevel()).isIn(MoneyCoachRiskLevel.HEALTHY, MoneyCoachRiskLevel.TIGHT);
        assertThat(response.getExplanation()).contains("payday");
    }

    @Test
    void fallsBackGracefullyWhenInvestecDataIsUnavailable() {
        when(investecAccountService.getBalance("acc-123")).thenThrow(new IllegalStateException("No token"));

        AdvancedSafeToSpendResponse response = service.calculateAdvancedSafeToSpend("acc-123", new AdvancedSafeToSpendRequest(
                LocalDate.now().plusDays(7),
                new BigDecimal("500.00"),
                BigDecimal.ZERO,
                null
        ));

        assertThat(response.getCurrentBalance()).isEqualByComparingTo("0.00");
        assertThat(response.getSafeToSpend()).isEqualByComparingTo("-500.00");
        assertThat(response.getRiskLevel()).isEqualTo(MoneyCoachRiskLevel.CRITICAL);
        assertThat(response.getExplanation()).contains("fallback");
    }

    private InvestecBalanceResponse balance(String accountId, String availableBalance, String currency) throws Exception {
        InvestecBalanceResponse response = new InvestecBalanceResponse();
        InvestecBalanceResponse.Data data = new InvestecBalanceResponse.Data();
        setField(data, "accountId", accountId);
        setField(data, "availableBalance", new BigDecimal(availableBalance));
        setField(data, "currentBalance", new BigDecimal(availableBalance));
        setField(data, "currency", currency);
        setField(response, "data", data);
        return response;
    }

    private InvestecTransactionResponse transactions(InvestecTransactionResponse.Transaction... transactions) throws Exception {
        InvestecTransactionResponse response = new InvestecTransactionResponse();
        InvestecTransactionResponse.Data data = new InvestecTransactionResponse.Data();
        setField(data, "transactions", List.of(transactions));
        setField(response, "data", data);
        return response;
    }

    private InvestecTransactionResponse.Transaction transaction(String accountId,
                                                               String description,
                                                               String amount,
                                                               LocalDate transactionDate) throws Exception {
        InvestecTransactionResponse.Transaction transaction = new InvestecTransactionResponse.Transaction();
        setField(transaction, "accountId", accountId);
        setField(transaction, "description", description);
        setField(transaction, "amount", new BigDecimal(amount));
        setField(transaction, "transactionDate", transactionDate.toString());
        return transaction;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
