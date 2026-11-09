package ar.edu.itba.certiflow.harness;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Las repeticiones en las que respondieron todos los casos. Solo esas entran en los totales: una repetición con una
 * llamada fallida sumaría un caso de menos y se leería como un omitido que el modelo nunca cometió.
 */
record CompleteRepetitions(List<CaseResult> cases, int runsPerCase) {

    List<Integer> numbers() {
        return IntStream.rangeClosed(1, runsPerCase)
                .filter(run -> !cases.isEmpty() && cases.stream()
                        .allMatch(result -> result.runs().stream().anyMatch(measured -> measured.run() == run)))
                .boxed().toList();
    }
}
