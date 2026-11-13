package com.yongyongthon.be.common.swagger;

import com.yongyongthon.be.common.response.ErrorCode;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @ApiErrorCodes가 붙은 API에 오류 응답(400, 404, 500 등)을 Swagger 문서로 추가한다.
 * 같은 상태 코드의 오류가 여러 개면 예시를 코드별로 나눠 보여 준다.
 */
@Component
public class ApiErrorCodesCustomizer implements OperationCustomizer {

    private static final String JSON = "application/json";

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        ApiErrorCodes annotation = handlerMethod.getMethodAnnotation(ApiErrorCodes.class);
        if (annotation == null) {
            return operation;
        }
        Set<ErrorCode> codes = new LinkedHashSet<>(List.of(annotation.value()));
        codes.add(ErrorCode.UNKNOWN_ERROR);

        Map<Integer, List<ErrorCode>> byStatus = new LinkedHashMap<>();
        for (ErrorCode code : codes) {
            byStatus.computeIfAbsent(code.getHttpStatus(), k -> new ArrayList<>()).add(code);
        }

        ApiResponses responses = operation.getResponses() == null ? new ApiResponses() : operation.getResponses();
        byStatus.forEach((status, list) -> {
            MediaType mediaType = new MediaType();
            for (ErrorCode code : list) {
                mediaType.addExamples(code.getCode(), new Example()
                        .summary(code.getCode() + " " + code.getMessage())
                        .value(errorBody(code)));
            }
            String reason = HttpStatus.valueOf(status).getReasonPhrase();
            String description = reason + " (" + String.join(", ", list.stream().map(ErrorCode::getCode).toList()) + ")";
            responses.addApiResponse(String.valueOf(status), new ApiResponse()
                    .description(description)
                    .content(new Content().addMediaType(JSON, mediaType)));
        });
        operation.setResponses(responses);
        return operation;
    }

    private static Map<String, Object> errorBody(ErrorCode code) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code.getCode());
        error.put("message", code.getMessage());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data", null);
        body.put("error", error);
        return body;
    }
}
