package ru.citeck.ecos.history.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorityDto {
    private String authorityName;
    private String userName;
}
