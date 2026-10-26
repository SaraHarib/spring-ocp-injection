package config;

import dao.DaoApi;
import dao.DaoFile;
import dao.DaoImpl;
import dao.DaoImpl2;
import metier.MetierImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

// Contexte indépendant : les quatre sources existent, seul dao est prioritaire.
@Configuration
@Import({DaoImpl2.class, DaoFile.class, DaoApi.class, MetierImpl.class})
public class PrimaryDaoConfig {
    @Bean("dao")
    @Primary
    public DaoImpl dao() {
        return new DaoImpl();
    }
}
