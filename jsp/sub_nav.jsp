<%@ page import="javax.portlet.WindowState" %>
<%@ include file="/html/plugins/org.dotcms.plugins.contentImporter/init.jsp" %>

<script type="text/javascript">
	function showAllJobs() {
		var href = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/content_importer/view_jobs" /></portlet:renderURL>';
		document.location.href = href;
	}
	function addJobJobs() {
		var href = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/content_importer/edit_job" /><portlet:param name="<%= Constants.CMD %>" value="<%= Constants.EDIT %>" /></portlet:actionURL>';
		document.location.href = href;
	}

	function importExternalContent(){
		var href = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/content_importer/import_external_contentlets" /><portlet:param name="<%= Constants.CMD %>" value="<%= Constants.EDIT %>" /></portlet:actionURL>';
		document.location.href = href;
	}
	function exportContent(){
		var href = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/content_importer/view_export_jobs" /></portlet:renderURL>';
		document.location.href = href;
	}
	function addExportJob() {
		var href = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/content_importer/edit_export_job" /><portlet:param name="<%= Constants.CMD %>" value="<%= Constants.EDIT %>" /></portlet:actionURL>';
		document.location.href = href;
	}
</script>

<%
	RenderRequestImpl rreq = (RenderRequestImpl) pageContext.getAttribute("renderRequest");
	String portletId1 = rreq.getPortletName();
	List<CrumbTrailEntry> entries = new ArrayList<CrumbTrailEntry>();
	
	if (!UtilMethods.isSet(portletId1))
		portletId1 = layouts[0].getPortletIds().get(0);
	
	Portlet portlet1 = PortletManagerUtil.getPortletById(company.getCompanyId(), portletId1);
	String strutsAction = ParamUtil.get(request, "struts_action", null);
	
	if (!UtilMethods.isSet(strutsAction) || strutsAction.equals(portlet1.getInitParams().get("view-action"))) {
		entries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), "javascript: showAllJobs();"));
		entries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "Export-Content"), null));
	}else if (strutsAction.equals("/ext/content_importer/view_jobs") && portlet1.getPortletId().equals("EXT_STRUTS_CONTENT_IMPORT")) {
		entries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), "javascript: showAllJobs();"));
		
	} else if (strutsAction.equals("/ext/content_importer/edit_job") && portlet1.getPortletId().equals("EXT_STRUTS_CONTENT_IMPORT")) {
		entries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), "javascript: showAllJobs();"));
		entries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "Add-Edit-Content-Import-Job"), null));
	} else if (strutsAction.equals("/ext/content_importer/import_external_contentlets")) {
		entries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), "javascript: showAllJobs();"));
		entries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "Import-External-Content"), null));
	}else if (strutsAction.equals("/ext/content_importer/view_export_jobs") && portlet1.getPortletId().equals("EXT_STRUTS_CONTENT_IMPORT")) {
		entries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), "javascript: showAllJobs();"));
		entries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "Export-Content"), null));
	} else if (strutsAction.equals("/ext/content_importer/edit_export_job") && portlet1.getPortletId().equals("EXT_STRUTS_CONTENT_IMPORT")) {
		entries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), "javascript: showAllJobs();"));
		entries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "Export-Content"), "javascript: exportContent();"));
		entries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "Add-Edit-Content-Export-Job"), null));
	}
	
	request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, entries);
	request.setAttribute(com.dotmarketing.util.WebKeys.DONT_DISPLAY_SUBNAV_ALL_HOSTS, false);

%>
<%@ include file="/html/portlet/ext/common/sub_nav_inc.jsp" %>