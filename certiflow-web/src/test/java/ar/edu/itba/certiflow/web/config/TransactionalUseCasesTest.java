package ar.edu.itba.certiflow.web.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class TransactionalUseCasesTest {

    interface Greet {
        String execute(String name);
    }

    static final class RecordingManager extends AbstractPlatformTransactionManager {
        final List<String> events = new ArrayList<>();

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            events.add("begin");
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            events.add("commit");
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            events.add("rollback");
        }
    }

    @Test
    void eachCallRunsInsideATransactionThatCommits() {
        RecordingManager manager = new RecordingManager();
        Greet greet = TransactionalUseCases.wrap(Greet.class, name -> "Hi " + name, new TransactionTemplate(manager));

        assertEquals("Hi Ana", greet.execute("Ana"));
        assertEquals(List.of("begin", "commit"), manager.events);
    }

    @Test
    void aDomainExceptionRollsBackAndReachesTheCallerUnwrapped() {
        RecordingManager manager = new RecordingManager();
        IllegalStateException failure = new IllegalStateException("rule broken");
        Greet greet = TransactionalUseCases.wrap(Greet.class, name -> {
            throw failure;
        }, new TransactionTemplate(manager));

        assertSame(failure, assertThrows(IllegalStateException.class, () -> greet.execute("Ana")));
        assertTrue(manager.events.contains("rollback"));
    }
}
