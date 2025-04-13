package at.peckventure;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

public enum LanguageManager
{
    INSTANCE;
    public JsonValue texts;
    private String langcode;




    public void setLangcode(String langcode)
    {
        this.langcode = langcode;
        texts = new JsonReader().parse(Gdx.files.internal("lang/" + langcode + ".json"));
    }

    public String getText(String key) {
        return texts.has(key) ? texts.getString(key) : key;
    }
}
