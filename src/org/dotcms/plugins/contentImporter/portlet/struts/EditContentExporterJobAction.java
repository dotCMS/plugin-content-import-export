package org.dotcms.plugins.contentImporter.portlet.struts;

import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.PortletConfig;

import com.dotcms.repackage.org.apache.commons.beanutils.BeanUtils;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import org.dotcms.plugins.contentImporter.portlet.form.ContentExporterForm;

import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.quartz.CronScheduledTask;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.Validator;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.util.servlet.SessionMessages;

/**
 * This class create, edit and delete export content quartz job
 * @author Oswaldo
 *
 */
public class EditContentExporterJobAction extends DotPortletAction {

	public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req, ActionResponse res)
	throws Exception {

		String cmd = req.getParameter(Constants.CMD);
		Logger.debug(this, "Inside EditContentExporterJobAction cmd=" + cmd);

		//get the user
		User user = _getUser(req);

		CronScheduledTask scheduler = null;

		try {
			Logger.debug(this, "I'm retrieving the schedule");
			scheduler = _retrieveScheduler(req, res, config, form);
		}
		catch (Exception ae) {
			_handleException(ae, req);
		}

		/*
		 *  if we are saving, 
		 *  
		 */
		if ((cmd != null) && cmd.equals(Constants.ADD)) {
			try {

				ContentExporterForm contentExporterForm = (ContentExporterForm) form;
				boolean hasErrors = false;

				if (!UtilMethods.isSet(contentExporterForm.getJobName())) {
					SessionMessages.add(req, "error", "message.Scheduler.invalidJobName");
					hasErrors = true;
				} else if (!contentExporterForm.isEditMode() && (scheduler != null)) {
					SessionMessages.add(req, "error", "message.Scheduler.jobAlreadyExists");
					hasErrors = true;
				}

				SimpleDateFormat sdf = new SimpleDateFormat(WebKeys.DateFormats.DOTSCHEDULER_DATE2);

				if(contentExporterForm.isHaveCronExpression()){
					if(!UtilMethods.isSet(contentExporterForm.getCronExpression())){
						SessionMessages.add(req, "error", "message.Scheduler.cronexpressionNeeded");
						hasErrors = true;
					}
				}
				Date endDate = null;
				if (contentExporterForm.isHaveEndDate()) {
					try {
						endDate = sdf.parse(contentExporterForm.getEndDate());
					} catch (Exception e) {
					}
				}

				if ((endDate != null) && !hasErrors) {
					Date startDate = null;
					if (contentExporterForm.isHaveStartDate()) {
						try {
							startDate = sdf.parse(contentExporterForm.getStartDate());
						} catch (Exception e) {
						}
					}

					if (startDate == null) {
						SessionMessages.add(req, "error", "message.Scheduler.startDateNeeded");
						hasErrors = true;
					} else if (endDate.before(startDate)) {
						SessionMessages.add(req, "error", "message.Scheduler.endDateBeforeStartDate");
						hasErrors = true;
					} else if (endDate.before(new Date())) {
						SessionMessages.add(req, "error", "message.Scheduler.endDateBeforeActualDate");
						hasErrors = true;
					}
				}

				if (!UtilMethods.isSet(contentExporterForm.getStructure())) {
					SessionMessages.add(req, "error", "message.content.exporter.structure.required");
					hasErrors = true;
				}

				if (!UtilMethods.isSet(contentExporterForm.getFilePath())) {
					SessionMessages.add(req, "error", "message.content.exporter.file.path.required");
					hasErrors = true;
				}

				if ((contentExporterForm.getFields() != null) && (0 < contentExporterForm.getFields().length)) {
					boolean containsIdentifier = false;
					for (String key: contentExporterForm.getFields()) {
						if (key.equals("0")) {
							containsIdentifier = true;
							break;
						}
					}
				}

				if (Validator.validate(req,form,mapping) && !hasErrors) {
					Logger.debug(this, "I'm Saving the scheduler");
					if (_saveScheduler(req, res, config, form, user)) {
						scheduler = _retrieveScheduler(req, res, config, form);

						if (scheduler != null) {
							_populateForm(form, scheduler);
							contentExporterForm.setMap(scheduler.getProperties());
						}

						String redirect = req.getParameter("referrer");
						if (UtilMethods.isSet(redirect)) {
							redirect = URLDecoder.decode(redirect, "UTF-8") + "&group=" + scheduler.getJobGroup();
							_sendToReferral(req, res, redirect);
							return;
						}
					} else {
						SessionMessages.clear(req);
						SessionMessages.add(req, "error", "message.Scheduler.invalidJobSettings");
						contentExporterForm.setMap(getSchedulerProperties(req, contentExporterForm));
						loadEveryDayForm(form, req);
					}
				} else {
					contentExporterForm.setMap(getSchedulerProperties(req, contentExporterForm));
					loadEveryDayForm(form, req);
				}
			}
			catch (Exception ae) {
				if (!ae.getMessage().equals(WebKeys.UNIQUE_SCHEDULER_EXCEPTION)){
					_handleException(ae, req);
				}
			}
		}

		/*
		 * deleting the list, return to listing page
		 *  
		 */
		else if ((cmd != null) && cmd.equals(Constants.DELETE)) {
			try {
				Logger.debug(this, "I'm deleting the scheduler");
				_deleteScheduler(req, res, config, form,user);

			}
			catch (Exception ae) {
				_handleException(ae, req);
			}

			String redirect = req.getParameter("referrer");
			if (UtilMethods.isSet(redirect)) {
				redirect = URLDecoder.decode(redirect, "UTF-8");
				_sendToReferral(req, res, redirect);
				return;
			}
		}

		/*
		 * Copy copy props from the db to the form bean 
		 * 
		 */
		if ((cmd != null) && cmd.equals(Constants.EDIT)) {
			if (scheduler != null) {
				_populateForm(form, scheduler);
				ContentExporterForm contentExporterForm = (ContentExporterForm) form;

				contentExporterForm.setEditMode(true);
				if (!UtilMethods.isSet(scheduler.getCronExpression())) {
					SessionMessages.add(req, "message", "message.Scheduler.jobExpired");
				}
			}
		}

		/*
		 * return to edit page
		 *  
		 */
		setForward(req, "portlet.ext.plugins.content.importer.struts.edit_export_job");
	}

	/**
	 * Load EveryDay form values
	 * @param form
	 * @param req
	 */
	private void loadEveryDayForm(ActionForm form, ActionRequest req) {
		String[] everyDay = req.getParameterValues("everyDay");

		ContentExporterForm contentExporterForm = (ContentExporterForm) form;
		if (UtilMethods.isSet(everyDay) && contentExporterForm.isEveryInfo()) {
			for (String dayOfWeek: everyDay) {
				if (dayOfWeek.equals("MON"))
					contentExporterForm.setMonday(true);
				else if (dayOfWeek.equals("TUE"))
					contentExporterForm.setTuesday(true);
				else if (dayOfWeek.equals("WED"))
					contentExporterForm.setWednesday(true);
				else if (dayOfWeek.equals("THU"))
					contentExporterForm.setThusday(true);
				else if (dayOfWeek.equals("FRI"))
					contentExporterForm.setFriday(true);
				else if (dayOfWeek.equals("SAT"))
					contentExporterForm.setSaturday(true);
				else if (dayOfWeek.equals("SUN"))
					contentExporterForm.setSunday(true);
			}

			contentExporterForm.setEveryInfo(true);
			contentExporterForm.setEvery("isDays");
		} else {
			contentExporterForm.setEvery("");
			contentExporterForm.setMonday(false);
			contentExporterForm.setTuesday(false);
			contentExporterForm.setWednesday(false);
			contentExporterForm.setThusday(false);
			contentExporterForm.setFriday(false);
			contentExporterForm.setSaturday(false);
			contentExporterForm.setSunday(false);
		}
	}

	/**
	 * Get a map with the the form properties 
	 * @param req
	 * @param contentExporterForm
	 * @return Map<String,String>
	 */
	private Map<String, String> getSchedulerProperties(ActionRequest req, ContentExporterForm contentExporterForm) {
		Map<String, String> properties = new HashMap<String, String>(5);
		Enumeration<String> propertiesNames = req.getParameterNames();

		if (UtilMethods.isSet(contentExporterForm.getMap())) {
			properties = contentExporterForm.getMap();
		}
		else {
			String propertyName;
			String propertyValue;

			for (; propertiesNames.hasMoreElements();) {
				propertyName = propertiesNames.nextElement();
				if (propertyName.startsWith("propertyName")) {
					propertyValue = req.getParameter("propertyValue" + propertyName.substring(12));

					if (UtilMethods.isSet(req.getParameter(propertyName)) && UtilMethods.isSet(propertyValue))
						properties.put(req.getParameter(propertyName), propertyValue);
				}
			}
		}

		return properties;
	}

	/**
	 * Search the scheduler in the database
	 * @param req
	 * @param res
	 * @param config
	 * @param form
	 * @return
	 * @throws Exception
	 */
	private CronScheduledTask _retrieveScheduler(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form) throws Exception {
		ContentExporterForm contentExporterForm = (ContentExporterForm) form;
		List<ScheduledTask> results = null;
		if (UtilMethods.isSet(contentExporterForm.getJobGroup())){
			results = (List<ScheduledTask>) QuartzUtils.getStandardScheduledTask(contentExporterForm.getJobName(), contentExporterForm.getJobGroup());
		} else{
			results = (List<ScheduledTask>) QuartzUtils.getStandardScheduledTask(req.getParameter("name"), req.getParameter("group"));
		}

		if(results.size() > 0)
			return (CronScheduledTask) results.get(0);
		else
			return null;
	}

	/**
	 * Save the export quartz job
	 * @param req
	 * @param res
	 * @param config
	 * @param form
	 * @param user
	 * @return
	 * @throws Exception
	 */
	private static boolean _saveScheduler(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user) throws Exception {
		ContentExporterForm contentExporterForm = (ContentExporterForm) form;

		SimpleDateFormat sdf = new SimpleDateFormat(WebKeys.DateFormats.DOTSCHEDULER_DATE2);

		Date startDate = null;
		if (contentExporterForm.isHaveStartDate()) {
			try {
				startDate = sdf.parse(contentExporterForm.getStartDate());
			} catch (Exception e) {
			}
		}

		Date endDate = null;
		if (contentExporterForm.isHaveEndDate()) {
			try {
				endDate = sdf.parse(contentExporterForm.getEndDate());
			} catch (Exception e) {
			}
		}

		Map<String, Object> properties = new HashMap<String, Object>(10);

		properties.put("structure", "" + contentExporterForm.getStructure());

		if ((contentExporterForm.getFields() != null) && (0 < contentExporterForm.getFields().length)) {
			StringBuilder fields = new StringBuilder(64);
			fields.ensureCapacity(8);
			for (String field: contentExporterForm.getFields()) {
				if (0 < fields.length())
					fields.append("," + field);
				else
					fields.append(field);
			}

			properties.put("fields", fields.toString());
		}

		if (UtilMethods.isSet(contentExporterForm.getFilePath()))
			properties.put("filePath", contentExporterForm.getFilePath());
		
		if (UtilMethods.isSet(contentExporterForm.getReportEmail()))
			properties.put("reportEmail", contentExporterForm.getReportEmail());

		if (UtilMethods.isSet(contentExporterForm.getCsvSeparatorDelimiter()))
			properties.put("csvSeparatorDelimiter", contentExporterForm.getCsvSeparatorDelimiter());

		if (UtilMethods.isSet(contentExporterForm.getCsvTextDelimiter()))
			properties.put("csvTextDelimiter", contentExporterForm.getCsvTextDelimiter());

		if (UtilMethods.isSet(contentExporterForm.getLanguage()))
			properties.put("language",Long.toString(contentExporterForm.getLanguage()));

		properties.put("overWriteFile", contentExporterForm.isOverWriteFile());
		properties.put("haveCronExpression", contentExporterForm.isHaveCronExpression());
		properties.put("userId", user.getUserId());

		String cronSecondsField = "0";
		String cronMinutesField = "0";
		String cronHoursField = "*";
		String cronDaysOfMonthField = "*";
		String cronMonthsField = "*";
		String cronDaysOfWeekField = "?";
		String cronYearsField = "*";

		String cronExpression = "";

		if(contentExporterForm.isHaveCronExpression()){
			cronExpression = contentExporterForm.getCronExpression();
		}else{
			if (contentExporterForm.isAtInfo()) {
				if (UtilMethods.isSet(req.getParameter("at")) && req.getParameter("at").equals("isTime")) {
					cronSecondsField = req.getParameter("atTimeSecond");
					cronMinutesField = req.getParameter("atTimeMinute");
					cronHoursField = req.getParameter("atTimeHour");
				}

				if (UtilMethods.isSet(req.getParameter("at")) && req.getParameter("at").equals("isBetween")) {
					cronHoursField = req.getParameter("betweenFromHour") + "-" + req.getParameter("betweenToHour");
				}
			}

			if (contentExporterForm.isEveryInfo()) {
				if (UtilMethods.isSet(req.getParameter("every")) && req.getParameter("every").equals("isDate")) {
					cronDaysOfMonthField = req.getParameter("everyDateDay");

					try {
						cronMonthsField = "" + (Integer.parseInt(req.getParameter("everyDateMonth")) + 1);
					} catch (Exception e) {
					}

					cronYearsField = req.getParameter("everyDateYear");
				}

				if (UtilMethods.isSet(req.getParameter("every")) && req.getParameter("every").equals("isDays")) {
					cronDaysOfMonthField = "?";

					String[] daysOfWeek = req.getParameterValues("everyDay");

					cronDaysOfWeekField = "";
					for(String day: daysOfWeek) {
						if (cronDaysOfWeekField.length() == 0) {
							cronDaysOfWeekField = day;
						} else {
							cronDaysOfWeekField = cronDaysOfWeekField + "," + day;
						}
					}
				}
			}

			if (UtilMethods.isSet(req.getParameter("eachInfo"))) {
				if (UtilMethods.isSet(req.getParameter("eachHours"))) {
					try {
						int eachHours = Integer.parseInt(req.getParameter("eachHours"));
						cronHoursField = cronHoursField + "/" + eachHours;
					} catch (Exception e) {
					}
				}

				if (UtilMethods.isSet(req.getParameter("eachMinutes"))) {
					try {
						int eachMinutes = Integer.parseInt(req.getParameter("eachMinutes"));
						cronMinutesField = cronMinutesField + "/" + eachMinutes;
					} catch (Exception e) {
					}
				}
			}

			cronExpression = cronSecondsField + " " + cronMinutesField + " " + cronHoursField + " " + cronDaysOfMonthField + " " + cronMonthsField + " " + cronDaysOfWeekField + " " + cronYearsField;
		}

		CronScheduledTask job = new CronScheduledTask();
		job.setJobName(contentExporterForm.getJobName());
		job.setJobGroup(contentExporterForm.getJobGroup());
		job.setJobDescription(contentExporterForm.getJobDescription());
		job.setJavaClassName("org.dotcms.plugins.contentImporter.quartz.ContentExporterThread");
		job.setProperties(properties);
		job.setStartDate(startDate);
		job.setEndDate(endDate);
		job.setCronExpression(cronExpression);

		try {
			QuartzUtils.scheduleTask(job);
		}catch(Exception e){
			Logger.error(EditContentImporterJobAction.class, e.getMessage(),e);
			return false;
		}

		SessionMessages.add(req, "message", "message.Scheduler.saved");

		return true;
	}

	/**
	 * Delete the export quartz job specified in the form
	 * @param req
	 * @param res
	 * @param config
	 * @param form
	 * @param user
	 * @throws Exception
	 */
	private void _deleteScheduler(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form , User user) throws Exception {
		ContentExporterForm contentExporterForm = (ContentExporterForm) form;

		if (UtilMethods.isSet(contentExporterForm.getJobGroup()))
			QuartzUtils.removeJob(contentExporterForm.getJobName(), contentExporterForm.getJobGroup());
		else
			QuartzUtils.removeJob(req.getParameter("name"), req.getParameter("group"));

		SessionMessages.add(req, "message", "message.Scheduler.delete");
	}

	/**
	 * Populate the ContentExporterForm with the scheduler task info
	 * @param form
	 * @param scheduler
	 */
	private void _populateForm(ActionForm form, CronScheduledTask scheduler) {
		try {
			BeanUtils.copyProperties(form, scheduler);
			ContentExporterForm contentExporterForm = ((ContentExporterForm) form);

			SimpleDateFormat sdf = new SimpleDateFormat(WebKeys.DateFormats.DOTSCHEDULER_DATE2);

			if (scheduler.getStartDate() != null) {
				contentExporterForm.setHaveStartDate(true);				
			} else {
				contentExporterForm.setHaveStartDate(true);
				contentExporterForm.setStartDate(sdf.format(new Date()));
			}

			if (scheduler.getEndDate() != null) {
				contentExporterForm.setHaveEndDate(true);			
			} else {
				contentExporterForm.setHaveEndDate(true);
				contentExporterForm.setEndDate(sdf.format(new Date()));
			}

			if(UtilMethods.isSet(scheduler.getCronExpression()))
			{
				StringTokenizer cronExpressionTokens = new StringTokenizer(scheduler.getCronExpression());
				String token;
				String[] intervalTokens;
				String[] rangeTokens;

				// Seconds Cron Expression
				if (cronExpressionTokens.hasMoreTokens()) {
					token = cronExpressionTokens.nextToken();

					if (token.equals("*")) {
						contentExporterForm.setAtInfo(false);
						contentExporterForm.setAt(null);
						contentExporterForm.setAtTimeSecond(0);
					} else {
						intervalTokens = token.split("/");
						rangeTokens = intervalTokens[0].split("-");

						if (rangeTokens.length == 2) {
							contentExporterForm.setAtInfo(true);
							contentExporterForm.setAt("isBetween");
							try {
								contentExporterForm.setBetweenFromSecond(Integer.parseInt(rangeTokens[0]));
								contentExporterForm.setBetweenToSecond(Integer.parseInt(rangeTokens[1]));
							} catch (Exception e) {
								contentExporterForm.setBetweenFromSecond(0);
								contentExporterForm.setBetweenToSecond(0);
							}
						} else {
							contentExporterForm.setAtInfo(true);
							contentExporterForm.setAt("isTime");
							try {
								contentExporterForm.setAtTimeSecond(Integer.parseInt(intervalTokens[0]));
							} catch (Exception e) {
								contentExporterForm.setAtTimeSecond(0);
							}
						}
					}
				}

				contentExporterForm.setEachInfo(false);

				// Minutes Cron Expression
				if (cronExpressionTokens.hasMoreTokens()) {
					token = cronExpressionTokens.nextToken();

					if (token.equals("*")) {
						contentExporterForm.setAtInfo(false);
						contentExporterForm.setAt(null);
						contentExporterForm.setAtTimeMinute(0);
					} else {
						intervalTokens = token.split("/");
						rangeTokens = intervalTokens[0].split("-");

						if (rangeTokens.length == 2) {
							contentExporterForm.setAtInfo(true);
							contentExporterForm.setAt("isBetween");
							try {
								contentExporterForm.setBetweenFromMinute(Integer.parseInt(rangeTokens[0]));
								contentExporterForm.setBetweenToMinute(Integer.parseInt(rangeTokens[1]));
							} catch (Exception e) {
								contentExporterForm.setBetweenFromMinute(0);
								contentExporterForm.setBetweenToMinute(0);
							}
						} else {
							contentExporterForm.setAtInfo(true);
							contentExporterForm.setAt("isTime");
							try {
								contentExporterForm.setAtTimeMinute(Integer.parseInt(intervalTokens[0]));
							} catch (Exception e) {
								contentExporterForm.setAtTimeMinute(0);
							}
						}

						if (intervalTokens.length == 2) {
							try {
								contentExporterForm.setEachMinutes(Integer.parseInt(intervalTokens[1]));
								contentExporterForm.setEachInfo(true);
							} catch (Exception e) {
								contentExporterForm.setEachMinutes(0);
							}
						}
					}
				}

				// Hours Cron Expression
				if (cronExpressionTokens.hasMoreTokens()) {
					token = cronExpressionTokens.nextToken();

					if (token.equals("*")) {
						contentExporterForm.setAtInfo(false);
						contentExporterForm.setAt(null);
						contentExporterForm.setAtTimeHour(0);
					} else {
						intervalTokens = token.split("/");
						rangeTokens = intervalTokens[0].split("-");

						if (rangeTokens.length == 2) {
							contentExporterForm.setAtInfo(true);
							contentExporterForm.setAt("isBetween");
							try {
								contentExporterForm.setBetweenFromHour(Integer.parseInt(rangeTokens[0]));
								contentExporterForm.setBetweenToHour(Integer.parseInt(rangeTokens[1]));
							} catch (Exception e) {
								contentExporterForm.setBetweenFromHour(0);
								contentExporterForm.setBetweenToHour(0);
							}
						} else {
							contentExporterForm.setAtInfo(true);
							contentExporterForm.setAt("isTime");
							try {
								contentExporterForm.setAtTimeHour(Integer.parseInt(intervalTokens[0]));
							} catch (Exception e) {
								contentExporterForm.setAtTimeHour(0);
							}
						}

						if (intervalTokens.length == 2) {
							try {
								contentExporterForm.setEachHours(Integer.parseInt(intervalTokens[1]));
								contentExporterForm.setEachInfo(true);
							} catch (Exception e) {
								contentExporterForm.setEachHours(0);
							}
						}
					}
				}

				contentExporterForm.setEveryInfo(false);
				contentExporterForm.setEvery(null);

				// Days of Month Cron Expression
				if (cronExpressionTokens.hasMoreTokens()) {
					token = cronExpressionTokens.nextToken();

					if (token.equals("*") || token.equals("?")) {
						contentExporterForm.setEveryDateDay(-1);
					} else {
						try {
							contentExporterForm.setEveryDateDay(Integer.parseInt(token));
							contentExporterForm.setEveryInfo(true);
							contentExporterForm.setEvery("isDate");
						} catch (Exception e) {
							contentExporterForm.setEveryDateDay(-1);
						}
					}
				}

				// Months Cron Expression
				if (cronExpressionTokens.hasMoreTokens()) {
					token = cronExpressionTokens.nextToken();

					if (token.equals("*")) {
						contentExporterForm.setEveryDateMonth(-1);
					} else {
						try {
							contentExporterForm.setEveryDateMonth(Integer.parseInt(token));
							contentExporterForm.setEveryInfo(true);
							contentExporterForm.setEvery("isDate");
						} catch (Exception e) {
							contentExporterForm.setEveryDateMonth(-1);
						}
					}
				}

				// Days of Week Cron Expression
				if (cronExpressionTokens.hasMoreTokens()) {
					token = cronExpressionTokens.nextToken();

					if ((!token.equals("*")) && (!token.equals("?"))) {
						StringTokenizer daysOfWeek = new StringTokenizer(token, ",");
						String dayOfWeek;

						for (; daysOfWeek.hasMoreTokens();) {
							dayOfWeek = daysOfWeek.nextToken();

							if (dayOfWeek.equals("MON"))
								contentExporterForm.setMonday(true);
							else if (dayOfWeek.equals("TUE"))
								contentExporterForm.setTuesday(true);
							else if (dayOfWeek.equals("WED"))
								contentExporterForm.setWednesday(true);
							else if (dayOfWeek.equals("THU"))
								contentExporterForm.setThusday(true);
							else if (dayOfWeek.equals("FRI"))
								contentExporterForm.setFriday(true);
							else if (dayOfWeek.equals("SAT"))
								contentExporterForm.setSaturday(true);
							else if (dayOfWeek.equals("SUN"))
								contentExporterForm.setSunday(true);
						}

						contentExporterForm.setEveryInfo(true);
						contentExporterForm.setEvery("isDays");
					}
				}

				// Years Cron Expression
				if (cronExpressionTokens.hasMoreTokens()) {
					token = cronExpressionTokens.nextToken();

					if (token.equals("*")) {
						contentExporterForm.setEveryDateYear(-1);
					} else {
						try {
							contentExporterForm.setEveryDateYear(Integer.parseInt(token));
							contentExporterForm.setEveryInfo(true);
							contentExporterForm.setEvery("isDate");
						} catch (Exception e) {
							contentExporterForm.setEveryDateYear(-1);
						}
					}
				}
			}

			Map properties = scheduler.getProperties();
			contentExporterForm.setStructure((String) properties.get("structure"));

			String[] fields = {};
			if (UtilMethods.isSet(properties.get("fields"))) {
				String[] strFields = ((String) properties.get("fields")).split(",");
				List<String> longFields = new ArrayList<String>(strFields.length);
				for (String field: strFields) {
					longFields.add(field);
				}

				String[] tempArray = new String[longFields.size()];
				for (int pos = 0; pos < longFields.size(); ++pos) {
					tempArray[pos] = longFields.get(pos);
				}
				fields = tempArray;
			}

			contentExporterForm.setFields(fields);
			contentExporterForm.setFilePath((String) properties.get("filePath"));
			contentExporterForm.setOverWriteFile((Boolean) properties.get("overWriteFile"));
			contentExporterForm.setReportEmail((String) properties.get("reportEmail"));
			contentExporterForm.setCsvSeparatorDelimiter((String) properties.get("csvSeparatorDelimiter"));
			contentExporterForm.setCsvTextDelimiter((String) properties.get("csvTextDelimiter"));			
			contentExporterForm.setLanguage((Long.parseLong((String) properties.get("language"))));
			contentExporterForm.setHaveCronExpression((Boolean) properties.get("haveCronExpression"));

			contentExporterForm.setNewForm(false);
		} catch (Exception e) {
			Logger.warn(this, e.getMessage());
		}
	}
}
