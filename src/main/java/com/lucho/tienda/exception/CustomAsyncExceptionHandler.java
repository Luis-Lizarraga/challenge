package com.lucho.tienda.exception;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.StaleObjectStateException;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.lang.NonNull;

import java.lang.reflect.Method;

@Slf4j
public class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Override
    public void handleUncaughtException(@NonNull Throwable throwable, @NonNull Method method, @NonNull Object ... params) {

        // 1. Smart Shield: Filter expected business or concurrency exceptions
        if (throwable instanceof BusinessException
                || throwable instanceof ObjectOptimisticLockingFailureException
                || throwable instanceof StaleObjectStateException) {

            // The worker already printed the 1-line log.warn().
            // We finish without printing the giant red StackTrace.
            return;
        }

        // 2. Only log actual server errors (500)
        log.error("Exception message - {}", throwable.getMessage());
        log.error("Method name - {}", method.getName());
        for (Object param : params) {
            log.error("Parameter value - {}", param);
        }
        log.error("Unexpected error occurred in async processing", throwable);
    }
}