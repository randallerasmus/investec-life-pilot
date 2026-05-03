package za.co.byteservices.moneycoach.dto;

import java.math.BigDecimal;

public class InvestecBalanceResponse {

    private Data data;
    private Links links;
    private Meta meta;

    public Data getData() {
        return data;
    }

    public Links getLinks() {
        return links;
    }

    public Meta getMeta() {
        return meta;
    }

    public static class Data {
        private String accountId;
        private BigDecimal currentBalance;
        private BigDecimal availableBalance;
        private String currency;

        public String getAccountId() {
            return accountId;
        }

        public BigDecimal getCurrentBalance() {
            return currentBalance;
        }

        public BigDecimal getAvailableBalance() {
            return availableBalance;
        }

        public String getCurrency() {
            return currency;
        }
    }

    public static class Links {
        private String self;

        public String getSelf() {
            return self;
        }
    }

    public static class Meta {
        private Integer totalPages;

        public Integer getTotalPages() {
            return totalPages;
        }
    }
}
