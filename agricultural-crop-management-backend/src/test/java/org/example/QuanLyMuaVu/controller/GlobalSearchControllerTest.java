package org.example.QuanLyMuaVu.controller;

import org.example.QuanLyMuaVu.Controller.GlobalSearchController;
import org.example.QuanLyMuaVu.DTO.Response.SearchResponse;
import org.example.QuanLyMuaVu.DTO.Response.SearchResultItemResponse;
import org.example.QuanLyMuaVu.Enums.SearchEntityType;
import org.example.QuanLyMuaVu.Repository.UserRepository;
import org.example.QuanLyMuaVu.Service.GlobalSearchService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalSearchController.class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GlobalSearchService globalSearchService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void search_rejectsMissingQuery() throws Exception {
        mockMvc.perform(get("/api/v1/search").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ERR_BAD_REQUEST"));

        verifyNoInteractions(globalSearchService);
    }

    @Test
    void search_rejectsShortQuery() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("q", "a")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ERR_BAD_REQUEST"));

        verifyNoInteractions(globalSearchService);
    }

    @Test
    void search_parsesTypesAndReturnsResults() throws Exception {
        SearchResultItemResponse item = SearchResultItemResponse.builder()
                .type("PLOT")
                .id(12L)
                .title("Plot A1")
                .subtitle("Farm: Binh Minh")
                .route("/farmer/plots?plotId=12")
                .extra(Map.of("farmId", 2))
                .build();

        Map<String, Integer> grouped = new LinkedHashMap<>();
        grouped.put("PLOT", 1);
        grouped.put("DOCUMENT", 0);

        SearchResponse response = SearchResponse.builder()
                .q("plot")
                .limit(5)
                .results(List.of(item))
                .grouped(grouped)
                .build();

        when(globalSearchService.search(eq("plot"), anySet(), eq(5))).thenReturn(response);

        mockMvc.perform(get("/api/v1/search")
                        .param("q", "plot")
                        .param("types", "plot,documents")
                        .param("limit", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.q").value("plot"))
                .andExpect(jsonPath("$.result.results[0].type").value("PLOT"))
                .andExpect(jsonPath("$.result.results[0].route").value("/farmer/plots?plotId=12"));

        ArgumentCaptor<Set<SearchEntityType>> captor = ArgumentCaptor.forClass(Set.class);
        verify(globalSearchService).search(eq("plot"), captor.capture(), eq(5));
        assertTrue(captor.getValue().contains(SearchEntityType.PLOT));
        assertTrue(captor.getValue().contains(SearchEntityType.DOCUMENT));
    }
}
