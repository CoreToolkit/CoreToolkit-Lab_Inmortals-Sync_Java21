package edu.eci.arsw.concurrency;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

public final class PauseController {
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition unpaused = lock.newCondition();
  private final Condition allPaused = lock.newCondition();
  
  private volatile boolean paused = false;
  private volatile boolean shuttingDown = false;
  private int pausedCount = 0;
  private int runningCount = 0;

  
  public void registerThread() {
    lock.lock();
    try {
      runningCount++;
    } finally {
      lock.unlock();
    }
  }

  public void unregisterThread() {
    lock.lock();
    try {
      runningCount--;
      if (paused && pausedCount >= runningCount) {
        allPaused.signalAll();
      }
    } finally {
      lock.unlock();
    }
  }

  public void pause() throws InterruptedException {
    lock.lock();
    try {
      paused = true;
      pausedCount = 0;

      long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500);
      while (pausedCount < runningCount && !shuttingDown) {
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0) {
          break;
        }
        allPaused.await(remaining, TimeUnit.NANOSECONDS);
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Reanuda todos los hilos pausados.
   */
  public void resume() {
    lock.lock();
    try {
      paused = false;
      unpaused.signalAll();
    } finally {
      lock.unlock();
    }
  }

  public boolean paused() {
    return paused;
  }

  public void shutdown() {
    lock.lock();
    try {
      shuttingDown = true;
      paused = false;
      unpaused.signalAll();
      allPaused.signalAll();
    } finally {
      lock.unlock();
    }
  }

  
  public void awaitIfPaused() throws InterruptedException {
    if (shuttingDown) return;
    
    lock.lockInterruptibly();
    try {
      while (paused && !shuttingDown) {
        if (pausedCount < runningCount) {
          pausedCount++;
          if (pausedCount >= runningCount) {
            allPaused.signalAll();
          }
        }
        unpaused.await();
      }
    } finally {
      lock.unlock();
    }
  }
}
