package edu.eci.arsw.immortals;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ManagerSmokeTest {
  @Test void startsAndStops() throws Exception {
    var m = new ImmortalManager(8, "ordered", 100, 10);
    m.start();
    Thread.sleep(50);
    m.pause();
    long sum = m.totalHealth();
    m.resume();
    m.stop();
    assertTrue(sum > 0);
  }

  @Test
  void multipleStarts() throws Exception {
    var m = new ImmortalManager(20, "ordered", 100, 10);

    for (int i = 0; i < 3; i++) {
      m.start();
      Thread.sleep(100);
      m.pause();
      assertTrue(m.totalHealth() > 0);
      m.resume();
      m.stop();
    }
  }

  @Test
  void pauseStopsProgress() throws Exception {
    var m = new ImmortalManager(20, "ordered", 100, 10);
    m.start();

    Thread.sleep(200);
    m.pause();
    long fights1 = m.scoreBoard().totalFights();

    Thread.sleep(300);
    long fights2 = m.scoreBoard().totalFights();

    m.stop();

    assertTrue(fights1 == fights2);
  }

  @Test
  void stressShortRun() throws Exception {
    var m = new ImmortalManager(200, "ordered", 100, 10);
    m.start();
    Thread.sleep(500);
    m.pause();
    assertTrue(m.aliveCount() > 0);
    m.stop();
  }




}
