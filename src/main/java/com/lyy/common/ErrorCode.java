package com.lyy.common;

/**
 * 错误代码
 *
 * @author lyy
 */
public enum ErrorCode {
    /**
     * 成功
     */
    SUCCESS(0, "ok", ""),
    /**
     * 参数错误
     */
    PARAMS_ERROR(40000, "请求参数错误", ""),
    /**
     * 零错误
     */
    NULL_ERROR(40001, "请求数据为空", ""),
    /**
     * 不登录
     */
    NOT_LOGIN(40100, "未登录", ""),
    /**
     * 没有身份验证
     */
    NO_AUTH(40101, "无权限", ""),
    /**
     * 被禁止
     */
    FORBIDDEN(40301, "禁止操作", ""),
    /**
     * 系统错误
     */
    SYSTEM_ERROR(50000, "系统内部异常", "");

    /**
     * 代码
     */
    private final int code;

    /**
     * 消息
     * 状态码信息
     */
    private final String message;

    /**
     * 描述
     * 状态码描述（详情）
     */
    private final String description;

    /**
     * 错误代码
     *
     * @param code        代码
     * @param message     消息
     * @param description 描述
     */
    ErrorCode(int code, String message, String description) {
        this.code = code;
        this.message = message;
        this.description = description;
    }

    /**
     * 获取代码
     *
     * @return int
     */
    public int getCode() {
        return code;
    }

    /**
     * 得到消息
     *
     * @return {@link String}
     */
    public String getMessage() {
        return message;
    }

    /**
     * 得到描述
     *
     * @return {@link String}
     */
    public String getDescription() {
        return description;
    }

}
