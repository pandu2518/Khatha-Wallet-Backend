package com.khathabook.controller;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.*;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.auth.http.HttpCredentialsAdapter; 
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/contacts")
@CrossOrigin(origins = "*")
public class GoogleContactsController {

    @PostMapping("/google")
    public ResponseEntity<?> getGoogleContacts(@RequestBody Map<String, String> body) {
        try {
            String accessToken = body.get("accessToken");

            if (accessToken == null) {
                return ResponseEntity.badRequest().body("Access token missing");
            }

            GoogleCredentials credentials =
                    GoogleCredentials.create(new AccessToken(accessToken, null));

            HttpRequestInitializer requestInitializer =
                    new HttpCredentialsAdapter(credentials);

            PeopleService peopleService = new PeopleService.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    requestInitializer
            ).setApplicationName("Khatha Wallet").build();

            Map<String, Map<String, String>> uniqueContacts = new LinkedHashMap<>();

            // 1. Fetch Connections
            try {
                ListConnectionsResponse connectionsResponse = peopleService.people().connections()
                        .list("people/me")
                        .setPageSize(1000)
                        .setPersonFields("names,emailAddresses,phoneNumbers")
                        .execute();
                processPersons(connectionsResponse.getConnections(), uniqueContacts);
            } catch (Exception e) {
                System.err.println("Error fetching connections: " + e.getMessage());
            }

            // 2. Fetch Other Contacts
            try {
                ListOtherContactsResponse otherResponse = peopleService.otherContacts()
                        .list()
                        .setPageSize(1000)
                        .setReadMask("names,emailAddresses,phoneNumbers")
                        .execute();
                processPersons(otherResponse.getOtherContacts(), uniqueContacts);
            } catch (Exception e) {
                System.err.println("Error fetching other contacts: " + e.getMessage());
            }

            return ResponseEntity.ok(new ArrayList<>(uniqueContacts.values()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to fetch contacts: " + e.getMessage());
        }
    }

    private void processPersons(List<Person> persons, Map<String, Map<String, String>> uniqueContacts) {
        if (persons == null) return;

        for (Person person : persons) {
            String name = "";
            String email = "";
            String phone = "";

            if (person.getNames() != null && !person.getNames().isEmpty()) {
                name = person.getNames().get(0).getDisplayName();
            }

            if (person.getEmailAddresses() != null && !person.getEmailAddresses().isEmpty()) {
                email = person.getEmailAddresses().get(0).getValue();
            }

            if (person.getPhoneNumbers() != null && !person.getPhoneNumbers().isEmpty()) {
                phone = person.getPhoneNumbers().get(0).getValue();
            }

            if (!name.isEmpty() || !email.isEmpty() || !phone.isEmpty()) {
                String key = !email.isEmpty() ? email : (!phone.isEmpty() ? phone : name);
                
                if (!uniqueContacts.containsKey(key)) {
                    Map<String, String> contact = new HashMap<>();
                    contact.put("name", name.isEmpty() ? "No Name" : name);
                    contact.put("email", email);
                    contact.put("phone", phone);
                    uniqueContacts.put(key, contact);
                }
            }
        }
    }
}
