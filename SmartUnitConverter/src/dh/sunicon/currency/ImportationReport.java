package dh.sunicon.currency;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ImportationReport
{
	public enum MessageType {INFO, WARNING, ERROR}

	private boolean isDatabaseChanged_ = false;
	private boolean isCancel_ = false;
	private ArrayList<ReportEntry> entries_ = new ArrayList<ReportEntry>();
	
	public ImportationReport() {}
	
	public ImportationReport(JSONObject savedState) throws JSONException {
		isDatabaseChanged_ = savedState.getBoolean("isDatabaseChanged");
		isCancel_ = savedState.getBoolean("isCancel");
		
		JSONArray entriesJson = savedState.getJSONArray("entries");
		int n = entriesJson.length();
		for (int i = 0; i<n; i++) {
			ReportEntry e = new ReportEntry((JSONObject) entriesJson.get(i));
			entries_.add(e);
		}
	}
	
	public JSONObject serialize() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("isDatabaseChanged", isDatabaseChanged_);
		json.put("isCancel", isCancel_);
		
		JSONArray entriesJson = new JSONArray();
		for (ReportEntry e : entries_) {
			entriesJson.put(e.serialize());
		}
		json.put("entries", entriesJson);
		
		return json;
	}
	
	public String getDisplayMessage() {
		if (entries_.size() == 1) {
			return entries_.get(0).getMessage();
		}
		else {
			MessageType type = getType();
			switch (type) { 
				case ERROR:
					return "Update Failed";
				case WARNING:
					if (isDatabaseChanged_) {
						return "Update partialy failed";
					}
					else {
						return "Update Failed";
					}
				default:
					return "Update complete";
			}
		}
	}
	
	public String getContentMessage() {
		String resu = getDisplayMessage() + '\n';
		for (ReportEntry e : entries_) {
			resu = resu + '\n'+e.toString();
		}
		return resu;
	}
	
	public MessageType getType()
	{
		boolean hasWarning = false;
		for (ReportEntry e : entries_) {
			if (e.getType() == MessageType.ERROR) {
				return MessageType.ERROR;
			}
			if (e.getType() == MessageType.WARNING) {
				hasWarning = true;
			}
		}
		if (hasWarning) {
			return MessageType.WARNING;
		}
		return MessageType.INFO;
	}
	
	public boolean successUpdateAll() {
		return getType() == MessageType.INFO;
	}
	
	public ArrayList<ReportEntry> getEntries()
	{
		return entries_;
	}
	public void add(ReportEntry reportEntry)
	{
		entries_.add(reportEntry);
	}
	
	public boolean isDatabaseChanged()
	{
		return isDatabaseChanged_;
	}
	public void setDatabaseChanged(boolean isDatabaseChanged)
	{
		isDatabaseChanged_ = isDatabaseChanged;
	}

	public boolean isCancel()
	{
		return isCancel_;
	}

	public void setCancel(boolean isCancel)
	{
		isCancel_ = isCancel;
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
	
	public class ReportEntry {
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
		public ReportEntry(JSONObject savedState) throws JSONException
		{
			this(getMessageType(savedState.getString("type")), savedState.getString("message"), savedState.getString("detail"));
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
		
		public JSONObject serialize() throws JSONException {
			JSONObject json = new JSONObject();
			json.put("type", getMessageType(type_));
			json.put("message", message_);
			json.put("detail", detail_);
			return json;
		}
	}
}
