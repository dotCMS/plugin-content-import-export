package org.dotcms.plugins.contentImporter.portlet.struts;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.dotcms.plugins.contentImporter.portlet.form.ImportExternalContentletsForm;
import org.dotcms.plugins.contentImporter.util.ContentletUtil;

import com.csvreader.CsvReader;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.action.ImportAuditUtil;
import com.dotmarketing.portlets.contentlet.action.ImportContentletsAction;
import com.dotmarketing.portlets.contentlet.action.ImportAuditUtil.ImportAuditResults;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.ImportUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.Validator;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.ActionResponseImpl;
import com.liferay.util.FileUtil;
import com.liferay.util.servlet.SessionMessages;
import com.liferay.util.servlet.UploadPortletRequest;

/**
 * This action class import external content from csv/text files. 
 * The csv file should contains as first line the required headers that match the structure fields
 *  
 * @author oswaldo
 * 
 */
public class ImportExternalContentsAction extends DotPortletAction{

	private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();
	private final static ContentletAPI conAPI = APILocator.getContentletAPI();
	private final static CategoryAPI catAPI = APILocator.getCategoryAPI();
	private final static LanguageAPI langAPI = APILocator.getLanguageAPI();
	private final static HostAPI hostAPI = APILocator.getHostAPI();
	private final static FolderAPI folderAPI = APILocator.getFolderAPI();
	private final static TagAPI tagAPI = APILocator.getTagAPI();

	private final static String languageCodeHeader = "languageCode";
	private final static String countryCodeHeader = "countryCode";
	private final static String FILE_TITLE="title";
	private final static String FILE_PATH="path";
	private final static String FILE_CONTENT="content";

	private static final SimpleDateFormat DATE_FIELD_FORMAT = new SimpleDateFormat("yyyyMMdd");


	/**
	 * @param permissionAPI the permissionAPI to set
	 */
	public void setPermissionAPI(PermissionAPI permissionAPIRef) {
		permissionAPI = permissionAPIRef;
	}

