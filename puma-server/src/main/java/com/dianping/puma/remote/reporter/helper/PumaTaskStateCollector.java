package com.dianping.puma.remote.reporter.helper;

import com.dianping.puma.core.model.state.PumaTaskState;
import com.dianping.puma.core.service.PumaTaskStateService;
import com.dianping.puma.server.TaskExecutor;
import com.dianping.puma.server.TaskExecutorContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class PumaTaskStateCollector {

	@Autowired
	PumaTaskStateService pumaTaskStateService;

	@Autowired
	TaskExecutorContainer taskExecutorContainer;

	@Scheduled(cron = "0/5 * * * * ?")
	public void collect() {
		pumaTaskStateService.removeAll();

		for (TaskExecutor taskExecutor: taskExecutorContainer.getAll()) {
			PumaTaskState taskState = taskExecutor.getTaskState();
			taskState.setGmtUpdate(new Date());
			pumaTaskStateService.add(taskState);
		}
	}
}