package cn.pumluda.trigger.controller;

import cn.pumluda.api.dto.BaguGenerateRequest;
import cn.pumluda.api.dto.BaguSetResponse;
import cn.pumluda.api.response.Response;
import cn.pumluda.domain.bagu.IBaguSkillService;
import cn.pumluda.types.enums.ResponseCode;
import java.util.List;
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

    @GetMapping("/sets")
    public Response<List<BaguSetResponse>> listSets() {
        return Response.<List<BaguSetResponse>>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .data(baguSkillService.listSets())
                .build();
    }

    @GetMapping("/sets/{id}")
    public Response<BaguSetResponse> getSet(@PathVariable("id") String id) {
        return Response.<BaguSetResponse>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .data(baguSkillService.getSet(id))
                .build();
    }

    @PostMapping("/evaluate")
    public Response<String> evaluate(@RequestBody java.util.Map<String, String> body) {
        String result = baguSkillService.evaluate(
            body.get("question"), body.get("standardAnswer"), body.get("userAnswer"));
        return Response.<String>builder().code(ResponseCode.SUCCESS.getCode()).data(result).build();
    }
}
