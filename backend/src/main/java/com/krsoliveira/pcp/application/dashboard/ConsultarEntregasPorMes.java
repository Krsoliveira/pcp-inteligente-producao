package com.krsoliveira.pcp.application.dashboard;

import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.OrdemProducaoRepository;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Caso de uso: entregas por mês do fim planejado, para o gráfico do dashboard e a aderência ao prazo.
 *
 * <p>"No prazo" usa o instante em que o sistema registrou a conclusão (a assinatura da ordem), e não a data de
 * fabricação do lote, que é digitada. Ver {@link OrdemProducao#foiConcluidaNoPrazo}.
 */
public class ConsultarEntregasPorMes {

    public static final int MESES_PADRAO = 12;
    public static final int MESES_MAXIMO = 24;

    private final OrdemProducaoRepository ordens;

    public ConsultarEntregasPorMes(OrdemProducaoRepository ordens) {
        this.ordens = ordens;
    }

    /**
     * @param hoje  dia de referência (parâmetro para o resultado ser testável)
     * @param fuso  fuso em que o instante da conclusão vira um dia
     * @param meses quantos meses mostrar, terminando no mês de {@code hoje}; fora de 1..24 é corrigido
     * @return um item por mês, do mais antigo ao mais recente, inclusive os meses sem ordens (com zeros)
     */
    public List<EntregasDoMes> executar(LocalDate hoje, ZoneId fuso, Integer meses) {
        int quantidade = meses == null ? MESES_PADRAO : Math.max(1, Math.min(meses, MESES_MAXIMO));
        YearMonth ultimo = YearMonth.from(hoje);
        YearMonth primeiro = ultimo.minusMonths(quantidade - 1L);

        Map<YearMonth, long[]> contagem = new TreeMap<>(); // [noPrazo, comAtraso, abertasAtrasadas, abertasNoPrazo]
        for (YearMonth mes = primeiro; !mes.isAfter(ultimo); mes = mes.plusMonths(1)) {
            contagem.put(mes, new long[4]);
        }

        for (OrdemProducao ordem : ordens.listarComFimPlanejadoEntre(primeiro.atDay(1), ultimo.atEndOfMonth())) {
            if (ordem.getStatus() == StatusOrdemProducao.CANCELADA) continue;
            long[] mes = contagem.get(YearMonth.from(ordem.getFimPlanejado()));
            if (ordem.getStatus() == StatusOrdemProducao.CONCLUIDA) {
                mes[ordem.foiConcluidaNoPrazo(fuso) ? 0 : 1]++;
            } else {
                mes[ordem.estaAtrasada(hoje) ? 2 : 3]++;
            }
        }

        List<EntregasDoMes> serie = new ArrayList<>();
        contagem.forEach((mes, n) -> serie.add(new EntregasDoMes(mes, n[0], n[1], n[2], n[3])));
        return serie;
    }
}
