package com.lyy.constants;

/**
 * redisson常量
 *
 * @author lyy
 */
public interface RedissonConstant {
    /**
     * 应用锁
     */
    String APPLY_LOCK = "super:apply:lock:";

    String DISBAND_EXPIRED_TEAM_LOCK = "super:disbandTeam:lock";
    String USER_RECOMMEND_LOCK = "super:user:recommend:lock";
}
