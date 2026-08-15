package dev.blackholemax.backend.service.impl;

import dev.blackholemax.backend.entity.VisitRecord;
import dev.blackholemax.backend.repository.VisitRecordRepository;
import dev.blackholemax.backend.service.VisitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 访问统计服务实现。
 */
@Service
public class VisitServiceImpl implements VisitService {

    private static final Logger log = LoggerFactory.getLogger(VisitServiceImpl.class);

    private final VisitRecordRepository visitRecordRepository;

    public VisitServiceImpl(VisitRecordRepository visitRecordRepository) {
        this.visitRecordRepository = visitRecordRepository;
    }

    /**
     * 异步记录一次访问（PV），不影响页面渲染。
     */
    @Override
    @Async
    public void recordVisit(Long portfolioId, String visitorId) {
        try {
            VisitRecord record = new VisitRecord();
            record.setPortfolioId(portfolioId);
            record.setVisitorId(visitorId);
            visitRecordRepository.save(record);
        } catch (Exception e) {
            log.warn("记录访问失败：{}", e.getMessage());
        }
    }
}
