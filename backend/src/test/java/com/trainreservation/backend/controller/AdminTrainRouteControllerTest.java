package com.trainreservation.backend.controller;

import com.trainreservation.backend.dto.response.TrainRouteResponse;
import com.trainreservation.backend.dto.response.ManagedTrainRouteStopResponse;
import com.trainreservation.backend.exception.GlobalExceptionHandler;
import com.trainreservation.backend.exception.TrainRouteConflictException;
import com.trainreservation.backend.exception.TrainRouteNotFoundException;
import com.trainreservation.backend.repository.UserRepository;
import com.trainreservation.backend.security.ApiAccessDeniedHandler;
import com.trainreservation.backend.security.ApiAuthenticationEntryPoint;
import com.trainreservation.backend.security.JwtAuthenticationFilter;
import com.trainreservation.backend.security.JwtService;
import com.trainreservation.backend.security.WebSecurityConfiguration;
import com.trainreservation.backend.service.TrainRouteManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminTrainRouteController.class)
@Import({
        WebSecurityConfiguration.class,
        JwtAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        GlobalExceptionHandler.class
})
class AdminTrainRouteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrainRouteManagementService routeManagementService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void anonymousUserCannotAccessTrainRoutes() throws Exception {
        mockMvc.perform(get("/api/admin/trains/5/route"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void regularUserCannotAccessTrainRoutes() throws Exception {
        mockMvc.perform(get("/api/admin/trains/5/route"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateRetrieveUpdateAddUpdateAndRemoveStops() throws Exception {
        TrainRouteResponse response = route();
        when(routeManagementService.createRoute(eq(5L), any())).thenReturn(response);
        when(routeManagementService.getRoute(5L)).thenReturn(response);
        when(routeManagementService.updateRoute(eq(5L), any())).thenReturn(response);
        when(routeManagementService.addStop(eq(5L), any())).thenReturn(response);
        when(routeManagementService.updateStop(eq(5L), eq(51L), any())).thenReturn(response);
        when(routeManagementService.removeStop(5L, 51L)).thenReturn(response);

        mockMvc.perform(post("/api/admin/trains/5/route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(routeRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.trainId").value(5))
                .andExpect(jsonPath("$.data.stops[0].id").value(51))
                .andExpect(jsonPath("$.data.stops[0].stationCode").value("NDLS"));
        mockMvc.perform(get("/api/admin/trains/5/route"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stops[1].stopNumber").value(2));
        mockMvc.perform(put("/api/admin/trains/5/route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(routeRequest()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/trains/5/route/stops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stop":{"stationCode":"BRC","stopNumber":3,"arrivalTime":"12:00:00",
                                "departureTime":null,"dayNumber":1,"distanceFromSource":200},
                                "previousStopDepartureTime":"10:05:00"}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(put("/api/admin/trains/5/route/stops/51")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stationCode":"NDLS","stopNumber":1,"arrivalTime":null,
                                "departureTime":"08:00:00","dayNumber":1,"distanceFromSource":0}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/trains/5/route/stops/51"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void routeRequestValidationRejectsFewerThanTwoStops() throws Exception {
        mockMvc.perform(post("/api/admin/trains/5/route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stops":[{"stationCode":"NDLS","stopNumber":1,"arrivalTime":null,
                                "departureTime":"08:00:00","dayNumber":1,"distanceFromSource":0}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void routeNotFoundReturnsNotFoundAndUnsafeChangeReturnsConflict() throws Exception {
        when(routeManagementService.getRoute(99L)).thenThrow(new TrainRouteNotFoundException(99L));
        when(routeManagementService.removeStop(5L, 51L))
                .thenThrow(new TrainRouteConflictException("Train route is referenced by scheduled runs"));

        mockMvc.perform(get("/api/admin/trains/99/route"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
        mockMvc.perform(delete("/api/admin/trains/5/route/stops/51"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    private String routeRequest() {
        return """
                {"stops":[
                  {"stationCode":"NDLS","stopNumber":1,"arrivalTime":null,"departureTime":"08:00:00",
                   "dayNumber":1,"distanceFromSource":0},
                  {"stationCode":"MMCT","stopNumber":2,"arrivalTime":"10:00:00","departureTime":null,
                   "dayNumber":1,"distanceFromSource":100}
                ]}
                """;
    }

    private TrainRouteResponse route() {
        return new TrainRouteResponse(5L, "T100", List.of(
                new ManagedTrainRouteStopResponse(51L, 11L, "NDLS", "New Delhi", 1, 1,
                        null, LocalTime.of(8, 0), BigDecimal.ZERO),
                new ManagedTrainRouteStopResponse(52L, 12L, "MMCT", "Mumbai Central", 2, 1,
                        LocalTime.of(10, 0), null, new BigDecimal("100"))));
    }
}
