package com.trainreservation.backend.controller;

import com.trainreservation.backend.dto.response.TrainSearchResponse;
import com.trainreservation.backend.dto.response.TrainStationResponse;
import com.trainreservation.backend.dto.response.TrainAvailabilityResponse;
import com.trainreservation.backend.exception.GlobalExceptionHandler;
import com.trainreservation.backend.exception.TrainNotFoundException;
import com.trainreservation.backend.repository.UserRepository;
import com.trainreservation.backend.security.ApiAccessDeniedHandler;
import com.trainreservation.backend.security.ApiAuthenticationEntryPoint;
import com.trainreservation.backend.security.JwtAuthenticationFilter;
import com.trainreservation.backend.security.JwtService;
import com.trainreservation.backend.security.WebSecurityConfiguration;
import com.trainreservation.backend.service.TrainAvailabilityService;
import com.trainreservation.backend.service.TrainService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TrainController.class)
@Import({
        WebSecurityConfiguration.class,
        JwtAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        GlobalExceptionHandler.class
})
class TrainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrainService trainService;

    @MockitoBean
    private TrainAvailabilityService trainAvailabilityService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void trainSearchIsPublicAndReturnsApiResponse() throws Exception {
        when(trainService.searchTrains("NDLS", "CNB", LocalDate.of(2026, 12, 1), null))
                .thenReturn(List.of(searchResponse()));

        mockMvc.perform(get("/api/trains/search")
                        .param("source", "NDLS")
                        .param("destination", "CNB")
                        .param("date", "2026-12-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].trainRunId").value(100))
                .andExpect(jsonPath("$.data[0].trainNumber").value("12001"))
                .andExpect(jsonPath("$.data[0].sourceStation.stationCode").value("NDLS"));
    }

    @Test
    void malformedSearchDateReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/trains/search")
                        .param("source", "NDLS")
                        .param("destination", "CNB")
                        .param("date", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void missingTrainDetailsReturnsNotFound() throws Exception {
        when(trainService.getTrainDetails(999L)).thenThrow(new TrainNotFoundException(999L));

        mockMvc.perform(get("/api/trains/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void availabilityEndpointIsPublicAndReturnsCalculatedAvailability() throws Exception {
        when(trainAvailabilityService.getAvailability(
                1L, "NDLS", "CNB", LocalDate.of(2026, 11, 20), "SL"))
                .thenReturn(new TrainAvailabilityResponse(
                        1L,
                        100L,
                        "12001",
                        "Example Express",
                        new TrainStationResponse(1L, "NDLS", "New Delhi"),
                        new TrainStationResponse(2L, "CNB", "Kanpur Central"),
                        LocalDate.of(2026, 11, 20),
                        1,
                        3,
                        "SL",
                        new BigDecimal("180.50"),
                        100,
                        75,
                        new BigDecimal("450.00"),
                        "INR"));

        mockMvc.perform(get("/api/trains/1/availability")
                        .param("source", "NDLS")
                        .param("destination", "CNB")
                        .param("date", "2026-11-20")
                        .param("classCode", "SL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.trainRunId").value(100))
                .andExpect(jsonPath("$.data.totalSeats").value(100))
                .andExpect(jsonPath("$.data.availableSeats").value(75))
                .andExpect(jsonPath("$.data.farePerPassenger").value(450.0));
    }

    private TrainSearchResponse searchResponse() {
        return new TrainSearchResponse(
                1L,
                100L,
                "12001",
                "Example Express",
                LocalDate.of(2026, 12, 1),
                new TrainStationResponse(1L, "NDLS", "New Delhi"),
                new TrainStationResponse(2L, "CNB", "Kanpur Central"),
                LocalDateTime.of(2026, 12, 1, 23, 10),
                LocalDateTime.of(2026, 12, 2, 4, 0),
                290,
                new BigDecimal("180.50"),
                List.of());
    }
}
