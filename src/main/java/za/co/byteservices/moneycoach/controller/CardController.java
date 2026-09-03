package za.co.byteservices.moneycoach.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import za.co.byteservices.moneycoach.config.CardGuardrailProperties;
import za.co.byteservices.moneycoach.dto.CardAuthorizationRequest;
import za.co.byteservices.moneycoach.dto.CardAuthorizationResponse;
import za.co.byteservices.moneycoach.dto.SpendingSnapshot;
import za.co.byteservices.moneycoach.service.CardAuthorizationService;
import za.co.byteservices.moneycoach.service.SpendingSnapshotService;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@RestController
public class CardController {

    static final String CARD_KEY_HEADER = "X-LifePilot-Card-Key";

    private final CardAuthorizationService cardAuthorizationService;
    private final SpendingSnapshotService spendingSnapshotService;
    private final CardGuardrailProperties properties;

    public CardController(CardAuthorizationService cardAuthorizationService,
                          SpendingSnapshotService spendingSnapshotService,
                          CardGuardrailProperties properties) {
        this.cardAuthorizationService = cardAuthorizationService;
        this.spendingSnapshotService = spendingSnapshotService;
        this.properties = properties;
    }

    /**
     * Answers a card authorisation from the cached snapshot.
     *
     * <p>Called from {@code beforeTransaction}, which has two seconds for
     * everything including the round trip, so this path does no outbound work.
     */
    @PostMapping("/api/lifepilot/cards/authorization")
    public CardAuthorizationResponse authorize(
            @RequestHeader(value = CARD_KEY_HEADER, required = false) String cardKey,
            @Valid @RequestBody CardAuthorizationRequest request) {
        requireCardKey(cardKey);
        return cardAuthorizationService.authorize(request);
    }

    /**
     * Rebuilds the snapshot for an account.
     *
     * <p>Slow: this is the forecast the authorisation path refuses to run. Call
     * it from {@code afterTransaction}, which has fifteen seconds, or on a
     * schedule.
     */
    @PostMapping("/api/lifepilot/cards/accounts/{accountId}/snapshot")
    public SpendingSnapshot refreshSnapshot(
            @RequestHeader(value = CARD_KEY_HEADER, required = false) String cardKey,
            @PathVariable String accountId) {
        requireCardKey(cardKey);
        return spendingSnapshotService.refresh(accountId, Instant.now());
    }

    /**
     * These endpoints face the public internet and answer with balances, so an
     * unconfigured secret disables them rather than leaving them open.
     */
    private void requireCardKey(String presented) {
        String expected = properties.getSharedSecret();

        if (expected == null || expected.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Card endpoints are disabled until lifepilot.card.shared-secret is configured.");
        }

        if (presented == null || !constantTimeEquals(expected, presented)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid card key.");
        }
    }

    /** Compared without short-circuiting so a wrong key does not leak its prefix. */
    private boolean constantTimeEquals(String expected, String presented) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                presented.getBytes(StandardCharsets.UTF_8));
    }
}
