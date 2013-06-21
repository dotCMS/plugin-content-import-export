package org.dotcms.plugins.contentImporter.deployer;

import java.util.List;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.factories.PublicGroupFactory;
import com.dotmarketing.cms.factories.PublicRoleFactory;
import com.dotmarketing.cms.factories.PublicUserFactory;
import com.dotmarketing.factories.RoleFactory;
import com.dotmarketing.plugin.PluginDeployer;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.portal.ejb.LayoutManagerUtil;
import com.liferay.portal.ejb.PortletManagerUtil;
import com.liferay.portal.ejb.RoleLocalManagerUtil;
import com.liferay.portal.ejb.UserUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.User;

public class ContentImporterPluginDeployer implements PluginDeployer {

	public boolean deploy() 
	{		
		try {
			PluginAPI pluginAPI = APILocator.getPluginAPI();
			Company company = PublicCompanyFactory.getDefaultCompany();
			UserAPI userAPI = APILocator.getUserAPI();
			User user = userAPI.getSystemUser();
						
			//User user = PublicUserFactory.getDefaultUser();
			PrincipalThreadLocal.setName(user.getUserId());

			//Check if the Role exist
			String roleName = pluginAPI.loadProperty("org.dotcms.plugins.contentImporter", "portlet.role");
			
			RoleAPI roleAPI = APILocator.getRoleAPI();
			List<Role> roles = roleAPI.getRolesByName(roleName, 0, 100);
			
			boolean roleFound = false;
			if (UtilMethods.isSet(roles)) {
				for (Role role: roles) {
					if (role.getName().equals(roleName)) {
						roleFound = true;
						break;
					}
				}
			}					
			
			if (!roleFound) {				
				RoleLocalManagerUtil.addRole(company.getCompanyId(), roleName);
			}
			
			//Check if the Group exist
			String groupName = pluginAPI.loadProperty("org.dotcms.plugins.contentImporter", "portlet.groupName");
			
			if(!PublicGroupFactory.existGroup(groupName))
			{
				try
				{
				PublicGroupFactory.addGroup(groupName);
				//Add the Tab and the Portlet
				String columnOrder = "w,";
				String tabName = "Content Importer New";
				String portletId = "EXT_STRUTS_CONTENT_IMPORT";
				Group group = PublicGroupFactory.getGroupByName(groupName);

				Portlet portlet = PortletManagerUtil.getPortletById(company.getCompanyId(),portletId);
				String[] portletIds = {portlet.getPortletId()};
				
				Layout layout = LayoutManagerUtil.addGroupLayout(group.getGroupId(), tabName,portletIds);
				String narrow1 = "";
				String narrow2 = "";
				String wide = portletId + ",";

				LayoutManagerUtil.updateLayout(
						layout.getPrimaryKey(), tabName, columnOrder, narrow1, narrow2, wide,
						layout.getStateMax(), layout.getStateMin(), layout.getModeEdit(),
						layout.getModeHelp());
				}
				catch(Exception ex)
				{
					String errorMessage = ex.getMessage();
					Logger.debug(ContentImporterPluginDeployer.class,errorMessage);
				}
			}
		} catch (Exception e) {
			Logger.warn(this, e.toString());
			
			return false;
		}
		
		return true;
	}

	public boolean redeploy(String version) {
		return deploy();
	}
}