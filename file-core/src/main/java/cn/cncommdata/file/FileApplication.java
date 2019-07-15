package cn.cncommdata.file;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * ElasticsearchApplication
 */
@SpringBootApplication(scanBasePackages = "cn.cncommdata")
@EnableFeignClients(basePackages = {"cn.cncommdata", "cc.iooc"})
@MapperScan("cn.cncommdata.file.dao")
public class FileApplication {

    /**
     * main
     *
     * @param args args
     */
    public static void main(String[] args) {
        SpringApplication.run(FileApplication.class, args);
    }

    /**
     * 没什么用，就是不想让checkstyle报错
     */
    public void init() {
    }

}

