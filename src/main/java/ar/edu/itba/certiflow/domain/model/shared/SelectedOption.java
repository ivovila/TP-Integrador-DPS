package ar.edu.itba.certiflow.domain.model.shared;

public record SelectedOption(String value) implements RecordedValue {

    @Override
    public boolean supersedes(RecordedValue previous) {
        return previous instanceof SelectedOption;
    }
}
