package org.dotcms.plugins.contentImporter.portlet.ajax;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.cache.ContentTypeCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * This class allow user to get the structures fields that the StructuresAjax class
 * doesn't allow
 * @author Oswaldo
 *
 */
public class ContentExporterAjax {

	/**
	 * Get a Map with all the structure fields except buttons, line divider and tab dividers
	 * @param structureInode
	 * @return
	 */
	public Map<String,Object> getKeyStructureFields (String structureInode) {
		Map<String,Object> result = new HashMap<String, Object>();
		boolean allowImport = true;

		Structure struct = CacheLocator.getContentTypeCache().getStructureByInode(structureInode);
		List<Field> fields = struct.getFields();
		ArrayList<Map> searchableFields = new ArrayList<Map> ();
		for (Field field : fields) {
			if (!field.getFieldType().equals(Field.FieldType.BUTTON.toString()) &&
					!field.getFieldType().equals(Field.FieldType.LINE_DIVIDER.toString()) &&
					!field.getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString())) {
				try {
					Map fieldMap = field.getMap();
					searchableFields.add(fieldMap);
				} catch (Exception e) {
					Logger.error(this, "Error getting the map of properties of a field: " + field.getInode());
				}
			}
		}

		try {
			WorkflowScheme scheme = APILocator.getWorkflowAPI().findSchemeForStruct(struct);
			if(scheme.isMandatory() && !UtilMethods.isSet(scheme.getEntryActionId())){
				allowImport = false;
			}
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage());
		}

		result.put("keyStructureFields",searchableFields);
		result.put("allowImport", allowImport);

		return result;
	}

	/**
	 * Get a Map with all the structure fields
	 * @param structureInode
	 * @return
	 */
	public Map<String,Object> getAllStructureFields (String structureInode) {
		Map<String,Object> result = new HashMap<String, Object>();
		boolean allowImport = true;

		Structure struct = CacheLocator.getContentTypeCache().getStructureByInode(structureInode);
		List<Field> fields = struct.getFields();
		ArrayList<Map> searchableFields = new ArrayList<Map> ();
		for (Field field : fields) {
			try {
				Map fieldMap = field.getMap();
				searchableFields.add(fieldMap);
			} catch (Exception e) {
				Logger.error(this, "Error getting the map of properties of a field: " + field.getInode());
			}			
		}

		try {
			WorkflowScheme scheme = APILocator.getWorkflowAPI().findSchemeForStruct(struct);
			if(scheme.isMandatory() && !UtilMethods.isSet(scheme.getEntryActionId())){
				allowImport = false;
			}
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage());
		}

		result.put("keyStructureFields",searchableFields);
		result.put("allowImport", allowImport);

		return result;
	}
}
