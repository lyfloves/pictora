package com.yufei.pictorabackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.yufei.pictorabackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class PictoraBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PictoraBackendApplication.class, args);
    }

}
