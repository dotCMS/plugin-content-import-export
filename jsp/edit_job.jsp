<%@ include file="/html/plugins/org.dotcms.plugins.contentImporter/init.jsp" %>
<%@ include file="/html/common/messages_inc.jsp" %>
<%@ page import="java.util.*" %>
<%@ page import="com.dotmarketing.beans.Host" %>
<%@ page import="com.dotmarketing.factories.HostFactory" %>
<%@ page import="javax.servlet.jsp.PageContext" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="org.dotcms.plugins.contentImporter.util.ContentImporterQuartzUtils" %>
<%@ page import="com.dotmarketing.portlets.structure.model.Structure" %>
<%@ page import="com.dotmarketing.portlets.structure.factories.StructureFactory" %>
<%@ page import="com.liferay.portal.model.User" %>
<%@ page import="com.liferay.portal.util.PortalUtil" %>
<%@ page import="com.liferay.portal.NoSuchUserException" %>
<%@ page import="org.dotcms.plugins.contentImporter.portlet.form.ContentImporterForm" %>
<%@ page import="com.dotmarketing.business.APILocator" %>

<%
	ContentImporterForm contentImporterForm = null;

	if (request.getAttribute("ContentImporterForm") != null) {
		contentImporterForm = (ContentImporterForm) request.getAttribute("ContentImporterForm");
	}
	
	java.util.Hashtable params = new java.util.Hashtable();
	params.put("struts_action", new String [] {"/ext/content_importer/view_jobs"} );
	
	String referrer = com.dotmarketing.util.PortletURLUtil.getRenderURL(request, javax.portlet.WindowState.MAXIMIZED.toString(), params);
	
	List<Structure> structures = StructureFactory.getStructuresWithWritePermissions(user, false);
	request.setAttribute("structures", structures);

	//Languages
	List<com.dotmarketing.portlets.languagesmanager.model.Language> languages = APILocator.getLanguageAPI().getLanguages();
	List<HashMap<String, Object>> languagesMap = new ArrayList<HashMap<String, Object>>();
	HashMap<String, Object> languageMap;
	for (com.dotmarketing.portlets.languagesmanager.model.Language language: languages) {
		languageMap = new HashMap<String, Object>();
		languageMap.put("id", language.getId());
		languageMap.put("description", language.getCountry() + " - " + language.getLanguage());
		languagesMap.add(languageMap);
	}
	languageMap = new HashMap<String, Object>();
	languageMap.put("id", -1);
	languageMap.put("description", "Multilingual File");
	languagesMap.add(languageMap);
	request.setAttribute("languages", languagesMap);
%>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value="Schedule a Content Import Task" />

<script type='text/javascript' src='/dwr/interface/StructureAjax.js'></script>
<script type='text/javascript' src='/dwr/engine.js'></script>
<script type='text/javascript' src='/dwr/util.js'></script>

<script language="Javascript">

function checkDate(element, fieldName) {
	if (element.checked) {
	  	eval("document.getElementById('" + fieldName + "Div').style.visibility = ''");
	} else {
		eval("document.getElementById('" + fieldName + "Div').style.visibility = 'hidden'");
	}
}

function <portlet:namespace />setCalendarDate_0(year, month, day) {	  
	document.forms[0].startDateYear.value = year;
	document.forms[0].startDateMonth.value = --month;
	document.forms[0].startDateDay.value = day;
	updateDate('startDate');
}

function <portlet:namespace />setCalendarDate_1(year, month, day) {	  
	document.forms[0].endDateYear.value = year;
	document.forms[0].endDateMonth.value = --month;
	document.forms[0].endDateDay.value = day;
	updateDate('endDate');
}

function <portlet:namespace />setCalendarDate_2(year, month, day) {	  
	document.forms[0].everyDateYear.value = year;
	document.forms[0].everyDateMonth.value = --month;
	document.forms[0].everyDateDay.value = day;
	updateDateOnly('everyDate');
}

