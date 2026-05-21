package za.co.byteservices.moneycoach.dto;

import java.util.List;

public class GuardrailCheckResponse {

    private final List<String> warnings;
    private final String safeAnswer;
    private final boolean rewritten;

    public GuardrailCheckResponse(List<String> warnings, String safeAnswer, boolean rewritten) {
        this.warnings = warnings;
        this.safeAnswer = safeAnswer;
        this.rewritten = rewritten;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public String getSafeAnswer() {
        return safeAnswer;
    }

    public boolean isRewritten() {
        return rewritten;
    }
}
