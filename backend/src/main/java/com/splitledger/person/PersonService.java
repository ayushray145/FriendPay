package com.splitledger.person;

import com.splitledger.user.AppUser;
import com.splitledger.user.AppUserRepository;
import com.splitledger.security.ApplicationUserNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonService {

    private final PersonRepository personRepository;
    private final AppUserRepository appUserRepository;

    public PersonService(PersonRepository personRepository, AppUserRepository appUserRepository) {
        this.personRepository = personRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public PersonResponse create(UUID ownerUserId, CreatePersonRequest request) {
        AppUser owner = appUserRepository.findById(ownerUserId)
                .orElseThrow(() -> new ApplicationUserNotFoundException(ownerUserId));
        String phoneNumber = normalizePhoneNumber(request.phoneNumber());
        String displayName = request.displayName() == null || request.displayName().isBlank()
                ? phoneNumber : request.displayName().trim();
        Person person = personRepository.save(new Person(owner, null, displayName, phoneNumber));
        return PersonResponse.from(person);
    }

    @Transactional(readOnly = true)
    public List<PersonResponse> list(UUID ownerUserId) {
        return personRepository.findAllByOwnerIdOrderByDisplayNameAsc(ownerUserId).stream()
                .map(PersonResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PersonResponse get(UUID ownerUserId, UUID personId) {
        Person person = personRepository.findByIdAndOwnerId(personId, ownerUserId)
                .orElseThrow(() -> new PersonNotFoundException(personId));
        return PersonResponse.from(person);
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) return null;
        String trimmed = phoneNumber.trim();
        return trimmed.length() == 10 ? "+91" + trimmed : trimmed;
    }
}
