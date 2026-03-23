/*
 * The MIT License
 *
 * Copyright 2017 Edson Passos - edsonpassosjr@outlook.com.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package me.roinujnosde.titansbattle.types;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * @author RoinujNosde
 */
public record Event(String gameName, me.roinujnosde.titansbattle.types.Event.Frequency frequency, int day, int hour,
                    int minute) {

    public Event {
        if (day < 0 || (day > 7 && frequency == Frequency.WEEKLY) || day > 31) {
            throw new IllegalArgumentException("invalid day");
        }
        if (hour < 0 || hour > 24) {
            throw new IllegalArgumentException("invalid hour");
        }
        if (minute < 0 || minute > 59) {
            throw new IllegalArgumentException("invalid minute");
        }
    }

    public long getDelay() {
        return switch (frequency) {
            case HOURLY -> getHourlyDelay();
            case DAILY -> getDailyDelay();
            case WEEKLY -> getWeeklyDelay();
            case MONTHLY -> getMonthlyDelay();
        };
    }

    private int getHourlyDelay() {
        int difference = minute() - LocalTime.now().getMinute();
        if (difference < 0) {
            difference += 60;
        }
        return difference * 60 * 1000;
    }

    private long getDailyDelay() {
        LocalDateTime target = LocalDateTime.of(LocalDate.now(), LocalTime.of(hour, minute));
        final LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(target)) {
            target = target.plusDays(1);
        }

        return now.until(target, ChronoUnit.MILLIS);
    }

    private long getWeeklyDelay() {
        final int dayOfWeek = LocalDate.now().getDayOfWeek().getValue();
        int dayOfWeekToStart = day;
        if (dayOfWeekToStart < dayOfWeek) {
            dayOfWeekToStart += 7;
        }
        final int difference = dayOfWeekToStart - dayOfWeek;
        LocalDateTime target = LocalDateTime.of(LocalDate.now().plusDays(difference), LocalTime.of(hour, minute));
        final LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(target)) {
            target = target.plusDays(7);
        }

        return now.until(target, ChronoUnit.MILLIS);
    }

    private long getMonthlyDelay() {
        final int dayOfMonth = LocalDate.now().getDayOfMonth();
        int dayOfMonthToStart = day;
        if (dayOfMonthToStart < dayOfMonth) {
            dayOfMonthToStart += LocalDate.now().lengthOfMonth();
        }
        final int difference = dayOfMonthToStart - dayOfMonth;
        LocalDateTime target = LocalDateTime.of(LocalDate.now().plusDays(difference), LocalTime.of(hour, minute));
        final LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(target)) {
            target = target.plusMonths(1);
        }

        return now.until(target, ChronoUnit.MILLIS);
    }

    public enum Frequency {
        HOURLY(3600000),
        DAILY(86400000),
        WEEKLY(604800000),
        MONTHLY(2628000000L);

        private final long periodInMillis;

        Frequency(final long periodInMillis) {
            this.periodInMillis = periodInMillis;
        }

        /**
         * Returns the frequency period in milliseconds.
         *
         * @return the period in millis
         */
        public long getPeriod() {
            return periodInMillis;
        }
    }

}
