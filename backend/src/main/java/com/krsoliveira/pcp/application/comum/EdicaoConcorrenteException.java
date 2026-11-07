package com.krsoliveira.pcp.application.comum;

/**
 * A edição foi feita sobre uma versão que já não é a atual: outra pessoa salvou o registro depois que esta tela o
 * abriu. Gravar por cima apagaria a alteração dela sem aviso, então a edição é recusada (409) e nada muda (ADR-0034).
 */
public class EdicaoConcorrenteException extends ConflitoException {

    public EdicaoConcorrenteException(String registro) {
        super("%s foi alterado por outra pessoa enquanto você editava. Recarregue para ver a versão atual e refaça a alteração."
                .formatted(registro));
    }

    /**
     * Recusa a edição se a versão lida pela tela não for a atual. Sem versão informada (cliente que não a envia), a
     * edição segue como antes: o campo é opcional para não quebrar o contrato da API.
     *
     * @param versaoLida  versão que a tela tinha ao abrir a edição, ou {@code null}
     * @param versaoAtual versão do registro, lida com o registro travado
     * @param registro    como a mensagem nomeia o registro, ex.: {@code O perfil "Supervisor"}
     */
    public static void exigirVersaoAtual(Long versaoLida, long versaoAtual, String registro) {
        if (versaoLida != null && versaoLida != versaoAtual) {
            throw new EdicaoConcorrenteException(registro);
        }
    }
}
