package edu.eci.arsw.highlandersim;

import edu.eci.arsw.immortals.Immortal;
import edu.eci.arsw.immortals.ImmortalManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public final class ControlFrame extends JFrame {

  private ImmortalManager manager;
  private final JTextArea output = new JTextArea(14, 40);
  private final JButton startBtn = new JButton("Start");
  private final JButton pauseAndCheckBtn = new JButton("Pause & Check");
  private final JButton resumeBtn = new JButton("Resume");
  private final JButton stopBtn = new JButton("Stop");
  private final JButton removeDeadBtn = new JButton("Remove Dead");

  private final JSpinner countSpinner = new JSpinner(new SpinnerNumberModel(8, 2, 5000, 1));
  private final JSpinner healthSpinner = new JSpinner(new SpinnerNumberModel(100, 10, 10000, 10));
  private final JSpinner damageSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 1000, 1));
  private final JComboBox<String> fightMode = new JComboBox<>(new String[]{"ordered", "naive"});

  public ControlFrame(int count, String fight) {
    setTitle("Highlander Simulator — ARSW");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLayout(new BorderLayout(8,8));

    JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
    top.add(new JLabel("Count:"));
    countSpinner.setValue(count);
    top.add(countSpinner);
    top.add(new JLabel("Health:"));
    top.add(healthSpinner);
    top.add(new JLabel("Damage:"));
    top.add(damageSpinner);
    top.add(new JLabel("Fight:"));
    fightMode.setSelectedItem(fight);
    top.add(fightMode);
    add(top, BorderLayout.NORTH);

    output.setEditable(false);
    output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    add(new JScrollPane(output), BorderLayout.CENTER);

    JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
    bottom.add(startBtn);
    bottom.add(pauseAndCheckBtn);
    bottom.add(resumeBtn);
    bottom.add(stopBtn);
    bottom.add(removeDeadBtn);
    add(bottom, BorderLayout.SOUTH);

    startBtn.addActionListener(this::onStart);
    pauseAndCheckBtn.addActionListener(this::onPauseAndCheck);
    resumeBtn.addActionListener(this::onResume);
    stopBtn.addActionListener(this::onStop);
    removeDeadBtn.addActionListener(this::onRemoveDead);

    pack();
    setLocationByPlatform(true);
    setVisible(true);
  }

  private void onStart(ActionEvent e) {
    safeStop();
    int n = (Integer) countSpinner.getValue();
    int health = (Integer) healthSpinner.getValue();
    int damage = (Integer) damageSpinner.getValue();
    String fight = (String) fightMode.getSelectedItem();
    manager = new ImmortalManager(n, fight, health, damage);
    manager.start();
    output.setText(String.format("Simulation started with %d immortals (health=%d, damage=%d, fight=%s)%n",
            n, health, damage, fight));
  }

  private void onPauseAndCheck(ActionEvent e) {
    if (manager == null) return;
    manager.pause();

    try {
      Thread.sleep(50);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }

    List<Immortal> pop = manager.populationSnapshot();
    long sum = 0;
    int alive = 0;
    int dead = 0;

    int initialN = (Integer) countSpinner.getValue();
    int initialHealth = (Integer) healthSpinner.getValue();
    long expectedTotal = (long) initialN * (long) initialHealth;

    StringBuilder sb = new StringBuilder();
    sb.append(String.format("%-14s | %5s | %s%n", "Immortal", "Health", "Status"));
    sb.append("----------------------------------------\n");

    for (Immortal im : pop) {
      int h = im.getHealth();
      sum += h;
      if (im.isAlive()) {
        alive++;
        sb.append(String.format("%-14s : %5d | ALIVE%n", im.name(), h));
      } else {
        dead++;
        sb.append(String.format("%-14s : %5d | DEAD%n", im.name(), h));
      }
    }

    long healthLost = expectedTotal - sum;

    sb.append("----------------------------------------\n");
    sb.append(String.format("Total Health: %d (Expected: %d)%n", sum, expectedTotal));
    sb.append(String.format("Health Lost: %d%n", healthLost));
    sb.append(String.format("Alive: %d | Dead: %d%n", alive, dead));
    sb.append(String.format("Fights: %d%n", manager.scoreBoard().totalFights()));

    output.setText(sb.toString());
  }

  private void onResume(ActionEvent e) {
    if (manager == null) return;
    manager.resume();
    output.append("Simulation resumed.\n");
  }

  private void onStop(ActionEvent e) {
    safeStop();
    output.append("Simulation stopped.\n");
  }

  private void onRemoveDead(ActionEvent e) {
    if (manager == null) return;
    manager.pause();
    manager.removeDead();
    output.append("Dead immortals removed from population.\n");
    onPauseAndCheck(e);
  }

  private void safeStop() {
    if (manager != null) {
      manager.stop();
      manager = null;
    }
  }

  public static void main(String[] args) {
    int count = Integer.getInteger("count", 8);
    String fight = System.getProperty("fight", "ordered");
    SwingUtilities.invokeLater(() -> new ControlFrame(count, fight));
  }
}