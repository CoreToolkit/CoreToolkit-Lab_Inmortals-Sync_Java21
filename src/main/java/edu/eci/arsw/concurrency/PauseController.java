package edu.eci.arsw.concurrency;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

public final class PauseController {
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition unpaused = lock.newCondition();
  private volatile boolean paused = false;
  private volatile boolean shuttingDown = false;

  public void pause() {
    lock.lock();
    try {
      paused = true;
    } finally {
      lock.unlock();
    }
  }

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
    shuttingDown = true;
    resume();
  }

  public void awaitIfPaused() throws InterruptedException {
    if (shuttingDown) return;
    lock.lockInterruptibly();
    try {
      while (paused && !shuttingDown) {
        unpaused.await(100, TimeUnit.MILLISECONDS);
      }
    } finally {
      lock.unlock();
    }
  }

  public void awaitIfPaused(long timeout, TimeUnit unit) throws InterruptedException {
    if (shuttingDown) return;
    lock.lockInterruptibly();
    try {
      while (paused && !shuttingDown) {
        if (!unpaused.await(timeout, unit)) {
          break;
        }
      }
    } finally {
      lock.unlock();
    }
  }
}