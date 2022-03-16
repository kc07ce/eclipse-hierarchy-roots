package gdfgd.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Shell;

public class ShowErrorDialogHandler {
	@Execute
	public static void execute(final Shell shell, Exception e) {
			// build the error message and include the current stack trace
			MultiStatus status = createMultiStatus(e.getLocalizedMessage(), e);
			// show error dialog
			ErrorDialog.openError(shell, "Error", "This is an error", status);
	}

	private static MultiStatus createMultiStatus(String msg, Throwable t) {

		List<Status> childStatuses = new ArrayList<>();
		StackTraceElement[] stackTraces = Thread.currentThread().getStackTrace();

		for (StackTraceElement stackTrace : stackTraces) {
			Status status = new Status(IStatus.ERROR, "com.vogella.tasks.ui", stackTrace.toString());
			childStatuses.add(status);
		}

		MultiStatus ms = new MultiStatus("com.vogella.tasks.ui", IStatus.ERROR, childStatuses.toArray(new Status[] {}),
				t.toString(), t);
		return ms;
	}
}
