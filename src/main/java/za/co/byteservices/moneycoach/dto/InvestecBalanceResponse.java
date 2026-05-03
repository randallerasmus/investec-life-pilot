package za.co.byteservices.moneycoach.dto;

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
        private Double currentBalance;
        private Double availableBalance;
        private String currency;

        public String getAccountId() {
            return accountId;
        }

        public Double getCurrentBalance() {
            return currentBalance;
        }

        public Double getAvailableBalance() {
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
