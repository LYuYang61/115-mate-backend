package com.lyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lyy.model.domain.UserTeam;
import org.apache.ibatis.annotations.Mapper;

/**
* @author lyy
* @description 针对表【user_team(用户队伍关系)】的数据库操作Mapper
* @createDate 2023-05-14 11:45:06
* @Entity com.lyy.domain.UserTeam
*/
@Mapper
public interface UserTeamMapper extends BaseMapper<UserTeam> {

}




