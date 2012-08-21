package dh.sunicon.datamodel;

import android.database.Cursor;

public class Conversion extends BaseEntity
{
	private long base;
	private long target;
	private Double fx; // target = fx*base
	private String formula; // target = sqrt(%base)
	private String reversedFormula; // base = %target^2

	public Conversion(DatabaseHelper dbHelper, long id, long base, long target,
			Double fx, String formula, String reversedFormula)
	{
		super(dbHelper, id);
		this.base = base;
		this.target = target;
		this.fx = fx;
		this.formula = formula;
		this.reversedFormula = reversedFormula;
	}

	public long getBase()
	{
		return base;
	}

	public long getTarget()
	{
		return target;
	}

	public Double getFx()
	{
		return fx;
	}

	public String getFormula()
	{
		return formula;
	}

	public String getReversedFormula()
	{
		return reversedFormula;
	}

	public Unit getBaseUnit()
	{
		return Unit.findById(this.getDbHelper(), this.getBase());
	}

	public Unit getTargetUnit()
	{
		return Unit.findById(this.getDbHelper(), this.getTarget());
	}

	public static Conversion parseCursor(DatabaseHelper dbHelper, Cursor cur)
	{
		return new Conversion(dbHelper, cur.getLong(cur.getColumnIndex("id")),
				cur.getLong(cur.getColumnIndex("base")), cur.getLong(cur
						.getColumnIndex("target")), cur.getDouble(cur
						.getColumnIndex("fx")), cur.getString(cur
						.getColumnIndex("formula")), cur.getString(cur
						.getColumnIndex("reversedFormula")));
	}
}
