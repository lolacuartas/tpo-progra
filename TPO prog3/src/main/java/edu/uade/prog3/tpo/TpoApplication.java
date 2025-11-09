package edu.uade.prog3.tpo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.core.Neo4jClient;

@SpringBootApplication
public class TpoApplication {
    public static void main(String[] args) {
        SpringApplication.run(TpoApplication.class, args); 
    }

    @Bean
    CommandLineRunner testNeo4j(Neo4jClient client) {
        return args -> {
            var ok = client.query("RETURN 1 AS ok").fetch().first();
            System.out.println("Neo4j OK? " + ok.orElse(null));
        };
    }


}