function setCalendars(fieldName) {
	eval("var month = document.forms[0]." + fieldName + "Month.value");
	eval("var day =   document.forms[0]." + fieldName + "Day.value");
	eval("var year =  document.forms[0]." + fieldName + "Year.value");
	var date= month + "/" + day + "/" + year;
	eval("document.forms[0]." + fieldName + ".value = date");
}

	  function updateDate(field)
	  {
	  	eval("var year  = document.forms[0]." + field + "Year.value");
	  	eval("var month = document.forms[0]." + field + "Month.value");
	  	month = parseInt(month) + 1;
	  	eval("var day = document.forms[0]." + field + "Day.value");
	  	eval("var hour = document.forms[0]." + field + "Hour.value");
	  	eval("var minute = document.forms[0]." + field + "Minute.value");
	  	eval("var second = document.forms[0]." + field + "Second.value");
	  	
	  	var date = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second;
	  	eval("document.forms[0]." + field + ".value = date");
	  }
	  
	  function updateDateOnly(field)
	  {
	  	eval("var year  = document.forms[0]." + field + "Year.value");
	  	eval("var month = document.forms[0]." + field + "Month.value");
	  	month = parseInt(month) + 1;
	  	eval("var day = document.forms[0]." + field + "Day.value");
	  	
	  	var date = year + "-" + month + "-" + day;
	  	eval("document.forms[0]." + field + ".value = date");
	  }
	  
	  function updateFieldFromDate(field)
	  {		
	  	eval("var date = document.forms[0]." + field + ".value");
	  	var dateAux = date.split(" ")[0];
	  	var timeAux = date.split(" ")[1];
		
		if(dateAux != null)
		{
		  	var dateArray = dateAux.split("-");
		  	if(dateArray.length >= 3)
		  	{	  
			 	var year = dateArray[0];
			 	var month = dateArray[1];
			 	month = parseInt(trimZero(month)) - 1;
		  		var day = dateArray[2];
		  		day = parseInt(trimZero(day));
	  		  
			  	eval("document.forms[0]." + field + "Day.value = day");
			 	eval("document.forms[0]." + field + "Month.value = month");
				eval("document.forms[0]." + field + "Year.value = year");	
	  		}
	  	}
	  	
	  	if(timeAux != null)
	  	{
		  	var timeArray = timeAux.split(":");
		  	if (timeArray.length >= 2)
		  	{
		  		var hour = timeArray[0];
		  		hour = parseInt(trimZero(hour));
			  	var minute = timeArray[1];
			  	var second = timeArray[2];
	  		  
			  	eval("document.forms[0]." + field + "Hour.value = hour");
			  	eval("document.forms[0]." + field + "Minute.value = minute");
			  	eval("document.forms[0]." + field + "Second.value = second");
		  	}	  	
	  	}
	  	amPm(field);
	  }
	  
	  function amPm(fieldName)
	  {
		var ele = document.getElementById(fieldName + "PM");
		eval("var val = document.forms[0]." + fieldName + "Hour.value");

		if(val > 11)
		{
			ele.innerHTML = "<font class=\"bg\" size=\"2\">PM</font>";
		}
		else
		{
			ele.innerHTML = "<font class=\"bg\" size=\"2\">AM</font>";
		}
	}

<liferay:include page="/html/js/calendar/calendar_js_box_ext.jsp" flush="true">
	<liferay:param name="calendar_num" value="<%= Integer.toString(3) %>" />
</liferay:include>

function submitfm(form) {
	if (validate()) {
		form.<portlet:namespace />cmd.value = '<%=Constants.ADD%>';
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/content_importer/edit_jobs" /></portlet:actionURL>';
		form.<portlet:namespace />redirect.value = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/content_importer/edit_jobs" /></portlet:renderURL>';
		form.referrer.value = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/content_importer/view_jobs" /><portlet:param name="group" value="<%= ContentImporterQuartzUtils.quartzGroup %>" /></portlet:renderURL>';
		submitForm(form);
	}
	
}

function cancelEdit() {
	self.location = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/content_importer/view_jobs" /></portlet:renderURL>';
}


