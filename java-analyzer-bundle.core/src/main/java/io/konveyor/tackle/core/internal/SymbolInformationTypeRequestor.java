package io.konveyor.tackle.core.internal;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.util.ArrayList;
import java.util.List;
import java.net.URI;

import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.core.search.TypeReferenceMatch;
import org.eclipse.jdt.internal.core.ImportDeclaration;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.core.ResolvedBinaryMethod;
import org.eclipse.jdt.internal.core.ResolvedSourceType;
import org.eclipse.jdt.internal.core.SourceRefElement;
import org.eclipse.jdt.core.search.MethodReferenceMatch;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;

public class SymbolInformationTypeRequestor extends SearchRequestor {
        private List<SymbolInformation> symbols;
		private int maxResults;
		private boolean sourceOnly;
		private boolean isSymbolTagSupported;
		private IProgressMonitor monitor;
        private int symbolKind;
        private String query;

		public SymbolInformationTypeRequestor(List<SymbolInformation> symbols, int maxResults, IProgressMonitor monitor, int symbolKind, String query) {
			this.symbols = symbols;
			this.maxResults = maxResults;
			this.monitor = monitor;
            this.symbolKind = symbolKind;
            this.query = query;
		}


        @Override
        public void acceptSearchMatch(SearchMatch match) throws CoreException {
            if (maxResults > 0 && symbols.size() >= maxResults) {
                monitor.setCanceled(true);
                logInfo("maxResults > 0 && symbols.size() >= maxResults");
                return;
            }
            // If we are not looking at files, then we don't want to return anytyhing for the match.
            //logInfo("getResource().getType()" + match.getResource().getType());
            if ((match.getResource().getType() | IResource.FILE) == 0 || match.getElement() == null) {
                logInfo("match.getResource().getType() | IResource.FILE");
                return;

            }
            IJavaElement element = (IJavaElement)match.getElement();
            SymbolKind k = convertSymbolKind(element);
            
            logInfo("symbolKind: " + k + " kind passed in " + this.symbolKind );
            switch (this.symbolKind) {
            case 9: 
                try {
                    TypeReferenceMatch m = (TypeReferenceMatch)match;
                    ILocalVariable var = (ILocalVariable)m.getLocalElement();
                    if (var == null ) {
                        return;
                    }
                    SymbolInformation symbol = new SymbolInformation();
                    symbol.setName(var.getElementName());
                    symbol.setKind(convertSymbolKind(var));
                    symbol.setContainerName(var.getParent().getElementName());
                    symbol.setLocation(JDTUtils.toLocation(var));
                    this.symbols.add(symbol);
                } catch (Exception e) {
                    logInfo("match:" + match + " Unable to convert for variable: " + e);
                    return;
                }
            case 8:
                try {
                    IImportDeclaration mod = (IImportDeclaration)match.getElement();
                    logInfo("match: " + mod);
                    SymbolInformation symbol = new SymbolInformation();
                    symbol.setName(mod.getElementName());
                    symbol.setKind(k);
                    symbol.setContainerName(mod.getParent().getElementName());
                    symbol.setLocation(JDTUtils.toLocation(mod));
                    this.symbols.add(symbol);
                } catch (Exception e) {
                    logInfo("element:" + match.getElement() + " Unable to convert for package: " + e);
                    return;
                }
            case 7:
                try {
                    IMethod method = (IMethod)match.getElement();
                    String signature = method.getReturnType();
                    String[] strings = this.query.split("\\.");
                    logInfo("signature: " + signature + "query: " + this.query);
                    for (String string : strings) {
                        // remove regex pattern match character
                        String s = string.replaceAll("\\*", "");
                        s = s.replaceAll("[", "");
                        // check if the string found is apart of the signature.
                        // TODO: Handle array cases. need to map [] to [ at the beginning.
                        logInfo("signature: " + signature + "replaced string" + s);
                        if (signature.contains(s)) {
                            logInfo(s);
                            SymbolInformation symbol = new SymbolInformation();
                            symbol.setName(method.getElementName());
                            symbol.setKind(k);
                            symbol.setContainerName(method.getParent().getElementName());
                            Location location = JDTUtils.toLocation(method);
                            if (location == null) {
                                IClassFile classFile = method.getClassFile();
                                String packageName = classFile.getParent().getElementName();
                                String jarName = classFile.getParent().getParent().getElementName();
                                String uriString = new URI("jdt", "contents", JDTUtils.PATH_SEPARATOR + jarName + JDTUtils.PATH_SEPARATOR + packageName + JDTUtils.PATH_SEPARATOR + classFile.getElementName(), classFile.getHandleIdentifier(), null).toASCIIString();
                                if (uriString == null) {
                                    uriString = method.getPath().toString();
                                }
                                Range range = JDTUtils.toRange(method.getOpenable(), method.getNameRange().getOffset(), method.getNameRange().getLength());
                                location = new Location(uriString, range);
                            }
                            symbol.setLocation(location);
                            this.symbols.add(symbol);
                            return;
                        }
                    }
                    logInfo("not found: " + signature);
                    return;

                } catch (Exception e) {
                    logInfo("Exception: " + e);
                    return;
                }
            case 5:
                try {
                    IType mod = (IType)match.getElement();
                    // A interface can not impment another interface
                    // this allows us to easily filter this search down.
                    if (mod.isInterface()) {
                        return;
                    }
                    SymbolInformation symbol = new SymbolInformation();
                    symbol.setName(mod.getElementName());
                    symbol.setKind(k);
                    symbol.setContainerName(mod.getParent().getElementName());
                    Location location = JDTUtils.toLocation(mod);
                    if (location == null) {
                        IClassFile classFile = mod.getClassFile();
                        String packageName = classFile.getParent().getElementName();
                        String jarName = classFile.getParent().getParent().getElementName();
                        String uriString = new URI("jdt", "contents", JDTUtils.PATH_SEPARATOR + jarName + JDTUtils.PATH_SEPARATOR + packageName + JDTUtils.PATH_SEPARATOR + classFile.getElementName(), classFile.getHandleIdentifier(), null).toASCIIString();
                        if (uriString == null) {
                            uriString = mod.getPath().toString();
                        }
                        Range range = JDTUtils.toRange(mod.getOpenable(), mod.getNameRange().getOffset(), mod.getNameRange().getLength());
                        location = new Location(uriString, range);
                    }
                    symbol.setLocation(location);
                    this.symbols.add(symbol);
                    return;
                } catch (Exception e) {
                    logInfo("element:" + match.getElement() + " Unable to convert for implements: " + e);
                    return;
                }
            case 1:
                try {
                    IType mod = (IType)match.getElement();
                    // Only add things that have a super class. This is an itermediate filtration.
                    // TODO: add ability to find the actual super type and find the correct type of object (interface or class) based on super
                    // TODO: I think that this is starting to get to the point where more than one symbol acceptor is going to be useful.
                    logInfo("mod: " + mod + " superclass name: " + mod.getSuperclassName());
                    if (mod.isClass() && (mod.getSuperclassName() == null || mod.getSuperclassName() == "java.lang.Object")) {
                        return;
                    }
                    if (mod.isInterface() && mod.getSuperInterfaceNames().length == 0) {
                        return;
                    }
                    SymbolInformation symbol = new SymbolInformation();
                    symbol.setName(mod.getElementName());
                    symbol.setKind(k);
                    symbol.setContainerName(mod.getParent().getElementName());
                    Location location = JDTUtils.toLocation(mod);
                    if (location == null) {
                        IClassFile classFile = mod.getClassFile();
		                String packageName = classFile.getParent().getElementName();
		                String jarName = classFile.getParent().getParent().getElementName();
                        String uriString = new URI("jdt", "contents", JDTUtils.PATH_SEPARATOR + jarName + JDTUtils.PATH_SEPARATOR + packageName + JDTUtils.PATH_SEPARATOR + classFile.getElementName(), classFile.getHandleIdentifier(), null).toASCIIString();
                        if (uriString == null) {
                            uriString = mod.getPath().toString();
                        }
                        Range range = JDTUtils.toRange(mod.getOpenable(), mod.getNameRange().getOffset(), mod.getNameRange().getLength());
                        location = new Location(uriString, range);
                    }
                    symbol.setLocation(location);
                    this.symbols.add(symbol);
                    return;
                } catch (Exception e) {
                    logInfo("element:" + match.getElement() + " Unable to convert for inheritance: " + e);
                    return;
                }
            case 4:
                try {
                    IAnnotatable mod = (IAnnotatable)match.getElement();
                    for (IAnnotation annotation: mod.getAnnotations()) {
                        SymbolInformation symbol = new SymbolInformation();
                        symbol.setName(annotation.getElementName());
                        symbol.setKind(k);
                        symbol.setContainerName(annotation.getParent().getElementName());
                        symbol.setLocation(getLocation(element));
                        this.symbols.add(symbol);
                    }
                    return;
                } catch (Exception e) {
                    logInfo("unable to get method from case 4(annotations): " + e);
                    return;
                }
            // Dealing with methods and constructors.
            case 2:
                // For Method Calls we will need to do the local variable trick
                try {
                    logInfo("match: " + match);
                    MethodReferenceMatch m = (MethodReferenceMatch)match;
                    IMethod e = (IMethod)m.getElement();
                    SymbolInformation symbol = new SymbolInformation();
                    symbol.setName(e.getElementName());
                    symbol.setKind(convertSymbolKind(e));
                    symbol.setContainerName(e.getParent().getElementName());
                    Location location = JDTUtils.toLocation(e);
                    if (location == null) {
                        IClassFile classFile = e.getClassFile();
		                String packageName = classFile.getParent().getElementName();
		                String jarName = classFile.getParent().getParent().getElementName();
                        String uriString = new URI("jdt", "contents", JDTUtils.PATH_SEPARATOR + jarName + JDTUtils.PATH_SEPARATOR + packageName + JDTUtils.PATH_SEPARATOR + classFile.getElementName(), classFile.getHandleIdentifier(), null).toASCIIString();
                        if (uriString == null) {
                            uriString = e.getPath().toString();
                        }
                        Range range = JDTUtils.toRange(e.getOpenable(), e.getNameRange().getOffset(), e.getNameRange().getLength());
                        location = new Location(uriString, range);
                    }
                    symbol.setLocation(location);
                    this.symbols.add(symbol);
                    return;
                } catch (Exception e) {
                    logInfo("match:" + match + " Unable to convert for variable: " + e);
                    return;
                }
            case 3:
                try {
                    IMethod mod = (IMethod)match.getElement();
                    SymbolInformation symbol = new SymbolInformation();
                    symbol.setName(mod.getElementName());
                    symbol.setKind(k);
                    symbol.setContainerName(mod.getParent().getElementName());
                    IClassFile classFile = mod.getClassFile();
		            String packageName = classFile.getParent().getElementName();
		            String jarName = classFile.getParent().getParent().getElementName();
                    String uriString = new URI("jdt", "contents", JDTUtils.PATH_SEPARATOR + jarName + JDTUtils.PATH_SEPARATOR + packageName + JDTUtils.PATH_SEPARATOR + classFile.getElementName(), classFile.getHandleIdentifier(), null).toASCIIString();
                    if (uriString == null) {
                        uriString = mod.getPath().toString();
                    }
                    Range range = JDTUtils.toRange(mod.getOpenable(), mod.getNameRange().getOffset(), mod.getNameRange().getLength());
                    Location loc = new Location(uriString, range);
                    symbol.setLocation(loc);
                    this.symbols.add(symbol);
                    return;
                } catch (Exception e) {
                    logInfo("Location: " + (JDTUtils.toLocation((JavaElement)match.getElement())));
                    logInfo("unable to get method from symbol kind 2 and 3(method and constructor): " + e);
                    return;
                }
            default:
                SymbolInformation symbol = new SymbolInformation();
                symbol.setName(element.getElementName());
                symbol.setKind(k);
                symbol.setContainerName(element.getParent().getElementName());
                Location location = getLocation(element);
                if (location != null) {
                    symbol.setLocation(location);
                }
                this.symbols.add(symbol);
            }
        }

