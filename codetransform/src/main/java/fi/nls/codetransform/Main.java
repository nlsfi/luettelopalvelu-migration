package fi.nls.codetransform;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import fi.nls.codetransform.koodistotsuomi.KoodistotSuomiFi;
import fi.nls.codetransform.transform.Configuration;

public class Main {

    private static final String SETTINGS_FILE = "settings.properties";
    private static final String KEY_KOODISTOT_BASE_URL = "koodistot.baseurl";
    private static final String KEY_KOODISTOT_APIKEY = "koodistot.apikey";
    private static final String KEY_KOODISTOT_ORG_ID = "koodistot.org";

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Error! Last arg must be path to configuration csv file!");
            System.exit(-1);
        }

        String mode = Arrays.stream(args).anyMatch(arg -> "-u".equals(arg)) ? "update" : "reload";
        boolean dryRun = Arrays.stream(args).anyMatch(arg -> "--dry-run".equals(arg));
        boolean setValid = Arrays.stream(args).anyMatch(arg -> "--set-valid".equals(arg));

        String lastArg = args[args.length - 1];
        Path codelistConfigFilePath = Path.of(lastArg);
        if (!codelistConfigFilePath.toFile().canRead()) {
            System.err.println("Error! Can not read file: " + lastArg);
            System.exit(-1);
        }

        List<Configuration> configs = parseConfiguration(codelistConfigFilePath);

        Properties appSettings = readSettings(SETTINGS_FILE);

        String koodistotBaseURL = getSetting(appSettings, KEY_KOODISTOT_BASE_URL);
        String apikey = getSetting(appSettings, KEY_KOODISTOT_APIKEY);
        String orgId = getSetting(appSettings, KEY_KOODISTOT_ORG_ID);

        if (apikey == null || apikey.isEmpty()) {
            System.err.println("Error! Missing apikey. Configure in " + SETTINGS_FILE + " with key: " + KEY_KOODISTOT_APIKEY + " or via env variable: " + KEY_KOODISTOT_APIKEY.toUpperCase().replace('.', '_'));
            System.exit(-2);
        }
        if (orgId == null || orgId.isEmpty()) {
            System.err.println("Error! Missing organization id. Configure in " + SETTINGS_FILE + " with key: " + KEY_KOODISTOT_ORG_ID + " or via env variable: " + KEY_KOODISTOT_ORG_ID.toUpperCase().replace('.', '_'));
            System.exit(-3);
        }

        int verbosity = parseVerbosity(args);

        if (verbosity > 0) {
            System.out.println("Using koodistot base url " + koodistotBaseURL);
            if (verbosity > 3) {
                System.out.println("Using apikey " + apikey);
            } else {
                // just show begin and end
                System.out.println("Using apikey " + apikey.substring(0,5) + "..." + apikey.substring(apikey.length() - 5));
            }
        }

        KoodistotSuomiFi koodistot = new KoodistotSuomiFi(koodistotBaseURL, apikey, orgId);

        switch (mode) {
            case "reload":
                Reload.reload(koodistot, configs, verbosity, dryRun, setValid);
                break;
            case "update":
                Update.update(koodistot, configs, verbosity, dryRun, setValid);
                break;
            default:
                System.err.println("Error! Invalid mode " + mode);
                System.exit(-3);
        }
    }

    private static int parseVerbosity(String[] args) {
        for (String arg : args) {
            if (arg.matches("\\-v+")) {
                return arg.length() - 1;
            }
        }
        return 0;
    }

    private static Properties readSettings(String path) throws Exception {
        Properties settings = new Properties();

        // Override/append with local file from $CWD/settings.properties
        Path p = Path.of(path);
        if (p.toFile().exists()) {
            try (BufferedReader r = Files.newBufferedReader(p)) {
                settings.load(r);
            }
        }

        return settings;
    }

    private static String getSetting(Properties properties, String key) {
        String propertyValue = properties.getProperty(key);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }
        // Fallback to env variable
        String envVar = key.toUpperCase().replace('.', '_');
        return System.getenv(envVar);
    }

    private static List<Configuration> parseConfiguration(Path configFilePath) throws Exception {
        return Files.readAllLines(configFilePath).stream()
            .map(line -> line.trim())
            .filter(line -> !line.isBlank())
            .filter(line -> line.charAt(0) != '#')
            .map(line -> line.split("\t", 4))
            .filter(arr -> arr.length >= 4)
            .map(arr -> new Configuration(
                arr[0],
                arr[1],
                arr[2],
                arr[3]
            ))
            .toList();
    }

}
