<%@ include file="/html/plugins/org.dotcms.plugins.contentImporter/init.jsp" %>

<script type="text/javascript">
	function showAllJobs() {
		var href = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/content_importer/view_jobs" /></portlet:renderURL>';
		document.location.href = href;
	}
</script>
<table border="0" cellpadding="4" cellspacing="0" width="100%">
  <tr class="beta">
    <td nowrap="nowrap">
      <font class="beta" size="2"><a class="beta" href="javascript: showAllJobs()">View All</a></font>
    </td>
    <td width="100%" nowrap="nowrap">
      <div id="addNewUserJob">|	
      <font class="beta" size="2">
	    <a class="beta" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/content_importer/edit_jobs" /></portlet:actionURL>">
		  Schedule a Content Import Task 
		</a>
      </font>
	  </div>
    </td>
  </tr>
</table>