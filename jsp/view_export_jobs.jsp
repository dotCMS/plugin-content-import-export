<%@ page import="javax.portlet.WindowState" %>
<%@ include file="/html/plugins/org.dotcms.plugins.contentImporter/init.jsp" %>
<%@ include file="/html/common/messages_inc.jsp" %>
<%@ page import="org.dotcms.plugins.contentImporter.util.ContentImporterQuartzUtils" %>
<%
	int pageNumber = 1;
	if (request.getParameter("pageNumber")!=null) {
		pageNumber = Integer.parseInt(request.getParameter("pageNumber")); 
	}
	
	int perPage = com.dotmarketing.util.Config.getIntProperty("PER_PAGE");
	int minIndex = (pageNumber - 1) * perPage;
	int maxIndex = perPage * pageNumber;
	
	java.util.Hashtable params = new java.util.Hashtable ();
	params.put("struts_action", new String [] {"/ext/content_importer/view_export_jobs"} );
	params.put("pageNumber",new String[] { pageNumber + "" });
	
	String referrer = com.dotmarketing.util.PortletURLUtil.getRenderURL(request, javax.portlet.WindowState.MAXIMIZED.toString(), params);
%>


<%@page import="com.dotmarketing.quartz.CronScheduledTask"%><script type="text/javascript">
function deleteExportJob(jobName,jobGroup)
{
	if(confirm('<%=LanguageUtil.get(pageContext,"content-importer-delete-job")%> \'' + jobName+ '\' ?'))
	{
		var action = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
		action +=    "<portlet:param name='struts_action' value='/ext/content_importer/edit_export_job' />";		
		action +=    "<portlet:param name='cmd' value='<%=Constants.DELETE%>' />";
		action +=    "<portlet:param name='referrer' value='<%= referrer %>' />";
		action +=    "</portlet:actionURL>";
		action +=    "&name=" + jobName;
		action +=    "&group=" + jobGroup;
		document.location.href = action;
	}
}
</script>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value="Content Import Jobs" />
	<style>
		.dijitSelect .dijitButtonText{width:150px;text-align:left;}
	</style>
<div class="yui-g portlet-toolbar">
	<div class="yui-u first" style="white-space: nowrap">
		<button dojoType="dijit.form.Button" onClick="exportContent()" iconClass="searchIcon">
		   <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "view-all")) %>
		</button>		
	</div>
	<div class="buttonBoxRight">
		<button dojoType="dijit.form.ComboButton" id="contentAddButton" optionsTitle='createOptions' onClick="addExportJob()" iconClass="plusIcon" title="<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-Edit-Content-Export-Job" )) %>">
			<span><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-Edit-Content-Export-Job" )) %></span>
			<div dojoType="dijit.Menu" id="createMenu" style="display: none;">
				<div dojoType="dijit.MenuItem" iconClass="plusIcon" onClick="addExportJob()">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-Edit-Content-Export-Job" )) %>
				</div>
				<div dojoType="dijit.MenuItem" iconClass="plusIcon" onClick="addJobJobs()">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-Edit-Content-Import-Job" )) %>
				</div>
				<div dojoType="dijit.MenuItem" iconClass="previewIcon" onClick="exportContent()">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "View-Export-Content" )) %>
				</div>
				<div dojoType="dijit.MenuItem" iconClass="previewIcon" onClick="showAllJobs()">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "View-Import-Content" )) %>
				</div>
				<div dojoType="dijit.MenuItem" iconClass="uploadIcon" onClick="importExternalContent()">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Import-External-Content" )) %>
				</div>
			</div>
		</button>
	</div>
