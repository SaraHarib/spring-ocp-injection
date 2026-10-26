package org.example;

import config.BeanAliasDaoConfig;
import config.DynamicDaoConfig;
import config.MultipleDaoConfig;
import config.PrimaryDaoConfig;
import config.ProfileDaoConfig;
import dao.DaoApi;
import dao.DaoFile;
import dao.DaoImpl;
import dao.DaoImpl2;
import dao.IDao;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import metier.IMetier;
import metier.MetierImpl;
import metier.MetierMultiple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import profiles.DaoProd;

import static org.junit.jupiter.api.Assertions.*;

class SpringInjectionTest {
    private static final Set<String> SOURCES = Set.of("dao", "dao2", "daoFile", "daoApi");

    static Stream<Arguments> implementations() {
        return Stream.of(
                Arguments.of(new DaoImpl(), 200.0),
                Arguments.of(new DaoImpl2(), 300.0),
                Arguments.of(new DaoFile(), 360.0),
                Arguments.of(new DaoApi(), 440.0));
    }

    @ParameterizedTest(name = "Calcul métier : résultat attendu {1}")
    @MethodSource("implementations")
    void calculAvecChaqueImplementation(IDao dao, double attendu) {
        assertEquals(attendu, new MetierImpl(dao).calcul());
    }

    @ParameterizedTest(name = "dao.target={0} -> {1}")
    @CsvSource({"dao,200.0", "dao2,300.0", "daoFile,360.0", "daoApi,440.0"})
    void selectionParPropriete(String target, double attendu) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            definirCible(context, target);
            context.register(DynamicDaoConfig.class);
            context.refresh();

