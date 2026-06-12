package cn.pumluda.domain.agent.service.rewrite;

/**
 * 查询改写接口 —— 将用户口语化问题转为更利于检索的表述
 */
public interface IQueryRewriter {

    /**
     * 改写查询
     *
     * @param rawQuery 用户原始输入
     * @return 改写后的检索文本，失败时返回原始输入
     */
    String rewrite(String rawQuery);

}
