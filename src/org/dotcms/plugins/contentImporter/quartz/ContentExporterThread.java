package org.dotcms.plugins.contentImporter.quartz;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.ContentTypeCache;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.Mailer;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;

/**
 * This class manage the quartz Export contentlet job process
 * @author Oswaldo
 *
 */
public class ContentExporterThread implements Job {
	private final static PluginAPI pluginAPI = APILocator.getPluginAPI();
	private ContentletAPI conAPI =APILocator.getContentletAPI();
	private CategoryAPI catAPI =APILocator.getCategoryAPI();
	private HostAPI hostAPI = APILocator.getHostAPI();

	public void execute(JobExecutionContext context) throws JobExecutionException {
		try {
			Logger.info(ContentExporterThread.class, "Beginning Export Content process");
			JobDataMap properties = context.getMergedJobDataMap();
			long language = -1;
			boolean isMultilanguage = false;
			try {
				language = Long.parseLong(properties.getString("language"));

				if(language == -1)
				{
					isMultilanguage = true;
				}
			}catch(Exception e4){
				language = APILocator.getLanguageAPI().getDefaultLanguage().getId();
			}
			boolean overWriteFile = (Boolean) properties.get("overWriteFile");
			String filePath = (String) properties.get("filePath");
			if(!UtilMethods.isSet(filePath)){
				filePath = pluginAPI.loadProperty("org.dotcms.plugins.contentImporter", "exportedFilePath");
			}

			//String exportlogFile = pluginAPI.loadProperty("org.dotcms.plugins.contentImporter", "exportlogFile");

			String structureId = (String) properties.get("structure");
			Structure structure = CacheLocator.getContentTypeCache().getStructureByInode(structureId);
			List<Field> fields = new ArrayList<Field>();
			if (UtilMethods.isSet(properties.get("fields"))) {
				String[] strFields = ((String) properties.get("fields")).split(",");
				for (String field: strFields) {
					fields.add(structure.getFieldVar(field));
				}
			}

			String reportEmail = (String) properties.get("reportEmail");

			String csvSeparatorDelimiter = (String) properties.get("csvSeparatorDelimiter");
			if (!UtilMethods.isSet(csvSeparatorDelimiter))
				csvSeparatorDelimiter = ",";
			else
				csvSeparatorDelimiter = "" + csvSeparatorDelimiter.trim().charAt(0);

			String csvTextDelimiter = (String) properties.get("csvTextDelimiter");
			if (UtilMethods.isSet(csvTextDelimiter)){
				csvTextDelimiter = "" + csvTextDelimiter.trim().charAt(0);
			}else{
				csvTextDelimiter="\"";
			}

			String userId = (String) properties.get("userId");
			downloadToExcel(structure,fields, language, isMultilanguage, csvTextDelimiter, csvSeparatorDelimiter, filePath,overWriteFile, reportEmail,userId);
			Logger.info(ContentExporterThread.class, "Finished Export Content process");

		} catch (Exception e1) {
			Logger.error(ContentExporterThread.class, e1.getMessage(),e1);                  
		}               
	}	

	/**
	 * Generated the excel file and then compressed
	 * @param st
	 * @param fields
	 * @param language
	 * @param isMultilanguage
	 * @param csvTextDelimiter
	 * @param csvSeparatorDelimiter
	 * @param filePath
	 * @param reportEmail
	 * @param userId
	 * @throws DotSecurityException
	 */
	private void downloadToExcel(Structure st, List<Field> fields,long language, boolean isMultilanguage, String csvTextDelimiter, String csvSeparatorDelimiter, String filePath, boolean overWriteFile, String reportEmail, String userId) throws DotSecurityException{
		PrintWriter pr = null;
		try {
			User user = APILocator.getUserAPI().loadUserById(userId, APILocator.getUserAPI().getSystemUser(), false);			
			String query ="+structureName:"+st.getVelocityVarName()+" +working:true +deleted:false";
			if(!isMultilanguage){
				query =query +" +languageId:"+language;
			}
			List<ContentletSearch> contentletsReducedList = conAPI.searchIndex(query, 0, 0, "modDate asc", user, false);

			String fileName = st.getVelocityVarName();
			if(!overWriteFile){
				fileName=fileName+"_"+UtilMethods.dateToHTMLDate(new Date(), "MMddyyyyHHmmss");
			}
			fileName=fileName+".csv";
			
			File fileDirectory = new File(filePath);
			if(!fileDirectory.exists()){
				fileDirectory.mkdir();
			}
			File file = new File(fileDirectory.getPath()+File.separator+fileName);
			pr = new PrintWriter(file);

			List<Field> stFields = FieldsCache.getFieldsByStructureInode(st.getInode());
			pr.print(csvTextDelimiter+"Identifier"+csvTextDelimiter);		
			pr.print(csvSeparatorDelimiter+csvTextDelimiter+"languageCode"+csvTextDelimiter);
			pr.print(csvSeparatorDelimiter+csvTextDelimiter+"countryCode"+csvTextDelimiter);
			for (Field f : stFields) {
				//we cannot export fields of these types
				if (f.getFieldType().equals(Field.FieldType.BUTTON.toString()) || 						
						f.getFieldType().equals(Field.FieldType.LINE_DIVIDER.toString()) ||
						f.getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString())){
					continue;
				}else if(f.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())){
					if(fields.size()== 0 || fields.contains(f)){
						pr.print(csvSeparatorDelimiter+csvTextDelimiter+(f.getFieldName().contains(",")?f.getFieldName().replaceAll(",", "&#44;"):f.getFieldName())+csvTextDelimiter);
					}
				}else if(f.getFieldType().equals(Field.FieldType.FILE.toString()) ||
						f.getFieldType().equals(Field.FieldType.IMAGE.toString()) ||
						f.getFieldType().equals(Field.FieldType.BINARY.toString())){
					if(fields.size()== 0 || fields.contains(f)){
						pr.print(csvSeparatorDelimiter+csvTextDelimiter+(f.getFieldName().contains(",")?f.getFieldName().replaceAll(",", "&#44;"):f.getFieldName())+csvTextDelimiter);
						pr.print(csvSeparatorDelimiter+csvTextDelimiter+(f.getFieldName().contains(",")?f.getFieldName().replaceAll(",", "&#44;"):f.getFieldName())+" (Path)"+csvTextDelimiter);
					}
				}else{
					if(fields.size()== 0 || fields.contains(f)){
						pr.print(csvSeparatorDelimiter+csvTextDelimiter+(f.getFieldName().contains(",")?f.getFieldName().replaceAll(",", "&#44;"):f.getFieldName())+csvTextDelimiter);
					}
				}
			}

