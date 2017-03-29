package org.dotcms.plugins.contentImporter.portlet.form;

import com.dotmarketing.portlets.scheduler.struts.SchedulerForm;

public class ContentImporterForm extends SchedulerForm {

    private static final long serialVersionUID = 1L;

	/** identifier field */    
	
	private String structure = "";
	private long language = 0;
	private String[] fields = new String[0];
	private boolean haveFileSource;
	private String filePath;
	private String fileAsset;
	private String fileAssetQuery;
	private String reportEmail;
	private String csvSeparatorDelimiter;
	private String csvTextDelimiter;
	private boolean publishContent;
	private boolean newForm;
	private String cronExpression;
	private boolean haveCronExpression;
	private boolean saveWithoutVersions;
	private boolean deleteAllContent;
	
	/**
	 * Indicate if the save without version checkbox was set
	 * @return true if is set, false if not
	 */
	public boolean isSaveWithoutVersions() {
		return saveWithoutVersions;
	}

	/**
	 * Set the value of the save without version flag
	 * @param saveWithoutVersions
	 */
	public void setSaveWithoutVersions(boolean saveWithoutVersions) {
		this.saveWithoutVersions = saveWithoutVersions;
	}

	/**
	 * Indicate if the delete all structure content checkbox was set
	 * @return true if is set, false if not
	 */
	public boolean isDeleteAllContent() {
		return deleteAllContent;
	}

	/**
	 * Set the value of the dele all content flag
	 * @param deleteAllContent
	 */
	public void setDeleteAllContent(boolean deleteAllContent) {
		this.deleteAllContent = deleteAllContent;
	}

	/**
	 * Indicate if a cron expression was set
	 * @return true is a cron expression was set, false if not
	 */
	public boolean isHaveCronExpression() {
		return haveCronExpression;
	}

	/**
	 * Update the variable that shows if a cron expresion was set or not
	 * @param haveCronExpression
	 */
	public void setHaveCronExpression(boolean haveCronExpression) {
		this.haveCronExpression = haveCronExpression;
	}

	/**
	 * Get the cron expression field
	 */
	public String getCronExpression() {
		return cronExpression;
	}

	/**
	 * Set the cron expresion field
	 */
	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}

	/**
	 * Indicate if a file asset was set
	 * @return true if a file asset was set, false if not
	 */
	public boolean isHaveFileSource() {
		return haveFileSource;
	}

	/**
	 * Update the variable that shows if a file asset was set or not
	 * @param haveFileSource
	 */
	public void setHaveFileSource(boolean haveFileSource) {
		this.haveFileSource = haveFileSource;
	}

	public ContentImporterForm() {
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
	 * @return the fileAssetQuery
	 */
	public String getFileAssetQuery() {
		return fileAssetQuery;
	}

	/**
	 * @param fileAssetQuery the fileAssetQuery to set
	 */
	public void setFileAssetQuery(String fileAssetQuery) {
		this.fileAssetQuery = fileAssetQuery;
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

	/**
	 * @return the publishContent
	 */
	public boolean isPublishContent() {
		return publishContent;
	}

	/**
	 * @param publishContent the publishContent to set
	 */
	public void setPublishContent(boolean publishContent) {
		this.publishContent = publishContent;
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