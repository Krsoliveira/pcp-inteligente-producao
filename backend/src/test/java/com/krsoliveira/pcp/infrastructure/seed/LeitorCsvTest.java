package com.krsoliveira.pcp.infrastructure.seed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LeitorCsvTest {

    @Test
    @DisplayName("separa campos simples, vazios e entre aspas com vírgula e aspas escapadas")
    void separaCampos() {
        assertThat(LeitorCsv.separarCampos("a,,\"Arame 1,0mm\",\"diz \"\"oi\"\"\","))
                .containsExactly("a", "", "Arame 1,0mm", "diz \"oi\"", "");
    }

    @Test
    @DisplayName("lê o CSV do classpath como mapas cabeçalho → valor")
    void leDoClasspath() {
        var materiais = LeitorCsv.ler("dados/materiais.csv");

        assertThat(materiais).isNotEmpty();
        assertThat(materiais.getFirst()).containsKeys("codigo", "descricao", "tipo", "unidade_de_medida");
        assertThat(materiais).anySatisfy(m -> assertThat(m.get("descricao")).contains(","));
    }
}