function deleteSchedule(form) {
	if(confirm("Are you sure you want to delete this schedule (this cannot be undone) ?")){
		form.<portlet:namespace />cmd.value = '<%=Constants.DELETE%>';
		form.<portlet:namespace />redirect.value = '<%= referrer %>';
		form.referrer.value = '<%= referrer %>';
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/content_importer/edit_jobs" /></portlet:actionURL>';
		submitForm(form);
	}
}
function browseTree() {
    var content = 'htmlpages';
    var popup = 'htmlinode';
    view = "<%= java.net.URLEncoder.encode("(working=" + com.dotmarketing.db.DbConnectionFactory.getDBTrue() + " and deleted = " + com.dotmarketing.db.DbConnectionFactory.getDBFalse() + ")","UTF-8") %>";
	pictureWindow = window.open('<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/folders/view_folders_popup" /></portlet:actionURL>&view=' + view + '&content='+content+'&popup='+popup+'&child=true', "newwin", 'width=700,height=400,scrollbars=yes,resizable=yes');
}
function submitParent() {
	var inode = document.getElementById("htmlinode").value;
	form = document.getElementById("fm");
	form.<portlet:namespace />cmd.value = '<%=Constants.EDIT%>';
	form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/content_importer/edit_jobs" /></portlet:actionURL>';
	submitForm(form);
}

	function validate() {

		if (trimString(document.getElementById("jobName").value) == '') {
			alert("The Task Description is required");
			return false;
		}
	
		if (document.getElementById("atInfo").checked) {
			if (!document.forms[0].at[0].checked &&
				!document.forms[0].at[1].checked) {
				alert("At least one AT option must be selected");
				return false;
			}
		}
		
		if (document.getElementById("everyInfo").checked) {
			if (!document.forms[0].every[0].checked &&
				!document.forms[0].every[1].checked) {
				alert("At least one EVERY option must be selected");
				return false;
			} else if (document.forms[0].every[1].checked) {
				var selected = false;
				for (var i = 0; i < document.forms[0].everyDay.length; ++i) {
					if (document.forms[0].everyDay[i].checked) {
						selected = true;
						break;
					}
				}
				
				if (!selected) {
					alert("At least one week day must be selected");
					return false;
				}
			}
		}
		
		var atInfo = document.getElementsByName("atInfo")[0];
		if (atInfo.checked) {
			var at = document.getElementsByName("at");
			if (at[1].checked) {
				var betweenFromHourObj = document.getElementsByName("betweenFromHour")[0];
				var betweenFromHour = betweenFromHourObj[betweenFromHourObj.selectedIndex].value;
				var betweenToHourObj = document.getElementsByName("betweenToHour")[0];
				var betweenToHour = betweenToHourObj[betweenToHourObj.selectedIndex].value;

				if (parseInt(betweenToHour) < parseInt(betweenFromHour) ) {
					alert("The 'From Hour' must be lesser than 'To Hour'");
					return false;
				}
				
				if (document.getElementById("eachInfo").checked) {
					var hours = parseInt(document.getElementById("eachHours").value);
					var minutes = parseInt(document.getElementById("eachMinutes").value);
					
					if ((isNaN(hours) &&
						 isNaN(minutes)) ||
						((hours == 0) &&
						 (minutes == 0)) ||
						(isNaN(hours) &&
						 (minutes == 0)) ||
						((hours == 0) &&
						 isNaN(minutes))) {
						alert("Must specify each hours and/or minutes with a value bigger than 0.");
						return false;
					}
				}
			} else {
				document.getElementById("eachHours").value = "";
				document.getElementById("eachMinutes").value = "";
			}
		}

		if (trimString(document.getElementById("filePath").value) == '') {
			alert("The File Path is required");
			return false;
		}

		if (document.ContentImporterForm.fields != null) {
			var isIdentifierKeyField = false;

			for (var i = 0; i < document.ContentImporterForm.fields.length; ++i) {
				if ((document.ContentImporterForm.fields[i].value == '0') && document.ContentImporterForm.fields[i].checked) {
					isIdentifierKeyField = true;
					break;
				}
			}
			
			if (isIdentifierKeyField && (trimString(document.getElementById("identifierFieldName").value) == '')) {
				alert("The Identifier Field Name is required");
				return false;
			}
		}
		
		return true;
	}

	function structureChanged() {
		var select = document.getElementById("structure");
		var inode = select.options[select.selectedIndex].value;
		StructureAjax.getKeyStructureFields(inode, fillFields);
	}

	function fillFields(data) {
		currentStructureFields = data;
		DWRUtil.removeAllRows("import_fields_table");
		
		var identifier = new Array();
		identifier["fieldName"] = "Identifier";
		identifier["inode"] = "0";
		identifier["fieldIndexed"] = true;

		var newData = new Array();
		newData.push(identifier);

		newData = newData.concat(data);
		
		DWRUtil.addRows("import_fields_table", newData, [fieldCheckbox], { escapeHtml: false });	
	}

	function fieldCheckbox(field) {
		var fieldName = field["fieldName"];
		var fieldInode = field["inode"];
		var fieldIndexed = field["fieldIndexed"];
		var disableField = "";
		if(!fieldIndexed){
		   disableField = "disabled"
		}
		
		<% 
			long[] fields = contentImporterForm.getFields();
			for (int i = 0; i < fields.length; i++) {

		%>
		if ((fieldInode == <%= fields[i] %>) && fieldIndexed)
			return "<input checked type=\"checkbox\" id=\"" + fieldInode + "Field\" name=\"fields\" value=\"" + fieldInode + "\" " + disableField + " /> " + fieldName;
		<%			
			
			} 
		%>
		return "<input type=\"checkbox\" id=\"" + fieldInode + "Field\" name=\"fields\" value=\"" + fieldInode + "\" " + disableField + " /> " + fieldName;
	}

	function languageChanged() {
		var select = document.getElementById("languageSelect");
		var languageId = select.options[select.selectedIndex].value;
		if (-1 < languageId) {
				document.getElementById("multiLingualImportNotes").style.display="none";
		} else {
			document.getElementById("multiLingualImportNotes").style.display="block";
		}
	}
</script>

<html:form action="/ext/content_importer/edit_jobs" styleId="fm">

<table border="0" width="100%" cellpadding="0" cellspacing="0">
	<tr>
		<td><img border="0" height="8" hspace="0" src="/html/skin/image/common/spacer.gif" vspace="0" width="1"></td>
	</tr>
