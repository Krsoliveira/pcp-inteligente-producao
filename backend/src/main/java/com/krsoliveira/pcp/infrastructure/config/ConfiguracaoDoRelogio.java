package com.krsoliveira.pcp.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * O relógio do sistema, no fuso da operação. "Hoje" decide validade de lote e atraso de ordem, então não pode depender
 * do fuso do servidor: em UTC, às 21h de Brasília o dia já teria virado.
 */
@Configuration
public class ConfiguracaoDoRelogio {

    @Bean
    Clock relogio(@Value("${pcp.fuso-horario:America/Sao_Paulo}") String fusoHorario) {
        return Clock.system(ZoneId.of(fusoHorario));
    }
}
