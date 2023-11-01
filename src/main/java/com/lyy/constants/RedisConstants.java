package com.lyy.constants;

/**
 * Redis常量
 *
 * @author lyy
 */
public interface RedisConstants {
    String LOGIN_USER_KEY = "super:login:token:";

    Long LOGIN_USER_TTL = 15L;
    /**
     * 注册验证码键
     */
    String REGISTER_CODE_KEY = "super:register:";
    /**
     * 注册验证码过期时间
     */
    Long REGISTER_CODE_TTL = 15L;
    /**
     * 用户更新电话键
     */
    String USER_UPDATE_PHONE_KEY = "suer:user:update:phone:";
    /**
     * 用户更新电话过期时间
     */
    Long USER_UPDATE_PHONE_TTL = 15L;
    /**
     * 用户更新邮件键
     */
    String USER_UPDATE_EMAIL_KEY = "suer:user:update:email:";
    /**
     * 用户更新邮件过期时间
     */
    Long USER_UPDATE_EMAIl_TTL = 15L;
    /**
     * 用户忘记密码键
     */
    String USER_FORGET_PASSWORD_KEY = "super:user:forget:";
    /**
     * 用户忘记密码过期时间
     */
    Long USER_FORGET_PASSWORD_TTL = 15L;
    /**
     * 博客推送键
     */
    String BLOG_FEED_KEY = "super:feed:blog:";
    /**
     * 新博文消息键
     */
    String MESSAGE_BLOG_NUM_KEY = "super:message:blog:num:";
    /**
     * 新点赞消息键
     */
    String MESSAGE_LIKE_NUM_KEY = "super:message:like:num:";
    /**
     * 用户推荐缓存
     */
    String USER_RECOMMEND_KEY = "super:recommend:";
}
