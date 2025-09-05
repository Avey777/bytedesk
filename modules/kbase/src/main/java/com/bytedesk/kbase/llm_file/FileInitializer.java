/*
 * @Author: jackning 270580156@qq.com
 * @Date: 2025-05-14 08:38:35
 * @LastEditors: jackning 270580156@qq.com
 * @LastEditTime: 2025-09-05 11:18:19
 * @Description: bytedesk.com https://github.com/Bytedesk/bytedesk
 *   Please be aware of the BSL license restrictions before installing Bytedesk IM – 
 *  selling, reselling, or hosting Bytedesk IM as a service is a breach of the terms and automatically terminates your rights under the license. 
 *  Business Source License 1.1: https://github.com/Bytedesk/bytedesk/blob/main/LICENSE 
 *  contact: 270580156@qq.com 
 * 
 * Copyright (c) 2025 by bytedesk.com, All Rights Reserved. 
 */
package com.bytedesk.kbase.llm_file;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

@Component
public class FileInitializer implements SmartInitializingSingleton {

    @Override
    public void afterSingletonsInstantiated() {
        // init();
    }

    // 迁移到kbaseInitializer
    public void init() {

    }
    
}