</table>

	<table border="0" cellpadding="2" cellspacing="2" id="main">
		<tr>
			<td>
				<table border="0" cellpadding="2" cellspacing="2">
				<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="">
				<input name="<portlet:namespace />redirect" type="hidden" value="">
				<input name="referrer" type="hidden" value="">
				<input type="hidden" name="jobGroup" id="jobGroup" value="<%= ContentImporterQuartzUtils.quartzGroup %>">
				<html:hidden property="editMode" />
				
				<tr>
					<td>
						<font class="bg" size="2"><b>Task Name:</b></font>
					</td>				
					<td>
						<html:text styleClass="form-text" size="40" property="jobName" styleId="jobName" />
					</td>
				</tr>
				<tr>
					<td>
						<font class="bg" size="2"><b>Task Description:</b></font>
					</td>				
					<td>
						<html:text styleClass="form-text" size="40" property="jobDescription" />
					</td>
				</tr>
				<tr>
					<td valign="top" rowspan="2">
						<font class="bg" size="2"><b>Execute:</b></font>
					</td>				
					<td>
						<table cellpadding="0" cellspacing="0">
<%
	SimpleDateFormat sdf = new SimpleDateFormat(com.dotmarketing.util.WebKeys.DateFormats.DOTSCHEDULER_DATE);

	int[] monthIds = CalendarUtil.getMonthIds();
	String[] months = CalendarUtil.getMonths(locale);

	int currentYear = GregorianCalendar.getInstance().get(Calendar.YEAR);
	int previous = 100;
%>
							<tr>
							    <td>
							    	From
							    </td>
								<td>
<%
	Calendar startDateCalendar = null;
	Date startDate;
	try {
		startDate = sdf.parse(contentImporterForm.getStartDate());
	} catch (Exception e) {
		try {
			SimpleDateFormat sdf2 = new SimpleDateFormat(com.dotmarketing.util.WebKeys.DateFormats.DOTSCHEDULER_DATE2);
			startDate = sdf2.parse(contentImporterForm.getStartDate());
		} catch (Exception ex) {
			startDate = new Date();
		}
	}

	if (contentImporterForm.isHaveStartDate() ||
		!UtilMethods.isSet(contentImporterForm.getJobGroup()) ||
		(UtilMethods.isSet(contentImporterForm.getJobGroup()) &&
		 !contentImporterForm.isHaveStartDate())) {
		startDateCalendar = GregorianCalendar.getInstance();
		startDateCalendar.setTime(startDate);
	}
%>
									<html:checkbox property="haveStartDate" onclick="checkDate(this, 'startDate')" styleId="haveStartDate" />
								</td>
								<td>
									<div id="startDateDiv">
									<table>
										<tr>
											<td>
												<select name="startDateMonth" onChange="updateDate('startDate');">
<%
	int startDateMonth = -1;

	if (startDateCalendar != null)
		startDateMonth = startDateCalendar.get(Calendar.MONTH);

	for (int i = 0; i < months.length; i++) {
%>
													<option <%= startDateMonth == monthIds[i] ? "selected" : "" %> value="<%= monthIds[i] %>"><%= months[i] %></option>
<%
	}
%>
												</select>
											</td>
											<td>
												<select name="startDateDay" onChange="updateDate('startDate');">
<%
	int startDateDay = -1;

	if (startDateCalendar != null)
		startDateDay = startDateCalendar.get(Calendar.DAY_OF_MONTH);

	for (int i = 1; i <= 31; i++) {
%>
													<option <%= startDateDay == i ? "selected" : "" %> value="<%= i %>"><%= i %></option>
<%
	}
%>
												</select>
											</td>
											<td>
												<select name="startDateYear" onChange="updateDate('startDate');">
<%
	int startDateYear = -1;

	if (startDateCalendar != null)
		startDateYear = startDateCalendar.get(Calendar.YEAR);

	for (int i = currentYear - previous; i <= currentYear + 10; i++) {
%>
													<option <%= startDateYear == i ? "selected" : "" %> value="<%= i %>"><%= i %></option>
<%
	}
%>
												</select>
											</td>
											<td>
												<img align="absmiddle" border="0" hspace="0" id="<portlet:namespace />calendar_input_0_button" src="<%= COMMON_IMG %>/calendar/calendar.gif" vspace="0" onClick="<portlet:namespace />calendarOnClick_0();">
											</td>
											<td nowrap="nowrap">
												<select name="startDateHour" onChange="amPm('startDate');updateDate('startDate');">
<%
	int startDateHour = -1;

	if (startDateCalendar != null)
		startDateHour = startDateCalendar.get(Calendar.HOUR_OF_DAY);

	for (int i = 0; i < 24; i++) 
	{
		int val = i > 12 ?  i - 12: i;
		if (val == 0)
			val = 12;
%>
													<option <%= startDateHour == i ? "selected" : "" %> value="<%= i %>"><%= val %></option>
<%
	}
%>
												</select> :
											</td>
											<td nowrap="nowrap">
												<select name="startDateMinute" onChange="updateDate('startDate');">
