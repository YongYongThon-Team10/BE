package com.yongyongthon.be.event.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * E19, E20 응답. 담기는 favorited=true, 해제는 false.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteResponseDto {
    private String eventId;
    private Boolean favorited;
}
