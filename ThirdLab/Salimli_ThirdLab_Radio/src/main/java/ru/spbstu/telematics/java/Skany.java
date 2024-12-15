package ru.spbstu.telematics.java;

import java.util.TreeSet;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Skany {
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private final Condition scanningEndedCondition = lock.newCondition();
    private final Condition eventCondition = lock.newCondition();
    private boolean isOn = false;
    private double frequency = 100.0;
    private boolean scanning = false;
    private boolean end = false;
    private boolean scanningUp = false;
    private boolean locked = false;
    private boolean lockRequested = false;
    private boolean unlockRequested = false;
    private boolean actionBlocked = false;
    private final double UPPER_BOUND = 108.0;
    private final double LOWER_BOUND = 80.0;
    private final double STEP = 0.5;
    private final TreeSet<Double> discoveredStations = new TreeSet<>();

    public void knOnOff() {
        lock.lock();
        try {
            if (!isOn) {
                isOn = true;
                frequency = 100.0;
                scanning = false;
                scanningUp = false;
                System.out.println("[Сканы] Включено!");
            } else {
                isOn = false;
                scanning = false;
                scanningUp = false;
                System.out.println("[Сканы] Выключено");
                scanningEndedCondition.signalAll();
            }
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void knScan() {
        lock.lock();
        try {
            if (isOn) {
                if (locked) {
                    actionBlocked = true;
                    eventCondition.signalAll();
                } else {
                    scanningUp = false;
                    scanning = true;
                    System.out.println("[Сканы] Сканирование вниз от " + frequency + " МГц!");
                    condition.signalAll();
                }
            } else {
                System.out.println("[Сканы] Упс! кажись радио выкл.!");
            }
        } finally {
            lock.unlock();
        }
    }

    public void knReset() {
        lock.lock();
        try {
            if (isOn) {
                if (locked) {
                    actionBlocked = true;
                    eventCondition.signalAll();
                } else {
                    scanningUp = true;
                    scanning = true;
                    System.out.println("[Сканы] Сканирование вверх от " + frequency + " Мгц!");
                    condition.signalAll();
                }
            } else {
                System.out.println("[Сканы] Упс! кажись радио выкл.!");
            }
        } finally {
            lock.unlock();
        }
    }

    public void ZaprosBlokirov() {
        lock.lock();
        try {
            lockRequested = true;
            eventCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void ZaprosRazblok() {
        lock.lock();
        try {
            unlockRequested = true;
            eventCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void end() {
        lock.lock();
        try {
            end = true;
            System.out.println("[Сканы] Завершение работы");
            condition.signalAll();
            scanningEndedCondition.signalAll();
            eventCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void LogDlyaSkanirov() {
        lock.lock();
        try {
            while (!end) {
                if (!isOn || !scanning) {
                    try {
                        condition.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    if (end) break;
                }

                if (!isOn) {
                    scanning = false;
                    continue;
                }

                while (locked && !end) {
                    condition.await();
                    if (end) break;
                }
                if (end) break;

                lock.unlock();
                try {
                    Thread.sleep((long) (Math.random() * 400 + 200));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                lock.lock();

                while (locked && !end) {
                    condition.await();
                    if (end) break;
                }
                if (end) break;

                Double nextStation = scanningUp
                        ? discoveredStations.higher(frequency)
                        : discoveredStations.lower(frequency);

                if (nextStation != null) {
                    frequency = nextStation;
                    scanning = false;
                    NajdenaStanciya(frequency, "(раннее находилась)");
                } else {
                    double newFreq = scanningUp ? frequency + STEP : frequency - STEP;

                    if ((scanningUp && newFreq > UPPER_BOUND) || (!scanningUp && newFreq < LOWER_BOUND)) {
                        frequency = scanningUp ? UPPER_BOUND : LOWER_BOUND;
                        scanning = false;
                        NajdenaStanciya(frequency, scanningUp ? "(верхняя граница)" : "(нижняя граница)");
                    } else {
                        frequency = newFreq;
                        if (Math.random() > 0.9) {
                            scanning = false;
                            NajdenaStanciya(frequency, " - новая станция");
                        }
                    }
                }

                if (!scanning) {
                    scanningEndedCondition.signalAll();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    private void NajdenaStanciya(double freq, String reason) {
        System.out.println("[Сканы] Станция нашлась " + freq + " Мгц " + reason);
        discoveredStations.add(freq);
    }

    public void OzhidanieKoncaSkanirov() {
        lock.lock();
        try {
            while (!end && scanning) {
                try {
                    scanningEndedCondition.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void LogDlyaBlokirov() {
        lock.lock();
        try {
            while (!end) {
                while (!end && !lockRequested && !unlockRequested && !actionBlocked) {
                    try {
                        eventCondition.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                if (end) break;

                if (lockRequested) {
                    locked = true;
                    lockRequested = false;
                    System.out.println("[Блокировка] Сканы заблокировано");
                }

                if (unlockRequested) {
                    locked = false;
                    unlockRequested = false;
                    System.out.println("[Блокировка] Разблокировано");
                    condition.signalAll();
                }

                if (actionBlocked) {
                    System.out.println("[Блокировка] Упс! Кажись радио заблокировано!");
                    actionBlocked = false;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public double getFrequency() {
        lock.lock();
        try {
            return frequency;
        } finally {
            lock.unlock();
        }
    }

    public boolean isOn() {
        lock.lock();
        try {
            return isOn;
        } finally {
            lock.unlock();
        }
    }

    public boolean isEnd() {
        lock.lock();
        try {
            return end;
        } finally {
            lock.unlock();
        }
    }

    public boolean isScanning() {
        lock.lock();
        try {
            return scanning;
        } finally {
            lock.unlock();
        }
    }

    public boolean isScanningUp() {
        lock.lock();
        try {
            return scanningUp;
        } finally {
            lock.unlock();
        }
    }

    public boolean isLocked() {
        lock.lock();
        try {
            return locked;
        } finally {
            lock.unlock();
        }
    }
}
