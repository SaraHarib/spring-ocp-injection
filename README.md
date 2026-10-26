# Spring OCP - Injection de dépendances

## Sélection dynamique des DAO

![Sélection dynamique des DAO](images/01-selection-dynamique-implementations-ocp.png)

Sélection dynamique de l’implémentation DAO à partir de la configuration, validation des différentes sources de données et respect du principe OCP avec injection par constructeur.

## Stratégies d’injection Spring

![Stratégies d’injection Spring](images/02-primary-bean-alias-profil.png)

Validation des stratégies `@Primary`, `@Bean` avec alias et profil Spring pour sélectionner l’implémentation DAO sans modifier la logique métier.

## Injection multiple

![Injection multiple](images/03-injection-multiple.png)

Injection automatique de plusieurs implémentations de `IDao` sous forme de `List<IDao>` et de `Map<String, IDao>`.
