package com.example.linkcargo.domain.cargo;

import com.example.linkcargo.domain.cargo.dto.request.CargoRequest;
import com.example.linkcargo.domain.cargo.dto.request.CargosRequest;
import com.example.linkcargo.domain.cargo.dto.response.CargoIdsResponse;
import com.example.linkcargo.domain.cargo.dto.response.CargoPageResponse;
import com.example.linkcargo.domain.cargo.dto.response.CargoResponse;
import com.example.linkcargo.global.response.ApiResponse;
import com.example.linkcargo.global.response.code.resultCode.SuccessStatus;
import com.example.linkcargo.global.security.CustomUserDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "2. Cargo", description = "화물 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cargos")
public class CargoController {

    private final CargoService cargoService;

    @Operation(summary = "화물 여러 개 추가", description = "화물 정보를 여러 개 추가합니다. CargosRequest 사용")
    @PostMapping
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "OK, 성공")
    })
    public ApiResponse<CargoIdsResponse> createCargos(
        @Valid @RequestBody CargosRequest cargosRequest,
        @AuthenticationPrincipal CustomUserDetail userDetail
    ) {
        CargoIdsResponse cargoIdsResponse = cargoService.createCargos(userDetail.getId(), cargosRequest);
        return ApiResponse.onSuccess(cargoIdsResponse);
    }

    @Operation(summary = "화물 조회", description = "특정 화물을 조회합니다.")
    @GetMapping("/{cargoId}")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "OK, 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "CARGO402", description = "해당 ID 의 CARGO 가 존재하지 않습니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    })
    public ApiResponse<CargoResponse> getCargo(@PathVariable("cargoId") String cargoId) {
        CargoResponse cargoResponse = cargoService.getCargo(cargoId);
        return ApiResponse.onSuccess(cargoResponse);
    }

    @Operation(summary = "나의 화물 목록 조회 - 페이징", description = "내가 추가한 화물의 목록을 조회합니다.")
    @GetMapping("/all")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "OK, 성공")
    })
    public ApiResponse<CargoPageResponse> getMyCargos(
        @Parameter(description = "페이지 번호") @RequestParam(value = "page", defaultValue = "0") int page,
        @Parameter(description = "페이지 크기") @RequestParam(value = "size", defaultValue = "10") int size,
        @Parameter(description = "정렬 기준") @RequestParam(value = "sort", defaultValue = "updatedAt_desc") String sort,
        @AuthenticationPrincipal CustomUserDetail userDetail
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, getSortObject(sort));
        CargoPageResponse cargoPageResponse = cargoService.getMyCargos(userDetail.getId(),
            pageRequest);
        return ApiResponse.onSuccess(cargoPageResponse);
    }

    @Operation(summary = "나의 화물 수정", description = "나의 화물을 수정합니다. CargoRequest 사용")
    @PutMapping("/{cargoId}")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "OK, 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "CARGO402", description = "해당 ID 의 CARGO 가 존재하지 않습니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "CARGO403", description = "해당 사용자의 화물이 아닙니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    })
    public ApiResponse<String> updateMyCargo(
        @PathVariable("cargoId") String cargoId,
        @RequestBody CargoRequest cargoRequest,
        @AuthenticationPrincipal CustomUserDetail userDetail
    ) {
        Cargo udpatedCargo = cargoService.updateMyCargo(userDetail.getId(), cargoId, cargoRequest);
        return ApiResponse.onSuccess(udpatedCargo.getId());
    }

    @Operation(summary = "나의 화물 삭제", description = "나의 화물을 삭제합니다.")
    @DeleteMapping("{cargoId}")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "OK, 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "CARGO402", description = "해당 ID 의 CARGO 가 존재하지 않습니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "CARGO403", description = "해당 사용자의 화물이 아닙니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    })
    public ApiResponse<SuccessStatus> deleteMyCargo(
        @PathVariable("cargoId") String cargoId,
        @AuthenticationPrincipal CustomUserDetail userDetail
    ) {
        cargoService.deleteMyCargo(userDetail.getId(), cargoId);
        return ApiResponse.onSuccess(SuccessStatus._OK);
    }

    @Operation(summary = "화물의 예상비용 조회", description = "화주가 화물만 입력했을 때 예상 비용을 계산하고 반환합니다.")
    @PostMapping("/calculate")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "OK, 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "CARGO402",description = "해당 ID의 화물이 존재하지 않습니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    })
    public ApiResponse<BigDecimal> calculateRawQuotation(
        @AuthenticationPrincipal CustomUserDetail userDetail,
        @Valid @RequestBody CargosRequest cargosRequest)
    {

        BigDecimal estimatedCost = cargoService.calculateCostByCargos(cargosRequest);
        return ApiResponse.onSuccess(estimatedCost);

    }


    private Sort getSortObject(String sort) {
        switch (sort) {
            case "updatedAt_asc":
                return Sort.by(Sort.Direction.ASC, "updatedAt");
            default:
                return Sort.by(Sort.Direction.DESC, "updatedAt");
        }
    }
}
