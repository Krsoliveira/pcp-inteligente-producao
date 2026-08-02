package com.krsoliveira.pcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da aplicação.
 *
 * A anotação {@code @SpringBootApplication} liga a autoconfiguração do Spring e
 * o escaneamento de componentes a partir deste pacote.
 */
@SpringBootApplication
public class PcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(PcpApplication.class, args);
    }
}
