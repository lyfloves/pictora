package com.yufei.pictorabackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求封装类
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = -4871046724662905982L;

    private String userAccount;

    private String userPassword;

}
