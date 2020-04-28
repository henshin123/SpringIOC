package com.zenshin.ioc;

import com.zenshin.ioc.common.ApplicationContext;
import com.zenshin.ioc.modular.controller.LoginController;

public class PlatformApplication {

    public static void main(String[] args) throws Exception {
        ApplicationContext applicationContext = new ApplicationContext();//初始化容器
        LoginController loginController = (LoginController) applicationContext.getIocBean("LoginController");//从容器中取出
        String login = loginController.login();
        System.out.println(login);
    }
}
