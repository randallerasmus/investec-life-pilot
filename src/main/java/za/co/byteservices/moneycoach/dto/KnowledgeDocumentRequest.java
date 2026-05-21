package za.co.byteservices.moneycoach.dto;

import jakarta.validation.constraints.NotBlank;

public class KnowledgeDocumentRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    @NotBlank
    private String source;

    public KnowledgeDocumentRequest() {
    }

    public KnowledgeDocumentRequest(String title, String content, String source) {
        this.title = title;
        this.content = content;
        this.source = source;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getSource() {
        return source;
    }
}
