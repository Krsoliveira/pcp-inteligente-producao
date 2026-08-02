package com.krsoliveira.pcp.domain.usuario;

/**
 * Porta do domínio para codificação e verificação de senhas.
 * A implementação concreta usa BCrypt (na camada de infraestrutura),
 * mas o domínio desconhece esse detalhe.
 */
public interface CodificadorDeSenha {

    /** Retorna a versão codificada da senha em texto plano. */
    String codificar(String senhaPlana);

    /** Verifica se a senha em texto plano corresponde ao hash armazenado. */
    boolean verificar(String senhaPlana, String senhaHash);
}
