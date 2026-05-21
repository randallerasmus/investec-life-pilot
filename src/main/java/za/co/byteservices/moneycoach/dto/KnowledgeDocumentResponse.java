package za.co.byteservices.moneycoach.dto;

public class KnowledgeDocumentResponse {

    private final String id;
    private final String title;
    private final String content;
    private final String source;

    public KnowledgeDocumentResponse(String id, String title, String content, String source) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.source = source;
    }

    public String getId() {
        return id;
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
