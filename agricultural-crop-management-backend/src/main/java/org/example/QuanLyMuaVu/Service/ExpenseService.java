package org.example.QuanLyMuaVu.Service;

import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.DTO.Request.ExpenseRequest;
import org.example.QuanLyMuaVu.DTO.Response.ExpenseResponse;
import org.example.QuanLyMuaVu.Entity.Expense;
import org.example.QuanLyMuaVu.Entity.Season;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Repository.ExpenseRepository;
import org.example.QuanLyMuaVu.Repository.SeasonRepository;
import org.example.QuanLyMuaVu.Repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final SeasonRepository seasonRepository;

    // ---------------------------
    // CREATE
    // ---------------------------
    public ExpenseResponse createExpense(ExpenseRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Season season = seasonRepository.findById(request.getSeasonId())
                .orElseThrow(() -> new RuntimeException("Season not found"));

        Expense expense = Expense.builder()
                .user(user)
                .season(season)
                .itemName(request.getItemName())
                .unitPrice(request.getUnitPrice())
                .quantity(request.getQuantity())
                .totalCost(request.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity())))
                .expenseDate(request.getExpenseDate())
                .createdAt(LocalDateTime.now())
                .build();

        return mapToResponse(expenseRepository.save(expense));
    }

    // ---------------------------
    // GET by ID
    // ---------------------------
    public ExpenseResponse getExpenseById(Integer id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        return mapToResponse(expense);
    }

    // ---------------------------
    // GET ALL
    // ---------------------------
    public List<ExpenseResponse> getAllExpenses() {
        return expenseRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ---------------------------
    // SEARCH by name
    // ---------------------------
    public List<ExpenseResponse> searchExpensesByName(String name) {
        return expenseRepository.findByItemNameContainingIgnoreCase(name)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ---------------------------
    // UPDATE
    // ---------------------------
    public ExpenseResponse updateExpense(Integer id, ExpenseRequest request) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Season season = seasonRepository.findById(request.getSeasonId())
                .orElseThrow(() -> new RuntimeException("Season not found"));

        expense.setUser(user);
        expense.setSeason(season);
        expense.setItemName(request.getItemName());
        expense.setUnitPrice(request.getUnitPrice());
        expense.setQuantity(request.getQuantity());
        expense.setTotalCost(request.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
        expense.setExpenseDate(request.getExpenseDate());

        return mapToResponse(expenseRepository.save(expense));
    }

    // ---------------------------
    // DELETE
    // ---------------------------
    public void deleteExpense(Integer id) {
        if (!expenseRepository.existsById(id)) {
            throw new RuntimeException("Expense not found");
        }
        expenseRepository.deleteById(id);
    }

    // ---------------------------
    // PRIVATE MAPPER
    // ---------------------------
    private ExpenseResponse mapToResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .userName(expense.getUser().getUsername())
                .seasonName(expense.getSeason().getSeasonName())
                .itemName(expense.getItemName())
                .unitPrice(expense.getUnitPrice())
                .quantity(expense.getQuantity())
                .totalCost(expense.getTotalCost())
                .expenseDate(expense.getExpenseDate())
                .createdAt(expense.getCreatedAt())
                .build();
    }
}
