package ar.edu.itba.certiflow.domain.model.shared;

public record YesNo(boolean value) implements RecordedValue {

    @Override
    public boolean supersedes(RecordedValue previous) {
        return previous instanceof YesNo;
    }
}
