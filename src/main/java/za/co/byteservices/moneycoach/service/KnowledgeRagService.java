package za.co.byteservices.moneycoach.service;

import org.springframework.stereotype.Service;
import za.co.byteservices.moneycoach.dto.KnowledgeDocumentRequest;
import za.co.byteservices.moneycoach.dto.KnowledgeDocumentResponse;
import za.co.byteservices.moneycoach.dto.KnowledgeSearchResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

@Service
public class KnowledgeRagService {

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^a-z0-9]+");

    private final CopyOnWriteArrayList<KnowledgeDocument> documents = new CopyOnWriteArrayList<>();

    public KnowledgeRagService() {
        seedDefaults();
    }

    public List<KnowledgeDocumentResponse> getDocuments() {
        return documents.stream()
                .map(document -> new KnowledgeDocumentResponse(
                        document.id(),
                        document.title(),
                        document.content(),
                        document.source()
                ))
                .toList();
    }

    public KnowledgeDocumentResponse addDocument(KnowledgeDocumentRequest request) {
        KnowledgeDocument document = new KnowledgeDocument(
                UUID.randomUUID().toString(),
                request.getTitle().trim(),
                request.getContent().trim(),
                request.getSource().trim()
        );
        documents.add(document);
        return new KnowledgeDocumentResponse(document.id(), document.title(), document.content(), document.source());
    }

    public List<KnowledgeSearchResult> search(String question) {
        List<String> terms = tokenize(question);
        if (terms.isEmpty()) {
            return List.of();
        }

        return documents.stream()
                .map(document -> toSearchResult(document, terms))
                .filter(result -> result.getRelevanceScore() > 0)
                .sorted(Comparator.comparingInt(KnowledgeSearchResult::getRelevanceScore).reversed())
                .limit(5)
                .toList();
    }

    private KnowledgeSearchResult toSearchResult(KnowledgeDocument document, List<String> terms) {
        String haystack = (document.title() + " " + document.content() + " " + document.source()).toLowerCase(Locale.US);
        int score = 0;
        for (String term : terms) {
            if (term.length() < 2) {
                continue;
            }
            if (document.title().toLowerCase(Locale.US).contains(term)) {
                score += 3;
            }
            if (document.source().toLowerCase(Locale.US).contains(term)) {
                score += 1;
            }
            if (haystack.contains(term)) {
                score += 2;
            }
        }
        return new KnowledgeSearchResult(
                document.title(),
                document.source(),
                snippet(document.content(), terms),
                score
        );
    }

    private String snippet(String content, List<String> terms) {
        String lowered = content.toLowerCase(Locale.US);
        int start = 0;
        for (String term : terms) {
            int index = lowered.indexOf(term);
            if (index >= 0) {
                start = Math.max(0, index - 20);
                break;
            }
        }
        int end = Math.min(content.length(), start + 160);
        return content.substring(start, end).trim();
    }

    private List<String> tokenize(String text) {
        List<String> terms = new ArrayList<>();
        for (String token : TOKEN_SPLIT.split(text.toLowerCase(Locale.US))) {
            if (!token.isBlank()) {
                terms.add(token);
            }
        }
        return terms;
    }

    private void seedDefaults() {
        documents.add(new KnowledgeDocument(
                UUID.randomUUID().toString(),
                "Emergency Fund Basics",
                "An emergency fund helps cover unexpected expenses without relying on debt or selling long-term investments under pressure.",
                "internal-financial-education"
        ));
        documents.add(new KnowledgeDocument(
                UUID.randomUUID().toString(),
                "Payday Buffer Planning",
                "A payday buffer reduces the risk of running out of money before income lands. Protect core bills and leave room for spending volatility.",
                "internal-financial-education"
        ));
        documents.add(new KnowledgeDocument(
                UUID.randomUUID().toString(),
                "Recurring Expense Review",
                "Review recurring subscriptions, debt repayments, rent, and insurance first when spending feels high. Fixed outflows often explain cash pressure.",
                "internal-financial-education"
        ));
    }

    private record KnowledgeDocument(String id, String title, String content, String source) {
    }
}
