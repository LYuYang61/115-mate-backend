package com.lyy.service;

import com.lyy.model.domain.BlogComments;
import com.lyy.model.request.AddCommentRequest;
import com.lyy.model.vo.BlogCommentsVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author lyy
* @description 针对表【blog_comments】的数据库操作Service
*/
public interface BlogCommentsService extends IService<BlogComments> {

    void addComment(AddCommentRequest addCommentRequest, Long userId);

    List<BlogCommentsVO> listComments(long blogId, long userId);

    BlogCommentsVO getComment(long commentId, Long userId);

    void likeComment(long commentId, Long userId);

    void deleteComment(Long id, Long userId, boolean isAdmin);

    List<BlogCommentsVO> listMyComments(Long id);
}
