package cn.pumluda.domain.bagu;

import cn.pumluda.api.dto.BaguSetResponse;
import java.util.List;

public interface IBaguSkillService {
    BaguSetResponse generate(String shelfName, List<String> documentIds);
    List<BaguSetResponse> listSets();
    BaguSetResponse getSet(String id);
}

