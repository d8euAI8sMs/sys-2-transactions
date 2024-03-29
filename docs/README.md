# sys-2-transactions @ d8euAI8sMs

## Запуск

```
java -jar sys-2-transact-xxx.jar --spring.profiles.active=debit-1
java -jar sys-2-transact-xxx.jar --spring.profiles.active=debit-2
```

## checher & affector

Демонстрация эффектов, связанных с разными уровнями изоляции транзакций. Первое приложение запускает транзакцию, в которой наблюдает эффекты, второе -- мелкие мешающие транзакции, создающие эти эффекты. Набор эффектов можно контролировать из `.properties`-файлов.

## debit-1 & debit-2

Практическое применение различных уровней изоляции на примере простого финансового приложения. Группа клиентов обменивается средствами случайным образом. При запуске двух экземпляров одновременно эти группы могут начать конфликтовать.

Для наблюдения за эффектами неконсистентности хранилища и их устранения применены оптимистические блокировки.

Используемая база данных применяет MVCC (Multiple Version Concurrency Control) по умолчанию, так что даже SERIALIZABLE-транзакции запускаются параллельно. В отсутствие оптимистических блокировок новая версия перетирает старую, что приводит к неконсистентости.

При отключении MVCC две SERIALIZABLE-транзакции уже не могут запуститься параллельно, что может приводить к DEALDLOCK'ам в некоторых ситуациях: две транзакции получают SHARED-блокировку на одну таблицу, ни одна из них более не сможет получить EXCLUSIVE-блокировку. COUNT-query не может сразу захватить EXCLUSIVE-блокировку (оно и понятно -- иначе заблокируется сразу вся таблица, а не отдельные ее регионы), потому предусмотрен специальный флаг, позволяющий/запрещающий использование COUNT для демонстрации соответствующих эффектов.

**NOTE:** при MVCC=false закрытие приложения может приводить к порче хранилища.
