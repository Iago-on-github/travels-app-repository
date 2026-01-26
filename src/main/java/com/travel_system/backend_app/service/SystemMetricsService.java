package com.travel_system.backend_app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadPoolExecutor;

@Service
public class SystemMetricsService {
    private final ThreadPoolExecutor threadPoolExecutor;

    private static final Logger logger = LoggerFactory.getLogger(SystemMetricsService.class);

    public SystemMetricsService(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
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

    private int percentCalc(int original, int percent) {
        return (original * percent) / 100;
    }

    // Auto-healing (Detecção de Offline)
}
