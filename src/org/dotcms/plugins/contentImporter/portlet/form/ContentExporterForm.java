package org.dotcms.plugins.contentImporter.portlet.form;

import com.dotmarketing.portlets.scheduler.struts.SchedulerForm;

/**
 * Form to manage the export job schedule
 * @author Oswaldo
 *
 */
public class ContentExporterForm extends SchedulerForm {

    private static final long serialVersionUID = 1L;

	/** identifier field */    
	
	private String structure = "";
	private long language = -1;
	private String[] fields = new String[0];
	private boolean haveFileTarget;
	private String filePath;
	private String fileAsset;
	private String fileAssetHost;
	private String fileAssetPath;
	private boolean overWriteFile=false;
	private boolean newForm;
	private String reportEmail;
	private String csvSeparatorDelimiter;
	private String csvTextDelimiter;
	private String cronExpression;
	private boolean haveCronExpression;
	
	
	/**
	 * Indicates if the current file should be overwritten
	 * @return boolean
	 */
	public boolean isOverWriteFile() {
		return overWriteFile;
	}

	/**
	 * Set if the current file should be overwritten
	 * @param overWriteFile
	 */
	public void setOverWriteFile(boolean overWriteFile) {
		this.overWriteFile = overWriteFile;
	}

	/**
	 * Indicate if a cron expression was set
	 * @return boolean
	 */
	public boolean isHaveCronExpression() {
		return haveCronExpression;
	}

	/**
	 * set if a cron expression was set
	 * @param haveCronExpression
	 */
	public void setHaveCronExpression(boolean haveCronExpression) {
		this.haveCronExpression = haveCronExpression;
	}

	/**
	 * get the specified cron expressions
	 * @return String
	 */
	public String getCronExpression() {
		return cronExpression;
	}

	/**
	 * set the specified cron expressions
	 * @return String
	 */
	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}

	/**
	 * Indicate if a file asset was set
	 * @return true if a file asset was set, false if not
	 */
	public boolean isHaveFileTarget() {
		return haveFileTarget;
	}

	/**
	 * Update the variable that shows if a file asset was set or not
	 * @param haveFileSource
	 */
	public void setHaveFileTarget(boolean haveFileTarget) {
		this.haveFileTarget = haveFileTarget;
	}
		
	public ContentExporterForm() {
		super();
		this.newForm = true;
	}
	
	/**
	 * @return the newForm
	 */
	public boolean isNewForm() {
		return newForm;
	}

	/**
	 * @param newForm the newForm to set
	 */
	public void setNewForm(boolean newForm) {
		this.newForm = newForm;
	}

	/**
	 * @return the filePath
	 */
	public String getFilePath() {
		return filePath;
	}

	/**
	 * @param filePath the filePath to set
	 */
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	/**
	 * @return the fileAsset
	 */
	public String getFileAsset() {
		return fileAsset;
	}

	/**
	 * @param fileAsset the fileAsset to set
	 */
	public void setFileAsset(String fileAsset) {
		this.fileAsset = fileAsset;
	}

	/**
	 * @return the fileAssetHost
	 */
	public String getFileAssetHost() {
		return fileAssetHost;
	}

	/**
	 * @param fileAssetHost the fileAssetHost to set
	 */
	public void setFileAssetHost(String fileAssetHost) {
		this.fileAssetHost = fileAssetHost;
	}

	/**
	 * @return the fileAssetPath
	 */
	public String getFileAssetPath() {
		return fileAssetPath;
	}

	/**
	 * @param fileAsset the fileAsset to set
	 */
	public void setFileAssetPath(String fileAssetPath) {
		this.fileAssetPath = fileAssetPath;
	}

	/**
	 * @return the reportEmail
	 */
	public String getReportEmail() {
		return reportEmail;
	}

	/**
	 * @param reportEmail the reportEmail to set
	 */
	public void setReportEmail(String reportEmail) {
		this.reportEmail = reportEmail;
	}

	/**
	 * @return the csvSeparatorDelimiter
	 */
	public String getCsvSeparatorDelimiter() {
		return csvSeparatorDelimiter;
	}

	/**
	 * @param csvSeparatorDelimiter the csvSeparatorDelimiter to set
	 */
	public void setCsvSeparatorDelimiter(String csvSeparatorDelimiter) {
		this.csvSeparatorDelimiter = csvSeparatorDelimiter;
	}

	/**
	 * @return the csvTextDelimiter
	 */
	public String getCsvTextDelimiter() {
		return csvTextDelimiter;
	}

	/**
	 * @param csvTextDelimiter the csvTextDelimiter to set
	 */
	public void setCsvTextDelimiter(String csvTextDelimiter) {
		this.csvTextDelimiter = csvTextDelimiter;
	}

	public String[] getFields() {
        return fields;
    }

    public void setFields(String[] fields) {
        this.fields = fields;
    }

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

}
