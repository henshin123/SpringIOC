package com.zenshin.ioc.modular.service.imp;


import com.zenshin.ioc.annotation.Autowired;
import com.zenshin.ioc.annotation.Service;
import com.zenshin.ioc.modular.dao.LoginMapping;
import com.zenshin.ioc.modular.service.ILoginService;

@Service
public class LoginService implements ILoginService {

    @Autowired
    private LoginMapping loginMapping;

    @Override
    public String login() {
        return loginMapping.login();
    }
}
