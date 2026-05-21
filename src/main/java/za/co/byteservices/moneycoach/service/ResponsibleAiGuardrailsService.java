package za.co.byteservices.moneycoach.service;

import org.springframework.stereotype.Service;
import za.co.byteservices.moneycoach.dto.GuardrailCheckResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ResponsibleAiGuardrailsService {

    public static final String EDUCATIONAL_DISCLAIMER =
            "This is educational guidance based on available transaction data, not regulated financial advice.";

    public GuardrailCheckResponse checkAnswer(String answer) {
        String resolvedAnswer = answer == null ? "" : answer.trim();
        String lowered = resolvedAnswer.toLowerCase(Locale.US);
        List<String> warnings = new ArrayList<>();

        if (lowered.contains("definitely") || lowered.contains("guaranteed") || lowered.contains("will definitely")) {
            warnings.add("Detected guaranteed-return or certainty language.");
        }
        if (lowered.contains("all your money in one stock") || lowered.contains("one stock")) {
            warnings.add("Detected concentrated investment advice presented too strongly.");
        }
        if (lowered.contains("take a loan") || lowered.contains("borrow more") || lowered.contains("max out")) {
            warnings.add("Detected reckless loan or leverage suggestion.");
        }
        if (lowered.contains("you should invest") || lowered.contains("you should buy") || lowered.contains("you should definitely")) {
            warnings.add("Detected unsupported financial instruction without qualification.");
        }
        if (lowered.contains("i checked") || lowered.contains("your income will") || lowered.contains("your salary will")) {
            warnings.add("Detected claim that may imply unavailable or unsupported data.");
        }

        if (warnings.isEmpty()) {
            return new GuardrailCheckResponse(List.of(), ensureDisclaimer(resolvedAnswer), false);
        }

        String safeAnswer = "Based on the available account and transaction context, avoid treating this as a guaranteed outcome. "
                + "Review affordability, diversification, and near-term cash needs before acting. "
                + EDUCATIONAL_DISCLAIMER;
        return new GuardrailCheckResponse(warnings, safeAnswer, true);
    }

    private String ensureDisclaimer(String answer) {
        if (answer == null || answer.isBlank()) {
            return EDUCATIONAL_DISCLAIMER;
        }
        if (answer.contains(EDUCATIONAL_DISCLAIMER)) {
            return answer;
        }
        return answer + " " + EDUCATIONAL_DISCLAIMER;
    }
}
