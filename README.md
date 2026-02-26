# 📌 TP 6 – Simulation d’un Conflit Concurrent avec Optimistic Locking

## 📚 Cours : Hibernate & JPA

---

## 🎯 Objectif du TP

Ce TP a pour objectif de :

- Comprendre le mécanisme de l’Optimistic Locking
- Utiliser l’annotation `@Version` dans une entité JPA
- Simuler un conflit concurrent entre deux threads
- Gérer l’exception `OptimisticLockException`
- Mettre en place une stratégie de retry

---

## 🛠 Technologies utilisées

- Java  
- Maven  
- Hibernate / JPA  
- H2 Database (base en mémoire)

---

## 🏛 Structure du projet

### Entités :

- `Utilisateur`
- `Salle`
- `Reservation` (avec @Version)

Dans l’entité `Reservation` :

```java
@Version
private Long version;
```

Ce champ permet à Hibernate de détecter les conflits de mise à jour.

---

## 🧪 Simulation du conflit

Une classe `ConcurrentReservationSimulator` a été créée.

Deux threads modifient la même réservation :

- Thread 1 → modifie le motif  
- Thread 2 → modifie les dates  

### Résultat :

- Le premier thread met à jour la réservation
- Le second thread déclenche une `OptimisticLockException`

Hibernate compare la version en base avec la version en mémoire avant la mise à jour.

---

## 🔄 Gestion du conflit (Retry)

Un `OptimisticLockingRetryHandler` a été implémenté.

Principe :

1. Détecter l’exception
2. Recharger l’entité
3. Réappliquer la modification
4. Réessayer (maximum 3 tentatives)

Avec cette stratégie, la mise à jour peut réussir même en cas de conflit concurrent.
## 🎥 Vidéo d’exécution
https://github.com/user-attachments/assets/54e038c3-6e36-467f-a2ae-3e1c02007135


         
La vidéo montre :

- L’exécution sans retry (conflit détecté)
- L’exécution avec retry (résolution automatique)
- L’évolution du champ `version`



## ✅ Conclusion

Ce TP m’a permis de :

- Comprendre le verrouillage optimiste
- Implémenter `@Version` avec JPA/Hibernate
- Simuler un conflit concurrent réel
- Gérer les exceptions liées à la concurrence
- Mettre en place une stratégie de retry

L’Optimistic Locking est une technique essentielle pour gérer la concurrence dans les systèmes de réservation.
