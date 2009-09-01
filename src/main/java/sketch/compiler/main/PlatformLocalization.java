package sketch.compiler.main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.util.Properties;

import sketch.compiler.CommandLineParamManager;

/**
 * get any variables related to this specific compile, e.g. version number, and
 * resolve the path to the cegis binary.
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public class PlatformLocalization {
    protected static PlatformLocalization singleton;
    public String version = "1.4.0";
    public String osname = "Linux";
    public String osarch = "amd64";
    public Properties localization;
    public File usersketchdir;
    /** only extract to a secure (not shared) location */
    public File tmpdir;
    /**
     * if this is false, fake values will be filled in for version, etc. so
     * other code doesn't fail.
     */
    public boolean isSet = false;

    public static PlatformLocalization getLocalization() {
        if (singleton == null) {
            singleton = new PlatformLocalization();
        }
        return singleton;
    }

    public PlatformLocalization() {
        // try to read the localization.properties file...
        localization = new Properties();
        URL resource = getCompilerRc("localization.properties");
        try {
            if (resource != null) {
                localization.load(resource.openStream());
                if (!localization.getProperty("version").contains("$")) {
                    version = localization.getProperty("version");
                    osname = localization.getProperty("osname");
                    osarch = localization.getProperty("osarch");
                    isSet = true;
                }
            }
        } catch (IOException e) {
            System.err.println("error retriving localization properties");
        }
        usersketchdir = md(path(System.getProperty("user.home"), ".sketch"));
        tmpdir = md(path(usersketchdir, "tmp"));
    }

    public String getCegisPath() {
        String cegisName = "cegis" + (isWin() ? ".exe" : "");
        if (platformMatchesJava() && tmpdir != null) {
            // try to get it from the jar
            try {
                URL rc_url = getCompilerRc(cegisName);
                if (rc_url != null) {
                    InputStream fileIn = getCompilerRc(cegisName).openStream();
                    if (fileIn.available() > 0) {
                        String result = loadTempFile(fileIn, cegisName);
                        if (result != null) {
                            return result;
                        }
                    }
                }
            } catch (IOException e) {
            }
        } else if (isSet && tmpdir != null) {
            System.err.println("Your system doesn't match the localization "
                    + "strings of the SKETCH jar: " + osname + ", " + osarch);
        }
        File jarpath = null;
        CodeSource codesource =
                getClass().getProtectionDomain().getCodeSource();
        if (codesource != null) {
            URL location = codesource.getLocation();
            if (location != null) {
                jarpath = path(location.getFile());
                if (jarpath != null && jarpath.isFile()) {
                    jarpath = jarpath.getParentFile();
                }
            }
        }
        File[] files =
                {
                        path(jarpath, "cegis", "src", "SketchSolver", cegisName),
                        path(jarpath, cegisName),
                        path(".", "cegis", "src", "SketchSolver", cegisName),
                        path("..", "sketch-backend", "src", "SketchSolver",
                                cegisName), path(cegisName),
                        path(usersketchdir, cegisName + "-" + version),
                        path(usersketchdir, cegisName) };
        for (File file : files) {
            if (file != null && file.isFile()) {
                try {
                    if (CommandLineParamManager.getParams().flagValue(
                            "verbosity") > 2)
                    {
                        System.out.println("resolved cegis to path "
                                + file.getCanonicalPath());
                    }
                    return file.getCanonicalPath();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return cegisName;
    }

    /** make directories if they don't already exist, return $dirname$ or null */
    public static File md(File dirname) {
        if (dirname == null) {
            return null;
        } else if (dirname.isDirectory()) {
            return dirname;
        } else if (dirname.mkdirs()) {
            return dirname;
        } else {
            return null;
        }
    }

    public static File path(File base, String... subpaths) {
        if (base == null) {
            return null;
        } else if (subpaths.length == 0) {
            return base;
        } else {
            String[] next = new String[subpaths.length - 1];
            System.arraycopy(subpaths, 1, next, 0, next.length);
            return path(new File(base, subpaths[0]), next);
        }
    }

    public static File path(String base, String... subpaths) {
        if (base == null) {
            return null;
        } else {
            return path(new File(base), subpaths);
        }
    }

    protected String loadTempFile(InputStream fileIn, String cegisName) {
        // try to extract it to the operating system temporary directory
        File path = new File(tmpdir, cegisName);
        String canonicalName = null;
        try {
            canonicalName = path.getCanonicalPath();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        if (path.exists()) {
            return canonicalName;
        } else {
            try {
                FileOutputStream fileOut = new FileOutputStream(path);
                byte[] buffer = new byte[8192];
                int len;
                while ((len = fileIn.read(buffer)) > 0) {
                    fileOut.write(buffer, 0, len);
                }
                assert (fileIn.available() == 0) : "didn't read all of file";
                if (!isWin()) {
                    Process proc =
                            (new ProcessBuilder("chmod", "755", canonicalName))
                                    .start();
                    assert (proc.waitFor() == 0) : "couldn't make cegis executable";
                }
                return canonicalName;
            } catch (Exception e) {
                System.err.println("couldn't extract cegis binary; " + e);
            }
        }
        return null;
    }

    // misc functions
    public URL getCompilerRc(String name) {
        return getClass().getClassLoader().getResource(
                "sketch/compiler/" + name);
    }

    public boolean platformMatchesJava() {
        return isSet && osname.equals(System.getProperty("os.name"))
                && osarch.equals(System.getProperty("os.arch"));
    }

    public boolean isWin() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    public String getDefaultTempFile() {
        try {
            if (tmpdir == null) {
                return null;
            } else {
                return (new File(tmpdir, "sketch.sk.tmp")).getCanonicalPath();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}