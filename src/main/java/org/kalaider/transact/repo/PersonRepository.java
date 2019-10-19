package org.kalaider.transact.repo;

import org.kalaider.transact.entity.Mood;
import org.kalaider.transact.entity.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonRepository extends JpaRepository<Person, UUID> {

    Optional<Person> findAllByName(String name);

    List<Person> findAllByMood(Mood mood);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Page<Person> findAll(Pageable p);

    @Query("select SUM(p.balance) from Person p")
    long totalBalance();
}
