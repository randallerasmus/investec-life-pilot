package za.co.byteservices.moneycoach.service;

import org.junit.jupiter.api.Test;
import za.co.byteservices.moneycoach.dto.KnowledgeDocumentRequest;
import za.co.byteservices.moneycoach.dto.KnowledgeDocumentResponse;
import za.co.byteservices.moneycoach.dto.KnowledgeSearchResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeRagServiceTest {

    private final KnowledgeRagService service = new KnowledgeRagService();

    @Test
    void addsDocumentAndReturnsItFromDocumentListing() {
        KnowledgeDocumentResponse response = service.addDocument(new KnowledgeDocumentRequest(
                "Emergency Fund Basics",
                "An emergency fund helps cover unexpected expenses without using debt.",
                "internal-financial-education"
        ));

        List<KnowledgeDocumentResponse> documents = service.getDocuments();

        assertThat(response.getId()).isNotBlank();
        assertThat(documents)
                .extracting(KnowledgeDocumentResponse::getTitle)
                .contains("Emergency Fund Basics");
    }

    @Test
    void searchesDocumentsUsingKeywordScoring() {
        service.addDocument(new KnowledgeDocumentRequest(
                "Emergency Fund Basics",
                "An emergency fund helps cover unexpected expenses without using debt.",
                "internal-financial-education"
        ));

        List<KnowledgeSearchResult> results = service.search("Why do I need an emergency fund?");

        assertThat(results).isNotEmpty();
        assertThat(results)
                .anyMatch(result -> result.getTitle().equals("Emergency Fund Basics")
                        && result.getRelevanceScore() > 0);
    }
}
