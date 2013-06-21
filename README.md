Configuration:

This plugin allows you to configure the following properties on the conf/plugin.properties file:

1. The portlet.role is the role that a user will need to have in order to add and remove content import tasks in back-end. It's recommended to kepp the default value.

portlet.role=Content Importer

2. the processedFilePath, indicates where the files are going to be moved once they have been processed

processedFilePath=/DotCMS/Uploads/Processed

3. the logFile, indicates where is saved the log file of each files after they have been processed

logFile=/DotCMS/Content_Importer_Logs

Add Import Task
To create/edit/delete an import task, go to the dotCMS tab where you included the content import portlet and select the option you need execute

If you are going to generate a new task, you need to ENTER:

1. The task name.

2. The task description.

3. The task execution properties (when and for how long the task should be executed).

4. The Structure where the content will be imported.

5. The Language of the content to be imported (multilangue, or just in a particular language).

6. The Key Fields of the structure, in case you want the import to update existing content.

7. The File Paht where the csv file(s) is/are located. If you specify a folder, all the csv files in it will be imported.

8. The report email. Use this parameter if you want to to receive a notification every time the content import runs. The email will indicate if the process finishes sucessfully or if there any errors importing the file.

9. The CSV Separator Delimiter. This parameter indicates the character that is used to separate the values of the fields in each row in the csv file.

10. The CSV Text Delimiter, this is if you use a " or ' or other symbol to indicate when a text end.

11.  The Publish property. Check it only if you want the process automatically publish the content that will be imported.