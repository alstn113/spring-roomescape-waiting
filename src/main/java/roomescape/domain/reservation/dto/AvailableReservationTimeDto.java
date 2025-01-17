package roomescape.domain.reservation.dto;

import java.time.LocalTime;

public record AvailableReservationTimeDto(Long id, LocalTime startAt, boolean alreadyBooked) {
}
