package org.example.QuanLyMuaVu.Service;

import org.example.QuanLyMuaVu.Constant.PredefinedRole;
import org.example.QuanLyMuaVu.DTO.Response.SearchResponse;
import org.example.QuanLyMuaVu.DTO.Response.SearchResultItemResponse;
import org.example.QuanLyMuaVu.Entity.Document;
import org.example.QuanLyMuaVu.Entity.Farm;
import org.example.QuanLyMuaVu.Entity.Plot;
import org.example.QuanLyMuaVu.Entity.Role;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Enums.SearchEntityType;
import org.example.QuanLyMuaVu.Repository.DocumentRepository;
import org.example.QuanLyMuaVu.Repository.ExpenseRepository;
import org.example.QuanLyMuaVu.Repository.FarmRepository;
import org.example.QuanLyMuaVu.Repository.PlotRepository;
import org.example.QuanLyMuaVu.Repository.SeasonRepository;
import org.example.QuanLyMuaVu.Repository.TaskRepository;
import org.example.QuanLyMuaVu.Repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalSearchServiceTest {

    @Mock
    private FarmAccessService farmAccessService;

    @Mock
    private PlotRepository plotRepository;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GlobalSearchService globalSearchService;

    @Test
    void search_farmerScopesPlotsToAccessibleFarms() {
        User farmer = buildUserWithRole(PredefinedRole.FARMER_ROLE);
        when(farmAccessService.getCurrentUser()).thenReturn(farmer);
        when(farmAccessService.getAccessibleFarmIdsForCurrentUser()).thenReturn(List.of(1));

        Farm farm = Farm.builder().id(1).name("Farm A").build();
        Plot plot = Plot.builder().id(10).plotName("Plot A").farm(farm).build();

        when(plotRepository.findByFarm_IdInAndPlotNameContainingIgnoreCase(eq(List.of(1)), eq("Plot"),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(plot)));

        SearchResponse response = globalSearchService.search("Plot", EnumSet.of(SearchEntityType.PLOT), 5);

        assertEquals(1, response.getResults().size());
        SearchResultItemResponse item = response.getResults().get(0);
        assertEquals("PLOT", item.getType());
        assertTrue(item.getRoute().contains("/farmer/plots"));

        verify(plotRepository).findByFarm_IdInAndPlotNameContainingIgnoreCase(eq(List.of(1)), eq("Plot"),
                any(Pageable.class));
    }

    @Test
    void search_adminUsesGlobalFarmSearch() {
        User admin = buildUserWithRole(PredefinedRole.ADMIN_ROLE);
        when(farmAccessService.getCurrentUser()).thenReturn(admin);

        Farm farm = Farm.builder().id(2).name("Farm B").build();
        when(farmRepository.findByNameContainingIgnoreCase(eq("Farm"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(farm)));

        SearchResponse response = globalSearchService.search("Farm", EnumSet.of(SearchEntityType.FARM), 5);

        assertEquals(1, response.getResults().size());
        SearchResultItemResponse item = response.getResults().get(0);
        assertEquals("FARM", item.getType());
        assertTrue(item.getRoute().contains("/admin/farms-plots"));

        verify(farmRepository).findByNameContainingIgnoreCase(eq("Farm"), any(Pageable.class));
        verify(plotRepository, never()).findByFarm_IdInAndPlotNameContainingIgnoreCase(any(), any(), any());
    }

    @Test
    void search_typesFilterDocumentsOnly() {
        User farmer = buildUserWithRole(PredefinedRole.FARMER_ROLE);
        when(farmAccessService.getCurrentUser()).thenReturn(farmer);
        when(farmAccessService.getAccessibleFarmIdsForCurrentUser()).thenReturn(List.of(1));

        Document doc = Document.builder().id(5).title("Soil Guide").topic("Soil").build();
        when(documentRepository.findAllVisible(eq("Guide"), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(doc)));

        SearchResponse response = globalSearchService.search("Guide", EnumSet.of(SearchEntityType.DOCUMENT), 5);

        assertEquals(1, response.getResults().size());
        SearchResultItemResponse item = response.getResults().get(0);
        assertEquals("DOCUMENT", item.getType());
        assertNotNull(item.getRoute());

        verify(plotRepository, never()).findByFarm_IdInAndPlotNameContainingIgnoreCase(any(), any(), any());
    }

    private User buildUserWithRole(String roleCode) {
        Role role = Role.builder().code(roleCode).build();
        return User.builder()
                .id(1L)
                .username("user")
                .roles(Set.of(role))
                .build();
    }
}
