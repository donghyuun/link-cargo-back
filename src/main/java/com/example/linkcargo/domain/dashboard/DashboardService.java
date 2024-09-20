package com.example.linkcargo.domain.dashboard;

import com.example.linkcargo.domain.cargo.Cargo;
import com.example.linkcargo.domain.cargo.CargoRepository;
import com.example.linkcargo.domain.dashboard.dto.response.DashboardNewsResponse;
import com.example.linkcargo.domain.dashboard.dto.response.DashboardPortCongestionResponse;
import com.example.linkcargo.domain.dashboard.dto.response.DashboardPredictionReasonResponse;
import com.example.linkcargo.domain.dashboard.dto.response.DashboardPredictionReasonResponse.PredictionReason;
import com.example.linkcargo.domain.dashboard.dto.response.DashboardPredictionResponse;
import com.example.linkcargo.domain.dashboard.dto.response.DashboardQuotationCompareResponse;
import com.example.linkcargo.domain.dashboard.dto.response.DashboardQuotationResponse;
import com.example.linkcargo.domain.dashboard.dto.response.DashboardRawQuotationResponse;
import com.example.linkcargo.domain.dashboard.dto.response.DashboardRecommendationResponse;
import com.example.linkcargo.domain.forwarding.Forwarding;
import com.example.linkcargo.domain.forwarding.ForwardingRepository;
import com.example.linkcargo.domain.news.News;
import com.example.linkcargo.domain.news.NewsRepository;
import com.example.linkcargo.domain.port.Port;
import com.example.linkcargo.domain.port.PortRepository;
import com.example.linkcargo.domain.prediction.Prediction;
import com.example.linkcargo.domain.prediction.PredictionRepository;
import com.example.linkcargo.domain.quotation.Quotation;
import com.example.linkcargo.domain.quotation.QuotationCalculationService;
import com.example.linkcargo.domain.quotation.QuotationRepository;
import com.example.linkcargo.domain.quotation.QuotationStatus;
import com.example.linkcargo.domain.quotation.dto.response.QuotationInfoResponse;
import com.example.linkcargo.domain.schedule.Schedule;
import com.example.linkcargo.domain.schedule.ScheduleRepository;
import com.example.linkcargo.domain.user.User;
import com.example.linkcargo.domain.user.UserRepository;
import com.example.linkcargo.global.response.code.resultCode.ErrorStatus;
import com.example.linkcargo.global.response.exception.handler.CargoHandler;
import com.example.linkcargo.global.response.exception.handler.PortHandler;
import com.example.linkcargo.global.response.exception.handler.QuotationHandler;
import com.example.linkcargo.global.response.exception.handler.ScheduleHandler;
import com.example.linkcargo.global.response.exception.handler.UsersHandler;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final QuotationRepository quotationRepository;
    private final CargoRepository cargoRepository;
    private final ScheduleRepository scheduleRepository;
    private final ForwardingRepository forwardingRepository;
    private final UserRepository userRepository;
    private final PredictionRepository predictionRepository;
    private final PortRepository portRepository;
    private final NewsRepository newsRepository;
    private final QuotationCalculationService quotationCalculationService;
    private final OpenAiService openAiService;



    public Integer convertToInteger(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    public DashboardRawQuotationResponse getRawQuotations(Long userId) {
        List<Quotation> quotations = quotationRepository.findByConsignorIdAndQuotationStatus(userId.toString(), QuotationStatus.RAW_SHEET);

        LocalDate currentDate = LocalDate.now();

        List<DashboardRawQuotationResponse.RawQuotationInfo> rawQuotationInfoList = quotations.stream()
            .filter(quotation -> !quotation.getCost().getCargoIds().isEmpty())
            .map(quotation -> {
                String firstCargoId = quotation.getCost().getCargoIds().get(0);
                Cargo cargo = cargoRepository.findById(firstCargoId).orElseThrow(() ->
                    new CargoHandler(ErrorStatus.CARGO_NOT_FOUND));
                Port exportPort = portRepository.findById(cargo.getExportPortId())
                    .orElseThrow(() -> new PortHandler(ErrorStatus.EXPORT_PORT_NOT_FOUND));
                Port importPort = portRepository.findById(cargo.getImportPortId())
                    .orElseThrow(() -> new PortHandler(ErrorStatus.IMPORT_PORT_NOT_FOUND));
                return DashboardRawQuotationResponse.RawQuotationInfo.fromEntity(quotation, cargo, exportPort, importPort);
            })
            .filter(info -> info.ETD().isAfter(currentDate))
            .collect(Collectors.toList());

        return DashboardRawQuotationResponse.fromEntity(rawQuotationInfoList);
    }

    public DashboardQuotationResponse getTheCheapestQuotation(String rawQuotationId) {
        List<Quotation> quotations
            = quotationRepository.findQuotationsByRawQuotationIdAndQuotationStatus(rawQuotationId, QuotationStatus.DETAIL_INFO);

        Quotation lowestCostQuotation = quotations.stream()
            .min(Comparator.comparing(quotation -> quotation.getCost().getTotalCost()))
            .orElseThrow(() -> new QuotationHandler(ErrorStatus.QUOTATION_NOT_FOUND));


        Schedule schedule = scheduleRepository.findById(
                Long.valueOf(lowestCostQuotation.getFreight().getScheduleId()))
            .orElseThrow(()-> new ScheduleHandler(ErrorStatus.SCHEDULE_NOT_FOUND));

        QuotationInfoResponse quotationInfoResponse = QuotationInfoResponse.fromEntity(lowestCostQuotation, schedule);

        User user = userRepository.findById(Long.valueOf(lowestCostQuotation.getForwarderId()))
            .orElseThrow(()-> new UsersHandler(ErrorStatus.USER_NOT_FOUND));

        BigDecimal totalCost = (lowestCostQuotation.getCost().getTotalCost()
            .setScale(1, RoundingMode.HALF_UP));

        return DashboardQuotationResponse.fromEntity(user, quotationInfoResponse, totalCost);
    }

    public DashboardQuotationCompareResponse getQuotationsForComparing(String rawQuotationId) {
        List<Quotation> quotations
            = quotationRepository.findQuotationsByRawQuotationIdAndQuotationStatus(rawQuotationId, QuotationStatus.DETAIL_INFO);

        List<DashboardQuotationResponse> dashboardQuotationResponses = quotations.stream()
            .map(quotation -> {

                Schedule schedule = scheduleRepository.findById(
                        Long.valueOf(quotation.getFreight().getScheduleId()))
                    .orElseThrow(() -> new ScheduleHandler(ErrorStatus.SCHEDULE_NOT_FOUND));

                QuotationInfoResponse quotationInfoResponse = QuotationInfoResponse.fromEntity(
                    quotation, schedule);

                User user = userRepository.findById(Long.valueOf(quotation.getForwarderId()))
                    .orElseThrow(() -> new UsersHandler(ErrorStatus.USER_NOT_FOUND));

                BigDecimal totalCost = quotation.getCost().getTotalCost()
                    .setScale(1, RoundingMode.HALF_UP);

                return DashboardQuotationResponse.fromEntity(user, quotationInfoResponse,
                    totalCost);

            })
            .toList();

        List<Map<String, Integer>> thcCostList = new ArrayList<>();
        List<Map<String, Integer>> handlingCostList = new ArrayList<>();
        List<Map<String, Integer>> cfsCostList = new ArrayList<>();
        List<Map<String, Integer>> liftStatusCostList = new ArrayList<>();
        List<Map<String, Integer>> customsClearanceCostList = new ArrayList<>();
        List<Map<String, Integer>> truckingCostList = new ArrayList<>();

        for (Quotation quotation : quotations) {
            Quotation.ChargeExport chargeExport = quotation.getCost().getChargeExport();
            String forwarderId = quotation.getForwarderId();
            User user = userRepository.findById(Long.valueOf(forwarderId))
                .orElseThrow(() -> new UsersHandler(ErrorStatus.USER_NOT_FOUND));

            Forwarding forwarding = user.getForwarding();
            String forwardingFirmName = forwarding.getFirmName();

            thcCostList.add(
                Map.of(forwardingFirmName, convertToInteger(chargeExport.getTHC().getLCL())));
            handlingCostList.add(Map.of(forwardingFirmName,
                convertToInteger(chargeExport.getHANDLING_FEE().getLCL())));
            cfsCostList.add(Map.of(forwardingFirmName,
                convertToInteger(chargeExport.getCFS_CHARGE().getLCL())));
            liftStatusCostList.add(Map.of(forwardingFirmName,
                convertToInteger(chargeExport.getLIFT_STATUS().getLCL())));
            customsClearanceCostList.add(Map.of(forwardingFirmName,
                convertToInteger(chargeExport.getCUSTOMS_CLEARANCE_FEE().getLCL())));
            truckingCostList.add(
                Map.of(forwardingFirmName, convertToInteger(chargeExport.getTRUCKING().getLCL())));

        }

        Map<String, List<Map<String, Integer>>> compareCostMap = Map.of(
            "thcCost", thcCostList,
            "handlingCost", handlingCostList,
            "cfsCost", cfsCostList,
            "liftStatusCost", liftStatusCostList,
            "customsClearanceCost", customsClearanceCostList,
            "truckingCost", truckingCostList
        );

        return DashboardQuotationCompareResponse.fromEntity(dashboardQuotationResponses, compareCostMap);
    }

    public DashboardPredictionResponse getPredictionInfo(Long exportPortId, Long importPortId) {
        LocalDate today = LocalDate.now();

        int currentYear = today.getYear();
        int currentMonth = today.getMonthValue();

        LocalDate sixMonthsLater = today.plusMonths(6);
        int endYear = sixMonthsLater.getYear();
        int endMonth = sixMonthsLater.getMonthValue();

        List<Prediction> predictions = predictionRepository.findPredictionsWithinPeriod(
            currentYear, currentMonth, endYear, endMonth);

        Map<Pair<String,String>,Integer> predictionList = predictions.stream()
            .collect(Collectors.toMap(
                prediction -> Pair.of(
                    String.valueOf(prediction.getYear()),
                    String.valueOf(prediction.getMonth())
                ),
                prediction -> Integer.parseInt(prediction.getFreightCostIndex()),
                (v1, v2) -> v1));

        Port exportPort = portRepository.findById(exportPortId)
            .orElseThrow(() -> new PortHandler(ErrorStatus.EXPORT_PORT_NOT_FOUND));
        Port importPort = portRepository.findById(importPortId)
            .orElseThrow(() -> new PortHandler(ErrorStatus.IMPORT_PORT_NOT_FOUND));

        return DashboardPredictionResponse.fromEntity(exportPort.getName(), importPort.getName(), predictionList);
    }

    // todo
    // API 호출 시간 문제
    public DashboardPredictionReasonResponse getPredictionReasonInfo() {
        LocalDate today = LocalDate.now();

        int currentYear = today.getYear();
        int currentMonth = today.getMonthValue();

        LocalDate sixMonthsLater = today.plusMonths(6);
        int endYear = sixMonthsLater.getYear();
        int endMonth = sixMonthsLater.getMonthValue();

        List<Prediction> predictions = predictionRepository.findPredictionsWithinPeriod(
            currentYear, currentMonth, endYear, endMonth);


        predictions.sort((p1, p2) -> {
            if (!Objects.equals(p1.getYear(), p2.getYear())) {
                return Integer.compare(p1.getYear(), p2.getYear());
            }
            return Integer.compare(p1.getMonth(), p2.getMonth());
        });

        List<PredictionReason> predictionReasons = IntStream.range(0, predictions.size() - 1)
            .mapToObj(i -> {
                Prediction current = predictions.get(i);
                Prediction next = predictions.get(i + 1);

                String status = Integer.parseInt(next.getFreightCostIndex()) > Integer.parseInt(current.getFreightCostIndex()) ? "rising" : "falling";

                Map<String, String> currentDate = Map.of(
                    "year", String.valueOf(current.getYear()),
                    "month", String.valueOf(current.getMonth())
                );
                Map<String, String> nextDate = Map.of(
                    "year", String.valueOf(next.getYear()),
                    "month", String.valueOf(next.getMonth())
                );

                String prompt = String.format(
                    "해운 운임 지수가 %s년 %s월부터 %s년 %s월 사이에 %s하고 있습니다. " +
                        "이전 월의 지수는 %s이고, 다음 월의 지수는 %s입니다. " +
                        "이러한 변화의 가능한 이유를 50단어 이내로 설명해주세요. " +
                        "국제 무역, 경제 상황, 연료 가격, 선박 공급량 등의 요인을 고려해 주세요.",
                    current.getYear(), current.getMonth(),
                    next.getYear(), next.getMonth(),
                    status.equals("rising") ? "상승" : "하락",
                    current.getFreightCostIndex(),
                    next.getFreightCostIndex()
                );

                ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                    .model("gpt-4")  // gpt-4 모델 사용
                    .messages(List.of(
                        new ChatMessage("system", "You are a helpful assistant."),
                        new ChatMessage("user", prompt)
                    ))
                    .maxTokens(100)
                    .temperature(0.7)
                    .build();

                String reason = openAiService.createChatCompletion(chatCompletionRequest)
                    .getChoices().get(0).getMessage().getContent().trim();

                return PredictionReason.fromEntity(
                    List.of(currentDate, nextDate),
                    status,
                    reason
                );
            })
            .collect(Collectors.toList());

        return DashboardPredictionReasonResponse.fromEntity(predictionReasons);
    }

    public DashboardPortCongestionResponse getImportPortCongestion(Long importPortId) {
        Port importPort = portRepository.findById(importPortId)
            .orElseThrow(() -> new PortHandler(ErrorStatus.IMPORT_PORT_NOT_FOUND));

        // todo
        // 입국항 이름을 사용하여 혼잡도 조회 API 사용

        // 임시
        Integer congestionPercent = 33;

        String status = null;

        if (congestionPercent >= 0 && congestionPercent <= 20) {
            status = "원활";
        } else if (congestionPercent > 20 && congestionPercent <= 60) {
            status = "보통";
        } else if (congestionPercent > 60 && congestionPercent <= 100) {
            status = "혼잡";
        }

        // 임시
        String description = "항구에 머물고 있는 컨테이너선의 비율이 큽니다. 선박이 대기하는 시간이 길어지고 하역 및 적재 작업이 지연될 수 있습니다.";

        return DashboardPortCongestionResponse.fromEntity(congestionPercent, status, description);
    }

    public DashboardNewsResponse getInterestingNews(List<String> interests) {
        LocalDate today = LocalDate.now();
        List<List<News>> newsList = interests.stream()
            .map(query ->  newsRepository.findByCategoryAndCreatedDate(query, today))
            .toList();

        String newsContents = newsList.stream()
            .flatMap(List::stream)
            .map(News::getContent)
            .collect(Collectors.joining(" "));

        // todo
        // 요약 API를 통한 요약

        // 임시
        String summary = "뉴스 요약 정보";

        return DashboardNewsResponse.fromEntity(interests, summary);

    }

    public DashboardRecommendationResponse getRecommendationInfoByCost(String rawQuotationId) {
        LocalDate today = LocalDate.now();

        int currentYear = today.getYear();
        int currentMonth = today.getMonthValue();

        LocalDate sixMonthsLater = today.plusMonths(6);
        int endYear = sixMonthsLater.getYear();
        int endMonth = sixMonthsLater.getMonthValue();

        List<Prediction> predictions = predictionRepository.findPredictionsWithinPeriod(
            currentYear, currentMonth, endYear, endMonth);

        Prediction todayMonthPrediction = predictionRepository.findByMonthAndYear(currentMonth, currentYear);

        Prediction minFreightCostPrediction = predictions.stream()
            .min(Comparator.comparingInt(p -> Integer.parseInt(p.getFreightCostIndex())))
            .get();

        // Prediction의 년도와 월을 이용해 YearMonth 객체 생성
        YearMonth predictionYearMonth = YearMonth.of(
            minFreightCostPrediction.getYear(),
            minFreightCostPrediction.getMonth()
        );

        YearMonth currentYearMonth = YearMonth.from(today);

        // 현재 날짜와 Prediction 날짜 사이의 기간 계산
        long monthsDifference = ChronoUnit.MONTHS.between(currentYearMonth, predictionYearMonth);
        System.out.println(minFreightCostPrediction.getFreightCostIndex());

        // 총 개월 수 계산
        Integer dateDifference = (int) monthsDifference;

        // 운임 비용 차이
        Integer indexDifference =
            Integer.parseInt(todayMonthPrediction.getFreightCostIndex()) - Integer.parseInt(
                minFreightCostPrediction.getFreightCostIndex());

        // 해당 화주가 요청한 견적서에 해당하는 알고리즘에 의해 계산된 견적서
        Quotation quotation
            = quotationRepository.findQuotationsByRawQuotationIdAndQuotationStatus(
                rawQuotationId,
                QuotationStatus.PREDICTION_SHEET
        ).get(0);

        // 알고리즘에 의한 견적서를 기반으로 비용 계산
        BigDecimal estimatedCost = quotationCalculationService.calculateTotalCost(quotation,
            Integer.valueOf(minFreightCostPrediction.getFreightCostIndex()));

        List<Schedule> schedules = scheduleRepository.findSchedulesByYearMonth(
            predictionYearMonth.getYear(), predictionYearMonth.getMonthValue());
        return DashboardRecommendationResponse.fromEntity(dateDifference, indexDifference,
            estimatedCost, schedules);
    }

}
