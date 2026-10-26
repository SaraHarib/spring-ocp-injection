package metier;

import dao.IDao;
import java.util.List;
import java.util.Map;

// Déclaré uniquement dans MultipleDaoConfig, sans implémenter IMetier.
public class MetierMultiple {
    private final List<IDao> daos;
    private final Map<String, IDao> daosParNom;

    public MetierMultiple(List<IDao> daos, Map<String, IDao> daosParNom) {
        this.daos = List.copyOf(daos);
        this.daosParNom = Map.copyOf(daosParNom);
    }

    public List<IDao> getDaos() {
        return daos;
    }

    public Map<String, IDao> getDaosParNom() {
        return daosParNom;
    }
}
