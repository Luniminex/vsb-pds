package sir.analyzer;

import sir.model.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ConfigLoader {

    private ConfigLoader() { }
    public static Configuration load(Path configPath) throws IOException {
        List<String> lines = Files.readAllLines(configPath);

        int width = 0, height = 0, initialInfected = 0;
        double infProb = 0.0, recProb = 0.0;
        Long seed = null;

        for (String line : lines) {
            String[] parts = line.split(":");
            if (parts.length != 2) continue;
            String key = parts[0].trim();
            String value = parts[1].trim().replace(",", ".");

            switch (key) {
                case "Width" -> width = Integer.parseInt(value);
                case "Height" -> height = Integer.parseInt(value);
                case "InitialInfectedCount" -> initialInfected = Integer.parseInt(value);
                case "InfectionProbability" -> infProb = Double.parseDouble(value);
                case "RecoveryProbability" -> recProb = Double.parseDouble(value);
                case "Seed" -> {
                    if (value.equalsIgnoreCase("Random (null)") || value.equalsIgnoreCase("null")) {
                        seed = null;
                    } else {
                        seed = Long.parseLong(value);
                    }
                }
            }
        }

        return new Configuration(width, height, initialInfected, infProb, recProb, seed);
    }
}