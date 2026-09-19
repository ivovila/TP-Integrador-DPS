package ar.edu.itba.certiflow.models.evaluation;


public interface ApprovalRule<A extends Answer> {
    boolean isSatisfiedBy(A answer);
}
