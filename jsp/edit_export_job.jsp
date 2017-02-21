<%@page import="org.dotcms.plugins.contentImporter.util.ContentletUtil"%>
<%@ page import="com.dotcms.repackage.javax.portlet.WindowState" %>
<%@ include file="/html/plugins/org.dotcms.plugins.contentImporter/init.jsp" %>
<%@ include file="/html/common/messages_inc.jsp" %>
<%@ page import="java.util.*" %>
<%@ page import="com.dotmarketing.beans.Host" %>
<%@ page import="javax.servlet.jsp.PageContext" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="org.dotcms.plugins.contentImporter.util.ContentImporterQuartzUtils" %>
<%@ page import="com.dotmarketing.portlets.structure.model.Structure" %>
<%@ page import="com.dotmarketing.portlets.structure.factories.StructureFactory" %>
<%@ page import="com.liferay.portal.model.User" %>
<%@ page import="com.liferay.portal.util.PortalUtil" %>
<%@ page import="com.liferay.portal.NoSuchUserException" %>
<%@ page import="org.dotcms.plugins.contentImporter.portlet.form.ContentExporterForm" %>
<%@ page import="com.dotmarketing.business.APILocator" %>

<%
	ContentExporterForm contentExporterForm = null;

	if (request.getAttribute("ContentExporterForm") != null) {
		contentExporterForm = (ContentExporterForm) request.getAttribute("ContentExporterForm");
	}
	
	java.util.Hashtable params = new java.util.Hashtable();
	params.put("struts_action", new String [] {"/ext/content_importer/view_export_jobs"} );
	
	String referrer = com.dotmarketing.util.PortletURLUtil.getRenderURL(request, com.dotcms.repackage.javax.portlet.WindowState.MAXIMIZED.toString(), params);
	
	List<Structure> structures = StructureFactory.getStructuresWithWritePermissions(user, false);
	request.setAttribute("structures", structures);

	//Languages
	List<com.dotmarketing.portlets.languagesmanager.model.Language> languages = APILocator.getLanguageAPI().getLanguages();
	List<HashMap<String, Object>> languagesMap = new ArrayList<HashMap<String, Object>>();
	HashMap<String, Object> languageMap;
	
	languageMap = new HashMap<String, Object>();
	languageMap.put("id", -1);
	languageMap.put("description", LanguageUtil.get(pageContext,"all-language"));
	languagesMap.add(languageMap);
	
	for (com.dotmarketing.portlets.languagesmanager.model.Language language: languages) {
		languageMap = new HashMap<String, Object>();
		languageMap.put("id", language.getId());
		languageMap.put("description", language.getCountry() + " - " + language.getLanguage());
		languagesMap.add(languageMap);
	}
	
	request.setAttribute("languages", languagesMap);
%>


<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.liferay.util.cal.CalendarUtil"%>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value="<%=LanguageUtil.get(pageContext,\"Add-Edit-Content-Export-Job\") %>" />

<script type='text/javascript' src='/dwr/interface/ContentExporterAjax.js'></script>
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
	
	
	function <portlet:namespace />setCalendarDate_0(year, month, day){
		dijit.byId('startDateYear').attr('value', year);
		dijit.byId('startDateMonth').attr('value', (parseInt(month) - 1));
		dijit.byId('startDateDay').attr('value', day);
		updateDate('startDate');
	}

	function <portlet:namespace />setCalendarDate_1(year, month, day){
		dijit.byId('endDateYear').attr('value', year);
		dijit.byId('endDateMonth').attr('value', (parseInt(month) - 1));
		dijit.byId('endDateDay').attr('value', day);
		updateDate('endDate');
	}

	function <portlet:namespace />setCalendarDate_2(year, month, day){
		dijit.byId('everyDateYear').attr('value', year);
		dijit.byId('everyDateMonth').attr('value', (parseInt(month) - 1));
		dijit.byId('everyDateDay').attr('value', day);
		updateDate('everyDate');
	}
	
	function setCalendars(fieldName) {
		var month = dijit.byId(fieldName + "Month").value;
		var day = dijit.byId(fieldName + "Day").value;
		var year = dijit.byId(fieldName + "Year").value;
		var date= month + "/" + day + "/" + year;
		dijit.byId(fieldName).attr('value', date);
	}
	
	function updateDate(field)
	{
		var year  = dijit.byId(field + "Year").value;
		var month = dijit.byId(field + "Month").value;
		month = parseInt(month) + 1;
		var day = dijit.byId(field + "Day").value;
		var hour = dijit.byId(field + "Hour").value;
		var minute = dijit.byId(field + "Minute").value;
		var second = dijit.byId(field + "Second").value;
				
		var date = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second;
		document.getElementById(field).value= date;
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
		var val = dijit.byId(fieldName + "Hour").value;
	
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
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/content_importer/edit_export_job" /></portlet:actionURL>';
		form.<portlet:namespace />redirect.value = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/content_importer/edit_export_job" /></portlet:renderURL>';
		form.referrer.value = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/content_importer/view_export_jobs" /><portlet:param name="group" value="<%= ContentImporterQuartzUtils.quartzExportGroup %>" /></portlet:renderURL>';
		submitForm(form);
	}
	
}

function cancelEdit() {
	self.location = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/content_importer/view_export_jobs" /></portlet:renderURL>';
}


