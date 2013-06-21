package org.dotcms.plugins.contentImporter.portlet.struts;

import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.dotcms.plugins.contentImporter.portlet.form.ContentImporterForm;

import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.scheduler.model.Scheduler;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.QuartzUtils;
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

		Scheduler scheduler = null;

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

				if (contentImporterForm.getStructure() < 1) {
					SessionMessages.add(req, "error", "message.content.importer.structure.required");
					hasErrors = true;
				}

				if (!UtilMethods.isSet(contentImporterForm.getFilePath())) {
					SessionMessages.add(req, "error", "message.content.importer.file.path.required");
					hasErrors = true;
				}

				if ((contentImporterForm.getFields() != null) && (0 < contentImporterForm.getFields().length)) {
					boolean containsIdentifier = false;
					for (long key: contentImporterForm.getFields()) {
						if (key == 0) {
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
							contentImporterForm.setHashMap(scheduler.getProperties());
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
						contentImporterForm.setHashMap(getSchedulerProperties(req, contentImporterForm));
						loadEveryDayForm(form, req);
					}
				} else {
					contentImporterForm.setHashMap(getSchedulerProperties(req, contentImporterForm));
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

	private HashMap<String, String> getSchedulerProperties(ActionRequest req, ContentImporterForm contentImporterForm) {
		HashMap<String, String> properties = new HashMap<String, String>(5);
		Enumeration<String> propertiesNames = req.getParameterNames();

		if (UtilMethods.isSet(contentImporterForm.getHashMap())) {
			properties = contentImporterForm.getHashMap();
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

	private Scheduler _retrieveScheduler(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form) throws Exception {
		ContentImporterForm contentImporterForm = (ContentImporterForm) form;

		if (UtilMethods.isSet(contentImporterForm.getJobGroup()))
			return QuartzUtils.getScheduler(contentImporterForm.getJobName(), contentImporterForm.getJobGroup());
		else
			return QuartzUtils.getScheduler(req.getParameter("name"), req.getParameter("group"));
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

		HashMap<String, String> properties = new HashMap<String, String>(10);

		properties.put("structure", "" + contentImporterForm.getStructure());

		if ((contentImporterForm.getFields() != null) && (0 < contentImporterForm.getFields().length)) {
			StringBuilder fields = new StringBuilder(64);
			fields.ensureCapacity(8);
			for (long field: contentImporterForm.getFields()) {
				if (0 < fields.length())
					fields.append("," + field);
				else
					fields.append(field);
			}

			properties.put("fields", fields.toString());
		}

		if (UtilMethods.isSet(contentImporterForm.getFilePath()))
			properties.put("filePath", contentImporterForm.getFilePath());

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

		String cronSecondsField = "0";
		String cronMinutesField = "0";
		String cronHoursField = "*";
		String cronDaysOfMonthField = "*";
		String cronMonthsField = "*";
		String cronDaysOfWeekField = "?";
		String cronYearsField = "*";

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

		String cronExpression = cronSecondsField + " " + cronMinutesField + " " + cronHoursField + " " + cronDaysOfMonthField + " " + cronMonthsField + " " + cronDaysOfWeekField + " " + cronYearsField;

		result = QuartzUtils.saveScheduler(contentImporterForm.getJobName(),
				contentImporterForm.getJobGroup(),
				contentImporterForm.getJobDescription(),
				"org.dotcms.plugins.contentImporter.quartz.ContentImporterThread",
				properties,
				startDate,
				endDate,
				cronExpression);

		SessionMessages.add(req, "message", "message.Scheduler.saved");

		return result;
	}

	private void _deleteScheduler(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form , User user) throws Exception {
		ContentImporterForm contentImporterForm = (ContentImporterForm) form;

		if (UtilMethods.isSet(contentImporterForm.getJobGroup()))
			QuartzUtils.deleteScheduler(contentImporterForm.getJobName(), contentImporterForm.getJobGroup());
		else
			QuartzUtils.deleteScheduler(req.getParameter("name"), req.getParameter("group"));

		SessionMessages.add(req, "message", "message.Scheduler.delete");
	}

	private void _populateForm(ActionForm form, Scheduler scheduler) {
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

			HashMap properties = scheduler.getProperties();
			contentImporterForm.setStructure(Long.parseLong((String) properties.get("structure")));

			long[] fields = {};
			if (UtilMethods.isSet(properties.get("fields"))) {
				String[] strFields = ((String) properties.get("fields")).split(",");
				List<Long> longFields = new ArrayList<Long>(strFields.length);
				for (String field: strFields) {
					longFields.add(Long.parseLong(field));
				}

				long[] tempArray = new long[longFields.size()];
				for (int pos = 0; pos < longFields.size(); ++pos) {
					tempArray[pos] = longFields.get(pos).longValue();
				}
				fields = tempArray;
			}

			contentImporterForm.setFields(fields);
			contentImporterForm.setFilePath((String) properties.get("filePath"));
			contentImporterForm.setReportEmail((String) properties.get("reportEmail"));
			contentImporterForm.setCsvSeparatorDelimiter((String) properties.get("csvSeparatorDelimiter"));
			contentImporterForm.setCsvTextDelimiter((String) properties.get("csvTextDelimiter"));			
			contentImporterForm.setLanguage((Long.parseLong((String) properties.get("language"))));

			if (UtilMethods.isSet(properties.get("publishContent")))
				contentImporterForm.setPublishContent(new Boolean((String) properties.get("publishContent")));

			contentImporterForm.setNewForm(false);
		} catch (Exception e) {
			Logger.warn(this, e.getMessage());
		}
	}
}