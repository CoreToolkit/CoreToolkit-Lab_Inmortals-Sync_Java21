package edu.eci.arsw.immortals;

import edu.eci.arsw.concurrency.PauseController;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public final class Immortal implements Runnable {
  private final String name;
  private final AtomicInteger health;
  private final int damage;
  private final List<Immortal> population;
  private final ScoreBoard scoreBoard;
  private final PauseController controller;
  private volatile boolean running = true;

  public Immortal(String name, int health, int damage, List<Immortal> population, ScoreBoard scoreBoard, PauseController controller) {
    this.name = Objects.requireNonNull(name);
    this.health = new AtomicInteger(health);
    this.damage = damage;
    this.population = Objects.requireNonNull(population);
    this.scoreBoard = Objects.requireNonNull(scoreBoard);
    this.controller = Objects.requireNonNull(controller);
  }

  public String name() { return name; }
  public int getHealth() { return health.get(); }
  public boolean isAlive() { return health.get() > 0 && running; }
  public void stop() { running = false; }

  @Override
  public void run() {
    controller.registerThread();  
    try {
      while (running) {
        controller.awaitIfPaused();
        if (!running) break;
        var opponent = pickOpponent();
        if (opponent == null) continue;
        String mode = System.getProperty("fight", "ordered");
        if ("naive".equalsIgnoreCase(mode)) fightNaive(opponent);
        else fightOrdered(opponent);
        Thread.sleep(2);
      }
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    } finally {
      controller.unregisterThread();  
    }
  }

  private Immortal pickOpponent() {
    if (population.size() <= 1) return null;
    Immortal other;
    int attempts = 0;
    int maxAttempts = population.size() * 2;
    do {
      other = population.get(ThreadLocalRandom.current().nextInt(population.size()));
      attempts++;
      if (attempts > maxAttempts) return null;
    } while (other == this || !other.isAlive());
    return other;
  }

  private void fightNaive(Immortal other) {
    synchronized (this) {
      synchronized (other) {
        executeFight(other);
      }
    }
  }

  private void fightOrdered(Immortal other) {
    Immortal first = this.name.compareTo(other.name) < 0 ? this : other;
    Immortal second = this.name.compareTo(other.name) < 0 ? other : this;
    synchronized (first) {
      synchronized (second) {
        executeFight(other);
      }
    }
  }

  private void executeFight(Immortal other) {
    if (this.health.get() <= 0 || other.health.get() <= 0) {
      return;
    }

    int otherHealth = other.health.get();
    int actualDamage = Math.min(this.damage, otherHealth);

    other.health.addAndGet(-actualDamage);
    this.health.addAndGet(actualDamage / 2);
    scoreBoard.recordFight();

    if (other.health.get() <= 0) {
      other.health.set(0);
      other.running = false;
    }
  }
}