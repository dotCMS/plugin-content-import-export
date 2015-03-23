package org.dotcms.plugins.contentImporter.portlet.struts;

import java.util.List;

import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotcms.repackage.javax.portlet.WindowState;
import javax.servlet.jsp.PageContext;

import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import org.dotcms.plugins.contentImporter.util.ContentImporterQuartzUtils;

import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.quartz.CronScheduledTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.util.Constants;

public class ViewContentExportJobsAction extends DotPortletAction {
	
	private static String[] quartzGroup = {ContentImporterQuartzUtils.quartzExportGroup};
	
	public ActionForward render(ActionMapping mapping, ActionForm form,	PortletConfig config, RenderRequest req, RenderResponse res) throws Exception {
		Logger.debug(this, "Running ViewContentExporterJobsAction!!!!");

		try {
			List<CronScheduledTask> list = ContentImporterQuartzUtils.getConfiguredSchedulers(quartzGroup);
			
			if (req.getWindowState().equals(WindowState.NORMAL)) {
				req.setAttribute(WebKeys.SCHEDULER_VIEW_PORTLET, list);
		        Logger.debug(this, "Going to: portlet.ext.plugins.content.importer.struts.view_export");
		        return mapping.findForward("portlet.ext.plugins.content.importer.struts.view_export");
			}
			else {
				req.setAttribute(WebKeys.SCHEDULER_LIST_VIEW, list);
		        Logger.debug(this, "Going to: portlet.ext.plugins.content.importer.struts.view_export.max");
		        return mapping.findForward("portlet.ext.plugins.content.importer.struts.view_export.max");
			}
		}
		catch (Exception e) {
			req.setAttribute(PageContext.EXCEPTION, e);
			return mapping.findForward(Constants.COMMON_ERROR);
		}
	}

}
