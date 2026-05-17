package com.skilize.expectation.domain;

import com.skilize.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_expectations")
@Getter
@NoArgsConstructor
public class UserExpectation {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "tl_expectation")
    private String tlExpectation;

    @Column(name = "company_expectation")
    private String companyExpectation;

    @Column(name = "tl_updated_at")
    private OffsetDateTime tlUpdatedAt;

    @Column(name = "company_updated_at")
    private OffsetDateTime companyUpdatedAt;

    public static UserExpectation create(User user) {
        UserExpectation e = new UserExpectation();
        e.user = user;
        return e;
    }

    public void updateTlExpectation(String expectation) {
        this.tlExpectation = expectation;
        this.tlUpdatedAt = OffsetDateTime.now();
    }

    public void updateCompanyExpectation(String expectation) {
        this.companyExpectation = expectation;
        this.companyUpdatedAt = OffsetDateTime.now();
    }
}
