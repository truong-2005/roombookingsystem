package com.example.roombooking.util;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;

public final class DateTimeUtils {

    private DateTimeUtils() {}

    public static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    public static final DateTimeFormatter MONTH_KEY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private static final Map<DayOfWeek, String> DAY_VI = Map.of(
            DayOfWeek.MONDAY, "Thứ Hai",
            DayOfWeek.TUESDAY, "Thứ Ba",
            DayOfWeek.WEDNESDAY, "Thứ Tư",
            DayOfWeek.THURSDAY, "Thứ Năm",
            DayOfWeek.FRIDAY, "Thứ Sáu",
            DayOfWeek.SATURDAY, "Thứ Bảy",
            DayOfWeek.SUNDAY, "Chủ nhật");

    // ===== Hiện tại =====
    public static LocalDateTime now() {
        return LocalDateTime.now(VN_ZONE);
    }

    public static LocalDate today() {
        return LocalDate.now(VN_ZONE);
    }

    // ===== Tuần (bắt đầu từ thứ Hai) =====
    public static LocalDate startOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static LocalDate endOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }

    // ===== Đầu / cuối khoảng =====
    public static LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    /** Đầu ngày kế tiếp, dùng với điều kiện "< endExclusive". */
    public static LocalDateTime endOfDayExclusive(LocalDate date) {
        return date.plusDays(1).atStartOfDay();
    }

    public static LocalDate startOfMonth(LocalDate date) {
        return date.withDayOfMonth(1);
    }

    // ===== Trùng khoảng thời gian =====
    /**
     * Hai khoảng [s1,e1) và [s2,e2) giao nhau khi s1 < e2 và e1 > s2.
     * Đặt sát nhau (8-10h rồi 10-12h) không bị tính là trùng.
     */
    public static boolean isOverlap(LocalDateTime s1, LocalDateTime e1, LocalDateTime s2, LocalDateTime e2) {
        return s1.isBefore(e2) && e1.isAfter(s2);
    }

    // ===== Thời lượng =====
    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        return Duration.between(start, end).toMinutes();
    }

    public static double hoursBetween(LocalDateTime start, LocalDateTime end) {
        return Duration.between(start, end).toMinutes() / 60.0;
    }

    /** Số giờ của khoảng [start,end) nằm trong cửa sổ [from,to). Bằng 0 nếu không giao nhau. */
    public static double overlapHours(LocalDateTime start, LocalDateTime end,
                                      LocalDateTime from, LocalDateTime to) {
        LocalDateTime s = start.isAfter(from) ? start : from;
        LocalDateTime e = end.isBefore(to) ? end : to;
        return e.isAfter(s) ? hoursBetween(s, e) : 0;
    }

    // ===== Định dạng hiển thị =====
    public static String format(LocalDateTime dt) {
        return dt == null ? "" : dt.format(DATE_TIME_FORMAT);
    }

    public static String format(LocalDate d) {
        return d == null ? "" : d.format(DATE_FORMAT);
    }

    public static String formatRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return "";
        if (start.toLocalDate().equals(end.toLocalDate())) {
            return start.format(TIME_FORMAT) + " - " + end.format(TIME_FORMAT)
                    + " ngày " + start.format(DATE_FORMAT);
        }
        return format(start) + " - " + format(end);
    }

    public static String monthKey(LocalDateTime dt) {
        return dt.format(MONTH_KEY_FORMAT);
    }

    public static String vietnameseDayName(DayOfWeek day) {
        return DAY_VI.get(day);
    }
}