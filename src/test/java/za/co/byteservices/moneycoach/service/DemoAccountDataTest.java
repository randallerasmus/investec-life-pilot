package za.co.byteservices.moneycoach.service;

import org.junit.jupiter.api.Test;
import za.co.byteservices.moneycoach.config.DemoProperties;
import za.co.byteservices.moneycoach.dto.InvestecTransactionResponse;
import za.co.byteservices.moneycoach.dto.RecurringPayment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DemoAccountDataTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 15);

    private final DemoProperties properties = new DemoProperties();
    private final DemoAccountData demoData = new DemoAccountData(properties);
    private final RecurringPaymentDetector detector = new RecurringPaymentDetector();

    @Test
    void producesTheSameHistoryEveryTimeForAGivenDate() {
        List<InvestecTransactionResponse.Transaction> first = transactions();
        List<InvestecTransactionResponse.Transaction> second = transactions();

        assertThat(first).hasSameSizeAs(second);
        for (int i = 0; i < first.size(); i += 1) {
            assertThat(first.get(i).getDescription()).isEqualTo(second.get(i).getDescription());
            assertThat(first.get(i).getAmount()).isEqualByComparingTo(second.get(i).getAmount());
            assertThat(first.get(i).getTransactionDate()).isEqualTo(second.get(i).getTransactionDate());
        }
    }

    @Test
    void movesWithTheDateSoTheDemoNeverGoesStale() {
        LocalDate laterToday = TODAY.plusYears(1);

        List<InvestecTransactionResponse.Transaction> later = demoData
                .transactions(laterToday.minusDays(180), laterToday)
                .getData()
                .getTransactions();

        assertThat(later).isNotEmpty();
        assertThat(LocalDate.parse(later.get(later.size() - 1).getTransactionDate()))
                .isAfterOrEqualTo(laterToday.minusDays(2));
    }

    @Test
    void doesNotSwingBetweenAdjacentDays() {
        // A window anchored to the requested date rather than to month boundaries
        // replayed a different number of paydays either side of the salary date,
        // which moved the balance by tens of thousands between two adjacent days.
        BigDecimal previous = null;

        for (int day = 1; day <= 28; day += 1) {
            LocalDate date = LocalDate.of(2026, 9, day);
            BigDecimal balance = demoData.balance(date).getData().getAvailableBalance();

            if (previous != null && day != 25) {
                assertThat(balance.subtract(previous).abs())
                        .as("day-on-day change into %s", date)
                        .isLessThan(new BigDecimal("21000.00"));
            }
            previous = balance;
        }
    }

    @Test
    void staysTightInsteadOfGrowingComfortableOverTheYears() {
        // The monthly surplus is small, but a ledger pinned to one fixed start
        // date would let it compound until the account no longer dips before
        // payday and the demo stops demonstrating anything.
        for (int year = 2026; year <= 2030; year += 1) {
            BigDecimal trough = troughOfMonth(LocalDate.of(year, 9, 1));

            assertThat(trough)
                    .as("September %s trough", year)
                    .isBetween(new BigDecimal("-8000.00"), new BigDecimal("8000.00"));
        }
    }

    @Test
    void dipsTowardsZeroJustBeforePayday() {
        BigDecimal dayBeforePayday = demoData.balance(LocalDate.of(2026, 9, 24))
                .getData().getAvailableBalance();
        BigDecimal payday = demoData.balance(LocalDate.of(2026, 9, 25))
                .getData().getAvailableBalance();

        assertThat(dayBeforePayday).isLessThan(new BigDecimal("3000.00"));
        assertThat(payday).isGreaterThan(dayBeforePayday.add(new BigDecimal("30000.00")));
    }

    @Test
    void carriesEnoughRepetitionForTheDetectorToFindTheCommitments() {
        List<String> merchants = detector.detect(transactions(), TODAY).stream()
                .map(RecurringPayment::getMerchant)
                .toList();

        assertThat(merchants)
                .anyMatch(merchant -> merchant.contains("BOND"))
                .anyMatch(merchant -> merchant.contains("NETFLIX"))
                .anyMatch(merchant -> merchant.contains("VEHICLE FINANCE"));
    }

    @Test
    void carriesADetectableSalary() {
        List<RecurringPayment> income = detector.detectIncome(transactions(), TODAY);

        assertThat(income).isNotEmpty();
        assertThat(income.get(0).getExpectedAmount()).isEqualByComparingTo("39000.00");
    }

    @Test
    void labelsDirectionWithTheTypeFieldTheLiveApiUses() {
        assertThat(transactions()).allSatisfy(transaction -> {
            assertThat(transaction.getType()).isIn("DEBIT", "CREDIT");
            // The live API reports positive amounts and carries direction in type.
            assertThat(transaction.getAmount()).isPositive();
        });
    }

    @Test
    void reportsABalanceThatAgreesWithTheHistory() {
        List<InvestecTransactionResponse.Transaction> history = transactions();
        BigDecimal balance = demoData.balance(TODAY).getData().getAvailableBalance();

        assertThat(balance)
                .isEqualByComparingTo(history.get(history.size() - 1).getRunningBalance());
    }

    @Test
    void listsBothDemoAccounts() {
        assertThat(demoData.accounts().getData().getAccounts())
                .extracting(account -> account.getAccountId())
                .containsExactly("demo-account", "demo-comfortable");
    }

    @Test
    void keepsTheComfortableAccountAboveZeroBeforeEveryPayday() {
        for (int monthsBack = 0; monthsBack < 6; monthsBack++) {
            LocalDate dayBeforePayday = TODAY.withDayOfMonth(24).minusMonths(monthsBack);
            BigDecimal balance = demoData.balance("demo-comfortable", dayBeforePayday).getData().getAvailableBalance();
            // The point of this account is that its trough has room in it.
            assertThat(balance).isGreaterThan(new BigDecimal("5000.00"));
        }
    }

    private BigDecimal troughOfMonth(LocalDate firstOfMonth) {
        BigDecimal trough = null;
        for (int day = 1; day <= 28; day += 1) {
            BigDecimal balance = demoData.balance(firstOfMonth.withDayOfMonth(day))
                    .getData().getAvailableBalance();
            if (trough == null || balance.compareTo(trough) < 0) {
                trough = balance;
            }
        }
        return trough;
    }

    private List<InvestecTransactionResponse.Transaction> transactions() {
        return demoData.transactions(TODAY.minusDays(180), TODAY).getData().getTransactions();
    }
}
