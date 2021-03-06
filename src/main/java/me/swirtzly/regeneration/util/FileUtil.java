package me.swirtzly.regeneration.util;

import me.swirtzly.regeneration.RegenerationMod;
import me.swirtzly.regeneration.client.skinhandling.SkinChangingHandler;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static me.swirtzly.regeneration.client.skinhandling.SkinChangingHandler.*;

public class FileUtil {
	
	
	public static void handleDownloads() throws IOException {
		String PACKS_URL = "https://raw.githubusercontent.com/Suffril/Regeneration/skins/index.json";
		String[] links = RegenerationMod.GSON.fromJson(getJsonFromURL(PACKS_URL), String[].class);
		for (String link : links) {
			unzipSkinPack(link);
		}
	}
	
	/**
	 * Creates skin folders
	 * Proceeds to download skins to the folders if they are empty
	 * If the download doesn't happen, NPEs will occur later on
	 */
	public static void createDefaultFolders() throws IOException {
		
		if (!SKIN_DIRECTORY.exists()) {
			FileUtils.forceMkdir(SKIN_DIRECTORY);
		}
		
		if (!SKIN_DIRECTORY_ALEX.exists()) {
			FileUtils.forceMkdir(SKIN_DIRECTORY_ALEX);
		}
		
		if (!SKIN_DIRECTORY_STEVE.exists()) {
			FileUtils.forceMkdir(SKIN_DIRECTORY_STEVE);
		}
		
		
		if (Objects.requireNonNull(SKIN_DIRECTORY_ALEX.list()).length == 0 || Objects.requireNonNull(SKIN_DIRECTORY_STEVE.list()).length == 0) {
			RegenerationMod.LOG.warn("One of the skin directories is empty, so we're going to fill both.");
			handleDownloads();
		}
	}
	
	/**
	 * @param url      - URL to download image from
	 * @param file     - Directory of where to save the image to [SHOULD NOT CONTAIN THE FILES NAME]
	 * @param filename - Filename of the image [SHOULD NOT CONTAIN FILE EXTENSION, PNG IS SUFFIXED FOR YOU]
	 * @throws IOException
	 */
	public static void downloadImage(URL url, File file, String filename) throws IOException {
		
		URLConnection uc;
		uc = url.openConnection();
		uc.connect();
		uc = url.openConnection();
		uc.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.75 Safari/537.36");
		SkinChangingHandler.SKIN_LOG.info("Downloading Skin from: {}", url.toString());
		BufferedImage img = ImageIO.read(uc.getInputStream());
		img = ClientUtil.ImageFixer.convertSkinTo64x64(img);
        if (!file.exists()) {
			file.mkdirs();
		}
		ImageIO.write(img, "png", new File(file, filename + ".png"));
	}
	
	public static void doSetupOnThread() {
		AtomicBoolean notDownloaded = new AtomicBoolean(true);
		new Thread(() -> {
			while (notDownloaded.get()) {
				try {
					createDefaultFolders();
					Trending.downloadTrendingSkins();
					notDownloaded.set(false);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}, RegenerationMod.NAME + " Download Daemon").start();
	}
	
	
	public static void unzipSkinPack(String url) throws IOException {
		File tempZip = new File(SKIN_DIRECTORY + "/temp/" + System.currentTimeMillis() + ".zip");
		RegenerationMod.LOG.info("Downloading " + url + " to " + tempZip.getAbsolutePath());
		FileUtils.copyURLToFile(new URL(url), tempZip);
		try (ZipFile file = new ZipFile(tempZip)) {
			FileSystem fileSystem = FileSystems.getDefault();
			Enumeration<? extends ZipEntry> entries = file.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.isDirectory()) {
					Files.createDirectories(fileSystem.getPath(SKIN_DIRECTORY + File.separator + entry.getName()));
				} else {
					InputStream is = file.getInputStream(entry);
					BufferedInputStream bis = new BufferedInputStream(is);
					String uncompressedFileName = SKIN_DIRECTORY + File.separator + entry.getName();
					Path uncompressedFilePath = fileSystem.getPath(uncompressedFileName);
					RegenerationMod.LOG.info("Extracting file: " + uncompressedFilePath);
					Files.createFile(uncompressedFilePath);
					FileOutputStream fileOutput = new FileOutputStream(uncompressedFileName);
					while (bis.available() > 0) {
						fileOutput.write(bis.read());
					}
					fileOutput.close();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (tempZip.exists()) {
			FileUtils.forceDelete(tempZip.getParentFile());
		}
	}
	
	public static String getJsonFromURL(String URL) {
		URL url = null;
		try {
			url = new URL(URL);
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			String line = bufferedReaderToString(in);
			line = line.replace("<pre>", "");
			line = line.replace("</pre>", "");
			return line;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String bufferedReaderToString(BufferedReader e) {
		StringBuilder builder = new StringBuilder();
		String aux = "";
		
		try {
			while ((aux = e.readLine()) != null) {
				builder.append(aux);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		return builder.toString();
	}
	
	public interface IEnum<E extends Enum<E>> {
		
		int ordinal();
		
		default E next() {
			E[] ies = this.getAllValues();
			return this.ordinal() != ies.length - 1 ? ies[this.ordinal() + 1] : null;
		}
		
		default E previous() {
			return this.ordinal() != 0 ? this.getAllValues()[this.ordinal() - 1] : null;
		}
		
		@SuppressWarnings("unchecked")
		default E[] getAllValues() {
			IEnum[] ies = this.getClass().getEnumConstants();
			return (E[]) ies;
		}
	}
}
