package com.lyy.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lyy.mapper.BlogLikeMapper;
import com.lyy.model.domain.BlogLike;
import com.lyy.service.BlogLikeService;
import org.springframework.stereotype.Service;

/**
* @author lyy
* @description 针对表【blog_like】的数据库操作Service实现
*/
@Service
public class BlogLikeServiceImpl extends ServiceImpl<BlogLikeMapper, BlogLike>
    implements BlogLikeService {

}




