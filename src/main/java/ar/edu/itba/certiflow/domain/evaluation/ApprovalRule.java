package ar.edu.itba.certiflow.domain.evaluation;


public interface ApprovalRule<A extends Answer> {
    boolean isSatisfiedBy(A answer);
}