            assertSame(context.getBean(target), context.getBean(IDao.class));
            assertEquals(attendu, context.getBean(IMetier.class).calcul());
            assertEquals(MetierImpl.class, context.getBean(IMetier.class).getClass());
            assertEquals(1, context.getBeansOfType(IMetier.class).size());
            assertTrue(context.getBeansOfType(IDao.class).keySet().containsAll(SOURCES));
        }
    }

    @Test
    void configurationInitialeUtiliseDaoApi() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(DynamicDaoConfig.class)) {
            assertEquals("daoApi", context.getEnvironment().getProperty("dao.target"));
            assertInstanceOf(DaoApi.class, context.getBean(IDao.class));
            assertEquals(440.0, context.getBean(IMetier.class).calcul());
        }
    }

    @Test
    void primarySelectionneDaoImplParmiQuatreSources() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(PrimaryDaoConfig.class)) {
            assertEquals(SOURCES, context.getBeansOfType(IDao.class).keySet());
            assertSame(context.getBean("dao"), context.getBean(IDao.class));
            assertInstanceOf(DaoImpl.class, context.getBean(IDao.class));
            assertEquals(200.0, context.getBean(IMetier.class).calcul());
        }
    }

    @Test
    void beanEtAliasSelectionnentLaMemeInstanceDaoApi() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(BeanAliasDaoConfig.class)) {
            IDao source = context.getBean("daoApi", IDao.class);
            assertSame(source, context.getBean(IDao.class));
            assertSame(source, context.getBean("selectedDao"));
            assertSame(source, context.getBean("daoAlias"));
            assertTrue(Arrays.asList(context.getAliases("selectedDao")).contains("daoAlias"));
            assertEquals(440.0, context.getBean(IMetier.class).calcul());
        }
    }

    static Stream<Class<?>> strategies() {
        return Stream.of(DynamicDaoConfig.class, PrimaryDaoConfig.class, BeanAliasDaoConfig.class);
    }

    @ParameterizedTest(name = "Un seul bean @Primary dans {0}")
    @MethodSource("strategies")
    void chaqueStrategiePossedeUnSeulPrimary(Class<?> configuration) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(configuration)) {
            long primaires = Arrays.stream(context.getBeanNamesForType(IDao.class))
                    .filter(nom -> context.getBeanFactory().getBeanDefinition(nom).isPrimary())
                    .count();
            assertEquals(1L, primaires);
        }
    }

    @Test
    void cibleInconnueRejeteeAvecMessageExplicite() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            definirCible(context, "unknown");
            context.register(DynamicDaoConfig.class);
            BeansException erreur = assertThrows(BeansException.class, context::refresh);
            Throwable cause = erreur.getMostSpecificCause();

            assertInstanceOf(IllegalArgumentException.class, cause);
            assertEquals("Cible DAO inconnue : 'unknown'. Valeurs autorisées : dao, dao2, daoFile, daoApi.",
                    cause.getMessage());
            assertFalse(context.isActive());
        }
    }

    @Test
    void constructeurUtiliseLaDependanceFournieSansSelectionInterne() {
        AtomicInteger appels = new AtomicInteger();
        IDao nouvelleSource = () -> {
            appels.incrementAndGet();
            return 37.5;
        };
        MetierImpl metier = new MetierImpl(nouvelleSource);

        assertEquals(75.0, metier.calcul());
        assertEquals(1, appels.get());
    }

    @Test
    void metierDependUniquementDeInterfaceParConstructeur() {
        Field[] champs = MetierImpl.class.getDeclaredFields();
        assertEquals(1, champs.length);
        assertEquals(IDao.class, champs[0].getType());
        assertTrue(Modifier.isPrivate(champs[0].getModifiers()));
        assertTrue(Modifier.isFinal(champs[0].getModifiers()));
        assertEquals(0, champs[0].getDeclaredAnnotations().length);

        Constructor<?>[] constructeurs = MetierImpl.class.getDeclaredConstructors();
        assertEquals(1, constructeurs.length);
        assertTrue(Modifier.isPublic(constructeurs[0].getModifiers()));
        assertArrayEquals(new Class<?>[]{IDao.class}, constructeurs[0].getParameterTypes());
        assertFalse(constructeurs[0].getParameters()[0].isAnnotationPresent(Qualifier.class));
        assertArrayEquals(new Class<?>[]{IMetier.class}, MetierImpl.class.getInterfaces());
        assertTrue(Arrays.stream(MetierImpl.class.getDeclaredMethods())
                .allMatch(methode -> methode.getParameterCount() == 0));
    }

    @Test
    void profilProdActiveAvantRefresh() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("prod");
            context.register(ProfileDaoConfig.class);
            context.refresh();

            assertArrayEquals(new String[]{"prod"}, context.getEnvironment().getActiveProfiles());
            assertEquals(Set.of("prodDao"), context.getBeansOfType(IDao.class).keySet());
            assertInstanceOf(DaoProd.class, context.getBean(IDao.class));
            assertEquals(200.0, context.getBean(IMetier.class).calcul());
        }
    }

    @Test
    void daoProdAbsentQuandProfilInactif() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("dev");
            context.register(DaoProd.class);
            context.refresh();
            assertTrue(context.getBeansOfType(IDao.class).isEmpty());
        }
    }

    @Test
    void injectionListEtMapContientLesQuatreSources() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(MultipleDaoConfig.class)) {
            MetierMultiple multiple = context.getBean(MetierMultiple.class);
            assertEquals(4, multiple.getDaos().size());
            assertEquals(SOURCES, multiple.getDaosParNom().keySet());
            assertEquals(Set.copyOf(multiple.getDaos()), Set.copyOf(multiple.getDaosParNom().values()));
            SOURCES.forEach(nom -> assertSame(context.getBean(nom), multiple.getDaosParNom().get(nom)));
            assertFalse(IMetier.class.isAssignableFrom(MetierMultiple.class));
        }
    }

    @Test
    void plusieursSourcesSansSelectionSontAmbigues() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(MultipleDaoConfig.class)) {
            NoUniqueBeanDefinitionException erreur = assertThrows(
                    NoUniqueBeanDefinitionException.class, () -> context.getBean(IDao.class));
            assertEquals(4, erreur.getNumberOfBeansFound());
            assertEquals(SOURCES, Set.copyOf(erreur.getBeanNamesFound()));
        }
    }

    private static void definirCible(AnnotationConfigApplicationContext context, String target) {
        context.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("test", Map.of("dao.target", target)));
    }
}
