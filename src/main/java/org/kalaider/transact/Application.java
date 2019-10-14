package org.kalaider.transact;

import lombok.extern.slf4j.Slf4j;
import org.kalaider.transact.config.AppProperties;
import org.kalaider.transact.entity.Mood;
import org.kalaider.transact.entity.Person;
import org.kalaider.transact.repo.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@SpringBootApplication(exclude = CacheAutoConfiguration.class)
@EnableConfigurationProperties(AppProperties.class)
@Slf4j
public class Application {

    private static final String[] names = {
            "Jane",
            "Joe",
            "Juliet",
            "James",
            "Jack",
            "John",
            "Jason"
    };

    @Autowired
    private AppProperties properties;
    @Autowired
    private TransactionTemplate transaction;
    @Autowired
    private PersonRepository repository;
    @Autowired
    private EntityManager entityManager;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    @Profile("checker")
    public CommandLineRunner checkerRunner() {
        return args -> {
            log.info("Hello from checker!");

            transaction.setIsolationLevel(chooseIsolation(properties.getIsolation()));
            transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

            // one big transaction to observe inter-transactional effects
            transaction.execute(t -> {
                Map<UUID, Person> old = null, cur;

                while (!Thread.currentThread().isInterrupted()) {
                    cur = repository.findAll().stream()
                                    .collect(Collectors.toMap(Person::getId, Function.identity()));

                    if (old != null) {
                        if (!checkForDirtyReads(old, cur)) {
                            log.warn("Dirty or non-repeatable reads detected");
                        }
                        if (!checkForPhantomReads(old, cur)) {
                            log.warn("Phantom reads detected");
                        }
                    }

                    old = cur;

                    if (!delay(Duration.ofSeconds(1))) return null;

                    // clear cache to ensure fresh data will be loaded next
                    entityManager.clear();
                }

                return null;
            });
        };
    }

    @Bean
    @Profile("affector")
    public CommandLineRunner affectorRunner() {
        return args -> {
            log.info("Hello from affector!");

            transaction.setIsolationLevel(chooseIsolation(properties.getIsolation()));
            transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

            // multiple small transactions
            //   either long-running with rollback (to allow for dirty reads observations)
            //   or short-running with commit (to demonstrate non-repeatable reads)
            while (!Thread.currentThread().isInterrupted()) {
                transaction.execute(t -> {
                    final ThreadLocalRandom r = ThreadLocalRandom.current();

                    List<Person> all = repository.findAll();

                    // alter all values in the table and flush immediately
                    all.forEach(p -> {
                        p.setMood(Mood.values()[r.nextInt(0, Mood.values().length)]);
                        p.setName(names[r.nextInt(0, names.length)]);
                        repository.saveAndFlush(p);
                    });

                    // add or remove some elements
                    final double baseProb = 1e-1;
                    if (r.nextDouble() < baseProb) {
                        Person p = Person.builder()
                                .mood(Mood.AWESOME)
                                .name("New")
                                .build();
                        repository.saveAndFlush(p);
                    }
                    final int desiredElements = 100;
                    final double prob = baseProb * Math.min(1, (double) all.size() / desiredElements);
                    if (r.nextDouble() < prob && all.size() > 0) {
                        repository.delete(all.get(r.nextInt(0, all.size())));
                        repository.flush();
                    }

                    // commit or rollback (see above)
                    if (properties.getDesiredEffects() == AppProperties.Effects.DIRTY_READS) {
                        if (!delay(Duration.ofSeconds(1))) return null;
                        t.setRollbackOnly();
                    }

                    return null;
                });

                if (properties.getDesiredEffects() == AppProperties.Effects.REPEATABLE_READS) {
                    if (!delay(Duration.ofSeconds(1))) return;
                }
            }
        };
    }

    private boolean checkForDirtyReads(Map<UUID, Person> p1, Map<UUID, Person> p2) {
        return p1.values().stream().allMatch(p ->
            Optional.ofNullable(p2.get(p.getId()))
                    .map(p0 -> p.getName().equals(p0.getName()) && p.getMood().equals(p0.getMood()))
                    // not interested in phantom-related effects
                    .orElse(true)
        );
    }

    private boolean checkForPhantomReads(Map<UUID, Person> p1, Map<UUID, Person> p2) {
        return p1.keySet().equals(p2.keySet());
    }

    private int chooseIsolation(AppProperties.Isolation isolation) {
        switch (isolation) {
            case SERIALIZABLE: return TransactionDefinition.ISOLATION_SERIALIZABLE;
            case READ_COMMITTED: return TransactionDefinition.ISOLATION_READ_COMMITTED;
            case READ_UNCOMMITTED: return TransactionDefinition.ISOLATION_READ_UNCOMMITTED;
            default: throw new Error("Unexpected enum value: " + isolation);
        }
    }

    private boolean delay(Duration d) {
        try {
            Thread.sleep(d.toMillis());
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }
}
