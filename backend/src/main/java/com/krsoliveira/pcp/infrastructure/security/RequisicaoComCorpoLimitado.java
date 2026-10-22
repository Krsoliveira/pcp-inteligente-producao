package com.krsoliveira.pcp.infrastructure.security;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Requisição cujo corpo lança {@link CorpoGrandeDemaisException} ao passar de
 * {@code limiteBytes}. Usada quando o cliente não declara Content-Length.
 */
class RequisicaoComCorpoLimitado extends HttpServletRequestWrapper {

    private final long limiteBytes;
    private ServletInputStream entrada;

    RequisicaoComCorpoLimitado(HttpServletRequest request, long limiteBytes) {
        super(request);
        this.limiteBytes = limiteBytes;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (entrada == null) {
            entrada = new EntradaLimitada(super.getInputStream(), limiteBytes);
        }
        return entrada;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        String charset = getCharacterEncoding() != null ? getCharacterEncoding() : "UTF-8";
        return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }

    private static final class EntradaLimitada extends ServletInputStream {

        private final ServletInputStream origem;
        private final long limiteBytes;
        private long lidos;

        EntradaLimitada(ServletInputStream origem, long limiteBytes) {
            this.origem = origem;
            this.limiteBytes = limiteBytes;
        }

        @Override
        public int read() throws IOException {
            int b = origem.read();
            if (b >= 0) {
                contar(1);
            }
            return b;
        }

        @Override
        public int read(byte[] destino, int inicio, int tamanho) throws IOException {
            int n = origem.read(destino, inicio, tamanho);
            if (n > 0) {
                contar(n);
            }
            return n;
        }

        private void contar(long bytes) {
            lidos += bytes;
            if (lidos > limiteBytes) {
                throw new CorpoGrandeDemaisException(limiteBytes);
            }
        }

        @Override
        public boolean isFinished() {
            return origem.isFinished();
        }

        @Override
        public boolean isReady() {
            return origem.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
            origem.setReadListener(listener);
        }
    }
}
