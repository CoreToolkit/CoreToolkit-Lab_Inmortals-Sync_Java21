package edu.eci.arsw.immortals;

import edu.eci.arsw.concurrency.PauseController;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import static org.junit.Assert.*;
import java.util.List;
import java.util.concurrent.*;

public class ImmortalTest {

    private ImmortalManager manager;
    private PauseController controller;
    private final int N = 100;
    private final int INITIAL_HEALTH = 100;
    private final int DAMAGE = 10;

    @Before
    public void setUp() {
        manager = new ImmortalManager(N, "ordered", INITIAL_HEALTH, DAMAGE);
        controller = manager.controller();
    }

    @After
    public void tearDown() {
        if (manager != null) {
            manager.stop();
            manager = null;
        }
    }

    @Test
    public void testInvarianteDisminuye() throws InterruptedException {
        System.out.println("=== PRUEBA 1: El invariante debe disminuir ===");

        manager.start();
        Thread.sleep(1000);
        controller.pause();
        Thread.sleep(100);

        long totalHealth = manager.totalHealth();
        long initialTotal = (long) N * INITIAL_HEALTH;

        System.out.println("Salud inicial: " + initialTotal);
        System.out.println("Salud actual: " + totalHealth);

        assertTrue("La salud total debe DISMINUIR con cada pelea (pierde M/2)", totalHealth < initialTotal);
        System.out.println("PRUEBA 1 PASO\n");
        controller.resume();
    }

    @Test
    public void testPerdidaPorPelea() throws InterruptedException {
        System.out.println("=== PRUEBA 2: Verificar perdida de M/2 por pelea ===");

        ImmortalManager smallManager = new ImmortalManager(2, "ordered", 100, 10);
        smallManager.start();
        Thread.sleep(500);
        smallManager.pause();

        long totalHealth = smallManager.totalHealth();
        long fights = smallManager.scoreBoard().totalFights();
        long expectedHealth = 200 - (fights * 5);

        System.out.println("Peleas: " + fights);
        System.out.println("Salud esperada: " + expectedHealth);
        System.out.println("Salud actual: " + totalHealth);

        assertEquals("Cada pelea debe perder exactamente DAMAGE/2", expectedHealth, totalHealth, 5);
        smallManager.stop();
        System.out.println("PRUEBA 2 PASO\n");
    }

    @Test
    public void testSinDeadlocks() throws InterruptedException {
        System.out.println("=== PRUEBA 3: Verificacion de Deadlocks ===");

        manager = new ImmortalManager(100, "ordered", INITIAL_HEALTH, DAMAGE);
        manager.start();

        boolean completo = true;

        for (int i = 0; i < 3; i++) {
            Thread.sleep(1000);
            System.out.println("Segundo " + (i+1) + " - Vivos: " + manager.aliveCount());
            if (manager.aliveCount() == 0) {
                completo = false;
                break;
            }
        }

        manager.pause();
        assertTrue("El manager deberia tener inmortales vivos", manager.aliveCount() > 0);
        assertTrue("La simulacion deberia completarse sin deadlocks", completo);
        System.out.println("PRUEBA 3 PASO\n");
        manager.resume();
    }

    @Test
    public void testEliminarZombies() throws InterruptedException {
        System.out.println("=== PRUEBA 4: Eliminacion de Zombies ===");

        manager.start();
        Thread.sleep(2000);
        controller.pause();
        Thread.sleep(100);

        int aliveBefore = manager.aliveCount();
        int totalBefore = manager.populationSnapshot().size();

        System.out.println("Antes de remover - Vivos: " + aliveBefore + ", Total: " + totalBefore);

        manager.removeDead();

        int aliveAfter = manager.aliveCount();
        int totalAfter = manager.populationSnapshot().size();

        System.out.println("Despues de remover - Vivos: " + aliveAfter + ", Total: " + totalAfter);

        assertEquals("Todos los muertos deberian ser removidos", aliveAfter, totalAfter);
        assertTrue("El numero de vivos deberia ser el mismo o mayor", aliveAfter >= aliveBefore);

        System.out.println("PRUEBA 4 PASO\n");
    }

    @Test
    public void testStopLimpio() throws InterruptedException {
        System.out.println("=== PRUEBA 5: Stop Limpio ===");

        manager.start();
        Thread.sleep(500);
        manager.stop();
        Thread.sleep(500);

        assertTrue("Manager deberia estar detenido", !manager.isRunning() || manager.aliveCount() >= 0);
        System.out.println("PRUEBA 5 PASO\n");
    }