</div>
<div id="contentImporterPanel">
	<form action="" method="post" name="order">
				<table border="0" cellpadding="0" cellspacing="0" width="100%" class="listingTable">
					
					<tr class="header" height="18px">
						<th align="right" width="50"><%=LanguageUtil.get(pageContext, "action")%></th>
						<th><%=LanguageUtil.get(pageContext, "name")%></td>
						<th><%=LanguageUtil.get(pageContext, "Description")%></th>
					</tr>

					<% java.util.List lists = (java.util.List) request.getAttribute(com.dotmarketing.util.WebKeys.SCHEDULER_LIST_VIEW);
					
						boolean itemShowed = false;
						String str_style = "";
						for (int k = minIndex; (k < maxIndex) && (k < lists.size()); k++) {
							
					     	if(k%2==0){
							  str_style="class=\"alternate_1\"";
							}
							else{
							  str_style="class=\"alternate_2\"";
							}
							
							CronScheduledTask scheduler = (CronScheduledTask) lists.get(k);
							itemShowed = true;
					%>
						<tr <%= str_style %>>
							<td align="center" width="50" class="icons">
							
								<a href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
								<portlet:param name="struts_action" value="/ext/content_importer/edit_export_job" />
								<portlet:param name="name" value="<%= scheduler.getJobName() %>" />
								<portlet:param name="group" value="<%= ContentImporterQuartzUtils.quartzExportGroup %>" />
								<portlet:param name="<%= Constants.CMD %>" value="<%= Constants.EDIT %>" /></portlet:actionURL>">
								<IMG border="0" src="/html/images/icons/pencil.png">
								</a>
								
								<a href="javascript:deleteExportJob('<%= scheduler.getJobName() %>','<%= ContentImporterQuartzUtils.quartzExportGroup %>');">
								<IMG border="0" src="/html/images/icons/cross.png">
								</a>

							</td>
							
							<td>
								<a href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
								<portlet:param name="struts_action" value="/ext/content_importer/edit_export_job" />
								<portlet:param name="name" value="<%= scheduler.getJobName() %>" />
								<portlet:param name="group" value="<%= ContentImporterQuartzUtils.quartzExportGroup %>" />
								<portlet:param name="<%= Constants.CMD %>" value="<%= Constants.EDIT %>" />
								</portlet:actionURL>"><%= scheduler.getJobName() %>
								</a>
							</td>
							
							<td>
									<%= scheduler.getJobDescription() %>
							</td>
						</tr>
						<%
					}%>
					<% if (!itemShowed) { %>
					<tr>
						<td colspan="4">
							<div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "There-are-no-Content-Export-Jobs-to-display") %></div>
						</td>
					</tr>					
					<% } %>
				</table>	
				<!-- Start Pagination -->
				<div class="yui-gb buttonRow">
					<div class="yui-u first" style="text-align:left;">		     
						<% if (minIndex != 0) { %>
							<button dojoType="dijit.form.Button" onClick="window.location.href = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/structure/view_export_jobs" /><portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber - 1) %>" /></portlet:renderURL>';" iconClass="previousIcon" type="button">
								<%= LanguageUtil.get(pageContext, "Previous") %>
							</button>
						<% } %>&nbsp;
					</div>
					<div class="yui-u">
						<%= LanguageUtil.get(pageContext, "Viewing") %>  <%= minIndex+1 %> - <% if (maxIndex > lists.size()) { %> <%= lists.size() %> <%}else{%>  <%= maxIndex %> <% } %> <%= LanguageUtil.get(pageContext, "of1") %> <% if (maxIndex > lists.size()) { %> <%= lists.size() %> <%}else{%> <%= lists.size() %> <%}%>
					</div>
					<div class="yui-u" style="text-align:right;">
						<% if (maxIndex < lists.size()) { %>
							<button dojoType="dijit.form.Button" onClick="window.location.href = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/structure/view_export_jobs" /><portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber + 1) %>" /></portlet:renderURL>';" iconClass="nextIcon" type="button">
								<%= LanguageUtil.get(pageContext, "Next") %>
							</button>
						<% } %>&nbsp;
					</div>
				</div>
			<!-- END Pagination -->		
	</form>
</div>	
</liferay:box>