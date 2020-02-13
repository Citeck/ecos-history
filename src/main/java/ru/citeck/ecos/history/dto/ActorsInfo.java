package ru.citeck.ecos.history.dto;

import lombok.Data;

import java.util.List;

@Data
public class ActorsInfo {
    private List<AuthorityDto> actors;
}
