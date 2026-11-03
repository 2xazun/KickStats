package com.kickstats.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LeagueForm {

    @NotBlank(message = "리그 이름을 입력하세요.")
    @Size(max = 50, message = "리그 이름은 50자 이하로 입력하세요.")
    private String name;

    @NotBlank(message = "국가를 입력하세요.")
    @Size(max = 50, message = "국가는 50자 이하로 입력하세요.")
    private String country;
}
