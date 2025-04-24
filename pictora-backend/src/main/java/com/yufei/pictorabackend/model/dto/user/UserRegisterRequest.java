package com.yufei.pictorabackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求封装类
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 1427296400335225661L;

    private String userAccount;

    private String userPassword;

    private String checkPassword;

}