function deleteSchedule(form) {
	if(confirm("<%=LanguageUtil.get(pageContext,"content-exporter-delete-job-schedule")%>")){
		form.<portlet:namespace />cmd.value = '<%=Constants.DELETE%>';
		form.<portlet:namespace />redirect.value = '<%= referrer %>';
		form.referrer.value = '<%= referrer %>';
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/content_importer/edit_export_job" /></portlet:actionURL>';
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
	form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/content_importer/edit_export_job" /></portlet:actionURL>';
	submitForm(form);
}

	function validate() {

		if (trimString(document.getElementById("jobName").value) == '') {
			alert("<%=LanguageUtil.get(pageContext,"message.content.exporter.description.required")%>");
			return false;
		}
		if(document.getElementById("haveCronExpression").checked){
			if (trimString(document.getElementById("cronExpression").value) == '') {
				alert("<%=LanguageUtil.get(pageContext,"message.Scheduler.cronexpressionNeeded")%>");
				return false;
			}
		}else{
			if (document.getElementById("atInfo").checked) {
				if (!document.forms[0].at[0].checked &&
					!document.forms[0].at[1].checked) {
					alert("<%=LanguageUtil.get(pageContext,"message.content.exporter.at.required")%>");
					return false;
				}
			}
			
			if (document.getElementById("everyInfo").checked) {
				if (!document.forms[0].every[0].checked &&
					!document.forms[0].every[1].checked) {
					alert("<%=LanguageUtil.get(pageContext,"message.content.exporter.every.required")%>");
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
						alert("<%=LanguageUtil.get(pageContext,"message.content.exporter.week.day.required")%>");
						return false;
					}
				}
			}
			
			var atInfo = document.getElementsByName("atInfo")[0];
			if (atInfo.checked) {
				var at = document.getElementsByName("at");
				if (at[1].checked) {
					var betweenFromHour = dijit.byId('betweenFromHour').attr('value'); 
					var betweenToHour = dijit.byId('betweenToHour').attr('value'); 
					
	
					if (parseInt(betweenToHour) < parseInt(betweenFromHour) ) {
						alert("<%=LanguageUtil.get(pageContext,"message.content.exporter.from.hour.minor.than.to.hour")%>");
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
							alert("<%=LanguageUtil.get(pageContext,"message.content.exporter.hours.minutes.values")%>");
							return false;
						}
					}
				} else {
					document.getElementById("eachHours").value = "";
					document.getElementById("eachMinutes").value = "";
				}
			}
		}

		if(document.getElementById("haveFilePath").checked){
			if (trimString(document.getElementById("filePath").value) == '') {
				alert("<%=LanguageUtil.get(pageContext,"message.content.exporter.filePath.required")%>");
				return false;
			}
		} else if(document.getElementById("haveFileAsset").checked){
			if (trimString(document.getElementById("fileAsset").value) == '') {
				alert("<%=LanguageUtil.get(pageContext,"message.content.exporter.fileAsset.required")%>");
				return false;
			}
			if (trimString(document.getElementById("fileAssetHost").value) == '') {
				alert("<%=LanguageUtil.get(pageContext,"message.content.exporter.fileAssetHost.required")%>");
				return false;
			}
			if (trimString(document.getElementById("fileAssetPath").value) == '') {
				alert("<%=LanguageUtil.get(pageContext,"message.content.exporter.fileAssetPath.required")%>");
				return false;
			}
		}

		if (document.ContentExporterForm.fields != null) {
			var isIdentifierKeyField = false;

			for (var i = 0; i < document.ContentExporterForm.fields.length; ++i) {
				if ((document.ContentExporterForm.fields[i].value == '0') && document.ContentExporterForm.fields[i].checked) {
					isIdentifierKeyField = true;
					break;
				}
			}
			
			if (isIdentifierKeyField && (trimString(document.getElementById("identifierFieldName").value) == '')) {
				alert("<%=LanguageUtil.get(pageContext,"message.content.exporter.identifier.field.name.required")%>");
				return false;
			}
		}
		
		return true;
	}

	function structureChanged () {
		var inode = dijit.byId("structuresSelect").attr('value');
		//StructureAjax.getKeyStructureFields(inode, fillFields);
		ContentExporterAjax.getKeyStructureFields(inode, fillFields);
	}

	function fillFields (data) {
		currentStructureFields = data;
		dwr.util.removeAllRows("import_fields_table");
		dwr.util.addRows("import_fields_table", data["keyStructureFields"], [fieldCheckbox], { escapeHtml: false });
		dojo.parser.parse('import_fields_table');
		document.getElementById("structureFieldsDiv").style.display="";
	}

	function fieldCheckbox (field) {
		var fieldName = field["fieldName"];
		var fieldInode = field["inode"];
		var fieldVarName = field["fieldVelocityVarName"];
		var fieldIndexed = field["fieldIndexed"];
		var disableField = "";
				
		<% 
			String[] fields = contentExporterForm.getFields();
			for (int i = 0; i < fields.length; i++) 
			{

		%>
		if ((fieldVarName == "<%=fields[i]%>")) {
			if (dijit.byId(fieldInode + 'Field'))
				dijit.byId(fieldInode + 'Field').destroy();
			return "<div><input checked type=\"checkbox\" dojoType=\"dijit.form.CheckBox\" id=\"" + fieldInode + "Field\" name=\"fields\" checked=\"checked\" value=\"" + fieldVarName + "\" /> "  + fieldName + "</div>";
		}
		<%			
			
			} 
		%>
		if (dijit.byId(fieldInode + 'Field'))
			dijit.byId(fieldInode + 'Field').destroy()
		return "<div style='margin:2px 0px;'><input type=\"checkbox\" dojoType=\"dijit.form.CheckBox\" id=\"" + fieldInode + "Field\" name=\"fields\" value=\"" + fieldVarName + "\" /> <label>" +  fieldName + "</label></div>";
	}

	function showRegularDates(elem){
		if(elem.checked && elem.value =="true"){
			document.getElementById("regularDates").style.display="none";
			document.getElementById("cronDiv").style.display="block";
		}else if(elem.checked && elem.value =="false"){	
			document.getElementById("regularDates").style.display="block";
			document.getElementById("cronDiv").style.display="none";
		}else{
			document.getElementById("regularDates").style.display="none";
			document.getElementById("cronDiv").style.display="none";
		}
	}
	function toggleFileTarget(elem){
		if(elem.checked && elem.value =="false"){
			document.getElementById("filePathDiv").style.display="block";
			document.getElementById("fileAssetDiv").style.display="none";
		}else if(elem.checked && elem.value =="true"){	
			document.getElementById("filePathDiv").style.display="none";
			document.getElementById("fileAssetDiv").style.display="block";
		}else{
			document.getElementById("filePathDiv").style.display="none";
			document.getElementById("fileAssetDiv").style.display="none";
		}
	}
