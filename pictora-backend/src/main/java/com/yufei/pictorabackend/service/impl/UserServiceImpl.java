package com.yufei.pictorabackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yufei.pictorabackend.constant.UserConstant;
import com.yufei.pictorabackend.exception.BusinessException;
import com.yufei.pictorabackend.exception.ErrorCode;
import com.yufei.pictorabackend.exception.ThrowUtils;
import com.yufei.pictorabackend.model.dto.user.UserLoginRequest;
import com.yufei.pictorabackend.model.dto.user.UserQueryRequest;
import com.yufei.pictorabackend.model.dto.user.UserRegisterRequest;
import com.yufei.pictorabackend.model.entity.User;
import com.yufei.pictorabackend.model.enums.UserRoleEnum;
import com.yufei.pictorabackend.model.vo.LoginUserVO;
import com.yufei.pictorabackend.model.vo.UserVO;
import com.yufei.pictorabackend.service.UserService;
import com.yufei.pictorabackend.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.yufei.pictorabackend.constant.UserConstant.ADMIN_ROLE;
import static com.yufei.pictorabackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author LYF
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-04-22 09:04:52
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    /**
     * 用户注册
     * @param userRegisterRequest
     * @return
     */
    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        // 1. 校验
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if (CharSequenceUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名不能小于 4 位");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能小于 8 位");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码输入不一致");
        }
        // 2. 查询数据库
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        Long count = this.baseMapper.selectCount(queryWrapper);

        // 2.1 该账号已被注册，操作失败
        ThrowUtils.throwIf(count > 0, ErrorCode.OPERATION_ERROR, "该用户名已存在");

        // 3. 写入数据库
        User user = new User();
        user.setUserAccount(userAccount);
        // 3.1 用户密码加密
        String encryptPassword = getEncryptPassword(userPassword);
        user.setUserPassword(encryptPassword);
        user.setUserName(UserConstant.DEFAULT_NO_NICKNAME);
        user.setUserRole(UserRoleEnum.USER.getValue());
        return baseMapper.insert(user);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @Override
    public LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
        // 1. 校验
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);

        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名或密码错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名或密码错误");
        }
        // 2. 查数据库
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        String encryptPassword = getEncryptPassword(userPassword);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = baseMapper.selectOne(queryWrapper);
        if (BeanUtil.isEmpty(user)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户登录态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        return getLoginUserVO(user);
    }

    /**
     * 用户退出登录
     *
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    /**
     * 获取脱敏后的用户信息
     *
     * @param user
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 根据 id 获取用户包装类
     *
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 获取脱敏后的用户列表
     *
     * @param userList
     * @return
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }

        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取当前登录用户信息
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        ThrowUtils.throwIf(userObj == null, ErrorCode.NOT_LOGIN_ERROR);
        User loginUser = (User) userObj;
        loginUser = this.baseMapper.selectById(loginUser.getId());

        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        return loginUser;
    }

    /**
     * 获取用户查询条件
     *
     * @param userQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");

        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjectUtil.isNotNull(id), "id", id);
        queryWrapper.eq(CharSequenceUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(CharSequenceUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(CharSequenceUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(CharSequenceUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(CharSequenceUtil.isNotEmpty(sortField), "ascend".equals(sortField), sortField);

        return queryWrapper;
    }

    /**
     * 密码加密
     *
     * @param password 密码
     * @return 加密后的密码
     */
    @Override
    public String getEncryptPassword(String password) {
        final String SALT = "yufei";
        return DigestUtils.md5DigestAsHex((SALT + password).getBytes());
    }

    /**
     * 判断是否为管理员
     *
     * @param user
     * @return
     */
    @Override
    public boolean isAdmin(User user) {
        return user != null && user.getUserRole().equals(ADMIN_ROLE);
    }
}




