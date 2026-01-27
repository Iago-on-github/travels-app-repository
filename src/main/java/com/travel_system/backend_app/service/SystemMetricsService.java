package com.travel_system.backend_app.service;

import com.travel_system.backend_app.model.Travel;
import com.travel_system.backend_app.model.enums.TravelStatus;
import com.travel_system.backend_app.repository.TravelRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class SystemMetricsService {
    private final ThreadPoolExecutor threadPoolExecutor;
    private final RedisTrackingService redisTrackingService;
    private final TravelRepository travelRepository;

    private static final Logger logger = LoggerFactory.getLogger(SystemMetricsService.class);

    public SystemMetricsService(ThreadPoolExecutor threadPoolExecutor, RedisTrackingService redisTrackingService, TravelRepository travelRepository) {
        this.threadPoolExecutor = threadPoolExecutor;
        this.redisTrackingService = redisTrackingService;
        this.travelRepository = travelRepository;
    }

    @Scheduled(fixedRate = 60000)
    public void getExecutorMetrics() {
        // original values
        int MAXIMUM_QUEUE_CAPACITY = 100;
        int CORE_POOL_SIZE = 5;

        int activeCount = threadPoolExecutor.getActiveCount();
        int queueSize = threadPoolExecutor.getQueue().size();
        int poolSize = threadPoolExecutor.getPoolSize();

        int maxQueueEightyPercent = percentCalc(MAXIMUM_QUEUE_CAPACITY, 80);
        int maxQueueFiftyPercent = percentCalc(MAXIMUM_QUEUE_CAPACITY, 50);

        // verficações de sobrecarga
        if (queueSize >= maxQueueEightyPercent) {
            logger.warn("RED ALERT: A fila ultrapassou 80% das tarefas. Prestes a ativar a CallerRunsPolicy");
        } else if (queueSize >= maxQueueFiftyPercent) {
            logger.warn("YELLOW ALERT: A fila ultrapassou 50% das tarefas. Firebase lento ou volume de ônibus cresceu muito");
        } else {
            logger.info("Status: OK.");
        }

        // verificações de elasticidade
        if (poolSize > CORE_POOL_SIZE) {
            logger.warn("poolSize maior que o core. A fila encheu e o Spring precisou criar novas Threads extras.");
        } else {
            logger.info("Status: OK.");
        }
    }

    // Auto-healing (Detecção de Offline)
    @Scheduled(fixedRate = 180000)
    public void busAutoHealingMonitor() {
        Set<String> allActiveTravelsId = redisTrackingService.getAllActiveTravelsId();

        for (String id : allActiveTravelsId) {
            Long lastPingTimestamp = redisTrackingService.getLastPingTimestamp(UUID.fromString(id));

            if (lastPingTimestamp == null) continue;

            if (isExpired(lastPingTimestamp)) {
                handleTravelTimeout(UUID.fromString(id));
            }
        }

    }

    // encerra a viagem e deleta as telemetrias de cache dessa viagem em específico no redis
    @Transactional
    private void handleTravelTimeout(UUID travelId) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new EntityNotFoundException("Viagem não encontrada: " + travelId));

        travel.setTravelStatus(TravelStatus.FINISH);
        travelRepository.save(travel);

        redisTrackingService.removeUnactiveTravel(travelId);
        redisTrackingService.clearTravelLocationCache(travelId);

        logger.info("[AUTO-HEALING] Viagem {} encerrada por inatividade.", travelId);
    }

    // verifica se o último ping foi há mais de 8 minutos (expired)
    private boolean isExpired(Long lastPing) {
        // 8 minutos em milissegundos
        long expirationMillis = 8 * 60 * 1000;

        return (System.currentTimeMillis() - lastPing) >= expirationMillis;
    }

    private int percentCalc(int original, int percent) {
        return (original * percent) / 100;
    }
}
