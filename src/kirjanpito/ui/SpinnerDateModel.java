package kirjanpito.ui;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import javax.swing.AbstractSpinnerModel;

public class SpinnerDateModel extends AbstractSpinnerModel {
	private DateTextField editor;
	private Calendar cal;

	private static final long serialVersionUID = 1L;

	public SpinnerDateModel(DateTextField editor) {
		this.editor = editor;
		this.cal = Calendar.getInstance();
		cal.setLenient(true);
	}

	@Override
	public Object getNextValue() {
		return addMonth(1);
	}

	@Override
	public Object getPreviousValue() {
		return addMonth(-1);
	}

	@Override
	public Object getValue() {
		try {
			return editor.getDate();
		}
		catch (ParseException e) {
			return null;
		}
	}

	@Override
	public void setValue(Object value) {
		editor.setDate((Date)value);
		fireStateChanged();
	}

	private Date addMonth(int month) {
		Date date;

		try {
			date = editor.getDate();
		}
		catch (ParseException e) {
			return null;
		}

		if (date == null) {
			return null;
		}

		cal.setTime(date);
		boolean lastDay = (cal.getActualMaximum(Calendar.DAY_OF_MONTH) == cal.get(Calendar.DAY_OF_MONTH));
		cal.add(Calendar.MONTH, month);

		if (lastDay) {
			cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
		}

		return cal.getTime();
	}
}