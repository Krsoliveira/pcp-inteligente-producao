package com.krsoliveira.pcp.infrastructure.seed;

import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Leitor mínimo de CSV (RFC 4180, um registro por linha) para os arquivos do dataset
 * sintético. Suporta campos entre aspas com vírgulas e aspas escapadas ({@code ""}),
 * que é exatamente o que o {@code csv.writer} do Python produz.
 *
 * Cada linha vira um mapa cabeçalho → valor; campos vazios viram {@code ""}.
 */
final class LeitorCsv {

    private LeitorCsv() {
    }

    static List<Map<String, String>> ler(String caminhoClasspath) {
        var recurso = new ClassPathResource(caminhoClasspath);
        try (var leitor = new BufferedReader(
                new InputStreamReader(recurso.getInputStream(), StandardCharsets.UTF_8))) {
            String linhaCabecalho = leitor.readLine();
            if (linhaCabecalho == null) {
                return List.of();
            }
            List<String> cabecalho = separarCampos(linhaCabecalho);
            List<Map<String, String>> registros = new ArrayList<>();
            String linha;
            int numero = 1;
            while ((linha = leitor.readLine()) != null) {
                numero++;
                if (linha.isBlank()) {
                    continue;
                }
                List<String> campos = separarCampos(linha);
                if (campos.size() != cabecalho.size()) {
                    throw new IllegalStateException("%s linha %d: esperados %d campos, encontrados %d."
                            .formatted(caminhoClasspath, numero, cabecalho.size(), campos.size()));
                }
                Map<String, String> registro = new LinkedHashMap<>();
                for (int i = 0; i < cabecalho.size(); i++) {
                    registro.put(cabecalho.get(i), campos.get(i));
                }
                registros.add(registro);
            }
            return registros;
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler " + caminhoClasspath, e);
        }
    }

    static List<String> separarCampos(String linha) {
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean entreAspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (entreAspas) {
                if (c == '"' && i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                    atual.append('"');
                    i++;
                } else if (c == '"') {
                    entreAspas = false;
                } else {
                    atual.append(c);
                }
            } else if (c == '"') {
                entreAspas = true;
            } else if (c == ',') {
                campos.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        campos.add(atual.toString());
        return campos;
    }
}
