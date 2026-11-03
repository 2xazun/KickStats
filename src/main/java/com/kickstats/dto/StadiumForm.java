package com.kickstats.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StadiumForm {

    @NotBlank(message = "경기장 이름을 입력하세요.")
    @Size(max = 100, message = "경기장 이름은 100자 이하로 입력하세요.")
    private String name;

    @Size(max = 50, message = "도시는 50자 이하로 입력하세요.")
    private String city;

    @Min(value = 0, message = "수용 인원은 0 이상으로 입력하세요.")
    private Integer capacity;
}
