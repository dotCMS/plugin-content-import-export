package org.dotcms.plugins.contentImporter.quartz;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.dotcms.plugins.contentImporter.util.ContentletUtil;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.Mailer;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;

public class ContentImporterThread implements Job {
	private final static PluginAPI pluginAPI = APILocator.getPluginAPI();

	public void execute(JobExecutionContext context) throws JobExecutionException {
		JobDataMap properties = context.getMergedJobDataMap();

		long language = 0;
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
		String logPath = "";


		boolean haveFileSource = false;
		String fileAsset = null, fileAssetQuery = null;
		String filePath = null;
		try {
			logPath = pluginAPI.loadProperty("org.dotcms.plugins.contentImporter", "logFile");

			haveFileSource = UtilMethods.isSet(properties.get("haveFileSource")) && (Boolean) properties.get("haveFileSource");

			fileAsset = (String) properties.get("fileAsset");
			fileAssetQuery = (String) properties.get("fileAssetQuery");
			filePath = (String) properties.get("filePath");

		} catch (Exception e) {
		}		


		try {
			String structure = (String) properties.get("structure");

			String[] fields = {};
			if (UtilMethods.isSet(properties.get("fields"))) {
				String[] strFields = ((String) properties.get("fields")).split(",");
				List<String> longFields = new ArrayList<String>(strFields.length);
				for (String field: strFields) {
					longFields.add(field);
				}

				String[] tempArray = new String[longFields.size()];
				for (int pos = 0; pos < longFields.size(); ++pos) {
					tempArray[pos] = longFields.get(pos);
				}
				fields = tempArray;
			}


			String reportEmail = (String) properties.get("reportEmail");

			String csvSeparatorDelimiter = (String) properties.get("csvSeparatorDelimiter");
			if (!UtilMethods.isSet(csvSeparatorDelimiter))
				csvSeparatorDelimiter = ",";
			else
				csvSeparatorDelimiter = "" + csvSeparatorDelimiter.trim().charAt(0);

			String csvTextDelimiter = (String) properties.get("csvTextDelimiter");
			if (UtilMethods.isSet(csvTextDelimiter))
				csvTextDelimiter = "" + csvTextDelimiter.trim().charAt(0);

			boolean publishContent = new Boolean((String) properties.get("publishContent"));
			boolean deleteAllContent = new Boolean((String) properties.get("deleteAllContent"));
			boolean saveWithoutVersions = new Boolean((String) properties.get("saveWithoutVersions"));


			if (haveFileSource) {
				importFromContent(
					fileAsset, fileAssetQuery, logPath, reportEmail,
					structure, fields, language, isMultilanguage,
					csvSeparatorDelimiter, csvTextDelimiter,
					publishContent, deleteAllContent, saveWithoutVersions
				);
			} else {
				importFromFileSystem(
					filePath, logPath, reportEmail, 
					structure, fields, language, isMultilanguage,
					csvSeparatorDelimiter, csvTextDelimiter,
					publishContent, deleteAllContent, saveWithoutVersions
				);
			}

		} catch (Exception e1) {
			Logger.warn(this, e1.toString());                  
		}               
	}

