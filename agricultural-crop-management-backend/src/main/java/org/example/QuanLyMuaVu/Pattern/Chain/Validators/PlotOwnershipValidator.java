package org.example.QuanLyMuaVu.Pattern.Chain.Validators;

import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.Entity.Plot;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Pattern.Chain.SeasonValidationContext;
import org.example.QuanLyMuaVu.Pattern.Chain.ValidationHandler;
import org.example.QuanLyMuaVu.Repository.PlotRepository;
import org.example.QuanLyMuaVu.Service.FarmAccessService;
import org.springframework.stereotype.Component;

/**
 * Chain of Responsibility: Plot Ownership Validator.
 * <p>
 * Validates that:
 * 1. The plot exists
 * 2. Current user has access to the plot (via farm ownership)
 * <p>
 * Also populates the Plot entity in the context for downstream validators.
 */
@Component
@RequiredArgsConstructor
public class PlotOwnershipValidator extends ValidationHandler<SeasonValidationContext> {

    private final PlotRepository plotRepository;
    private final FarmAccessService farmAccessService;

    @Override
    protected void doValidate(SeasonValidationContext ctx) {
        if (ctx.getPlotId() == null) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        Plot plot = plotRepository.findById(ctx.getPlotId())
                .orElseThrow(() -> new AppException(ErrorCode.PLOT_NOT_FOUND));

        // Verify ownership using existing FarmAccessService
        farmAccessService.assertCurrentUserCanAccessPlot(plot);

        // Populate context for downstream validators
        ctx.setPlot(plot);
    }

    @Override
    public String getValidatorName() {
        return "PlotOwnershipValidator";
    }
}
