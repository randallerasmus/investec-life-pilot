package za.co.byteservices.moneycoach.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import za.co.byteservices.moneycoach.config.DemoProperties;
import za.co.byteservices.moneycoach.dto.InvestecAccountResponse;
import za.co.byteservices.moneycoach.dto.InvestecBalanceResponse;
import za.co.byteservices.moneycoach.dto.InvestecTransactionResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates a plausible account for someone evaluating LifePilot.
 *
 * <p>The figures are built relative to the date they are asked for rather than
 * hardcoded, so the demo never drifts into the past. They are also fully
 * deterministic: the same date always produces the same history, which keeps
 * the forecast reproducible and the tests honest.
 *
 * <p>The generated data goes through Jackson into the same DTOs the live API
 * fills, so it exercises the real deserialisation path rather than a parallel
 * one that could quietly diverge.
 *
 * <p>The profile is deliberately tight rather than comfortable. Income roughly
 * matches outgoings, so the balance climbs on payday and grinds down to a
 * trough just before the next one. That trough is the thing LifePilot exists
 * to show, and an account with a large surplus would demonstrate nothing.
 *
 * <p>A second, comfortable profile exists for the simulator alone. On the tight
 * account every decision fails, so without it the simulator could only ever say
 * one thing. It earns more and owes less, leaving a few thousand rand of surplus
 * a month: enough that a small decision fits and a large one does not.
 */
@Service
public class DemoAccountData {

    private static final String CURRENCY = "ZAR";

    /** Whole months of history behind the requested date. See {@link #ledgerStart}. */
    private static final int LEDGER_MONTHS = 12;

    private static final BigDecimal OPENING_BALANCE = new BigDecimal("31500.00");

    /** Monthly commitments, as day-of-month to description and amount. */
    private static final List<Commitment> COMMITMENTS = List.of(
            new Commitment(1, "BOND REPAYMENT STANDARD BANK", "12500.00"),
            new Commitment(1, "MEDICAL AID DISCOVERY", "2100.00"),
            new Commitment(2, "VIRGIN ACTIVE GYM", "499.00"),
            new Commitment(3, "VEHICLE FINANCE WESBANK", "4200.00"),
            new Commitment(5, "SCHOOL FEES CRAWFORD", "3500.00"),
            new Commitment(7, "CAR INSURANCE OUTSURANCE", "899.00"),
            new Commitment(12, "SPOTIFY PREMIUM", "79.00"),
            new Commitment(15, "NETFLIX SUBSCRIPTION", "199.00")
    );

    private static final int SALARY_DAY = 25;
    private static final BigDecimal SALARY = new BigDecimal("39000.00");

    private static final Profile TIGHT = new Profile(
            "LifePilot Demo Account", "10012345678", OPENING_BALANCE, "SALARY BYTE SERVICES", SALARY, COMMITMENTS);

    private static final Profile COMFORTABLE = new Profile(
            "LifePilot Demo Account (room to spare)",
            "10012345679",
            new BigDecimal("12000.00"),
            "SALARY BYTE SERVICES",
            new BigDecimal("30000.00"),
            List.of(
                    new Commitment(1, "RENT PAM GOLDING", "9500.00"),
                    new Commitment(1, "MEDICAL AID DISCOVERY", "2100.00"),
                    new Commitment(2, "VIRGIN ACTIVE GYM", "499.00"),
                    new Commitment(7, "CAR INSURANCE OUTSURANCE", "899.00"),
                    new Commitment(12, "SPOTIFY PREMIUM", "79.00"),
                    new Commitment(15, "NETFLIX SUBSCRIPTION", "199.00")
            )
    );

    private final DemoProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DemoAccountData(DemoProperties properties) {
        this.properties = properties;
    }

    public InvestecAccountResponse accounts() {
        return accountsAlongside(null);
    }

    /**
     * The demo account, listed after any real ones.
     *
     * <p>Appending rather than replacing means a deployment with working
     * credentials can still reach the demo account, which is what makes a
     * screenshot or a walkthrough reproducible for someone else.
     */
    public InvestecAccountResponse accountsAlongside(InvestecAccountResponse existing) {
        List<Object> accounts = new ArrayList<>();

        if (existing != null && existing.getData() != null && existing.getData().getAccounts() != null) {
            existing.getData().getAccounts().forEach(account -> accounts.add(objectMapper.convertValue(account, Map.class)));
        }

        accounts.add(listing(properties.getAccountId(), TIGHT));
        if (properties.getComfortableAccountId() != null) {
            accounts.add(listing(properties.getComfortableAccountId(), COMFORTABLE));
        }

        return objectMapper.convertValue(
                Map.of("data", Map.of("accounts", accounts)),
                InvestecAccountResponse.class
        );
    }

    private Map<String, Object> listing(String accountId, Profile profile) {
        Map<String, Object> demo = new LinkedHashMap<>();
        demo.put("accountId", accountId);
        demo.put("accountNumber", profile.accountNumber);
        demo.put("accountName", profile.name);
        demo.put("referenceName", "Demo");
        demo.put("productName", "Demo Account");
        return demo;
    }

    public InvestecBalanceResponse balance(LocalDate asOf) {
        return balance(properties.getAccountId(), asOf);
    }

    public InvestecBalanceResponse balance(String accountId, LocalDate asOf) {
        Profile profile = profileFor(accountId);
        BigDecimal current = replayTo(profile, accountId, asOf, null, null);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("accountId", accountId);
        data.put("currentBalance", current);
        data.put("availableBalance", current);
        data.put("currency", CURRENCY);

        return objectMapper.convertValue(Map.of("data", data), InvestecBalanceResponse.class);
    }

