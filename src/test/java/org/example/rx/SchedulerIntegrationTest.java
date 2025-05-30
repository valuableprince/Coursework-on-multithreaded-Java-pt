package org.example.rx;

import org.example.rx.schedulers.ComputationScheduler;
import org.example.rx.schedulers.IOThreadScheduler;
import org.example.rx.schedulers.SingleThreadScheduler;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестовый класс для проверки интеграции планировщиков
 */
class SchedulerIntegrationTest {
    /**
     * Тест проверяет работу subscribeOn с разными планировщиками
     * - Создается Observable с IOThreadScheduler
     * - Проверяется, что выполнение происходит в отдельном потоке
     * - Проверяется, что поток отличается от основного
     */
    @Test
    void testSubscribeOnWithDifferentSchedulers() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> executionThread = new AtomicReference<>();
        String mainThread = Thread.currentThread().getName();

        Observable.create(observer -> {
            executionThread.set(Thread.currentThread().getName());
            observer.onNext(1);
            observer.onComplete();
            latch.countDown();
        })
        .subscribeOn(new IOThreadScheduler())
        .subscribe(
            item -> {},
            error -> fail("Unexpected error"),
            () -> {}
        );

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertNotEquals(mainThread, executionThread.get());
    }

    /**
     * Тест проверяет работу observeOn с разными планировщиками
     * - Создается Observable с ComputationScheduler
     * - Проверяется, что наблюдение происходит в отдельном потоке
     * - Проверяется, что поток отличается от основного
     */
    @Test
    void testObserveOnWithDifferentSchedulers() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> observationThread = new AtomicReference<>();
        String mainThread = Thread.currentThread().getName();

        Observable.create(observer -> {
            observer.onNext(1);
            observer.onComplete();
        })
        .observeOn(new ComputationScheduler())
        .subscribe(
            item -> observationThread.set(Thread.currentThread().getName()),
            error -> fail("Unexpected error"),
            () -> latch.countDown()
        );

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertNotEquals(mainThread, observationThread.get());
    }

    /**
     * Тест проверяет последовательное выполнение в SingleThreadScheduler
     * - Создаются два Observable с одним и тем же SingleThreadScheduler
     * - Проверяется, что оба Observable выполняются в одном потоке
     * - Проверяется последовательность выполнения
     */
    @Test
    void testSingleThreadSchedulerSequentialExecution() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        List<String> threadNames = new ArrayList<>();
        SingleThreadScheduler scheduler = new SingleThreadScheduler();

        Observable.create(observer -> {
            threadNames.add(Thread.currentThread().getName());
            observer.onNext(1);
            observer.onComplete();
            latch.countDown();
        })
        .subscribeOn(scheduler)
        .subscribe(
            item -> {},
            error -> fail("Unexpected error"),
            () -> {}
        );

        Observable.create(observer -> {
            threadNames.add(Thread.currentThread().getName());
            observer.onNext(2);
            observer.onComplete();
            latch.countDown();
        })
        .subscribeOn(scheduler)
        .subscribe(
            item -> {},
            error -> fail("Unexpected error"),
            () -> {}
        );

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(2, threadNames.size());
        assertEquals(threadNames.get(0), threadNames.get(1));
    }

    /**
     * Тест проверяет отмену подписки с несколькими подписками
     * - Создается Observable с бесконечной эмиссией
     * - На него подписываются два разных планировщика
     * - Проверяется корректная отмена подписок
     */
    @Test
    void testDisposableWithMultipleSubscriptions() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Integer> lastValue = new AtomicReference<>(0);

        Observable<Integer> source = Observable.create(observer -> {
            int i = 0;
            while (!Thread.currentThread().isInterrupted()) {
                observer.onNext(i++);
                Thread.sleep(100);
            }
        });

        Disposable subscription1 = source
            .subscribeOn(new IOThreadScheduler())
            .subscribe(
                item -> lastValue.set(item),
                error -> fail("Unexpected error"),
                () -> {}
            );

        Disposable subscription2 = source
            .subscribeOn(new IOThreadScheduler())
            .subscribe(
                item -> {},
                error -> fail("Unexpected error"),
                () -> {}
            );

        Thread.sleep(500);
        subscription1.dispose();
        subscription2.dispose();
        int finalValue = lastValue.get();
        Thread.sleep(200);
        assertEquals(finalValue, lastValue.get());
    }

    /**
     * Тест проверяет обработку ошибок в конкурентных операциях
     * - Создается Observable, выбрасывающий ошибку
     * - Используются разные планировщики для подписки и наблюдения
     * - Проверяется корректное получение ошибки
     */
    @Test
    void testErrorHandlingInConcurrentOperations() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        String errorMessage = "Concurrent error";

        Observable.create(observer -> {
            observer.onNext(1);
            throw new RuntimeException(errorMessage);
        })
        .subscribeOn(new IOThreadScheduler())
        .observeOn(new ComputationScheduler())
        .subscribe(
            item -> fail("Should not receive items after error"),
            error -> {
                receivedError.set(error);
                latch.countDown();
            },
            () -> fail("Should not complete")
        );

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertNotNull(receivedError.get());
        assertEquals(errorMessage, receivedError.get().getMessage());
    }

    /**
     * Тест проверяет комбинацию subscribeOn и observeOn
     * - Создается Observable с разными планировщиками
     * - Проверяется, что подписка и наблюдение происходят в разных потоках
     * - Проверяется, что все потоки отличаются от основного
     */
    @Test
    void testSubscribeOnAndObserveOnCombination() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> subscribeThread = new AtomicReference<>();
        AtomicReference<String> observeThread = new AtomicReference<>();
        String mainThread = Thread.currentThread().getName();

        Observable.create(observer -> {
            subscribeThread.set(Thread.currentThread().getName());
            observer.onNext(1);
            observer.onComplete();
        })
        .subscribeOn(new IOThreadScheduler())
        .observeOn(new ComputationScheduler())
        .subscribe(
            item -> observeThread.set(Thread.currentThread().getName()),
            error -> fail("Unexpected error"),
            () -> latch.countDown()
        );

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertNotEquals(mainThread, subscribeThread.get());
        assertNotEquals(mainThread, observeThread.get());
        assertNotEquals(subscribeThread.get(), observeThread.get());
    }

    /**
     * Тест проверяет отмену подписки при переключении планировщиков
     * - Создается Observable с бесконечной эмиссией
     * - Используются разные планировщики для подписки и наблюдения
     * - Проверяется корректная отмена подписки
     */
    @Test
    void testDisposableWithSchedulerSwitch() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger count = new AtomicInteger(0);
        AtomicBoolean disposed = new AtomicBoolean(false);

        Observable<Integer> source = Observable.create(observer -> {
            while (!disposed.get()) {
                observer.onNext(count.incrementAndGet());
                Thread.sleep(100);
            }
        });

        Disposable subscription = source
            .subscribeOn(new IOThreadScheduler())
            .observeOn(new ComputationScheduler())
            .subscribe(
                item -> {},
                error -> fail("Unexpected error"),
                () -> {}
            );

        Thread.sleep(500);
        subscription.dispose();
        disposed.set(true);
        int finalCount = count.get();
        Thread.sleep(200);
        assertEquals(finalCount, count.get());
    }

    /**
     * Тест проверяет распространение ошибок через планировщики
     * - Создается Observable, выбрасывающий ошибку
     * - Используются разные планировщики и операторы
     * - Проверяется корректное получение ошибки
     */
    @Test
    void testErrorPropagationWithSchedulers() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        String errorMessage = "Scheduler error";

        Observable.create(observer -> {
            observer.onNext(1);
            throw new RuntimeException(errorMessage);
        })
        .subscribeOn(new IOThreadScheduler())
        .map(x -> x * 2)
        .observeOn(new ComputationScheduler())
        .subscribe(
            item -> fail("Should not receive items after error"),
            error -> {
                receivedError.set(error);
                latch.countDown();
            },
            () -> fail("Should not complete")
        );

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertNotNull(receivedError.get());
        assertEquals(errorMessage, receivedError.get().getMessage());
    }
} 