package co.nalu;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica en cada build la dirección de las dependencias entre capas y la prohibición de SQL
 * nativo (plan de desarrollo §4.2 y §4.4).
 *
 * <p>Las reglas admiten estar vacías ({@code allowEmptyShould}) porque en la Fase 0 todavía no
 * existen controladores, servicios ni repositorios. En cuanto aparezca la primera clase de una
 * capa, la regla correspondiente empieza a aplicarse sin cambiar esta prueba.
 */
@AnalyzeClasses(packages = "co.nalu", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaCapasTest {

    @ArchTest
    static final ArchRule capas = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("API").definedBy("co.nalu.api..")
            .layer("Negocio").definedBy("co.nalu.negocio..")
            .layer("Integracion").definedBy("co.nalu.integracion..")
            .layer("Datos").definedBy("co.nalu.datos..")
            .whereLayer("API").mayNotBeAccessedByAnyLayer()
            .whereLayer("Negocio").mayOnlyBeAccessedByLayers("API", "Integracion")
            .whereLayer("Integracion").mayNotBeAccessedByAnyLayer()
            .whereLayer("Datos").mayOnlyBeAccessedByLayers("Negocio");

    @ArchTest
    static final ArchRule controladoresSinEntidades = noClasses()
            .that().resideInAPackage("co.nalu.api..")
            .should().dependOnClassesThat().resideInAPackage("co.nalu.datos..")
            .because("los controladores trabajan con DTO, nunca con entidades JPA ni repositorios");

    @ArchTest
    static final ArchRule metodosTransaccionalesSoloEnNegocio = methods()
            .that().areAnnotatedWith(Transactional.class)
            .should().beDeclaredInClassesThat().resideInAPackage("co.nalu.negocio..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule clasesTransaccionalesSoloEnNegocio = classes()
            .that().areAnnotatedWith(Transactional.class)
            .should().resideInAPackage("co.nalu.negocio..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule sinTransaccionalDeJakarta = noClasses()
            .should().dependOnClassesThat().haveFullyQualifiedName("jakarta.transaction.Transactional")
            .because("las transacciones se demarcan con el @Transactional de Spring");

    // ORM obligatorio: nada de SQL escrito a mano.
    @ArchTest
    static final ArchRule sinJdbcDirecto = noClasses()
            .should().dependOnClassesThat().resideInAPackage("org.springframework.jdbc.core..");

    @ArchTest
    static final ArchRule sinConsultasNativasProgramaticas = noClasses()
            .should().callMethodWhere(target(name("createNativeQuery")))
            .because("las consultas se escriben con Spring Data, Specifications o JPQL");

    @ArchTest
    static final ArchRule sinAnotacionNativeQuery = noClasses()
            .should().dependOnClassesThat().areAssignableTo(NativeQuery.class);

    @ArchTest
    static final ArchRule sinQueryNativa = methods()
            .that().areAnnotatedWith(Query.class)
            .should(new ArchCondition<JavaMethod>("no usar nativeQuery = true") {
                @Override
                public void check(JavaMethod metodo, ConditionEvents eventos) {
                    if (metodo.getAnnotationOfType(Query.class).nativeQuery()) {
                        eventos.add(SimpleConditionEvent.violated(metodo,
                                metodo.getFullName() + " usa SQL nativo"));
                    }
                }
            })
            .allowEmptyShould(true);
}
