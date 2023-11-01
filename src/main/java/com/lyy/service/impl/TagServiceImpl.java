package com.lyy.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lyy.mapper.TagMapper;
import com.lyy.model.domain.Tag;
import com.lyy.service.TagService;
import org.springframework.stereotype.Service;

/**
* @author lyy
* @description 针对表【tag】的数据库操作Service实现
*/
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {
}




