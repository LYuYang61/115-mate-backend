package com.lyy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lyy.common.ErrorCode;
import com.lyy.exception.BusinessException;
import com.lyy.mapper.BlogCommentsMapper;
import com.lyy.model.domain.*;
import com.lyy.service.*;
import com.lyy.model.domain.*;
import com.lyy.model.enums.MessageTypeEnum;
import com.lyy.model.request.AddCommentRequest;
import com.lyy.model.vo.BlogCommentsVO;
import com.lyy.model.vo.BlogVO;
import com.lyy.model.vo.UserVO;
import com.lyy.service.*;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

import static com.lyy.constants.RedisConstants.MESSAGE_LIKE_NUM_KEY;
import static com.lyy.constants.SystemConstants.QiNiuUrl;

/**
 * @author lyy
 * @description 针对表【blog_comments】的数据库操作Service实现
 */
@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments>
        implements BlogCommentsService {

    @Resource
    private UserService userService;

    @Resource
    private BlogService blogService;

    @Resource
    private CommentLikeService commentLikeService;

    @Resource
    private MessageService messageService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 添加评论
     * @param addCommentRequest
     * @param userId
     */
    @Override
    @Transactional
    public void addComment(AddCommentRequest addCommentRequest, Long userId) {
        BlogComments blogComments = new BlogComments();
        blogComments.setUserId(userId);
        blogComments.setBlogId(addCommentRequest.getBlogId());
        blogComments.setContent(addCommentRequest.getContent());
        blogComments.setLikedNum(0);
        blogComments.setStatus(0);
        this.save(blogComments);
        Blog blog = blogService.getById(addCommentRequest.getBlogId());
        blogService.update().eq("id", addCommentRequest.getBlogId())
                .set("comments_num", blog.getCommentsNum() + 1).update();
    }

    /**
     * 根据id获取博文评论
     * @param blogId
     * @param userId
     * @return
     */
    @Override
    public List<BlogCommentsVO> listComments(long blogId, long userId) {
        LambdaQueryWrapper<BlogComments> blogCommentsLambdaQueryWrapper = new LambdaQueryWrapper<>();
        blogCommentsLambdaQueryWrapper.eq(BlogComments::getBlogId, blogId);
        List<BlogComments> blogCommentsList = this.list(blogCommentsLambdaQueryWrapper);
        return blogCommentsList.stream().map((comment) -> {
            BlogCommentsVO blogCommentsVO = new BlogCommentsVO();
            BeanUtils.copyProperties(comment, blogCommentsVO);
            User user = userService.getById(comment.getUserId());
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            blogCommentsVO.setCommentUser(userVO);
            LambdaQueryWrapper<CommentLike> commentLikeLambdaQueryWrapper = new LambdaQueryWrapper<>();
            commentLikeLambdaQueryWrapper.eq(CommentLike::getCommentId, comment.getId()).eq(CommentLike::getUserId, userId);
            long count = commentLikeService.count(commentLikeLambdaQueryWrapper);
            blogCommentsVO.setIsLiked(count > 0);
            return blogCommentsVO;
        }).collect(Collectors.toList());
    }

    /**
     * 通过id获取评论
     * @param commentId
     * @param userId
     * @return
     */
    @Override
    public BlogCommentsVO getComment(long commentId, Long userId) {
        BlogComments comments = this.getById(commentId);
        BlogCommentsVO blogCommentsVO = new BlogCommentsVO();
        BeanUtils.copyProperties(comments, blogCommentsVO);
        LambdaQueryWrapper<CommentLike> commentLikeLambdaQueryWrapper = new LambdaQueryWrapper<>();
        commentLikeLambdaQueryWrapper.eq(CommentLike::getUserId, userId).eq(CommentLike::getCommentId, commentId);
        long count = commentLikeService.count(commentLikeLambdaQueryWrapper);
        blogCommentsVO.setIsLiked(count > 0);
        return blogCommentsVO;
    }

    /**
     * 点赞博文评论
     * @param commentId
     * @param userId
     */
    @Override
    @Transactional
    public void likeComment(long commentId, Long userId) {
        LambdaQueryWrapper<CommentLike> commentLikeLambdaQueryWrapper = new LambdaQueryWrapper<>();
        commentLikeLambdaQueryWrapper.eq(CommentLike::getCommentId, commentId).eq(CommentLike::getUserId, userId);
        long count = commentLikeService.count(commentLikeLambdaQueryWrapper);
        if (count == 0) {
            CommentLike commentLike = new CommentLike();
            commentLike.setCommentId(commentId);
            commentLike.setUserId(userId);
            commentLikeService.save(commentLike);
            BlogComments blogComments = this.getById(commentId);
            this.update().eq("id", commentId)
                    .set("liked_num", blogComments.getLikedNum() + 1)
                    .update();
            String likeNumKey = MESSAGE_LIKE_NUM_KEY + blogComments.getUserId();
            Boolean hasKey = stringRedisTemplate.hasKey(likeNumKey);
            if (Boolean.TRUE.equals(hasKey)) {
                stringRedisTemplate.opsForValue().increment(likeNumKey);
            } else {
                stringRedisTemplate.opsForValue().set(likeNumKey, "1");
            }
            Message message = new Message();
            message.setType(MessageTypeEnum.BLOG_COMMENT_LIKE.getValue());
            message.setFromId(userId);
            message.setToId(blogComments.getUserId());
            message.setData(String.valueOf(blogComments.getId()));
            messageService.save(message);
        } else {
            commentLikeService.remove(commentLikeLambdaQueryWrapper);
            BlogComments blogComments = this.getById(commentId);
            this.update().eq("id", commentId)
                    .set("liked_num", blogComments.getLikedNum() - 1)
                    .update();
        }
    }

    /**
     * 根据id删除评论
     * @param id
     * @param userId
     * @param isAdmin
     */
    @Override
    @Transactional
    public void deleteComment(Long id, Long userId, boolean isAdmin) {
        BlogComments blogComments = this.getById(id);
        if (isAdmin) {
            this.removeById(id);
            Integer commentsNum = blogService.getById(blogComments.getBlogId()).getCommentsNum();
            blogService.update().eq("id", blogComments.getBlogId()).set("comments_num", commentsNum - 1).update();
            return;
        }
        if (blogComments == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (!blogComments.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        this.removeById(id);
        Integer commentsNum = blogService.getById(blogComments.getBlogId()).getCommentsNum();
        blogService.update().eq("id", blogComments.getBlogId()).set("comments_num", commentsNum - 1).update();
    }

    /**
     * 获取我发布的博文评论列表
     * @param id
     * @return
     */
    @Override
    public List<BlogCommentsVO> listMyComments(Long id) {
        LambdaQueryWrapper<BlogComments> blogCommentsLambdaQueryWrapper = new LambdaQueryWrapper<>();
        blogCommentsLambdaQueryWrapper.eq(BlogComments::getUserId, id);
        List<BlogComments> blogCommentsList = this.list(blogCommentsLambdaQueryWrapper);
        return blogCommentsList.stream().map((item) -> {
            BlogCommentsVO blogCommentsVO = new BlogCommentsVO();
            BeanUtils.copyProperties(item, blogCommentsVO);
            User user = userService.getById(item.getUserId());
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            blogCommentsVO.setCommentUser(userVO);

            Long blogId = blogCommentsVO.getBlogId();
            Blog blog = blogService.getById(blogId);
            BlogVO blogVO = new BlogVO();
            BeanUtils.copyProperties(blog, blogVO);
            String images = blogVO.getImages();
            if (images == null) {
                blogVO.setCoverImage(null);
            } else {
                String[] imgStr = images.split(",");
                blogVO.setCoverImage(imgStr[0]);
            }
            Long authorId = blogVO.getUserId();
            User author = userService.getById(authorId);
            UserVO authorVO = new UserVO();
            BeanUtils.copyProperties(author, authorVO);
            blogVO.setAuthor(authorVO);

            blogCommentsVO.setBlog(blogVO);

            LambdaQueryWrapper<CommentLike> commentLikeLambdaQueryWrapper = new LambdaQueryWrapper<>();
            commentLikeLambdaQueryWrapper.eq(CommentLike::getUserId, id).eq(CommentLike::getCommentId, item.getId());
            long count = commentLikeService.count(commentLikeLambdaQueryWrapper);
            blogCommentsVO.setIsLiked(count > 0);
            return blogCommentsVO;
        }).collect(Collectors.toList());
    }
}