</script>

<html:form action="/ext/content_importer/edit_export_job" styleId="fm">

<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="">
<input name="<portlet:namespace />redirect" type="hidden" value="">
<input name="referrer" type="hidden" value="">
<input type="hidden" name="jobGroup" id="jobGroup" value="<%= ContentImporterQuartzUtils.quartzExportGroup %>">
<html:hidden property="editMode" />

<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false" >
	<!-- Basic Properties -->    
	<div id="fileBasicTab" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Basic-Properties") %>">
		<dl>
			<dt><img src="/html/images/icons/required.gif"/><%=LanguageUtil.get(pageContext,"content-exporter-task-name")%>: 
			    <br/><em><%=LanguageUtil.get(pageContext,"content-exporter-task-name-hint")%></em>
			</dt>
			<dd><input class="form-text" dojoType="dijit.form.TextBox" name="jobName" id="jobName" value="<%= UtilMethods.isSet(contentExporterForm.getJobName()) ? contentExporterForm.getJobName() : "" %>" style="width: 300px;" type="text" ></dd>
			<dt><%=LanguageUtil.get(pageContext,"content-exporter-task-description")%>: 
				<br/><em><%=LanguageUtil.get(pageContext,"content-exporter-task-description-hint")%></em>
			</dt>
			<dd><input class="form-text" dojoType="dijit.form.TextBox" name="jobDescription" id="jobDescription" value="<%= UtilMethods.isSet(contentExporterForm.getJobDescription()) ? contentExporterForm.getJobDescription() : "" %>"  style="width: 300px;" type="text" ></dd>
			<dt><%=LanguageUtil.get(pageContext,"Execute")%>: 
				<br/><em><%=LanguageUtil.get(pageContext,"Execute-hint")%></em>
			</dt>
			<dd><br/>			    			
			   <%
					SimpleDateFormat sdf = new SimpleDateFormat(ContentletUtil.DOTSCHEDULER_DATE);
				
					int[] monthIds = CalendarUtil.getMonthIds();
					String[] months = CalendarUtil.getMonths(locale);
				
					int currentYear = GregorianCalendar.getInstance().get(Calendar.YEAR);
					int previous = 100;
				%>
				<div id="startDateDiv">
					<span><img src="/html/images/icons/required.gif"/><%=LanguageUtil.get(pageContext,"from")%>:</span>
					<%
						Calendar startDateCalendar = null;
						Date startDate;
						try {
							startDate = sdf.parse(contentExporterForm.getStartDate());
						} catch (Exception e) {
							try {
								SimpleDateFormat sdf2 = new SimpleDateFormat(com.dotmarketing.util.WebKeys.DateFormats.DOTSCHEDULER_DATE2);
								startDate = sdf2.parse(contentExporterForm.getStartDate());
							} catch (Exception ex) {
								startDate = new Date();
							}
						}
					
						if (contentExporterForm.isHaveStartDate() ||
							!UtilMethods.isSet(contentExporterForm.getJobGroup()) ||
							(UtilMethods.isSet(contentExporterForm.getJobGroup()) &&
							 !contentExporterForm.isHaveStartDate())) {
							startDateCalendar = GregorianCalendar.getInstance();
							startDateCalendar.setTime(startDate);
						}
					%>
					<input type="hidden" id="haveStartDate" name="haveStartDate" value="true">
					<select dojoType="dijit.form.FilteringSelect" style="width: 120px;" name="startDateMonth" id="startDateMonth" onChange="updateDate('startDate');">
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
					<select dojoType="dijit.form.FilteringSelect" style="width: 80px;" style="width: 80px;" name="startDateDay" id="startDateDay" onChange="updateDate('startDate');">
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
					<select dojoType="dijit.form.FilteringSelect" style="width: 80px;" style="width: 80px;" name="startDateYear" id="startDateYear" onChange="updateDate('startDate');">
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
					<img align="absmiddle" border="0" hspace="0" id="<portlet:namespace />calendar_input_0_button" src="/html/images/icons/calendar-month.png" vspace="0" onClick="<portlet:namespace />calendarOnClick_0();">
		
					<select dojoType="dijit.form.FilteringSelect" style="width: 80px;" name="startDateHour" id="startDateHour" onChange="amPm('startDate');updateDate('startDate');">
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
					<select dojoType="dijit.form.FilteringSelect" style="width: 80px;" name="startDateMinute" id="startDateMinute" onChange="updateDate('startDate');">
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
					<select dojoType="dijit.form.FilteringSelect" style="width: 80px;" name="startDateSecond" id="startDateSecond" onChange="updateDate('startDate');">
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
					<span id="startDatePM"><font class="bg" size="2">AM</font></span>
					<input type="hidden" name="startDate" value="" id="startDate">						
				</div>		
				
			</dd>
			<dd>
				<div id="endDateDiv">
					<%
						Calendar endDateCalendar = null;
						Date endDate;
						try {
							endDate = sdf.parse(contentExporterForm.getEndDate());
						} catch (Exception e) {
							try {
								SimpleDateFormat sdf2 = new SimpleDateFormat(com.dotmarketing.util.WebKeys.DateFormats.DOTSCHEDULER_DATE2);
								endDate = sdf2.parse(contentExporterForm.getEndDate());
							} catch (Exception ex) {
								endDate = new Date();
							}
						}
					
						if (contentExporterForm.isHaveEndDate() ||
							!UtilMethods.isSet(contentExporterForm.getJobGroup()) ||
							(UtilMethods.isSet(contentExporterForm.getJobGroup()) &&
							 !contentExporterForm.isHaveEndDate())) {
							endDateCalendar = GregorianCalendar.getInstance();
							endDateCalendar.setTime(endDate);
						}
					%>
					<span><img src="/html/images/icons/required.gif"/><%=LanguageUtil.get(pageContext,"to")%>:</span>
					<input type="hidden" id="haveEndDate" name="haveEndDate" value="true">
					<select dojoType="dijit.form.FilteringSelect" style="width: 120px;" name="endDateMonth" id="endDateMonth" onChange="updateDate('endDate');">
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
					<select dojoType="dijit.form.FilteringSelect" style="width: 80px;" name="endDateDay" id="endDateDay" onChange="updateDate('endDate');">
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
					<select dojoType="dijit.form.FilteringSelect" style="width: 80px;" name="endDateYear" id="endDateYear" onChange="updateDate('endDate');">
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
					<img align="absmiddle" border="0" hspace="0" id="<portlet:namespace />calendar_input_1_button" src="/html/images/icons/calendar-month.png" vspace="0" onClick="<portlet:namespace />calendarOnClick_1();">
					<select dojoType="dijit.form.FilteringSelect" style="width: 80px;" name="endDateHour" id="endDateHour" onChange="amPm('endDate');updateDate('endDate');">
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
					<select dojoType="dijit.form.FilteringSelect" style="width: 80px;" name="endDateMinute" id="endDateMinute" onChange="updateDate('endDate');">
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
					<select dojoType="dijit.form.FilteringSelect" style="width: 80px;" name="endDateSecond" id="endDateSecond" onChange="updateDate('endDate');">
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
					<span id="endDatePM"><font class="bg" size="2">AM</font></span>			
				</div>
		
				<input type="hidden" name="endDate" value="" id="endDate">
				</dd>
			<dd>
				<img src="/html/images/icons/required.gif"/><%=LanguageUtil.get(pageContext,"content-exporter-frequency")%>
				<br/><br/><input type="radio" dojoType="dijit.form.RadioButton" <%=!contentExporterForm.isHaveCronExpression()?"checked":""%> id="haveCronExpression2" name="haveCronExpression" value="false" onclick="showRegularDates(this)"/><%=LanguageUtil.get(pageContext,"content-exporter-dont-use-cronexpression")%>
			    <br/><em><%=LanguageUtil.get(pageContext,"content-exporter-dont-use-cronexpression-hint")%></em>	
			</dd>
		<div id="regularDates">
			<dd>
				<%
					contentExporterForm.setAtInfo(true);
				%>
				<div style="display: none;margin-left:20px;">
					<input type="checkbox" dojoType="dijit.form.CheckBox" id="atInfo" name="atInfo" <%=contentExporterForm.isAtInfo()?"checked":""%> />
				</div>
			</dd>
			<dd>
				<div style="margin-left:20px;">
					<input type="radio" name="at" id="at1" dojoType="dijit.form.RadioButton" value="isTime" <%= UtilMethods.isSet(contentExporterForm.getAt()) && contentExporterForm.getAt().equals("isTime") ? "checked" : "" %> >
					<span><img src="/html/images/icons/required.gif"/><%=LanguageUtil.get(pageContext,"at")%></span>
					<select dojoType="dijit.form.FilteringSelect" style="width: 80px;" name="atTimeHour" id="atTimeHour" onChange="amPm('atTime');">
						<%
							for (int i = 0; i < 24; i++) 
							{
								int val = i > 12 ?  i - 12: i;
								if (val == 0)
									val = 12;
						%>
							<option <%= contentExporterForm.getAtTimeHour() == i ? "selected" : "" %> value="<%= i %>"><%= val %></option>
						<%
							}
						%>
					</select> :
					<select dojoType="dijit.form.FilteringSelect" style="width: 80px;" name="atTimeMinute">
						<%
							for (int i = 0; i < 60; ++i) {
								String val = (i < 10) ? "0" + i: String.valueOf(i);
						%>
							<option <%= contentExporterForm.getAtTimeMinute() == i ? "selected" : "" %> value="<%= i %>"><%= val %></option>
						<%
							}
						%>
					</select> :
					<select dojoType="dijit.form.FilteringSelect" style="width: 80px;" name="atTimeSecond">
						<%
							for (int i = 0; i < 60; ++i) {
								String val = (i < 10) ? "0" + i: String.valueOf(i);
						%>
							<option <%= contentExporterForm.getAtTimeSecond() == i ? "selected" : "" %> value="<%= i %>"><%= val %></option>
						<%
							}
						%>
					</select>
					<span id="atTimePM">AM</span>
				</div>
			</dd>
			<dd>
				<div style="margin-left:20px;">	
					<input type="radio" name="at" id="at" dojoType="dijit.form.RadioButton" value="isBetween" <%= UtilMethods.isSet(contentExporterForm.getAt()) && contentExporterForm.getAt().equals("isBetween") ? "checked" : "" %>/>
					<span><img src="/html/images/icons/required.gif"/><%=LanguageUtil.get(pageContext,"between")%></span>
					<select dojoType="dijit.form.FilteringSelect" style="width: 80px;" name="betweenFromHour" id="betweenFromHour" onChange="amPm('betweenFrom');">
						<%
							for (int i = 0; i < 24; i++) 
							{
								int val = i > 12 ?  i - 12: i;
								if (val == 0)
									val = 12;
						%>
							<option <%= contentExporterForm.getBetweenFromHour() == i ? "selected" : "" %> value="<%= i %>"><%= val %></option>
						<%
							}
						%>
					</select>
					<span id="betweenFromPM"><font class="bg" size="2">AM</font></span>
					<select dojoType="dijit.form.FilteringSelect" style="width: 80px;" name="betweenToHour" id="betweenToHour" onChange="amPm('betweenTo');">
						<%
							for (int i = 0; i < 24; i++) 
							{
								int val = i > 12 ?  i - 12: i;
								if (val == 0)
									val = 12;
						%>
							<option <%= contentExporterForm.getBetweenToHour() == i ? "selected" : "" %> value="<%= i %>"><%= val %></option>
						<%
							}
						%>
					</select>
					<span id="betweenToPM"><font class="bg" size="2">AM</font></span>
					<%
						contentExporterForm.setEachInfo(true);
					%>
					<div style="display: none;">
						<input type="checkbox" dojoType="dijit.form.CheckBox" id="eachInfo" name="eachInfo" <%=contentExporterForm.isEachInfo()?"checked":""%>/>
					</div>
					<span><%= LanguageUtil.get(pageContext, "each") %> <input type="text" dojoType="dijit.form.TextBox" style="width: 30px;"  class="form-text" name="eachHours" id="eachHours" maxlength="3"  <%= 0 < contentExporterForm.getEachHours() ? "value=\"" + contentExporterForm.getEachHours() + "\"" : "" %> > <%= LanguageUtil.get(pageContext, "hours-and") %> <input type="text" class="form-text" dojoType="dijit.form.TextBox" style="width: 30px;"  name="eachMinutes" id="eachMinutes" maxlength="3" <%= 0 < contentExporterForm.getEachMinutes() ? "value=\"" + contentExporterForm.getEachMinutes() + "\"" : "" %> > <%= LanguageUtil.get(pageContext, "minutes") %></span>
					<%
						if (!contentExporterForm.isEveryInfo())
						contentExporterForm.setEvery("");
					%>
				</div>
			</dd>
			<dd>
				<div style="margin-left:20px;">
					<input type="checkbox" dojoType="dijit.form.CheckBox" id="everyInfo" name="everyInfo" <%=contentExporterForm.isEveryInfo()?"checked":"" %>/><span><%=LanguageUtil.get(pageContext,"every").toLowerCase()%></span>
					<br/><em><%=LanguageUtil.get(pageContext,"content-exporter-every-hint")%></em>
				</div>
			</dd>
			<dd>
			    <table style="margin-left:40px;">
					<tr>
						<td>
							<input type="radio" name="every" id="every" dojoType="dijit.form.RadioButton" value="isDate" <%= UtilMethods.isSet(contentExporterForm.getEvery()) && contentExporterForm.getEvery().equals("isDate") ? "checked" : "" %> >
							<select dojoType="dijit.form.FilteringSelect" style="width: 120px;" name="everyDateMonth" id="everyDateMonth" onChange="updateDateOnly('everyDate');">
									<option value="*">-</option>
								<%
									for (int i = 0; i < months.length; i++) {
								%>
									<option <%= (contentExporterForm.getEveryDateMonth()-1) == monthIds[i] ? "selected" : "" %> value="<%= monthIds[i] %>"><%= months[i] %></option>
								<%
									}
								%>
							</select>
							<select dojoType="dijit.form.FilteringSelect" style="width: 80px;" name="everyDateDay" id="everyDateDay" onChange="updateDateOnly('everyDate');">
									<option value="*">-</option>
								<%
									for (int i = 1; i <= 31; i++) {
								%>
									<option <%= contentExporterForm.getEveryDateDay() == i ? "selected" : "" %> value="<%= i %>"><%= i %></option>
								<%
									}
								%>
							</select>
							<select dojoType="dijit.form.FilteringSelect" style="width: 80px;" name="everyDateYear" id="everyDateYear" onChange="updateDateOnly('everyDate');">
								<option value="*">-</option>
								<%
									for (int i = currentYear - previous; i <= currentYear + 10; i++) {
								%>
								<option <%= contentExporterForm.getEveryDateYear() == i ? "selected" : "" %> value="<%= i %>"><%= i %></option>
								<%
									}
								%>
							</select>
							<img align="absmiddle" border="0" hspace="0" id="<portlet:namespace />calendar_input_2_button" src="/html/images/icons/calendar-month.png" vspace="0" onClick="<portlet:namespace />calendarOnClick_2();">
							<input type="hidden" name="everyDate" value="" id="everyDate">
						</td>
					</tr>
				</table>
			</dd>
			<dd>
				<table style="margin-left:40px;" width="200">
					<tr>
						<td rowspan="2">
							<input type="radio" name="every" id="every1" dojoType="dijit.form.RadioButton" value="isDays" <%= UtilMethods.isSet(contentExporterForm.getEvery()) && contentExporterForm.getEvery().equals("isDays") ? "checked" : "" %> >
						</td>
						<td align="center">
							<%=LanguageUtil.get(pageContext,"content-exporter-mon")%>
						</td>
						<td align="center">
						<%=LanguageUtil.get(pageContext,"content-exporter-tue")%>
						</td>
						<td align="center">
						<%=LanguageUtil.get(pageContext,"content-exporter-wed")%>
						</td>
						<td align="center">
						<%=LanguageUtil.get(pageContext,"content-exporter-thu")%>
						</td>
						<td align="center">
						<%=LanguageUtil.get(pageContext,"content-exporter-fri")%>
						</td>
						<td align="center">
						<%=LanguageUtil.get(pageContext,"content-exporter-sat")%>
						</td>
						<td align="center">
						<%=LanguageUtil.get(pageContext,"content-exporter-sun")%>
						</td>
					</tr>
					<tr>
						<td align="center">
							<input type="checkbox" dojoType="dijit.form.CheckBox" name="everyDay" id="everyDay1" value="MON" <%= contentExporterForm.isMonday() ? "checked" : "" %> >
						</td>
						<td align="center">
							<input type="checkbox" dojoType="dijit.form.CheckBox" name="everyDay" id="everyDay2" value="TUE" <%= contentExporterForm.isTuesday() ? "checked" : "" %> >
						</td>
						<td align="center">
							<input type="checkbox" dojoType="dijit.form.CheckBox" name="everyDay" id="everyDay3" value="WED" <%= contentExporterForm.isWednesday() ? "checked" : "" %> >
						</td>
						<td align="center">
							<input type="checkbox" dojoType="dijit.form.CheckBox" name="everyDay" id="everyDay4" value="THU" <%= contentExporterForm.isThusday() ? "checked" : "" %> >
						</td>
						<td align="center">
							<input type="checkbox" dojoType="dijit.form.CheckBox" name="everyDay" id="everyDay5" value="FRI" <%= contentExporterForm.isFriday() ? "checked" : "" %> >
						</td>
						<td align="center">
							<input type="checkbox" dojoType="dijit.form.CheckBox" name="everyDay" id="everyDay6" value="SAT" <%= contentExporterForm.isSaturday() ? "checked" : "" %> >
						</td>
						<td align="center">
							<input type="checkbox" dojoType="dijit.form.CheckBox" name="everyDay" id="everyDay7" value="SUN" <%= contentExporterForm.isSunday() ? "checked" : "" %> >
						</td>
					</tr>
				</table>
			</dd>
		</div><!-- close regulardates -->
		<dd>
			<input type="radio" dojoType="dijit.form.RadioButton" <%=contentExporterForm.isHaveCronExpression()?"checked":""%> id="haveCronExpression" name="haveCronExpression" value="true" onclick="showRegularDates(this)"/><%=LanguageUtil.get(pageContext,"content-importer-use-cronexpression")%>
			<br/><em><%=LanguageUtil.get(pageContext,"content-importer-use-cronexpression-hint")%></em>
			<br/>
			<div id="cronDiv" style="margin-left:20px;">
				<br/><img src="/html/images/icons/required.gif"/><%=LanguageUtil.get(pageContext,"content-importer-cronexpression")%><input class="form-text" dojoType="dijit.form.TextBox" name="cronExpression" size="75" id="cronExpression" value="<%= UtilMethods.isSet(contentExporterForm.getCronExpression()) ? contentExporterForm.getCronExpression().replaceAll("\"","&quot;"):"" %>" style="width: 300px;" type="text" size="3">
			   	<br/><em><%=LanguageUtil.get(pageContext,"content-importer-cronexpression-hint")%></em>
			</div>
		</dd>
		<script>
		showRegularDates(document.getElementById('haveCronExpression'));
		</script>
			<dt><img src="/html/images/icons/required.gif"/><%=LanguageUtil.get(pageContext,"Structure-to-Export")%>:
				<br/><em><%=LanguageUtil.get(pageContext,"Structure-to-Export-hint")%></em>
			</dt>
			<dd>
				<br/><select dojoType="dijit.form.FilteringSelect" name="structure" id="structuresSelect" onchange="structureChanged()" value="<%= UtilMethods.isSet(contentExporterForm.getStructure()) ? contentExporterForm.getStructure() : "" %>" >
					<%
						for (Structure structure: structures) {
					%>
						<option value="<%= structure.getInode() %>"><%= structure.getName() %></option>
					<%
						}
					%>
				</select>
			</dd>
			<dt><img src="/html/images/icons/required.gif"/><%=LanguageUtil.get(pageContext,"Language-of-the-Contents-to-Export")%>: 
				<br/><em><%=LanguageUtil.get(pageContext,"Language-of-the-Contents-to-Export-hint")%></em>
			</dt>
			<dd>
				<br/><select dojoType="dijit.form.FilteringSelect" name="language" id="languageSelect" value="<%= UtilMethods.isSet(contentExporterForm.getLanguage()) ? contentExporterForm.getLanguage() : "-1" %>" >
					<%
									
						for (HashMap<String, Object> language: languagesMap) {
					%>
						<option value="<%= language.get("id") %>"><%= language.get("description") %></option>
					<%
						}
					%>
				</select>				
			</dd>
			<div id="structureFieldsDiv" style="display:none;">
			<dt><%=LanguageUtil.get(pageContext,"Fields-to-export")%>:
				<br/><em><%=LanguageUtil.get(pageContext,"Fields-to-export-hint")%></em>
			</dt>
			<dd>
				<table>
					<tbody id="import_fields_table"> </tbody>
				</table>
			</dd>
			</div>
			<dt><img src="/html/images/icons/required.gif"/><%=LanguageUtil.get(pageContext,"content-exporter-file-target")%>:</dt>
			<dd>
				<input type="radio" dojoType="dijit.form.RadioButton" <%=(!contentExporterForm.isHaveFileTarget()) ? "checked" : "" %> id="haveFilePath" name="haveFileTarget" value="false" onclick="toggleFileTarget(this)" /><%=LanguageUtil.get(pageContext,"content-exporter-use-filepath")%>
				<div id="filePathDiv" style="margin-left:20px;">
					<img src="/html/images/icons/required.gif"/><%=LanguageUtil.get(pageContext,"content-exporter-file-path-title")%><input class="form-text" dojoType="dijit.form.TextBox" name="filePath" size="75" id="filePath" value="<%= UtilMethods.isSet(contentExporterForm.getFilePath()) ? contentExporterForm.getFilePath() : "" %>" style="width: 300px;" type="text" >
					<br/>
					<em><%=LanguageUtil.get(pageContext,"content-exporter-file-path-hint")%></em>
				</div>
				<br/>
				<input type="radio" dojoType="dijit.form.RadioButton" <%=(contentExporterForm.isHaveFileTarget()) ? "checked" : "" %> id="haveFileAsset" name="haveFileTarget" value="true" onclick="toggleFileTarget(this)" /><%=LanguageUtil.get(pageContext,"content-exporter-use-fileasset")%>
				<div id="fileAssetDiv" style="margin-left:20px;">
					<img src="/html/images/icons/required.gif"/><%=LanguageUtil.get(pageContext,"content-exporter-file-asset-title")%><select dojoType="dijit.form.FilteringSelect" name="fileAsset" id="fileAsset" value="<%= UtilMethods.isSet(contentExporterForm.getFileAsset()) ? contentExporterForm.getFileAsset() : "" %>" >
						<%
							for( Structure structure : APILocator.getStructureAPI().find( APILocator.getUserAPI().getSystemUser(), false, false, "structuretype = "+ Structure.STRUCTURE_TYPE_FILEASSET, "name", Integer.MAX_VALUE, 0, "asc" ) ) {
						%>
							<option <%= structure.getVelocityVarName().equals(contentExporterForm.getFileAsset()) ? "selected" : "" %> value="<%= structure.getVelocityVarName() %>"><%= structure.getName() %></option>
						<%
							}
						%>
					</select>
					<br/>
					<em><%=LanguageUtil.get(pageContext,"content-exporter-file-asset-hint")%></em>
					<br/>
					<br/>
					<img src="/html/images/icons/required.gif"/><%=LanguageUtil.get(pageContext,"content-exporter-file-asset-host-title")%><select dojoType="dijit.form.FilteringSelect" name="fileAssetHost" id="fileAssetHost" value="<%= UtilMethods.isSet(contentExporterForm.getFileAssetHost()) ? contentExporterForm.getFileAssetHost() : "" %>" >
						<%
							for( Structure structure : APILocator.getStructureAPI().find( APILocator.getUserAPI().getSystemUser(), false, false, "structuretype = "+ Structure.STRUCTURE_TYPE_FILEASSET, "name", Integer.MAX_VALUE, 0, "asc" ) ) {
						%>
							<option <%= structure.getVelocityVarName().equals(contentExporterForm.getFileAssetHost()) ? "selected" : "" %> value="<%= structure.getVelocityVarName() %>"><%= structure.getName() %></option>
						<%
							}
						%>
					</select>
					<br/>
					<em><%=LanguageUtil.get(pageContext,"content-exporter-file-asset-host-hint")%></em>
					<br/>
					<br/>
					<img src="/html/images/icons/required.gif"/><%=LanguageUtil.get(pageContext,"content-exporter-file-asset-path-title")%><input class="form-text" dojoType="dijit.form.TextBox" name="fileAssetPath" size="75" id="fileAssetPath" value="<%= UtilMethods.isSet(contentExporterForm.getFileAssetPath()) ? contentExporterForm.getFileAssetPath() : "" %>" style="width: 300px;" type="text" >
					<br/>
					<em><%=LanguageUtil.get(pageContext,"content-exporter-file-asset-path-hint")%></em>
				</div>
			</dd>
			<script>
			toggleFileTarget(document.getElementById('haveFilePath'));
			</script>
			<dt><%=LanguageUtil.get(pageContext,"content-exporter-overwrite-file")%>:
				<br/><em><%=LanguageUtil.get(pageContext,"content-exporter-overwrite-file-hint")%></em>
			</dt>
			<dd><br/><input type="checkbox" class="form-text" dojoType="dijit.form.CheckBox" name="overWriteFile" id="overWriteFile" value="true" <%= contentExporterForm.isOverWriteFile() ? "checked" : "" %> ></dd>
			<dt><img src="/html/images/icons/required.gif"/><%=LanguageUtil.get(pageContext,"content-exporter-csv-separator-delimiter")%>:
				<br/><em><%=LanguageUtil.get(pageContext,"content-exporter-csv-separator-delimiter-hint")%></em>
			</dt>
			<dd><br/><input class="form-text" dojoType="dijit.form.TextBox" name="csvSeparatorDelimiter" size="75" id="csvSeparatorDelimiter" value="<%= UtilMethods.isSet(contentExporterForm.getCsvSeparatorDelimiter()) ? contentExporterForm.getCsvSeparatorDelimiter() : "," %>" style="width: 300px;" type="text" size="3"></dd>
			<dt><img src="/html/images/icons/required.gif"/><%=LanguageUtil.get(pageContext,"content-exporter-csv-text-delimiter")%>:
				<br/><em><%=LanguageUtil.get(pageContext,"content-exporter-csv-text-delimiter-hint")%></em>
			</dt>
			<dd><input class="form-text" dojoType="dijit.form.TextBox" name="csvTextDelimiter" size="75" id="csvTextDelimiter" value="<%= UtilMethods.isSet(contentExporterForm.getCsvTextDelimiter()) ? contentExporterForm.getCsvTextDelimiter().replaceAll("\"","&quot;"):"&quot;" %>" style="width: 300px;" type="text" size="3"></dd>
			<dt><%=LanguageUtil.get(pageContext,"content-exporter-report-email")%>:
				<br/><em><%=LanguageUtil.get(pageContext,"content-exporter-report-email-hint")%></em>
			</dt>
			<dd><br/><br/><input class="form-text" dojoType="dijit.form.TextBox" name="reportEmail" size="75" id="reportEmail" value="<%= UtilMethods.isSet(contentExporterForm.getReportEmail()) ? contentExporterForm.getReportEmail() : "" %>" style="width: 300px;" type="text" ></dd>
		</dl>

	</div>
