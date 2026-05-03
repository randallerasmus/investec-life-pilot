package za.co.byteservices.moneycoach.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InvestecTransactionResponse {

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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private List<Transaction> transactions;

        public List<Transaction> getTransactions() {
            return transactions;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Transaction {
        private String accountId;
        private String type;
        private String transactionType;
        private String status;
        private String description;
        private String cardNumber;
        private String postedOrder;
        private String postingDate;
        private String valueDate;
        private String actionDate;
        private String transactionDate;
        private BigDecimal amount;
        private BigDecimal runningBalance;

        public String getAccountId() {
            return accountId;
        }

        public String getType() {
            return type;
        }

        public String getTransactionType() {
            return transactionType;
        }

        public String getStatus() {
            return status;
        }

        public String getDescription() {
            return description;
        }

        public String getCardNumber() {
            return cardNumber;
        }

        public String getPostedOrder() {
            return postedOrder;
        }

        public String getPostingDate() {
            return postingDate;
        }

        public String getValueDate() {
            return valueDate;
        }

        public String getActionDate() {
            return actionDate;
        }

        public String getTransactionDate() {
            return transactionDate;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public BigDecimal getRunningBalance() {
            return runningBalance;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Links {
        private String self;

        public String getSelf() {
            return self;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        private Integer totalPages;

        public Integer getTotalPages() {
            return totalPages;
        }
    }
}
