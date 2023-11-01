package com.lyy.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lyy.common.BaseResponse;
import com.lyy.model.domain.User;
import com.lyy.model.request.UserUpdateRequest;
import com.lyy.model.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author lyy
 * @description 针对表【user】的数据库操作Service
 * @createDate 2023-05-07 19:56:01
 */
public interface UserService extends IService<User> {
    long userRegister(String phone, String code, String userAccount, String userPassword, String checkPassword);

    String userLogin(String userAccount, String userPassword, HttpServletRequest request);

    User getSafetyUser(User originUser);

    int userLogout(HttpServletRequest request);

    Page<User> searchUsersByTags(List<String> tagNameList, long currentPage);

    boolean isAdmin(User loginUser);

    boolean updateUser(User user, HttpServletRequest request);

    Page<UserVO> userPage(long currentPage);

    User getLoginUser(HttpServletRequest request);

//    List<User> matchUsers(long num, User user);

    Boolean isLogin(HttpServletRequest request);

    Page<UserVO> matchUser(long currentPage, User loginUser);

    UserVO getUserById(Long userId, Long loginUserId);

    List<String> getUserTags(Long id);

    void updateTags(List<String> tags, Long userId);

    void updateUserWithCode(UserUpdateRequest updateRequest, Long userId);

    Page<UserVO> getRandomUser();

    void updatePassword(String phone, String code, String password, String confirmPassword);

    Page<UserVO> preMatchUser(long currentPage, String username, User loginUser);

    String uploadFileAvatar(MultipartFile file, HttpServletRequest request);
}
