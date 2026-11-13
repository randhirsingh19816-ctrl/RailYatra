package com.trainreservation.backend.security;

import com.trainreservation.backend.controller.BookingController;
import com.trainreservation.backend.dto.response.BookingConfirmationResponse;
import com.trainreservation.backend.dto.response.BookingHistoryPageResponse;
import com.trainreservation.backend.dto.response.BookingPassengerResponse;
import com.trainreservation.backend.dto.response.CancellationResponse;
import com.trainreservation.backend.dto.response.TrainStationResponse;
import com.trainreservation.backend.enums.BookingStatus;
import com.trainreservation.backend.enums.CancellationStatus;
import com.trainreservation.backend.enums.Gender;
import com.trainreservation.backend.enums.PaymentStatus;
import com.trainreservation.backend.repository.UserRepository;
import com.trainreservation.backend.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BookingController.class)
@Import({
        WebSecurityConfiguration.class,
        JwtAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class BookingControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "alice@example.test", roles = "USER")
    void userCanCreateBookingAndReceivesPendingPaymentConfirmation() throws Exception {
        when(bookingService.createBooking(any())).thenReturn(confirmation());

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Booking created; payment is pending"))
                .andExpect(jsonPath("$.data.pnrNumber").value("ABC23456789HJKLM"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.passengers[0].seatNumber").value("1"));
    }

    @Test
    @WithMockUser(username = "admin@example.test", roles = "ADMIN")
    void adminCannotCreateUserBooking() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden());

        verify(bookingService, never()).createBooking(any());
    }

    @Test
    void anonymousClientCannotCreateBooking() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "alice@example.test", roles = "USER")
    void invalidPassengerDetailsAreRejectedBeforeServiceCall() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "trainRunId": 10,
                                  "sourceStation": "NDLS",
                                  "destinationStation": "LKO",
                                  "journeyDate": "2026-12-01",
                                  "classCode": "SL",
                                  "passengers": [
                                    {"fullName": "Invalid Passenger", "age": 0, "gender": "OTHER"}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(bookingService, never()).createBooking(any());
    }

    @Test
    @WithMockUser(username = "alice@example.test", roles = "USER")
    void userCanRetrieveBookingHistory() throws Exception {
        when(bookingService.getMyBookings(0, 20))
                .thenReturn(new BookingHistoryPageResponse(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/bookings/my-bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.bookings").isArray());

        verify(bookingService).getMyBookings(0, 20);
    }

    @Test
    @WithMockUser(username = "admin@example.test", roles = "ADMIN")
    void adminCanRetrieveBookingByPnr() throws Exception {
        when(bookingService.getBookingByPnr("ABC23456789HJKLM")).thenReturn(confirmation());

        mockMvc.perform(get("/api/bookings/ABC23456789HJKLM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pnrNumber").value("ABC23456789HJKLM"));
    }

    @Test
    @WithMockUser(username = "alice@example.test", roles = "USER")
    void userCanRequestCancellationAndSeesRefundIsPending() throws Exception {
        when(bookingService.cancelBooking("ABC23456789HJKLM")).thenReturn(new CancellationResponse(
                "ABC23456789HJKLM",
                BookingStatus.CANCELLED,
                new BigDecimal("280.00"),
                CancellationStatus.REQUESTED,
                LocalDateTime.of(2026, 11, 1, 10, 0)));

        mockMvc.perform(post("/api/bookings/ABC23456789HJKLM/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.data.refundAmount").value(280.00))
                .andExpect(jsonPath("$.data.refundStatus").value("REQUESTED"))
                .andExpect(jsonPath("$.message").value(
                        "Booking cancelled; refund request is pending and no refund has been processed"));
    }

    private String validRequest() {
        return """
                {
                  "trainRunId": 10,
                  "sourceStation": "NDLS",
                  "destinationStation": "LKO",
                  "journeyDate": "2026-12-01",
                  "classCode": "SL",
                  "passengers": [
                    {"fullName": "Alice Example", "age": 30, "gender": "FEMALE"}
                  ]
                }
                """;
    }

    private BookingConfirmationResponse confirmation() {
        return new BookingConfirmationResponse(
                "ABC23456789HJKLM",
                BookingStatus.PENDING,
                PaymentStatus.PENDING,
                1L,
                "12001",
                "Example Express",
                LocalDate.of(2026, 12, 1),
                new TrainStationResponse(1L, "NDLS", "New Delhi"),
                new TrainStationResponse(2L, "LKO", "Lucknow"),
                "SL",
                List.of(new BookingPassengerResponse("Alice Example", 30, Gender.FEMALE, "S1", "1")),
                new BigDecimal("350.00"),
                new BigDecimal("350.00"),
                "INR",
                LocalDateTime.of(2026, 11, 1, 10, 0));
    }
}
