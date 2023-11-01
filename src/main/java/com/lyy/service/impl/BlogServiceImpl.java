package com.lyy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lyy.common.ErrorCode;
import com.lyy.constants.RedisConstants;
import com.lyy.mapper.BlogMapper;
import com.lyy.model.domain.*;
import com.lyy.model.request.BlogAddRequest;
import com.lyy.model.request.BlogUpdateRequest;
import com.lyy.model.vo.BlogVO;
import com.lyy.service.*;
import com.lyy.utils.FileUploadUtil;
import com.lyy.utils.FileUtils;
import com.lyy.exception.BusinessException;
import com.lyy.model.domain.*;
import com.lyy.model.enums.MessageTypeEnum;
import com.lyy.model.vo.UserVO;
import com.lyy.service.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.lyy.constants.SystemConstants.PAGE_SIZE;
import static com.lyy.constants.SystemConstants.QiNiuUrl;

/**
 * @author lyy
 * @description 针对表【blog】的数据库操作Service实现
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog>
        implements BlogService {

    @Resource
    private BlogLikeService blogLikeService;

    @Resource
    private UserService userService;

    @Resource
    private FollowService followService;

    @Resource
    private MessageService messageService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 添加博客
     * @param blogAddRequest
     * @param loginUser
     * @return
     */
    @Override
    public Long addBlog(BlogAddRequest blogAddRequest, User loginUser) {
        Blog blog = new Blog();
        ArrayList<String> imageNameList = new ArrayList<>();
        try {
            MultipartFile[] images = blogAddRequest.getImages();
            if (images != null) {
                for (MultipartFile image : images) {
                    String filename = FileUploadUtil.uploadFileAvatar(image);
                    imageNameList.add(filename);
                }
                String imageStr = StringUtils.join(imageNameList, ",");
                blog.setImages(imageStr);
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, e.getMessage());
        }
        blog.setUserId(loginUser.getId());
        blog.setTitle(blogAddRequest.getTitle());
        blog.setContent(blogAddRequest.getContent());
        boolean saved = this.save(blog);
        if (saved) {
            List<UserVO> userVOList = followService.listFans(loginUser.getId());
            if (!userVOList.isEmpty()) {
                for (UserVO userVO : userVOList) {
                    String key = RedisConstants.BLOG_FEED_KEY + userVO.getId();
                    stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());
                    String likeNumKey = RedisConstants.MESSAGE_BLOG_NUM_KEY + userVO.getId();
                    Boolean hasKey = stringRedisTemplate.hasKey(likeNumKey);
                    if (Boolean.TRUE.equals(hasKey)) {
                        stringRedisTemplate.opsForValue().increment(likeNumKey);
                    } else {
                        stringRedisTemplate.opsForValue().set(likeNumKey, "1");
                    }
                }
            }
        }

        return blog.getId();
    }

    /**
     * 获取我写的博文
     * @param currentPage
     * @param id
     * @return
     */
    @Override
    public Page<BlogVO> listMyBlogs(long currentPage, Long id) {
        if (currentPage <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LambdaQueryWrapper<Blog> blogLambdaQueryWrapper = new LambdaQueryWrapper<>();
        blogLambdaQueryWrapper.eq(Blog::getUserId, id);
        Page<Blog> blogPage = this.page(new Page<>(currentPage, PAGE_SIZE), blogLambdaQueryWrapper);
        Page<BlogVO> blogVoPage = new Page<>();
        BeanUtils.copyProperties(blogPage, blogVoPage);
        List<BlogVO> blogVOList = blogPage.getRecords().stream().map((blog) -> {
            BlogVO blogVO = new BlogVO();
            BeanUtils.copyProperties(blog, blogVO);
            return blogVO;
        }).collect(Collectors.toList());
        for (BlogVO blogVO : blogVOList) {
            String images = blogVO.getImages();
            if (images == null) {
                continue;
            }
            String[] imgStr = images.split(",");
            blogVO.setCoverImage(imgStr[0]);
        }
        blogVoPage.setRecords(blogVOList);
        return blogVoPage;
    }

    /**
     * 点赞博文
     * @param blogId
     * @param userId
     */
    @Override
    public void likeBlog(long blogId, Long userId) {
        Blog blog = this.getById(blogId);
        if (blog == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "博文不存在");
        }
        LambdaQueryWrapper<BlogLike> blogLikeLambdaQueryWrapper = new LambdaQueryWrapper<>();
        blogLikeLambdaQueryWrapper.eq(BlogLike::getBlogId, blogId);
        blogLikeLambdaQueryWrapper.eq(BlogLike::getUserId, userId);
        long isLike = blogLikeService.count(blogLikeLambdaQueryWrapper);
        if (isLike > 0) {
            blogLikeService.remove(blogLikeLambdaQueryWrapper);
            int newNum = blog.getLikedNum() - 1;
            this.update().eq("id", blogId).set("liked_num", newNum).update();
        } else {
            BlogLike blogLike = new BlogLike();
            blogLike.setBlogId(blogId);
            blogLike.setUserId(userId);
            blogLikeService.save(blogLike);
            int newNum = blog.getLikedNum() + 1;
            this.update().eq("id", blogId).set("liked_num", newNum).update();
            Message message = new Message();
            message.setType(MessageTypeEnum.BLOG_LIKE.getValue());
            message.setFromId(userId);
            message.setToId(blog.getUserId());
            message.setData(String.valueOf(blog.getId()));
            messageService.save(message);
            String likeNumKey = RedisConstants.MESSAGE_LIKE_NUM_KEY + blog.getUserId();
            Boolean hasKey = stringRedisTemplate.hasKey(likeNumKey);
            if (Boolean.TRUE.equals(hasKey)) {
                stringRedisTemplate.opsForValue().increment(likeNumKey);
            } else {
                stringRedisTemplate.opsForValue().set(likeNumKey, "1");
            }
        }
    }

    /**
     * 分页查询博客列表
     * @param currentPage
     * @param title
     * @param userId
     * @return
     */
    @Override
    public Page<BlogVO> pageBlog(long currentPage, String title, Long userId) {
        LambdaQueryWrapper<Blog> blogLambdaQueryWrapper = new LambdaQueryWrapper<>();
        blogLambdaQueryWrapper.like(StringUtils.isNotBlank(title), Blog::getTitle, title);
        blogLambdaQueryWrapper.orderBy(true, false, Blog::getCreateTime);
        Page<Blog> blogPage = this.page(new Page<>(currentPage, PAGE_SIZE), blogLambdaQueryWrapper);
        Page<BlogVO> blogVoPage = new Page<>();
        BeanUtils.copyProperties(blogPage, blogVoPage);
        List<BlogVO> blogVOList = blogPage.getRecords().stream().map((blog) -> {
            BlogVO blogVO = new BlogVO();
            BeanUtils.copyProperties(blog, blogVO);
            if (userId != null) {
                LambdaQueryWrapper<BlogLike> blogLikeLambdaQueryWrapper = new LambdaQueryWrapper<>();
                blogLikeLambdaQueryWrapper.eq(BlogLike::getBlogId, blog.getId()).eq(BlogLike::getUserId, userId);
                long count = blogLikeService.count(blogLikeLambdaQueryWrapper);
                blogVO.setIsLike(count > 0);
            }
            return blogVO;
        }).collect(Collectors.toList());
        for (BlogVO blogVO : blogVOList) {
            String images = blogVO.getImages();
            if (images == null) {
                continue;
            }
            String[] imgStrs = images.split(",");
            blogVO.setCoverImage(imgStrs[0]);
        }
        blogVoPage.setRecords(blogVOList);
        return blogVoPage;
    }

    /**
     * 通过id获取博客
     * @param blogId
     * @param userId
     * @return
     */
    @Override
    public BlogVO getBlogById(long blogId, Long userId) {
        Blog blog = this.getById(blogId);
        BlogVO blogVO = new BlogVO();
        BeanUtils.copyProperties(blog, blogVO);
        LambdaQueryWrapper<BlogLike> blogLikeLambdaQueryWrapper = new LambdaQueryWrapper<>();
        blogLikeLambdaQueryWrapper.eq(BlogLike::getUserId, userId);
        blogLikeLambdaQueryWrapper.eq(BlogLike::getBlogId, blogId);
        long isLike = blogLikeService.count(blogLikeLambdaQueryWrapper);
        blogVO.setIsLike(isLike > 0);
        User author = userService.getById(blog.getUserId());
        UserVO authorVO = new UserVO();
        BeanUtils.copyProperties(author, authorVO);
        LambdaQueryWrapper<Follow> followLambdaQueryWrapper = new LambdaQueryWrapper<>();
        followLambdaQueryWrapper.eq(Follow::getFollowUserId, authorVO.getId()).eq(Follow::getUserId, userId);
        long count = followService.count(followLambdaQueryWrapper);
        authorVO.setIsFollow(count > 0);
        blogVO.setAuthor(authorVO);
        String images = blogVO.getImages();
        if (images == null) {
            return blogVO;
        }
        String[] imgStrs = images.split(",");
        ArrayList<String> imgStrList = new ArrayList<>();
        for (String imgStr : imgStrs) {
            imgStrList.add(imgStr);
        }
        String imgStr = StringUtils.join(imgStrList, ",");
        blogVO.setImages(imgStr);
        blogVO.setCoverImage(imgStrList.get(0));
        return blogVO;
    }

    /**
     * 删除博客
     * @param blogId
     * @param userId
     * @param isAdmin
     */
    @Override
    public void deleteBlog(Long blogId, Long userId, boolean isAdmin) {
        if (isAdmin) {
            this.removeById(blogId);
            return;
        }
        Blog blog = this.getById(blogId);
        if (!userId.equals(blog.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        this.removeById(blogId);
    }

    /**
     * 更新博客
     * @param blogUpdateRequest
     * @param userId
     * @param isAdmin
     */
    @Override
    public void updateBlog(BlogUpdateRequest blogUpdateRequest, Long userId, boolean isAdmin) {
        if (blogUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long createUserId = this.getById(blogUpdateRequest.getId()).getUserId();
        if (!createUserId.equals(userId) && !isAdmin) {
            throw new BusinessException(ErrorCode.NO_AUTH, "没有权限");
        }
        String title = blogUpdateRequest.getTitle();
        String content = blogUpdateRequest.getContent();
        if (StringUtils.isAnyBlank(title, content)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Blog blog = new Blog();
        blog.setId(blogUpdateRequest.getId());
        ArrayList<String> imageNameList = new ArrayList<>();
        if (StringUtils.isNotBlank(blogUpdateRequest.getImgStr())) {
            String imgStr = blogUpdateRequest.getImgStr();
            String[] imgs = imgStr.split(",");
            for (String img : imgs) {
                imageNameList.add(img.substring(25));
            }
        }
        if (blogUpdateRequest.getImages() != null) {
            MultipartFile[] images = blogUpdateRequest.getImages();
            for (MultipartFile image : images) {
                String filename = FileUploadUtil.uploadFileAvatar(image);
                imageNameList.add(filename);
            }
        }
        if (imageNameList.size() > 0) {
            String imageStr = StringUtils.join(imageNameList, ",");
            blog.setImages(imageStr);
        }
        blog.setTitle(blogUpdateRequest.getTitle());
        blog.setContent(blogUpdateRequest.getContent());
        this.updateById(blog);
    }
}



