/*
 * @Author: jackning 270580156@qq.com
 * @Date: 2025-01-08 10:00:00
 * @LastEditors: jackning 270580156@qq.com
 * @LastEditTime: 2025-09-24 11:02:47
 * @Description: bytedesk.com https://github.com/Bytedesk/bytedesk
 *   Please be aware of the BSL license restrictions before installing Bytedesk IM – 
 *  selling, reselling, or hosting Bytedesk IM as a service is a breach of the terms and automatically terminates your rights under the license.
 *  Business Source License 1.1: https://github.com/Bytedesk/bytedesk/blob/main/LICENSE 
 *  contact: 270580156@qq.com 
 *  联系：270580156@qq.com
 * Copyright (c) 2024 by bytedesk.com, All Rights Reserved. 
 */
package com.bytedesk.core.push.service;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import com.bytedesk.core.config.properties.BytedeskProperties;
import com.bytedesk.core.constant.I18Consts;
import com.bytedesk.core.constant.TypeConsts;
import com.bytedesk.core.ip.IpService;
import com.bytedesk.core.ip.IpUtils;
import com.bytedesk.core.push.PushRequest;
import com.bytedesk.core.push.PushRestService;
import com.bytedesk.core.push.PushStatusEnum;
import com.bytedesk.core.push.strategy.AuthValidationStrategy;
import com.bytedesk.core.push.strategy.AuthValidationStrategyFactory;
import com.bytedesk.core.rbac.auth.AuthRequest;
import com.bytedesk.core.utils.Utils;
import com.bytedesk.core.push.PushFilterService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 验证码发送服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushSendService {
    
    private final AuthValidationStrategyFactory strategyFactory;
    private final PushServiceEmail pushServiceEmail;
    private final PushServiceSms pushServiceSms;
    private final BytedeskProperties bytedeskProperties;
    private final IpService ipService;
    private final PushFilterService pushFilterService;
    private final PushRestService pushRestService;

    public PushSendResult sendCode(AuthRequest authRequest, HttpServletRequest request) {
        
        // 参数非空校验
        Assert.notNull(authRequest, "AuthRequest cannot be null");
        Assert.notNull(request, "HttpServletRequest cannot be null");
        Assert.hasText(authRequest.getType(), "AuthRequest type cannot be null or empty");
        Assert.hasText(authRequest.getReceiver(), "AuthRequest receiver cannot be null or empty");
        Assert.hasText(authRequest.getPlatform(), "AuthRequest platform cannot be null or empty");

        String ip = IpUtils.getClientIp(request);
        String type = authRequest.getType();
        String receiver = authRequest.getReceiver();
        String country = authRequest.getCountry();
        String platform = authRequest.getPlatform();
        
        // 验证IP频率限制
        if (!pushFilterService.canSendCode(ip)) {
            // log.info("验证码发送过于频繁，IP: {}", ip);
            return PushSendResult.failure(PushSendResult.SendCodeErrorType.TOO_FREQUENT, I18Consts.I18N_CAPTCHA_SEND_TOO_FREQUENT);
        }
        
        // 使用策略模式处理不同类型的验证逻辑
        AuthValidationStrategy strategy = strategyFactory.getStrategy(type);
        if (strategy != null) {
            strategy.validateUserStatus(authRequest, receiver, platform);
        }
        
        // 检查是否已发送验证码
        if (pushRestService.existsByStatusAndTypeAndReceiver(PushStatusEnum.PENDING.name(), type, receiver)) {
            return PushSendResult.failure(PushSendResult.SendCodeErrorType.ALREADY_SENT, I18Consts.I18N_CAPTCHA_ALREADY_SENT);
        }

        // 生成验证码
        String code = generateCode(receiver);
        
        // 发送验证码
        PushSendResult sendResult = sendCodeByType(authRequest, receiver, country, code, request);
        if (!sendResult.isSuccess()) {
            return sendResult;
        }

        // 保存验证码记录
        saveCodeRecord(authRequest, code, ip, request);
        
        // 更新IP最后发送验证码的时间
        pushFilterService.updateIpLastSentTime(ip);

        return PushSendResult.success();
    }

    private String generateCode(String receiver) {
        if (bytedeskProperties.isInWhitelist(receiver) || bytedeskProperties.isSuperUser(receiver)) {
            return bytedeskProperties.getValidateCode();
        }
        return Utils.getRandomCode();
    }

    private PushSendResult sendCodeByType(AuthRequest authRequest, String receiver, String country, 
                                  String code, HttpServletRequest request) {
        if (authRequest.isEmail()) {
            return pushServiceEmail.sendEmailWithResult(receiver, code, request);
        } else if (authRequest.isMobile()) {
            return pushServiceSms.sendSmsWithResult(receiver, country, code, request);
        }
        return PushSendResult.failure(PushSendResult.SendCodeErrorType.SEND_FAILED, I18Consts.I18N_CAPTCHA_UNSUPPORTED_TYPE);
    }

    private void saveCodeRecord(AuthRequest authRequest, String code, String ip, HttpServletRequest request) {
        String ipLocation = ipService.getIpLocation(ip);
        
        PushRequest pushRequest = new PushRequest();
        // 复制必要的字段
        pushRequest.setType(authRequest.getType());
        pushRequest.setCountry(authRequest.getCountry());
        pushRequest.setReceiver(authRequest.getReceiver());
        pushRequest.setPlatform(authRequest.getPlatform());
        pushRequest.setSender(TypeConsts.TYPE_SYSTEM);
        pushRequest.setContent(code);
        pushRequest.setIp(ip);
        pushRequest.setIpLocation(ipLocation);
        pushRequest.setChannel(authRequest.getChannel());
        pushRequest.setDeviceUid(authRequest.getDeviceUid());
        // 
        pushRestService.create(pushRequest);
    }
}
