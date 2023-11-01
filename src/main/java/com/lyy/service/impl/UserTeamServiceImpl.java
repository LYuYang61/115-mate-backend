package com.lyy.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lyy.mapper.UserTeamMapper;
import com.lyy.model.domain.UserTeam;
import com.lyy.service.UserTeamService;
import org.springframework.stereotype.Service;

/**
* @author lyy
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService {

}




