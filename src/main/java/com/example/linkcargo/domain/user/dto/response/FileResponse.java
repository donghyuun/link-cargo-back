package com.example.linkcargo.domain.user.dto.response;


import com.example.linkcargo.global.s3.dto.FileDTO;

public record FileResponse(
    FileDTO file
) {

}