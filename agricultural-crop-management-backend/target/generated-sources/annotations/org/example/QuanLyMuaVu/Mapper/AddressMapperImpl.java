package org.example.QuanLyMuaVu.Mapper;

import javax.annotation.processing.Generated;
import org.example.QuanLyMuaVu.DTO.Response.ProvinceResponse;
import org.example.QuanLyMuaVu.DTO.Response.WardResponse;
import org.example.QuanLyMuaVu.Entity.Province;
import org.example.QuanLyMuaVu.Entity.Ward;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-27T19:31:04+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.9 (Microsoft)"
)
@Component
public class AddressMapperImpl implements AddressMapper {

    @Override
    public ProvinceResponse toProvinceResponse(Province province) {
        if ( province == null ) {
            return null;
        }

        ProvinceResponse.ProvinceResponseBuilder provinceResponse = ProvinceResponse.builder();

        provinceResponse.id( province.getId() );
        provinceResponse.name( province.getName() );
        provinceResponse.slug( province.getSlug() );
        provinceResponse.type( province.getType() );
        provinceResponse.nameWithType( province.getNameWithType() );

        return provinceResponse.build();
    }

    @Override
    public WardResponse toWardResponse(Ward ward) {
        if ( ward == null ) {
            return null;
        }

        WardResponse.WardResponseBuilder wardResponse = WardResponse.builder();

        wardResponse.provinceId( wardProvinceId( ward ) );
        wardResponse.id( ward.getId() );
        wardResponse.name( ward.getName() );
        wardResponse.slug( ward.getSlug() );
        wardResponse.type( ward.getType() );
        wardResponse.nameWithType( ward.getNameWithType() );

        return wardResponse.build();
    }

    private Integer wardProvinceId(Ward ward) {
        if ( ward == null ) {
            return null;
        }
        Province province = ward.getProvince();
        if ( province == null ) {
            return null;
        }
        Integer id = province.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
