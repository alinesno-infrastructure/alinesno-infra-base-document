package com.alinesno.infra.base.search.service.impl;

import com.alinesno.infra.base.search.entity.ApplicationEntity;
import com.alinesno.infra.base.search.mapper.ApplicationMapper;
import com.alinesno.infra.base.search.service.IApplicationService;
import com.alinesno.infra.common.core.service.impl.IBaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 应用管理Service业务层处理
 *
 * @author luoxiaodong
 * @version 1.0.0
 */
@Slf4j
@Service
public class ApplicationServiceImpl extends IBaseServiceImpl<ApplicationEntity, ApplicationMapper> implements IApplicationService {

}
