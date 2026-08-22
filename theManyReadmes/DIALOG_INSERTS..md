this is a jsonArray that can be used to heavily modify a given string. each line of the jsonArray is a jsonObject with the following data:
* "replaced": String. this is the data in the line that is replaced
* "dialogValue": [DIALOG_VALUES.md](https://github.com/Alaricdragon/Starlords_Temp/tree/master/theManyReadmes/DIALOG_VALUES.md) insets the outputed value into the line.
* "memory": String. inserts the inputted memory data into the line. only takes memory that are strings
* "lordMemory": String. inserts the inputted lord memory data into the line. only takes memory that are strings.
* "DialogData": String. inserts the inputted dialog data into the line. only takes data that are strings
* "customDialogInsert" jsonObject. were each item is "customDialogID": jsonObject. "customDialogID" should match a created 'DialogInsert_custom'.
    * to use this you must do the following:
    * 1) you must create a class that overrides DialogInsert_custom.
    * 2) override the 'getInsertedData' function. within this class, you can run whatever it is you want this insert to do.
        * please keep in mind, your class with have its 'json' variable set whenever it attempts to run. this is the jsonObject you fed your insert.
    * 3) create an instance of your class (new DialogInsert_custom(String id)). when created, your class will automatically be added to the list of possible custom insets. please keep in mind, only one item with a given ID can exist at a time. so I addive you use something like "modID_whatThisDoes" were 'modID' is your mods ID, and 'whatThisDoes' is... whatever you deside. this should prevent you from overriding someones else custom insert.
    * 4) in customDialogInsert{} add in your insert as a jsonObject. the items id should match your classes id, and the jsonObject you input will be the one your class will be given whenever it is ran.
