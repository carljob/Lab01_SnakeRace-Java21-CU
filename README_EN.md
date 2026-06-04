# Lab Report (English Version)

## Part I — PrimeFinder

**Repository:** https://github.com/carljob/Lab01_PrimeFinder-CU

The PrimeFinder program was modified so that every 5 seconds it pauses all worker threads, shows how many primes have been found so far, and waits for the user to press ENTER to continue.

Each worker thread checks on every iteration whether it should pause. If so, it sleeps using `wait()` without consuming CPU. When the user presses ENTER, the `Control` wakes all threads with `notifyAll()`. A `while` loop is used instead of `if` to avoid lost wakeups — if a thread wakes up spuriously, it re-checks the condition before continuing.

---

## Part II — SnakeRace

### 1) Concurrency Analysis

#### How does the code use threads?
Each snake runs in its own virtual thread created with `Executors.newVirtualThreadPerTaskExecutor()`. Each thread executes a `SnakeRunner` in a continuous loop, moving its snake autonomously. There is also a `GameClock` thread that triggers screen repaints every 60ms and the Swing thread (EDT) that draws the board. With N snakes, there are N+2 threads running simultaneously over the same shared objects.

#### Race conditions found

- **`Snake.body`**: An `ArrayDeque` modified by the snake's thread via `advance()` while Swing reads it via `snapshot()` for rendering. This can cause `ConcurrentModificationException`.

- **`Snake.direction`**: Although marked as `volatile`, the `turn()` method reads and writes the direction without full synchronization, which can cause inconsistent reads.

- **`SnakeApp.snakes`**: An `ArrayList` iterated by Swing in `paintComponent()` while other threads use it. `ArrayList` is not thread-safe.

#### Unsafe collections

- `Snake.body` → `ArrayDeque` without synchronization
- `SnakeApp.snakes` → `ArrayList` without synchronization
- `Board.randomEmpty()` → accesses `mice`, `obstacles`, `turbo` and `teleports` without synchronization

#### Busy-waiting / unnecessary synchronization

There is no explicit busy-wait, but the `GameClock` keeps firing ticks even when the game is paused and simply ignores them with an `if`. This is unnecessary synchronization — the scheduler keeps consuming resources without doing useful work.

---

### 2) Minimal Corrections and Critical Sections

The following problems were fixed:

- **`Snake.body`**: All methods `advance()`, `snapshot()`, `head()`, `turn()` and `direction()` were synchronized to prevent the snake's thread and Swing from accessing the body at the same time. `volatile` was removed from `direction` since synchronization already guarantees visibility.

- **`SnakeApp.snakes`**: Changed from `ArrayList` to `CopyOnWriteArrayList` so Swing can iterate the list without risk of `ConcurrentModificationException`.

- **`Board.step()`**: `snake.advance()` was moved outside the `synchronized` block of `Board` to avoid a potential deadlock, since `step()` held the `Board` lock and called `advance()` which acquires the `Snake` lock.

#### Busy-waiting eliminated

The original `GameClock` kept firing ticks even when the game was paused, ignoring them with an `if` — unnecessary synchronization. This was replaced with `checkPause()` using `wait/notifyAll` on the `GameClock` monitor. `SnakeRunner` threads call `checkPause()` on each iteration and sleep without consuming CPU until the game resumes.

---

### 3) Safe Execution Control (UI)

The following changes were implemented:

- The button correctly toggles between **Pause** and **Resume**.
- When pausing, `SnakeRunner` threads stop using `checkPause()` in `GameClock` with `wait/notifyAll`, ensuring the displayed state is consistent.
- A 500ms delay was added before calculating statistics to ensure all threads are fully stopped.
- When paused, the **longest snake** is shown in green and the **first to die** is shown in red.
- Dead snakes are shown in **gray** when paused and disappear when resumed.
- When resuming, dead snakes are removed from the list and only the living ones continue.

---

### 4) Robustness Under Load

The game was run with `-Dsnakes=20` and higher speed without any errors. No `ConcurrentModificationException`, inconsistent reads, or deadlocks were observed. The synchronization fixes implemented in the previous points guarantee stability under high load.