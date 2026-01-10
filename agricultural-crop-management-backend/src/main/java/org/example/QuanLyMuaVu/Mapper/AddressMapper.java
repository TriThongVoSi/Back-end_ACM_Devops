package org.example.QuanLyMuaVu.Mapper;

import org.example.QuanLyMuaVu.DTO.Response.ProvinceResponse;
import org.example.QuanLyMuaVu.DTO.Response.WardResponse;
import org.example.QuanLyMuaVu.Entity.Province;
import org.example.QuanLyMuaVu.Entity.Ward;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for address entities (Province, Ward).
 */
@Mapper(componentModel = "spring")
public interface AddressMapper {

    /**
     * Map Province entity to ProvinceResponse DTO.
     */
    ProvinceResponse toProvinceResponse(Province province);

    /**
     * Map Ward entity to WardResponse DTO.
     */
    @Mapping(target = "provinceId", source = "province.id")
    WardResponse toWardResponse(Ward ward);
}
