package com.lyy.model.request;

import lombok.Data;

@Data
public class TeamKickOutRequest {
    private Long teamId;
    private Long userId;
}
