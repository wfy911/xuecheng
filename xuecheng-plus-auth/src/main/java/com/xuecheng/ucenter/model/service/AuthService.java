package com.xuecheng.ucenter.model.service;

import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;

public interface AuthService {

    XcUserExt execute(AuthParamsDto authParamsDto);
}
