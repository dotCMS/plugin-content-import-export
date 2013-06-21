package org.dotcms.plugins.contentImporter.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import com.dotmarketing.portlets.scheduler.model.Scheduler;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.QuartzUtils;

public class ContentImporterQuartzUtils extends QuartzUtils {
	public static String quartzGroup = "DotCMS Content Importer";
	
	public static List<Scheduler> getConfiguredSchedulers(String[] groupNames) {
		List<Scheduler> result = new ArrayList<Scheduler>(10);
		
		try {
			org.quartz.Scheduler sched = StdSchedulerFactory.getDefaultScheduler();
			
			String[] jobNames;
			Scheduler scheduler;
			JobDetail jobDetail;
			
			for (String groupName: groupNames) {
				jobNames = sched.getJobNames(groupName);
				
				for (String jobName: jobNames) {
					jobDetail = sched.getJobDetail(jobName, groupName);
					Trigger[] triggers = sched.getTriggersOfJob(jobName, groupName);
					
					scheduler = new Scheduler();
					scheduler.setJobName(jobDetail.getName());
					scheduler.setJobGroup(jobDetail.getGroup());
					scheduler.setJobDescription(jobDetail.getDescription());
					scheduler.setProperties((HashMap<String, String>) jobDetail.getJobDataMap().getWrappedMap());
					
					if ((0 < triggers.length) &&
						((triggers[0].getEndTime() == null) ||
						 (triggers[0].getEndTime().after(new Date())))) {
						
						SimpleDateFormat sdf = new SimpleDateFormat(com.dotmarketing.util.WebKeys.DateFormats.DOTSCHEDULER_DATE2);
						
						if (triggers[0].getStartTime() != null)
							scheduler.setStartDate(sdf.format(triggers[0].getStartTime()));
						else
							scheduler.setStartDate(null);
						
						if (triggers[0].getEndTime() != null)
							scheduler.setEndDate(sdf.format(triggers[0].getEndTime()));
						else
							scheduler.setEndDate(null);
						
						scheduler.setCronExpression(((CronTrigger) triggers[0]).getCronExpression());
					}
					
					result.add(scheduler);
				}
			}
			
		} catch (Exception e) {
			Logger.info(QuartzUtils.class, e.toString());
		}
		
		return result;
	}
}