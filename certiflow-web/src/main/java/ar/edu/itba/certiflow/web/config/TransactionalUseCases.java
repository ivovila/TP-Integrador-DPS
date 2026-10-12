package ar.edu.itba.certiflow.web.config;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import org.springframework.transaction.support.TransactionTemplate;

public final class TransactionalUseCases {

    private TransactionalUseCases() {
    }

    public static <U> U wrap(Class<U> useCase, U target, TransactionTemplate transactions) {
        Object proxy = Proxy.newProxyInstance(useCase.getClassLoader(), new Class<?>[] {useCase},
                (instance, method, arguments) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return method.invoke(target, arguments);
                    }
                    return transactions.execute(status -> {
                        try {
                            return method.invoke(target, arguments);
                        } catch (InvocationTargetException failure) {
                            throw unchecked(failure.getCause());
                        } catch (IllegalAccessException failure) {
                            throw new IllegalStateException(failure);
                        }
                    });
                });
        return useCase.cast(proxy);
    }

    private static RuntimeException unchecked(Throwable cause) {
        if (cause instanceof RuntimeException runtime) {
            return runtime;
        }
        if (cause instanceof Error error) {
            throw error;
        }
        return new IllegalStateException(cause);
    }
}