			pr.print("\r\n");
			Logger.info(ContentExporterThread.class, "Export Content processing "+contentletsReducedList.size()+" Contentlet(s)");
			for(ContentletSearch cont :  contentletsReducedList){
				Contentlet content = conAPI.find(cont.getInode(), user, false);
				List<Category> catList = (List<Category>) catAPI.getParents(content, user, false);
				pr.print(csvTextDelimiter+content.getIdentifier()+csvTextDelimiter);
				Language lang =APILocator.getLanguageAPI().getLanguage(content.getLanguageId());
				pr.print(csvSeparatorDelimiter+csvTextDelimiter +lang.getLanguageCode()+csvTextDelimiter);
				pr.print(csvSeparatorDelimiter+csvTextDelimiter+lang.getCountryCode()+csvTextDelimiter);

				for (Field f : stFields) {
					if(fields.size()== 0 || fields.contains(f)){
						try {
							//we cannot export fields of these types
							if (f.getFieldType().equals(Field.FieldType.BUTTON.toString()) || 
									f.getFieldType().equals(Field.FieldType.LINE_DIVIDER.toString()) ||
									f.getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString())){
								continue;
							}
							Object value = "";
							if(conAPI.getFieldValue(content,f) != null)  
								value = conAPI.getFieldValue(content,f);
							String text = "";
							String assetPath = "";
							String hostname = "";

							if(f.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())){
								if(UtilMethods.isSet(value)){
									try{
										text = value.toString();
										if(text.endsWith(",")){
											text = text.substring (0, text.length()-1);
										}
										Host host = APILocator.getHostAPI().find(text, user, false);
										hostname = host.getHostname();
									}catch(Exception e){
										Logger.error(ContentExporterThread.class, e.getMessage());
									}
								}
							}else if(f.getFieldType().equals(Field.FieldType.FILE.toString()) || f.getFieldType().equals(Field.FieldType.IMAGE.toString())){
								text = value.toString();
								if(UtilMethods.isSet(text)){
									try{
										Contentlet asset = conAPI.findContentletByIdentifier(text, false, (language==-1?APILocator.getLanguageAPI().getDefaultLanguage().getId():language), user, false);
										FileAsset fileAsset = APILocator.getFileAssetAPI().fromContentlet(asset);
										text = fileAsset.getFileName();
										if(text.endsWith(",")){
											text = text.substring (0, text.length()-1);
										}
										assetPath ="http://"+hostAPI.find(fileAsset.getHost(), user, false).getHostname()+fileAsset.getURI();
									}catch(Exception e){
										Logger.error(ContentExporterThread.class, e.getMessage());
									}
								}
							}else if(f.getFieldType().equals(Field.FieldType.BINARY.toString())){								
								File binaryfile = content.getBinary(f.getVelocityVarName()); 								
								if(UtilMethods.isSet(binaryfile)){
									try{
										text = binaryfile.getName();
										if(text.endsWith(",")){
											text = text.substring (0, text.length()-1);
										}
										assetPath = "http://"+hostAPI.find(content.getHost(), user, false).getHostname()+"/contentAsset/raw-data/"+content.getIdentifier()+"/"+ f.getVelocityVarName() + "/" + content.getInode();
									}catch(Exception e){
										Logger.error(ContentExporterThread.class, e.getMessage());
									}
								}
							}else if(f.getFieldType().equals(Field.FieldType.CATEGORY.toString())){

								Category category = catAPI.find(f.getValues(), user, false);
								List<Category> children = catList;
								List<Category> allChildren= catAPI.getAllChildren(category, user, false);


								if (children.size() >= 1 && catAPI.canUseCategory(category, user, false)) {
									//children = (List<Category>)CollectionUtils.retainAll(catList, children);
									for(Category cat : children){
										if(allChildren.contains(cat)){
											if(UtilMethods.isSet(cat.getKey())){
												text = text+csvSeparatorDelimiter+cat.getKey();
											}else{
												text = text+csvSeparatorDelimiter+cat.getCategoryName();
											}
										}
									}
								}
								if(UtilMethods.isSet(text)){
									text=text.substring(1);
								}
							}else{

								if (value instanceof Date || value instanceof Timestamp) {
									if(f.getFieldType().equals(Field.FieldType.DATE.toString())) {
										SimpleDateFormat formatter = new SimpleDateFormat (WebKeys.DateFormats.EXP_IMP_DATE);
										text = formatter.format(value);
									} else if(f.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {
										SimpleDateFormat formatter = new SimpleDateFormat (WebKeys.DateFormats.EXP_IMP_DATETIME);
										text = formatter.format(value);
									} else if(f.getFieldType().equals(Field.FieldType.TIME.toString())) {
										SimpleDateFormat formatter = new SimpleDateFormat (WebKeys.DateFormats.EXP_IMP_TIME);
										text = formatter.format(value);
									}                                    
								} else {
									text = value.toString();
									if(text.endsWith(",")){
										text = text.substring (0, text.length()-1);
									}
								}

							}
							//Windows carriage return conversion
							text = text.replaceAll("\r","");

							if(f.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())){
								pr.print(csvSeparatorDelimiter+csvTextDelimiter+text+csvTextDelimiter);
							}else if(f.getFieldType().equals(Field.FieldType.FILE.toString()) || f.getFieldType().equals(Field.FieldType.IMAGE.toString()) ||
									f.getFieldType().equals(Field.FieldType.BINARY.toString())){
								pr.print(csvSeparatorDelimiter+csvTextDelimiter+text+csvTextDelimiter);
								pr.print(csvSeparatorDelimiter+csvTextDelimiter+assetPath+csvTextDelimiter);
							}else if(text.contains(",") || text.contains("\n")) {
								//Double quotes replacing
								text = text.replaceAll("\"","\"\"");
								//pr.print(csvSeparatorDelimiter+"\""+text+"\"");
								pr.print(csvSeparatorDelimiter+csvTextDelimiter+text+csvTextDelimiter);
							} else{
								pr.print(csvSeparatorDelimiter+csvTextDelimiter+text+csvTextDelimiter);						
							}
						}catch(Exception e){
							pr.print(csvSeparatorDelimiter);
							Logger.error(this,e.getMessage(),e);
						}
					}
				}
				pr.print("\r\n");
			}

			pr.flush();
			pr.close();
			HibernateUtil.closeSession();

			/*Compressing file*/
			file = zipFile(file);
			if(UtilMethods.isSet(reportEmail)){
				sendExportReport(reportEmail,"Export process succeed for structure:"+st.getName(),"Export process succeed for structure:"+st.getName()+"<br/>See the file attached.", file);
			}

		}catch(Exception p){
			Logger.error(this,p.getMessage(),p);
			sendExportReport(reportEmail,"Error"," Error: "+p.getMessage(), null);
		}
	}

	/**
	 * Send report results
	 * @param reportEmail
	 * @param subject
	 * @param message
	 * @param file
	 */
	private void sendExportReport(String reportEmail,String subject, String message,File file){
		Company company = PublicCompanyFactory.getDefaultCompany();
		for(String email : reportEmail.split(",")){
			Mailer m = new Mailer();
			m.setFromEmail(company.getEmailAddress());
			m.setToEmail(email);
			m.setSubject(subject);
			m.setHTMLBody(message);
			if(UtilMethods.isSet(file)){
				m.addAttachment(file);
			}
			m.sendMessage();
		}
	}

	/**
	 * Compress the giving file into a zip file
	 * @param from
	 * @param to
	 * @return File
	 * @throws IOException
	 */
	private File zipFile(File file) throws IOException {
		String compressedfileName = file.getPath().substring(0,file.getPath().lastIndexOf("."))+".zip";
		File compressedFile = new File(compressedfileName);
		ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(compressedFile)));
		byte[] data = new byte[1000]; 		
		int count;
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
		out.putNextEntry(new ZipEntry(file.getName()));
		while((count = in.read(data,0,1000)) != -1){  
			out.write(data, 0, count);
		}
		in.close();
		out.flush();
		out.close();
		file.delete();
		return compressedFile;
	}


}
