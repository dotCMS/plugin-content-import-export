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
	params.put("struts_action", new String [] {"/ext/content_importer/view_jobs"} );
	params.put("pageNumber",new String[] { pageNumber + "" });
	
	String referrer = com.dotmarketing.util.PortletURLUtil.getRenderURL(request, javax.portlet.WindowState.MAXIMIZED.toString(), params);
%>

<script type="text/javascript">
function deleteJob(jobName,jobGroup)
{
	if(confirm('Do you want to delete \'' + jobName+ '\' ?'))
	{
		var action = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
		action +=    "<portlet:param name='struts_action' value='/ext/content_importer/edit_jobs' />";		
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

	<table border="0" cellpadding="0" cellspacing="0" width="100%" >
		<tr>
			<td>
				<table border="0" cellpadding="0" cellspacing="0" width="100%" class="listingTable">
					
					<tr class="header" height="18px">
						<td align="right" width="50">Action</td>
						<td>Name</td>
						<td>Description</td>
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
							
							com.dotmarketing.portlets.scheduler.model.Scheduler scheduler = (com.dotmarketing.portlets.scheduler.model.Scheduler) lists.get(k);
							itemShowed = true;
					%>
						<tr <%= str_style %>>
							<td align="center" width="50" class="icons">
							
								<a href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
								<portlet:param name="struts_action" value="/ext/content_importer/edit_jobs" />
								<portlet:param name="name" value="<%= scheduler.getJobName() %>" />
								<portlet:param name="group" value="<%= ContentImporterQuartzUtils.quartzGroup %>" />
								<portlet:param name="<%= Constants.CMD %>" value="<%= Constants.EDIT %>" /></portlet:actionURL>">
								<IMG border="0" src="/portal/images/icon_edit.gif">
								</a>
								
								<a href="javascript:deleteJob('<%= scheduler.getJobName() %>','<%= ContentImporterQuartzUtils.quartzGroup %>');">
								<IMG border="0" src="/portal/images/icon_delete.gif">
								</a>

							</td>
							
							<td>
								<a href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
								<portlet:param name="struts_action" value="/ext/content_importer/edit_jobs" />
								<portlet:param name="name" value="<%= scheduler.getJobName() %>" />
								<portlet:param name="group" value="<%= ContentImporterQuartzUtils.quartzGroup %>" />
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
						<tr>
							<td colspan="2" align=left>
							<% if (minIndex != 0) { %>
							<img src="<%= SKIN_COMMON_IMG %>/02_left.gif">
			   				<a href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
							<portlet:param name="struts_action" value="/ext/content_importer/view_jobs" />
							<portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber - 1) %>" />
							</portlet:renderURL>">Previous</a>
							<% } %>
							</td>
							
							<td colspan="1" align=right>
							<% if (maxIndex < lists.size()) { %>
			   				<a class="bg" href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
							<portlet:param name="struts_action" value="/ext/content_importer/view_jobs" />
							<portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber + 1) %>" />
							</portlet:renderURL>">Next</a>
							<img src="<%= SKIN_COMMON_IMG %>/02_right.gif">
							<% } %>
							</td>
						</tr>
						
					<% if (!itemShowed) { %>
					<tr><td colspan=3>&nbsp;</td></tr>
					<tr>
						<td colspan="3" align=center>
							There are no Content Import Jobs to show
						</td>
					</tr>
					<% } %>
				</table>
			</td>
		</tr>
	</table>
</liferay:box>