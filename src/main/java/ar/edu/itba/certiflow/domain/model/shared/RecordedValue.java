package ar.edu.itba.certiflow.domain.model.shared;

public interface RecordedValue {

    boolean supersedes(RecordedValue previous);
}
