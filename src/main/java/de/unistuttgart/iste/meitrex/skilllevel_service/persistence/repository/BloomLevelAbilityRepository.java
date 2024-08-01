package de.unistuttgart.iste.meitrex.skilllevel_service.persistence.repository;

import de.unistuttgart.iste.meitrex.generated.dto.BloomLevel;
import de.unistuttgart.iste.meitrex.skilllevel_service.persistence.entity.BloomLevelAbilityEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@Profile("!test")
public interface BloomLevelAbilityRepository extends JpaRepository<BloomLevelAbilityEntity, UUID> {
    BloomLevelAbilityEntity findByUserIdAndBloomLevel(UUID userId, BloomLevel bloomLevel);
}
