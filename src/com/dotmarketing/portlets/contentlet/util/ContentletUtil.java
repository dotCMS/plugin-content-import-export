package com.dotmarketing.portlets.contentlet.util;

import java.io.IOException;
import java.io.Reader;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.csvreader.CsvReader;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.cms.factories.PublicRoleFactory;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.DotHibernate;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.factories.HostFactory;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.action.ImportContentletsAction;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.tag.factories.TagFactory;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.LuceneUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.User;

public class ContentletUtil {
	
	//API
	private PermissionAPI permissionAPI = APILocator.getPermissionAPI();
	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private CategoryAPI catAPI = APILocator.getCategoryAPI();
	private LanguageAPI langAPI = APILocator.getLanguageAPI();
	
	//Final Variables
	public final static String languageCodeHeader = "languageCode";
	public final static String countryCodeHeader = "countryCode";
	public final static String identifierFieldName = "identifier";
	private int identifierFieldPosition = -1;
	private int languageFieldPosition = -1;
	private int countryFieldPosition = -1;
	
	private Host defaultHost = HostFactory.getDefaultHost();

	//Temp maps used to parse the file
	private HashMap<Integer, Field> headers;
	private HashMap<Integer, Field> keyFields;

	//Counters for the preview page 
	private int newContentCounter;
	private int contentToUpdateCounter;

	//Counters for the results page
	private int contentUpdatedDuplicated;
	private int contentUpdated;
	private int contentCreated;
	private HashSet keyContentUpdated = new HashSet(); 
	private StringBuffer choosenKeyField;

	private int commitGranularity = 10;
	private int sleepTime = 200;
	private Role CMSAdmin; 
	private List<Permission> structurePermissions;
	
	//Field from the Action
	private Reader reader;
	private CsvReader csvreader;
	private String[] csvHeaders;	
	
	public ContentletUtil(Reader reader, CsvReader csvreader)
	{
		try
		{
		this.reader = reader;
		this.csvreader = csvreader;
		this.csvHeaders = csvreader.getHeaders();
		}
		catch(Exception ex)
		{
			throw new DotRuntimeException(ex.getMessage(),ex);
		}
	}
		
	public HashMap<String, List<String>> importFile(long structure, long[] keyfields, boolean preview,User user,boolean isMultilingual, long language,boolean publishContent)
	throws DotRuntimeException 
	{
		if (!preview && DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)) {
			ReindexThread.getInstance().pause();
		}
		HashMap<String, List<String>> results = new HashMap();
		results.put("warnings", new ArrayList<String>());
		results.put("errors", new ArrayList<String>());
		results.put("messages", new ArrayList<String>());
		results.put("results", new ArrayList<String>());

		Structure st = StructureCache.getStructureByInode (structure);
		try {
			CMSAdmin = PublicRoleFactory.getCMSAdminRole();
		} catch (Exception e1) {
			Logger.error (this, "importFile: failed retrieving the CMSAdmin role.", e1);
			throw new DotRuntimeException (e1.getMessage());
		}
		structurePermissions = permissionAPI.getPermissions(st);

		//Initializing variables
		int lines = 0;
		int errors = 0;
		int lineNumber = 0;
		newContentCounter = 0;
		contentToUpdateCounter = 0;
		contentCreated = 0;
		contentUpdated = 0;
		contentUpdatedDuplicated = 0;
		keyContentUpdated = new HashSet (); 
		choosenKeyField = new StringBuffer();

		headers = new HashMap<Integer, Field> ();
		keyFields = new HashMap<Integer, Field> ();


