package dh.sunicon.datamodel;

import org.json.JSONException;
import org.json.JSONObject;


public class EnumValue extends BaseEntity
{
	private long unitId_;
	private String value_;
	
	public EnumValue(DatabaseHelper dbHelper, long unitId, long id, String value)
	{
		super(dbHelper, id);
		unitId_ = unitId;
		value_ = value;
	}
	
	public EnumValue(DatabaseHelper dbHelper, JSONObject json) throws JSONException{
		this(dbHelper, json.getLong("unitId"), json.getLong("id"), json.optString("value"));
	}

	public long getUnitId()
	{
		return unitId_;
	}

	public String getValue()
	{
		return value_;
	}
	
	@Override
	public String toString()
	{
		return value_;
	}
	
	public JSONObject serialize() throws JSONException {
		JSONObject json = new JSONObject();
		
		json.put("id", getId());
		json.put("unitId", unitId_);
		json.put("value", value_);
		
		return json;
	}
}
