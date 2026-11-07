package com.krsoliveira.pcp.arquitetura;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;

import java.time.LocalDate;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** As regras invioláveis da arquitetura (ADR-0001), impostas pelo build em vez de só pela disciplina. */
@AnalyzeClasses(packages = "com.krsoliveira.pcp", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

    private static final String[] FRAMEWORKS = {"org.springframework..", "jakarta..", "org.hibernate..", "com.fasterxml.."};

    @ArchTest
    static final ArchRule dominioNaoConheceAsOutrasCamadas = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..", "..infrastructure..");

    @ArchTest
    static final ArchRule dominioSemFramework = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORKS);

    @ArchTest
    static final ArchRule aplicacaoNaoConheceInfraestrutura = noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule aplicacaoSemFramework = noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORKS);

    @ArchTest
    static final ArchRule entidadeJpaFicaNaPersistencia = classes().that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..infrastructure.persistence..")
            .andShould().haveSimpleNameEndingWith("JpaEntity");

    /** "Hoje" vem do relógio configurado: o fuso do servidor não decide validade de lote nem atraso de ordem. */
    @ArchTest
    static final ArchRule hojeVemDoRelogio = noClasses().should().callMethod(LocalDate.class, "now");
}
