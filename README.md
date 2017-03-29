plugin-Automated-Content-Import-Export
======================================

This plugin allows the configuration of a quartz job to import and export content automatically.

The plugin contains a portlet that shows the queue of content to be imported and exported in separated list. 
Also the portlet allows you to add, edit and remove import/export quartz jobs.

This version include two new options in the import process:

1. Update existing contents without generating new versions.

2. Delete all the existing contents in the structure before running the import.


Important:
=========

Once you get the code from github, you will need to rename the folder where this plugin will be installed in your /plugins directory to: org.dotcms.plugins.contentImporter


Configuration:
==============
This plugin allows you to configure the following properties on the conf/plugin.properties file:

1. The portlet.role is the role that a user will need to have in order to add and remove content import tasks in back-end. It's recommended to keep the default value.
portlet.role=Content Importer

2. the processedFilePath, indicates where the files are going to be moved once they have been processed.
processedFilePath=/DotCMS/Uploads/Processed

3. the logFile, indicates where is saved the log file of each files after they have been processed.
logFile=/DotCMS/Content_Importer_Logs

4. the exportedFilePath, indicates where the exported files are going to be created (this is the default value for the export).
exportedFilePath=/DotCMS/exported


To create/edit/delete an export task 
------------------------------------

Go to the dotCMS tab where you included the content import/export portlet and select the option you need to execute:

If you are going to generate a new task, you need to enter:

1. The task name.

2. The task description.

3. The task execution properties (when and for how long the task should be executed). Here you could use a cron expression or use the configuration properties.

4. The Structure you will be exporting.

5. The Language of the content to be exported (multilangue, or just in a particular language).

6. The Fields to export from the structure. If no fields are selected, all the fields in the structure will be exported. If some fields are selected, just those  and the system fields mentioned next will be exported to the csv file:identifier, language and country.

7. The File Path where the csv file(s) zip file will be located. 
7.a. File in the server's filesystem .- Here you should specify an existing folder in the server running dotCMS  where all the compressed csv file will be exported.
7.b. Content File .- In this case, the exported file will be stored as a fileasset in your dotCMS instance. You will need to specify the File Content Type to be assigned to the file and the combiantion of host (or site) plus  folder where the file will be stored.

8. Overwrite export file?. When checked, every execution of the job will overwrite the previously generated file. If not, on every job execution will create a new file.

9. The report email. Use this parameter if you want to to receive an email notification with the compressed csv file every time the content export runs. This value can be a comma separated list of emails if more than one person should receive the exported file.

10. The CSV Separator Delimiter. This parameter indicates the character that is going to be used to separate the values of the fields in each row in the csv file.

11. The CSV Text Delimiter. This is if you use a " or ' or other symbol to indicate when a text end.


To create/edit/delete an import task 
------------------------------------

Go to the dotCMS tab where you included the content import/export portlet and select the option you need to execute:

If you are going to generate a new task, you need to enter:

1. The task name.

2. The task description.

3. The task execution properties (when and for how long the task should be executed). Here you could use a cron expression or use the configuration properties.

4. The Structure where the content will be imported.

5. The Language of the content to be imported (multilangue, or just in a particular language).

6. The Key Fields of the structure, in case you want the import to update existing content.

7. The source from where the content to be imported can be loaded:
7.a File in the server's filesystem .- In this case, the CSV file will be read as a file in the filesystem. You will need to enter the path of the folder from where any file with extension .csv will be read.
7.b Content File .- In this case, the CSV will be a file asset read as content from your dotCMS instance. You will need to enter the File Content Type associated to the file. You can also define a lucene query that will allow you to filter the files under the specified Content Type that will be pulled and loaded during the execution of the import.

8. The report email. Use this parameter if you want to to receive a notification every time the content import runs. The email will indicate if the process finishes sucessfully or if there any errors importing the file.

9. The CSV Separator Delimiter. This parameter indicates the character that is used to separate the values of the fields in each row in the csv file.

10. The CSV Text Delimiter. This is if you use a " or ' or other symbol to indicate when a text end.

11. The Publish property. Check it only if you want the process automatically publish the content that will be imported.

12. The Override existing content version? property. Check it, if you want that the import process don't generate a new version of the existing content.

13. The Delete structure contents? property. Check it, if you want to delete all the contents in the structure before doing the import.

