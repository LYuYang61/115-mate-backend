package com.lyy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lyy.model.domain.Blog;
import com.lyy.model.domain.User;
import com.lyy.model.request.BlogAddRequest;
import com.lyy.model.request.BlogUpdateRequest;
import com.lyy.model.vo.BlogVO;

/**
* @author lyy
* @description 针对表【blog】的数据库操作Service
*/
public interface BlogService extends IService<Blog> {

    Long addBlog(BlogAddRequest blogAddRequest, User loginUser);

    Page<BlogVO> listMyBlogs(long currentPage, Long id);

    void likeBlog(long blogId, Long userId);

    Page<BlogVO> pageBlog(long currentPage,String title, Long id);

    BlogVO getBlogById(long blogId, Long userId);

    void deleteBlog(Long blogId, Long userId, boolean isAdmin);

    void updateBlog(BlogUpdateRequest blogUpdateRequest, Long userId, boolean isAdmin);
}
