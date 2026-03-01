package com.extension.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Chrome Extension Backend 主启动类
 */
@SpringBootApplication
@MapperScan("com.extension.backend.mapper")
@EnableConfigurationProperties
public class ChromeExtensionBackendApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ChromeExtensionBackendApplication.class, args);
    }
}
