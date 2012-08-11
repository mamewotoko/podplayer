package com.mamewo.podplayer0;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class DoublePreference
	extends DialogPreference
	implements OnClickListener
{
	private double double_;
	private EditText editText_;
	private TextView dialogText_;
	private Button minusButton_;
	private Button plusButton_;
	static final
	private double STEP = 0.1;
	
	public DoublePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		double_ = 0.0;
		setDialogLayoutResource(R.layout.double_preference);
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		editText_ = (EditText) view.findViewById(R.id.double_pref_value);
		//TODO: add attribute to preference.xml
		dialogText_ = (TextView) view.findViewById(R.id.double_pref_text);
		dialogText_.setText("Set threshold of gesture score. 3.0 is recommended value.");
		minusButton_ = (Button) view.findViewById(R.id.double_minus_button);
		minusButton_.setOnClickListener(this);
		plusButton_ = (Button) view.findViewById(R.id.double_plus_button);
		plusButton_.setOnClickListener(this);
		setEditText(double_);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		if (positiveResult) {
			String strValue = editText_.getText().toString();
			double_ = Double.valueOf(strValue);
			setDoubleValue(double_);
		}
	}
	
	private void setEditText(double value) {
		editText_.setText(String.format("%.2f", value));
	}
	
	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getString(index);
	}
	
	private void setDoubleValue(double v) {
		double value = v;
		if (value < 0.0) {
			value = 0.0;
		}
		if (callChangeListener(value)) {
			double_ = value;
			persistString(Double.toString(value));
		}
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		double value;
		
		if (restoreValue) {
			value = Double.valueOf(getPersistedString(Double.toString(double_)));
		}
		else {
			value = Double.valueOf((String) defaultValue);
		}
		setDoubleValue(value);
	}

	@Override
	public void onClick(View view) {
		double doubleValue = double_;
		try {
			doubleValue = Double.valueOf(editText_.getText().toString());
		}
		catch (NumberFormatException e) {
			//do nothing
		}
		if (view == minusButton_) {
			doubleValue -= STEP;
		}
		else if (view == plusButton_) {
			doubleValue += STEP;
		}
		if (doubleValue < 0.0) {
			doubleValue = 0.0;
		}
		setEditText(doubleValue);
	}
}
