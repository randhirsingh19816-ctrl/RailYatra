package com.trainreservation.backend.controller;

import com.trainreservation.backend.dto.response.StationPageResponse;
import com.trainreservation.backend.dto.response.StationResponse;
import com.trainreservation.backend.exception.DuplicateStationCodeException;
import com.trainreservation.backend.exception.GlobalExceptionHandler;
import com.trainreservation.backend.exception.StationNotFoundException;
import com.trainreservation.backend.repository.UserRepository;
import com.trainreservation.backend.security.ApiAccessDeniedHandler;
import com.trainreservation.backend.security.ApiAuthenticationEntryPoint;
import com.trainreservation.backend.security.JwtAuthenticationFilter;
import com.trainreservation.backend.security.JwtService;
import com.trainreservation.backend.security.WebSecurityConfiguration;
import com.trainreservation.backend.service.StationManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminStationController.class)
@Import({
        WebSecurityConfiguration.class,
        JwtAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        GlobalExceptionHandler.class
})
class AdminStationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StationManagementService stationManagementService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void anonymousUserCannotAccessStationAdministration() throws Exception {
        mockMvc.perform(get("/api/admin/stations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void regularUserCannotAccessStationAdministration() throws Exception {
        mockMvc.perform(get("/api/admin/stations"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateStationAndReceivesCreatedResponse() throws Exception {
        when(stationManagementService.createStation(any())).thenReturn(station());

        mockMvc.perform(post("/api/admin/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stationCode").value("NDLS"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidStationNameAndCodeAreRejected() throws Exception {
        mockMvc.perform(post("/api/admin/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stationName":" ","stationCode":"NDLS","city":"New Delhi","state":"Delhi"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(post("/api/admin/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stationName":"New Delhi","stationCode":"N-DLS","city":"New Delhi","state":"Delhi"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void duplicateStationCodeReturnsConflict() throws Exception {
        when(stationManagementService.createStation(any()))
                .thenThrow(new DuplicateStationCodeException("NDLS"));

        mockMvc.perform(post("/api/admin/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanRetrieveSearchAndDeactivateStations() throws Exception {
        when(stationManagementService.getStations(0, 10, "stationName", "asc"))
                .thenReturn(new StationPageResponse(List.of(station()), 0, 10, 1, 1));
        when(stationManagementService.searchStations("Delhi")).thenReturn(List.of(station()));
        when(stationManagementService.deactivateStation(7L))
                .thenReturn(new StationResponse(7L, "New Delhi", "NDLS", "New Delhi", "Delhi", false));

        mockMvc.perform(get("/api/admin/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stations[0].stationCode").value("NDLS"));
        mockMvc.perform(get("/api/admin/stations/search").param("keyword", "Delhi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].stationName").value("New Delhi"));
        mockMvc.perform(patch("/api/admin/stations/7/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanRetrieveAndUpdateStationAndUnknownStationReturnsNotFound() throws Exception {
        when(stationManagementService.getStationById(7L)).thenReturn(station());
        when(stationManagementService.updateStation(org.mockito.ArgumentMatchers.eq(7L), any()))
                .thenReturn(station());
        when(stationManagementService.getStationById(999L))
                .thenThrow(new StationNotFoundException("999"));

        mockMvc.perform(get("/api/admin/stations/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stationName").value("New Delhi"));
        mockMvc.perform(put("/api/admin/stations/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stationCode").value("NDLS"));
        mockMvc.perform(get("/api/admin/stations/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    private String validRequest() {
        return """
                {"stationName":"New Delhi","stationCode":"ndls","city":"New Delhi","state":"Delhi"}
                """;
    }

    private StationResponse station() {
        return new StationResponse(7L, "New Delhi", "NDLS", "New Delhi", "Delhi", true);
    }
}
