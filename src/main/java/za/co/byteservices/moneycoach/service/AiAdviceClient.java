package za.co.byteservices.moneycoach.service;

import za.co.byteservices.moneycoach.dto.MoneyCoachAdviceResponse;

import java.util.Optional;

public interface AiAdviceClient {

    Optional<String> generateAdvice(MoneyCoachAdviceResponse deterministicAdvice);
}
