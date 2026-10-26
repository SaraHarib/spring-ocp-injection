package profiles;

import dao.IDao;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("prodDao")
@Profile("prod")
public class DaoProd implements IDao {
    @Override
    public double getValue() {
        System.out.println("[DAO] Source simulée réservée au profil prod");
        return 100.0;
    }
}