		//Parsing the file line per line
		try {
			if ((csvHeaders != null) || (csvreader.readHeaders())) {
				
				boolean isIdentifierKeyField = false;
				for (long key: keyfields) {
					if (key == 0) {
						isIdentifierKeyField = true;
						break;
					}
				}

				//Importing headers from the first file line
				csvHeaders = (csvHeaders != null ? csvHeaders : csvreader.getHeaders());											
				importHeaders(csvHeaders, st, keyfields, isIdentifierKeyField,preview, isMultilingual, user, results);
				lineNumber++;

				//Reading the whole file
				if (headers.size() > 0) {

					if (!preview)
						DotHibernate.startTransaction();

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
								importLine(csvLine, st, preview, lineNumber, isIdentifierKeyField, isMultilingual, user, results,language,publishContent);
							} else {								
								dotCMSLanguage = langAPI.getLanguage(csvLine[languageFieldPosition], csvLine[countryFieldPosition]);
								
								if (0 < dotCMSLanguage.getId()) {
									importLine(csvLine, st, preview, lineNumber, isIdentifierKeyField,isMultilingual, user, results,dotCMSLanguage.getId(),publishContent);
								} else {
									results.get("errors").add("Line #" + lineNumber + " Locale not found for languageCode='" + csvLine[languageFieldPosition] + "' countryCode='" + csvLine[countryFieldPosition] + "'");
									errors++;
								}
							}

							if (!preview && (lineNumber % commitGranularity == 0)) {
								DotHibernate.commitTransaction();
								DotHibernate.startTransaction();
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
						DotHibernate.commitTransaction();
						conAPI.reindex(st);	
					}

					results.get("messages").add(lines + " lines of data were read.");
					if (errors > 0)
						results.get("errors").add(errors + " input lines had errors.");

					if(preview && choosenKeyField.length() > 1)
						results.get("messages").add("Fields selected as key: "+choosenKeyField.substring(1).toString()+".");

					if (newContentCounter > 0)
						results.get("messages").add("Approximately " + (newContentCounter) + " new content will be created.");
					if (contentToUpdateCounter > 0)
						results.get("messages").add("Approximately " + (contentUpdatedDuplicated) + " old content will be updated.");

					results.get("results").add(contentCreated + " new \"" + st.getName() + "\" were created.");
					results.get("results").add(contentUpdatedDuplicated + " \"" + st.getName() + "\" contentlets updated corresponding to "+contentUpdated+" repeated contents based on the key provided");

					if (errors > 0)
						results.get("results").add(errors + " contentlets were ignored due to invalid information");

				} else {
					results.get("errors").add("No headers found on the file, nothing will be imported.");
				}
			}
		} catch (Exception e) {
			Logger.error(ImportContentletsAction.class,e.getMessage());

		} finally {
			if (!preview && DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)) {
				ReindexThread.getInstance().unpause();
			}
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {

				}
		}	
		
		
		Logger.info(this, lines + " lines read correctly. " + errors + " errors found.");

		return results;
	}
	
	private void importHeaders(String[] headerLine, 
			                   Structure structure, 
			                   long[] keyFieldsInodes, 
			                   boolean isIdentifierKeyField,							   
			                   boolean preview, 
			                   boolean isMultilingual, 
			                   User user, 
			                   HashMap<String,List<String>> results) 
	throws DotRuntimeException {

		int importableFields = 0;

		//Importing headers and storing them in a hashmap to be reused later in the whole import process
		List<Field> fields = FieldsCache.getFieldsByStructureInode(structure.getInode());
		for (int i = 0; i < headerLine.length; i++) {
			boolean found = false;
			String header = headerLine[i].replaceAll("'", "");
			for (Field field : fields) {
				if (field.getFieldName().equalsIgnoreCase(header)) {
					if (field.getFieldType().equals(Field.FieldType.BUTTON.toString())){
						found = true;
						results.get("warnings").add("Header: \"" + header + "\" matches a field of type button, this column of data will be ignored.");
					}
					else if (field.getFieldType().equals(Field.FieldType.FILE.toString())){
						found = true;
						results.get("warnings").add("Header: \"" + header + "\" matches a field of type file, this column of data will be ignored.");
					}
					else if (field.getFieldType().equals(Field.FieldType.IMAGE.toString())){
						found = true;
						results.get("warnings").add("Header: \"" + header + "\" matches a field of type image, this column of data will be ignored.");
					}
					else if (field.getFieldType().equals(Field.FieldType.LINE_DIVIDER.toString())){
						found = true;
						results.get("warnings").add("Header: \"" + header + "\" matches a field of type line divider, this column of data will be ignored.");
					}
					else if (field.getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString())){
						found = true;
						results.get("warnings").add("Header: \"" + header + "\" matches a field of type tab divider, this column of data will be ignored.");
					}
					else {
						found = true;
						headers.put(i, field);
						for (long fieldInode : keyFieldsInodes) {
							if (fieldInode == field.getInode())
								keyFields.put(i, field);
						}
						break;
					}
				}
			}
			//If is Identifier Field
			if (isIdentifierKeyField && identifierFieldName.equalsIgnoreCase(header)) 
			{				
				found = true;
				identifierFieldPosition = i;
			} 
			//If is Language Field
			else if (isMultilingual && languageCodeHeader.equalsIgnoreCase(header)) 
			{
				found = true;
				languageFieldPosition = i;
			}
			//If is Country Field
			else if (isMultilingual && countryCodeHeader.equalsIgnoreCase(header)) 
			{
				found = true;
				countryFieldPosition = i;
			}
			else if (!found) 
			{
				results.get("warnings").add("Header: \"" + header + "\" doesn't match any structure field, this column of data will be ignored.");
			}
		}

		for (Field field : fields) {
			if (isImportableField(field)){
				importableFields++;
			}
		}

		//Checking keyField selected by the user against the headers
		for (long keyField : keyFieldsInodes) {
			boolean found = false;
			for (Field headerField : headers.values()) {
				if (headerField.getInode() == keyField) {
					found = true;
					break;
				}
			}
			if (!found) {
				results.get("errors").add("Key field: \"" + FieldFactory.getFieldByInode(keyField).getFieldName() + "\" chosen doesn't match any of the headers found in the file.");
			}
		}

		if (keyFieldsInodes.length == 0)
			results.get("warnings").add("No key fields were chosen, it could give to you duplicated content.");

		//Adding some messages to the results
		if (importableFields == headers.size()) {
			results.get("messages").add("All the " + headers.size() + " headers found on the file matches all the structure fields.");
		} else {
			if (headers.size() > 0)
				results.get("messages").add(headers.size() + " headers found on the file matches the structure fields.");
			else
				results.get("messages").add("No headers found on the file that match any of the structure fields. The process will not import anything.");
			results.get("warnings").add("Not all the structure fields were matched against the file headers. Some content fields could be left empty.");
		}
	}
	
	private void importLine(String[] line, 
							Structure structure, 
							boolean preview,
							int lineNumber,
							boolean isIdentifierKeyField,
							boolean isMultilingual, 
							User user, 
							HashMap<String, List<String>> results, 							 
							long language,
							boolean publishContent)
	throws DotRuntimeException {
		try {			
			//Building a values HashMap based on the headers/columns position			
			HashMap<Integer, Object> values = new HashMap<Integer, Object>();
			Set<Category> categories = new HashSet<Category> ();
			String []categoryUniqueKey = new String[25];
			String []categorykeyFields = new String[25];
			int categoryCounter = 0;
			int j=0;
			int i=0;

			for (Integer column : headers.keySet()) {
				Field field = headers.get(column);
				if (line.length < column) {
					throw new DotRuntimeException("Incomplete line found, the line #" + lineNumber + 
					" doesn't contain all the required columns.");
				}
				String value = line[column];
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
							categoryUniqueKey[categoryCounter] = value.toString();
							categoryCounter++;
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
				} else {
					valueObj = UtilMethods.escapeUnicodeCharsForHTML(value);
				}
				values.put(column, valueObj);
			}
						
			//Searching contentlets to be updated by key fields
			List<Contentlet> contentlets = new ArrayList<Contentlet>();
			HashMap<String,String> conditionMap = new HashMap<String,String>();
			String conditionValues = "";
			StringBuffer buffy = new StringBuffer();
			
			buffy.append("+structureInode:" + structure.getInode() + " +working:true +deleted:false +languageId:" + language);

			if (keyFields.size() > 0 || ((isIdentifierKeyField) && UtilMethods.isSet(line[identifierFieldPosition]))) {
				
				if ((isIdentifierKeyField) && UtilMethods.isSet(line[identifierFieldPosition])) {
					buffy.append(" +identifier:" + line[identifierFieldPosition]);
					conditionValues += line[identifierFieldPosition] + "-";
				}

				for (Integer column : keyFields.keySet()) {
					Field field = keyFields.get(column);
					Object value = values.get(column);
					String text = null;
					if (value instanceof Date || value instanceof Timestamp) {
						SimpleDateFormat formatter = null;
						if(field.getFieldType().equals(Field.FieldType.DATE.toString())
								|| field.getFieldType().equals(Field.FieldType.DATE_TIME.toString()))
						{
							text = LuceneUtils.toLuceneDate((Date)value);
						} else if(field.getFieldType().equals(Field.FieldType.TIME.toString())) {
							text =  LuceneUtils.toLuceneTime((Date)value);
						} else {
							formatter = new SimpleDateFormat();
							text = formatter.format(value);
							Logger.warn(getClass(),"importLine: field's date format is undetermined.");
						}                              
					} else {
						text = value.toString();
					}
					if(!UtilMethods.isSet(text)){
						throw new DotRuntimeException("Line #" + lineNumber + " key field "+field.getFieldName()+" is required since it was defined as a key\n");
					}else{  
						if (field.getFieldType().equals(Field.FieldType.CATEGORY.toString()) || field.getFieldType().equals(Field.FieldType.CATEGORIES_TAB.toString())) {
							CategoryAPI catAPI=APILocator.getCategoryAPI();
							for (j=0;j<categoryCounter;j++) {
								if(value.toString().equals(categoryUniqueKey[j])) {
									Category cat = catAPI.findByKey(value.toString(), user, false);
									if (buffy.indexOf((" +c" + cat.getInode() + "c:on"))==-1)
										buffy.append(" +c" + cat.getInode() + "c:on");
								}
							}
							categorykeyFields[i]=value.toString();
							i++;
						}
						else {
							buffy.append(" +" + field.getFieldContentlet() + ":" + escapeLuceneSpecialCharacter(text));
						}
						conditionValues += conditionValues + value + "-";
					}

					if(choosenKeyField.indexOf(field.getFieldName()) == -1){
						choosenKeyField.append(", "+field.getFieldName());
					}
				}
				j=0;
				i=0;
				int matchingFields=0;
				List<Contentlet> cons = conAPI.checkout(buffy.toString(), user, true); 
				for (Contentlet con : cons) 
				{	
					if ((isIdentifierKeyField) && UtilMethods.isSet(line[identifierFieldPosition])) {
						if (con.getIdentifier() == Long.parseLong(line[identifierFieldPosition])) {
							contentlets.add(con);
							continue;
						}
					}
					matchingFields = 0;
					for (Integer column : keyFields.keySet()) {
						Field field = keyFields.get(column);
						Object value = values.get(column);
						if (field.getFieldType().equals("category")){
							CategoryAPI catAPI=APILocator.getCategoryAPI();
							Category cat = catAPI.findByKey(value.toString(), user, false);
							if(cat.getKey().equals(categorykeyFields[j]))
								matchingFields++;
							j++;
							
						}	
						else {
							if(conAPI.getFieldValue(con, field).toString().equalsIgnoreCase(value.toString()))
								matchingFields++;
						}
					}
					if(matchingFields==keyFields.size())
						contentlets.add(con);
				}
			}


			//Creating/updating content
			boolean isNew = false;
			if (contentlets.size() == 0) {
				newContentCounter++;
				isNew = true;
				//if (!preview) {
				Contentlet newCont = new Contentlet();
				boolean setLive = (publishContent ? true : false);
				newCont.setLive(setLive);				
					
				newCont.setWorking(true);
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
						long lastIdentifier = 0 ;
						isNew = true;
						for (Contentlet contentlet: contentlets) {
							if (contentlet.getIdentifier() != lastIdentifier) {
								newContentCounter++;
								Contentlet newCont = new Contentlet();
								newCont.setIdentifier(contentlet.getIdentifier());
								boolean setLive = (publishContent ? true : false);
								newCont.setLive(setLive);
								newCont.setWorking(true);
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
						contentToUpdateCounter += contentlets.size();
						if (preview)
							keyContentUpdated.add(conditionValues);
					}
					if (contentlets.size() > 0) {
						results.get("warnings").add(
								"Line #" + lineNumber + ". The key fields chosen match more than one content, in this case: "  
								+ " matches: " + contentlets.size() + " different content(s)");
					}
				}
			}
			
			
			
			for (Contentlet cont : contentlets) 
			{
				//Fill the new contentlet with the data
				for (Integer column : headers.keySet()) {
					Field field = headers.get(column);
					Object value = values.get(column);
					conAPI.setContentletProperty(cont, field, value);
					if (field.getFieldType().equals(Field.FieldType.TAG.toString()) &&
							value instanceof String) {
						String[] tags = ((String)value).split(",");
						for (String tag : tags) {
							TagFactory.addTagInode((String)tag.trim(), Long.toString(cont.getInode()));
						}
					}
				}
				
				boolean setLive = (publishContent ? true : false);
				cont.setLive(setLive);

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
					cont = conAPI.checkin(cont, new ArrayList<Category>(categories), structurePermissions, user, false);					
				}

				if (isNew){
					contentCreated++;
				}else{
					if (conditionValues.equals("") || !keyContentUpdated.contains(conditionValues)) {
						contentUpdated++;
						contentUpdatedDuplicated++;
						keyContentUpdated.add(conditionValues);
					}else{
						contentUpdatedDuplicated++;
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

	public static boolean isImportableField(Field field) {
		return !(field.getFieldType().equals(Field.FieldType.IMAGE.toString()) ||
				field.getFieldType().equals(Field.FieldType.FILE.toString()) ||
				field.getFieldType().equals(Field.FieldType.BUTTON.toString()) ||
				field.getFieldType().equals(Field.FieldType.LINE_DIVIDER.toString()) ||
				field.getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString()));
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

	public int getLanguageFieldPosition() {
		return languageFieldPosition;
	}

	public int getCountryFieldPosition() {
		return countryFieldPosition;
	}
	
	//Utility method
	/*private String lineToString(String[] line) {
		StringBuffer returnSt = new StringBuffer();
		for (String st : line) {
			returnSt.append(st + ",");
		}
		return returnSt.toString();
	}*/
}
