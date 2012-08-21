package dh.sunicon.datamodel;

public abstract class BaseEntity {
	
	private DatabaseHelper dbHelper;
	private long id;

	public BaseEntity(DatabaseHelper db, long id) {
		super();
		this.dbHelper = db;
		this.id = id;
	}

	public DatabaseHelper getDbHelper() {
		return dbHelper;
	}
	public long getId() {
		return id;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BaseEntity other = (BaseEntity) obj;
		if (id != other.id)
			return false;
		return true;
	}
}