<%
	int startDateMinute = -1;

	if (startDateCalendar != null)
		startDateMinute = startDateCalendar.get(Calendar.MINUTE);

	for (int i = 0; i < 60; ++i) {
		String val = (i < 10) ? "0" + i: String.valueOf(i);
%>
													<option <%= startDateMinute == i ? "selected" : "" %> value="<%= val %>"><%= val %></option>
<%
	}
%>
												</select> :
											</td>
											<td nowrap="nowrap">
												<select name="startDateSecond" onChange="updateDate('startDate');">
<%
	int startDateSecond = -1;

	if (startDateCalendar != null)
		startDateSecond = startDateCalendar.get(Calendar.SECOND);

	for (int i = 0; i < 60; ++i) {
		String val = (i < 10) ? "0" + i: String.valueOf(i);
%>
													<option <%= startDateSecond == i ? "selected" : "" %> value="<%= val %>"><%= val %></option>
<%
	}
%>
												</select>
											</td>
											<td>
												<span id="startDatePM"><font class="bg" size="2">AM</font></span>
											</td>
											<td>&nbsp;
												
											</td>
										</tr>
									</table>
									</div>
								</td>
							</tr>
							<input type="hidden" name="startDate" value="" id="startDate">
							<script language="javascript">
<%
	if (!UtilMethods.isSet(contentImporterForm.getJobGroup())) {
%>
								document.getElementById('haveStartDate').checked = true;
<%
	}
%>
								checkDate(document.forms[0].haveStartDate, 'startDate');
								amPm('startDate');
								updateDate('startDate');
							</script>
							<tr>
								<td>
									To
								</td>
								<td>
<%
	Calendar endDateCalendar = null;
	Date endDate;
	try {
		endDate = sdf.parse(contentImporterForm.getEndDate());
	} catch (Exception e) {
		try {
			SimpleDateFormat sdf2 = new SimpleDateFormat(com.dotmarketing.util.WebKeys.DateFormats.DOTSCHEDULER_DATE2);
			endDate = sdf2.parse(contentImporterForm.getEndDate());
		} catch (Exception ex) {
			endDate = new Date();
		}
	}

	if (contentImporterForm.isHaveEndDate() ||
		!UtilMethods.isSet(contentImporterForm.getJobGroup()) ||
		(UtilMethods.isSet(contentImporterForm.getJobGroup()) &&
		 !contentImporterForm.isHaveEndDate())) {
		endDateCalendar = GregorianCalendar.getInstance();
		endDateCalendar.setTime(endDate);
	}
%>
									<html:checkbox property="haveEndDate" onclick="checkDate(this, 'endDate')" styleId="haveEndDate" />
								</td>
								<td>
									<div id="endDateDiv">
									<table>
										<tr>
											<td>
												<select name="endDateMonth" onChange="updateDate('endDate');">
<%
	int endDateMonth = -1;

	if (endDateCalendar != null)
		endDateMonth = endDateCalendar.get(Calendar.MONTH);

	for (int i = 0; i < months.length; i++) {
%>
													<option <%= endDateMonth == monthIds[i] ? "selected" : "" %> value="<%= monthIds[i] %>"><%= months[i] %></option>
<%
	}
%>
												</select>
											</td>
											<td>
												<select name="endDateDay" onChange="updateDate('endDate');">
<%
	int endDateDay = -1;

	if (endDateCalendar != null)
		endDateDay = endDateCalendar.get(Calendar.DAY_OF_MONTH);

	for (int i = 1; i <= 31; i++) {
%>
													<option <%= endDateDay == i ? "selected" : "" %> value="<%= i %>"><%= i %></option>
<%
	}
%>
												</select>
											</td>
											<td>
												<select name="endDateYear" onChange="updateDate('endDate');">
<%
	int endDateYear = -1;

	if (endDateCalendar != null)
		endDateYear = endDateCalendar.get(Calendar.YEAR);

	for (int i = currentYear - previous; i <= currentYear + 10; i++) {
%>
													<option <%= endDateYear == i ? "selected" : "" %> value="<%= i %>"><%= i %></option>
<%
	}
%>
												</select>
											</td>
											<td>
												<img align="absmiddle" border="0" hspace="0" id="<portlet:namespace />calendar_input_1_button" src="<%= COMMON_IMG %>/calendar/calendar.gif" vspace="0" onClick="<portlet:namespace />calendarOnClick_1();">
											</td>
											<td nowrap="nowrap">
												<select name="endDateHour" onChange="amPm('endDate');updateDate('endDate');">
<%
	int endDateHour = -1;

	if (endDateCalendar != null)
		endDateHour = endDateCalendar.get(Calendar.HOUR_OF_DAY);

	for (int i = 0; i < 24; i++) 
	{
		int val = i > 12 ?  i - 12: i;
		if (val == 0)
			val = 12;
%>
													<option <%= endDateHour == i ? "selected" : "" %> value="<%= i %>"><%= val %></option>
<%
	}
