package com.example.linkcargo.global.response.code.resultCode;

import com.example.linkcargo.global.response.code.BaseErrorCode;
import com.example.linkcargo.global.response.code.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

// Enum Naming Format : {주체}_{이유}
// Message format : 동사 명사형으로 마무리
@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {
    // Global
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GLOBAL501", "서버 오류"),


    // User
    USER_EXISTS_EMAIL(HttpStatus.BAD_REQUEST, "USER401", "중복된 이메일입니다."),
    USER_EXISTS_BUSINESS_NUMBER(HttpStatus.BAD_REQUEST, "USER402", "중복된 사업자번호입니다."),
    USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "USER403", "해당 정보의 유저를 찾을 수 없습니다."),
    USER_PROFILE_UPLOAD_FAIL(HttpStatus.INTERNAL_SERVER_ERROR,"USER404",  "프로필 업로드에 실패했습니다."),

    // JWT
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401", "서명이 잘못된 엑세스 토큰입니다."),
    MALFORMED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH402", "형식이 잘못된 엑세스 토큰입니다."),
    EXPIRED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH403", "만료된 엑세스 토큰입니다."),
    UNSUPPORTED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH404", "서버가 처리할 수 없는 형식의 엑세스 토큰입니다"),
    ILLEGAL_ARGUMENT_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH405", "잘못된 값이 포함된 엑세스 토큰입니다."),
    PREMATURE_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH406", "아직 사용이 가능하지 않은 엑세스 토큰입니다."),

    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH407", "서명이 잘못된 리프레시 토큰입니다."),
    MALFORMED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH408", "형식이 잘못된 리프레시 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH409", "만료된 리프레시 토큰입니다."),
    UNSUPPORTED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH4010", "서버가 처리할 수 없는 형식의 리프레시 토큰입니다"),
    ILLEGAL_ARGUMENT_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH4011", "잘못된 값이 포함된 리프레시 토큰입니다."),
    PREMATURE_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH4012", "아직 사용이 가능하지 않은 리프레시 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH4013", "DB의 리프레시 토큰과 일치하지 않는 리프레시 토큰입니다."),

    // Cargo
    INVALID_CARGO_INPUT(HttpStatus.BAD_REQUEST, "CARGO401", "유효하지 않은 CARGO 입력 정보입니다."),
    CARGO_NOT_FOUND(HttpStatus.NOT_FOUND, "CARGO402", "해당 ID 의 CARGO 가 존재하지 않습니다."),
    CARGO_USER_NOT_MATCH(HttpStatus.BAD_REQUEST, "CARGO403", "해당 사용자의 화물이 아닙니다."),

    // Schedule
    SCHEDULE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "SCHEDULE401", "이미 존재하는 선박 스케줄 입니다."),
    SCHEDULE_CREATED_FAIL(HttpStatus.NOT_FOUND, "SCHEDULE402", "선박 스케줄 생성에 실패하였습니다."),
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "SCHEDULE403   ", "선박 스케줄이 존재 하지 않습니다.."),
    SCHEDULE_UPDATED_FAIL(HttpStatus.NOT_FOUND, "SCHEDULE404", "선박 스케줄 변경에 실패하였습니다"),
    SCHEDULE_DELETED_FAIL(HttpStatus.NOT_FOUND, "SCHEDULE405", "선박 스케줄 삭제에 실패하였습니다"),

    // Port
    IMPORT_PORT_NOT_FOUND(HttpStatus.NOT_FOUND, "PORT401", "존재 하지 않는 수입항 입니다."),
    EXPORT_PORT_NOT_FOUND(HttpStatus.NOT_FOUND, "PORT402", "존재 하지 않는 수출항 입니다."),
    PORT_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "PORT403", "이미 존재하는 항구 입니다."),
    PORT_CREATED_FAIL(HttpStatus.NOT_FOUND, "PORT404", "항구 생성에 실패하였습니다."),
    PORT_NOT_FOUND(HttpStatus.NOT_FOUND, "PORT405", "항구가 존재 하지 않습니다.."),
    PORT_UPDATED_FAIL(HttpStatus.NOT_FOUND, "PORT406","항구 변경에 실패하였습니다"),
    PORT_DELETED_FAIL(HttpStatus.NOT_FOUND, "PORT407","항구 삭제에 실패하였습니다"),

    // Forwarding
    FORWARDING_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "FORWARDING401", "이미 존재하는 포워딩 업체 입니다."),
    FORWARDING_CREATED_FAIL(HttpStatus.NOT_FOUND, " FORWARDING402", "포워딩 업체 생성에 실패하였습니다."),
    FORWARDING_NOT_FOUND(HttpStatus.NOT_FOUND, "FORWARDING403", "포워딩 업체가 존재 하지 않습니다."),
    FORWARDING_UPDATED_FAIL(HttpStatus.NOT_FOUND, "FORWARDING404","포워딩 업체 변경에 실패하였습니다"),
    FORWARDING_DELETED_FAIL(HttpStatus.NOT_FOUND, "FORWARDING405","포워딩 업체 삭제에 실패하였습니다"),
    NOT_FORWARDER(HttpStatus.BAD_REQUEST, "FORWARDING406","포워더 역활이 아닙니다."),

    // FCMToken
    FCM_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "FCM_TOKEN401", "FCM 토큰이 존재하지 않습니다."),

    // Notification
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION401", "알림이 존재 하지 않습니다."),


    // S3
    S3_FILE_NAME_ERROR(HttpStatus.BAD_REQUEST, "S3401","잘못된 형식의 파일입니다."),

    // Image
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "IMAGE401", "파일이 존재하지 않습니다."),
    IMAGE_UPLOAD_FAIL(HttpStatus.BAD_REQUEST, "IMAGE402", "이미지 업로드에 실패하였습니다."),

    // Quotation
    QUOTATION_DUPLICATE(HttpStatus.BAD_REQUEST, "QUOTATION401","이미 동일한 견적서가 존재합니다."),
    QUOTATION_NOT_FOUND(HttpStatus.NOT_FOUND, "QUOTATION402", "견적서가 존재하지 않습니다."),
    QUOTATION_UPDATED_FAIL(HttpStatus.NOT_FOUND, "QUOTATION403","포워더 견적서 업데이트에 실패하였습니다"),

    // CHAT
    CHAT_FILE_UPLOAD_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "CHAT401","파일 업로드에 살패했습니다."),

    // ETC
    EXTERNAL_API_ERROR(HttpStatus.BAD_REQUEST, "ETC401", "외부 API 호출 오류"),

    // Prediction
    PREDICTION_NOT_FOUND(HttpStatus.NOT_FOUND, "PREDICTION401", "해당 년,월의 운임지수가 존재하지 않습니다.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDto getReason() {
        return ErrorReasonDto.builder()
            .message(message)
            .code(code)
            .isSuccess(false)
            .build();
    }

    @Override
    public ErrorReasonDto getReasonHttpStatus() {
        return ErrorReasonDto.builder()
            .message(message)
            .code(code)
            .isSuccess(false)
            .httpStatus(httpStatus)
            .build();
    }
}
