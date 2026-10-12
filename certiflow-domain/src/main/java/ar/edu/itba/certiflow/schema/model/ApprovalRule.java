package ar.edu.itba.certiflow.schema.model;

public interface ApprovalRule<A extends Answer> {

    boolean isSatisfiedBy(A answer);

    A answerFrom(RawAnswer raw);
}
