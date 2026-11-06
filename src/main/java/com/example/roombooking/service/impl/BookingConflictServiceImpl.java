package com.example.roombooking.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.roombooking.constant.BookingConstants;
import com.example.roombooking.dto.booking.BookingConflictResponse;
import com.example.roombooking.entity.Booking;
import com.example.roombooking.entity.Room;
import com.example.roombooking.exception.BadRequestException;
import com.example.roombooking.exception.BookingConflictException;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.mapper.BookingMapper;
import com.example.roombooking.repository.BookingRepository;
import com.example.roombooking.repository.RoomRepository;
import com.example.roombooking.service.BookingConflictService;
import com.example.roombooking.util.DateTimeUtils;


@Service
@Transactional(readOnly = true)
public class BookingConflictServiceImpl implements BookingConflictService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final BookingMapper bookingMapper;

    @java.beans.ConstructorProperties({"bookingRepository", "roomRepository", "bookingMapper"})
    public BookingConflictServiceImpl(
        BookingRepository bookingRepository,
        RoomRepository roomRepository,
        BookingMapper bookingMapper
    ) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.bookingMapper = bookingMapper;
    }

    @Override
    public List<Booking> findConflicts(Long roomId, LocalDateTime start, LocalDateTime end, Long excludeBookingId) {
        return bookingRepository.findConflicts(roomId, BookingConstants.BLOCKING_STATUSES, start, end,
                excludeBookingId == null ? -1L : excludeBookingId);
    }

    @Override
    public BookingConflictResponse check(Long roomId, LocalDateTime start, LocalDateTime end, Long excludeBookingId) {
        if (!roomRepository.existsById(roomId)) throw new ResourceNotFoundException("phòng", roomId);
        if (start == null || end == null) throw new BadRequestException("Vui lòng nhập thời gian bắt đầu và kết thúc");
        if (!end.isAfter(start)) throw new BadRequestException("Thời gian kết thúc phải sau thời gian bắt đầu");
        return bookingMapper.toConflictResponse(findConflicts(roomId, start, end, excludeBookingId));
    }

    @Override
    public void assertNoConflict(Long roomId, LocalDateTime start, LocalDateTime end, Long excludeBookingId) {
        List<Booking> conflicts = findConflicts(roomId, start, end, excludeBookingId);
        if (!conflicts.isEmpty()) {
            Booking first = conflicts.get(0);
            throw new BookingConflictException(
                    BookingConstants.ERR_CONFLICT + ": \"" + first.getTitle() + "\" ("
                            + DateTimeUtils.formatRange(first.getStartTime(), first.getEndTime()) + ")",
                    bookingMapper.toConflictItems(conflicts));
        }
    }

    @Override
    public void validateRules(Room room, LocalDateTime start, LocalDateTime end, Integer attendees) {
        if (!room.isActive()) throw new BadRequestException(BookingConstants.ERR_ROOM_INACTIVE);
        if (!end.isAfter(start)) throw new BadRequestException("Thời gian kết thúc phải sau thời gian bắt đầu");
        if (!start.toLocalDate().equals(end.toLocalDate()))
            throw new BadRequestException("Lịch đặt phải bắt đầu và kết thúc trong cùng một ngày");

        LocalDateTime now = DateTimeUtils.now();
        if (start.isBefore(now.plusMinutes(BookingConstants.MIN_ADVANCE_MINUTES)))
            throw new BadRequestException("Không thể đặt phòng trong quá khứ");
        if (start.isAfter(now.plusDays(BookingConstants.MAX_ADVANCE_DAYS)))
            throw new BadRequestException("Chỉ được đặt trước tối đa " + BookingConstants.MAX_ADVANCE_DAYS + " ngày");

        if (start.toLocalTime().isBefore(BookingConstants.OPENING_TIME)
                || end.toLocalTime().isAfter(BookingConstants.CLOSING_TIME))
            throw new BadRequestException(String.format(BookingConstants.ERR_OUTSIDE_HOURS,
                    BookingConstants.OPENING_TIME, BookingConstants.CLOSING_TIME));

        long minutes = DateTimeUtils.minutesBetween(start, end);
        if (minutes < BookingConstants.MIN_DURATION_MINUTES)
            throw new BadRequestException(String.format(BookingConstants.ERR_TOO_SHORT,
                    BookingConstants.MIN_DURATION_MINUTES));
        if (minutes > BookingConstants.MAX_DURATION_HOURS * 60L)
            throw new BadRequestException(String.format(BookingConstants.ERR_TOO_LONG,
                    BookingConstants.MAX_DURATION_HOURS));

        if (attendees != null && attendees > room.getCapacity())
            throw new BadRequestException(String.format(BookingConstants.ERR_OVER_CAPACITY, room.getCapacity()));
    }
}