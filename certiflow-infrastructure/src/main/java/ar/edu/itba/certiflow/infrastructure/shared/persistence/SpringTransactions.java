package ar.edu.itba.certiflow.infrastructure.shared.persistence;

import ar.edu.itba.certiflow.shared.model.Transactions;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Cada unidad de trabajo es una transacción propia: confirma si termina y se deshace si lanza. */
@Component
public class SpringTransactions implements Transactions {

    private final TransactionTemplate transactionTemplate;

    public SpringTransactions(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T within(Supplier<T> work) {
        return transactionTemplate.execute(status -> work.get());
    }
}
