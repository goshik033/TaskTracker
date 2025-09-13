package service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

public final class TimeGridArray {
    private static final int SLOT_MIN = 15;

    private final LocalDateTime yearStart;
    private final boolean[] busy;

    public TimeGridArray(LocalDateTime nowMoment) {
        this.yearStart = LocalDate.of(nowMoment.getYear(), 1, 1).atStartOfDay();
        int slots = LocalDate.of(nowMoment.getYear(), 1, 1).lengthOfYear() * 24 * (60 / SLOT_MIN);
        this.busy = new boolean[slots];
    }

    private int slot(LocalDateTime t) {
        long minutes = ChronoUnit.MINUTES.between(yearStart, t);
        int totalMinutes = busy.length * SLOT_MIN;
        if (minutes < 0 || minutes >= totalMinutes) {
            throw new IllegalArgumentException("Время вне границ года");
        }
        return (int) (minutes / SLOT_MIN);
    }

    private int span(Duration d) {
        long m = d.toMinutes();
        return (int) Math.ceil(m / (double) SLOT_MIN);
    }

    public boolean intersects(LocalDateTime start, Duration dur) {
        int from = slot(start);
        int to = from + span(dur);
        for (int i = from; i < to; i++) {
            if (busy[i]) return true;
        }
        return false;
    }

    public boolean tryReserve(LocalDateTime start, Duration dur) {
        int from = slot(start);
        int to = from + span(dur);
        for (int i = from; i < to; i++) {
            if (busy[i]) return false;
        }
        Arrays.fill(busy, from, to, true);  // бронируем
        return true;
    }

    public void release(LocalDateTime start, Duration dur) {
        int from = slot(start);
        int to = from + span(dur);
        Arrays.fill(busy, from, to, false);
    }
}
