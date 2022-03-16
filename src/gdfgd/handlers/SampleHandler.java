package gdfgd.handlers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

public class SampleHandler extends AbstractHandler {

	private String content = null;
	private String packageName = null;
	private String methodName = null;
	private String className = null;
	private String[] methodArgs = null;
	private Set<String> traversedSet;
	private Set<String> projectSet;
	private Set<String> apiList;
	private ExecutionEvent event;

	private static final String API = "api    : ";
	private static final String CLASS = "class  : ";
	private static final String METHOD = "method : ";
	private static final String SOURCE = "source : ";

	private String modifyAPI(String api) {
		String modifiedAPI = "";
		for (int i = 0; i < api.length(); i++) {
			if (api.charAt(i) != ' ' && api.charAt(i) != '"') {
				modifiedAPI += api.charAt(i);
			}
		}
		return modifiedAPI;
	}

	public void addIntoSet(int noOfCallers, IMethod m, Set<String> apiSet, Set<String> nonAPISet, Set<String> testSet,
			Set<String> callORRun) throws ExecutionException, JavaModelException {
//		System.out.println(m.getSource());
//		System.out.println(m.getPath().toString()+"|"+ m.getElementName()+"|"+noOfCallers);
		if (this.traversedSet.contains(m.getKey()))
			return;
		this.traversedSet.add(m.getKey());
		boolean addAPI = false;
		try {
			List<String> classLevelAPI = new ArrayList<String>();
			for (IAnnotation annotation : m.getDeclaringType().getAnnotations()) {
				if (annotation.getSource().contains("Mapping")) {
					if (annotation.getSource().contains("{")) {
						for (String classAPI : annotation.getSource().split("\\{")[1].split("\\}")[0].split(",")) {
							classLevelAPI.add(modifyAPI(classAPI));
						}
					}
					for (IMemberValuePair x : annotation.getMemberValuePairs()) {
						if ((x.getMemberName().equals("value") || x.getMemberName().equals("path"))
								&& x.getValue() instanceof String) {
							classLevelAPI.clear();
							classLevelAPI.add((String) x.getValue());
						}
					}
					if (classLevelAPI.size() == 0)
						classLevelAPI.add("");
					addAPI = true;
				}
			}
			List<String> methodLevelAPI = new ArrayList<String>();
			String httpMethod = "";
			for (IAnnotation annotation : m.getAnnotations()) {
				if (annotation.getSource().contains("Mapping")) {
					httpMethod = annotation.getElementName();
					if (annotation.getSource().contains("{")) {
						System.out.println(annotation.getSource().split("\\{")[1]);
						for (String methodAPI : annotation.getSource().split("\\{")[1].split("\\}")[0].split(",")) {
							methodLevelAPI.add(modifyAPI(methodAPI));
						}
						System.out.println("asdf");
					}
					for (IMemberValuePair x : annotation.getMemberValuePairs()) {
						if ((x.getMemberName().equals("value") || x.getMemberName().equals("path"))
								&& x.getValue() instanceof String) {
							methodLevelAPI.clear();
							methodLevelAPI.add((String) x.getValue());
						}
						if (x.getMemberName().equals("method"))
							httpMethod = (String) x.getValue();
					}
					if (methodLevelAPI.size() == 0)
						methodLevelAPI.add("");
					addAPI = true;
				}
			}

			String message = "";
			message += CLASS + m.getDeclaringType().getPath().toString() + "\n" + METHOD + m.getElementName();
			if (addAPI) {
				String api = "";
				for (String classAPI : classLevelAPI) {
					for (String methodAPI : methodLevelAPI) {
						api += classAPI + methodAPI + " | " + httpMethod + "\n";
					}
				}
				message = API + api + message;
				apiSet.add(message);
				this.apiList.add(api);
			} else if (noOfCallers == 0) {
				if (m.getElementName().equals("run") || m.getElementName().equals("call")) {
					message += "\n" + SOURCE + "\n" + m.getSource();
					callORRun.add(message);
				} else if (m.getPath().toString().contains("src/main"))
					nonAPISet.add(message);
				else if (m.getPath().toString().contains("src/test"))
					testSet.add(message);
			}
		} catch (Exception e) {
			ShowErrorDialogHandler.execute(HandlerUtil.getActiveShellChecked(this.event), e);
		}
	}

