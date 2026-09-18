package ar.edu.itba.certiflow.domain.evaluation;


public enum StandardEvidenceKind implements EvidenceKind {
    PHOTO("Photo"),
    DOCUMENT("Document");

    private final String label;

    StandardEvidenceKind(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