%>
												</select> :
											</td>
											<td nowrap="nowrap">
												<select name="endDateMinute" onChange="updateDate('endDate');">
<%
	int endDateMinute = -1;

	if (endDateCalendar != null)
		endDateMinute = endDateCalendar.get(Calendar.MINUTE);

	for (int i = 0; i < 60; ++i) {
		String val = (i < 10) ? "0" + i: String.valueOf(i);
%>
													<option <%= endDateMinute == i ? "selected" : "" %> value="<%= val %>"><%= val %></option>
<%
	}
%>
												</select> :
											</td>
											<td nowrap="nowrap">
												<select name="endDateSecond" onChange="updateDate('endDate');">
<%
	int endDateSecond = -1;

	if (endDateCalendar != null)
		endDateSecond = endDateCalendar.get(Calendar.SECOND);

	for (int i = 0; i < 60; ++i) {
		String val = (i < 10) ? "0" + i: String.valueOf(i);
%>
													<option <%= endDateSecond == i ? "selected" : "" %> value="<%= val %>"><%= val %></option>
<%
	}
%>
												</select>
											</td>
											<td>
												<span id="endDatePM"><font class="bg" size="2">AM</font></span>
											</td>
											<td>&nbsp;
												
											</td>
										</tr>
									</table>
									</div>
								</td>
							</tr>
							<input type="hidden" name="endDate" value="" id="endDate">
							<script language="javascript">
<%
	if (!UtilMethods.isSet(contentImporterForm.getJobGroup())) {
%>
								document.getElementById('haveEndDate').checked = true;
<%
	}
%>
								checkDate(document.forms[0].haveEndDate, 'endDate');
								amPm('endDate');
								updateDate('endDate');
							</script>
						</table>
					</td>
				</tr>
				<tr>
					<td>
						<table cellpadding="0" cellspacing="0">
							<tr>
								<!--td-->
								<td colspan="3">
<%
	contentImporterForm.setAtInfo(true);
%>
									<div style="display: none;">
										<html:checkbox property="atInfo" styleId="atInfo" />
									</div>
								<!--/td>
								<td colspan="2"-->
									<font class="bg" size="2">&nbsp;&nbsp;<strong>at</strong></font>								
                                    </td>
							</tr>
							<tr>
								<td>&nbsp;
									
								</td>
								<td>
									<table>
										<tr>
											<td>
												<input type="radio" name="at" id="at" value="isTime" <%= UtilMethods.isSet(contentImporterForm.getAt()) && contentImporterForm.getAt().equals("isTime") ? "checked" : "" %> >
											</td>
										</tr>
									</table>
								</td>
								<td>
									<table>
										<tr>
											<td>
												<select name="atTimeHour" onChange="amPm('atTime');">
<%
	for (int i = 0; i < 24; i++) 
	{
		int val = i > 12 ?  i - 12: i;
		if (val == 0)
			val = 12;
%>
													<option <%= contentImporterForm.getAtTimeHour() == i ? "selected" : "" %> value="<%= i %>"><%= val %></option>
<%
	}
%>
												</select> :
											</td>
											<td>
												<select name="atTimeMinute">
<%
	for (int i = 0; i < 60; ++i) {
		String val = (i < 10) ? "0" + i: String.valueOf(i);
%>
													<option <%= contentImporterForm.getAtTimeMinute() == i ? "selected" : "" %> value="<%= i %>"><%= val %></option>
<%
	}
%>
												</select> :
											</td>
											<td>
												<select name="atTimeSecond">
<%
	for (int i = 0; i < 60; ++i) {
		String val = (i < 10) ? "0" + i: String.valueOf(i);
%>
													<option <%= contentImporterForm.getAtTimeSecond() == i ? "selected" : "" %> value="<%= i %>"><%= val %></option>
<%
	}
%>
												</select>
											</td>
											<td>
												<span id="atTimePM"><font class="bg" size="2">AM</font></span>
											</td>
										</tr>
									</table>
									<script language="javascript">
										amPm('atTime');
									</script>
								</td>
							</tr>
							<tr>
								<td>&nbsp;
									
								</td>
								<td>
									<table>
										<tr>
											<td>
												<input type="radio" name="at" id="at" value="isBetween" <%= UtilMethods.isSet(contentImporterForm.getAt()) && contentImporterForm.getAt().equals("isBetween") ? "checked" : "" %> >
											</td>
										</tr>
									</table>
								</td>
								<td>
									<table>
										<tr>
											<td>
												<font class="bg" size="2">between</font>
											</td>
											<td>
												<select name="betweenFromHour" onChange="amPm('betweenFrom');">
<%
	for (int i = 0; i < 24; i++) 
	{
		int val = i > 12 ?  i - 12: i;
		if (val == 0)
			val = 12;
%>
													<option <%= contentImporterForm.getBetweenFromHour() == i ? "selected" : "" %> value="<%= i %>"><%= val %></option>
<%
	}
