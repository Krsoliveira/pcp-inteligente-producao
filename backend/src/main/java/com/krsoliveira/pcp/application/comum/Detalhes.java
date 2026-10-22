package com.krsoliveira.pcp.application.comum;

import java.math.BigDecimal;
import java.time.temporal.TemporalAccessor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Monta o mapa de detalhes de um evento de auditoria só com tipos simples (texto,
 * número, booleano) — datas, UUIDs e enums viram texto. Assim a serialização para
 * JSON é previsível e a leitura da trilha não depende de classes Java.
 */
public final class Detalhes {

    private final Map<String, Object> valores = new LinkedHashMap<>();

    private Detalhes() {
    }

    public static Detalhes vazio() {
        return new Detalhes();
    }

    public static Detalhes com(String chave, Object valor) {
        return new Detalhes().e(chave, valor);
    }

    public Detalhes e(String chave, Object valor) {
        if (valor != null) {
            valores.put(chave, simplificar(valor));
        }
        return this;
    }

    /** Registra uma mudança de valor como {@code {"de": ..., "para": ...}}. */
    public Detalhes mudanca(String chave, Object de, Object para) {
        Map<String, Object> mudanca = new LinkedHashMap<>();
        mudanca.put("de", de == null ? null : simplificar(de));
        mudanca.put("para", para == null ? null : simplificar(para));
        valores.put(chave, mudanca);
        return this;
    }

    public Map<String, Object> mapa() {
        return valores;
    }

    private static Object simplificar(Object valor) {
        if (valor instanceof BigDecimal bd) {
            return bd.stripTrailingZeros().toPlainString();
        }
        if (valor instanceof Number || valor instanceof Boolean || valor instanceof String) {
            return valor;
        }
        if (valor instanceof TemporalAccessor || valor instanceof UUID || valor instanceof Enum<?>) {
            return valor.toString();
        }
        return String.valueOf(valor);
    }
}