    public InvestecTransactionResponse transactions(LocalDate fromDate, LocalDate toDate) {
        return transactions(properties.getAccountId(), fromDate, toDate);
    }

    public InvestecTransactionResponse transactions(String accountId, LocalDate fromDate, LocalDate toDate) {
        List<Map<String, Object>> transactions = new ArrayList<>();
        replayTo(profileFor(accountId), accountId, toDate, fromDate, transactions);

        return objectMapper.convertValue(
                Map.of("data", Map.of("transactions", transactions)),
                InvestecTransactionResponse.class
        );
    }

    private Profile profileFor(String accountId) {
        return properties.isComfortable(accountId) ? COMFORTABLE : TIGHT;
    }

    /**
     * Walks the ledger from {@link #LEDGER_START} to {@code toDate}, collecting
     * the transactions from {@code emitFrom} onwards when a sink is supplied.
     *
     * <p>Balance and transactions come from this one walk, so the closing
     * balance and the last running balance cannot disagree.
     */
    private BigDecimal replayTo(Profile profile,
                                String accountId,
                                LocalDate toDate,
                                LocalDate emitFrom,
                                List<Map<String, Object>> sink) {
        BigDecimal running = profile.openingBalance;

        for (LocalDate date = ledgerStart(toDate); !date.isAfter(toDate); date = date.plusDays(1)) {
            for (Movement movement : movementsOn(profile, date)) {
                running = movement.credit
                        ? running.add(movement.amount)
                        : running.subtract(movement.amount);

                if (sink != null && !date.isBefore(emitFrom)) {
                    sink.add(toMap(accountId, movement, date, running));
                }
            }
        }

        return running.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Where the replay begins: the first of the month, a whole number of months back.
     *
     * <p>Whole months matter twice over. A window that cut mid-month would
     * include a different number of paydays depending on the day it was asked
     * for, which made the balance jump between adjacent dates. And a window
     * fixed to one calendar date would let the small monthly surplus compound,
     * so the demo account would quietly grow comfortable and stop showing the
     * pre-payday dip it exists to demonstrate. A window of constant length
     * anchored to month boundaries has neither problem.
     */
    private LocalDate ledgerStart(LocalDate asOf) {
        return asOf.withDayOfMonth(1).minusMonths(LEDGER_MONTHS);
    }

    private List<Movement> movementsOn(Profile profile, LocalDate date) {
        List<Movement> movements = new ArrayList<>();
        int dayOfMonth = date.getDayOfMonth();

        if (dayOfMonth == SALARY_DAY) {
            movements.add(new Movement(profile.salaryDescription, profile.salary, true));
        }

        for (Commitment commitment : profile.commitments) {
            if (commitment.dayOfMonth == dayOfMonth) {
                movements.add(new Movement(commitment.description, new BigDecimal(commitment.amount), false));
            }
        }

        movements.addAll(variableSpendOn(date));
        return movements;
    }

    /**
     * Everyday card spending, varied by a hash of the date so it looks real
     * without being random: the same date always produces the same basket.
     */
    private List<Movement> variableSpendOn(LocalDate date) {
        List<Movement> movements = new ArrayList<>();
        int seed = seed(date);
        int dayOfWeek = date.getDayOfWeek().getValue();

        // A weekly grocery shop, on Saturdays.
        if (dayOfWeek == 6) {
            movements.add(new Movement("WOOLWORTHS FOOD", amount(900, 600, seed), false));
        }

        // Fuel roughly every ten days.
        if (date.getDayOfYear() % 10 == 3) {
            movements.add(new Movement("ENGEN FUEL", amount(850, 300, seed + 1), false));
        }

        // Coffee and lunch on weekdays.
        if (dayOfWeek <= 5) {
            movements.add(new Movement("CARD PURCHASE", amount(120, 180, seed + 2), false));
        }

        // A restaurant most Fridays.
        if (dayOfWeek == 5 && seed % 4 != 0) {
            movements.add(new Movement("RESTAURANT", amount(380, 320, seed + 3), false));
        }

        return movements;
    }

    /** A stable pseudo-random value for a date; no {@code Random}, so no JDK drift. */
    private int seed(LocalDate date) {
        long hash = date.toEpochDay() * 2654435761L;
        hash ^= hash >>> 15;
        return (int) Math.floorMod(hash, 1000L);
    }

    private BigDecimal amount(int base, int spread, int seed) {
        int offset = spread == 0 ? 0 : Math.abs(seed * 37 % spread);
        return BigDecimal.valueOf(base + offset).setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Object> toMap(String accountId, Movement movement, LocalDate date, BigDecimal running) {
        Map<String, Object> transaction = new LinkedHashMap<>();
        transaction.put("accountId", accountId);
        transaction.put("type", movement.credit ? "CREDIT" : "DEBIT");
        transaction.put("transactionType", movement.credit ? "CardCredit" : "CardPurchases");
        transaction.put("status", "POSTED");
        transaction.put("description", movement.description);
        transaction.put("transactionDate", date.toString());
        transaction.put("valueDate", date.toString());
        transaction.put("postingDate", date.toString());
        transaction.put("actionDate", date.toString());
        transaction.put("amount", movement.amount);
        transaction.put("runningBalance", running.setScale(2, RoundingMode.HALF_UP));
        return transaction;
    }

    private record Commitment(int dayOfMonth, String description, String amount) {
    }

    private record Profile(String name,
                           String accountNumber,
                           BigDecimal openingBalance,
                           String salaryDescription,
                           BigDecimal salary,
                           List<Commitment> commitments) {
    }

    private static final class Movement {
        private final String description;
        private final BigDecimal amount;
        private final boolean credit;

        private Movement(String description, BigDecimal amount, boolean credit) {
            this.description = description;
            this.amount = amount;
            this.credit = credit;
        }
    }
}
