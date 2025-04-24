package com.yufei.pictorabackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yufei.pictorabackend.model.dto.user.UserLoginRequest;
import com.yufei.pictorabackend.model.dto.user.UserQueryRequest;
import com.yufei.pictorabackend.model.dto.user.UserRegisterRequest;
import com.yufei.pictorabackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yufei.pictorabackend.model.vo.LoginUserVO;
import com.yufei.pictorabackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author LYF
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-04-22 09:04:52
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param userRegisterRequest
     * @return
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

    /**
     * 用户退出登录
     * @param request
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏后的用户信息
     *
     * @param user
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 根据 id 获取用户包装类
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后的用户列表
     *
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取用户查询条件
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
    /**
     * 对密码加密加盐
     *
     * @param userPassword 用户初始密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 获取当前登录用户信息
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 判断是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);
}
