package com.splitledger.person;

import static org.assertj.core.api.Assertions.assertThat;

import com.splitledger.user.AppUser;
import com.splitledger.user.AppUserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PersonRepositoryTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PersonRepository personRepository;

    @Test
    void personQueriesOnlyReturnContactsOwnedByRequestedUser() {
        AppUser firstOwner = appUserRepository.saveAndFlush(
                new AppUser("google", "subject-one", "one@example.com", "First Owner"));
        AppUser secondOwner = appUserRepository.saveAndFlush(
                new AppUser("google", "subject-two", "two@example.com", "Second Owner"));
        Person firstContact = personRepository.saveAndFlush(new Person(firstOwner, null, "Rahul"));
        personRepository.saveAndFlush(new Person(secondOwner, null, "Rahul"));

        assertThat(personRepository.findAllByOwnerIdOrderByDisplayNameAsc(firstOwner.getId()))
                .extracting(Person::getDisplayName)
                .containsExactly("Rahul");
        assertThat(personRepository.findByIdAndOwnerId(firstContact.getId(), secondOwner.getId()))
                .isEmpty();
    }

    @Test
    void ownerScopedLookupReturnsEmptyForUnknownContact() {
        UUID ownerId = UUID.randomUUID();

        assertThat(personRepository.findByIdAndOwnerId(UUID.randomUUID(), ownerId)).isEmpty();
    }
}
