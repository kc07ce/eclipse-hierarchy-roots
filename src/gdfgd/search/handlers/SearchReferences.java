package gdfgd.search.handlers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jface.text.Document;

public class SearchReferences extends SearchRequestor {

	public static void searchReferences(IMethod method) {
		try {
			SearchPattern pattern = SearchPattern.createPattern(method, IJavaSearchConstants.REFERENCES);
			IJavaSearchScope scope = SearchEngine.createWorkspaceScope();
			SearchEngine searchEngine = new SearchEngine();
			SearchReferences requestor = new SearchReferences();
			searchEngine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, scope,
					requestor, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private int getLineNumber(SearchMatch match) throws Exception{

		IResource resource = match.getResource();
		if (!(resource instanceof IFile)) {
			// Log Error
			return -1;
		}
		IFile file = (IFile) resource;
		int offset = match.getOffset();
		byte[] bytes = new byte[offset];
		int read = file.getContents().read(bytes, 0, offset);
		if (read != offset) {
			// Log error
			return -1;
		}
		String contents = new String(bytes);
		Document fileSource = new Document(contents);
		return fileSource.getLineOfOffset(offset) + 1;
	}

	@Override
	public void acceptSearchMatch(SearchMatch arg0) throws CoreException {
//		System.out.println(arg0.getElement().toString());
		// TODO Auto-generated method stub
		try {
			System.out.println(getLineNumber(arg0));
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
