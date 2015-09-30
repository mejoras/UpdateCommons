package com.indra.isl.malaga;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * clase principal de la aplicación de consola
 * 
 * @author ajifernandez
 *
 */
public class UpdateCommons {
	private static final int REQUIRED_ARGS = 4;
	/** Inicio entrada xml */
	private static final String CLASSPATHENTRY_KIND_LIB_PATH_LIB = "<classpathentry kind=\"lib\" path=\"..\\lib\\";
	/** Fin entrada xml */
	private static final String EXPORTED_TRUE = "\" exported=\"true\"/>";
	/** Carpeta de librerías */
	private static final String FOLDER_LIB_NEW = "lib-new";

	/** Página web */
	// public static final String WEB =
	// "http://cvsdv20:9090/repo_ivy/davinci20/common/";
	public static final String WEB = "https://slmaven.indra.es/nexus/content/repositories/davinci20/common/";
	private static final String SLASH = "/";
	private static final int LOGGED = 200;
	private static final String HTML = ".html";
	private static String user;
	private static String pass;
	private static HttpClient client;

	/**
	 * Método principal
	 * 
	 * @param args
	 *            Ejemplo L:/Development/WS_SGC_old
	 *            L:\Development\WS_SGC_old\applications
	 *            \sgc_client\sgc_client.properties
	 */
	public static void main(String[] args) {
		if (args.length < REQUIRED_ARGS) {
			System.err.println("Número de argumentos inválido");
			System.err
					.println("args[0] = username de https://slmaven.indra.es");
			System.err.println("args[1] = pass de https://slmaven.indra.es");
			System.err.println("args[2] = Ruta WS (ej: L:/Development/WS_SGC)");
			System.err
					.println("args[3] = Ruta fichero commons (ej: L:/Development/WS_SGC/applications/sgc_client/sgc_client.properties)");
			System.err
					.println("args[n](Opcional) = Ruta fichero commons (ej: L:/Development/WS_SGC/applications/sgc_server/sgc_server.properties)");
		} else {
			// preparación de datos
			long tiempoInicio = System.currentTimeMillis();
			DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
			Calendar cal = Calendar.getInstance();

			System.out.println("Iniciando actualización de commons "
					+ dateFormat.format(cal.getTime()));
			SortedSet<String> versionSet;
			user = args[0];
			pass = args[1];
			String ruta = args[2];
			// String commonFile = args[1];
			// String commonFile2 = args.length == 3 ? args[2] : null;

			// Creamos la carpeta donde se almacenaran las librerías
			try {
				FileUtils.deleteDirectory(new File(FOLDER_LIB_NEW));
				new File(FOLDER_LIB_NEW).mkdir();
			} catch (IOException e) {
				e.printStackTrace();
			}

			String commonFile = "";
			String[] files = new String[args.length - (REQUIRED_ARGS - 1)];

			Map<String, SortedSet<String>> commons = new HashMap<String, SortedSet<String>>();
			for (int i = REQUIRED_ARGS - 1; i < args.length; i++) {
				commonFile = args[i];
				mergeMap(commons, readCommonsFile(commonFile));

				files[i + 1 - REQUIRED_ARGS] = args[i];
			}

			// Map<String, String> commons = readCommonsFile(commonFile,
			// commonFile2);

			printCommons(commons);

			// Construimos el cliente con el usuario y password pasados como
			// argumento
			System.setProperty("org.apache.commons.logging.Log",
					"org.apache.commons.logging.impl.NoOpLog");
			client = new HttpClient();
			client.getState().setCredentials(AuthScope.ANY,
					new UsernamePasswordCredentials(user, pass));

			// Se comprueba que el usuario y password sean correctos y tengan
			// permisos para acceder a los archivos
			GetMethod method = new GetMethod(WEB);
			method.setDoAuthentication(true);
			int executeMethod;

			try {
				executeMethod = client.executeMethod(method);

				if (executeMethod == LOGGED) { // El usuario está autenticado y
												// tiene los permisos necesarios
												// para ver los archivos

					// Iteramos la estructura de commons creada
					for (String project : commons.keySet()) {
						versionSet = commons.get(project);

						// set de versiones auxiliar para guardar la versión
						// tal y como aparece en link de descarga
						SortedSet<String> versionSetAux = new TreeSet<String>();
						String versions = "";
						for (String version : versionSet) {
							// Se busca el nombre completo de la versión
							version = getVersion(version, project);

							versions += ", " + getJarName(version, project);

							versionSetAux.add(version);
						}

						versionSet = versionSetAux;
						System.out.println("Actualizando [" + versions + "]");
						// Descargamos el jar
						for (String version : versionSet) {

							downloadJar(version, project);
						}
						// Actualizamos classpath
						modifyClasspath(versionSet, ruta, project);

						// Actualizamos los commons

						modifyCommons(versionSetAux, files, project);
					}

					// Eliminamos los antiguos common_ jar
					deleteOldJar(ruta);

					System.out.println("Copiando jar");
					// Copiamos los nuevos jar
					copyNewJar(ruta);

					// Borramos la carpeta donde se almacenaran las librerías
					try {
						FileUtils.deleteDirectory(new File(FOLDER_LIB_NEW));
					} catch (IOException e) {
						e.printStackTrace();
					}

					cal = Calendar.getInstance();
					long totalTiempo = System.currentTimeMillis()
							- tiempoInicio;
					System.out.println("Fin actualización de commons "
							+ dateFormat.format(cal.getTime()) + " en "
							+ totalTiempo / 1000 + " seg");

				}
			} catch (HttpException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * Método usado para hacer un merge de dos mapas
	 * 
	 * @param commons
	 * @param readCommonsFile
	 */
	private static void mergeMap(Map<String, SortedSet<String>> original,
			Map<String, SortedSet<String>> newMap) {

		for (String key : newMap.keySet()) {
			if (original.containsKey(key)) {
				SortedSet<String> originalSet = original.get(key);
				originalSet.addAll(newMap.get(key));
				original.put(key, originalSet);
			} else {
				original.put(key, newMap.get(key));
			}
		}
	}

	/**
	 * Copiamos los nuevos jar a la ruta indicada
	 * 
	 * @param ruta
	 */
	private static void copyNewJar(String ruta) {
		File folder_lib_new = new File(FOLDER_LIB_NEW);
		File[] ficheros_new = folder_lib_new.listFiles();
		for (int x = 0; x < ficheros_new.length; x++) {
			File file_common = new File(ruta + "/lib/"
					+ ficheros_new[x].getName());

			try {
				// copyFileUsingStream(ficheros_new[x], file_common);
				if (!file_common.getPath().endsWith(HTML)) {
					FileUtils.copyFile(ficheros_new[x], file_common);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Borramos los common jars de la ruta indicada
	 * 
	 * @param ruta
	 */
	private static void deleteOldJar(String ruta) {
		File folder_lib = new File(ruta + "/lib");
		File[] ficheros = folder_lib.listFiles();
		for (int x = 0; x < ficheros.length; x++) {
			if (ficheros[x].getName().startsWith("common_")) {
				// Modificamos el contenido
				ficheros[x].delete();
			}
		}
	}

	/**
	 * Modificamos el classpath para el proyecto en concreto
	 * 
	 * @param version
	 * @param ruta
	 * @param project
	 */
	private static void modifyClasspath(SortedSet<String> versionSet,
			String ruta, String project) {
		// modificar el classpath de todos los proyectos
		File f = new File(ruta);// Para las pruebas
		if (f.exists()) { // Directorio existe
			File[] ficheros = f.listFiles();
			for (int x = 0; x < ficheros.length; x++) {
				File classPathFile = new File(getClasspath(ruta, ficheros, x));
				if (classPathFile.exists()) {
					File classPathFileTemp = new File(getClaspathTemp(ruta,
							ficheros, x));
					// Modificamos el contenido
					modifyClasspathFile(versionSet, project, classPathFile,
							classPathFileTemp);
				}

			}
		} else {
			System.err.println(f.getName() + " no existe");
		}
	}

	/**
	 * Escribe por pantalla el mapa de commons
	 * 
	 * @param commons
	 */
	private static void printCommons(Map<String, SortedSet<String>> commons) {
		for (String key : commons.keySet()) {
			System.out.println(key + " - " + commons.get(key));
		}

	}

	/**
	 * Modifica el fichero de classpath
	 * 
	 * @param version
	 *            Versión del jar
	 * @param project
	 *            Proyecto
	 * @param classPathFile
	 *            Fichero classpath
	 * @param classPathFileTemp
	 *            Fichero temporal
	 */
	@SuppressWarnings("resource")
	private static void modifyClasspathFile(SortedSet<String> versionSet,
			String project, File classPathFile, File classPathFileTemp) {
		try {
			FileReader fr;
			BufferedReader br;
			FileWriter fw;
			BufferedWriter bw;
			String s;

			fw = new FileWriter(classPathFileTemp, true);
			bw = new BufferedWriter(fw);

			fr = new FileReader(classPathFile);
			br = new BufferedReader(fr);

			while ((s = br.readLine()) != null) {
				if (s.contains(project + "-")) {
					// Como coincide el nombre del proyecto, preguntamos en el
					// caso de que tengamos varias opciones cual debe ponerse en
					// el classpath
					String version = "";
					if (versionSet.size() > 1) {
						boolean valid = false;
						while (!valid) {
							String path = classPathFile.getAbsolutePath();

							String paths = path.substring(0,
									path.lastIndexOf(".classpath") - 1);

							System.out
									.println("Hay más de una opción para el classpath de "
											+ paths);
							System.out.println("Elija cual debe ponerse:");

							int i = 0;
							Iterator<String> iteratorSet = versionSet
									.iterator();
							while (iteratorSet.hasNext()) {
								System.out.println(i + " - "
										+ iteratorSet.next());
								i++;
							}
							Scanner scanner = new Scanner(System.in);
							int choice = scanner.nextInt();

							if (choice >= 0 && choice < i) {
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
						}

					} else {
						version = versionSet.first();
					}

					bw.write("\t");
					bw.write(CLASSPATHENTRY_KIND_LIB_PATH_LIB
							+ getJarName(version, project) + EXPORTED_TRUE);
				} else {
					bw.write(s);
				}
				bw.write("\n");
			}
			br.close();
			bw.close();

			classPathFile.delete();

			// Copiamos el fichero
			copyFileUsingStream(classPathFileTemp, classPathFile);
			// FileUtils.copyFileToDirectory(classPathFileTemp, classPathFile);

			classPathFileTemp.delete();
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (UnsupportedEncodingException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Obtiene la ruta completa hasta el fichero temporal
	 * 
	 * @param ruta
	 * @param ficheros
	 * @param x
	 * @return
	 */
	private static String getClaspathTemp(String ruta, File[] ficheros, int x) {
		return ruta + SLASH + ficheros[x].getName() + SLASH + ".classpathTemp";
	}

	/**
	 * Obtiene la ruta completa hasta el fichero classpath
	 * 
	 * @param ruta
	 * @param ficheros
	 * @param x
	 * @return
	 */
	private static String getClasspath(String ruta, File[] ficheros, int x) {
		return ruta + SLASH + ficheros[x].getName() + SLASH + ".classpath";
	}

	/**
	 * Lee el fichero de commons
	 * 
	 * @param commonFile
	 *            Ruta del fichero
	 * @return Map<String, String>
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static Map<String, SortedSet<String>> readCommonsFile(
			String commonFile) {
		Map<String, SortedSet<String>> projects = new HashMap<String, SortedSet<String>>();
		try {
			FileReader fr = new FileReader(commonFile);
			BufferedReader br = new BufferedReader(fr);
			String line;

			while ((line = br.readLine()) != null) {
				if (!line.startsWith("#")) {
					String prj = line.split("=")[0].replaceAll("_tag", "");
					if (line.split("=")[0].startsWith("common")
							&& line.split("=")[0].endsWith("_tag")) {
						String ver = line.split("=")[1];
						if (projects.get(prj.trim()) != null) {
							StringBuilder errorBuilder = new StringBuilder();
							throw new RuntimeException(
									errorBuilder
											.append("El .jar ")
											.append(prj.trim())
											.append(" se encuentra repetido en el fichero: ")
											.append(commonFile).toString());
						}
						// prj = fixSGD(prj);
						SortedSet<String> sortedSet = projects.get(prj.trim());
						if (sortedSet != null) {
						} else {
							sortedSet = new TreeSet<String>();
						}
						sortedSet.add(ver.trim());
						projects.put(prj.trim(), sortedSet);
					}
				}
			}
			br.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return projects;
	}

	/**
	 * Modificamos los archivos commons para el proyecto en concreto
	 * 
	 * @param version
	 * @param ruta
	 * @param project
	 */
	private static void modifyCommons(SortedSet<String> fVersionSet,
			String[] files, String project) {
		// modificar los commons de todos los proyectos
		for (int i = 0; i < files.length; i++) {
			File f = new File(files[i]);

			if (f.exists()) { // el fichero existe
				File commonsFileTemp = new File(getCommonsTemp(files[i]));
				// Modificamos el contenido del archivo
				modifyCommonsFile(fVersionSet, project, f, commonsFileTemp);

			} else {
				System.err.println(f.getName() + " no existe");
			}
		}
	}

	/**
	 * Obtiene la ruta completa hasta el fichero temporal
	 * 
	 * @param ruta
	 * @return
	 */
	private static String getCommonsTemp(String ruta) {
		return ruta + "Temp";
	}

	/**
	 * Modifica el fichero de commons
	 * 
	 * @param version
	 *            Versión del jar
	 * @param project
	 *            Proyecto
	 * @param classPathFile
	 *            Fichero classpath
	 * @param classPathFileTemp
	 *            Fichero temporal
	 */
	/**
	 * Modifica el fichero de commons
	 * 
	 * @param oVersionSet
	 * @param fVersionSet
	 * @param project
	 * @param commonsFile
	 * @param commonsFileTemp
	 */
	@SuppressWarnings("resource")
	private static void modifyCommonsFile(SortedSet<String> fVersionSet,
			String project, File commonsFile, File commonsFileTemp) {
		try {
			FileReader fr;
			BufferedReader br;
			FileWriter fw;
			BufferedWriter bw;
			String s;

			fw = new FileWriter(commonsFileTemp, true);
			bw = new BufferedWriter(fw);

			fr = new FileReader(commonsFile);
			br = new BufferedReader(fr);

			// buscamos dentro del archivo la línea correspondiente al proyecto
			// deseado
			while ((s = br.readLine()) != null) {
				if (s.contains(project + "_tag" + "=") && !s.startsWith("#")) {
					// es el proyecto que estamos buscando
					// y no es una línea comentada
					String version = s.split("=")[1];
					if (version == "") {
						StringBuilder errorBuilder = new StringBuilder();
						throw new RuntimeException(
								errorBuilder
										.append("El archivo ")
										.append(commonsFile)
										.append(" se encuentra no contiene versión para el commons: ")
										.append(project).toString());
					} else {
						// se busca la versión correcta del commons
						for (String fVersion : fVersionSet) {
							if (fVersion.contains("-" + version)) {
								// se ha encontrado la versión deseada
								// cambiamos el texto contenido en la
								// versión
								version = fVersion;
							}
						}
					}
					// se actualiza la línea que contiene el texto
					bw.write(project + "_tag=" + version);

				} else {
					bw.write(s);
				}
				bw.write("\n");
			}

			br.close();
			bw.close();

			commonsFile.delete();
			// Copiamos el fichero
			copyFileUsingStream(commonsFileTemp, commonsFile);
			// FileUtils.copyFileToDirectory(classPathFileTemp, classPathFile);

			commonsFileTemp.delete();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Copia un fichero
	 * 
	 * @param source
	 *            Origen
	 * @param dest
	 *            Destino
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
	 * Descarga el fichero jar según la versión y el proyecto
	 * 
	 * @param version
	 *            Número de versión a descargar
	 * @param project
	 *            Proyecto a descargar
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private static void downloadJar(String version, String project) {

		downloadJarImpl(project, version, getJarName(version, project));
		downloadJarImpl(project, version, getSourceJarName(version, project));

	}

	private static void downloadJarImpl(String project, String version,
			String jarName) {
		GetMethod method = null;
		try {
			method = new GetMethod(WEB + project + SLASH + version + SLASH
					+ jarName);
			// method = new GetMethod(WEB + project);
			method.setDoAuthentication(true);

			client.executeMethod(method);
			InputStream in = new BufferedInputStream(
					method.getResponseBodyAsStream());
			OutputStream out = new BufferedOutputStream(new FileOutputStream(
					new File(FOLDER_LIB_NEW + "/" + jarName)));
			IOUtils.copy(in, out);

			out.close();
			in.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			method.releaseConnection();

		}

	}

	/**
	 * Obtiene el nombre del fichero jar según versión y proyecto dados
	 * 
	 * @param version
	 *            Versión del fichero
	 * @param project
	 *            Nombre del proyecto del fichero
	 * @return Nombre del jar
	 */
	private static String getJarName(String version, String project) {
		return project + "-" + version + ".jar";
	}

	/**
	 * Obtiene el nombre del fichero jar de los ficheros fuentes según versión y
	 * proyecto dados
	 * 
	 * @param version
	 *            Versión del fichero
	 * @param project
	 *            Nombre del proyecto del fichero
	 * @return Nombre del jar
	 */
	private static String getSourceJarName(String version, String project) {
		return project + "-" + version + "-sources.jar";
	}

	/**
	 * Obtiene la versión del jar encontrada a partir del proyecto pasado por
	 * argumento
	 * 
	 * @return
	 */
	private static String getVersion(String version, String project) {
		String res = version;
		GetMethod method = null;

		try {
			method = new GetMethod(WEB + project + SLASH);

			method.setDoAuthentication(true);

			client.executeMethod(method);
			InputStream in = new BufferedInputStream(
					method.getResponseBodyAsStream());

			// descargamos la página html para encontrar la versión que deseamos
			File file = new File(FOLDER_LIB_NEW + "/" + getHtmlName(project));
			OutputStream out = new BufferedOutputStream(new FileOutputStream(
					file));
			// new File(FOLDER_LIB_NEW + "/"
			// + getHtmlName(project))));
			IOUtils.copy(in, out);

			out.close();
			in.close();

			// creamos el documento
			Document doc = Jsoup.parse(file, null);
			// iteramos sobre el código para encontrar la versión deseada
			Elements commons = doc.getElementsByTag("tr");

			SimpleDateFormat sdf = new SimpleDateFormat(
					"EEE MMM dd HH:mm:ss zzz yyyy", new Locale("es_ES"));

			Date versionDate = null;

			for (Element common : commons) {
				if (common.getElementsByTag("td").size() >= 2) {// hay datos
																// dentro del
																// elemento
					// obtenemos el nombre de la versión
					String vaux = common.getElementsByTag("td").get(0)
							.getElementsByTag("td").get(0).select("a").text()
							.toString();
					vaux = vaux.split("/")[0];

					if (vaux.contains("-" + version)) { // puede ser la versión
														// deseada
						Date vauxDate;
						try {
							// se comprueba la fecha
							vauxDate = sdf.parse(common.getElementsByTag("td")
									.get(1).getElementsByTag("td").get(0)
									.text().toString());

							if ((versionDate == null)
									|| (versionDate.compareTo(vauxDate) < 0)) {
								versionDate = vauxDate;
								res = vaux;
							}
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}

				}
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

		return res;
	}

	/**
	 * Obtiene el nombre del fichero html según versión y proyecto dados
	 * 
	 * @param version
	 *            Versión del fichero
	 * @param project
	 *            Nombre del proyecto del fichero
	 * @return Nombre del html
	 */
	private static String getHtmlName(String project) {
		return project + ".html";
	}
}
