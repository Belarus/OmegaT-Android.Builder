import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;

import org.apache.commons.io.FileUtils;

import android.control.App;
import android.control.Cyanogen;
import android.control.Translation;

/**
 * Imports source files from cyanogen.
 */
public class UnpackCyanogenResources {
	static String projectPath = "../../Android.OmegaT/Android/";

	public static void main(String[] args) throws Exception {
		JAXBContext ctx = JAXBContext.newInstance(Translation.class);
		Translation translationInfo = (Translation) ctx.createUnmarshaller()
				.unmarshal(new File(projectPath + "../translation.xml"));

		// process cyanogen resources
		for (App app : translationInfo.getApp()) {
			File outDir = new File(projectPath + "/source/" + app.getDirName());
			for (Cyanogen cy : app.getCyanogen()) {
				String version = args[1];
				if (cy.getOutSuffix() != null) {
					version = cy.getOutSuffix() + '-' + version;
				}

				System.out.println("From dir " + cy.getSrc() + " version="
						+ version + " into " + outDir.getAbsolutePath());

				outDir.mkdirs();
				copyIfExist(new File(args[0], cy.getSrc()
						+ "/values/strings.xml"), new File(outDir, "strings_"
						+ version + ".xml"));
				copyIfExist(new File(args[0], cy.getSrc()
						+ "/values/arrays.xml"), new File(outDir, "arrays_"
						+ version + ".xml"));
				copyIfExist(new File(args[0], cy.getSrc()
						+ "/values/plurals.xml"), new File(outDir, "plurals_"
						+ version + ".xml"));
			}
		}
	}

	static void copyIfExist(File in, File out) throws IOException {
		if (in.exists()) {
			System.out.println("        " + in.getAbsolutePath() + " -> "
					+ out.getAbsolutePath());
			FileUtils.copyFile(in, out);
		}
	}

	static final Pattern RE_REF = Pattern.compile("ref: refs/heads/([a-z]+)\n");

	protected static String getCyanogenDirVersion(String gitsDir,
			String cyanogenDir) throws Exception {
		int p = cyanogenDir.indexOf('/');
		File resDir = new File(gitsDir, cyanogenDir);
		File head = new File(getGitDir(resDir), "HEAD");
		String ref = FileUtils.readFileToString(head);
		Matcher m = RE_REF.matcher(ref);
		if (!m.matches()) {
			System.out.println("Error version of cyanogen package: '" + ref
					+ "'");
		}
		return m.group(1);
	}

	protected static File getGitDir(File dir) {
		File gitDir = new File(dir, ".git");
		if (gitDir.exists() && gitDir.isDirectory()) {
			return gitDir;
		} else {
			File parent = dir.getParentFile();
			if (parent == null) {
				return null;
			} else {
				return getGitDir(parent);
			}
		}
	}
}
