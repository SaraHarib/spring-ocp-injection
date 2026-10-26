package org.example;

import config.BeanAliasDaoConfig;
import config.DynamicDaoConfig;
import config.MultipleDaoConfig;
import config.PrimaryDaoConfig;
import config.ProfileDaoConfig;
import dao.DaoApi;
import dao.DaoImpl;
import dao.IDao;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import metier.IMetier;
import metier.MetierImpl;
import metier.MetierMultiple;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import profiles.DaoProd;

public class Main {
    private static final List<String> SOURCES = List.of("dao", "dao2", "daoFile", "daoApi");
    private static final Map<String, Double> RESULTATS = Map.of(
            "dao", 200.0, "dao2", 300.0, "daoFile", 360.0, "daoApi", 440.0);

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("TP SPRING - INJECTION DE DEPENDANCES OCP");
        System.out.println("========================================");

        configurationEtAmbiguite();
        selectionDynamique();
        toutesLesSources();
        principeOcp();
        strategiePrimary();
        strategieBeanAlias();
        profilSpring();
        injectionMultiple();
        cibleInconnue();

        System.out.println("\n========================================");
        System.out.println("VERIFICATION FINALE");
        System.out.println("========================================");
        System.out.println("[OK] Interface IDao validée.");
        System.out.println("[OK] Injection par constructeur validée.");
        System.out.println("[OK] @Primary validé.");
        System.out.println("[OK] Alias @Bean validé.");
        System.out.println("[OK] Sélection par propriété validée.");
        System.out.println("[OK] Profils Spring validés.");
        System.out.println("[OK] Injection multiple validée.");
        System.out.println("[OK] Principe OCP validé.");
        System.out.println("[OK] Toutes les vérifications de la démonstration ont réussi.");
        System.out.println("Tests automatisés JUnit : exécuter mvn clean test.");
    }

    private static void configurationEtAmbiguite() {
        section("1. CONFIGURATION SPRING ET AMBIGUITE");
        // Ce contexte expose seulement les sources et l'injection multiple, sans sélection.
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(MultipleDaoConfig.class)) {
            Map<String, IDao> beans = context.getBeansOfType(IDao.class);
            verifier(beans.size() == 4 && beans.keySet().containsAll(SOURCES), "Quatre sources attendues");
            System.out.println("Beans DAO disponibles :");
            SOURCES.forEach(nom -> System.out.println("- " + nom));
            try {
                context.getBean(IDao.class);
                throw new IllegalStateException("Une ambiguïté était attendue sans stratégie de sélection");
            } catch (NoUniqueBeanDefinitionException exception) {
                verifier(exception.getNumberOfBeansFound() == 4, "Quatre candidats attendus");
                System.out.println("Ambiguïté démontrée : 4 beans IDao, aucun @Primary.");
                System.out.println("[OK] NoUniqueBeanDefinitionException attendue et interceptée.");
            }
            System.out.println("[OK] Configuration Spring validée.");
        }
    }

    private static void selectionDynamique() {
        section("2. SELECTION DYNAMIQUE (app.properties)");
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(DynamicDaoConfig.class);
            context.refresh();
            String target = context.getEnvironment().getRequiredProperty("dao.target");
            System.out.println("dao.target = " + target);
            double resultat = context.getBean(IMetier.class).calcul();
            verifier(RESULTATS.get(target).doubleValue() == resultat, "Résultat dynamique incorrect");
            verifier(context.getBean(IDao.class) == context.getBean(target), "Mauvaise source sélectionnée");
            System.out.println("Résultat final : " + resultat);
            System.out.println("[OK] Sélection dynamique validée.");
        }
    }

    private static void toutesLesSources() {
        section("3. TEST DES IMPLEMENTATIONS PAR PROPRIETE");
        for (String target : SOURCES) {
            try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
                definirCible(context, target);
                context.register(DynamicDaoConfig.class);
                context.refresh();
                IMetier metier = context.getBean(IMetier.class);
                verifier(metier.getClass() == MetierImpl.class, "La classe métier doit rester identique");
                double resultat = metier.calcul();
                verifier(RESULTATS.get(target).doubleValue() == resultat, "Résultat incorrect pour " + target);
                System.out.println(target + " -> " + resultat);
            }
        }
        System.out.println("[OK] Sélection dynamique des DAO validée.");
        System.out.println("[OK] Toutes les implémentations DAO validées.");
    }

    private static void principeOcp() {
        section("4. PRINCIPE OCP ET INJECTION PAR CONSTRUCTEUR");
        verifier(IDao.class.isInterface(), "IDao doit être une interface");
        Field[] champs = MetierImpl.class.getDeclaredFields();
        verifier(champs.length == 1 && champs[0].getType() == IDao.class,
                "Le métier doit dépendre uniquement de IDao");
        verifier(Modifier.isPrivate(champs[0].getModifiers()) && Modifier.isFinal(champs[0].getModifiers()),
                "La dépendance doit être privée et finale");
        Constructor<?>[] constructeurs = MetierImpl.class.getDeclaredConstructors();
        verifier(constructeurs.length == 1 && constructeurs[0].getParameterCount() == 1
                        && constructeurs[0].getParameterTypes()[0] == IDao.class,
                "Un constructeur prenant uniquement IDao est attendu");
        // Une nouvelle source arbitraire fonctionne aussi, sans adapter le métier.
        verifier(new MetierImpl(() -> 37.5).calcul() == 75.0, "Le métier doit accepter toute source IDao");
        System.out.println("MetierImpl dépend de : IDao (champ private final, constructeur unique)");
        System.out.println("[OK] MetierImpl dépend uniquement de IDao.");
        System.out.println("[OK] MetierImpl reste inchangé lors du changement de source.");
        System.out.println("[OK] Principe OCP respecté.");
    }

    private static void strategiePrimary() {
        section("5. STRATEGIE @Primary : DaoImpl PAR DEFAUT");
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(PrimaryDaoConfig.class)) {
            IDao dao = context.getBean(IDao.class);
            verifier(dao.getClass() == DaoImpl.class, "DaoImpl doit être prioritaire");
            double valeur = dao.getValue();
            double resultat = context.getBean(IMetier.class).calcul();
            verifier(valeur == 100.0 && resultat == 200.0, "Résultat @Primary incorrect");
            System.out.println("Valeur DAO = " + valeur);
            System.out.println("Résultat métier = " + resultat);
            System.out.println("[OK] Stratégie @Primary validée.");
        }
    }

    private static void strategieBeanAlias() {
        section("6. STRATEGIE @Bean / ALIAS : DaoApi");
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(BeanAliasDaoConfig.class)) {
            IDao dao = context.getBean(IDao.class);
            verifier(dao.getClass() == DaoApi.class && dao == context.getBean("daoApi")
                    && dao == context.getBean("daoAlias"), "L'alias doit désigner DaoApi");
            double valeur = dao.getValue();
            double resultat = context.getBean(IMetier.class).calcul();
            verifier(valeur == 220.0 && resultat == 440.0, "Résultat @Bean incorrect");
            System.out.println("Bean @Primary : selectedDao ; alias Spring : daoAlias");
            System.out.println("Même instance que le bean source daoApi.");
            System.out.println("DaoApi = " + valeur);
            System.out.println("Résultat = " + resultat);
            System.out.println("[OK] Alias @Bean validé.");
        }
    }

    private static void profilSpring() {
        section("7. PROFIL SPRING");
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("prod");
            context.register(ProfileDaoConfig.class);
            context.refresh();
            System.out.println("Profil actif : " + String.join(", ", context.getEnvironment().getActiveProfiles()));
            verifier(context.getBean(IDao.class) instanceof DaoProd, "DAO du profil prod attendu");
            double resultat = context.getBean(IMetier.class).calcul();
            verifier(resultat == 200.0, "Résultat du profil prod incorrect");
            System.out.println("Résultat métier = " + resultat);
            System.out.println("[OK] Profil Spring prod validé.");
        }
    }

    private static void injectionMultiple() {
        section("8. INJECTION MULTIPLE");
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(MultipleDaoConfig.class)) {
            MetierMultiple multiple = context.getBean(MetierMultiple.class);
            verifier(multiple.getDaos().size() == 4 && multiple.getDaosParNom().size() == 4,
                    "Quatre DAO attendus dans List et Map");
            verifier(multiple.getDaos().containsAll(multiple.getDaosParNom().values()),
                    "List et Map doivent contenir les mêmes instances");
            System.out.println("List<IDao> : " + multiple.getDaos().size() + " implémentations");
            System.out.println("Map<String, IDao> - DAO détectés :");
            SOURCES.forEach(nom -> System.out.println("- " + nom + " -> "
                    + multiple.getDaosParNom().get(nom).getClass().getSimpleName()));
            System.out.println("[OK] Injection List<IDao> / Map<String, IDao> validée.");
        }
    }

    private static void cibleInconnue() {
        section("9. GESTION DES ERREURS");
        System.out.println("Essai volontaire : dao.target=unknown (avertissement Spring attendu)");
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            definirCible(context, "unknown");
            context.register(DynamicDaoConfig.class);
            try {
                context.refresh();
                throw new IllegalStateException("La cible inconnue aurait dû être rejetée");
            } catch (BeansException exception) {
                Throwable cause = exception.getMostSpecificCause();
                verifier(cause instanceof IllegalArgumentException && cause.getMessage().contains("unknown"),
                        "Une erreur explicite concernant la cible inconnue est attendue");
                System.out.println("Cible inconnue rejetée : " + cause.getMessage());
                System.out.println("[OK] Cible DAO inconnue correctement rejetée.");
                System.out.println("[OK] Gestion de configuration invalide validée.");
            }
        }
    }

    private static void definirCible(AnnotationConfigApplicationContext context, String target) {
        // Surcharge locale au contexte : app.properties n'est jamais modifié.
        context.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("demonstration", Map.of("dao.target", target)));
    }

    private static void section(String titre) {
        System.out.println("\n" + titre);
        System.out.println("----------------------------------------");
    }

    private static void verifier(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
