/**
 * 
 */
package org.dotcms.plugins.contentImporter.portlet.struts;

import java.util.List;

import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;
import javax.servlet.jsp.PageContext;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.dotcms.plugins.contentImporter.util.ContentImporterQuartzUtils;

import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.quartz.CronScheduledTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.util.Constants;

/**
 * @author Armando Siem
 *
 */
public class ViewContentImporterJobsAction extends DotPortletAction {
	
	private static String[] quartzGroup = {ContentImporterQuartzUtils.quartzGroup};
	
	public ActionForward render(ActionMapping mapping, ActionForm form,	PortletConfig config, RenderRequest req, RenderResponse res) throws Exception {
		Logger.debug(this, "Running ViewContentImporterJobsAction!!!!");

		try {
			List<CronScheduledTask> list = ContentImporterQuartzUtils.getConfiguredSchedulers(quartzGroup);
			
			if (req.getWindowState().equals(WindowState.NORMAL)) {
				req.setAttribute(WebKeys.SCHEDULER_VIEW_PORTLET, list);
		        Logger.debug(this, "Going to: portlet.ext.plugins.content.importer.struts.view");
		        return mapping.findForward("portlet.ext.plugins.content.importer.struts.view");
			}
			else {
				req.setAttribute(WebKeys.SCHEDULER_LIST_VIEW, list);
		        Logger.debug(this, "Going to: portlet.ext.plugins.content.importer.struts.view.max");
		        return mapping.findForward("portlet.ext.plugins.content.importer.struts.view.max");
			}
		}
		catch (Exception e) {
			req.setAttribute(PageContext.EXCEPTION, e);
			return mapping.findForward(Constants.COMMON_ERROR);
		}
	}
}