</div>



</html:form>
<div class="buttonRow">
	<% if ((contentExporterForm != null) && (!contentExporterForm.isNewForm())) { %>
		<button dojoType="dijit.form.Button" onClick="deleteSchedule(document.getElementById('fm'))" iconClass="deleteIcon">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete")) %>
		</button>
	<% } %>
		<button dojoType="dijit.form.Button"  onClick="cancelEdit()" iconClass="cancelIcon">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Cancel")) %>
		</button>
		<button dojoType="dijit.form.Button" onClick="submitfm(document.getElementById('fm'))" iconClass="saveIcon">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Save")) %>
		</button>
	</div>
</liferay:box>

<script type="text/javascript">
dojo.addOnLoad(function() {
	var structure = dijit.byId("structuresSelect").attr('value');
	if ((structure != null) && (structure != '')) {
		structureChanged(structure);
	}
	
	<%
	if (!UtilMethods.isSet(contentExporterForm.getJobGroup())) {
	%>
		//document.getElementById('haveStartDate').checked = true;
	<%
		}
	%>
		//checkDate(document.forms[0].haveStartDate, 'startDate');
		amPm('startDate');
		updateDate('startDate');
	<%
	if (!UtilMethods.isSet(contentExporterForm.getJobGroup())) {
	%>
		//document.getElementById('haveEndDate').checked = true;
	<%
		}
	%>
	<%if(contentExporterForm.isHaveCronExpression()){%> 
		showRegularDates(document.getElementById("haveCronExpression"));
	<%} else {%>			
		showRegularDates(document.getElementById("haveCronExpression2"));
    <% } %>
		//checkDate(document.forms[0].haveEndDate, 'endDate');
		amPm('endDate');
		updateDate('endDate');

		amPm('atTime');
		amPm('betweenFrom');
		amPm('betweenTo');
		updateDateOnly('everyDate');

	<%if(!contentExporterForm.isHaveFileTarget()){%> 
		toggleFileTarget(document.getElementById("haveFilePath"));
	<%} else {%>			
		toggleFileTarget(document.getElementById("haveFileAsset"));
	<% } %>
});
</script>