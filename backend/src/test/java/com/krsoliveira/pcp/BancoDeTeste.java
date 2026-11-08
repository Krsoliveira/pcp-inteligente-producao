package com.krsoliveira.pcp;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Um único PostgreSQL para a suíte inteira, com um banco próprio para cada classe de teste ({@link BancoDeTesteIsolado}).
 *
 * <p>Antes, cada classe subia o próprio contêiner e o JUnit o parava no fim da classe, enquanto o Spring guardava o
 * contexto em cache com o pool apontando para um banco que já não existia. Na saída da JVM, os contextos fechavam um a
 * um contra bancos mortos e passavam de 30 s: o Surefire matava a JVM. Com o contêiner vivo até o fim, fecham na hora.
 *
 * <p>O isolamento é o mesmo de antes: cada classe ganha um banco vazio ({@code CREATE DATABASE}), migrado pelo Flyway
 * na subida do contexto, e o contexto não é compartilhado com outra classe.
 */
public final class BancoDeTeste {

    /** A mesma versão do banco de produção (ADR-0033). */
    public static final String IMAGEM = "postgres:18-alpine";

    /** Cada contexto guardado em cache mantém o pool aberto; dimensionado para a suíte toda caber no contêiner. */
    private static final int MAXIMO_DE_CONEXOES_DO_CONTEINER = 600;
    private static final int MAXIMO_DE_CONEXOES_POR_CONTEXTO = 5;

    private static final AtomicInteger SEQUENCIA = new AtomicInteger();

    /** Sobe no primeiro uso e vive até o fim da JVM; quem o derruba é o Ryuk do Testcontainers. */
    private static PostgreSQLContainer conteiner;

    private BancoDeTeste() {
    }

    @SuppressWarnings("resource") // fica aberto de propósito até o fim da JVM
    private static synchronized PostgreSQLContainer conteiner() {
        if (conteiner == null) {
            conteiner = new PostgreSQLContainer(IMAGEM)
                    .withCommand("postgres", "-c", "max_connections=" + MAXIMO_DE_CONEXOES_DO_CONTEINER);
            conteiner.start();
        }
        return conteiner;
    }

    /** Cria um banco vazio no contêiner compartilhado e devolve a URL JDBC dele. */
    static String novoBanco(Class<?> classeDeTeste) {
        PostgreSQLContainer postgres = conteiner();
        String nome = "teste_" + SEQUENCIA.incrementAndGet();
        try (Connection conexao = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement comando = conexao.createStatement()) {
            comando.execute("CREATE DATABASE " + nome);
        } catch (SQLException e) {
            throw new IllegalStateException("Não foi possível criar o banco de teste de " + classeDeTeste.getName(), e);
        }
        return postgres.getJdbcUrl().replaceFirst("/" + postgres.getDatabaseName() + "(?=\\?|$)", "/" + nome);
    }

    /** Banco vazio e sem Spring, para testes que migram por etapas (inserir dados antigos e só então migrar). */
    static ConexaoDeTeste novoBancoParaMigracao(Class<?> classeDeTeste) {
        PostgreSQLContainer postgres = conteiner();
        return new ConexaoDeTeste(novoBanco(classeDeTeste), postgres.getUsername(), postgres.getPassword());
    }

    record ConexaoDeTeste(String url, String usuario, String senha) {
    }

    /** Liga o banco próprio ao contexto da classe marcada com {@link BancoDeTesteIsolado}. */
    public static final class Fabrica implements ContextCustomizerFactory {

        @Override
        public ContextCustomizer createContextCustomizer(Class<?> classeDeTeste,
                                                         List<ContextConfigurationAttributes> atributos) {
            return new Personalizacao(classeDeTeste);
        }
    }

    /**
     * Igualdade pela classe de teste: o contexto (e o banco) não é reaproveitado por outra classe, como acontecia quando
     * cada uma tinha o próprio contêiner.
     */
    private record Personalizacao(Class<?> classeDeTeste) implements ContextCustomizer {

        @Override
        public void customizeContext(ConfigurableApplicationContext contexto, MergedContextConfiguration configuracao) {
            PostgreSQLContainer postgres = conteiner();
            TestPropertyValues.of(
                    "spring.datasource.url=" + novoBanco(classeDeTeste),
                    "spring.datasource.username=" + postgres.getUsername(),
                    "spring.datasource.password=" + postgres.getPassword(),
                    "spring.datasource.hikari.maximum-pool-size=" + MAXIMO_DE_CONEXOES_POR_CONTEXTO,
                    "spring.datasource.hikari.minimum-idle=1"
            ).applyTo(contexto);
        }
    }
}