	public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req, ActionResponse res) throws Exception {
		Logger.debug(this, "Import Contentlets Action");



		HttpSession session = ((ActionRequestImpl)req).getHttpServletRequest().getSession();
		String importSession =Long.toString(System.currentTimeMillis());

		if(UtilMethods.isSet(session.getAttribute("importSession"))){
			importSession = (String) session.getAttribute("importSession");
		}

		String cmd = req.getParameter(Constants.CMD);
		String referer = req.getParameter("referer");

		Logger.debug(this, "ImportContentletsAction cmd=" + cmd);

		User user = _getUser(req);

		/*
		 * We are submiting the file to process
		 */
		if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.PREVIEW)) {

			try {
				Logger.debug(this, "Calling Preview Upload Method");

				//Validation
				if (Validator.validate(req,form,mapping)) {
					UploadPortletRequest uploadReq = PortalUtil.getUploadPortletRequest(req);
					byte[] bytes = FileUtil.getBytes(uploadReq.getFile("file"));



					if (bytes == null || bytes.length == 0) {
						SessionMessages.add(req, "error", "message.contentlet.file.required");
						setForward(req, "portlet.ext.plugins.content.importer.struts.import_external_contentlets");
					} else {
						try {
							Reader reader = null;
							CsvReader csvreader = null;
							String[] csvHeaders = null;
							int languageCodeHeaderColumn = -1;
							int countryCodeHeaderColumn = -1;

							ImportExternalContentletsForm importForm = (ImportExternalContentletsForm) form;
							if (importForm.getLanguage() == -1)
								reader = new InputStreamReader(new ByteArrayInputStream(bytes), Charset.forName("UTF-8"));
							else
								reader = new InputStreamReader(new ByteArrayInputStream(bytes));

							csvreader = new CsvReader(reader);
							csvreader.setSafetySwitch(false);

							switch ((int) importForm.getLanguage()) {
							case -1:
								if (csvreader.readHeaders()) {
									csvHeaders = csvreader.getHeaders();
									for (int column = 0; column < csvHeaders.length; ++column) {
										if (csvHeaders[column].equals(languageCodeHeader))
											languageCodeHeaderColumn = column;
										if (csvHeaders[column].equals(countryCodeHeader))
											countryCodeHeaderColumn = column;

										if ((-1 < languageCodeHeaderColumn) && (-1 < countryCodeHeaderColumn))
											break;
									}

									if ((-1 == languageCodeHeaderColumn) || (-1 == countryCodeHeaderColumn)) {
										SessionMessages.add(req, "error", "message.import.contentlet.csv_headers.required");
										setForward(req, "portlet.ext.contentlet.import_contentlets");
									} else {
										_generatePreview(req, res, config, form, user, bytes, csvHeaders, csvreader, languageCodeHeaderColumn, countryCodeHeaderColumn, reader);
										setForward(req, "portlet.ext.contentlet.import_contentlets_preview");
									}
								} else {
									SessionMessages.add(req, "error", "message.import.contentlet.language.required");
									setForward(req, "portlet.ext.plugins.content.importer.struts.import_external_contentlets");
								}
								break;
							case 0:
								SessionMessages.add(req, "error", "message.import.contentlet.language.required");
								setForward(req, "portlet.ext.plugins.content.importer.struts.import_external_contentlets");
								break;
							default:
								_generatePreview(req, res, config, form, user, bytes, csvHeaders, csvreader, languageCodeHeaderColumn, countryCodeHeaderColumn, reader);
								setForward(req, "portlet.ext.plugins.content.importer.struts.import_external_contentlets_preview");
								break;
							}

							csvreader.close();
						} catch (Exception e) {
							_handleException(e, req);
							return;
						}
					}
				}else{
					setForward(req, "portlet.ext.plugins.content.importer.struts.import_external_contentlets");
				}
			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
		} else if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.PUBLISH)) {

			try {
				Logger.debug(this, "Calling Process File Method");

				Reader reader = null;
				CsvReader csvreader = null;
				String[] csvHeaders = null;
				int languageCodeHeaderColumn = -1;
				int countryCodeHeaderColumn = -1;

				ActionRequestImpl reqImpl = (ActionRequestImpl) req;
				HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
				byte[] bytes = (byte[]) httpReq.getSession().getAttribute("file_to_import");
				File file= (File)httpReq.getSession().getAttribute("csvFile");
				ImportExternalContentletsForm importContentletsForm = (ImportExternalContentletsForm) form;
				if (importContentletsForm.getLanguage() == -1)
					reader = new InputStreamReader(new ByteArrayInputStream(bytes), Charset.forName("UTF-8"));
				else
					reader = new InputStreamReader(new ByteArrayInputStream(bytes));
				csvreader = new CsvReader(reader);
				csvreader.setSafetySwitch(false);

				if (importContentletsForm.getLanguage() == -1) {
					if (csvreader.readHeaders()) {
						csvHeaders = csvreader.getHeaders();
						for (int column = 0; column < csvHeaders.length; ++column) {
							if (csvHeaders[column].equals(languageCodeHeader))
								languageCodeHeaderColumn = column;
							if (csvHeaders[column].equals(countryCodeHeader))
								countryCodeHeaderColumn = column;

							if ((-1 < languageCodeHeaderColumn) && (-1 < countryCodeHeaderColumn))
								break;
						}
					}
				}

				long importId = ImportAuditUtil.createAuditRecord(user.getUserId(), (String)httpReq.getSession().getAttribute("fileName"));

				if(importSession.equals((String) session.getAttribute("importSession") )){
					session.removeAttribute("importSession");
					_processFile(req, res, config, form, user, csvHeaders, csvreader, languageCodeHeaderColumn, countryCodeHeaderColumn, reader);

				}

				HashMap<String, List<String>> importresults = (HashMap<String, List<String>>)req.getAttribute("importResults");

				List<String> counters= importresults .get("counters");
				int contentsToImport=0;
				for(String counter: counters ){
					String counterArray[]=counter.split("=");
					if(counterArray[0].equals("newContent") || counterArray[0].equals("contentToUpdate"))
						contentsToImport=contentsToImport + Integer.parseInt(counterArray[1]);		
				}

				List<String> inodes= importresults.get("lastInode");

				ImportAuditUtil.updateAuditRecord(inodes.get(0), contentsToImport, importId, importresults);

				csvreader.close();

				setForward(req, "portlet.ext.plugins.content.importer.struts.import_external_contentlets_results");

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}

		} else if(cmd != null && cmd.equals("downloadCSVTemplate")){
			_downloadCSVTemplate(req, res,config,form);
		} else {
			ImportAuditResults audits = ImportAuditUtil.loadAuditResults(user.getUserId());
			req.setAttribute("audits", audits);			
			session.setAttribute("importSession", importSession);
			setForward(req, "portlet.ext.plugins.content.importer.struts.import_external_contentlets");
		}                    

	}

	// /// ************** ALL METHODS HERE *************************** ////////
	private void _downloadCSVTemplate(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form) throws Exception {

		ActionResponseImpl resImpl = (ActionResponseImpl)res;
		HttpServletResponse httpRes = resImpl.getHttpServletResponse();

		httpRes.setContentType("application/octet-stream");
		httpRes.setHeader("Content-Disposition", "attachment; filename=\"CSV_Template.csv\"");

		ServletOutputStream out = httpRes.getOutputStream();
		ImportExternalContentletsForm importForm = (ImportExternalContentletsForm) form;

		List<Field> fields = FieldsCache.getFieldsByStructureInode(importForm.getStructure());
		for(int i = 0; i < fields.size(); i++) {
			Field field = fields.get(i);
			if (ContentletUtil.isImportableField(field)) {
				String fieldName = field.getFieldName();
				if(fieldName.contains(","))
					out.print("\"" + fieldName + "\"");
				else
					out.print(fieldName);
				if(i < fields.size() - 1) 
					out.print(",");
			}
		}
		out.print("\n");

		for(int i = 0; i < fields.size(); i++) {
			Field field = fields.get(i);
			if (ContentletUtil.isImportableField(field)) {
				if (field.getFieldType().equals(Field.FieldType.DATE.toString())) {
					out.print("MM/dd/yyyy");
				}        	
				else if (field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {
					out.print("MM/dd/yyyy hh:mm aa");
				}         	
				else if (field.getFieldType().equals(Field.FieldType.TIME.toString())) {
					out.print("hh:mm aa");
				}else if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) {
					out.print("Host/Folder Identifier");
				}else if (field.getFieldType().equals(Field.FieldType.CATEGORY.toString())) {
					out.print("Category Unique Key");
				} else {
					out.print("XXX");
				}
				if(i < fields.size() - 1) 
					out.print(",");
			}
		}

		out.flush();
		out.close();
		HibernateUtil.closeSession();
	}

	private void _generatePreview(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user, byte[] bytes, String[] csvHeaders, CsvReader csvreader, int languageCodeHeaderColumn, int countryCodeHeaderColumn, Reader reader) throws Exception {

		// wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		httpReq.getSession().setAttribute("file_to_import", bytes);
		httpReq.getSession().setAttribute("form_to_import", form);

		ImportExternalContentletsForm importForm = (ImportExternalContentletsForm) form;
		httpReq.getSession().setAttribute("fileName", importForm.getFileName());

		HashMap<String, List<String>> results = importFile(importForm.getStructure(), null, true, (importForm.getLanguage() == -1), user, importForm.getLanguage(), csvHeaders, csvreader, languageCodeHeaderColumn, countryCodeHeaderColumn, reader,importForm.getTitleField(),importForm.getContentField(),importForm.getPathField());

		req.setAttribute("previewResults", results);
	}

	private void _processFile(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user, String[] csvHeaders, CsvReader csvreader, int languageCodeHeaderColumn, int countryCodeHeaderColumn, Reader reader)
	throws Exception {

		// wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		ImportExternalContentletsForm importForm = (ImportExternalContentletsForm) httpReq.getSession().getAttribute("form_to_import");

		HashMap<String, List<String>> results = importFile(importForm.getStructure(), null, false, (importForm.getLanguage() == -1), user, importForm.getLanguage(), csvHeaders, csvreader, languageCodeHeaderColumn, countryCodeHeaderColumn, reader,importForm.getTitleField(),importForm.getContentField(),importForm.getPathField());

		req.setAttribute("importResults", results);
	}

	private final static int commitGranularity = 10;
	private final static int sleepTime = 200;

	private class Counters {
		private int newContentCounter = 0;
		private int contentToUpdateCounter = 0;
		private int contentCreated = 0;
		private int contentUpdated = 0;
		private int contentUpdatedDuplicated = 0;
		/**
		 * @return the newContentCounter
		 */
		public int getNewContentCounter() {
			return newContentCounter;
		}
		/**
		 * @param newContentCounter the newContentCounter to set
		 */
		public void setNewContentCounter(int newContentCounter) {
			this.newContentCounter = newContentCounter;
		}
		/**
		 * @return the contentToUpdateCounter
		 */
		public int getContentToUpdateCounter() {
			return contentToUpdateCounter;
		}
		/**
		 * @param contentToUpdateCounter the contentToUpdateCounter to set
		 */
		public void setContentToUpdateCounter(int contentToUpdateCounter) {
			this.contentToUpdateCounter = contentToUpdateCounter;
		}
		/**
		 * @return the contentCreated
		 */
		public int getContentCreated() {
			return contentCreated;
		}
		/**
		 * @param contentCreated the contentCreated to set
		 */
		public void setContentCreated(int contentCreated) {
			this.contentCreated = contentCreated;
		}
		/**
		 * @return the contentUpdated
		 */
		public int getContentUpdated() {
			return contentUpdated;
		}
		/**
		 * @param contentUpdated the contentUpdated to set
		 */
		public void setContentUpdated(int contentUpdated) {
			this.contentUpdated = contentUpdated;
		}
		/**
		 * @return the contentUpdatedDuplicated
		 */
		public int getContentUpdatedDuplicated() {
			return contentUpdatedDuplicated;
		}
		/**
		 * @param contentUpdatedDuplicated the contentUpdatedDuplicated to set
		 */
		public void setContentUpdatedDuplicated(int contentUpdatedDuplicated) {
			this.contentUpdatedDuplicated = contentUpdatedDuplicated;
		}
	}

	private HashMap<String, List<String>> importFile(String structure, String[] keyfields, boolean preview, boolean isMultilingual, User user, long language, String[] csvHeaders, CsvReader csvreader, int languageCodeHeaderColumn, int countryCodeHeaderColumn, Reader reader, String titleField, String contentField, String pathField)
	throws DotRuntimeException, DotDataException {

		HashMap<String, List<String>> results = new HashMap<String, List<String>>();
		results.put("warnings", new ArrayList<String>());
		results.put("errors", new ArrayList<String>());
		results.put("messages", new ArrayList<String>());
		results.put("results", new ArrayList<String>());
		results.put("counters", new ArrayList<String>());
		results.put("identifiers", new ArrayList<String>());
		results.put("lastInode", new ArrayList<String>());

		Structure st = StructureCache.getStructureByInode (structure);
		List<Permission> structurePermissions = permissionAPI.getPermissions(st);

		//Initializing variables
		int lines = 0;
		int errors = 0;
		int lineNumber = 0;

		Counters counters = new Counters();
		HashSet<String> keyContentUpdated = new HashSet<String>(); 
		StringBuffer choosenKeyField = new StringBuffer();

		HashMap<Integer, Field> headers = new HashMap<Integer, Field>();
		HashMap<Integer, Field> keyFields = new HashMap<Integer, Field> ();

		//Parsing the file line per line
		try {
			if ((csvHeaders != null) || (csvreader.readHeaders())) {

				//Importing headers from the first file line
				if (csvHeaders != null)
					importHeaders(csvHeaders, st, keyfields, preview, isMultilingual, user, results, headers, keyFields,titleField,contentField,pathField);
				else
					importHeaders(csvreader.getHeaders(), st, keyfields, preview, isMultilingual, user, results, headers, keyFields,titleField,contentField,pathField);
				lineNumber++;

				//Reading the whole file
				if (headers.size() > 0) {

					if (!preview)
						HibernateUtil.startTransaction();

					String[] csvLine;
					Language dotCMSLanguage;
					while (csvreader.readRecord()) {
						lineNumber++;
						csvLine = csvreader.getValues();
						try {
							lines++;
							Logger.debug(this, "Line " + lines + ": (" + csvreader.getRawRecord() + ").");
							//Importing a line
							if (0 < language) {
								results.get("identifiers").add(csvLine[0]);
								importLine(csvLine, st, preview, isMultilingual, user, results, lineNumber, language, headers, keyFields, choosenKeyField,
										counters, keyContentUpdated, structurePermissions,titleField,contentField,pathField);
							} else {

								dotCMSLanguage = langAPI.getLanguage(csvLine[languageCodeHeaderColumn], csvLine[countryCodeHeaderColumn]);

								if (0 < dotCMSLanguage.getId()) {
									results.get("identifiers").add(csvLine[0]);
									importLine(csvLine, st, preview, isMultilingual, user, results, lineNumber, dotCMSLanguage.getId(), headers, keyFields, choosenKeyField,
											counters, keyContentUpdated, structurePermissions,titleField,contentField,pathField);
								} else {
									results.get("errors").add(LanguageUtil.get(user, "Line--" ) + lineNumber + LanguageUtil.get(user, "Locale-not-found-for-languageCode" )+" ='" + csvLine[languageCodeHeaderColumn] + "' countryCode='" + csvLine[countryCodeHeaderColumn] + "'");
									errors++;
								}
							}

							if (!preview && (lineNumber % commitGranularity == 0)) {
								HibernateUtil.commitTransaction();
								HibernateUtil.startTransaction();
							}

							if (!preview)
								Thread.sleep(sleepTime);
						} catch (DotRuntimeException ex) {

							String errorMessage = ex.getMessage();
							if(errorMessage.indexOf("Line #") == -1){
								errorMessage = "Line #"+lineNumber+" "+errorMessage;
							}
							results.get("errors").add(errorMessage);
							errors++;
							Logger.debug(this, "Error line: " + lines + " (" + csvreader.getRawRecord()
									+ "). Line Ignored.");
						}
					}

					if(!preview){
						results.get("counters").add("linesread="+lines);
						results.get("counters").add("errors="+errors);
						results.get("counters").add("newContent="+counters.getNewContentCounter());
						results.get("counters").add("contentToUpdate="+counters.getContentToUpdateCounter());
						HibernateUtil.commitTransaction();
					}

					results.get("messages").add(lines + " "+LanguageUtil.get(user, "lines-of-data-were-read" ));
					if (errors > 0)
						results.get("errors").add(errors + " " + LanguageUtil.get(user, "input-lines-had-errors" ));  

					if(preview && choosenKeyField.length() > 1)
						results.get("messages").add( LanguageUtil.get(user, "Fields-selected-as-key")+": "+choosenKeyField.substring(1).toString()+".");

					if (counters.getNewContentCounter() > 0)
						results.get("messages").add(LanguageUtil.get(user, "Approximately") + " " + (counters.getNewContentCounter()) + " " + LanguageUtil.get(user, "new-content-will-be-created"));

					if (counters.getContentToUpdateCounter() > 0)
						results.get("messages").add(LanguageUtil.get(user, "Approximately") + " " + (counters.getContentToUpdateCounter()) + " " + LanguageUtil.get(user, "old-content-will-be-updated"));


					results.get("results").add(counters.getContentCreated() + " "+LanguageUtil.get(user, "new")+" "+"\"" + st.getName() + "\" "+ LanguageUtil.get(user, "were-created"));
					results.get("results").add(counters.getContentUpdatedDuplicated() + " \"" + st.getName() + "\" "+ LanguageUtil.get(user, "contentlets-updated-corresponding-to")+" "+ counters.getContentUpdated() +" "+ LanguageUtil.get(user, "repeated-contents-based-on-the-key-provided"));

					if (errors > 0)
						results.get("results").add(errors + " "+ LanguageUtil.get(user, "contentlets-were-ignored-due-to-invalid-information"));

				} else {
					results.get("errors").add(LanguageUtil.get(user, "No-headers-found-on-the-file-nothing-will-be-imported"));
				}
			}
		} catch (Exception e) {
			Logger.error(ImportContentletsAction.class,e.getMessage());

		} finally {

			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {

				}
		}
		Logger.info(this, lines + " lines read correctly. " + errors + " errors found.");

		return results;
	}

	private void importHeaders(String[] headerLine, Structure structure, String[] keyFieldsInodes, boolean preview, boolean isMultilingual, User user, HashMap<String, List<String>> results, HashMap<Integer, Field> headers, HashMap<Integer, Field> keyFields, String titleField, String contentField, String pathField) throws Exception  {

		int titleInt=0;
		int contentInt=0;
		if(headerLine.length == 2){
			boolean titleFound=false;
			boolean pathFound=false;

			for (int i = 0; i < headerLine.length; i++) {
				String header=headerLine[i].toLowerCase();
				if(header.equals(FILE_TITLE)){
					titleFound=true;
					titleInt=i;
				}
				if(header.equals(FILE_PATH)) {
					pathFound=true;
					contentInt=i;
				}
			}
			if (!titleFound ||  !pathFound) {
				results.get("errors").add(LanguageUtil.get(user, "Headers ")+LanguageUtil.get(user, "file-headers-doesn-t-match-any-of-the-headers-required-in-the-file"));
			}
		}else{
			results.get("errors").add(LanguageUtil.get(user, "Headers ")+LanguageUtil.get(user, "file-headers-doesn-t-match-any-of-the-headers-required-in-the-file"));
		}

		List<Field> fields = FieldsCache.getFieldsByStructureInode(structure.getInode());
		int i =0;
		if(UtilMethods.isSet(titleField)){
			boolean found=false;
			for(Field field : fields){
				if(field.getVelocityVarName().equals(titleField)){
					found=true;
					headers.put(titleInt, field);
					keyFields.put(titleInt,field);
					i++;
				}
			}
			if(!found){
				results.get("errors").add(titleField+" "+LanguageUtil.get(user, "doesn-t-match-any-structure-field"));
			}
		}else {
			results.get("errors").add(LanguageUtil.get(user, "message.import.external.content.importer.title.required"));
		}
		if(UtilMethods.isSet(contentField)){
			boolean found=false;
			for(Field field : fields){
				if(field.getVelocityVarName().equals(contentField)){
					found=true;
					headers.put(contentInt, field);
					i++;
				}
			}
			if(!found){
				results.get("errors").add(contentField+LanguageUtil.get(user, "doesn-t-match-any-structure-field"));
			}
		}else {
			results.get("errors").add(LanguageUtil.get(user, "message.import.external.content.importer.content.required"));
		}
		if(UtilMethods.isSet(pathField)){
			boolean found=false;
			for(Field field : fields){
				if(field.getVelocityVarName().equals(pathField)){
					found=true;
					headers.put(3, field);
					i++;
				}
			}
			if(!found){
				results.get("errors").add(pathField+LanguageUtil.get(user, "doesn-t-match-any-structure-field"));
			}
		}

		if(headers.size() ==0){
			results.get("errors").add(LanguageUtil.get(user, "doesn-t-match-any-structure-field"));
		}
	}

	private void importLine(String[] line, Structure structure, boolean preview, boolean isMultilingual, User user, HashMap<String, List<String>> results, int lineNumber, long language,
			HashMap<Integer, Field> headers, HashMap<Integer, Field> keyFields, StringBuffer choosenKeyField, Counters counters,
			HashSet<String> keyContentUpdated, List<Permission> structurePermissions,String titleField, String contentField, String pathField) throws DotRuntimeException {
		try {
			//Building a values HashMap based on the headers/columns position 
			if(headers.size() ==0){
				throw new DotRuntimeException(LanguageUtil.get(user,"doesn-t-match-any-structure-field"));
			}
			if(!UtilMethods.isSet(titleField)){
				throw new DotRuntimeException(LanguageUtil.get(user,"message.import.external.content.importer.title.required"));
			}
			if(!UtilMethods.isSet(contentField)){
				throw new DotRuntimeException(LanguageUtil.get(user,"message.import.external.content.importer.content.required"));
			}
			HashMap<Integer, Object> values = new HashMap<Integer, Object>();
			Set<Category> categories = new HashSet<Category> ();
			int i =0;
			for (Integer column : headers.keySet()) {
				Field field = headers.get(column);
				if(field.getVelocityVarName().equals(pathField)){
					continue;
				}
				if (line.length < 2) {
					throw new DotRuntimeException("Incomplete line found, the line #" + lineNumber + " doesn't contain all the required columns.");
				}
				String value = line[column];
				i++;
				Object valueObj = value;
				if (field.getFieldType().equals(Field.FieldType.DATE.toString())) {
					if (field.getFieldContentlet().startsWith("date")) {
						if(UtilMethods.isSet(value)) {
							try { valueObj = parseExcelDate(value) ;} catch (ParseException e) { 
								throw new DotRuntimeException("Line #" + lineNumber + " contains errors, Column: " + field.getFieldName() + 
										", value: " + value + ", couldn't be parsed as any of the following supported formats: " + 
										printSupportedDateFormats());
							}
						} else {
							valueObj = null;
						}
					}
				} else if (field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {
					if (field.getFieldContentlet().startsWith("date")) {
						if(UtilMethods.isSet(value)) {
							try { valueObj = parseExcelDate(value) ;} catch (ParseException e) { 
								throw new DotRuntimeException("Line #" + lineNumber + " contains errors, Column: " + field.getFieldName() + 
										", value: " + value + ", couldn't be parsed as any of the following supported formats: " + 
										printSupportedDateFormats());
							}
						} else {
							valueObj = null;
						}
					}
				} else if (field.getFieldType().equals(Field.FieldType.TIME.toString())) {
					if (field.getFieldContentlet().startsWith("date")) {
						if(UtilMethods.isSet(value)) {
							try { valueObj = parseExcelDate(value) ;} catch (ParseException e) { 
								throw new DotRuntimeException("Line #" + lineNumber + " contains errors, Column: " + field.getFieldName() + 
										", value: " + value + ", couldn't be parsed as any of the following supported formats: " + 
										printSupportedDateFormats());
							}
						} else {
							valueObj = null;
						}
					}
				} else if (field.getFieldType().equals(Field.FieldType.CATEGORY.toString()) || field.getFieldType().equals(Field.FieldType.CATEGORIES_TAB.toString())) {
					valueObj = value;
					if(UtilMethods.isSet(value)) {
						String[] categoryKeys = value.split(",");
						for(String catKey : categoryKeys) {
							Category cat = catAPI.findByKey(catKey.trim(), user, false);
							if(cat == null)
								throw new DotRuntimeException("Line #" + lineNumber + " contains errors, Column: " + field.getFieldName() + 
										", value: " + value + ", invalid category key found, line will be ignored.");
							categories.add(cat);
						}
					}
				}
				else if (field.getFieldType().equals(Field.FieldType.CHECKBOX.toString()) ||
						field.getFieldType().equals(Field.FieldType.SELECT.toString()) ||
						field.getFieldType().equals(Field.FieldType.MULTI_SELECT.toString()) ||
						field.getFieldType().equals(Field.FieldType.RADIO.toString())
				) {
					valueObj = value;
					if(UtilMethods.isSet(value)) 
					{


						String fieldEntriesString = field.getValues();
						String[] fieldEntries = fieldEntriesString.split("\n");
						boolean found = false;
						for(String fieldEntry : fieldEntries) 
						{
							String[] splittedValue = fieldEntry.split("\\|");
							String entryValue = splittedValue[splittedValue.length - 1].trim();

							if(entryValue.equals(value) || value.contains(entryValue))
							{
								found = true;
								break;
							}														
						}
						if(!found)
						{
							throw new DotRuntimeException("Line #" + lineNumber + " contains errors, Column: " + field.getFieldName() + 
									", value: " + value + ", invalid value found, line will be ignored.");
						}
					}
					else {
						valueObj = null;
					}
				} 
				else if (field.getFieldType().equals(Field.FieldType.TEXT.toString())) {
					if (value.length() > 255)
						value = value.substring(0, 255);
					valueObj = UtilMethods.escapeUnicodeCharsForHTML(value);
				}//http://jira.dotmarketing.net/browse/DOTCMS-3232
				else if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) {
					if(InodeFactory.isInode(value)){
						valueObj = value;
					}else{
						throw new DotRuntimeException("Line #" + lineNumber + " contains errors, Column: " + field.getFieldName() + 
								", value: " + value + ", invalid host/folder inode found, line will be ignored.");
					}
				}else {
					valueObj = UtilMethods.escapeUnicodeCharsForHTML(value);
				}
				values.put(column, valueObj);
				if(field.getVelocityVarName().equals(contentField) && UtilMethods.isSet(pathField)){
					values.put(3, valueObj);
				}
			}

			//Searching contentlets to be updated by key fields
			List<Contentlet> contentlets = new ArrayList<Contentlet>();
			String conditionValues = "";
			StringBuffer buffy = new StringBuffer();

			if (isMultilingual)
				buffy.append("+structureInode:" + structure.getInode() + " +working:true +deleted:false");
			else
				buffy.append("+structureInode:" + structure.getInode() + " +working:true +deleted:false +languageId:" + language);


			if (keyFields.size() > 0) {

				for (Integer column : keyFields.keySet()) {
					Field field = keyFields.get(column);
					Object value = values.get(column);
					String text = null;
					if (value instanceof Date || value instanceof Timestamp) {
						SimpleDateFormat formatter = null;
						if(field.getFieldType().equals(Field.FieldType.DATE.toString())){
							text = DATE_FIELD_FORMAT.format((Date)value);
						}else if(field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())){
							DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
							text = df.format((Date)value);
						}else if(field.getFieldType().equals(Field.FieldType.TIME.toString())) {
							DateFormat df = new SimpleDateFormat("HHmmss");
							text =  df.format((Date)value);
						} else {
							formatter = new SimpleDateFormat();
							text = formatter.format(value);
							Logger.warn(ImportUtil.class,"importLine: field's date format is undetermined.");
						}                              
					} else {
						text = UtilMethods.doubleQuoteIt(value.toString());
					}
					if(!UtilMethods.isSet(text)){
						throw new DotRuntimeException("Line #" + lineNumber + " key field "+field.getFieldName()+" is required since it was defined as a key\n");
					}else{  
						if(field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString()))
							buffy.append(" +(conhost:" + text + " conFolder:" + text+")");
						else
							buffy.append(" +" + field.getFieldContentlet() + ":" + escapeLuceneSpecialCharacter(text));
						conditionValues += conditionValues + value + "-";
					}

					if(choosenKeyField.indexOf(field.getFieldName()) == -1){
						choosenKeyField.append(", "+field.getFieldName());
					}
				}
				List<Contentlet> cons = conAPI.checkoutWithQuery(buffy.toString(), user, true); 
				for (Contentlet con : cons) {
					boolean columnExists = false;
					for (Integer column : keyFields.keySet()) {
						Field field = keyFields.get(column);
						Object value = values.get(column);
						if(conAPI.getFieldValue(con, field).toString().equalsIgnoreCase(value.toString())){
							columnExists = true;
						}else{
							columnExists = false;
							break;
						}
					}
					if(columnExists)
						contentlets.add(con);
				}
			}


			//Creating/updating content
			boolean isNew = false;
			if (contentlets.size() == 0) {
				counters.setNewContentCounter(counters.getNewContentCounter() + 1);
				isNew = true;
				//if (!preview) {
				Contentlet newCont = new Contentlet();
				//APILocator.getVersionableAPI().setLive(newCont);
				//APILocator.getVersionableAPI().setWorking(newCont);
				newCont.setStructureInode(structure.getInode());
				newCont.setLanguageId(language);
				contentlets.add(newCont);
				//}
			} else {
				if (isMultilingual) {
					List<Contentlet> multilingualContentlets = new ArrayList<Contentlet>();

					for (Contentlet contentlet: contentlets) {
						if (contentlet.getLanguageId() == language)
							multilingualContentlets.add(contentlet);
					}

					if (multilingualContentlets.size() == 0) {
						String lastIdentifier = "" ;
						isNew = true;
						for (Contentlet contentlet: contentlets) {
							if (!contentlet.getIdentifier().equals(lastIdentifier)) {
								counters.setNewContentCounter(counters.getNewContentCounter() + 1);
								Contentlet newCont = new Contentlet();
								newCont.setIdentifier(contentlet.getIdentifier());
								//APILocator.getVersionableAPI().setLive(newCont);
								//APILocator.getVersionableAPI().setWorking(newCont);
								newCont.setStructureInode(structure.getInode());
								newCont.setLanguageId(language);
								multilingualContentlets.add(newCont);

								lastIdentifier = contentlet.getIdentifier();
							}
						}
					}

					contentlets = multilingualContentlets;
				}

				if (!isNew) {
					if (conditionValues.equals("") || !keyContentUpdated.contains(conditionValues) || isMultilingual) {
						counters.setContentToUpdateCounter(counters.getContentToUpdateCounter() + contentlets.size());
						if (preview)
							keyContentUpdated.add(conditionValues);
					}
					if (contentlets.size() > 0) {
						results.get("warnings").add(
								LanguageUtil.get(user, "Line--") + lineNumber + ". "+ LanguageUtil.get(user, "The-key-fields-choosen-match-more-than-one-content-in-this-case")+": "  
								+ " "+ LanguageUtil.get(user, "matches")+": " + contentlets.size() + " " +LanguageUtil.get(user, "different-content-s-looks-like-the-key-fields-choosen")+" " +
								LanguageUtil.get(user, "aren-t-a-real-key"));
					}
				}
			}


			for (Contentlet cont : contentlets) 
			{
				//Fill the new contentlet with the data
				for (Integer column : headers.keySet()) {
					Field field = headers.get(column);
					Object value = values.get(column);
					if(field.getVelocityVarName().equals(contentField)){
						String path = (String) value; 
						value= getExternalField(path);
					}


					if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) { // DOTCMS-4484

						Host host = hostAPI.find(value.toString(), user, false);				
						Folder folder = folderAPI.find(value.toString(), user, false);

						if (folder != null && folder.getInode().equalsIgnoreCase(value.toString())) {
							if (!permissionAPI.doesUserHavePermission(folder,PermissionAPI.PERMISSION_CAN_ADD_CHILDREN,user)) {
								throw new DotSecurityException("User have no Add Children Permissions on selected host");
							}
							cont.setHost(folder.getHostId());
							cont.setFolder(value.toString());
						}
						else if(host != null) {						
							if (!permissionAPI.doesUserHavePermission(host,PermissionAPI.PERMISSION_CAN_ADD_CHILDREN,user)) {
								throw new DotSecurityException("User have no Add Children Permissions on selected host");
							}
							cont.setHost(value.toString());
							cont.setFolder(FolderAPI.SYSTEM_FOLDER);
						} 
						continue;
					}
					conAPI.setContentletProperty(cont, field, value);
					if (field.getFieldType().equals(Field.FieldType.TAG.toString()) &&
							value instanceof String) {
						String[] tags = ((String)value).split(",");
						for (String tag : tags) {
							try {
								tagAPI.addTagInode((String)tag.trim(), cont.getInode(), Host.SYSTEM_HOST);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}

				//DOTCMS-4528 Retaining Categories when content updated with partial imports
				if(UtilMethods.isSet(cont.getIdentifier())){					

					List<Field> structureFields = FieldsCache.getFieldsByStructureInode(structure.getInode());
					List<Field> categoryFields = new ArrayList<Field>();
					List<Field> nonHeaderCategoryFields = new ArrayList<Field>();			
					List<Category> nonHeaderParentCats = new ArrayList<Category>();
					List<Category> categoriesToRetain = new ArrayList<Category>();
					List<Category> categoriesOnWorkingContent = new ArrayList<Category>();

					for(Field field : structureFields){
						if(field.getFieldType().equals(Field.FieldType.CATEGORY.toString()) || field.getFieldType().equals(Field.FieldType.CATEGORIES_TAB.toString()))
							categoryFields.add(field);			
					}

					for (Integer column : headers.keySet()) {
						Field headerField = headers.get(column);
						Iterator<Field> itr = categoryFields.iterator();
						while(itr.hasNext()){
							Field field = itr.next();
							if(headerField.getInode().equalsIgnoreCase(field.getInode())){
								itr.remove();
							}
						}
					}

					nonHeaderCategoryFields.addAll(categoryFields);

					for(Field field : nonHeaderCategoryFields){
						nonHeaderParentCats.add(catAPI.find(field.getValues(), user, false));
					}

					for(Category cat : nonHeaderParentCats){
						categoriesToRetain.addAll(catAPI.getChildren(cat,false, user, false));
					}		

					Contentlet workingCont = conAPI.findContentletByIdentifier(cont.getIdentifier(), false,langAPI.getDefaultLanguage().getId() , user, false);
					categoriesOnWorkingContent = catAPI.getParents(workingCont, user, false);

					for(Category existingCat : categoriesOnWorkingContent){
						for(Category retainCat :categoriesToRetain){
							if(existingCat.compareTo(retainCat) == 0){
								categories.add(existingCat);
							}
						}
					}

				}				


				//Check the new contentlet with the validator
				try
				{
					conAPI.validateContentlet(cont,new ArrayList<Category>(categories));
				}
				catch(DotContentletValidationException ex)
				{
					StringBuffer sb = new StringBuffer("Line #" + lineNumber + " contains errors\n");
					HashMap<String,List<Field>> errors = (HashMap<String,List<Field>>) ex.getNotValidFields();
					Set<String> keys = errors.keySet();					
					for(String key : keys)
					{
						sb.append(key + ": ");
						List<Field> fields = errors.get(key);
						for(Field field : fields)
						{
							sb.append(field.getFieldName() + ",");
						}
						sb.append("\n");
					}
					throw new DotRuntimeException(sb.toString());
				}

				//If not preview save the contentlet
				if (!preview) 
				{
					cont.setLowIndexPriority(true);
					cont = conAPI.checkin(cont, new ArrayList<Category>(categories), structurePermissions, user, false);
					APILocator.getVersionableAPI().setLive(cont);
					APILocator.getVersionableAPI().setWorking(cont);

					results.get("lastInode").clear();
					List<String> l = results.get("lastInode");
					l.add(cont.getInode());
					results.put("lastInode", l);
				}

				if (isNew){
					counters.setContentCreated(counters.getContentCreated() + 1);
				}else{
					if (conditionValues.equals("") || !keyContentUpdated.contains(conditionValues)) {
						counters.setContentUpdated(counters.getContentUpdated() + 1);
						counters.setContentUpdatedDuplicated(counters.getContentUpdatedDuplicated() + 1);
						keyContentUpdated.add(conditionValues);
					}else{
						counters.setContentUpdatedDuplicated(counters.getContentUpdatedDuplicated() + 1);
					}

				}
			}          

		} catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
			throw new DotRuntimeException(e.getMessage());
		}

	}

	public static final String[] IMP_DATE_FORMATS = new String[] { "d-MMM-yy", "MMM-yy", "MMMM-yy", "d-MMM", "dd-MMM-yyyy", 
		"MM/dd/yy hh:mm aa", "MM/dd/yyyy hh:mm aa",	"MM/dd/yy HH:mm", "MM/dd/yyyy HH:mm", "MMMM dd, yyyy", "M/d/y", "M/d", 
		"EEEE, MMMM dd, yyyy", "MM/dd/yyyy", "hh:mm:ss aa", "HH:mm:ss", "hh:mm aa" };

	private static String printSupportedDateFormats () {
		StringBuffer ret = new StringBuffer("[ ");
		for (String pattern : IMP_DATE_FORMATS) {
			ret.append(pattern + ", ");
		}
		ret.append(" ] ");
		return ret.toString();
	}

	private Date parseExcelDate (String date) throws ParseException 
	{    	    
		return DateUtil.convertDate(date, IMP_DATE_FORMATS);    	
	}

	/**
	 * Escape lucene reserved characters
	 * @param text
	 * @return String
	 */
	private String escapeLuceneSpecialCharacter(String text){
		text = text.replaceAll("\\[","\\\\[").replaceAll("\\]","\\\\]");
		text = text.replaceAll("\\{","\\\\{").replaceAll("\\}","\\\\}");
		text = text.replaceAll("\\+","\\\\+").replaceAll(":","\\\\:");
		text = text.replaceAll("\\*","\\\\*").replaceAll("\\?","\\\\?");
		text = text.replaceAll("\\(","\\\\(").replaceAll("\\)","\\\\)");
		text = text.replaceAll("&&","\\\\&&").replaceAll("\\|\\|","\\\\||");
		text = text.replaceAll("!","\\\\!").replaceAll("\\^","\\\\^");
		text = text.replaceAll("-","\\\\-").replaceAll("~","\\\\~");
		text = text.replaceAll("\"","\\\"");

		return text;
	}

	private String getExternalField(String path){

		try {
			java.net.URL url = new java.net.URL( path);
			java.net.URLConnection uconn = url.openConnection( );
			if(!(uconn instanceof java.net.HttpURLConnection) ){
				throw new java.lang.IllegalArgumentException("URL protocol must be HTTP." );
			}
			java.net.HttpURLConnection conn = (java.net.HttpURLConnection)uconn;

			// Set up a request.
			conn.setConnectTimeout( 600000 );    // 10 minutes
			conn.setReadTimeout( 600000 );       // 10 minutes
			conn.setInstanceFollowRedirects( true );
			conn.setRequestProperty( "User-agent", "spider" );

			// Send the request.
			conn.connect( );

			// Get the response.
			java.util.Map<String,java.util.List<String>> responseHeader = conn.getHeaderFields( );
			int responseCode  = conn.getResponseCode( );
			java.net.URL responseURL = conn.getURL( );
			String MIMEtype = null;
			String charset = null;
			Object content = null;

			int length  = conn.getContentLength( );
			String type = conn.getContentType( );
			if ( type != null ) {
				final String[] parts = type.split( ";" );
				MIMEtype = parts[0].trim( );
				for ( int i = 1; i < parts.length && charset == null; i++ ) {
					final String t  = parts[i].trim( );
					final int index = t.toLowerCase( ).indexOf( "charset=" );
					if ( index != -1 )
						charset = t.substring( index+8 );
				}
			}

			// Get the content.
			java.io.InputStream stream = conn.getErrorStream( );
			if ( stream != null )
				content = readStream(charset, length, stream );
			else if ( (content = conn.getContent( )) != null && content instanceof java.io.InputStream )
				content = readStream(charset, length, (java.io.InputStream)content );

			conn.disconnect( );

			if(content instanceof String){
				return (String) content;
			}else{
				return new String(((byte[])content));	
			}
		}catch(Exception e1){
			Logger.error(ContentletUtil.class, e1.getMessage());
			return null;
		}

	}


	/**
	 * Read stream bytes and transcode.
	 */
	private Object readStream(String charset, int length, java.io.InputStream stream )
	throws java.io.IOException {
		int buflen = Math.max( 1024, Math.max( length, stream.available() ) );
		byte[] buf   = new byte[buflen];;
		byte[] bytes = null;


		for ( int nRead = stream.read(buf); nRead != -1; nRead = stream.read(buf) ) {
			if ( bytes == null ) {
				bytes = buf;
				buf   = new byte[buflen];
				continue;
			}
			byte[] newBytes = new byte[ bytes.length + nRead ];
			System.arraycopy( bytes, 0, newBytes, 0, bytes.length );
			System.arraycopy( buf, 0, newBytes, bytes.length, nRead );
			bytes = newBytes;

		}
		if ( charset == null ){
			return bytes;
		}
		try {
			return new String( bytes, charset );
		} catch(java.io.UnsupportedEncodingException e ) { 
			return bytes;
		}
	}

}
