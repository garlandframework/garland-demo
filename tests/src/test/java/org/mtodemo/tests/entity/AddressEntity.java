package org.mtodemo.tests.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressEntity {

    @Id
    private UUID id;

    private String street;
    private String city;
    private String country;

    @Column(name = "zip_code")
    private String zipCode;
}
