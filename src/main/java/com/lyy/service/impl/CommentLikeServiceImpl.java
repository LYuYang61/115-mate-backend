package com.lyy.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lyy.mapper.CommentLikeMapper;
import com.lyy.model.domain.CommentLike;
import com.lyy.service.CommentLikeService;
import org.springframework.stereotype.Service;

/**
 * @author lyy
 * @description 针对表【comment_like】的数据库操作Service实现
 */
@Service
public class CommentLikeServiceImpl extends ServiceImpl<CommentLikeMapper, CommentLike>
        implements CommentLikeService {

}




