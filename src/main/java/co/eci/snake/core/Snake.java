package co.eci.snake.core;

import java.util.ArrayDeque;
import java.util.Deque;

public final class Snake {
  private final Deque<Position> body = new ArrayDeque<>();
  private  Direction direction;
  private int maxLength = 5;
  private boolean alive = true;
  private long deathTime = Long.MAX_VALUE;
  private int maxSegmentsReached = 5;


  private Snake(Position start, Direction dir) {
    body.addFirst(start);
    this.direction = dir;
  }

  public static Snake of(int x, int y, Direction dir) {
    return new Snake(new Position(x, y), dir);
  }

  public synchronized Direction direction() {
    return direction;
  }

  public synchronized void turn(Direction dir) {
    if ((direction == Direction.UP && dir == Direction.DOWN) ||
            (direction == Direction.DOWN && dir == Direction.UP) ||
            (direction == Direction.LEFT && dir == Direction.RIGHT) ||
            (direction == Direction.RIGHT && dir == Direction.LEFT)) {
      return;
    }
    this.direction = dir;
  }

  public synchronized Position head() {
    return body.peekFirst();
  }

  public synchronized Deque<Position> snapshot() {
    return new ArrayDeque<>(body);
  }

  public synchronized void advance(Position newHead, boolean grow) {
    body.addFirst(newHead);
    if (grow) {
      maxLength++;
      maxSegmentsReached = Math.max(maxSegmentsReached, maxLength);
    }
    while (body.size() > maxLength) body.removeLast();
  }

  public synchronized boolean isAlive() {
    return alive;
  }

  public synchronized void die() {
    if (alive) {
      alive = false;
      deathTime = System.currentTimeMillis();
    }
  }

  public synchronized long getDeathTime() {
    return deathTime;
  }

  public synchronized int getMaxSegmentsReached() {
    return maxSegmentsReached;
  }
}
