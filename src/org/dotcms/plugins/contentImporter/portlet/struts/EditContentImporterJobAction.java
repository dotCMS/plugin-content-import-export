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
import org.dotcms.plugins.contentImporter.portlet.form.ContentImporterForm;

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
 * @author Armando
 */

public class EditContentImporterJobAction extends DotPortletAction {

	public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req, ActionResponse res)
	throws Exception {

		String cmd = req.getParameter(Constants.CMD);
		Logger.debug(this, "Inside EditContentImporterJobAction cmd=" + cmd);

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

				ContentImporterForm contentImporterForm = (ContentImporterForm) form;
				boolean hasErrors = false;

				if (!UtilMethods.isSet(contentImporterForm.getJobName())) {
					SessionMessages.add(req, "error", "message.Scheduler.invalidJobName");
					hasErrors = true;
				} else if (!contentImporterForm.isEditMode() && (scheduler != null)) {
					SessionMessages.add(req, "error", "message.Scheduler.jobAlreadyExists");
					hasErrors = true;
				}

				SimpleDateFormat sdf = new SimpleDateFormat(WebKeys.DateFormats.DOTSCHEDULER_DATE2);

				if(contentImporterForm.isHaveCronExpression()){
					if(!UtilMethods.isSet(contentImporterForm.getCronExpression())){
						SessionMessages.add(req, "error", "message.Scheduler.cronexpressionNeeded");
						hasErrors = true;
					}
				}

				Date endDate = null;
				if (contentImporterForm.isHaveEndDate()) {
					try {
						endDate = sdf.parse(contentImporterForm.getEndDate());
					} catch (Exception e) {
					}
				}

				if ((endDate != null) && !hasErrors) {
					Date startDate = null;
					if (contentImporterForm.isHaveStartDate()) {
						try {
							startDate = sdf.parse(contentImporterForm.getStartDate());
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

				if (!UtilMethods.isSet(contentImporterForm.getStructure())) {
					SessionMessages.add(req, "error", "message.content.importer.structure.required");
					hasErrors = true;
				}


				if(!contentImporterForm.isHaveFileSource()){
					if(!UtilMethods.isSet(contentImporterForm.getFilePath())){
						SessionMessages.add(req, "error", "message.content.importer.file.path.required");
						hasErrors = true;
					}
				} else {
					if(!UtilMethods.isSet(contentImporterForm.getFileAsset())){
						SessionMessages.add(req, "error", "message.content.importer.file.asset.required");
						hasErrors = true;
					}
				}

				if ((contentImporterForm.getFields() != null) && (0 < contentImporterForm.getFields().length)) {
					boolean containsIdentifier = false;
					for (String key: contentImporterForm.getFields()) {
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
							contentImporterForm.setMap(scheduler.getProperties());
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
						contentImporterForm.setMap(getSchedulerProperties(req, contentImporterForm));
						loadEveryDayForm(form, req);
					}
				} else {
					contentImporterForm.setMap(getSchedulerProperties(req, contentImporterForm));
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
				ContentImporterForm contentImporterForm = (ContentImporterForm) form;

				contentImporterForm.setEditMode(true);
				if (!UtilMethods.isSet(scheduler.getCronExpression())) {
					SessionMessages.add(req, "message", "message.Scheduler.jobExpired");
				}
			}
		}

		/*
		 * return to edit page
		 *  
		 */
		setForward(req, "portlet.ext.plugins.content.importer.struts.edit_job");
	}

	private void loadEveryDayForm(ActionForm form, ActionRequest req) {
		String[] everyDay = req.getParameterValues("everyDay");

		ContentImporterForm contentImporterForm = (ContentImporterForm) form;
		if (UtilMethods.isSet(everyDay) && contentImporterForm.isEveryInfo()) {
			for (String dayOfWeek: everyDay) {
				if (dayOfWeek.equals("MON"))
					contentImporterForm.setMonday(true);
				else if (dayOfWeek.equals("TUE"))
					contentImporterForm.setTuesday(true);
				else if (dayOfWeek.equals("WED"))
					contentImporterForm.setWednesday(true);
				else if (dayOfWeek.equals("THU"))
					contentImporterForm.setThusday(true);
				else if (dayOfWeek.equals("FRI"))
					contentImporterForm.setFriday(true);
				else if (dayOfWeek.equals("SAT"))
					contentImporterForm.setSaturday(true);
				else if (dayOfWeek.equals("SUN"))
					contentImporterForm.setSunday(true);
			}

			contentImporterForm.setEveryInfo(true);
			contentImporterForm.setEvery("isDays");
		} else {
			contentImporterForm.setEvery("");
			contentImporterForm.setMonday(false);
			contentImporterForm.setTuesday(false);
			contentImporterForm.setWednesday(false);
			contentImporterForm.setThusday(false);
			contentImporterForm.setFriday(false);
			contentImporterForm.setSaturday(false);
			contentImporterForm.setSunday(false);
		}
	}

	private Map<String, String> getSchedulerProperties(ActionRequest req, ContentImporterForm contentImporterForm) {
		Map<String, String> properties = new HashMap<String, String>(5);
		Enumeration<String> propertiesNames = req.getParameterNames();

		if (UtilMethods.isSet(contentImporterForm.getMap())) {
			properties = contentImporterForm.getMap();
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

	private CronScheduledTask _retrieveScheduler(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form) throws Exception {
		ContentImporterForm contentImporterForm = (ContentImporterForm) form;
		List<ScheduledTask> results = null;
		if (UtilMethods.isSet(contentImporterForm.getJobGroup())){
			results = (List<ScheduledTask>) QuartzUtils.getStandardScheduledTask(contentImporterForm.getJobName(), contentImporterForm.getJobGroup());
		} else{
			results = (List<ScheduledTask>) QuartzUtils.getStandardScheduledTask(req.getParameter("name"), req.getParameter("group"));
		}

		if(results.size() > 0)
			return (CronScheduledTask) results.get(0);
		else
			return null;
	}

	private static boolean _saveScheduler(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user) throws Exception {
		boolean result = false;
		ContentImporterForm contentImporterForm = (ContentImporterForm) form;

		SimpleDateFormat sdf = new SimpleDateFormat(WebKeys.DateFormats.DOTSCHEDULER_DATE2);

		Date startDate = null;
		if (contentImporterForm.isHaveStartDate()) {
			try {
				startDate = sdf.parse(contentImporterForm.getStartDate());
			} catch (Exception e) {
			}
		}

		Date endDate = null;
		if (contentImporterForm.isHaveEndDate()) {
			try {
				endDate = sdf.parse(contentImporterForm.getEndDate());
			} catch (Exception e) {
			}
		}

		Map<String, Object> properties = new HashMap<String, Object>(10);

		properties.put("structure", "" + contentImporterForm.getStructure());

		if ((contentImporterForm.getFields() != null) && (0 < contentImporterForm.getFields().length)) {
			StringBuilder fields = new StringBuilder(64);
			fields.ensureCapacity(8);
			for (String field: contentImporterForm.getFields()) {
				if (0 < fields.length())
					fields.append("," + field);
				else
					fields.append(field);
			}

			properties.put("fields", fields.toString());
		}

		properties.put("haveFileSource", contentImporterForm.isHaveFileSource());
		if (UtilMethods.isSet(contentImporterForm.getFilePath()))
			properties.put("filePath", contentImporterForm.getFilePath());
		if (UtilMethods.isSet(contentImporterForm.getFileAsset()))
			properties.put("fileAsset", contentImporterForm.getFileAsset());

		if (UtilMethods.isSet(contentImporterForm.getReportEmail()))
			properties.put("reportEmail", contentImporterForm.getReportEmail());

		if (UtilMethods.isSet(contentImporterForm.getCsvSeparatorDelimiter()))
			properties.put("csvSeparatorDelimiter", contentImporterForm.getCsvSeparatorDelimiter());

		if (UtilMethods.isSet(contentImporterForm.getCsvTextDelimiter()))
			properties.put("csvTextDelimiter", contentImporterForm.getCsvTextDelimiter());

		if (UtilMethods.isSet(contentImporterForm.getLanguage()))
			properties.put("language",Long.toString(contentImporterForm.getLanguage()));

		if (contentImporterForm.isPublishContent())
			properties.put("publishContent", "true");
		else
			properties.put("publishContent", "false");
		
		if (contentImporterForm.isDeleteAllContent())
			properties.put("deleteAllContent", "true");
		else
			properties.put("deleteAllContent", "false");
		
		if (contentImporterForm.isSaveWithoutVersions())
			properties.put("saveWithoutVersions", "true");
		else
			properties.put("saveWithoutVersions", "false");

		properties.put("haveCronExpression", contentImporterForm.isHaveCronExpression());

		String cronSecondsField = "0";
		String cronMinutesField = "0";
		String cronHoursField = "*";
		String cronDaysOfMonthField = "*";
		String cronMonthsField = "*";
		String cronDaysOfWeekField = "?";
		String cronYearsField = "*";

		String cronExpression = "";

		if(contentImporterForm.isHaveCronExpression()){
			cronExpression = contentImporterForm.getCronExpression();
		}else{
			if (contentImporterForm.isAtInfo()) {
				if (UtilMethods.isSet(req.getParameter("at")) && req.getParameter("at").equals("isTime")) {
					cronSecondsField = req.getParameter("atTimeSecond");
					cronMinutesField = req.getParameter("atTimeMinute");
					cronHoursField = req.getParameter("atTimeHour");
				}

				if (UtilMethods.isSet(req.getParameter("at")) && req.getParameter("at").equals("isBetween")) {
					cronHoursField = req.getParameter("betweenFromHour") + "-" + req.getParameter("betweenToHour");
				}
			}

			if (contentImporterForm.isEveryInfo()) {
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
		job.setJobName(contentImporterForm.getJobName());
		job.setJobGroup(contentImporterForm.getJobGroup());
		job.setJobDescription(contentImporterForm.getJobDescription());
		job.setJavaClassName("org.dotcms.plugins.contentImporter.quartz.ContentImporterThread");
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

	private void _deleteScheduler(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form , User user) throws Exception {
		ContentImporterForm contentImporterForm = (ContentImporterForm) form;

		if (UtilMethods.isSet(contentImporterForm.getJobGroup()))
			QuartzUtils.removeJob(contentImporterForm.getJobName(), contentImporterForm.getJobGroup());
		else
			QuartzUtils.removeJob(req.getParameter("name"), req.getParameter("group"));

		SessionMessages.add(req, "message", "message.Scheduler.delete");
	}

	private void _populateForm(ActionForm form, CronScheduledTask scheduler) {
		try {
			BeanUtils.copyProperties(form, scheduler);
			ContentImporterForm contentImporterForm = ((ContentImporterForm) form);

			SimpleDateFormat sdf = new SimpleDateFormat(WebKeys.DateFormats.DOTSCHEDULER_DATE2);

			if (scheduler.getStartDate() != null) {
				contentImporterForm.setHaveStartDate(true);				
			} else {
				contentImporterForm.setHaveStartDate(true);
				contentImporterForm.setStartDate(sdf.format(new Date()));
			}

			if (scheduler.getEndDate() != null) {
				contentImporterForm.setHaveEndDate(true);			
			} else {
				contentImporterForm.setHaveEndDate(true);
				contentImporterForm.setEndDate(sdf.format(new Date()));
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
						contentImporterForm.setAtInfo(false);
						contentImporterForm.setAt(null);
						contentImporterForm.setAtTimeSecond(0);
					} else {
						intervalTokens = token.split("/");
						rangeTokens = intervalTokens[0].split("-");

						if (rangeTokens.length == 2) {
							contentImporterForm.setAtInfo(true);
							contentImporterForm.setAt("isBetween");
							try {
								contentImporterForm.setBetweenFromSecond(Integer.parseInt(rangeTokens[0]));
								contentImporterForm.setBetweenToSecond(Integer.parseInt(rangeTokens[1]));
							} catch (Exception e) {
								contentImporterForm.setBetweenFromSecond(0);
								contentImporterForm.setBetweenToSecond(0);
							}
						} else {
							contentImporterForm.setAtInfo(true);
							contentImporterForm.setAt("isTime");
							try {
								contentImporterForm.setAtTimeSecond(Integer.parseInt(intervalTokens[0]));
							} catch (Exception e) {
								contentImporterForm.setAtTimeSecond(0);
							}
						}
					}
				}

				contentImporterForm.setEachInfo(false);

				// Minutes Cron Expression
				if (cronExpressionTokens.hasMoreTokens()) {
					token = cronExpressionTokens.nextToken();

					if (token.equals("*")) {
						contentImporterForm.setAtInfo(false);
						contentImporterForm.setAt(null);
						contentImporterForm.setAtTimeMinute(0);
					} else {
						intervalTokens = token.split("/");
						rangeTokens = intervalTokens[0].split("-");

						if (rangeTokens.length == 2) {
							contentImporterForm.setAtInfo(true);
							contentImporterForm.setAt("isBetween");
							try {
								contentImporterForm.setBetweenFromMinute(Integer.parseInt(rangeTokens[0]));
								contentImporterForm.setBetweenToMinute(Integer.parseInt(rangeTokens[1]));
							} catch (Exception e) {
								contentImporterForm.setBetweenFromMinute(0);
								contentImporterForm.setBetweenToMinute(0);
							}
						} else {
							contentImporterForm.setAtInfo(true);
							contentImporterForm.setAt("isTime");
							try {
								contentImporterForm.setAtTimeMinute(Integer.parseInt(intervalTokens[0]));
							} catch (Exception e) {
								contentImporterForm.setAtTimeMinute(0);
							}
						}

						if (intervalTokens.length == 2) {
							try {
								contentImporterForm.setEachMinutes(Integer.parseInt(intervalTokens[1]));
								contentImporterForm.setEachInfo(true);
							} catch (Exception e) {
								contentImporterForm.setEachMinutes(0);
							}
						}
					}
				}

				// Hours Cron Expression
				if (cronExpressionTokens.hasMoreTokens()) {
					token = cronExpressionTokens.nextToken();

					if (token.equals("*")) {
						contentImporterForm.setAtInfo(false);
						contentImporterForm.setAt(null);
						contentImporterForm.setAtTimeHour(0);
					} else {
						intervalTokens = token.split("/");
						rangeTokens = intervalTokens[0].split("-");

						if (rangeTokens.length == 2) {
							contentImporterForm.setAtInfo(true);
							contentImporterForm.setAt("isBetween");
							try {
								contentImporterForm.setBetweenFromHour(Integer.parseInt(rangeTokens[0]));
								contentImporterForm.setBetweenToHour(Integer.parseInt(rangeTokens[1]));
							} catch (Exception e) {
								contentImporterForm.setBetweenFromHour(0);
								contentImporterForm.setBetweenToHour(0);
							}
						} else {
							contentImporterForm.setAtInfo(true);
							contentImporterForm.setAt("isTime");
							try {
								contentImporterForm.setAtTimeHour(Integer.parseInt(intervalTokens[0]));
							} catch (Exception e) {
								contentImporterForm.setAtTimeHour(0);
							}
						}

						if (intervalTokens.length == 2) {
							try {
								contentImporterForm.setEachHours(Integer.parseInt(intervalTokens[1]));
								contentImporterForm.setEachInfo(true);
							} catch (Exception e) {
								contentImporterForm.setEachHours(0);
							}
						}
					}
				}

				contentImporterForm.setEveryInfo(false);
				contentImporterForm.setEvery(null);

				// Days of Month Cron Expression
				if (cronExpressionTokens.hasMoreTokens()) {
					token = cronExpressionTokens.nextToken();

					if (token.equals("*") || token.equals("?")) {
						contentImporterForm.setEveryDateDay(-1);
					} else {
						try {
							contentImporterForm.setEveryDateDay(Integer.parseInt(token));
							contentImporterForm.setEveryInfo(true);
							contentImporterForm.setEvery("isDate");
						} catch (Exception e) {
							contentImporterForm.setEveryDateDay(-1);
						}
					}
				}

				// Months Cron Expression
				if (cronExpressionTokens.hasMoreTokens()) {
					token = cronExpressionTokens.nextToken();

					if (token.equals("*")) {
						contentImporterForm.setEveryDateMonth(-1);
					} else {
						try {
							contentImporterForm.setEveryDateMonth(Integer.parseInt(token));
							contentImporterForm.setEveryInfo(true);
							contentImporterForm.setEvery("isDate");
						} catch (Exception e) {
							contentImporterForm.setEveryDateMonth(-1);
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
								contentImporterForm.setMonday(true);
							else if (dayOfWeek.equals("TUE"))
								contentImporterForm.setTuesday(true);
							else if (dayOfWeek.equals("WED"))
								contentImporterForm.setWednesday(true);
							else if (dayOfWeek.equals("THU"))
								contentImporterForm.setThusday(true);
							else if (dayOfWeek.equals("FRI"))
								contentImporterForm.setFriday(true);
							else if (dayOfWeek.equals("SAT"))
								contentImporterForm.setSaturday(true);
							else if (dayOfWeek.equals("SUN"))
								contentImporterForm.setSunday(true);
						}

						contentImporterForm.setEveryInfo(true);
						contentImporterForm.setEvery("isDays");
					}
				}

				// Years Cron Expression
				if (cronExpressionTokens.hasMoreTokens()) {
					token = cronExpressionTokens.nextToken();

					if (token.equals("*")) {
						contentImporterForm.setEveryDateYear(-1);
					} else {
						try {
							contentImporterForm.setEveryDateYear(Integer.parseInt(token));
							contentImporterForm.setEveryInfo(true);
							contentImporterForm.setEvery("isDate");
						} catch (Exception e) {
							contentImporterForm.setEveryDateYear(-1);
						}
					}
				}
			}

			Map properties = scheduler.getProperties();
			contentImporterForm.setStructure((String) properties.get("structure"));

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

			contentImporterForm.setFields(fields);

			contentImporterForm.setFilePath((String) properties.get("filePath"));
			contentImporterForm.setFileAsset((String) properties.get("fileAsset"));
			if (UtilMethods.isSet(properties.get("haveFileSource")))
				contentImporterForm.setHaveFileSource((Boolean) properties.get("haveFileSource"));

			contentImporterForm.setReportEmail((String) properties.get("reportEmail"));
			contentImporterForm.setCsvSeparatorDelimiter((String) properties.get("csvSeparatorDelimiter"));
			contentImporterForm.setCsvTextDelimiter((String) properties.get("csvTextDelimiter"));			
			contentImporterForm.setLanguage((Long.parseLong((String) properties.get("language"))));
			contentImporterForm.setHaveCronExpression((Boolean) properties.get("haveCronExpression"));

			if (UtilMethods.isSet(properties.get("publishContent")))
				contentImporterForm.setPublishContent(new Boolean((String) properties.get("publishContent")));
			 
			if (UtilMethods.isSet(properties.get("saveWithoutVersions")))
				contentImporterForm.setSaveWithoutVersions(new Boolean((String) properties.get("saveWithoutVersions")));

			if (UtilMethods.isSet(properties.get("deleteAllContent")))
				contentImporterForm.setDeleteAllContent(new Boolean((String) properties.get("deleteAllContent")));

			
			contentImporterForm.setNewForm(false);
		} catch (Exception e) {
			Logger.warn(this, e.getMessage());
		}
	}
}