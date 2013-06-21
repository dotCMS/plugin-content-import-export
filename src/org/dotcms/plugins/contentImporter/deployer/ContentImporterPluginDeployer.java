package org.dotcms.plugins.contentImporter.deployer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.jsp.html.portlet.ext.containers.add_005fvariables_jsp;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.plugin.PluginDeployer;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.portal.ejb.PortletManagerUtil;
import com.liferay.portal.ejb.UserUtil;
import com.liferay.portal.model.Company;
import com.dotmarketing.business.Layout;
import com.liferay.portal.model.Portlet;
import com.dotmarketing.business.Role;
import com.liferay.portal.model.User;

public class ContentImporterPluginDeployer implements PluginDeployer {

	public boolean deploy() 
	{		
		try {
			PluginAPI pluginAPI = APILocator.getPluginAPI();
			Company company = PublicCompanyFactory.getDefaultCompany();
			UserAPI userAPI = APILocator.getUserAPI();
			User user = userAPI.getSystemUser();
			LayoutAPI layoutAPI = APILocator.getLayoutAPI();
			Role role = null;
			//User user = PublicUserFactory.getDefaultUser();
			PrincipalThreadLocal.setName(user.getUserId());

			//Check if the Role exist
			String roleName = pluginAPI.loadProperty("org.dotcms.plugins.contentImporter", "portlet.role");
			String roleKey = pluginAPI.loadProperty("org.dotcms.plugins.contentImporter", "portlet.role.key");

			RoleAPI roleAPI = APILocator.getRoleAPI();
			List<Role> roles = roleAPI.findRolesByNameFilter(roleName, 0, 100);

			boolean roleFound = false;
			if (UtilMethods.isSet(roles)) {
				for (Role rol: roles) {
					if (rol.getName().equals(roleName)) {
						roleFound = true;
						role=rol;
						break;
					}
				}
			}					

			if (!roleFound) {	
				role = new Role();
				role.setName(roleName);
				role.setRoleKey(roleKey);
				role.setSystem(false);
				roleAPI.save(role);
			}else{
				if(!UtilMethods.isSet(role.getRoleKey())){
					role.setRoleKey(roleKey);
					roleAPI.save(role);
				}
			}

			//Add the Tab and the Portlet
			String tabName = "Content Import Jobs";
			String tabDescription = "Quartz Content Importer";
			String portletId = "EXT_STRUTS_CONTENT_IMPORT";
			boolean existLayout = false;
			Layout layout = null;
			List<Layout> allLayouts = layoutAPI.findAllLayouts();
			for(Layout oldLayout : allLayouts) {	
				if(oldLayout.getName().equals(tabName)){
					existLayout = true;
					layout = oldLayout;
					break;
				}
			}

			//if the layout doesn't exists
			Portlet portlet = PortletManagerUtil.getPortletById(company.getCompanyId(),portletId);
			List<String> portletIds = new ArrayList<String>();
			portletIds.add(portlet.getPortletId());
			if(!existLayout){
				layout = new Layout();
				layout.setId(UUIDGenerator.generateUuid());
			}

			layout.setName(tabName);
			layout.setDescription(tabDescription);
			layout.setTabOrder(1);
			layoutAPI.saveLayout(layout);

			layoutAPI.setPortletIdsToLayout(layout, portletIds);

			List<Layout> layouts = layoutAPI.loadLayoutsForRole(role);
			//Looking for removed layouts
			for(Layout l : layouts) {
				boolean found = false;
				if(layout.equals(l.getId())) {
					found = true;
					break;
				}

				if(!found) {
					roleAPI.removeLayoutFromRole(l, role);
				}
			}

			//Looking for added layouts
			boolean found = false;
			for(Layout l : layouts) {
				if(layout.equals(l.getId())) {
					found = true;
					break;
				}
			}
			Layout layoutChanged = layoutAPI.loadLayout(layout.getId());
			if(!found) {
				roleAPI.addLayoutToRole(layoutChanged, role);
			}


		} catch (Exception e) {
			Logger.error(this, e.toString());
			return false;
		}

		return true;
	}

	public boolean redeploy(String version) {
		return deploy();
	}
}