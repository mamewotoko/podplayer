/**
 * 
 * @author declanshanaghy
 * http://blog.350nice.com/wp/archives/240
 * MultiChoice Preference Widget for Android
 *
 * @contributor matiboy
 * Added support for check all/none and custom separator defined in XML.
 * IMPORTANT: The following attributes MUST be defined (probably inside attr.xml)
 * for the code to even compile
 * <declare-styleable name="ListPreferenceMultiSelect">
    	<attr format="string" name="separator" />
    </declare-styleable>
 *  Whether you decide to then use those attributes is up to you.
 *
 */

package com.mamewo.podplayer0;
//modified by Takashi Masuyama <mamewotoko@gmail.com>

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class MultiListPreference
	extends ListPreference
{
	final static
	public String SEPARATOR = "!";
	private boolean[] mClickedDialogEntryIndices;
	
	public MultiListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		// Initialize the array of boolean to the same size as number of entries
		mClickedDialogEntryIndices = new boolean[getEntries().length];
	}

	@Override
	public void setEntries(CharSequence[] entries) {
		super.setEntries(entries);
		// Initialize the array of boolean to the same size as number of entries
		mClickedDialogEntryIndices = new boolean[entries.length];
	}

	public MultiListPreference(Context context) {
		this(context, null);
	}

	@Override
	protected void onPrepareDialogBuilder(Builder builder) {
		CharSequence[] entries = getEntries();
		CharSequence[] entryValues = getEntryValues();
		if (entries == null || entryValues == null ||
				entries.length != entryValues.length) {
			throw new IllegalStateException(
					"ListPreference requires an entries array and an entryValues array" +
					"which are both the same length");
		}

		restoreCheckedEntries();
		builder.setMultiChoiceItems(entries, mClickedDialogEntryIndices, 
				new DialogInterface.OnMultiChoiceClickListener() {
			public void onClick(DialogInterface dialog, int which, boolean val) {
				mClickedDialogEntryIndices[which] = val;
			}
		});
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getString(index);
	}

	public String[] parseStoredValue(CharSequence val) {
		if (null == val || "".equals(val)) {
			return null;
		}
		else {
			return ((String)val).split(SEPARATOR);
		}
	}

	private void restoreCheckedEntries() {
		CharSequence[] entryValues = getEntryValues();

		// Explode the string read in sharedpreferences
		String[] vals = parseStoredValue(getValue());

		if (vals != null) {
			List<String> valuesList = Arrays.asList(vals);
			for (int i = 0; i < entryValues.length; i++) {
				CharSequence entry = entryValues[i];
				if (valuesList.contains(entry)) {
					mClickedDialogEntryIndices[i] = true;
				}
			}
		}
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		ArrayList<String> values = new ArrayList<String>();

		CharSequence[] entryValues = getEntryValues();
		if (positiveResult && entryValues != null) {
			for (int i = 0; i < entryValues.length; i++) {
				if (mClickedDialogEntryIndices[i]) {
					// Don't save the state of check all option - if any
					String val = (String) entryValues[i];
					values.add(val);
				}
			}

			if (callChangeListener(values)) {
				setValue(join(values, SEPARATOR));
			}
		}
	}

	// Credits to kurellajunior on this post http://snippets.dzone.com/posts/show/91
	static
	protected String join(Iterable< ? extends Object > pColl, String separator) {
		Iterator< ? extends Object > oIter;
		if ( pColl == null || ( !( oIter = pColl.iterator() ).hasNext() ) )
			return "";
		StringBuilder oBuilder = new StringBuilder( String.valueOf( oIter.next() ) );
		while ( oIter.hasNext() )
			oBuilder.append( separator ).append( oIter.next() );
		return oBuilder.toString();
	}
}
