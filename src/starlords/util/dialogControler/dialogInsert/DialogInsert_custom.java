package starlords.util.dialogControler.dialogInsert;


import org.json.JSONObject;

public class DialogInsert_custom extends DialogInsert_Base{
    public JSONObject json;
    public DialogInsert_custom(String key){
        DialogInsert_customList.inserts.put(key,this);
    }
}
