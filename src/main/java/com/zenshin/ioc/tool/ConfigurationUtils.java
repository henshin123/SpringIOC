package com.zenshin.ioc.tool;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigurationUtils {
    /**
     * 项目配置文件信息
     */
    public static Properties properties;

    public ConfigurationUtils(String propertiesPath) {
        properties = this.getBeanScanPath(propertiesPath);
    }
    /**
     读取配置文件
     */
    private Properties getBeanScanPath(String propertiesPath) {
        if (StringUtils.isEmpty(propertiesPath)) {
            propertiesPath = "/application.properties";
        }
        Properties properties = new Properties();
        // 通过类的加载器获取具有给定名称的资源
        InputStream in = ConfigurationUtils.class.getResourceAsStream(propertiesPath);
        try {
            System.out.println("正在加载配置文件application.properties");
            properties.load(in);
            return properties;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return properties;
    }

    /**
     根据配置文件的key获取value的值
     */
    public static Object getPropertiesByKey(String propertiesKey) {
        if (properties.size() > 0) {
            return properties.get(propertiesKey);
        }
        return null;
    }

}
