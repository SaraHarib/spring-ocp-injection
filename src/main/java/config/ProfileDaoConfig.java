package config;

import metier.MetierImpl;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan("profiles")
@Import(MetierImpl.class)
public class ProfileDaoConfig {
}
