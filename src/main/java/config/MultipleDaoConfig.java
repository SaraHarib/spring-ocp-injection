package config;

import metier.MetierMultiple;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan("dao")
@Import(MetierMultiple.class)
public class MultipleDaoConfig {
}
