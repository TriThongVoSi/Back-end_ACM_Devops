package org.example.QuanLyMuaVu.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Constant.PredefinedRole;
import org.example.QuanLyMuaVu.DTO.Response.SearchResponse;
import org.example.QuanLyMuaVu.DTO.Response.SearchResultItemResponse;
import org.example.QuanLyMuaVu.Entity.Document;
import org.example.QuanLyMuaVu.Entity.Expense;
import org.example.QuanLyMuaVu.Entity.Farm;
import org.example.QuanLyMuaVu.Entity.Plot;
import org.example.QuanLyMuaVu.Entity.Role;
import org.example.QuanLyMuaVu.Entity.Season;
import org.example.QuanLyMuaVu.Entity.Task;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Enums.SearchEntityType;
import org.example.QuanLyMuaVu.Repository.DocumentRepository;
import org.example.QuanLyMuaVu.Repository.ExpenseRepository;
import org.example.QuanLyMuaVu.Repository.FarmRepository;
import org.example.QuanLyMuaVu.Repository.PlotRepository;
import org.example.QuanLyMuaVu.Repository.SeasonRepository;
import org.example.QuanLyMuaVu.Repository.TaskRepository;
import org.example.QuanLyMuaVu.Repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GlobalSearchService {

    private static final int MAX_LIMIT = 20;

    private static final List<SearchEntityType> FARMER_TYPES = List.of(
            SearchEntityType.PLOT,
            SearchEntityType.SEASON,
            SearchEntityType.TASK,
            SearchEntityType.EXPENSE,
            SearchEntityType.DOCUMENT
    );

    private static final List<SearchEntityType> ADMIN_TYPES = List.of(
            SearchEntityType.FARM,
            SearchEntityType.PLOT,
            SearchEntityType.SEASON,
            SearchEntityType.DOCUMENT,
            SearchEntityType.USER
    );

    FarmAccessService farmAccessService;
    PlotRepository plotRepository;
    SeasonRepository seasonRepository;
    TaskRepository taskRepository;
    ExpenseRepository expenseRepository;
    DocumentRepository documentRepository;
    FarmRepository farmRepository;
    UserRepository userRepository;

    @Transactional(readOnly = true)
    public SearchResponse search(String q, Set<SearchEntityType> requestedTypes, int limit) {
        String keyword = q == null ? "" : q.trim();
        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));

        User currentUser = farmAccessService.getCurrentUser();
        boolean isAdmin = isAdmin(currentUser);
        List<SearchEntityType> allowedTypes = isAdmin ? ADMIN_TYPES : FARMER_TYPES;
        List<SearchEntityType> effectiveTypes = resolveTypes(requestedTypes, allowedTypes);

        List<Integer> farmIds = isAdmin ? Collections.emptyList()
                : farmAccessService.getAccessibleFarmIdsForCurrentUser();

        List<SearchResultItemResponse> results = new ArrayList<>();
        Map<String, Integer> grouped = new LinkedHashMap<>();

        for (SearchEntityType type : effectiveTypes) {
            List<SearchResultItemResponse> items = switch (type) {
                case FARM -> isAdmin ? searchFarms(keyword, safeLimit) : List.of();
                case PLOT -> searchPlots(keyword, safeLimit, isAdmin, farmIds);
                case SEASON -> searchSeasons(keyword, safeLimit, isAdmin, farmIds);
                case DOCUMENT -> searchDocuments(keyword, safeLimit, isAdmin);
                case USER -> isAdmin ? searchUsers(keyword, safeLimit) : List.of();
                case TASK -> !isAdmin ? searchTasks(keyword, safeLimit, currentUser) : List.of();
                case EXPENSE -> !isAdmin ? searchExpenses(keyword, safeLimit, currentUser) : List.of();
            };
            results.addAll(items);
            grouped.put(type.name(), items.size());
        }

        return SearchResponse.builder()
                .q(keyword)
                .limit(safeLimit)
                .results(results)
                .grouped(grouped)
                .build();
    }

    private List<SearchResultItemResponse> searchFarms(String keyword, int limit) {
        Page<Farm> farms = farmRepository.findByNameContainingIgnoreCase(keyword, PageRequest.of(0, limit));
        return farms.getContent().stream()
                .map(farm -> SearchResultItemResponse.builder()
                        .type(SearchEntityType.FARM.name())
                        .id(farm.getId() != null ? farm.getId().longValue() : null)
                        .title(farm.getName() != null ? farm.getName() : buildFallbackTitle("Farm", farm.getId()))
                        .subtitle(buildFarmSubtitle(farm))
                        .route(buildRoute("/admin/farms-plots", params(
                                "tab", "farms",
                                "farmId", farm.getId()
                        )))
                        .extra(buildExtra(params("farmId", farm.getId())))
                        .build())
                .toList();
    }

    private List<SearchResultItemResponse> searchPlots(String keyword, int limit, boolean isAdmin, List<Integer> farmIds) {
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by("plotName").ascending());
        Page<Plot> page;
        if (isAdmin) {
            page = plotRepository.findByPlotNameContainingIgnoreCase(keyword, pageRequest);
        } else {
            if (farmIds.isEmpty()) {
                return List.of();
            }
            page = plotRepository.findByFarm_IdInAndPlotNameContainingIgnoreCase(farmIds, keyword, pageRequest);
        }

        return page.getContent().stream()
                .map(plot -> SearchResultItemResponse.builder()
                        .type(SearchEntityType.PLOT.name())
                        .id(plot.getId() != null ? plot.getId().longValue() : null)
                        .title(plot.getPlotName() != null ? plot.getPlotName() : buildFallbackTitle("Plot", plot.getId()))
                        .subtitle(buildPlotSubtitle(plot))
                        .route(isAdmin
                                ? buildRoute("/admin/farms-plots", params(
                                        "tab", "plots",
                                        "plotId", plot.getId(),
                                        "farmId", plot.getFarm() != null ? plot.getFarm().getId() : null
                                ))
                                : buildRoute("/farmer/plots", params(
                                        "plotId", plot.getId()
                                )))
                        .extra(buildExtra(params(
                                "farmId", plot.getFarm() != null ? plot.getFarm().getId() : null
                        )))
                        .build())
                .toList();
    }

    private List<SearchResultItemResponse> searchSeasons(String keyword, int limit, boolean isAdmin, List<Integer> farmIds) {
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by("seasonName").ascending());
        Page<Season> page;
        if (isAdmin) {
            page = seasonRepository.findBySeasonNameContainingIgnoreCase(keyword, pageRequest);
        } else {
            if (farmIds.isEmpty()) {
                return List.of();
            }
            page = seasonRepository.findByPlot_Farm_IdInAndSeasonNameContainingIgnoreCase(farmIds, keyword, pageRequest);
        }

        return page.getContent().stream()
                .map(season -> SearchResultItemResponse.builder()
                        .type(SearchEntityType.SEASON.name())
                        .id(season.getId() != null ? season.getId().longValue() : null)
                        .title(season.getSeasonName() != null ? season.getSeasonName() : buildFallbackTitle("Season", season.getId()))
                        .subtitle(buildSeasonSubtitle(season))
                        .route(isAdmin
                                ? buildRoute("/admin/farms-plots", params(
                                        "tab", "plots",
                                        "plotId", season.getPlot() != null ? season.getPlot().getId() : null,
                                        "seasonId", season.getId()
                                ))
                                : buildRoute("/farmer/seasons", params(
                                        "seasonId", season.getId()
                                )))
                        .extra(buildExtra(params(
                                "plotId", season.getPlot() != null ? season.getPlot().getId() : null,
                                "farmId", season.getPlot() != null && season.getPlot().getFarm() != null
                                        ? season.getPlot().getFarm().getId()
                                        : null
                        )))
                        .build())
                .toList();
    }

    private List<SearchResultItemResponse> searchTasks(String keyword, int limit, User user) {
        Page<Task> page = taskRepository.findByUserAndTitleContainingIgnoreCase(
                user,
                keyword,
                PageRequest.of(0, limit, Sort.by("dueDate").ascending())
        );

        return page.getContent().stream()
                .map(task -> SearchResultItemResponse.builder()
                        .type(SearchEntityType.TASK.name())
                        .id(task.getId() != null ? task.getId().longValue() : null)
                        .title(task.getTitle() != null ? task.getTitle() : buildFallbackTitle("Task", task.getId()))
                        .subtitle(buildTaskSubtitle(task))
                        .route(buildRoute("/farmer/tasks", params(
                                "q", task.getTitle(),
                                "seasonId", task.getSeason() != null ? task.getSeason().getId() : null
                        )))
                        .extra(buildExtra(params(
                                "seasonId", task.getSeason() != null ? task.getSeason().getId() : null
                        )))
                        .build())
                .toList();
    }

    private List<SearchResultItemResponse> searchExpenses(String keyword, int limit, User user) {
        Page<Expense> page = expenseRepository.findByUser_IdAndItemNameContainingIgnoreCaseAndDeletedAtIsNull(
                user.getId(),
                keyword,
                PageRequest.of(0, limit, Sort.by("expenseDate").descending())
        );

        return page.getContent().stream()
                .map(expense -> SearchResultItemResponse.builder()
                        .type(SearchEntityType.EXPENSE.name())
                        .id(expense.getId() != null ? expense.getId().longValue() : null)
                        .title(expense.getItemName() != null ? expense.getItemName() : buildFallbackTitle("Expense", expense.getId()))
                        .subtitle(buildExpenseSubtitle(expense))
                        .route(buildRoute("/farmer/expenses", params(
                                "expenseId", expense.getId(),
                                "seasonId", expense.getSeason() != null ? expense.getSeason().getId() : null
                        )))
                        .extra(buildExtra(params(
                                "seasonId", expense.getSeason() != null ? expense.getSeason().getId() : null,
                                "plotId", expense.getSeason() != null && expense.getSeason().getPlot() != null
                                        ? expense.getSeason().getPlot().getId()
                                        : null
                        )))
                        .build())
                .toList();
    }

    private List<SearchResultItemResponse> searchDocuments(String keyword, int limit, boolean isAdmin) {
        Page<Document> page = isAdmin
                ? documentRepository.findByTitleContainingIgnoreCase(keyword, PageRequest.of(0, limit))
                : documentRepository.findAllVisible(keyword, null, null, null, PageRequest.of(0, limit));

        String baseRoute = isAdmin ? "/admin/documents" : "/farmer/documents";
        return page.getContent().stream()
                .map(doc -> SearchResultItemResponse.builder()
                        .type(SearchEntityType.DOCUMENT.name())
                        .id(doc.getId() != null ? doc.getId().longValue() : null)
                        .title(doc.getTitle() != null ? doc.getTitle() : buildFallbackTitle("Document", doc.getId()))
                        .subtitle(buildDocumentSubtitle(doc))
                        .route(buildRoute(baseRoute, params(
                                "documentId", doc.getId()
                        )))
                        .extra(buildExtra(params("documentId", doc.getId())))
                        .build())
                .toList();
    }

    private List<SearchResultItemResponse> searchUsers(String keyword, int limit) {
        Page<User> page = userRepository.searchByKeyword(keyword, PageRequest.of(0, limit));
        return page.getContent().stream()
                .map(user -> SearchResultItemResponse.builder()
                        .type(SearchEntityType.USER.name())
                        .id(user.getId())
                        .title(resolveUserTitle(user))
                        .subtitle(resolveUserSubtitle(user))
                        .route(buildRoute("/admin/users-roles", params(
                                "tab", "users",
                                "userId", user.getId()
                        )))
                        .extra(buildExtra(params("userId", user.getId())))
                        .build())
                .toList();
    }

    private List<SearchEntityType> resolveTypes(Set<SearchEntityType> requestedTypes,
                                                List<SearchEntityType> allowedTypes) {
        if (requestedTypes == null || requestedTypes.isEmpty()) {
            return allowedTypes;
        }
        List<SearchEntityType> resolved = new ArrayList<>();
        for (SearchEntityType type : allowedTypes) {
            if (requestedTypes.contains(type)) {
                resolved.add(type);
            }
        }
        return resolved;
    }

    private boolean isAdmin(User user) {
        if (user == null || user.getRoles() == null) {
            return false;
        }
        for (Role role : user.getRoles()) {
            if (role != null && PredefinedRole.ADMIN_ROLE.equalsIgnoreCase(role.getCode())) {
                return true;
            }
        }
        return false;
    }

    private String resolveUserTitle(User user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return buildFallbackTitle("User", user.getId());
    }

    private String resolveUserSubtitle(User user) {
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return null;
    }

    private String buildPlotSubtitle(Plot plot) {
        List<String> parts = new ArrayList<>();
        if (plot.getFarm() != null && plot.getFarm().getName() != null) {
            parts.add("Farm: " + plot.getFarm().getName());
        }
        String area = formatArea(plot.getArea());
        if (area != null) {
            parts.add(area);
        }
        return joinParts(parts);
    }

    private String buildSeasonSubtitle(Season season) {
        List<String> parts = new ArrayList<>();
        if (season.getPlot() != null && season.getPlot().getPlotName() != null) {
            parts.add("Plot: " + season.getPlot().getPlotName());
        }
        if (season.getPlot() != null && season.getPlot().getFarm() != null && season.getPlot().getFarm().getName() != null) {
            parts.add("Farm: " + season.getPlot().getFarm().getName());
        }
        return joinParts(parts);
    }

    private String buildTaskSubtitle(Task task) {
        List<String> parts = new ArrayList<>();
        if (task.getSeason() != null && task.getSeason().getSeasonName() != null) {
            parts.add("Season: " + task.getSeason().getSeasonName());
        }
        if (task.getDueDate() != null) {
            parts.add("Due: " + task.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        return joinParts(parts);
    }

    private String buildExpenseSubtitle(Expense expense) {
        List<String> parts = new ArrayList<>();
        if (expense.getSeason() != null && expense.getSeason().getSeasonName() != null) {
            parts.add("Season: " + expense.getSeason().getSeasonName());
        }
        BigDecimal amount = expense.getEffectiveAmount();
        if (amount != null) {
            parts.add("Amount: " + formatAmount(amount));
        }
        return joinParts(parts);
    }

    private String buildDocumentSubtitle(Document doc) {
        List<String> parts = new ArrayList<>();
        if (doc.getTopic() != null && !doc.getTopic().isBlank()) {
            parts.add(doc.getTopic());
        }
        if (doc.getCrop() != null && !doc.getCrop().isBlank()) {
            parts.add(doc.getCrop());
        }
        if (doc.getStage() != null && !doc.getStage().isBlank()) {
            parts.add(doc.getStage());
        }
        if (parts.isEmpty() && doc.getDocumentType() != null) {
            parts.add(doc.getDocumentType().name());
        }
        return joinParts(parts);
    }

    private String buildFarmSubtitle(Farm farm) {
        List<String> parts = new ArrayList<>();
        if (farm.getUser() != null && farm.getUser().getUsername() != null) {
            parts.add("Owner: " + farm.getUser().getUsername());
        }
        String area = formatArea(farm.getArea());
        if (area != null) {
            parts.add(area);
        }
        return joinParts(parts);
    }

    private String buildFallbackTitle(String prefix, Object id) {
        if (id == null) {
            return prefix;
        }
        return prefix + " #" + id;
    }

    private String joinParts(List<String> parts) {
        if (parts.isEmpty()) {
            return null;
        }
        return String.join(" | ", parts);
    }

    private String formatArea(BigDecimal area) {
        if (area == null) {
            return null;
        }
        return area.stripTrailingZeros().toPlainString() + " ha";
    }

    private String formatAmount(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }

    private String buildRoute(String basePath, Map<String, Object> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(basePath);
        if (params != null) {
            params.forEach((key, value) -> {
                if (value != null && value.toString().length() > 0) {
                    builder.queryParam(key, value);
                }
            });
        }
        return builder.build().encode().toUriString();
    }

    private Map<String, Object> params(Object... keyValues) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (keyValues == null || keyValues.length == 0) {
            return params;
        }
        for (int i = 0; i < keyValues.length - 1; i += 2) {
            String key = String.valueOf(keyValues[i]);
            Object value = keyValues[i + 1];
            params.put(key, value);
        }
        return params;
    }

    private Map<String, Object> buildExtra(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return null;
        }
        Map<String, Object> extra = new LinkedHashMap<>();
        params.forEach((key, value) -> {
            if (value != null) {
                extra.put(key, value);
            }
        });
        return extra.isEmpty() ? null : extra;
    }
}
