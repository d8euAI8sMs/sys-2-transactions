package org.kalaider.transact.repo;

import org.kalaider.transact.entity.Mood;
import org.kalaider.transact.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonRepository extends JpaRepository<Person, UUID> {

    Optional<Person> findAllByName(String name);

    List<Person> findAllByMood(Mood mood);
}
