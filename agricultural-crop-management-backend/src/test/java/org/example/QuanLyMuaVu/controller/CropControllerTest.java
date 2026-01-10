package org.example.QuanLyMuaVu.controller;

import org.example.QuanLyMuaVu.Controller.CropController;
import org.example.QuanLyMuaVu.DTO.Request.CropRequest;
import org.example.QuanLyMuaVu.DTO.Response.CropResponse;
import org.example.QuanLyMuaVu.Service.CropService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CropController.class)
class CropControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CropService cropService;

    @Test
    void list_returnsCrops() throws Exception {
        when(cropService.getAll()).thenReturn(List.of(
                CropResponse.builder().id(1).cropName("Rice").build(),
                CropResponse.builder().id(2).cropName("Corn").build()
        ));

        mockMvc.perform(get("/api/v1/crops").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(2)))
                .andExpect(jsonPath("$.result[0].cropName").value("Rice"));
    }
}
