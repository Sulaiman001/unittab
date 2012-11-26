package dh.sunicon.workarounds;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Manipulate 3 instances of NumberFormat to efficiently format a great number of double values.
 * avoid to create an object NumberFormat each time.
 * 
 * 3 instances of NumberFormat will be reused to format a value v:
 * 
 * if v<EXP_DOWN, uses nfBelow
 * if EXP_DOWN<=v<=EXP_UP, uses nfNormal
 * if EXP_UP<v, uses nfAbove
 * 
 * nfBelow, nfNormal and nfAbove will be generated base on the precision_ parameter. 
 */
public class DoubleFormatter
{
	private static final double EXP_UP = 1e9; //in my experience: EXP_UP should not be greater than 1e17
	private static final double EXP_DOWN = 1.e-3;
	private int precision_;
	private NumberFormat nfBelow_; 
	private NumberFormat nfNormal_;
	private NumberFormat nfAbove_;
	
	private enum NumberFormatKind {Below, Normal, Above}
	
	public DoubleFormatter(int precision)
	{
		setPrecision(precision);
	}
	
	public void setPrecision(int precision)
	{
		Preconditions.checkArgument(precision>=0);
		
		if (precision == precision_) {
			return;
		}
		
		precision_ = precision;
		nfBelow_ = createNumberFormat(NumberFormatKind.Below);
		nfNormal_ = createNumberFormat(NumberFormatKind.Normal);
		nfAbove_ = createNumberFormat(NumberFormatKind.Above);
	}

	private NumberFormat createNumberFormat(NumberFormatKind kind) {
		final String sharpByPrecision = Strings.repeat("#", precision_); //if you do not use Guava library, replace with createSharp(precision);
		NumberFormat f = NumberFormat.getInstance(Locale.US);
		
		//Apply banker's rounding:  this is the rounding mode that statistically minimizes cumulative error when applied repeatedly over a sequence of calculations
		f.setRoundingMode(RoundingMode.HALF_EVEN);
		
		if (f instanceof DecimalFormat) {
			DecimalFormat df = (DecimalFormat) f;
			DecimalFormatSymbols dfs = df.getDecimalFormatSymbols();
			
			//set group separator to space instead of comma

			//dfs.setGroupingSeparator(' ');

			//set Exponent symbol to minus 'e' instead of 'E'
			if (kind == NumberFormatKind.Above) {
				dfs.setExponentSeparator("e+"); //force to display the positive sign in the exponent part
			}
			else {
				dfs.setExponentSeparator("e");
			}
			
			df.setDecimalFormatSymbols(dfs);
			
			//use exponent format if v is out side of [EXP_DOWN,EXP_UP]
			
			if (kind == NumberFormatKind.Normal) {
				if (precision_ == 0) {
					df.applyPattern("#,##0");
				}
				else {
					df.applyPattern("#,##0."+sharpByPrecision);
				}
			}
			else {
				if (precision_ == 0) {
					df.applyPattern("0E0");
				}
				else {
					df.applyPattern("0."+sharpByPrecision+"E0");
				}
			}
		}
		
		return f;
	} 
	
	public String format(double v) {
		if (Double.isNaN(v)) {
			return "-";
		}
		if (v==0) {
			return "0"; 
		}
		final double absv = Math.abs(v);
		
		if (absv<EXP_DOWN) {
			return nfBelow_.format(v);
		}
		
		if (absv>EXP_UP) {
			return nfAbove_.format(v);
		}
		
		return nfNormal_.format(v);
	}
	
	/**
	 * format and higlight the important part (integer part & exponent part) 
	 */
	public String formatHtml(double v) {
		if (Double.isNaN(v)) {
			return "-";
		}
		return htmlize(format(v));
	}
	
	/**
	 * This is the base alogrithm: create a instance of NumberFormat for the value, then format it. It should
	 * not be used to format a great numbers of value 
	 * 
	 * We will never use this methode, it is here only to understanding
	 * 
	 * Algo principal:
	 * 
	 * format v to string. precision_ is numbers of digits after decimal. 
	 * if EXP_DOWN <= abs(v) <= EXP_UP, display the normal format: 124.45678
	 * otherwise display scientist format with: 1.2345e+30 
	 * 
	 * pre-condition: precision >= 1
	 */
	@Deprecated
	public String formatInefficient(double v) {

		final String sharpByPrecision = Strings.repeat("#", precision_); //if you do not use Guava library, replace with createSharp(precision);
		
		final double absv = Math.abs(v);
		
		NumberFormat f = NumberFormat.getInstance(Locale.US);
		
		//Apply banker's rounding:  this is the rounding mode that statistically minimizes cumulative error when applied repeatedly over a sequence of calculations
		f.setRoundingMode(RoundingMode.HALF_EVEN);
		
		if (f instanceof DecimalFormat) {
			DecimalFormat df = (DecimalFormat) f;
			DecimalFormatSymbols dfs = df.getDecimalFormatSymbols();
			
			//set group separator to space instead of comma

			dfs.setGroupingSeparator(' ');

			//set Exponent symbol to minus 'e' instead of 'E'
			
			if (absv>EXP_UP) {
				dfs.setExponentSeparator("e+"); //force to display the positive sign in the exponent part
			}
			else {
				dfs.setExponentSeparator("e");
			}
			df.setDecimalFormatSymbols(dfs);
			
			//use exponent format if v is out side of [EXP_DOWN,EXP_UP]
			
			if (absv<EXP_DOWN || absv>EXP_UP) {
				df.applyPattern("0."+sharpByPrecision+"E0");
			}
			else {
				df.applyPattern("#,##0."+sharpByPrecision);
			}
		}

		return f.format(v);
	}
	
	private static String htmlize(String s)
	{
		StringBuilder resu = new StringBuilder("<b>");
		int p1 = s.indexOf('.');
		
		if (p1>0) {
			resu.append(s.substring(0, p1));
			resu.append("</b>");
		}
		else {
			p1 = 0;
		}
		
		int p2 = s.lastIndexOf('e');
		if (p2>0)
		{
			resu.append(s.substring(p1, p2));
			resu.append("<b>");
			resu.append(s.substring(p2, s.length()));
			resu.append("</b>");
		}
		else
		{
			resu.append(s.substring(p1, s.length()));
			if (p1==0)
			{
				resu.append("</b>");
			}
		}
		
		return resu.toString();
	}
}
