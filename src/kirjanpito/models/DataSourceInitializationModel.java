package kirjanpito.models;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import kirjanpito.ui.Kirjanpito;
import kirjanpito.util.AppSettings;

public class DataSourceInitializationModel {
	private File archiveDir;
	private ArrayList<File> files;
	private ArrayList<String> names;
	private Logger logger = Logger.getLogger(Kirjanpito.LOGGER_NAME);

	public DataSourceInitializationModel() {
		/* Tilikarttamallitiedostot ovat tilikarttamallit-nimisessä
		 * hakemistossa, joka sijaitsee samassa hakemistossa kuin
		 * ohjelman JAR-tiedosto. */
		File jarFile;
		
		try {
			jarFile = new File(DataSourceInitializationModel.class.
			getProtectionDomain().getCodeSource().getLocation().toURI());
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
			return;
		}
		
		archiveDir = new File(jarFile.getParentFile(), "tilikarttamallit");
	}
	
	public void update() {
		files = new ArrayList<File>();
		names = new ArrayList<String>();
		
		readDir(archiveDir);
		
		/* Etsitään tiedostoja myös käyttäjän asetushakemistosta. */
		AppSettings settings = AppSettings.getInstance();
		readDir(new File(settings.getDirectoryPath()));
	}
	
	private void readDir(File dir) {
		if (!dir.isDirectory()) return;
		File[] jarFiles = dir.listFiles(filter);
		Arrays.sort(jarFiles);
		
		for (File file : jarFiles) {
			try {
				JarFile jar = new JarFile(file);
				Manifest manifest = jar.getManifest();
				
				if (manifest == null) {
					logger.log(Level.WARNING, "Tiedosto " + file.getName() +
							" ei sisällä MANIFEST.MF-tiedostoa");
					jar.close();
					continue;
				}
				
				Attributes attr = manifest.getMainAttributes();
				String name = attr.getValue("Name");
				jar.close();
				
				if (name == null) {
					logger.log(Level.WARNING, "Tiedosto " + file.getName() +
						" ei sisällä Name-attribuuttia");
					continue;
				}
				
				files.add(file);
				names.add(name);
			}
			catch (IOException e) {
				logger.log(Level.WARNING,
						"JAR-tiedoston lukeminen epäonnistui", e);
			}
		}
	}
	
	public File getArchiveDirectory() {
		return archiveDir;
	}
	
	public int getFileCount() {
		return files.size();
	}
	
	public File getFile(int index) {
		return files.get(index);
	}
	
	public String getName(int index) {
		return names.get(index);
	}
	
	private FilenameFilter filter = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			return name.endsWith(".jar");
		}
	};
}
