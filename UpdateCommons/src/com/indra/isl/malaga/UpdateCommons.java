package com.indra.isl.malaga;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;



/**
 * UpdateCommons and external libraries
 *
 * @author ajifernandez
 *
 */
public class UpdateCommons {
	/** Number of required input arguments */
	private static final int REQUIRED_ARGS = 4;
	/** To show debug lines **/
	private static final boolean DEBUG_MODE = false;

	/** Classpath file: entry start tag xml */
	private static final String CLASSPATHENTRY_KIND_LIB_PATH_LIB = "<classpathentry kind=\"lib\" path=\"..\\lib\\";

	/** Classpath file: entry end tag xml */
	private static final String EXPORTED_TRUE = "\" exported=\"true\"/>";

	/** Name for Temporal folder */
	private static final String FOLDER_LIB_NEW = "lib-new";

	/** Name for lib folder (where to find already downloaded libraries and copy final versions)*/
	private static final String FOLDER_LIB = "lib";

	/** Name extension of classpath files in the directory **/
	private static final String CLASSPATHFILES = ".classpath";

	/** Name of build gradle files in the directory **/
	private static final String BUILDGRADLEFILE_EXTLIBS = "build.gradle";

	/** Name for temporal classpath files **/
	private static final String CLASSPATHTEMPFILES = ".classpathTemp";

	/** Name for temporal properties files **/
	private static final String COMMONTEMPFILES = ".commonTemp";

	/** Name for temporal properties files **/
	private static final String BUILDGRADLETEMPFILES = ".gradleTemp";

	/** File extension for repository versions info  **/
	private static final String HTML = ".html";

	/** tag to identify external libraries in build gradle file  **/
	private static final String TAG_LIBRERA_EXTERNA = "libreras-externas";

	/** internal tag to update to latest version empty tags in properties field**/
	private static final String TAG_LATEST = "LATEST";

	/** prefix to identify common libraries in .PROPERTIES files**/
	private static final String TAG_COMMONLIBS_PREFIX = "common_";

	/** Lable to identify list of external libraries in .PROPERTIES files**/
	private static final String EXT_LIBS_LABEL = "#EXT_LIBS";

	/** sufix to identify tags in properties and build.gradle**/
	private static final String TAG_LIBS_SUFIX = "_tag";

	/**BUILD GRADLE READING TAGS**/
	private static final String BUILDGRADLE_VERSION_SEPARATOR = "\"";
	private static final String BUILDGRADLE_LIBRARY_SEPARATOR = "\'";
	private static final String BUILDGRADLE_COMPILEGROUP_TAG = "compile group";
	private static final String BUILDGRADLE_RUNTIMEGROUP_TAG = "runtime group";

	/** REPOSITORY folder for common libraries **/
	public static final String REPO_COMMONS_TAG = "/common/";

	/** REPOSITORY folder for external libraries**/
	public static final String REPO_EXTLIBS_TAG = "/libreras-externas/";

	/** MAIN REPOSITORY **/
	public static String repo;
	/** REPOSITORY folder for common libraries **/
	private static String repoCommons;
	/** REPOSITORY folder for external libraries**/
	private static String repoExtLibs;

	private static String user;
	private static String pass;
	private static HttpClient client;
	private static boolean skipLibJars;
	private static Scanner scanner;
	private static final String SLASH = "/";

	/**login dialog**/
	private static JDialog dialog = null;
	private static SwingWorker worker;
	private static JTextField tfUsername ;
	private static JPasswordField pfPassword ;
	private static JOptionPane jop;


