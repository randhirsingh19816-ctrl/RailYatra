package com.trainreservation.backend.controller;

import com.trainreservation.backend.dto.response.TrainPageResponse;
import com.trainreservation.backend.dto.response.TrainResponse;
import com.trainreservation.backend.enums.TrainType;
import com.trainreservation.backend.exception.DuplicateTrainNumberException;
import com.trainreservation.backend.exception.GlobalExceptionHandler;
import com.trainreservation.backend.exception.TrainNotFoundException;
import com.trainreservation.backend.repository.UserRepository;
import com.trainreservation.backend.security.ApiAccessDeniedHandler;
import com.trainreservation.backend.security.ApiAuthenticationEntryPoint;
import com.trainreservation.backend.security.JwtAuthenticationFilter;
import com.trainreservation.backend.security.JwtService;
import com.trainreservation.backend.security.WebSecurityConfiguration;
import com.trainreservation.backend.service.TrainManagementService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminTrainController.class)
@Import({
        WebSecurityConfiguration.class,
        JwtAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        GlobalExceptionHandler.class
})
class AdminTrainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrainManagementService trainManagementService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void anonymousUserCannotAccessTrainAdministration() throws Exception {
        mockMvc.perform(get("/api/admin/trains"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void regularUserCannotAccessTrainAdministration() throws Exception {
        mockMvc.perform(get("/api/admin/trains"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateTrainAndReceivesCreatedResponse() throws Exception {
        when(trainManagementService.createTrain(any())).thenReturn(train(true));

        mockMvc.perform(post("/api/admin/trains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.trainNumber").value("T12951"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidTrainNumberAndNameAreRejected() throws Exception {
        mockMvc.perform(post("/api/admin/trains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"trainNumber":"129 51","trainName":"Mumbai Rajdhani","trainType":"EXPRESS"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(post("/api/admin/trains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"trainNumber":"12951","trainName":" ","trainType":"EXPRESS"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void duplicateTrainNumberReturnsConflict() throws Exception {
        when(trainManagementService.createTrain(any()))
                .thenThrow(new DuplicateTrainNumberException("12951"));

        mockMvc.perform(post("/api/admin/trains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanListAndSearchTrainsWithPaginationAndActiveFilter() throws Exception {
        when(trainManagementService.getTrains(1, 5, "trainName", "desc", false))
                .thenReturn(new TrainPageResponse(List.of(train(false)), 1, 5, 6, 2));
        when(trainManagementService.searchTrains("Rajdhani", 0, 10, "trainName", "asc"))
                .thenReturn(new TrainPageResponse(List.of(train(true)), 0, 10, 1, 1));

        mockMvc.perform(get("/api/admin/trains")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sortBy", "trainName")
                        .param("direction", "desc")
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.trains[0].active").value(false));
        mockMvc.perform(get("/api/admin/trains/search").param("keyword", "Rajdhani"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trains[0].trainName").value("Mumbai Rajdhani"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanRetrieveUpdateAndDeactivateTrainAndUnknownTrainReturnsNotFound() throws Exception {
        when(trainManagementService.getTrainById(7L)).thenReturn(train(true));
        when(trainManagementService.updateTrain(eq(7L), any())).thenReturn(train(true));
        when(trainManagementService.deactivateTrain(7L)).thenReturn(train(false));
        when(trainManagementService.getTrainById(999L)).thenThrow(new TrainNotFoundException(999L));

        mockMvc.perform(get("/api/admin/trains/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trainNumber").value("T12951"));
        mockMvc.perform(put("/api/admin/trains/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trainName").value("Mumbai Rajdhani"));
        mockMvc.perform(patch("/api/admin/trains/7/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
        mockMvc.perform(get("/api/admin/trains/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    private String validRequest() {
        return """
                {"trainNumber":"t12951","trainName":"Mumbai Rajdhani","trainType":"EXPRESS"}
                """;
    }

    private TrainResponse train(boolean active) {
        return new TrainResponse(7L, "T12951", "Mumbai Rajdhani", TrainType.EXPRESS, active);
    }
}
