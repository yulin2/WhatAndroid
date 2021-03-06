package what.whatandroid.comments.tags;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.QuoteSpan;
import android.text.style.StyleSpan;
import what.whatandroid.comments.spans.LargeQuoteSpan;

/**
 * Handles site quote tags. Also applies newlines to beginning and end
 * of the quote to make sure it's spaced properly
 */
public class QuoteTagStyle implements TagStyle {
	@Override
	public Spannable getStyle(CharSequence param, CharSequence text){
		SpannableStringBuilder ssb = new SpannableStringBuilder();
		ssb.append("\n");
		String user = param == null ? null : param.toString();
		if (user != null){
			int nameEnd = user.indexOf('|');
			if (nameEnd != -1){
				user = user.substring(0, nameEnd);
			}
			user += " wrote: \n";
			ssb.append(user);
			ssb.setSpan(new StyleSpan(Typeface.BOLD), 1, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		int end = ssb.length();
		ssb.append(text);
		ssb.setSpan(new LargeQuoteSpan(0xff33b5e5), end, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		ssb.append(" \n");
		return ssb;
	}
}
