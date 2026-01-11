package org.example.QuanLyMuaVu.Mapper;

import javax.annotation.processing.Generated;
import org.example.QuanLyMuaVu.DTO.Request.FarmerCreationRequest;
import org.example.QuanLyMuaVu.DTO.Request.FarmerUpdateRequest;
import org.example.QuanLyMuaVu.DTO.Response.FarmerResponse;
import org.example.QuanLyMuaVu.Entity.Province;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Entity.Ward;
import org.example.QuanLyMuaVu.Enums.UserStatus;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-11T18:00:21+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class FarmerMapperImpl implements FarmerMapper {

    @Override
    public User toUser(FarmerCreationRequest request) {
        if ( request == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.username( request.getUsername() );
        user.password( request.getPassword() );

        return user.build();
    }

    @Override
    public FarmerResponse toFarmerResponse(User user) {
        if ( user == null ) {
            return null;
        }

        FarmerResponse.FarmerResponseBuilder farmerResponse = FarmerResponse.builder();

        farmerResponse.id( user.getId() );
        farmerResponse.username( user.getUsername() );
        farmerResponse.email( user.getEmail() );
        farmerResponse.fullName( user.getFullName() );
        farmerResponse.phone( user.getPhone() );
        farmerResponse.status( userStatusCode( user ) );
        Integer id = userProvinceId( user );
        if ( id != null ) {
            farmerResponse.provinceId( id.longValue() );
        }
        farmerResponse.provinceName( userProvinceName( user ) );
        Integer id1 = userWardId( user );
        if ( id1 != null ) {
            farmerResponse.wardId( id1.longValue() );
        }
        farmerResponse.wardName( userWardName( user ) );
        farmerResponse.joinedDate( user.getJoinedDate() );

        return farmerResponse.build();
    }

    @Override
    public void updateUser(User user, FarmerUpdateRequest request) {
        if ( request == null ) {
            return;
        }

        user.setPassword( request.getPassword() );
    }

    private String userStatusCode(User user) {
        if ( user == null ) {
            return null;
        }
        UserStatus status = user.getStatus();
        if ( status == null ) {
            return null;
        }
        String code = status.getCode();
        if ( code == null ) {
            return null;
        }
        return code;
    }

    private Integer userProvinceId(User user) {
        if ( user == null ) {
            return null;
        }
        Province province = user.getProvince();
        if ( province == null ) {
            return null;
        }
        Integer id = province.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String userProvinceName(User user) {
        if ( user == null ) {
            return null;
        }
        Province province = user.getProvince();
        if ( province == null ) {
            return null;
        }
        String name = province.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    private Integer userWardId(User user) {
        if ( user == null ) {
            return null;
        }
        Ward ward = user.getWard();
        if ( ward == null ) {
            return null;
        }
        Integer id = ward.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String userWardName(User user) {
        if ( user == null ) {
            return null;
        }
        Ward ward = user.getWard();
        if ( ward == null ) {
            return null;
        }
        String name = ward.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }
}
