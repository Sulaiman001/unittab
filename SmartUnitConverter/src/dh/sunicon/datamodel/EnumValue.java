package dh.sunicon.datamodel;


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

	public long getUnitId()
	{
		return unitId_;
	}

	public String getValue()
	{
		return value_;
	}
}
