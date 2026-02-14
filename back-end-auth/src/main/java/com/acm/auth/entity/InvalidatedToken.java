package com.acm.auth.entity;

import java.util.Date;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "invalidated_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvalidatedToken {

    @Id
    @Column(name = "id")
    String id;

    @Column(name = "expiry_time", nullable = false)
    Date expiryTime;
}
