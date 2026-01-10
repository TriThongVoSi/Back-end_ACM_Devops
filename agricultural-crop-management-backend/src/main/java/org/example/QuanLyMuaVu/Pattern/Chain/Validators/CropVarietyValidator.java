package org.example.QuanLyMuaVu.Pattern.Chain.Validators;

import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.Entity.Crop;
import org.example.QuanLyMuaVu.Entity.Variety;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Pattern.Chain.SeasonValidationContext;
import org.example.QuanLyMuaVu.Pattern.Chain.ValidationHandler;
import org.example.QuanLyMuaVu.Repository.CropRepository;
import org.example.QuanLyMuaVu.Repository.VarietyRepository;
import org.springframework.stereotype.Component;

/**
 * Chain of Responsibility: Crop and Variety Validator.
 * <p>
 * Validates:
 * 1. Crop exists
 * 2. If variety is provided, it belongs to the specified crop
 * <p>
 * Also populates Crop and Variety entities in context for downstream use.
 */
@Component
@RequiredArgsConstructor
public class CropVarietyValidator extends ValidationHandler<SeasonValidationContext> {

    private final CropRepository cropRepository;
    private final VarietyRepository varietyRepository;

    @Override
    protected void doValidate(SeasonValidationContext ctx) {
        if (ctx.getCropId() == null) {
            throw new AppException(ErrorCode.CROP_NOT_FOUND);
        }

        Crop crop = cropRepository.findById(ctx.getCropId())
                .orElseThrow(() -> new AppException(ErrorCode.CROP_NOT_FOUND));
        ctx.setCrop(crop);

        // Validate variety if provided
        if (ctx.getVarietyId() != null) {
            Variety variety = varietyRepository.findById(ctx.getVarietyId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

            // Ensure variety belongs to the specified crop
            if (variety.getCrop() == null || !variety.getCrop().getId().equals(crop.getId())) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }

            ctx.setVariety(variety);
        }
    }

    @Override
    public String getValidatorName() {
        return "CropVarietyValidator";
    }
}
