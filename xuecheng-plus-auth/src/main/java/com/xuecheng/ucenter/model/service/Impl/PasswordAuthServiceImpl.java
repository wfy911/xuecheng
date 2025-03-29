package com.xuecheng.ucenter.model.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.feignclient.CheckCodeClient;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.model.service.AuthService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service("password_authservice")
public class PasswordAuthServiceImpl implements AuthService {
    @Autowired
    CheckCodeClient checkCodeClient;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    XcUserMapper xcUserMapper;
    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        String checkcode = authParamsDto.getCheckcode();
        if (checkcode==null){
            throw new RuntimeException("未输入验证码");
        }
        String checkcodekey = authParamsDto.getCheckcodekey();
        Boolean verify = checkCodeClient.verify(checkcodekey, checkcode);
        if (verify==null||!verify){
            throw new RuntimeException("验证码错误");
        }
        String username = authParamsDto.getUsername();
        LambdaQueryWrapper<XcUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(XcUser::getUsername, username);
        XcUser xcUser = xcUserMapper.selectOne(queryWrapper);
        if (xcUser == null) {
            throw new RuntimeException("没有用户");
        }
        String passwordDb = xcUser.getPassword();
        String passwordForm = authParamsDto.getPassword();
        boolean flag = passwordEncoder.matches(passwordForm, passwordDb);
        if (!flag){
            throw new RuntimeException("密码错误");
        }
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(xcUser,xcUserExt);
        return xcUserExt;
    }
}
