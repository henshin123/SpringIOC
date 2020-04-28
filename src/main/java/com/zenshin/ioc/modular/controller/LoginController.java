package com.zenshin.ioc.modular.controller;

import com.zenshin.ioc.annotation.Autowired;
import com.zenshin.ioc.annotation.Controller;
import com.zenshin.ioc.annotation.Value;
import com.zenshin.ioc.modular.service.ILoginService;

@Controller
public class LoginController {
    @Value(value = "ioc.scan.pathTest")
    private String pathtest;

    @Autowired
    private ILoginService loginService;

    public String login() {
        return loginService.login();
    }
}
