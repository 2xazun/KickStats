package com.kickstats.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TeamForm {

    @NotBlank(message = "팀 이름을 입력하세요.")
    @Size(max = 50, message = "팀 이름은 50자 이하로 입력하세요.")
    private String name;

    @Size(max = 50, message = "연고지는 50자 이하로 입력하세요.")
    private String city;

    private Integer homeStadiumId;

    @Min(value = 1800, message = "창단 연도는 1800 이상으로 입력하세요.")
    @Max(value = 2100, message = "창단 연도는 2100 이하로 입력하세요.")
    private Integer foundedYear;
}
