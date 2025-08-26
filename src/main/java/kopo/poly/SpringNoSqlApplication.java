package kopo.poly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class SpringNoSqlApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringNoSqlApplication.class, args);
    }

}
