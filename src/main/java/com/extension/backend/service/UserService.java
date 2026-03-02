package com.extension.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.extension.backend.entity.User;
import com.extension.backend.exception.BusinessException;
import com.extension.backend.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * 用户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 根据 Token 查找用户
     */
    public User findByToken(String token) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("token", token);
        return userMapper.selectOne(wrapper);
    }

    /**
     * 根据邮箱查找用户
     */
    public User findByEmail(String email) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", email);
        return userMapper.selectOne(wrapper);
    }

    /**
     * 注册用户
     */
    public User register(String email, String password) {
        // 检查邮箱是否已存在
        if (findByEmail(email) != null) {
            throw BusinessException.badRequest("该邮箱已注册");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setToken(generateToken());
        user.setIsActive(true);
        user.setDailyQuota(30);
        user.setRequestsToday(0);
        user.setCreatedAt(LocalDateTime.now());

        userMapper.insert(user);
        return user;
    }

    /**
     * 登录验证
     */
    public User login(String email, String password) {
        User user = findByEmail(email);
        if (user == null) {
            throw BusinessException.badRequest("用户不存在");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw BusinessException.badRequest("密码错误");
        }

        if (!user.getIsActive()) {
            throw BusinessException.forbidden("账号已被禁用");
        }

        return user;
    }

    /**
     * 更新用户
     */
    public void updateUser(User user) {
        userMapper.updateById(user);
    }

    /**
     * 生成随机 Token
     */
    private String generateToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 重置密码
     */
    public void resetPassword(String email, String newPassword) {
        User user = findByEmail(email);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }
}
