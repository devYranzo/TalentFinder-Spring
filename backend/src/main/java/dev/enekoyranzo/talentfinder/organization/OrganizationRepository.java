package dev.enekoyranzo.talentfinder.organization;

import org.springframework.data.jpa.repository.JpaRepository;

interface OrganizationRepository extends JpaRepository<Organization, Long> {
    boolean existsByNameIgnoreCase(String name);
}
