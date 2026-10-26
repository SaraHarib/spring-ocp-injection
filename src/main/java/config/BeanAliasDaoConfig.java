package config;

import dao.DaoApi;
import dao.IDao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@Configuration
@Import(AppConfig.class)
public class BeanAliasDaoConfig {
    @Bean(name = {"selectedDao", "daoAlias"})
    @Primary
    public IDao selectedDao(DaoApi daoApi) {
        return daoApi;
    }
}