	public void getRecurisveCallers(IMethod m, Set<String> apiSet, Set<String> nonAPISet, Set<String> testSet,
			Set<String> callORRun) throws JavaModelException, ExecutionException {

		CallHierarchy callHierarchy = CallHierarchy.getDefault();
		IMember[] members = { m };
		MethodWrapper[] methodWrappers = callHierarchy.getCallerRoots(members);
		for (MethodWrapper mw : methodWrappers) {
			MethodWrapper[] mw2 = {};
			if (!(m.getElementName().equals("run") || m.getElementName().equals("call")))
				mw2 = mw.getCalls(new NullProgressMonitor());

			// filtering out calls from out of workspace
			List<MethodWrapper> filteredList = new ArrayList<MethodWrapper>();
			int nonTestMethods = 0;
			for (MethodWrapper methodWrapper : mw2) {
				IMethod x = getIMethodFromMethodWrapper(methodWrapper);
				for (String project : this.projectSet) {
					if (x.getPath().toString().contains(project) && x.getPath().toString().contains("src/main")) {
						filteredList.add(methodWrapper);
						break;
					}
				}
			}

			// have any more callers of this method? if not add it if not already added.
			// Else get callers and loop again for each
			addIntoSet(filteredList.size(), m, apiSet, nonAPISet, testSet, callORRun);
			for (MethodWrapper mw3 : filteredList) {
				IMethod imethod = getIMethodFromMethodWrapper(mw3);
				getRecurisveCallers(imethod, apiSet, nonAPISet, testSet, callORRun);
			}
		}
	}

	private IMethod getIMethodFromMethodWrapper(MethodWrapper m) {
		try {
			IMember im = m.getMember();
			if (im.getElementType() == IJavaElement.METHOD) {
				return (IMethod) m.getMember();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private IMethod getCursorMethod() {
		try {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			IEditorPart activeEditor = page.getActiveEditor();
			if (activeEditor instanceof JavaEditor) {
				IJavaElement elt = EditorUtility.getEditorInputJavaElement(activeEditor, false)
						.getElementAt(((TextSelection) page.getSelection()).getOffset());
				if (elt.getElementType() == IJavaElement.METHOD) {
					return (IMethod) elt;
				}
			}
		} catch (Exception e) {

		}
		return null;
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		this.event = event;
//		// Taking input
//		String input = null;
//		InputDialog dlg = new InputDialog(HandlerUtil.getActiveShellChecked(event), "Enter qualified method name", "",
//				"", null);
//		if (dlg.open() == Window.OK) {
//			input = dlg.getValue();
//		}
//		if (input == null || input == "")
//			return null;
//		try {
//			String methodInfo = input.split("\\(")[0];
//			this.methodArgs = input.split("\\(")[1].split("\\)")[0].split(",");
//			String[] segs = methodInfo.split("\\.");
//			int n = segs.length;
//			if (n < 3)
//				throw new Exception("Invalid method");
//			this.methodName = segs[n - 1];
//			this.className = segs[n - 2];
//			this.packageName = String.join(".", Arrays.copyOfRange(segs, 0, n - 2));
//		} catch (Exception e2) {
//			ShowErrorDialogHandler.execute(HandlerUtil.getActiveShellChecked(event), e2);
//			return null;
//		}

		this.apiList = new HashSet<String>();
		this.content = "";
		this.projectSet = new HashSet<String>();
		this.traversedSet = new HashSet<String>();
		Set<String> set = new HashSet<String>();
		Set<String> apiSet = new HashSet<String>();
		Set<String> nonAPISet = new HashSet<String>();
		Set<String> testSet = new HashSet<String>();
		Set<String> callORRun = new HashSet<String>();

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		// Get all projects in the workspace
		IProject[] projects = root.getProjects();
		for (IProject project : projects)
			this.projectSet.add(project.getName());

		IMethod method = getCursorMethod();
		if (method == null) {
			return null;
		}
		try {
			getRecurisveCallers(method, apiSet, nonAPISet, testSet, callORRun);
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// setting content to be displayed
		if (!apiSet.isEmpty()) {
			this.content += "++++++++++++++++++++++++++++++APIs Flows+++++++++++++++++++++++++++\n";
			for (String s : apiSet) {
				this.content += s + "\n" + "-------------------------------------------------------------------------"
						+ "\n";
			}
		}

		if (!this.apiList.isEmpty()) {
			this.content += "API Summary\n";
			for (String s : this.apiList) {
				this.content += s + "\n";
			}
		}
		if (!nonAPISet.isEmpty()) {
			this.content += "\n\n" + "++++++++++++++++++++++++++++++Non API Flows+++++++++++++++++++++++++++++++++++"
					+ "\n";
			for (String s : nonAPISet) {
				this.content += s + "\n" + "-------------------------------------------------------------------------"
						+ "\n";
			}
		}
		if (!callORRun.isEmpty()) {
			this.content += "\n\n" + "++++++++++++++++++++++++++++++References+++++++++++++++++++++++++++++++++++"
					+ "\n";
			for (String s : callORRun) {
				this.content += s + "\n" + "-------------------------------------------------------------------------"
						+ "\n";
			}
		}
		if (!testSet.isEmpty()) {
			this.content += "\n\n" + "++++++++++++++++++++++++++++++Test Flows+++++++++++++++++++++++++++++++++++"
					+ "\n";
			for (String s : testSet) {
				this.content += s + "\n" + "-------------------------------------------------------------------------"
						+ "\n";
			}
		}
		if (this.content != "")
			ShowResultDialogHandler.showInConsole(this.content, "Results for " + method.getKey(), event);
		return null;
	}

}