	private void importFromContent(
		String fileAsset, String fileAssetQuery, String logPath, String reportEmail,
		String structure, String[] fields, long language, boolean isMultilanguage, String csvSeparatorDelimiter,
		String csvTextDelimiter, boolean publishContent, boolean deleteAllContent, boolean saveWithoutVersions
	) {
		HashMap<String, List<String>> results = createResults();

		try {
			String luceneQuery = "+structureName:"+ fileAsset +" ";
			
			if (UtilMethods.isSet(fileAssetQuery)) {
				luceneQuery += fileAssetQuery;
			} else {
				luceneQuery += "+"+ fileAsset +".fileName:*.csv +deleted:false  +live:true";
			}

			if (!isMultilanguage) {
				luceneQuery += " +languageId:"+ language;
			}

			List<Contentlet> hits = APILocator.getContentletAPI().search(
				luceneQuery, -1, 0, "modDate asc", APILocator.getUserAPI().getSystemUser(), false
			);

			for(Contentlet contentlet : hits) {

				String fileName = fileAsset;

				try {
					FileAsset fileAssetCont = APILocator.getFileAssetAPI().fromContentlet(contentlet);

					fileName = fileAssetCont.getFileName();
					int index = fileName.lastIndexOf(".");
					if (-1 < index)
						fileName = fileName.substring(0, index);

					try {

						HashMap<String, List<String>> currentResults = importInputStream(
							fileAssetCont.getFileInputStream(), csvTextDelimiter, csvSeparatorDelimiter,
							structure, fields, fileAssetCont.getLanguageId(), isMultilanguage,
							publishContent, deleteAllContent, saveWithoutVersions
						);

						if (currentResults != null) {
							results = currentResults;
						}						

					} finally {

						moveImportedAsset(
							contentlet, fileAssetCont,
							fileName + " - " + UtilMethods.dateToHTMLDate(new Date(), "yyyyMMddHHmmss") + ".csv.old"
						);

					}
				} catch (Exception e) {

					((List<String>) results.get("errors")).add("Exception: " + e.toString());
					Logger.error(ContentImporterThread.class, e.getMessage(),e);

				} finally {

					sendResults(results, reportEmail, fileName + " Import Results", logPath, fileName);
				}
			}

		} catch (Exception e) {
			((List<String>) results.get("errors")).add("Exception: " + e.toString());

			Logger.error(ContentImporterThread.class, e.getMessage(),e);

			sendResults(results, reportEmail, fileAsset + " Import Results", logPath, fileAsset);						
		}
	}

