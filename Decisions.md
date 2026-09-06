1. How did you handle the concurrency race condition?

The concurrency race condition was handled using database-level pessimistic locking.

The wallet row is locked using JPA's `PESSIMISTIC_WRITE` lock before checking and updating the balance. This ensures that when multiple debit requests for the same wallet arrive at the same time, only one request can modify the wallet balance at a time.

For example, if a wallet has a balance of ₹500 and ten concurrent requests of ₹100 arrive, the requests are serialized at the database level. The first five requests successfully debit ₹100 each, reducing the balance to ₹0. The remaining five requests see the updated balance and fail due to insufficient funds.

This prevents multiple requests from reading the same old balance and accidentally allowing the wallet to become negative.

The transaction processing is also wrapped in a database transaction using `@Transactional`, so the balance update and transaction record are handled atomically.

2. Where did your AI assistant give you an incorrect or sub-optimal suggestion?

An initial AI-generated implementation suggested checking the transaction ID and then updating the wallet without making the duplicate check and balance update sufficiently dependent on database-level concurrency control.

That approach could be unsafe under concurrent requests because two requests could potentially check for the same transaction before either request had committed its transaction record.

I improved the implementation by using database-level locking on the wallet with `PESSIMISTIC_WRITE` and a unique database constraint on `transactionId`.

The unique constraint provides an additional database-level guarantee that the same transaction ID cannot be stored more than once.

Another sub-optimal aspect of the initial implementation was relying too heavily on application-level synchronization. Application-level locks such as Java `synchronized` are not reliable for a distributed application because multiple application instances would have separate JVM locks.

Therefore, the final implementation relies on database transactions and database-level locking, which is more appropriate for a transaction ledger service.
