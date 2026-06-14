package cn.pumluda.trigger.controller;

import cn.pumluda.api.dto.BaguGenerateRequest;
import cn.pumluda.api.dto.BaguSetResponse;
import cn.pumluda.api.response.Response;
import cn.pumluda.domain.bagu.IBaguSkillService;
import cn.pumluda.types.enums.ResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/bagu")
@RequiredArgsConstructor
public class BaguController {

    private final IBaguSkillService baguSkillService;

    @PostMapping("/generate")
    public Response<BaguSetResponse> generate(@RequestBody BaguGenerateRequest request) {
        log.info("[BaguController] 收到生成请求: shelf={}, docs={}", request.getShelfName(), request.getDocumentIds().size());
        BaguSetResponse result = baguSkillService.generate(request.getShelfName(), request.getDocumentIds());
        return Response.<BaguSetResponse>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info("生成成功")
                .data(result)
                .build();
    }
}
