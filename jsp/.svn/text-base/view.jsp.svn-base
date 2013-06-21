<%@ include file="/html/plugins/org.dotcms.plugins.contentImporter/init.jsp" %>

<%@page import="com.dotmarketing.util.UtilMethods"%>

<%
	java.util.Hashtable params = new java.util.Hashtable ();
	params.put("struts_action", new String [] {"/ext/content_importer/view_jobs"} );

	String referrer = com.dotmarketing.util.PortletURLUtil.getRenderURL(request, javax.portlet.WindowState.MAXIMIZED.toString(), params);
%>

<%@page import="com.dotmarketing.util.Config"%>
<%@page import="org.dotcms.plugins.contentImporter.util.ContentImporterQuartzUtils"%>

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

<br />
<table border="0" cellpadding="0" cellspacing="0" width="100%" height="90" class="listingTable">
	
	<tr class="header"  height="18">
		<td align="center" width="50">Action</td>
		<td>Name</td>
		<td>Description</td>
	</tr>
	
	<%
		java.util.List list = (java.util.List) request.getAttribute(com.dotmarketing.util.WebKeys.SCHEDULER_VIEW_PORTLET);
		boolean itemShowed = false;
		int k = 0;
		int numsItemShowed = 0;
		
		String str_style=""; 
		for (; (numsItemShowed < Config.getIntProperty("MAX_ITEMS_MINIMIZED_VIEW")) && (k < list.size()); ++k) {
		
       		com.dotmarketing.portlets.scheduler.model.Scheduler scheduler = (com.dotmarketing.portlets.scheduler.model.Scheduler) list.get(k);
	 		itemShowed = true;
	 		
			
			if(numsItemShowed%2==0){
			  str_style="class=\"alternate_1\"";
			}
			else{
			  str_style="class=\"alternate_2\"";
			}
			
			numsItemShowed++;
	%>
		<tr <%=str_style %> >
			<td align="center" width="50" class="icons">
				<a href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
				<portlet:param name="struts_action" value="/ext/content_importer/edit_jobs" />
				<portlet:param name="name" value="<%= scheduler.getJobName() %>" />
				<portlet:param name="group" value="<%= ContentImporterQuartzUtils.quartzGroup %>" />
				<portlet:param name="<%= Constants.CMD %>" value="<%= Constants.EDIT %>" />
				</portlet:actionURL>">
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
		}
%>
	<tr><td colspan=3>&nbsp;</td></tr>
	<% if (!itemShowed) { %>
		<tr>
			<td colspan="3" align=center>
				There are no Content Import Jobs to show.
			</td>
		</tr>
	<% } %>
</table>


<table align="right" id="user_jobs_options">
	<tr>
		<td nowrap>
   				<a class="bg" href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/content_importer/view_jobs" /><portlet:param name="group" value="<%= ContentImporterQuartzUtils.quartzGroup %>" /></portlet:renderURL>">
				   all</a>
			| 
   				<a class="bg" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/content_importer/edit_jobs" /><portlet:param name="<%= Constants.CMD %>" value="<%= Constants.EDIT %>" /></portlet:actionURL>">
          new</a>&nbsp;&nbsp;
		</td>
	</tr>
</table>