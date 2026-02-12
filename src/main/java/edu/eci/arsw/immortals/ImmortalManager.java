package edu.eci.arsw.immortals;

import edu.eci.arsw.concurrency.PauseController;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Collections;

public final class ImmortalManager implements AutoCloseable {
  private final List<Immortal> population = new CopyOnWriteArrayList<>();
  private final List<Future<?>> futures = new CopyOnWriteArrayList<>();
  private final PauseController controller = new PauseController();
  private final ScoreBoard scoreBoard = new ScoreBoard();
  private ExecutorService exec;
  private final String fightMode;
  private final int initialHealth;
  private final int damage;
  private volatile boolean running = false;

  public ImmortalManager(int n, String fightMode) {
    this(n, fightMode, Integer.getInteger("health", 100), Integer.getInteger("damage", 10));
  }

  public ImmortalManager(int n, String fightMode, int initialHealth, int damage) {
    this.fightMode = fightMode;
    this.initialHealth = initialHealth;
    this.damage = damage;
    reiniciarPoblacion(n);
  }

  private void reiniciarPoblacion(int n) {
    population.clear();
    for (int i = 0; i < n; i++) {
      Immortal immortal = new Immortal("Immortal-" + i, initialHealth, damage,
              population, scoreBoard, controller);
      population.add(immortal);
    }
  }

  public synchronized void start() {
    if (running) {
      stop();
    }
    running = true;
    System.setProperty("fight", fightMode);
    exec = Executors.newVirtualThreadPerTaskExecutor();
    futures.clear();

    for (Immortal im : population) {
      futures.add(exec.submit(im));
    }
  }

  public void pause() {
    controller.pause();
  }

  public void resume() {
    controller.resume();
  }

  public void stop() {
    running = false;
    controller.resume();
    for (Immortal im : population) {
      im.stop();
    }
    if (exec != null) {
      exec.shutdown();
      try {
        if (!exec.awaitTermination(2, TimeUnit.SECONDS)) {
          exec.shutdownNow();
        }
      } catch (InterruptedException e) {
        exec.shutdownNow();
        Thread.currentThread().interrupt();
      }
      exec = null;
    }
    futures.clear();
  }

  public int aliveCount() {
    int count = 0;
    for (Immortal im : population) {
      if (im.isAlive()) count++;
    }
    return count;
  }

  public long totalHealth() {
    long sum = 0;
    for (Immortal im : population) {
      sum += im.getHealth();
    }
    return sum;
  }

  public void removeDead() {
    List<Immortal> toRemove = new ArrayList<>();
    for (Immortal im : population) {
      if (!im.isAlive()) {
        toRemove.add(im);
      }
    }
    population.removeAll(toRemove);
  }

  public List<Immortal> populationSnapshot() {
    return Collections.unmodifiableList(new ArrayList<>(population));
  }

  public ScoreBoard scoreBoard() {
    return scoreBoard;
  }

  public PauseController controller() {
    return controller;
  }

  @Override
  public void close() {
    stop();
  }

  public boolean isRunning() {
    return running && exec != null && !exec.isShutdown();
  }
}