package za.co.byteservices.moneycoach.dto;

public class KnowledgeSearchResult {

    private final String title;
    private final String source;
    private final String contentSnippet;
    private final int relevanceScore;

    public KnowledgeSearchResult(String title, String source, String contentSnippet, int relevanceScore) {
        this.title = title;
        this.source = source;
        this.contentSnippet = contentSnippet;
        this.relevanceScore = relevanceScore;
    }

    public String getTitle() {
        return title;
    }

    public String getSource() {
        return source;
    }

    public String getContentSnippet() {
        return contentSnippet;
    }

    public int getRelevanceScore() {
        return relevanceScore;
    }
}
