package gdfgd.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.handlers.HandlerUtil;

public class ShowResultDialogHandler {

	public static void showInConsole(String content, String name, ExecutionEvent event) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();

		MessageConsole myConsole = findConsole(name);
		myConsole.clearConsole();
		conMan.addConsoles(new IConsole[] { myConsole });
		MessageConsoleStream out = myConsole.newMessageStream();
		out.println(content);
		IWorkbenchWindow win = HandlerUtil.getActiveWorkbenchWindow(event);
		IWorkbenchPage page = win.getActivePage();
		String id = IConsoleConstants.ID_CONSOLE_VIEW;
		IConsoleView view;
		try {
			view = (IConsoleView) page.showView(id);
			view.display(myConsole);
		} catch (PartInitException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
	}

	private static MessageConsole findConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		for (int i = 0; i < existing.length; i++)
			if (name.equals(existing[i].getName()))
				return (MessageConsole) existing[i];
		// no console found, so create a new one
		MessageConsole myConsole = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[] { myConsole });
		return myConsole;
	}
}