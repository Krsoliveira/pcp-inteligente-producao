package com.krsoliveira.pcp.domain.auditoria;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssinaturaTest {

    @Test
    @DisplayName("nova assinatura: criador e último alterador são o mesmo usuário")
    void nova() {
        Assinatura assinatura = Assinatura.nova("ana@pcp");

        assertThat(assinatura.criadoPor()).isEqualTo("ana@pcp");
        assertThat(assinatura.alteradoPor()).isEqualTo("ana@pcp");
        assertThat(assinatura.alteradoEm()).isEqualTo(assinatura.criadoEm());
    }

    @Test
    @DisplayName("alteração preserva quem criou e quando")
    void alterada() {
        Assinatura original = Assinatura.nova("ana@pcp");

        Assinatura alterada = original.alterada("bruno@pcp");

        assertThat(alterada.criadoPor()).isEqualTo("ana@pcp");
        assertThat(alterada.criadoEm()).isEqualTo(original.criadoEm());
        assertThat(alterada.alteradoPor()).isEqualTo("bruno@pcp");
        assertThat(alterada.alteradoEm()).isAfterOrEqualTo(original.criadoEm());
    }

    @Test
    @DisplayName("não existe assinatura sem usuário")
    void exigeUsuario() {
        assertThatThrownBy(() -> Assinatura.nova(" "))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> Assinatura.nova("ana@pcp").alterada(null))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    @DisplayName("mudança de status da ordem fica assinada por quem a fez")
    void mudancaDeStatusAssinada() {
        OrdemProducao ordem = OrdemProducao.criar("OP-1", UUID.randomUUID(), UUID.randomUUID(), null,
                "CNC", 10, LocalDate.now(), LocalDate.now().plusDays(3), "ana@pcp");

        ordem.alterarStatusPara(StatusOrdemProducao.LIBERADA, "bruno@pcp");

        assertThat(ordem.getAssinatura().criadoPor()).isEqualTo("ana@pcp");
        assertThat(ordem.getAssinatura().alteradoPor()).isEqualTo("bruno@pcp");
        assertThatThrownBy(() -> ordem.alterarStatusPara(StatusOrdemProducao.EM_PRODUCAO, ""))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThat(ordem.getStatus()).isEqualTo(StatusOrdemProducao.LIBERADA);
    }
}
