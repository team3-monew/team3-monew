package com.monew.server.interest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record InterestRegisterRequest(

        @NotBlank(message = "관심사 이름은 필수입니다.")
        @Size(min = 1, max = 50, message = "관심사 이름은 1자 이상 50자 이하여야 합니다.")
        String name,

        @NotEmpty(message = "키워드는 1개 이상이어야 합니다.")
        @Size(min = 1, max = 10, message = "키워드는 1개 이상 10개 이하여야 합니다.")
        List<@NotBlank(message = "키워드는 공백일 수 없습니다.")
             @Size(max = 50, message = "키워드는 50자 이하여야 합니다.")
                String> keywords

) {
}
