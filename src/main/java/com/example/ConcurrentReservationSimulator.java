package com.example;

import com.example.model.Reservation;
import com.example.model.Salle;
import com.example.model.Utilisateur;
import com.example.service.ReservationService;
import com.example.service.ReservationServiceImpl;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

public class ConcurrentReservationSimulator {

    private static final EntityManagerFactory emf =
            Persistence.createEntityManagerFactory("optimistic-locking-demo");

    private static final ReservationService reservationService =
            new ReservationServiceImpl(emf);

    public static void main(String[] args) throws InterruptedException {

        // Initialisation
        initData();

        System.out.println("\n=== Simulation d'un conflit sans retry ===");
        simulateConcurrentReservationConflict();

        // Réinitialisation
        initData();

        System.out.println("\n=== Simulation d'un conflit avec retry ===");
        simulateConcurrentReservationConflictWithRetry();

        emf.close();
    }

    // ---------------------------------------------------------
    // Initialisation des données
    // ---------------------------------------------------------

    private static void initData() {

        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();

            Utilisateur utilisateur1 =
                    new Utilisateur("Othmane", "MTN", "OTHMANE@example.com");

            Utilisateur utilisateur2 =
                    new Utilisateur("ALI", "MTNN", "ALI@example.com");

            Salle salle = new Salle("Salle 7", 40);
            salle.setDescription("Salle avec projecteur");

            em.persist(utilisateur1);
            em.persist(utilisateur2);
            em.persist(salle);

            Reservation reservation = new Reservation(
                    LocalDateTime.now().plusDays(1).withHour(10),
                    LocalDateTime.now().plusDays(1).withHour(12),
                    "Réunion entre d'équipe"
            );

            reservation.setUtilisateur(utilisateur1);
            reservation.setSalle(salle);

            em.persist(reservation);

            em.getTransaction().commit();

            System.out.println("Données stockees !");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    // ---------------------------------------------------------
    // Simulation SANS retry
    // ---------------------------------------------------------

    private static void simulateConcurrentReservationConflict()
            throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);

        Thread thread1 = new Thread(() -> {
            try {
                latch.await();

                Reservation r1 = reservationService.findById(1L).get();
                System.out.println("le 1er thread" + r1.getVersion());

                Thread.sleep(850);

                r1.setMotif("Le 1er thread est bien modifié");

                reservationService.update(r1);
                System.out.println("Tres bon");

            } catch (Exception e) {
                System.out.println("Un conflit est bien detecte dans le premier thread");
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                latch.await();

                Reservation r2 = reservationService.findById(1L).get();
                System.out.println("le 2eme thread" + r2.getVersion());

                r2.setDateDebut(r2.getDateDebut().plusHours(1));
                r2.setDateFin(r2.getDateFin().plusHours(1));

                reservationService.update(r2);
                System.out.println("Le 2eme thread est bien modifié");

            } catch (Exception e) {
                System.out.println("Un conflit est bien detecte dans le duxieme thread");
            }
        });

        thread1.start();
        thread2.start();
        latch.countDown();

        thread1.join();
        thread2.join();
    }

    // ---------------------------------------------------------
    // Simulation AVEC retry
    // ---------------------------------------------------------

    private static void simulateConcurrentReservationConflictWithRetry()
            throws InterruptedException {

        OptimisticLockingRetryHandler retryHandler =
                new OptimisticLockingRetryHandler(reservationService, 3);

        CountDownLatch latch = new CountDownLatch(1);

        Thread thread1 = new Thread(() -> {
            try {
                latch.await();

                retryHandler.executeWithRetry(1L, r -> {
                    System.out.println("Thread 1 : Modification motif");
                    r.setMotif("Modifié par Thread 1 a l'aide retry");

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });

            } catch (Exception e) {
                System.out.println("1er thraed : Dernier Exception : "
                        + e.getMessage());
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                latch.await();

                retryHandler.executeWithRetry(1L, r -> {
                    System.out.println("Thread 2 : Modification dates");
                    r.setDateDebut(r.getDateDebut().plusHours(1));
                    r.setDateFin(r.getDateFin().plusHours(1));
                });

            } catch (Exception e) {
                System.out.println("2eme thraed : Dernier Exception : "
                        + e.getMessage());
            }
        });

        thread1.start();
        thread2.start();
        latch.countDown();

        thread1.join();
        thread2.join();

        Optional<Reservation> finalReservation =
                reservationService.findById(1L);

        finalReservation.ifPresent(r -> {
            System.out.println("\n=== Dernier etat a l'aide de retry ===");
            System.out.println("Le Motif : " + r.getMotif());
            System.out.println("Date début : " + r.getDateDebut());
            System.out.println("Date fin : " + r.getDateFin());
            System.out.println("Version : " + r.getVersion());
        });
    }
}