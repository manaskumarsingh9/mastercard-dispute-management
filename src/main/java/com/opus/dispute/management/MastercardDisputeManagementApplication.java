package com.opus.dispute.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MastercardDisputeManagementApplication {

        public static void main(String[] args) {
                SpringApplication.run(MastercardDisputeManagementApplication.class, args);
        }

}