	// Mostly borrowed from com.dotmarketing.portlets.fileassets.business.FileAssetAPIImpl.renameFile(Contentlet, String, User, boolean)
	public void moveImportedAsset(Contentlet contentlet, FileAsset fileAssetCont, String newName) throws DotStateException, DotDataException, DotSecurityException, IOException {
		Identifier id = APILocator.getIdentifierAPI().find(contentlet);
		Host host = APILocator.getHostAPI().find(id.getHostId(), APILocator.getUserAPI().getSystemUser(), false);
		Folder folder = APILocator.getFolderAPI().findFolderByPath(id.getParentPath(), host, APILocator.getUserAPI().getSystemUser(), false);

		if(!APILocator.getFileAssetAPI().fileNameExists(host, folder, newName, id.getId())){			    
		    File oldFile = contentlet.getBinary(FileAssetAPI.BINARY_FIELD);
			File newFile = new File(oldFile.getPath().substring(0,oldFile.getPath().indexOf(oldFile.getName()))+newName);

			try {
				APILocator.getContentletIndexAPI().removeContentFromIndex(contentlet);

				FileUtils.copyFile(oldFile, newFile);
				contentlet.setInode(null);
				contentlet.setFolder(folder.getInode());
				contentlet.setBinary(FileAssetAPI.BINARY_FIELD, newFile);
				contentlet.setStringProperty(FileAssetAPI.TITLE_FIELD, newName);
				contentlet.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, newName);
				contentlet= APILocator.getContentletAPI().checkin(contentlet, APILocator.getUserAPI().getSystemUser(), false);

				APILocator.getContentletIndexAPI().addContentToIndex(contentlet);

				APILocator.getVersionableAPI().setLive(contentlet);

				RefreshMenus.deleteMenu(folder);
				CacheLocator.getNavToolCache().removeNav(folder.getHostId(), folder.getInode());
				CacheLocator.getIdentifierCache().removeFromCacheByVersionable(contentlet);

			} catch (Exception e) {
				Logger.error(this, "Unable to rename file asset to "+ newName + " for asset " + id.getId(), e);
				throw e;
			} finally {
				if (newFile != null) {
					FileUtils.deleteQuietly(newFile);
				}
			}
		}
	}

	private void importFromFileSystem(
		String filePath, String logPath, String reportEmail,
		String structure, String[] fields, long language, boolean isMultilanguage,
		String csvSeparatorDelimiter, String csvTextDelimiter,
		boolean publishContent, boolean deleteAllContent, boolean saveWithoutVersions
	) {
		HashMap<String, List<String>> results = createResults();

		String fileName = new File(filePath).getName();
		int index = fileName.lastIndexOf(".");
		if (-1 < index)
			fileName = fileName.substring(0, index);

		File tempfile = new File(filePath);
		List<File> filesList = new ArrayList<File>();
		if (!tempfile.exists()) {
			((List<String>) results.get("errors")).add("File: " + filePath + " doesn't exist.");

			sendResults(results, reportEmail, fileName + " Import Results", logPath, fileName);
		}else if(tempfile.isDirectory()){
			File[] files = tempfile.listFiles();
			for(File  f : files){
				if(f.getName().toLowerCase().endsWith(".csv")){
					filesList.add(f);
				}
			}
		} else {
			filesList.add(tempfile);
		}
		Collections.sort(filesList);
		for(File file : filesList){
			if (!file.exists()) {
				((List<String>) results.get("errors")).add("File: " + filePath + " doesn't exist.");

				sendResults(results, reportEmail, fileName + " Import Results", logPath, fileName);
			} else if (!file.isFile()) {
				((List<String>) results.get("errors")).add(filePath + " isn't a file.");

				sendResults(results, reportEmail, fileName + " Import Results", logPath, fileName);
			} else if (!file.canRead()) {
				((List<String>) results.get("errors")).add("File: " + filePath + " can't be readed.");

				sendResults(results, reportEmail, fileName + " Import Results", logPath, fileName);
			} else {
				try {
					File renameFile = new File(file.getPath() + ".lock");
					file.renameTo(renameFile);
					file = renameFile;

					HashMap<String, List<String>> currentResults = importInputStream(
						new FileInputStream(file), csvTextDelimiter, csvSeparatorDelimiter,
						structure, fields, language, isMultilanguage,
						publishContent, deleteAllContent, saveWithoutVersions
					);
					if (currentResults != null) {
						results = currentResults;
					}
				} catch (Exception e) {
					((List<String>) results.get("errors")).add("Exception: " + e.toString());
					Logger.error(ContentImporterThread.class, e.getMessage(),e);
				} finally {
					moveImportedFile(file.getPath());
					sendResults(results, reportEmail, fileName + " Import Results", logPath, fileName);						
				}
			}
		}
	}

	private void moveImportedFile(String filePath) {
		try {
			String processedFilePath = pluginAPI.loadProperty("org.dotcms.plugins.contentImporter", "processedFilePath");
			File processedFilePathDir = new File(processedFilePath);
			if (!processedFilePathDir.exists())
				processedFilePathDir.mkdirs();

			File file = new File(filePath);
			String fileName = file.getName();
			String fileNameNoExt = "";
			String fileExt = "";
			int index = fileName.lastIndexOf(".");
			if (-1 < index) {
				fileNameNoExt = fileName.substring(0, index);
				fileExt = fileName.substring(index + 1);
			}

			file.renameTo(new File(processedFilePathDir.getPath() + File.separator + fileNameNoExt + " - " + UtilMethods.dateToHTMLDate(new Date(), "yyyyMMddHHmmss") + "." + fileExt));
		} catch (Exception e) {
			Logger.info(this, e.toString());
		}
	}

	private HashMap<String, List<String>> importInputStream(
		InputStream inputStream, String csvTextDelimiter, String csvSeparatorDelimiter,
		String structure, String[] fields, long language, boolean isMultilanguage,
		boolean publishContent, boolean deleteAllContent, boolean saveWithoutVersions
	) throws IOException, DotDataException, DotSecurityException {
		Reader reader = null;
		CsvReader csvreader = null;

		try {
			reader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
			csvreader = new CsvReader(reader, csvSeparatorDelimiter.charAt(0));

			if (UtilMethods.isSet(csvTextDelimiter))
				csvreader.setTextQualifier(csvTextDelimiter.charAt(0));

			csvreader.setSafetySwitch(false);

			User user = APILocator.getUserAPI().getSystemUser();

			if (csvreader.readHeaders()) 
			{
				ContentletUtil contentletUtil = new ContentletUtil(reader, csvreader);
				if(deleteAllContent){
					contentletUtil.deleteAllContent(structure, user);
				}
				return contentletUtil.importFile(structure, fields, false, user, isMultilanguage, language,publishContent,saveWithoutVersions);
			}
			return null;
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception e) {
				}
			}

			if (csvreader != null) {
				try {
					csvreader.close();
				} catch (Exception e) {
				}
			}
		}
	}

	private HashMap<String, List<String>> createResults() {
		HashMap<String, List<String>> results = new HashMap<String, List<String>>();

		results.put("warnings", new ArrayList<String>());
		results.put("errors", new ArrayList<String>());
		results.put("messages", new ArrayList<String>());
		results.put("results", new ArrayList<String>());

		return results;
	}

	private void sendResults(HashMap<String, List<String>> results, String reportEmail, String subject,String logPath, String fileName) {
		StringBuilder message = new StringBuilder(1024);
		message.ensureCapacity(256);

		List<String> messages = ((List<String>) results.get("errors"));
		if ((messages != null) && (0 < messages.size())) {
			message.append("\nError Found:\n\n");
			Logger.info(ContentImporterThread.class,"\nError Found:\n");

			for (String tempMsg: messages) {
				message.append(tempMsg + "\n");
				Logger.info(ContentImporterThread.class,tempMsg);
			}
		}

		messages = ((List<String>) results.get("warnings"));
		if ((messages != null) && (0 < messages.size())) {
			message.append("\nWarnings Found:\n\n");
			Logger.info(ContentImporterThread.class,"\nWarnings Found:\n");

			for (String tempMsg: messages) {
				message.append(tempMsg + "\n");
				Logger.info(ContentImporterThread.class,tempMsg);
			}
		}

		messages = ((List<String>) results.get("results"));
		if ((messages != null) && (0 < messages.size())) {
			message.append("\nResults:\n\n");
			Logger.info(ContentImporterThread.class,"\nResults:\n");

			for (String tempMsg: messages) {
				message.append(tempMsg + "\n");
				Logger.info(ContentImporterThread.class,tempMsg);
			}
		}

		Company company = PublicCompanyFactory.getDefaultCompany();

		contentImporterLogger(logPath, fileName,message.toString());
		if(UtilMethods.isSet(reportEmail)){
			Mailer m = new Mailer();
			m.setToEmail(reportEmail);
			m.setFromEmail(company.getEmailAddress());
			m.setCc(null);
			m.setBcc(null);
			m.setSubject(subject);
			m.setTextBody(message.toString());

			if (!m.sendMessage()) {
				Logger.info(ContentImporterThread.class,"Email couldn't be sent.");
			}
		}
	}

	private void contentImporterLogger(String filePath, String fileName, String message) {
		BufferedWriter out = null;
		try {
			File folderPath = new File(filePath);
			if (!folderPath.exists()) {
				folderPath.mkdirs();
			}

			File outputFile = new File(folderPath.getPath() + File.separator + fileName + " - " + UtilMethods.dateToHTMLDate(new Date(), "yyyyMMddHHmmss") + ".txt");
			if (!outputFile.exists())
				outputFile.createNewFile();

			out = new BufferedWriter(new FileWriter(outputFile));
			out.write(message);

		} catch (Exception e1) {
			Logger.error(this, e1.toString());
		} finally{	
			if (out != null) {
				try {
					out.close();
				} catch (Exception e2) {
				}
			}
		}
	}
}