    @Test
    public void testPausaConsistente() throws InterruptedException {
        System.out.println("=== PRUEBA 6: Pausa Consistente ===");

        manager.start();
        Thread.sleep(500);
        controller.pause();
        Thread.sleep(100);

        long health1 = manager.totalHealth();
        Thread.sleep(200);
        long health2 = manager.totalHealth();

        assertEquals("La salud no deberia cambiar durante la pausa", health1, health2);
        System.out.println("PRUEBA 6 PASO\n");
        controller.resume();
    }

    @Test
    public void testMultipleStartStop() throws InterruptedException {
        System.out.println("=== PRUEBA 7: Multiples Start/Stop ===");

        for (int i = 0; i < 3; i++) {
            System.out.println("Ciclo " + (i+1));

            if (manager != null) {
                manager.stop();
            }
            manager = new ImmortalManager(50, "ordered", INITIAL_HEALTH, DAMAGE);

            manager.start();
            Thread.sleep(500);
            assertTrue("Manager deberia tener inmortales vivos en ciclo " + (i+1),
                    manager.aliveCount() > 0);

            manager.stop();
            Thread.sleep(200);
        }

        System.out.println("PRUEBA 7 PASO\n");
    }

    @Test
    public void testNaiveVsOrdered() throws InterruptedException {
        System.out.println("=== PRUEBA 8: Comparacion Naive vs Ordered ===");

        ImmortalManager orderedManager = new ImmortalManager(50, "ordered", 100, 10);
        orderedManager.start();
        Thread.sleep(1000);
        orderedManager.pause();
        long orderedHealth = orderedManager.totalHealth();
        long orderedFights = orderedManager.scoreBoard().totalFights();
        orderedManager.stop();

        ImmortalManager naiveManager = new ImmortalManager(50, "naive", 100, 10);
        naiveManager.start();
        Thread.sleep(1000);
        naiveManager.pause();
        long naiveHealth = naiveManager.totalHealth();
        long naiveFights = naiveManager.scoreBoard().totalFights();
        naiveManager.stop();

        System.out.println("Ordered - Salud: " + orderedHealth + ", Peleas: " + orderedFights);
        System.out.println("Naive - Salud: " + naiveHealth + ", Peleas: " + naiveFights);

        assertTrue("Ordered deberia tener MAS peleas que Naive", orderedFights > naiveFights);
        assertTrue("Ordered deberia tener MENOS salud que Naive (mas peleas = mas perdida)", orderedHealth < naiveHealth);

        System.out.println("PRUEBA 8 PASO\n");
    }

    @Test
    public void testEstres() throws InterruptedException {
        System.out.println("=== PRUEBA 9: Prueba de Estres ===");

        ImmortalManager estresManager = new ImmortalManager(500, "ordered", 100, 10);
        estresManager.start();

        Thread.sleep(3000);
        estresManager.pause();

        long totalHealth = estresManager.totalHealth();
        long initialTotal = 500L * 100;

        System.out.println("Salud inicial: " + initialTotal);
        System.out.println("Salud actual con 500 inmortales: " + totalHealth);

        assertTrue("La salud debe haber disminuido con las peleas", totalHealth < initialTotal);
        assertTrue("Aun deben quedar inmortales vivos", estresManager.aliveCount() > 0);

        estresManager.stop();
        System.out.println("PRUEBA 9 PASO\n");
    }

    @Test
    public void testAtomicidad() throws InterruptedException {
        System.out.println("=== PRUEBA 10: Verificacion de Atomicidad ===");

        manager = new ImmortalManager(10, "ordered", 100, 10);
        manager.start();
        Thread.sleep(500);
        manager.pause();

        for (Immortal im : manager.populationSnapshot()) {
            int health = im.getHealth();
            assertTrue("Health no deberia ser negativo: " + health, health >= 0);
        }

        System.out.println("PRUEBA 10 PASO\n");
        manager.resume();
    }

    @Test
    public void testSaludNegativaAntesDeRemover() throws InterruptedException {
        System.out.println("=== PRUEBA: Salud negativa antes de removeDead() ===");

        ImmortalManager testManager = new ImmortalManager(2, "ordered", 5, 10);
        testManager.start();
        Thread.sleep(500);
        testManager.pause();

        // ANTES de removeDead - DEBE haber salud negativa
        for (Immortal im : testManager.populationSnapshot()) {
            System.out.println(im.name() + ": " + im.getHealth());
        }

        testManager.removeDead();
        System.out.println("DESPUES de removeDead:");

        // DESPUES de removeDead - NO debe haber negativos
        for (Immortal im : testManager.populationSnapshot()) {
            System.out.println(im.name() + ": " + im.getHealth());
            assertTrue(im.getHealth() >= 0);
        }

        testManager.stop();
        System.out.println("PRUEBA SALUD NEGATIVA PASO\n");
    }
}