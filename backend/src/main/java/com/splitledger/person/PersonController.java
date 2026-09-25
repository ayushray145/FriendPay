package com.splitledger.person;

import com.splitledger.security.ApplicationOidcUser;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/people")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @PostMapping
    public ResponseEntity<PersonResponse> create(
            @Valid @RequestBody CreatePersonRequest request,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        PersonResponse person = personService.create(user.getApplicationUserId(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{personId}")
                .buildAndExpand(person.id())
                .toUri();
        return ResponseEntity.created(location).body(person);
    }

    @GetMapping
    public List<PersonResponse> list(@AuthenticationPrincipal ApplicationOidcUser user) {
        return personService.list(user.getApplicationUserId());
    }

    @GetMapping("/{personId}")
    public PersonResponse get(
            @PathVariable UUID personId,
            @AuthenticationPrincipal ApplicationOidcUser user) {
        return personService.get(user.getApplicationUserId(), personId);
    }
}