	/**
	 * Updates common and external libraries
	 *
	 * Read provided properties files and build gradle files in the WS folder
	 * For each common library and external library reference found:
	 * Checks if the jar is up to date to the latest version in the repository
	 * If required, the new jar is downloaded and copied to the lib folder
	 * Files are updated to reflect changes (classpath, properties and build gradle)
	 *
	 * If the property file contains a _tag without value,
	 * if it is a common library it will throw an error . If it is an
	 * If it is an external library, it will be updated to the latest version of the library (if found).
	 *
	 * If an external library in the build gradle points to an specific version number it is changed to a tag (unless a download failure occurs).
	 *    If the tag exists, uses it (the version specified in the properties).
	 *    If the tag doesn't exists, it is created (in every property file) and its value is set
	 * 	  to the latest version (if found).
	 *
	 * @param args
	 *            Example:
	 *            https://slmaven.mycompany.es/nexus/content/repositories/team/
	 *            ShouldSkipJarsAlreadyInLibs(Yes/no)
	 *            D:\PROJECTS\MY_WS
	 *            D:\PROJECTS\MY_WS\applications\app1\app2.properties
	 *            D:\PROJECTS\MY_WS\applications\app2\app2.properties
	 */
	public static void main(String[] args) {
		if (args.length < REQUIRED_ARGS) {
			System.err.println("Invalid number of arguments. Usage: ");
			System.err.println("args[0] = repository path (https://slmaven.mycompany.es/nexus/content/repositories/product/)");
			System.err.println("args[1] = Yes/No to skip or not to skip jars already present in folder lib");
			System.err.println("args[2] = WS Path (L:/Development/MY_WS)");
			System.err.println("args[3] = Properties file path  (L:/Development/MY_WS/applications/app1/app1.properties)");
			System.err.println("args[n](Optional) = Properties file path (L:/Development/MY_WS/applications/app2/app2.properties)");
		} else {

			initDialog();

			System.out.println(">> Reading input files ");
			//read input arguments
			repo = args[0];
			if (repo.endsWith(SLASH)){
				repo = repo.substring(0,repo.length()-1);
			}
			repoCommons = repo + REPO_COMMONS_TAG;
			repoExtLibs = repo + REPO_EXTLIBS_TAG;

			skipLibJars = false;
			String skipIn = args[1].toUpperCase();
			if (skipIn.equals("YES") || skipIn.equals("Y")) {
				skipLibJars = true;
			}

			String wsPath = args[2];

			//Create temporal folder
			try {
				FileUtils.deleteDirectory(new File(FOLDER_LIB_NEW));
				new File(FOLDER_LIB_NEW).mkdir();
			} catch (IOException e) {
				e.printStackTrace();
			}

			scanner = new Scanner(System.in);

			// READ input property files
			// creates the map of common libraries and external libraries
			String commonFile = null;
			List<String> commonFilesList = new ArrayList<String>();
			Map<String, SortedSet<String>> myExtLibs = new HashMap<String, SortedSet<String>>();
			Map<String, SortedSet<String>> myExtLibsTemp = new HashMap<String, SortedSet<String>>();
			Map<String, SortedSet<String>> commonsLibrariesMap = new HashMap<String, SortedSet<String>>();
			Map<String, SortedSet<String>> commonsLibrariesMapTemp =  new HashMap<String, SortedSet<String>>();
			for (int i = REQUIRED_ARGS - 1; i < args.length; i++) {
				commonFile = args[i];
				commonsLibrariesMapTemp =  new HashMap<String, SortedSet<String>>();
				myExtLibsTemp = new HashMap<String, SortedSet<String>>();
				readCommonsFile(commonsLibrariesMapTemp,myExtLibsTemp,commonFile);
				mergeMap(commonsLibrariesMap,commonsLibrariesMapTemp);
				mergeMap(myExtLibs,myExtLibsTemp);
				commonFilesList.add(commonFile);
			}

			//Print the list of libraries found
			printMap(commonsLibrariesMap);
			printMap(myExtLibs);
			System.out.println(">> " + commonsLibrariesMap.keySet().size() + " common libraries found ");
			System.out.println(">> " + myExtLibs.keySet().size() + " external libraries found ");

			//PRINT WARNING if a library name is contained in more than one library of the list
			//This has been solved using new function "getProjectFromClasspathLine" .... >>>
			if (DEBUG_MODE){
				WarningDuplicates(commonsLibrariesMap);
				WarningDuplicates(myExtLibs);
			}

			//Elapsed time measurement: init
			long tiempoInicio = System.currentTimeMillis();
			DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
			Calendar cal = Calendar.getInstance();
			System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
			System.out.println(">> Starting common libraries update "
					+ dateFormat.format(cal.getTime()));

			// List  of .classpath files in the path
			List<String> classpathFilesList = new ArrayList<String>();
			classpathFilesList = getFilesInDir(wsPath, CLASSPATHFILES);

			// Create http client for querying the repository
			System.setProperty("org.apache.commons.logging.Log",
					"org.apache.commons.logging.impl.NoOpLog");
			client = new HttpClient();
			boolean repoAccessOK = false;
			do {
				//ask for user and password for repo
//				System.out.println(">>Login in "+repo +": ");
//				System.out.println(">user: ");
//				user = scanner.nextLine();
//				System.out.println(">password: ");
//				pass = scanner.nextLine();
				login();

				client.getState().setCredentials(AuthScope.ANY,
						new UsernamePasswordCredentials(user, pass));
				// Updates the map with the latest versions in the repository
				repoAccessOK = updateCommonLibs(commonsLibrariesMap, wsPath);
			}while (!repoAccessOK);

			// Write common library changes to files
			System.out.println(">>Updating common libraries in files ");
			System.out.print("Updating " + commonsLibrariesMap.keySet().size() + " common libraries ");
			System.out.println("in " + classpathFilesList.size() + " classpath files and "+commonFilesList.size() + " properties files" );
			for (String f : classpathFilesList) {
				modifyClasspathFile(commonsLibrariesMap, null, f);
			}
			for (String f : commonFilesList) {
				modifyCommonsFile(commonsLibrariesMap,null, f);
			}

			//Elapsed time: end common libraries update
			cal = Calendar.getInstance();
			long totalTiempo = System.currentTimeMillis() - tiempoInicio;
			System.out.println(">>Elapsed time: common libraries update:  "
					+ dateFormat.format(cal.getTime()) + " in " + totalTiempo
					/ 1000 + " sec");

			//Elapsed time measurement: init extlibs
			long tiempoInicioExtLibs = System.currentTimeMillis();
			System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
			System.out.println(">> Starting external libraries update "
					+ dateFormat.format(cal.getTime()));

			// Reads build gradle files and updates the map of libraries,
			// Updates the newTagsMap with the list of tags to be added to the properties file (if found)
			HashMap<String,String> newTagsMap = new HashMap<String,String>();
			List<String> buildGradleFiles = readAllBuildGradleFiles(wsPath,myExtLibs,newTagsMap);

			// Update external libraries versions from repository
			Map<String, HashMap<String, String>> jarMap = new HashMap<String, HashMap<String,String>>();
			updateExternalLibs(myExtLibs,wsPath, jarMap);

			// Update library info in files (properties, classpath and build gradle)
			System.out.println(">> Updating files ");
			System.out.print("Updating " + myExtLibs.keySet().size() + " external libraries ");
			System.out.println("in " + classpathFilesList.size() + " classpath files y "+buildGradleFiles.size() + " build.gradle files" );
			for (String f : classpathFilesList) {
				modifyClasspathFile(myExtLibs, jarMap, f);
			}
			for (String f : buildGradleFiles) {
				modifyBuildGradleFile(myExtLibs, f);
			}
			for (String f : commonFilesList) {
				modifyCommonsFile(myExtLibs,newTagsMap, f);
			}

			//Elapsed time measurement: end ext libs
			totalTiempo = System.currentTimeMillis() - tiempoInicioExtLibs;
			System.out.println(">> Elapsed time: external libraries update: "
					+ dateFormat.format(cal.getTime()) + " en " + totalTiempo
					/ 1000 + " seg");
			System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

			// Delete old common jars before copying new ones
			System.out.println(">>>>Delete Commons ");
			deleteUpdatedJars(wsPath,commonsLibrariesMap);
			System.out.println(">>>>Delete Ext Libs ");
			deleteUpdatedJars(wsPath,myExtLibs);

			// Copy new updated jars
			System.out.println("Copying new jars");
			copyNewJar(wsPath);

			// Delete temporal folder
			try {
				FileUtils.deleteDirectory(new File(FOLDER_LIB_NEW));
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (scanner != null){
				scanner.close();
			}

			//Elapsed time measurement: end
			cal = Calendar.getInstance();
			totalTiempo = System.currentTimeMillis() - tiempoInicio;
			System.out.println(">> Total elapsed time: common and external libraries update: "
					+ dateFormat.format(cal.getTime()) + " en " + totalTiempo
					/ 1000 + " seg");
		}

	}

//	/**
//	 * Password dialog
//	 * http://blogger.ziesemer.com/2007/03/java-password-dialog.html
//	 * @return
//	 */
//	private static String login() {
////		System.out.println("... loading password dialog ");
////		final JPasswordField jpf = new JPasswordField();
////		JOptionPane jop = new JOptionPane(jpf, JOptionPane.QUESTION_MESSAGE,
////		        JOptionPane.OK_CANCEL_OPTION);
////		JDialog dialog = jop.createDialog("Password:");
////		dialog.addComponentListener(new ComponentAdapter() {
////		    @Override
////		    public void componentShown(ComponentEvent e) {
////		        SwingUtilities.invokeLater(new Runnable() {
////		            @Override
////		            public void run() {
////		                jpf.requestFocusInWindow();
////		            }
////		        });
////		    }
////		});
////
////		dialog.setVisible(true);
////		int result = (Integer) jop.getValue();
////		dialog.dispose();
////		char[] password = null;
////		String res = "";
////		if (result == JOptionPane.OK_OPTION) {
////		    password = jpf.getPassword();
////		    res = String.valueOf(password);
////		}
////		return res;
//	}

	/**
	 * Show Password dialog
	 *
	 */
	private static int login() {
		System.out.println("... loading password dialog ");
		while (dialog == null){
			try {
				System.out.println("...");
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		dialog.setVisible(true);
		int result = (Integer) jop.getValue();
		dialog.dispose();
		if (result == JOptionPane.OK_OPTION) {
			user = tfUsername.getText();
		    pass = String.valueOf(pfPassword.getPassword());
		}
		return result;
	}

	/**
	 * Deletes jars in folder lib that has been updated (downloaded)
	 * @param wsPath
	 * @param myExtLibs
	 */
	private static void deleteUpdatedJars(String wsPath,
			Map<String, SortedSet<String>> libsMap) {

		File folder_lib = new File(wsPath + SLASH + FOLDER_LIB);

		File[] files = folder_lib.listFiles();
		List<File> deleteFiles = new ArrayList<File>();
		String libName = "";
		//Select the jars that has been updated
		if (files != null) {
			for (int x = 0; x < files.length; x++) {
				if (files[x].getName().endsWith(".jar")) {
					libName = getLibraryNameFromJarName(files[x].getName());
					if (libsMap.containsKey(libName)||libsMap.containsKey(libName.replace(".","_"))||libsMap.containsKey(libName.replace("-","_"))){
						deleteFiles.add(files[x]);
						if (DEBUG_MODE){
							System.out.println("DEBUG DELETE FILES: lib : " + libName + "FOUND!" );
							System.out.println("DEBUG DELETE FILES: DELETE THIS FILE : " + files[x].getName());
						}
					}else{
						if (DEBUG_MODE){
							System.out.println("DEBUG DELETE FILES: lib : " + libName + "NOT FOUND!" );
							System.out.println("DEBUG DELETE FILES: no updated : " + files[x].getName());
						}
					}
				}else{
					if (DEBUG_MODE){
						System.out.println("DEBUG DELETE FILES: no jar : " + files[x].getName());
					}
				}
			}
		}
		//Delete them
		for (File f : deleteFiles) {
			f.delete();
		}
	}




	/**
	 * Checks if the libraries in the map contains similar names
	 * (more than one match when checking substring)
	 *
	 * @param map
	 * @return true if has more than one match
	 */
	private static boolean WarningDuplicates(Map<String, SortedSet<String>> map) {
		boolean duplicatedEntries =false;

			List<String> matches;
			int cnt = 0;
			for (String project1 : map.keySet()) {
				cnt = 0;
				matches = new ArrayList<String>();
				for (String project2 : map.keySet()) {
					if (project2.contains(project1) || project2.contains(project1.replace("_","."))|| project2.contains(project1.replace("_","-"))){
						matches.add(project2);
						cnt++;
					}
				}
				if (cnt >1){
					System.out.println("WARNING entry for library "+project1 +" HAS MULTIPLE MATCHES !!!!");
					System.out.println("DEBUG matches: " + matches);
					duplicatedEntries = true;
				}
			}
			return duplicatedEntries;
	}

	/**
	 * Updates the map of common libraries to the
	 * latest version found in the repository
	 *
	 * If a library version is not found in the repository,
	 * it is removed from the original map, so that no changes are made in files
	 *
	 * @param commonsLibrariesMap original map with libraries and the list of required versions to be updated
	 * @param wsPath
	 * @return True if success accessing repository, false otherwise
	 */
	private static boolean updateCommonLibs(Map<String, SortedSet<String>> commonsLibrariesMap,String wsPath) {
		GetMethod method = new GetMethod(repoCommons);
		method.setDoAuthentication(true);
		int executeMethod;
		SortedSet<String> versionSet;
		boolean access_ok = false;
		try {
			executeMethod = client.executeMethod(method);
			if (executeMethod == HttpStatus.SC_OK) {
				access_ok = true;
				// For each library, and each version checks the version in the repository
				for (String project : commonsLibrariesMap.keySet()) {
					versionSet = commonsLibrariesMap.get(project);
					SortedSet<String> versionSetAux = new TreeSet<String>();
					for (String version : versionSet) {
						// Get latest version from repository
						version = getVersion(repoCommons, version, project);
						versionSetAux.add(version);
					}
					//Save changes to the map
					commonsLibrariesMap.replace(project, versionSetAux);
				}

				//if skipLibJars is set to true, check if the jar is already present in the lib folder
				//if so, remove it from the map
				Map<String, SortedSet<String>> commonsLibrariesMapReduced = new HashMap<String, SortedSet<String>>();
				commonsLibrariesMapReduced.putAll(commonsLibrariesMap);
				if (skipLibJars) {
					List<CharSequence> separators = new ArrayList<CharSequence>();
					separators.add("-");
					skipExistingJarInLib(wsPath, commonsLibrariesMap,
							commonsLibrariesMapReduced,separators,null);
				}

				// Download jars from repository
				System.out.println("Downloading " + commonsLibrariesMapReduced.keySet().size() + " common libraries ");
				for (String project : commonsLibrariesMapReduced.keySet()) {
					versionSet = commonsLibrariesMapReduced.get(project);
					List<String> versionNotFound = new ArrayList<String>();
					for (String version : versionSet) {
						if (!downloadJarImplAndSource(repoCommons, version, project)) {
							versionNotFound.add(version);
							System.out.println("Error downloading " + repoCommons
									+ project + SLASH + version + SLASH
									+ getJarName(version, project));
						}else{
							System.out.println("Updated common library: " + repoCommons
									+ project + SLASH + version + SLASH
									+ getJarName(version, project));
						}
					}

					// Remove not found versions from original map
					for (String version : versionNotFound) {
						versionSet.remove(version);
						// Update original map with updated versions
						if (commonsLibrariesMap.get(project) != null) {
							if (versionSet.size() > 0) {
								commonsLibrariesMap.replace(project, versionSet);
							} else {
								commonsLibrariesMap.remove(project);
							}
						} else {
							System.out
									.println("unexpected error lib not found in original map");
						}
					}
				}
			} else if (executeMethod == HttpStatus.SC_UNAUTHORIZED) {
				System.out.println("Unauthorized. Check credentials.");
			} else {
				System.out.println("Http error :" + executeMethod);
			}

		} catch (HttpException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return access_ok;
	}

	/**
	 *  Downloads the specified project specified in the map externalLibs.
	 *  Updates the jarMap with the name of the jar found
	 *  Updates externalLibs removing not found versions
	 *
	 * @param externalLibs map of libraries and versions to download
	 * @param ruta path of the WS (to skip downloaded jars)
	 * @param jarMap map of names of downloaded jars for each project-version downloaded
	 */
	private static void updateExternalLibs(Map<String, SortedSet<String>> externalLibs , String ruta, Map<String, HashMap<String, String>> jarMap) {
		if (externalLibs.size() > 0) {
			String latestVersion = null;
			String jarName = null;
			GetMethod method = new GetMethod(repoExtLibs);
			method.setDoAuthentication(true);
			int executeMethod;
			SortedSet<String> versionSet;

			try {
				executeMethod = client.executeMethod(method);

				if (executeMethod == HttpStatus.SC_OK) {
					// For each library and version, check latest version in the repository
					for (String project : externalLibs.keySet()) {
						versionSet = externalLibs.get(project);
						if (versionSet.size()!=1){
							System.out.println("WARNING! More than 1 version for external library. Discarded, using latest!");
						}
						if (versionSet.first().equals(TAG_LATEST)){
							latestVersion = getLatestVersionExtLib(repoExtLibs,	project);
							versionSet.clear();
							versionSet.add(latestVersion);
							externalLibs.replace(project, versionSet);
						}

					}

					//if skipLibJars is set to true, check if the jar is already present in the lib folder
					//if so, remove it from the map and adds the skipped jarName to the map
					Map<String, SortedSet<String>> externalLibsReduced = new HashMap<String, SortedSet<String>>();
					externalLibsReduced.putAll(externalLibs);
					if (skipLibJars) {
						List<CharSequence> separators = new ArrayList<CharSequence>();
						separators.add("-");
						separators.add(".");
						separators.add("_");
						skipExistingJarInLib(ruta, externalLibs, externalLibsReduced, separators, jarMap);
					}

					// Download required versions and save the jar name of each library and version
					List<String> notFoundVersion = new ArrayList<String>();
					System.out.println("Downloading " + externalLibsReduced.keySet().size() + " external libraries");
					for (String project : externalLibsReduced.keySet()) {
						HashMap<String,String> versionMap = new HashMap<String,String>();
						for (String ver : externalLibsReduced.get(project)){
							if (!ver.endsWith(TAG_LIBS_SUFIX)){ //skip tags
								// try the download
								jarName = downloadJars(project,	ver);
								// if jar found and downloaded, add it to the map, otherwise to the notfound list
								if (jarName != null) {
									versionMap.put(ver,jarName);
								} else {
									notFoundVersion.add(project);
								}
							}
						}
						jarMap.put(project, versionMap);
					}

					// Remove not found versions from map
					for (String f : notFoundVersion) {
						if (externalLibs.containsKey(f)) {
							externalLibs.remove(f);
						}
					}

				} else if (executeMethod == HttpStatus.SC_UNAUTHORIZED) {
					System.out.println("Unauthorized. Check credentials.");
				} else {
					System.out.println("Http error :" + executeMethod);
				}
			} catch (HttpException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("No external libraries found ");
		}
	}

	/**
	 * Downloads the jar for the specified version of the library
	 *
	 * project name is expected to be in the form: name_of_project
	 * the version is expect to be in the form: 1.2.whatever
	 *
	 * the function tries to download:
	 *  name_of_project-1.2.whatever.jar
	 *  name-of-project-1.2.whatever.jar
	 *  name.of.project-1.2.whatever.jar
	 *
	 * @param project name of the library
	 * @param version name of the version
	 * @return the name of the jar downloaded or null if not found
	 */
	private static String downloadJars(String project, String version) {
		String jarName = null;
		if (!downloadJar(repoExtLibs, project.replace("_", "-"), version,
				getJarName(version, project.replace("_", "-")))) {
			if (!downloadJar(repoExtLibs, project.replace("_", "."), version,
					getJarName(version, project.replace("_", ".")))) {
				if (!downloadJar(repoExtLibs, project, version,	getJarName(version, project))){
					System.out.println("Error downloading " + repoExtLibs
							+ project + SLASH + version + SLASH
							+ getJarName(version, project));
						System.out.println("Error downloading " + repoExtLibs
								+ project.replace("_", "-") + SLASH + version + SLASH
								+ getJarName(version, project.replace("_", "-")));
						System.out.println("Error downloading " + repoExtLibs
								+ project.replace("_", ".") + SLASH + version + SLASH
								+ getJarName(version, project.replace("_", ".")));
				}
			} else {
				System.out.println("Updated ext lib ["
						+ project.replace("_", ".") + SLASH + version + SLASH
						+ getJarName(version, project.replace("_", ".")) + "]");
				jarName = getJarName(version, project.replace("_", "."));
			}
		} else {
			System.out.println("Updated ext lib ["
					+ project.replace("_", "-") + SLASH + version + SLASH
					+ getJarName(version, project.replace("_", "-")) + "]");
			jarName = getJarName(version, project.replace("_", "-"));
		}

		return jarName;

	}

	/**
	 * Returns the list of files in directoryName
	 * which names ends with the specified string

	 * @param directoryName
	 * @param endsWithPattern
	 * @return
	 */
	private static List<String> getFilesInDir(String directoryName,
			String endsWithPattern) {
		File directory = new File(directoryName);
		File[] files = directory.listFiles();
		File[] files2;
		ArrayList<String> foundFiles = new ArrayList<String>();
		try {
			if (files != null){
				for (File file : files) {
					if (file.isFile()) {
						if (file.getName().endsWith(endsWithPattern)) {
							foundFiles.add(file.getCanonicalPath());
						}
					} else if (file.isDirectory()) {
						files2 = file.listFiles();
						for (File file2 : files2) {
							if (file2.isFile()) {
								if (file2.getName().endsWith(endsWithPattern)) {
									foundFiles.add(file2.getCanonicalPath());
								}
							}
						}
					}
			}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return foundFiles;
	}

	/**
	 *
	 * Saves the html file with the versions of a library from the repository
	 *
	 * @param repo
	 * @param library
	 * @return Elements of the html file or null if not found
	 */
	private static Elements getElements(String repo, String library) {
		GetMethod method = null;
		Elements commons = null;
		try {
			if (DEBUG_MODE){
				System.out.println("Repository query: ."+repo + library + SLASH);
			}
			method = new GetMethod(repo + library + SLASH);
			method.setDoAuthentication(true);
			int status = client.executeMethod(method);
			if (status == HttpStatus.SC_OK) {
				InputStream in = new BufferedInputStream(
						method.getResponseBodyAsStream());
				File file = new File(FOLDER_LIB_NEW + SLASH
						+ getHtmlName(library));
				OutputStream out = new BufferedOutputStream(
						new FileOutputStream(file));

				IOUtils.copy(in, out);

				out.close();
				in.close();
				Document doc = Jsoup.parse(file, null);
				commons = doc.getElementsByTag("tr");
			} else if (status == HttpStatus.SC_UNAUTHORIZED) {
				System.out.println("Unauthorized. Check credentials.");
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			method.releaseConnection();
		}
		return commons;
	}

	/**
	 *
	 * Looks for the latest version of an external library in the repository
	 *
	 * project name is expected to be in the form: name_of_project
	 * the version is expect to be in the form: 1.2.whatever
	 *
	 * the function look for version of libraries :
	 * 	name_of_project
	 *  name-of-project
	 *  name.of.project
	 *
	 * @param project
	 * @return latestVersion found or null if no version found
	 */
	private static String getLatestVersionExtLib(String repo, String project) {
		String latestVersion = null;

		Elements commons = getElements(repo, project);

		if (commons == null || commons.size() == 0) {
			commons = getElements(repo, project.replace("_", "-"));
		}
		if (commons == null || commons.size() == 0) {
			commons = getElements(repo, project.replace("_", "."));
		}
		if (commons == null || commons.size() == 0) {
			return null;
		}

		SimpleDateFormat sdf = new SimpleDateFormat(
				"EEE MMM dd HH:mm:ss zzz yyyy", new Locale("es_ES"));

		Date versionDate = null;
		for (Element common : commons) {
			if (common.getElementsByTag("td").size() >= 2) {
				// get version name from html elements
				String vaux = common.getElementsByTag("td").get(0)
						.getElementsByTag("td").get(0).select("a").text()
						.toString();
				vaux = vaux.split(SLASH)[0];
				Date vauxDate;
				try {
					//check date
					vauxDate = sdf.parse(common.getElementsByTag("td").get(1)
							.getElementsByTag("td").get(0).text().toString());
					//Warning message for same timestamps
					if ((versionDate == null) || (versionDate.compareTo(vauxDate) == 0)){
						System.out.println("Warning project has multiple versions with the same timestamp!! "+vaux + " version to download is:" +latestVersion);
					}
					if (((versionDate == null) || (versionDate
							.compareTo(vauxDate) < 0)) && !vaux.contains(".xml")) {
						// FIXED Added condition != "xml" because some projects xml files are the most recent files %)
						versionDate = vauxDate;
						latestVersion = vaux;
					}
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}
		return latestVersion;
	}


	/**
	 * Updates build gradle with latest versions specified in the map
	 *
	 * @param maplibraries map of libraries and versions to update
	 * @param buildgradle list of files to update
	 */
	private static void modifyBuildGradleFile(
			Map<String, SortedSet<String>> maplibraries, String buildgradle) {
		FileReader fr;
		BufferedReader br = null;
		FileWriter fw;
		BufferedWriter bw = null;
		String line;

		try {

			String gradleBuildTemp = buildgradle + BUILDGRADLETEMPFILES;

			File buildgradleFile = new File(buildgradle);
			File gradleBuildFileTemp = new File(gradleBuildTemp);

			fw = new FileWriter(gradleBuildFileTemp, true);
			bw = new BufferedWriter(fw);

			fr = new FileReader(buildgradleFile);
			br = new BufferedReader(fr);

			while ((line = br.readLine()) != null) {
				//Check if the line belongs to an external library reference
				if (line.trim().startsWith(
						"compile group: \'" + TAG_LIBRERA_EXTERNA + "\'")
						|| line.trim().startsWith(
								"runtime group: \'" + TAG_LIBRERA_EXTERNA
										+ "\'")) {
					String[] splittedLine = line.split(",");
					//check format
					if (splittedLine.length == 3) {
						try {
							String prj = splittedLine[1];
							//Get name form line
							prj = prj.substring(
											prj.indexOf(BUILDGRADLE_LIBRARY_SEPARATOR) + 1,
											prj.lastIndexOf(BUILDGRADLE_LIBRARY_SEPARATOR));
							//Get version from file
							String oldVersion = splittedLine[2];
							//If the file version is a tag, keep it. Otherwise, replace with the new tag.
							if (!oldVersion.contains(TAG_LIBS_SUFIX)) {
								oldVersion = oldVersion.substring(
												oldVersion.indexOf(BUILDGRADLE_VERSION_SEPARATOR) + 1,
												oldVersion.lastIndexOf(BUILDGRADLE_VERSION_SEPARATOR));
								//check that the library is in the map (has to be updated in the file)
								if (maplibraries.containsKey(prj)) {
									//add the NEW tag for this project
									//FIXME:Get tag from map ?
									line = line.replace( BUILDGRADLE_VERSION_SEPARATOR + oldVersion + BUILDGRADLE_VERSION_SEPARATOR,prj+TAG_LIBS_SUFIX);
											bw.write(line);
									bw.write("\n");
								} else {
									System.out.println("Warning library " + prj + " in file "+buildgradle + " havent been proccesed ! check download error ");
									bw.write(line);
									bw.write("\n");
								}
							} else {
								//System.out.println("DEBUG NO CHANGES IN LINE : " + line  + " ! ");
								bw.write(line);
								bw.write("\n");
							}

						} catch (IndexOutOfBoundsException e) {
							System.out
									.println("Unexpected format Reading build gradle: "
											+ buildgradle);
							System.out.println("Line: " + line);
						}
					} else {
						System.out
								.println("Unexpected format Reading build gradle: "
										+ buildgradle);
						System.out.println("Line: " + line);
					}
				} else {
					bw.write(line);
					bw.write("\n");
				}
			}
			br.close();
			bw.close();
			// delete original file
			buildgradleFile.delete();
			// Copy new temporal to original file
			copyFileUsingStream(gradleBuildFileTemp, buildgradleFile);
			// delete temporal file
			gradleBuildFileTemp.delete();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) {
					br.close();
				}
				if (bw != null) {
					bw.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	/**
	 * Looks for build gradle files in the provided path
	 * Read files found looking for external libraries
	 * Updates the map with new required libraries
	 * Adds new tags to the map (for new libraries not found in the properties file)
	 *
	 * @param path of the project
	 * @param myExtLibs map of external libraries
	 * @param newTagsMap map of new tags required
	 * @return the list of build gradle files found
	 */
	private static List<String>  readAllBuildGradleFiles(String path,  Map<String, SortedSet<String>> myExtLibs, HashMap<String,String> newTagsMap){
		Map<String, SortedSet<String>> externalLibs = new HashMap<String, SortedSet<String>>();
		List<String> buildGradleFiles = getFilesInDir(path,	BUILDGRADLEFILE_EXTLIBS);
		if (buildGradleFiles.size() > 0) {
			// Read build gradle looking for external libraries
			Map<String, SortedSet<String>> myExtLibsTemp = new HashMap<String, SortedSet<String>>();
			for (String f : buildGradleFiles) {
				myExtLibsTemp = new HashMap<String, SortedSet<String>>();
				readBuildGradleFile(f, BUILDGRADLE_COMPILEGROUP_TAG, myExtLibsTemp);
				mergeMap(externalLibs,myExtLibsTemp);
				myExtLibsTemp = new HashMap<String, SortedSet<String>>();
				readBuildGradleFile(f, BUILDGRADLE_RUNTIMEGROUP_TAG, myExtLibsTemp);
				mergeMap(externalLibs,myExtLibsTemp);
			}
			System.out.println(externalLibs.keySet().size() + " external libraries found in build.gradle ");
			//Update ExtLib map with new required libraries found in gradle files
			for ( String project : externalLibs.keySet()){
				//If it was already in the map discard info from build
				if (!myExtLibs.containsKey(project)){
					//Add latest version of found library
					myExtLibs.put(project, new TreeSet<String>());
					myExtLibs.get(project).add(TAG_LATEST);
					newTagsMap.put(project, TAG_LATEST);
				}
			}
		} else {
			System.out.println("No "
					+ BUILDGRADLEFILE_EXTLIBS + " files found in " + path);
		}
		return buildGradleFiles;
	}

	/**
	 * Reads buildgradle file looking for references to external libraries
	 * and add them to the map
	 *
	 * A line refers to an external library if contains a string:
	 * group + ": \'" + TAG_LIBRERA_EXTERNA+ "\'"
	 * where group is a parameter : "compile group" or "runtime group"
	 *
	 * @param ruta path of buildgradle file to read
	 * @param group name to search
	 * @param externalLibs map of libraries to update
	 */
	private static void readBuildGradleFile(String path, String group,
			 Map<String, SortedSet<String>> externalLibsTemp) {
		BufferedReader br = null;
		try {
			String buildgradleFile = path;
			FileReader fr = new FileReader(buildgradleFile);
			br = new BufferedReader(fr);
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.startsWith(group + ": \'" + TAG_LIBRERA_EXTERNA+ "\'")) {
					String[] splittedLine = line.split(",");

					if (splittedLine.length == 3) {
						try {
							String prj = splittedLine[1];
							prj = prj.substring(prj.indexOf(BUILDGRADLE_LIBRARY_SEPARATOR) + 1,
									prj.lastIndexOf(BUILDGRADLE_LIBRARY_SEPARATOR));

							String ver = splittedLine[2];
							if (!ver.contains(TAG_LIBS_SUFIX)){
								ver = ver.substring(ver.indexOf(BUILDGRADLE_VERSION_SEPARATOR) + 1,
										ver.lastIndexOf(BUILDGRADLE_VERSION_SEPARATOR));

							}else{
								ver = ver.substring(ver.indexOf(":")+1,ver.length());
							}

							if (externalLibsTemp.get(prj.trim()) != null) {
								StringBuilder errorBuilder = new StringBuilder();
								throw new RuntimeException(
										errorBuilder
												.append(" Library reference  ")
												.append(prj.trim())
												.append(" duplicated in file: ")
												.append(buildgradleFile).toString());
							}
							SortedSet<String> sortedSet = externalLibsTemp.get(prj.trim());
							if (sortedSet == null) {
								sortedSet = new TreeSet<String>();
							}
							sortedSet.add(ver.trim());
							//if it is not a tag, adds it
							if (!sortedSet.contains(prj.trim()+TAG_LIBS_SUFIX)){
								sortedSet.add(prj.trim()+TAG_LIBS_SUFIX);
							}
							externalLibsTemp.put(prj.trim(), sortedSet);

						} catch (IndexOutOfBoundsException e) {
							System.out
									.println("Unexpected format reading build gradle: "
											+ path);
							System.out.println("Line: " + line);
						}

					} else {
						System.out
								.println("Unexpected format reading build gradle: "
										+ path);
						System.out.println("Line: " + line);
					}
				}
			}
			br.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {

				e.printStackTrace();
			}
		}

	}

	/**
	 * Merges info from two map of libraries and versions omitting duplicates
	 *
	 * @param original map to be updated
	 * @param newMap map to add to the orignal map
	 */
	private static void mergeMap(Map<String, SortedSet<String>> original,
			Map<String, SortedSet<String>> newMap) {

		for (String key : newMap.keySet()) {
			if (original.containsKey(key)) {
				SortedSet<String> originalSet = original.get(key);
				for (String version : newMap.get(key) ){
					//	if the version is in the set, don't add it
					if (!originalSet.contains(version)){
						originalSet.add(version);
					}
				}
				original.put(key, originalSet);
			} else {
				original.put(key, newMap.get(key));
			}
		}
	}

	/**
	 * Copies files from temporal folder (FOLDER_LIB_NEW) to lib folder (FOLDER_LIB)
	 *
	 * @param ruta
	 */
	private static void copyNewJar(String ruta) {
		File folder_lib_new = new File(FOLDER_LIB_NEW);
		File[] ficheros_new = folder_lib_new.listFiles();
		System.out.println("Copying files to folder "+ruta + SLASH + FOLDER_LIB );
		for (int x = 0; x < ficheros_new.length; x++) {
			File file_common = new File(ruta + SLASH + FOLDER_LIB + SLASH
					+ ficheros_new[x].getName());
			try {
				if (!file_common.getPath().endsWith(HTML)) {
					FileUtils.copyFile(ficheros_new[x], file_common);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Prints the map of libraries and versions
	 *
	 * @param commons
	 */
	private static void printMap(Map<String, SortedSet<String>> commons) {
		for (String key : commons.keySet()) {
			System.out.println(key + " - " + commons.get(key));
		}
	}

	/**
	 * Updates classpath file with latest library versions specified in the map
	 *
	 * Check if the classpath reference any library from the map
	 *
	 * it checks for every line if the line contains the name of the project ( library)
	 * also checks replacing "_" for "." and "-"
	 *
	 * if the line contains the string, the line is updated with the path to the latest version
	 *
	 * @param mapVersions mapa of libraries and versions
	 * @param mapJars map with the name of the jar (if null uses getJarName function)
	 * @param classPathFilePath file to update
	 */
	private static void modifyClasspathFile(Map<String,SortedSet<String>> mapVersions,
			Map<String, HashMap<String, String>>  mapJars, String classPathFilePath) {
		FileReader fr;
		BufferedReader br = null;
		FileWriter fw;
		BufferedWriter bw = null;
		String s;

		try {
			File classPathFile = new File(classPathFilePath);
			File classPathFileTemp = new File(classPathFilePath	+ CLASSPATHTEMPFILES);

			fw = new FileWriter(classPathFileTemp, true);
			bw = new BufferedWriter(fw);

			fr = new FileReader(classPathFile);
			br = new BufferedReader(fr);
			SortedSet<String> versionSet;

			boolean copyLine;
			String project;
			while ((s = br.readLine()) != null) {
				if (!s.equals("")){
					copyLine = true;
					project = getProjectFromClasspathLine(s, mapVersions);
					if (project !=null){
							String version = "";
							versionSet = mapVersions.get(project);
								if (versionSet.size() > 1) {
									boolean valid = false;
									while (!valid) {
										String path = classPathFile
												.getAbsolutePath();
										String paths = path.substring(0,
												path.lastIndexOf(".classpath") - 1);
										System.out
												.println("Multiple versions for entry :  "
														+ paths);
										System.out
												.println("Choose version:");
										int i = 0;
										Iterator<String> iteratorSet = versionSet
												.iterator();
										while (iteratorSet.hasNext()) {
											System.out.println(i + " - "
													+ iteratorSet.next());
											i++;
										}
										int choice = -1;

										String lineScan = "";
										try {
											lineScan = scanner.nextLine();
											choice = Integer.parseInt(lineScan);
											if (!(choice >= 0 && choice < i)){
												System.out.println("ERROR unvalid option");
											}
										} catch (NumberFormatException e) {
										    System.out.println("ERROR unknown option");
										}

										i = 0;
										iteratorSet = versionSet.iterator();
										while (iteratorSet.hasNext()) {
											if (i == choice) {
												version = iteratorSet.next();
												valid = true;
												break;
											}
											iteratorSet.next();
											i++;
										}
									}
								} else {
									version = versionSet.first();
								}
							bw.write("\t");
							if (mapJars != null) {
								if ((mapJars.containsKey(project)) && (mapJars.get(project).containsKey(version))) {
									bw.write(CLASSPATHENTRY_KIND_LIB_PATH_LIB	+ mapJars.get(project).get(version) + EXPORTED_TRUE);
								//}//else the jar was skipped from download, dont know the name, so no change are made in file
								//else if ((mapJars.containsKey(project)) && (mapJars.get(project).containsKey(version+TAG_LATEST))) {
									//FIXME Remove??
									// System.out.println("VERSION TAGGED AS LATEST!!");
									//bw.write(CLASSPATHENTRY_KIND_LIB_PATH_LIB	+ mapJars.get(project).get(version+TAG_LATEST) + EXPORTED_TRUE);
								}else {
								    System.out.println("UNKNOWN JAR NAME ??? " + project + " version " + version);
								}
							} else {
								bw.write(CLASSPATHENTRY_KIND_LIB_PATH_LIB
										+ getJarName(version, project)
										+ EXPORTED_TRUE);
							}
							bw.write("\n");
							copyLine = false;
						}
					if (copyLine) {
						bw.write(s);
						bw.write("\n");
					}
				}
			}
			br.close();
			bw.close();

			classPathFile.delete();
			copyFileUsingStream(classPathFileTemp, classPathFile);
			classPathFileTemp.delete();

		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (UnsupportedEncodingException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			try {
				if (br != null) {
					br.close();
				}
				if (bw != null) {
					bw.close();
				}
			} catch (IOException e) {

				e.printStackTrace();
			}
		}
	}

	/**
	 * Returns the name of the library that matches the line of the classpath provided.
	 *
	 * Line is expected to be in the form
	 *  <classpathentry kind="lib" path="..\lib\saxon8-XXX.jar" exported="true"/>
	 *  linePath is the line substring between "path=" and ".jar";
	 *  lineJarName the linePath substring between " \" and ".jar"; or "/" ??
	 *  library is obtained using getLibraryNameFromJarName
	 *
	 * If the library is found in the map (or any of its variations) it is returned
	 * otherwise returns null.
	 *
	 * @param line
	 * @param mapVersions
	 * @return the library found in the line and in the map, null otherwise
	 */
	private static String getProjectFromClasspathLine(String line,
			Map<String, SortedSet<String>> mapVersions) {

		String key = null;
		//expected lines :
		//<classpathentry kind="lib" path="..\lib\saxon8-XXX.jar" exported="true"/>
		//<classpathentry kind="lib" path="..\lib\saxon8-dom-XXX.jar" exported="true"/>
		if (line.contains(".jar") && line.contains(" path=") ){
			try {
				if (DEBUG_MODE) System.out.println("DEBUG line : " + line);
				String linePath = line.substring(line.indexOf(" path=")+7,line.length());
				linePath = linePath.substring(0,linePath.indexOf(".jar")+4);
				if (DEBUG_MODE) System.out.println("DEBUG line path : " + linePath);
				String lineJarName;
				if (linePath.lastIndexOf("\\") != -1){
					lineJarName  = linePath.substring(linePath.lastIndexOf("\\")+1,linePath.length());
				}else{
					lineJarName  = linePath.substring(linePath.lastIndexOf(SLASH)+1,linePath.length());
				}
				if (DEBUG_MODE) System.out.println("DEBUG linejarname: " + lineJarName);
				String library = getLibraryNameFromJarName(lineJarName);
				if (DEBUG_MODE) System.out.println("DEBUG library: " + library);
				if (mapVersions.containsKey(library)){
					key = library;
				}
				if (mapVersions.containsKey(library.replace(".","_"))){
					key = library.replace(".","_");
				}
				if ( mapVersions.containsKey(library.replace("-","_"))){
					key = library.replace("-","_");
				}
				if (DEBUG_MODE) System.out.println("DEBUG library in map is : " + library);
			}catch( IndexOutOfBoundsException e){
				if (DEBUG_MODE) System.out.println(" Unexpected line in classpath: " + line);
				if (DEBUG_MODE) System.out.println(e);
			}
		}
		return key;
	}




	/***
	 * Reads property file looking for properties for common or external libraries
	 * and add them to the map
	 *
	 * A propety line refers to a library if it ends with "_tab" (TAG_LIBS_SUFIX)
	 * if also starts with "common_" (TAG_COMMONLIBS_PREFIX) it is a common library
	 * otherwise an external library
	 *
	 * @param mapCommons map of common libraries to update
	 * @param mapExtLibs map of external libraries to update
	 * @param commonFile path of the file to read
	 */
	private static void readCommonsFile( Map<String, SortedSet<String>> mapCommons,  Map<String, SortedSet<String>> mapExtLibs,
			String commonFile) {

		BufferedReader br = null;
		try {
			FileReader fr = new FileReader(commonFile);
			br = new BufferedReader(fr);
			String line;
			String[] splittedLine;
			String ver;
			boolean extLibsStarted =false;
			while ((line = br.readLine()) != null) {
				if (line.contains(EXT_LIBS_LABEL)){
					extLibsStarted = true;
				}
				if (!line.startsWith("#")) {
					splittedLine = line.split("=");
					String prj = splittedLine[0].replaceAll(TAG_LIBS_SUFIX, "");
					// check if it is a library
					if (splittedLine[0].endsWith(TAG_LIBS_SUFIX)) {
						// check if it is a common library
						if (splittedLine[0].startsWith(TAG_COMMONLIBS_PREFIX)) {
							ver = splittedLine[1];
							if (mapCommons.get(prj.trim()) != null) {
								StringBuilder errorBuilder = new StringBuilder();
								throw new RuntimeException(
										errorBuilder
												.append(" Tag for ")
												.append(prj.trim())
												.append(" duplicated !!  in file : ")
												.append(commonFile).toString());
							}
							SortedSet<String> sortedSet = mapCommons.get(prj
									.trim());
							if (sortedSet != null) {
							} else {
								sortedSet = new TreeSet<String>();
							}
							sortedSet.add(ver.trim());
							mapCommons.put(prj.trim(), sortedSet);
						} else {
							//if label EXT_LIBS already read
							if (extLibsStarted){
								// otherwise must be external library
								if (splittedLine.length > 1) {
									ver = splittedLine[1];
								} else {
									ver = TAG_LATEST;
								}
								if (mapExtLibs.get(prj.trim()) != null) {
									StringBuilder errorBuilder = new StringBuilder();
									throw new RuntimeException(
											errorBuilder
													.append(" Tag for  ")
													.append(prj.trim())
													.append(" duplicated !!  in file : ")
													.append(commonFile).toString());
								}

								SortedSet<String> sortedSet = mapExtLibs
										.get(prj.trim());
								if (sortedSet != null) {
								} else {
									sortedSet = new TreeSet<String>();
								}
								sortedSet.add(ver.trim());
								mapExtLibs.put(prj.trim(), sortedSet);
							}
						}
					}//SKIP LINE
				}//SKIP LINE
			}
			if (!extLibsStarted){
				System.err.println("Warning! "+ EXT_LIBS_LABEL +" label not found in property file! ");
			}
			br.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {

				e.printStackTrace();
			}
		}

	}

	/**
	 * Updates properties file with latest library versions specified in the library map
	 * and adds new tags specified in the tag map
	 * Check if the classpath reference any library from the map
	 *
	 * A propety line refers to a library if it ends with "_tab" (TAG_LIBS_SUFIX)
	 * if also starts with "common_" (TAG_COMMONLIBS_PREFIX) it is a common library
	 * otherwise an external library
	 *
	 * External libraries should be updated to tags (if they dont exists, they are
	 * added and set to the latest version in the repository)
	 *
	 * @param map library map
	 * @param newTagsMap new tags map
	 * @param commonsFilePath properties file to modify
	 */
	private static void modifyCommonsFile(Map<String, SortedSet<String>> map, HashMap<String, String> newTagsMap,
			String commonsFilePath) {

		FileReader fr;
		BufferedReader br = null;
		FileWriter fw;
		BufferedWriter bw = null;
		String s;
		try {
			File commonsFile = new File(commonsFilePath);
			File commonsFileTemp = new File(commonsFilePath + COMMONTEMPFILES);

			fw = new FileWriter(commonsFileTemp, true);
			bw = new BufferedWriter(fw);

			fr = new FileReader(commonsFile);
			br = new BufferedReader(fr);

			SortedSet<String> fVersionSet;
			String fileProject = null;
			String[] splittedLine;
			String version ="";
			boolean extLibsStarted = false;
			// for each line check if contains a project tag to update
			while ((s = br.readLine()) != null) {
				if (s.contains(EXT_LIBS_LABEL)){
					extLibsStarted = true;
				}
				fileProject = (s.contains(TAG_LIBS_SUFIX) && (!s.contains("#"))) ? s
						.substring(0, s.indexOf(TAG_LIBS_SUFIX)) : null;
				if (map.containsKey(fileProject)) {
					fVersionSet = map.get(fileProject);
					splittedLine = s.split("=");
					//if version has value save it in version otherwise is empty
					if (splittedLine.length > 1){
						version = s.split("=")[1];
					}
					//if commons, skip empty versions
					if (fileProject.contains(TAG_COMMONLIBS_PREFIX)){
						if (version == "") {
							StringBuilder errorBuilder = new StringBuilder();
							throw new RuntimeException(
									errorBuilder
											.append("Error. File  ")
											.append(commonsFile)
											.append(" has no value for : ")
											.append(fileProject).toString());
						}else{
							// Look for updated version
							for (String fVersion : fVersionSet) {
								if (fVersion.contains("-" + version)) {
									version = fVersion;
								}
							}
						}
					} else{
						// if external library
						if (version == ""){
							if (fVersionSet.size()>1){
								System.out.println("WARNING! unexpected number of versions for Ext Lib! "+fileProject + " found:"+fVersionSet.size());
							}
							version = fVersionSet.first();
						}

					}
					// update tag version
					bw.write(fileProject +TAG_LIBS_SUFIX+ "=" + version);
					bw.write("\n");
				} else {
					bw.write(s);
					bw.write("\n");
				}
			}
			// Add new tags (if required)
			if (newTagsMap!=null){
				//add label for external libs in file
				if (!extLibsStarted){
					System.err.println("Warning! New label:"+EXT_LIBS_LABEL+ " added for external tags! Check property file." + commonsFilePath);
					bw.write(EXT_LIBS_LABEL);
					bw.write("\n");
				}
				for (String key : newTagsMap.keySet()){
					if (map.containsKey(key)){
						if (map.get(key).size()>1){
							System.out.println("WARNING! unexpected number of versions for Ext Lib! key:" + key);
						}
						bw.write(key +TAG_LIBS_SUFIX+ "=" + map.get(key).first());
						bw.write("\n");
					}else{
						// download of new tag failed? empty tag
						bw.write(key +TAG_LIBS_SUFIX+ "=");
						bw.write("\n");
					}
				}
			}

			br.close();
			bw.close();

			commonsFile.delete();
			copyFileUsingStream(commonsFileTemp, commonsFile);
			commonsFileTemp.delete();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) {
					br.close();
				}
				if (bw != null) {
					bw.close();
				}
			} catch (IOException e) {

				e.printStackTrace();
			}
		}
	}

	/**
	 * Copies the file from file source to file destiny
	 *
	 * @param source
	 * @param des
	 * @throws IOException
	 */
	private static void copyFileUsingStream(File source, File dest)
			throws IOException {
		InputStream is = null;
		OutputStream os = null;
		try {
			is = new FileInputStream(source);
			os = new FileOutputStream(dest);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = is.read(buffer)) > 0) {
				os.write(buffer, 0, length);
			}
		} finally {
			is.close();
			os.close();
		}
	}

	/**
	 * Downloads implementation jar and source jar
	 * for a given project and version in the repository
	 *
	 * @param version version to download
	 * @param project project to download
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private static boolean downloadJarImplAndSource(String repo,
			String version, String project) {

		return downloadJar(repo, project, version, getJarName(version, project))
				&& (downloadJar(repo, project, version,
						getSourceJarName(version, project)) || downloadJar(
						repo, project, version,
						getSourceJarName2(version, project)));
	}

	/**
	 * Downloads the jar for the given version, and project from the specified repository
	 *
	 * @param project
	 * @param version
	 * @param jarName
	 */
	private static boolean downloadJar(String repo, String project,
			String version, String jarName) {
		GetMethod method = null;
		boolean res = false;
		try {
			method = new GetMethod(repo + project + SLASH + version + SLASH
					+ jarName);
			// method = new GetMethod(WEB + project);
			method.setDoAuthentication(true);

			int status = client.executeMethod(method);
			if (status == HttpStatus.SC_OK) {
				InputStream in = new BufferedInputStream(
						method.getResponseBodyAsStream());
				OutputStream out = new BufferedOutputStream(
						new FileOutputStream(new File(FOLDER_LIB_NEW + SLASH
								+ jarName)));
				IOUtils.copy(in, out);

				out.close();
				in.close();
				res = true;
			} //como puede fallar al probar rutas distintas no muestro error ...
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			method.releaseConnection();
		}
		return res;
	}

	/**
	 * Returns the expected name of the jar for a given project and version
	 *
	 * @param version
	 *            version of the project
	 * @param project
	 *            Name of the library/project
	 * @return Expected name of the Jar
	 */
	private static String getJarName(String version, String project) {
		return project + "-" + version + ".jar";
	}

	/**
	 * Returns the expected name of the source jar for a given project and version
	 * Obtiene el nombre del fichero jar de los ficheros fuentes según versión y
	 * proyecto dados
	 *
	 * @param version Version of the project
	 * @param project Name of the library/project
	 * @return Expected name of the source Jar
	 */
	private static String getSourceJarName(String version, String project) {
		return project + "-" + version + "-sources.jar";
	}

	/**
	 * Returns (Another) expected name of the source jar for a given project and version
	 * Obtiene el nombre del fichero jar de los ficheros fuentes según versión y
	 * proyecto dados
	 *
	 * @param version Version of the project
	 * @param project Name of the library/project
	 * @return Expected name of the source Jar
	 */
	private static String getSourceJarName2(String version, String project) {
		return project + "-" + version + "-src.jar";
	}

	/**
	 * Returns the latest jar for the specified project and version in the repository
	 *
	 * @param repo
	 * @param version
	 * @param project
	 * @return
	 */
	private static String getVersion(String repo, String version, String project) {
		String res = version;

		// Get elements from repo
		Elements commons = getElements(repo, project);

		SimpleDateFormat sdf = new SimpleDateFormat(
				"EEE MMM dd HH:mm:ss zzz yyyy", new Locale("es_ES"));

		Date versionDate = null;

		for (Element common : commons) {
			if (common.getElementsByTag("td").size() >= 2) {
				//get the name of the version
				String vaux = common.getElementsByTag("td").get(0)
						.getElementsByTag("td").get(0).select("a").text()
						.toString();
				vaux = vaux.split(SLASH)[0];

				if (DEBUG_MODE)  System.out.println("DEBUG vaux: "+vaux);
				// check if it is the version we are looking for
				if (vaux.contains("-" + version)) {
					// System.out.println("DEBUG version FOUND!  ");
					Date vauxDate;
					try {
						// check date to get latest jar
						vauxDate = sdf.parse(common.getElementsByTag("td")
								.get(1).getElementsByTag("td").get(0).text()
								.toString());
						if (DEBUG_MODE)  System.out.println("DEBUG version DATE  "+vauxDate);
						// warnign message for same timestamp
						if ((versionDate == null) || (versionDate.compareTo(vauxDate) == 0)){
							System.out.println("Warning project has multiple versions with the same timestamp!! "+vaux + " version to download is:" +res);
						}
						if ((versionDate == null) || (versionDate.compareTo(vauxDate) < 0) && !vaux.contains(".xml")) {
							// FIX Added condition != "xml" because some projects xml files are the most recent files %)
							versionDate = vauxDate;
							res = vaux;
							if (DEBUG_MODE)  System.out.println("DEBUG newer version found: "+vaux);
						}
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}

			}
		}
		if (DEBUG_MODE) System.out.println("DEBUG newest version : "+res);
		return res;
	}

	/**
	 * Returns the expected name of the html file for a given project / library
	 *
	 * @param project Name of the project / library
	 * @return Name of the html file
	 */
	private static String getHtmlName(String project) {
		return project +HTML;
	}

	/**
	 * Gets the name of the version from the jar name
	 * @param jarName
	 * @return version
	 */
	private static String getVersionFromJarName(String jarName){
		String versionJar = null;
		int index1 = 0;
		int index2 = 0;
		if (jarName.startsWith(TAG_COMMONLIBS_PREFIX)){
			index1 = jarName.indexOf("-");
		}else{
			index1 = jarName.lastIndexOf("-");
		}
		index2 = jarName.lastIndexOf(".jar");
		versionJar = jarName.substring(index1 + 1,index2);
		return versionJar;
	}

	/**
	 * Gets the name of the library from the jar name
	 *
	 * @param jarName
	 * @return project
	 */
	private static String getLibraryNameFromJarName(String jarName){
		String libraryNameJar = null;
		int index1 = 0;
		if (jarName.startsWith(TAG_COMMONLIBS_PREFIX)){
			index1 = jarName.indexOf("-");
		}else{
			index1 = jarName.lastIndexOf("-");
		}
		libraryNameJar =jarName.substring(0, index1);
		return libraryNameJar;
	}

	/**
	 *
	 * Checks for each jar in the lib folder
	 * if they are in the map of libraries to download
	 * if so, remove it from the map
	 *
	 * @param wsPath
	 * @param libraryMap
	 * @param libraryMapReduced
	 * @param replacements
	 */
	private static void skipExistingJarInLib(String wsPath, Map<String, SortedSet<String>> libraryMap, Map<String, SortedSet<String>> libraryMapReduced,
			List<CharSequence> replacements, Map<String, HashMap<String, String>> jarMap) {
		File directory = new File((String) wsPath + SLASH + FOLDER_LIB);
		File[] contents = directory.listFiles();
		int cnt = 0;
		String jarName = null;

		SortedSet<String> versionList;
		String commonNameFromJar = null;
		String commonVersionFromJar  = null;
		if (contents!=null){
			for (File f : contents) {
				jarName = f.getName();
				//check only jar files in folder lib
				if (jarName.contains(".jar")) {
					commonVersionFromJar = getVersionFromJarName(jarName);
					commonNameFromJar = getLibraryNameFromJarName(jarName);
					if (commonVersionFromJar != null && commonNameFromJar !=null) {
						for (CharSequence s : replacements) {
							commonNameFromJar = commonNameFromJar.replace(s,"_");
							if (DEBUG_MODE) System.out.println("DEBUG JAR searching... "+ commonNameFromJar);
							if (libraryMap.containsKey(commonNameFromJar)) {
								if (DEBUG_MODE) System.out.println("DEBUG  JAR found "+ jarName);
									versionList = libraryMap.get(commonNameFromJar);
									// Remove version from map
									if (versionList.contains(commonVersionFromJar)) {
										if (versionList.size() == 1) {
											// If no moree versions, remove the project / library
											libraryMapReduced.remove(commonNameFromJar);
										} else {
											versionList.remove(commonVersionFromJar);
										}
										//Add to the map of Jars (if passed )
										if (jarMap !=null){
											if (jarMap.containsKey(commonNameFromJar)){
												jarMap.get(commonNameFromJar).put(commonVersionFromJar,jarName);
											}else{
												HashMap<String,String> vers = new HashMap<String,String>();
												vers.put(commonVersionFromJar,jarName);
												jarMap.put(commonNameFromJar, vers);
											}
										}
										cnt++;
									}
								break;
							}
						}
					}
				}
			}
		}
		System.out.println(cnt
				+ " jars found in lib that will not be downloaded");
		if (libraryMapReduced.size() == cnt) {
			System.out.println("Any library need no be updated. ");
		}
	}

	/**
	 * swingworker for login dialog
	 */
	private static void initDialog(){
		 worker = new SwingWorker<JDialog, Void>() {
		    @Override
		    public JDialog doInBackground() {
		        JPanel panel = new JPanel(new GridBagLayout());
			    GridBagConstraints cs = new GridBagConstraints();

			    cs.fill = GridBagConstraints.HORIZONTAL;

			    JLabel lbUsername = new JLabel("Username: ");
			    cs.gridx = 0;
			    cs.gridy = 0;
			    cs.gridwidth = 1;
			    panel.add(lbUsername, cs);

			    tfUsername = new JTextField(20);
			    cs.gridx = 1;
			    cs.gridy = 0;
			    cs.gridwidth = 2;
			    panel.add(tfUsername, cs);

			    JLabel lbPassword = new JLabel("Password: ");
			    cs.gridx = 0;
			    cs.gridy = 1;
			    cs.gridwidth = 1;
			    panel.add(lbPassword, cs);

			    pfPassword = new JPasswordField(20);
			    cs.gridx = 1;
			    cs.gridy = 1;
			    cs.gridwidth = 2;
			    panel.add(pfPassword, cs);

				jop = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE,
				        JOptionPane.OK_CANCEL_OPTION);
				dialog = jop.createDialog("Login SVN:");
		        return dialog;
		    }
		};
		worker.execute();
	}
}

