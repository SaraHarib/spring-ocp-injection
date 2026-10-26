package config;

import dao.IDao;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@Configuration
@Import(AppConfig.class)
public class DynamicDaoConfig {
    @Bean
    @Primary
    public IDao selectedDao(@Value("${dao.target}") String target, Map<String, IDao> daos) {
        // Spring fournit les DAO sources ; le bean en cours de création est exclu de la Map.
        IDao selected = daos.get(target);
        if (selected == null) {
            throw new IllegalArgumentException("Cible DAO inconnue : '" + target
                    + "'. Valeurs autorisées : dao, dao2, daoFile, daoApi.");
        }
        return selected;
    }
}
