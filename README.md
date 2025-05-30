# Отчет по курсовой работе: Реализация RxJava

**1. Введение**

Данная курсовая работа посвящена реализации базовых компонентов библиотеки RxJava на языке Java. Целью работы является изучение и практическое применение принципов реактивного программирования, включая работу с потоками данных, управление потоками выполнения (Schedulers) и обработку событий с использованием паттерна «Наблюдатель» (Observer pattern). В ходе выполнения работы были реализованы основные компоненты RxJava, операторы преобразования данных, механизмы управления потоками выполнения и средства для тестирования корректности работы всех компонентов.

**2. Архитектура и реализация**

Реализация RxJava в рамках данного проекта основывается на следующих ключевых компонентах:

*   **Observer:** Интерфейс `Observer` определяет методы для обработки событий потока: `onNext(T item)` (получение элементов потока), `onError(Throwable t)` (обработка ошибок) и `onComplete()` (оповещение о завершении потока).
*   **Observable:** Класс `Observable` представляет собой поток данных, поддерживающий подписку (`subscribe`) на события от `Observer`. Он отвечает за управление жизненным циклом потока и уведомление подписчиков о событиях.
*   **Schedulers:** Интерфейс `Scheduler` определяет механизм управления потоками выполнения (асинхронное выполнение задач). Реализованы следующие конкретные `Scheduler` для управления потоками:
    *   `IOThreadScheduler`: Использует `CachedThreadPool` для выполнения операций ввода-вывода, позволяя выполнять задачи в отдельных потоках, оптимизируя время выполнения операций, связанных с I/O.
    *   `ComputationScheduler`: Использует `FixedThreadPool` для выполнения вычислений, обеспечивая параллельную обработку данных и повышая производительность операций.
    *   `SingleThreadScheduler`:  Использует один поток для выполнения задач последовательно, гарантируя порядок обработки событий.
*   **Операторы преобразования данных:** Реализованы основные операторы для преобразования потоков данных:
    *   `map(Function mapper)`: Преобразует элементы потока, применяя переданную функцию к каждому элементу.
    *   `filter(Predicate predicate)`: Фильтрует элементы потока, оставляя только те, которые удовлетворяют заданному условию (предикату).
    *   `flatMap(Function<t, observable<r="">> mapper)`: Преобразует элементы в новые `Observable` и объединяет их в один поток, обеспечивая возможность работы с вложенными потоками данных.
*   **Disposable:** Интерфейс `Disposable` обеспечивает механизм отмены подписки на `Observable`, позволяя контролировать жизненный цикл потоков.
*   **Обработка ошибок:** Механизм обработки ошибок реализован посредством вызова метода `onError(Throwable t)` у подписчиков, позволяя обрабатывать исключения, возникающие в процессе работы с потоком.

**3. Реализация операторов и управление потоками выполнения**

*   **Операторы преобразования данных:**  Операторы `map`, `filter` и `flatMap` были реализованы для обработки и преобразования данных в потоке. Каждый оператор принимает на вход функцию или предикат, которые применяются к элементам потока.
*   **Управление потоками выполнения (Schedulers):** Интерфейс `Scheduler` был реализован для управления потоками выполнения. Реализованы три типа `Scheduler`: `IOThreadScheduler`, `ComputationScheduler` и `SingleThreadScheduler`.  Методы `subscribeOn(Scheduler scheduler)` и `observeOn(Scheduler scheduler)` позволяют управлять потоками, в которых выполняются подписка и обработка элементов, соответственно. Это позволяет выполнять ресурсоемкие операции в отдельных потоках, не блокируя основной поток.

**4. Дополнительные операторы и управление подписками**

*   **flatMap(Function<t, observable<r="">> mapper):** Реализация оператора `flatMap` обеспечивает преобразование элементов в новые `Observable` и объединение этих потоков в один, что позволяет эффективно обрабатывать вложенные асинхронные операции.
*   **Disposable:** Интерфейс `Disposable` реализован, чтобы обеспечить механизм отмены подписки на `Observable`, что дает возможность контролировать жизненный цикл потока и освобождать ресурсы, когда они больше не нужны.
*   **Обработка ошибок:** Механизм обработки ошибок реализован, позволяющий перехватывать и обрабатывать исключения, возникающие в процессе работы с потоком, и передавать их подписчикам через метод `onError()`.

**5. Тестирование**

Для проверки корректности работы всех компонентов системы были написаны юнит-тесты. Тестирование проводилось с использованием библиотеки JUnit. Тесты охватывают следующие основные сценарии:

*   **Проверка работы базовых компонентов:** Тестирование `Observer`, `Observable` и статического метода `create()`.
*   **Проверка корректности работы операторов:**  Тестирование операторов `map`, `filter` и `flatMap`, включая различные варианты входных данных и комбинации операторов.
*   **Тестирование многопоточной работы Schedulers:** Тесты, проверяющие корректную работу Schedulers в многопоточной среде, включая проверку правильного распределения задач по потокам и предотвращение гонок данных (data races).
*   **Проверка обработки ошибок:** Тесты, проверяющие корректную обработку ошибок в различных ситуациях, включая передачу ошибок в метод `onError()`.

**6. Примеры использования**

Ниже приведены примеры использования реализованной библиотеки RxJava.

```java
// Пример использования map, filter и IOThreadScheduler
Observable.create(emitter -> {
    emitter.onNext("Hello");
    emitter.onNext("World");
    emitter.onComplete();
})
.subscribeOn(Schedulers.io()) // Выполняем в потоке IO
.map(String::toUpperCase)
.filter(s -> s.length() > 4)
.observeOn(Schedulers.single()) // Обрабатываем в потоке Single
.subscribe(
    item -> System.out.println("Received: " + item),
    error -> System.err.println("Error: " + error.getMessage()),
    () -> System.out.println("Completed")
);

//Пример использования flatMap
Observable.just(1, 2, 3)
    .flatMap(num -> Observable.just(num * 2, num * 2 + 1))
    .subscribe(System.out::println);
