package dev.blackholemax.backend.service;

/**
 * 访问统计服务：异步记录作品集访问（PV/UV）。
 */
public interface VisitService {

    /** 异步记录一次访问（PV），不影响页面渲染 */
    void recordVisit(Long portfolioId, String visitorId);
}
