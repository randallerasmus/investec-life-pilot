package za.co.byteservices.moneycoach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MoneyCoachApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoneyCoachApplication.class, args);
    }

}