%>
												</select>
											</td>
											<td>
												<span id="betweenFromPM"><font class="bg" size="2">AM</font></span>
												<script language="javascript">
													amPm('betweenFrom');
												</script>
											</td>
											<td>&nbsp;</td>
											<td>
												<select name="betweenToHour" onChange="amPm('betweenTo');">
<%
	for (int i = 0; i < 24; i++) 
	{
		int val = i > 12 ?  i - 12: i;
		if (val == 0)
			val = 12;
%>
													<option <%= contentImporterForm.getBetweenToHour() == i ? "selected" : "" %> value="<%= i %>"><%= val %></option>
<%
	}
%>
												</select>
											</td>
											<td>
												<span id="betweenToPM"><font class="bg" size="2">AM</font></span>
												<script language="javascript">
													amPm('betweenTo');
												</script>
											</td>
										</tr>
									</table>
								</td>
							</tr>
							<tr>
								<td>
<%
	contentImporterForm.setEachInfo(true);
%>
									<div style="display: none;">
										<html:checkbox property="eachInfo" styleId="eachInfo" />
									</div>
								</td>
								<td>&nbsp;
									
								</td>
								<td>
									<table>
										<tr>
											<td>
												<font class="bg" size="2">each <input type="text" class="form-text" name="eachHours" id="eachHours" maxlength="3" size="3" <%= 0 < contentImporterForm.getEachHours() ? "value=\"" + contentImporterForm.getEachHours() + "\"" : "" %> > hours and <input type="text" class="form-text" name="eachMinutes" id="eachMinutes" maxlength="3" size="3" <%= 0 < contentImporterForm.getEachMinutes() ? "value=\"" + contentImporterForm.getEachMinutes() + "\"" : "" %> > minutes</font>
											</td>
										</tr>
									</table>
								</td>
							</tr>
<%
		if (!contentImporterForm.isEveryInfo())
			contentImporterForm.setEvery("");
%>
							<tr>
								<td>
									<html:checkbox property="everyInfo" styleId="everyInfo" />
								</td>
								<td colspan="2">
									<font class="bg" size="2">every</font>
								</td>
							</tr>
							<tr>
								<td>&nbsp;
									
								</td>
								<td>
									<table>
										<tr>
											<td>
												<input type="radio" name="every" id="every" value="isDate" <%= UtilMethods.isSet(contentImporterForm.getEvery()) && contentImporterForm.getEvery().equals("isDate") ? "checked" : "" %> >
											</td>
										</tr>
									</table>
								</td>
								<td>
									<table>
										<tr>
											<td>
												<select name="everyDateMonth" onChange="updateDateOnly('everyDate');">
													<option value="*">-</option>
<%
	for (int i = 0; i < months.length; i++) {
%>
													<option <%= (contentImporterForm.getEveryDateMonth()-1) == monthIds[i] ? "selected" : "" %> value="<%= monthIds[i] %>"><%= months[i] %></option>
<%
	}
%>
												</select>
											</td>
											<td>
												<select name="everyDateDay" onChange="updateDateOnly('everyDate');">
													<option value="*">-</option>
<%
	for (int i = 1; i <= 31; i++) {
%>
													<option <%= contentImporterForm.getEveryDateDay() == i ? "selected" : "" %> value="<%= i %>"><%= i %></option>
<%
	}
%>
												</select>
											</td>
											<td>
												<select name="everyDateYear" onChange="updateDateOnly('everyDate');">
													<option value="*">-</option>
<%
	for (int i = currentYear - previous; i <= currentYear + 10; i++) {
%>
													<option <%= contentImporterForm.getEveryDateYear() == i ? "selected" : "" %> value="<%= i %>"><%= i %></option>
<%
	}
