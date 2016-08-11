package org.dotcms.plugins.contentImporter.portlet.form;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.repackage.org.apache.struts.action.ActionErrors;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotcms.repackage.org.apache.struts.action.ActionMessage;
import com.dotcms.repackage.org.apache.struts.action.ActionMessages;
import com.dotcms.repackage.org.apache.struts.validator.ValidatorForm;

import com.dotmarketing.util.UtilMethods;

public class ImportExternalContentletsForm extends ValidatorForm {

    private static final long serialVersionUID = 1L;

	/** identifier field */    
	
	private String structure = "";
	
	private String fileName = "";
	
	private long language = 0;
	
	private String titleField = "";
	
	private String contentField = "";
	
	private String pathField = "";
	
    public String getStructure() {
        return structure;
    }

    public void setStructure(String structure) {
        this.structure = structure;
    }

    public long getLanguage() {
        return language;
    }

    public void setLanguage(long language) {
        this.language = language;
    }

    @Override
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest req) {
        
    	ActionErrors ae = super.validate(mapping, req);
        	
       	if (!UtilMethods.isSet(this.getContentField())) {
       		ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.import.external.content.importer.content.required"));
    	}
       	if (!UtilMethods.isSet(this.getTitleField())) {
       		ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.import.external.content.importer.title.required"));
    	}
        return ae;
    }

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
	}

	public void setTitleField(String titleField) {
		this.titleField = titleField;
	}

	public String getTitleField() {
		return titleField;
	}

	public void setContentField(String contentField) {
		this.contentField = contentField;
	}

	public String getContentField() {
		return contentField;
	}

	public void setPathField(String pathField) {
		this.pathField = pathField;
	}

	public String getPathField() {
		return pathField;
	}
    
    
    
}

