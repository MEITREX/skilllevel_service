package de.unistuttgart.iste.gits.skilllevel_service.persistence.mapper;

import de.unistuttgart.iste.gits.generated.dto.SkillLevels;
import de.unistuttgart.iste.gits.skilllevel_service.persistence.entity.AllSkillLevelsEntity;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SkillLevelMapper {

    private final ModelMapper modelMapper;

    public SkillLevels entityToDto(AllSkillLevelsEntity templateEntity) {
        return modelMapper.map(templateEntity, SkillLevels.class);
    }

}
