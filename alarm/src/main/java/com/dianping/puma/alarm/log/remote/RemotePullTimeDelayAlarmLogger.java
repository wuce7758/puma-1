package com.dianping.puma.alarm.log.remote;

import com.dianping.puma.alarm.log.PullAlarmLogger;
import com.dianping.puma.alarm.model.data.PullTimeDelayAlarmData;
import com.dianping.puma.alarm.service.ClientAlarmDataService;
import com.dianping.puma.common.AbstractPumaLifeCycle;
import com.dianping.puma.common.intercept.exception.PumaInterceptException;
import com.dianping.puma.common.utils.Clock;
import com.dianping.puma.common.utils.NamedThreadFactory;
import com.dianping.puma.core.dto.BinlogHttpMessage;
import com.dianping.puma.core.dto.binlog.request.BinlogGetRequest;
import com.google.common.collect.MapMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by xiaotian.li on 16/3/8.
 * Email: lixiaotian07@gmail.com
 */
public class RemotePullTimeDelayAlarmLogger extends AbstractPumaLifeCycle implements PullAlarmLogger<BinlogHttpMessage> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ConcurrentMap<String, Long> pullTimeMap = new MapMaker().makeMap();

    private ClientAlarmDataService clientAlarmDataService;

    private Clock clock = new Clock();

    private long flushIntervalInSecond = 5;

    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(
            1, new NamedThreadFactory("remote-pull-delay-alarm-logger-executor", true));

    @Override
    public void start() {
        super.start();

        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    flush();
                } catch (Throwable t) {
                    logger.error("Failed to flush pull time periodically.", t);
                }
            }
        }, 0, flushIntervalInSecond, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        super.stop();

        executor.shutdownNow();
    }

    @Override
    public void before(BinlogHttpMessage data) throws PumaInterceptException {
        if (data instanceof BinlogGetRequest) {
            BinlogGetRequest binlogGetRequest = (BinlogGetRequest) data;

            String clientName = binlogGetRequest.getClientName();
            pullTimeMap.put(clientName, clock.getTimestamp());
        }
    }

    @Override
    public void after(BinlogHttpMessage data) throws PumaInterceptException {
        // do nothing.
    }

    @Override
    public void error(BinlogHttpMessage data) throws PumaInterceptException {
        // do nothing.
    }

    private void flush() {
        for (Map.Entry<String, Long> entry: pullTimeMap.entrySet()) {
            String clientName = entry.getKey();
            long pullTime = entry.getValue();
            long pullTimeDelayInSecond = clock.getTimestamp() - pullTime;

            PullTimeDelayAlarmData data = new PullTimeDelayAlarmData();
            data.setPullTimeDelayInSecond(pullTimeDelayInSecond);

            try {
                clientAlarmDataService.replacePullTimeDelay(clientName, data);
            } catch (Throwable t) {
                logger.error("Failed to flush pull time delay[{}] for client[{}].",
                        pullTimeDelayInSecond, clientName, t);
            }
        }
    }

    public void setClientAlarmDataService(ClientAlarmDataService clientAlarmDataService) {
        this.clientAlarmDataService = clientAlarmDataService;
    }
}