package com.ll;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class Article {
    private long id;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private String title;
    private String body;

    // 컬럼명이 isBlind 이므로, Lombok 이 만드는 setBlind 대신 필드에 직접 매핑한다.
    @JsonProperty("isBlind")
    private boolean isBlind;
}
