package com.lyy.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lyy.common.ErrorCode;
import com.lyy.constants.ChatConstant;
import com.lyy.mapper.ChatMapper;
import com.lyy.model.domain.Chat;
import com.lyy.model.domain.Team;
import com.lyy.model.domain.User;
import com.lyy.model.request.ChatRequest;
import com.lyy.model.vo.ChatMessageVO;
import com.lyy.model.vo.WebSocketVO;
import com.lyy.exception.BusinessException;
import com.lyy.service.ChatService;
import com.lyy.service.TeamService;
import com.lyy.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.lyy.constants.UserConstants.ADMIN_ROLE;

/**
 * @author lyy
 * @description 针对表【chat(聊天消息表)】的数据库操作Service实现
 */
@Service
public class ChatServiceImpl extends ServiceImpl<ChatMapper, Chat>
        implements ChatService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    /**
     * 私聊
     * @param chatRequest
     * @param chatType
     * @param loginUser
     * @return
     */
    @Override
    public List<ChatMessageVO> getPrivateChat(ChatRequest chatRequest, int chatType, User loginUser) {
        Long toId = chatRequest.getToId();
        if (toId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<ChatMessageVO> chatRecords = getCache(ChatConstant.CACHE_CHAT_PRIVATE, loginUser.getId() + String.valueOf(toId));
        if (chatRecords != null) {
            saveCache(ChatConstant.CACHE_CHAT_PRIVATE, loginUser.getId() + String.valueOf(toId), chatRecords);
            return chatRecords;
        }
        LambdaQueryWrapper<Chat> chatLambdaQueryWrapper = new LambdaQueryWrapper<>();
        chatLambdaQueryWrapper.
                and(privateChat -> privateChat.eq(Chat::getFromId, loginUser.getId()).eq(Chat::getToId, toId)
                        .or().
                        eq(Chat::getToId, loginUser.getId()).eq(Chat::getFromId, toId)
                ).eq(Chat::getChatType, chatType);
        // 两方共有聊天
        List<Chat> list = this.list(chatLambdaQueryWrapper);
        List<ChatMessageVO> chatMessageVOList = list.stream().map(chat -> {
            ChatMessageVO ChatMessageVO = chatResult(loginUser.getId(), toId, chat.getText(), chatType, chat.getCreateTime());
            if (chat.getFromId().equals(loginUser.getId())) {
                ChatMessageVO.setIsMy(true);
            }
            return ChatMessageVO;
        }).collect(Collectors.toList());
        saveCache(ChatConstant.CACHE_CHAT_PRIVATE, loginUser.getId() + String.valueOf(toId), chatMessageVOList);
        return chatMessageVOList;
    }

    /**
     * 获取聊天缓存
     * @param redisKey
     * @param id
     * @return
     */
    @Override
    public List<ChatMessageVO> getCache(String redisKey, String id) {
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        List<ChatMessageVO> chatRecords;
        if (redisKey.equals(ChatConstant.CACHE_CHAT_HALL)) {
            chatRecords = (List<ChatMessageVO>) valueOperations.get(redisKey);
        } else {
            chatRecords = (List<ChatMessageVO>) valueOperations.get(redisKey + id);
        }
        return chatRecords;
    }

    /**
     * 存储聊天缓存
     * @param redisKey
     * @param id
     * @param chatMessageVOS
     */
    @Override
    public void saveCache(String redisKey, String id, List<ChatMessageVO> chatMessageVOS) {
        try {
            ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
            // 解决缓存雪崩
            int i = RandomUtil.randomInt(2, 3);
            if (redisKey.equals(ChatConstant.CACHE_CHAT_HALL)) {
                valueOperations.set(redisKey, chatMessageVOS, 2 + i / 10, TimeUnit.MINUTES);
            } else {
                valueOperations.set(redisKey + id, chatMessageVOS, 2 + i / 10, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.error("redis set key error");
        }
    }

    /**
     * 聊天消息
     * @param userId
     * @param text
     * @return
     */
    private ChatMessageVO chatResult(Long userId, String text) {
        ChatMessageVO ChatMessageVO = new ChatMessageVO();
        User fromUser = userService.getById(userId);
        WebSocketVO fromWebSocketVo = new WebSocketVO();
        BeanUtils.copyProperties(fromUser, fromWebSocketVo);
        ChatMessageVO.setFormUser(fromWebSocketVo);
        ChatMessageVO.setText(text);
        return ChatMessageVO;
    }

    /**
     * 聊天消息
     * @param userId
     * @param toId
     * @param text
     * @param chatType
     * @param createTime
     * @return
     */
    @Override
    public ChatMessageVO chatResult(Long userId, Long toId, String text, Integer chatType, Date createTime) {
        ChatMessageVO ChatMessageVO = new ChatMessageVO();
        User fromUser = userService.getById(userId);
        User toUser = userService.getById(toId);
        WebSocketVO fromWebSocketVo = new WebSocketVO();
        WebSocketVO toWebSocketVo = new WebSocketVO();
        BeanUtils.copyProperties(fromUser, fromWebSocketVo);
        BeanUtils.copyProperties(toUser, toWebSocketVo);
        ChatMessageVO.setFormUser(fromWebSocketVo);
        ChatMessageVO.setToUser(toWebSocketVo);
        ChatMessageVO.setChatType(chatType);
        ChatMessageVO.setText(text);
        ChatMessageVO.setCreateTime(DateUtil.format(createTime, "yyyy-MM-dd HH:mm:ss"));
        return ChatMessageVO;
    }

    /**
     * 删除缓存
     * @param key
     * @param id
     */
    @Override
    public void deleteKey(String key, String id) {
        if (key.equals(ChatConstant.CACHE_CHAT_HALL)) {
            redisTemplate.delete(key);
        } else {
            redisTemplate.delete(key + id);
        }
    }

    /**
     * 队伍聊天
     * @param chatRequest
     * @param chatType
     * @param loginUser
     * @return
     */
    @Override
    public List<ChatMessageVO> getTeamChat(ChatRequest chatRequest, int chatType, User loginUser) {
        Long teamId = chatRequest.getTeamId();
        if (teamId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求有误");
        }
        List<ChatMessageVO> chatRecords = getCache(ChatConstant.CACHE_CHAT_TEAM, String.valueOf(teamId));
        if (chatRecords != null) {
            List<ChatMessageVO> chatMessageVOS = checkIsMyMessage(loginUser, chatRecords);
            saveCache(ChatConstant.CACHE_CHAT_TEAM, String.valueOf(teamId), chatMessageVOS);
            return chatMessageVOS;
        }
        Team team = teamService.getById(teamId);
        LambdaQueryWrapper<Chat> chatLambdaQueryWrapper = new LambdaQueryWrapper<>();
        chatLambdaQueryWrapper.eq(Chat::getChatType, chatType).eq(Chat::getTeamId, teamId);
        List<ChatMessageVO> chatMessageVOS = returnMessage(loginUser, team.getUserId(), chatLambdaQueryWrapper);
        saveCache(ChatConstant.CACHE_CHAT_TEAM, String.valueOf(teamId), chatMessageVOS);
        return chatMessageVOS;
    }

    /**
     * 大厅聊天
     * @param chatType
     * @param loginUser
     * @return
     */
    @Override
    public List<ChatMessageVO> getHallChat(int chatType, User loginUser) {
        List<ChatMessageVO> chatRecords = getCache(ChatConstant.CACHE_CHAT_HALL, String.valueOf(loginUser.getId()));
        if (chatRecords != null) {
            List<ChatMessageVO> chatMessageVOS = checkIsMyMessage(loginUser, chatRecords);
            saveCache(ChatConstant.CACHE_CHAT_HALL, String.valueOf(loginUser.getId()), chatMessageVOS);
            return chatMessageVOS;
        }
        LambdaQueryWrapper<Chat> chatLambdaQueryWrapper = new LambdaQueryWrapper<>();
        chatLambdaQueryWrapper.eq(Chat::getChatType, chatType);
        List<ChatMessageVO> chatMessageVOS = returnMessage(loginUser, null, chatLambdaQueryWrapper);
        saveCache(ChatConstant.CACHE_CHAT_HALL, String.valueOf(loginUser.getId()), chatMessageVOS);
        return chatMessageVOS;
    }

    /**
     * 检查是否有用户自己的消息
     * @param loginUser
     * @param chatRecords
     * @return
     */
    private List<ChatMessageVO> checkIsMyMessage(User loginUser, List<ChatMessageVO> chatRecords) {
        return chatRecords.stream().peek(chat -> {
            if (chat.getFormUser().getId() != loginUser.getId() && chat.getIsMy()) {
                chat.setIsMy(false);
            }
            if (chat.getFormUser().getId() == loginUser.getId() && !chat.getIsMy()) {
                chat.setIsMy(true);
            }
        }).collect(Collectors.toList());
    }

    /**
     * 返回聊天消息列表
     * @param loginUser
     * @param userId
     * @param chatLambdaQueryWrapper
     * @return
     */
    private List<ChatMessageVO> returnMessage(User loginUser, Long userId, LambdaQueryWrapper<Chat> chatLambdaQueryWrapper) {
        List<Chat> chatList = this.list(chatLambdaQueryWrapper);
        return chatList.stream().map(chat -> {
            ChatMessageVO ChatMessageVO = chatResult(chat.getFromId(), chat.getText());
            boolean isCaptain = userId != null && userId.equals(chat.getFromId());
            if (userService.getById(chat.getFromId()).getRole() == ADMIN_ROLE || isCaptain) {
                ChatMessageVO.setIsAdmin(true);
            }
            if (chat.getFromId().equals(loginUser.getId())) {
                ChatMessageVO.setIsMy(true);
            }
            ChatMessageVO.setCreateTime(DateUtil.format(chat.getCreateTime(), "yyyy年MM月dd日 HH:mm:ss"));
            return ChatMessageVO;
        }).collect(Collectors.toList());
    }
}