        private Location getLocation(IJavaElement element) {
            try {
                // This casting is safe or is assumed to be safer because the ToString on SearchMatch does it
                return JDTUtils.toLocation(element);
            } catch (Exception e) {
                JavaLanguageServerPlugin.logException("Unable to determine location for the element " + element, e);
                return null;
            }
        }

        private SymbolKind convertSymbolKind(IJavaElement element) {
            switch (element.getElementType()) {
            case IJavaElement.TYPE:
                try {
                    IType type = (IType)element;
                    if (type.isInterface()) {
                        return SymbolKind.Interface;
                    }
                    else if (type.isEnum()) {
                        return SymbolKind.Enum;
                    }
                } catch (JavaModelException ignore) {
                }
                return SymbolKind.Class;
            case IJavaElement.ANNOTATION:
                return SymbolKind.Property; // TODO: find a better mapping
            case IJavaElement.CLASS_FILE:
            case IJavaElement.COMPILATION_UNIT:
                return SymbolKind.File;
            case IJavaElement.FIELD:
                IField field = (IField) element;
                try {
                    if (field.isEnumConstant()) {
                        return SymbolKind.EnumMember;
                    }
                    int flags = field.getFlags();
                    if (Flags.isStatic(flags) && Flags.isFinal(flags)) {
                        return SymbolKind.Constant;
                    }
                } catch (JavaModelException ignore) {
                }
                return SymbolKind.Field;
            case IJavaElement.IMPORT_CONTAINER:
            case IJavaElement.IMPORT_DECLARATION:
                //should we return SymbolKind.Namespace?
            case IJavaElement.JAVA_MODULE:
                return SymbolKind.Module;
            case IJavaElement.INITIALIZER:
                return SymbolKind.Constructor;
            case IJavaElement.LOCAL_VARIABLE:
                return SymbolKind.Variable;
            case IJavaElement.TYPE_PARAMETER:
                return SymbolKind.TypeParameter;
            case IJavaElement.METHOD:
                try {
                    // TODO handle `IInitializer`. What should be the `SymbolKind`?
                    if (element instanceof IMethod) {
                        if (((IMethod) element).isConstructor()) {
                            return SymbolKind.Constructor;
                        }
                    }
                    return SymbolKind.Method;
                } catch (JavaModelException e) {
                    return SymbolKind.Method;
                }
            case IJavaElement.PACKAGE_DECLARATION:
                return SymbolKind.Package;
        }
        return SymbolKind.String;
    }

    public List<SymbolInformation> getSymbols() {
        return this.symbols;
    }
}
