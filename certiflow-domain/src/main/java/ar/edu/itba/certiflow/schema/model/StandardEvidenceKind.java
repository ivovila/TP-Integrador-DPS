package ar.edu.itba.certiflow.schema.model;


public enum StandardEvidenceKind implements EvidenceKind {
    PHOTO("Photo"),
    DOCUMENT("Document");

    private final String label;

    StandardEvidenceKind(String label) {
        this.label = label;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public String label() {
        return label;
    }
}
