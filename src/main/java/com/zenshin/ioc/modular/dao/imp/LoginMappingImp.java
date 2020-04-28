package com.zenshin.ioc.modular.dao.imp;

import com.zenshin.ioc.annotation.Mapping;
import com.zenshin.ioc.modular.dao.LoginMapping;

@Mapping
public class LoginMappingImp implements LoginMapping {
    @Override
    public String login() {
        return "项目启动成功";
    }
}
