package za.co.byteservices.moneycoach.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * What the card code knows at the moment of a swipe.
 *
 * <p>Field names follow the Investec authorisation object so the card code can
 * forward values without renaming them. The amount stays in cents, as the card
 * reports it, and is converted once on this side.
 */
public class CardAuthorizationRequest {

    @NotBlank
    private String accountId;

    @PositiveOrZero
    private long centsAmount;

    private String currencyCode;
    private String merchantName;
    private String merchantCategoryCode;
    private String cardId;
    private String reference;

    public CardAuthorizationRequest() {
    }

    public CardAuthorizationRequest(String accountId,
                                    long centsAmount,
                                    String currencyCode,
                                    String merchantName,
                                    String merchantCategoryCode,
                                    String cardId,
                                    String reference) {
        this.accountId = accountId;
        this.centsAmount = centsAmount;
        this.currencyCode = currencyCode;
        this.merchantName = merchantName;
        this.merchantCategoryCode = merchantCategoryCode;
        this.cardId = cardId;
        this.reference = reference;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public long getCentsAmount() {
        return centsAmount;
    }

    public void setCentsAmount(long centsAmount) {
        this.centsAmount = centsAmount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getMerchantCategoryCode() {
        return merchantCategoryCode;
    }

    public void setMerchantCategoryCode(String merchantCategoryCode) {
        this.merchantCategoryCode = merchantCategoryCode;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}
