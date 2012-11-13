package dh.sunicon.currency;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;

import android.text.TextUtils;

import dh.sunicon.datamodel.DatabaseHelper;

public class UpdatingReport implements Serializable
{
	private static final long serialVersionUID = 7032023292731426695L;

	public enum MessageType {INFO, WARNING, ERROR}

	private boolean isCancel_ = false;
	private boolean inProgress = false;
	private ArrayList<String> updatedCurrencies_ = new ArrayList<String>();
	private ArrayList<ReportEntry> entries_ = new ArrayList<ReportEntry>();
	private boolean isSuccessAll_ = false;
	
	public UpdatingReport() {}
	
	public ArrayList<String> getUpdatedCurrencies()
	{
		return updatedCurrencies_;
	}

	public String getDisplayMessage() {
		if (entries_.size() == 1) {
			return entries_.get(0).getMessage();
		}
		else {
			if (updatedCurrencies_.size() == 0) {
				return "Failed update rates."; //TODO multi-language
			}
			
			if (isSuccessAll()) {
				return "Done"; //TODO multi-language
			}
			
			return String.format("Updated rates for %d/%d currencies.",updatedCurrencies_.size(),DatabaseHelper.CURRENCY_COUNT);
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
		
		if (updatedCurrencies_.size() == 0) {
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
		return isSuccessAll_ || updatedCurrencies_.size() >= DatabaseHelper.CURRENCY_COUNT-1; //cheat!
	}
	
	public ArrayList<ReportEntry> getEntries()
	{
		return entries_;
	}
	void add(ReportEntry reportEntry)
	{
		entries_.add(reportEntry);
	}
	public boolean isCurrencyUdpated(String currencyCode) {
		return updatedCurrencies_.contains(currencyCode);
	}
	void reportUpdatedCurrency(String currencyCode)
	{
		updatedCurrencies_.add(currencyCode);
	}
	public boolean isDatabaseChanged() {
		return !updatedCurrencies_.isEmpty();
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
		isSuccessAll_ = true;
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
}
