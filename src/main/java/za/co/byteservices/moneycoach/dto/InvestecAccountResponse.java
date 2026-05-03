package za.co.byteservices.moneycoach.dto;

import java.util.List;

public class InvestecAccountResponse {

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
        private List<Account> accounts;

        public List<Account> getAccounts() {
            return accounts;
        }
    }

    public static class Account {
        private String accountId;
        private String accountNumber;
        private String accountName;
        private String referenceName;
        private String productName;

        public String getAccountId() {
            return accountId;
        }

        public String getAccountNumber() {
            return accountNumber;
        }

        public String getAccountName() {
            return accountName;
        }

        public String getReferenceName() {
            return referenceName;
        }

        public String getProductName() {
            return productName;
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