%>
												</select>
											</td>
											<td>
												<img align="absmiddle" border="0" hspace="0" id="<portlet:namespace />calendar_input_2_button" src="<%= COMMON_IMG %>/calendar/calendar.gif" vspace="0" onClick="<portlet:namespace />calendarOnClick_2();">
											</td>
										</tr>
									</table>
									<input type="hidden" name="everyDate" value="" id="everyDate">
									<script language="javascript">
										updateDateOnly('everyDate');
									</script>
								</td>
							</tr>
							<tr>
								<td>&nbsp;
									
								</td>
								<td valign="top">
									<table>
										<tr>
											<td>
												<input type="radio" name="every" id="every" value="isDays" <%= UtilMethods.isSet(contentImporterForm.getEvery()) && contentImporterForm.getEvery().equals("isDays") ? "checked" : "" %> >
											</td>
										</tr>
									</table>
								</td>
								<td>
									<table>
										<tr>
											<td>
												Mon
											</td>
											<td>
												Tue
											</td>
											<td>
												Wed
											</td>
											<td>
												Thu
											</td>
											<td>
												Fri
											</td>
											<td>
												Sat
											</td>
											<td>
												Sun
											</td>
										</tr>
										<tr>
											<td>
												<input type="checkbox" name="everyDay" id="everyDay" value="MON" <%= contentImporterForm.isMonday() ? "checked" : "" %> >
											</td>
											<td>
												<input type="checkbox" name="everyDay" id="everyDay" value="TUE" <%= contentImporterForm.isTuesday() ? "checked" : "" %> >
											</td>
											<td>
												<input type="checkbox" name="everyDay" id="everyDay" value="WED" <%= contentImporterForm.isWednesday() ? "checked" : "" %> >
											</td>
											<td>
												<input type="checkbox" name="everyDay" id="everyDay" value="THU" <%= contentImporterForm.isThusday() ? "checked" : "" %> >
											</td>
											<td>
												<input type="checkbox" name="everyDay" id="everyDay" value="FRI" <%= contentImporterForm.isFriday() ? "checked" : "" %> >
											</td>
											<td>
												<input type="checkbox" name="everyDay" id="everyDay" value="SAT" <%= contentImporterForm.isSaturday() ? "checked" : "" %> >
											</td>
											<td>
												<input type="checkbox" name="everyDay" id="everyDay" value="SUN" <%= contentImporterForm.isSunday() ? "checked" : "" %> >
											</td>
										</tr>
									</table>
								</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>
						<font class="bg" size="2"><b>Structure to Import:</b></font>
					</td>				
					<td>
						<html:select property="structure" styleId="structure" onchange="structureChanged()">
							<html:options collection="structures" property="inode" labelProperty="name" />
						</html:select>
					</td>
				</tr>
				<tr>
				<td width="30%" style="vertical-align: top">
					<font class="bg" size="2"><b>Language of the Contents to Import:</b></font>
				</td>
				<td>
					<html:select property="language" styleId="languageSelect" onchange="languageChanged()">
						<html:options collection="languages" property="id" labelProperty="description" />
					</html:select>
					<div id="multiLingualImportNotes" style="display: none">
						<font class="bg" size="1">
							<b>Note:</b>
						<p>
							In order to import correctly a multilingual file:
							<ol>
								<li>The CSV file must saved using "UTF-8" enconding</li>
								<li>There CSV file must have two extra fields: "languageCode" and "countryCode"</li>
								<li>A key field must be selected</li>
							</ol>
						</p>
						</font>
					</div>
				</td>
			</tr>
				<tr>
					<td width="30%" valign="top">
						<font class="bg" size="2"><b>Key Fields:</b></font>
					</td>
					<td>
						<table>
							<tbody id="import_fields_table"> </tbody>
						</table>
					</td>
				</tr>
				<tr>
					<td>
						<font class="bg" size="2"><b>File Path:</b></font>
					</td>				
					<td>
						<html:text styleClass="form-text" property="filePath" styleId="filePath" size="75" />
					</td>
				</tr>
				<tr>
					<td>
						<font class="bg" size="2"><b>Report Email:</b></font>
					</td>				
					<td>
						<html:text styleClass="form-text" property="reportEmail" size="75" />
					</td>
				</tr>
				<tr>
					<td>
						<font class="bg" size="2"><b>CSV Separator Delimiter:</b></font>
					</td>				
					<td>
						<html:text styleClass="form-text" maxlength="1" property="csvSeparatorDelimiter" size="3" />
					</td>
				</tr>
				<tr>
					<td>
						<font class="bg" size="2"><b>CSV Text Delimiter:</b></font>
					</td>				
					<td>
						<html:text styleClass="form-text" maxlength="1" property="csvTextDelimiter" size="3" />
					</td>
				</tr>								
				<tr>
					<td>
						<font class="bg" size="2"><b>Publish content:</b></font>
					</td>				
					<td>
						<html:checkbox styleClass="form-text" property="publishContent" value="true" />
					</td>
				</tr>
				</table>
			</td>
		</tr>
		<tr>
			<td align="center">
				<br>
				<br>
				<table border="0" cellpadding="2" cellspacing="2">
				<tr>
					<td align=center colspan="2">
						<BR>
						<% if ((contentImporterForm != null) && (!contentImporterForm.isNewForm())) { %>
						<input type="button" class="form-btn" onClick="deleteSchedule(document.getElementById('fm'))" value="Delete">
						<% } %>
						<input type="button" class="form-btn" onClick="cancelEdit()" value="Cancel">
						<input type="button" class="form-btn" onClick="submitfm(document.getElementById('fm'))" value="Save">
					</td>
				</tr>
				</table>
			</td>
		</tr>
	</table>
</html:form>
</liferay:box>

<script type="text/javascript">
	var select = document.getElementById("structure");
	var index = select.selectedIndex;
	if (-1 < index) {
		structureChanged(select.options[index].value);
	}
</script>