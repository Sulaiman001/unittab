package dh.sunicon.currency;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;

import android.text.TextUtils;
import dh.sunicon.datamodel.DatabaseHelper;

public class UpdatingReport implements Serializable
{
	private static final long serialVersionUID = 7032023292731426695L;

	public enum MessageType {INFO, WARNING, ERROR}
	public enum OperationType {INSERT, UPDATE}

	private boolean isCancel_ = false;
	private boolean inProgress = false;
	private int updatedCount_ = 0;
	private boolean forcedSuccessAll_ = false;
	private HashMap<String, UpdateItem> cacheUpdates_ = new HashMap<String, UpdateItem>();
	private ArrayList<ReportEntry> entries_ = new ArrayList<ReportEntry>();
	
	public UpdatingReport() {}
	
	public String getDisplayMessage() {
		if (entries_.size() == 1) {
			return entries_.get(0).getMessage();
		}
		else {
			if (updatedCount_ == 0) {
				return "Failed update rates."; //TODO multi-language
			}
			
			if (isSuccessAll()) {
				return "Done"; //TODO multi-language
			}
			
			return String.format("Updated rates for %d/%d currencies.", updatedCount_, DatabaseHelper.CURRENCY_COUNT);
		}
	}
	
	public boolean containsErrorOrWarning() {
		for (ReportEntry e : entries_) {
			if (e.getType() == MessageType.ERROR || e.getType() == MessageType.WARNING) {
				return true;
			}
		}
		return false;
	}
	
	public MessageType getType()
	{
		if (entries_.size() == 1) {
			return entries_.get(0).getType();
		}
		
		if (isSuccessAll()) {
			return MessageType.INFO;
		}
		
		if (updatedCount_ == 0) {
			return MessageType.ERROR;
		}
		
		return MessageType.WARNING;
	}
	
	public String getContentMessage() {
		StringWriter sw = new StringWriter();
		sw.append(getDisplayMessage() + '\n');
		for (ReportEntry e : entries_) {
			sw.append('\n'+e.toString());
		}
		return sw.toString();
	}
	
	public String getDetailMessage() {
		StringWriter sw = new StringWriter();
		
		sw.append(getDisplayMessage() + '\n');
		for (ReportEntry e : entries_) {
			boolean hasDetail = !TextUtils.isEmpty(e.getDetail()); 
			
			if (hasDetail) {
				sw.append("\n");
			}
			
			sw.append('\n'+e.toString());
			
			if (hasDetail) {
				sw.append("\n"+e.getDetail());
			}
		}
		
		return sw.toString();
	}
	
	/**
	 * warning: must change the methode name to "successUpdateMostly"
	 */
	public boolean isSuccessAll() {
		return forcedSuccessAll_ || updatedCount_ >= DatabaseHelper.CURRENCY_COUNT-10; //cheat!
	}
	
	/**
	 * warning: must change the methode name to "successUpdateMostly"
	 */
	public boolean isCacheFull() {
		return cacheUpdates_.size() == DatabaseHelper.CURRENCY_COUNT;
	}
	
	public ArrayList<ReportEntry> getEntries()
	{
		return entries_;
	}
	void add(ReportEntry reportEntry)
	{
		entries_.add(reportEntry);
	}
	public boolean isCached(String currencyCode) {
		return cacheUpdates_.containsKey(currencyCode);
	}
	void cacheUpdate(String currencyCode, UpdateItem updateItem) {
		cacheUpdates_.put(currencyCode, updateItem);
	}
	public boolean isDatabaseChanged() {
		return updatedCount_>0;
	}

	public boolean isCancel()
	{
		return isCancel_;
	}

	void setCancel(boolean isCancel)
	{
		isCancel_ = isCancel;
	}
	
	void forceSuccessAll()
	{
		forcedSuccessAll_ = true;
	}

	public static MessageType getMessageType(String s) {
		if (s.equals("E")) return MessageType.ERROR;
		if (s.equals("W")) return MessageType.WARNING;
		return MessageType.INFO;
	}
	
	public static String getMessageType(MessageType t) {
		switch (t) {
			case ERROR: return "E";
			case WARNING: return "W";
			default: return "I";
		}
	}
	
	public boolean isInProgress()
	{
		return inProgress;
	}

	void setInProgress(boolean inProgress)
	{
		this.inProgress = inProgress;
	}

	HashMap<String, UpdateItem> getCacheUpdates()
	{
		return cacheUpdates_;
	}

	void incrementUpdatedCount() {
		updatedCount_++;
	}
	
	void resetUpdatedCount() {
		updatedCount_ = 0;
	}
	
	public class ReportEntry implements Serializable {
		private static final long serialVersionUID = -324637469219624409L;
		private MessageType type_;
		private String message_;
		private String detail_;
	
		public ReportEntry(MessageType type, String message)
		{
			super();
			type_ = type;
			message_ = message;
		}
		public ReportEntry(MessageType type, String message, String detail)
		{
			this(type, message);
			detail_ = detail;
		}
		public MessageType getType()
		{
			return type_;
		}
		public String getMessage()
		{
			return message_;
		}
		public String getDetail()
		{
			return detail_;
		}
		
		public String toString() {
			return "["+getMessageType(type_) +"] "+message_;
		};
	}
	
	public class UpdateItem implements Serializable {
		private static final long serialVersionUID = -1864164486618962935L;
		private OperationType operation_;
		private long base_;
		private long target_;
		private double rate_;
		public UpdateItem(OperationType operation, long base, long target,
				double rate)
		{
			super();
			operation_ = operation;
			base_ = base;
			target_ = target;
			rate_ = rate;
		}
		public OperationType getOperation()
		{
			return operation_;
		}
		public long getBase()
		{
			return base_;
		}
		public long getTarget()
		{
			return target_;
		}
		public double getRate()
		{
			return rate_;
		}
		
	}
}
