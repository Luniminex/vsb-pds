package sir.analyzer;

import org.knowm.xchart.*;
import org.knowm.xchart.style.CategoryStyler;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class GraphGenerator {

    public static void main(String[] args) throws IOException {
        Path inputDir = Paths.get("src/main/resources/processed");
        Path outputDir = Paths.get("src/main/resources/graphs");

        Path avgStepDir = outputDir.resolve("avg_step_time");
        Path totalTimeDir = outputDir.resolve("total_time");
        Path avgSpeedDir = outputDir.resolve("avg_speed_ticks_per_ms");
        Path comparisonByGridDir = outputDir.resolve("solver_comparison_by_grid");

        Files.createDirectories(avgStepDir);
        Files.createDirectories(totalTimeDir);
        Files.createDirectories(avgSpeedDir);
        Files.createDirectories(comparisonByGridDir);

        Map<String, Map<String, List<Long>>> performanceDataByGridSize = new LinkedHashMap<>();

        try (Stream<Path> files = Files.list(inputDir)) {
            List<Path> csvFiles = files
                    .filter(p -> p.toString().endsWith(".csv"))
                    .toList();

            for (Path csvFile : csvFiles) {
                String solverName = csvFile.getFileName().toString().replace(".csv", "");

                Map<String, List<Long>> genToTicksList = new LinkedHashMap<>();
                Map<String, List<Long>> genToTotalTimeNsList = new LinkedHashMap<>();
                Map<String, List<Long>> genToAvgStepNsList = new LinkedHashMap<>();
                Map<String, String> genToGridSize = new LinkedHashMap<>();

                List<String> lines = Files.readAllLines(csvFile).stream().skip(1).toList();

                for (String line : lines) {
                    String[] parts = line.split(",");
                    if (parts.length < 12) {
                        System.err.println("Skipping malformed line in " + csvFile.getFileName() + " (expected at least 12 parts): " + line);
                        continue;
                    }

                    try {
                        String generationKey = parts[0];
                        int width = Integer.parseInt(parts[3]);
                        int height = Integer.parseInt(parts[4]);
                        String currentGridSize = width + "x" + height;

                        long currentRunTicks = Long.parseLong(parts[9]);
                        long currentRunTotalTimeNs = Long.parseLong(parts[10]);
                        long currentRunAvgStepNs = Long.parseLong(parts[11]);

                        genToTicksList.computeIfAbsent(generationKey, g -> new ArrayList<>()).add(currentRunTicks);
                        genToTotalTimeNsList.computeIfAbsent(generationKey, g -> new ArrayList<>()).add(currentRunTotalTimeNs);
                        genToAvgStepNsList.computeIfAbsent(generationKey, g -> new ArrayList<>()).add(currentRunAvgStepNs);
                        genToGridSize.putIfAbsent(generationKey, currentGridSize);

                        performanceDataByGridSize
                                .computeIfAbsent(currentGridSize, k -> new LinkedHashMap<>())
                                .computeIfAbsent(solverName, k -> new ArrayList<>())
                                .add(currentRunTotalTimeNs);

                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                        System.err.println("Error parsing line in " + csvFile.getFileName() + ": " + line + ". Error: " + e.getMessage());
                    }
                }

                List<String> generationLabelsForFile = new ArrayList<>();
                List<Double> avgStepMsValues = new ArrayList<>();
                List<Double> avgTotalTimeSecValues = new ArrayList<>();
                List<Double> avgSpeedTicksPerMsValues = new ArrayList<>();

                for (String generationKey : genToTicksList.keySet()) {
                    String gridSizeForGen = genToGridSize.getOrDefault(generationKey, "?x?");
                    String label = generationKey + " - " + gridSizeForGen;
                    generationLabelsForFile.add(label);

                    double avgStep = genToAvgStepNsList.getOrDefault(generationKey, Collections.emptyList()).stream()
                            .mapToLong(Long::longValue).average().orElse(0) / 1_000_000.0;
                    avgStepMsValues.add(avgStep);

                    double avgTotalTime = genToTotalTimeNsList.getOrDefault(generationKey, Collections.emptyList()).stream()
                            .mapToLong(Long::longValue).average().orElse(0) / 1_000_000_000.0;
                    avgTotalTimeSecValues.add(avgTotalTime);

                    double avgTicks = genToTicksList.getOrDefault(generationKey, Collections.emptyList()).stream()
                            .mapToLong(Long::longValue).average().orElse(0);

                    double speedInTicksPerMs = (avgTotalTime > 0) ? (avgTicks / (avgTotalTime * 1000.0)) : 0;
                    avgSpeedTicksPerMsValues.add(speedInTicksPerMs);
                }

                if (!generationLabelsForFile.isEmpty()) {
                    saveChart(avgStepDir, solverName + "_avg_step.png", "Avg Step Time - " + solverName, generationLabelsForFile, avgStepMsValues, "Avg Step Time (ms)");
                    saveChart(totalTimeDir, solverName + "_total_time.png", "Total Time - " + solverName, generationLabelsForFile, avgTotalTimeSecValues, "Total Time (s)");
                    saveChart(avgSpeedDir, solverName + "_avg_speed_ticks_per_ms.png", "Avg Speed (Ticks/ms) - " + solverName, generationLabelsForFile, avgSpeedTicksPerMsValues, "Ticks per ms");
                    System.out.println("Saved individual charts for: " + solverName);
                } else {
                    System.out.println("No data to generate individual charts for: " + solverName);
                }
            }
        }

        if (!performanceDataByGridSize.isEmpty()) {
            generateSolverComparisonByGridSizeChart(performanceDataByGridSize, comparisonByGridDir);
            System.out.println("Saved solver comparison by grid size chart.");
        } else {
            System.out.println("No data to generate solver comparison by grid size chart.");
        }
    }

    private static void saveChart(Path folder, String fileName, String title,
                                  List<String> xLabels, List<Double> yValues, String yLabel) throws IOException {
        if (xLabels.isEmpty() || yValues.isEmpty()) {
            System.err.println("Skipping chart generation for " + title + " due to empty data.");
            return;
        }

        CategoryChart chart = new CategoryChartBuilder()
                .width(1400)
                .height(700)
                .title(title)
                .xAxisTitle("Generation - GridSize")
                .yAxisTitle(yLabel)
                .build();

        Styler styler = chart.getStyler();
        styler.setLegendPosition(Styler.LegendPosition.InsideNE);
        styler.setToolTipsEnabled(false);

        String seriesName = yLabel;
        if (yLabel.contains("(")) {
            seriesName = yLabel.substring(0, yLabel.indexOf('(')).trim();
        }
        seriesName = seriesName.substring(0, Math.min(seriesName.length(), 30));

        chart.addSeries(seriesName, xLabels, yValues);

        Path outputFile = folder.resolve(fileName);
        BitmapEncoder.saveBitmap(chart, outputFile.toString(), BitmapEncoder.BitmapFormat.PNG);
    }

    private static void generateSolverComparisonByGridSizeChart(
            Map<String, Map<String, List<Long>>> data, Path outputDir) throws IOException {

        List<String> sortedGridSizes = new ArrayList<>(data.keySet());
        sortedGridSizes.sort((s1, s2) -> {
            try {
                String[] parts1 = s1.split("x");
                String[] parts2 = s2.split("x");
                long area1 = Long.parseLong(parts1[0]) * Long.parseLong(parts1[1]);
                long area2 = Long.parseLong(parts2[0]) * Long.parseLong(parts2[1]);
                if (area1 != area2) return Long.compare(area1, area2);
                return Long.compare(Long.parseLong(parts1[0]), Long.parseLong(parts2[0]));
            } catch (Exception e) {
                return s1.compareTo(s2);
            }
        });

        Set<String> solverNamesSet = new HashSet<>();
        data.values().forEach(solverMap -> solverNamesSet.addAll(solverMap.keySet()));
        List<String> sortedSolverNames = new ArrayList<>(solverNamesSet);
        Collections.sort(sortedSolverNames);

        CategoryChart chart = new CategoryChartBuilder()
                .width(1000)
                .height(600)
                .title("Solver Performance: Total Time vs. Grid Size")
                .xAxisTitle("Grid Size (Width x Height - Sorted by Area)")
                .yAxisTitle("Average Total Time (s)")
                .build();

        CategoryStyler styler = chart.getStyler();
        styler.setLegendPosition(Styler.LegendPosition.InsideNE);
        styler.setToolTipsEnabled(false);
        styler.setDefaultSeriesRenderStyle(CategorySeries.CategorySeriesRenderStyle.Line);
        styler.setMarkerSize(5);
        styler.setXAxisLabelRotation(45);

        for (String solverName : sortedSolverNames) {
            List<Double> avgTotalTimesForSolver = new ArrayList<>();

            for (String gridSize : sortedGridSizes) {
                Map<String, List<Long>> solverMap = data.get(gridSize);
                if (solverMap != null) {
                    List<Long> times = solverMap.get(solverName);
                    if (times != null && !times.isEmpty()) {
                        double average = times.stream().mapToLong(Long::longValue).average().orElse(Double.NaN);
                        avgTotalTimesForSolver.add(average / 1_000_000_000.0);
                    } else {
                        avgTotalTimesForSolver.add(Double.NaN);
                    }
                } else {
                    avgTotalTimesForSolver.add(Double.NaN);
                }
            }

            if (!avgTotalTimesForSolver.stream().allMatch(value -> Double.isNaN(value))) {
                CategorySeries series = chart.addSeries(solverName, sortedGridSizes, avgTotalTimesForSolver);
                series.setMarker(SeriesMarkers.CIRCLE);
            }
        }

        Path outputFile = outputDir.resolve("solver_performance_by_grid_size.png");
        BitmapEncoder.saveBitmap(chart, outputFile.toString(), BitmapEncoder.BitmapFormat.PNG);
        System.out.println("Successfully generated solver comparison chart (line style): " + outputFile);
    }
}
