package at.peckventure.world.generator;

/**
 * NoiseGenerator liefert 2D‑Noise-Werte mithilfe von OpenSimplex‑Noise.
 * Die API ähnelt dabei der von Godot FastNoiseLite.
 */
public class NoiseGenerator {

    // Eigenschaften, analog zu FastNoiseLite
    private double frequency = 1.0;      // Basis-Frequenz (wird beim Abruf skaliert)
    private int octaves = 1;             // Anzahl der Oktaven
    private double persistence = 0.5;    // Abnahme der Amplitude pro Oktave

    // Intern wird OpenSimplexNoise genutzt – er liefert ein „schönes“ Noise ohne die häufigen Blockartefakte.
    private OpenSimplexNoise noise;

    /**
     * Standardkonstruktor: initialisiert mit dem aktuellen Zeitstempel als Seed.
     */
    public NoiseGenerator() {
        this(System.currentTimeMillis());
    }

    /**
     * Konstruktor mit festgelegtem Seed.
     *
     * @param seed Seed-Wert für den Noise-Generator.
     */
    public NoiseGenerator(long seed) {
        noise = new OpenSimplexNoise(seed);
    }

    // ----- Getter und Setter für die Eigenschaften -----

    public void setFrequency(double frequency) {
        this.frequency = frequency;
    }

    public double getFrequency() {
        return frequency;
    }

    public void setOctaves(int octaves) {
        this.octaves = octaves;
    }

    public int getOctaves() {
        return octaves;
    }

    public void setPersistence(double persistence) {
        this.persistence = persistence;
    }

    public double getPersistence() {
        return persistence;
    }

    // ----- Öffentliche Methoden (ähnlich wie FastNoiseLite) -----

    /**
     * Liefert den 2D‑Noise-Wert an der Stelle (x, y) unter Berücksichtigung der Frequenz.
     *
     * @param x X‑Koordinate.
     * @param y Y‑Koordinate.
     * @return Noise-Wert im Bereich ca. [-1, 1].
     */
    public double getNoise2d(double x, double y) {
        return noise.eval(x * frequency, y * frequency);
    }

    /**
     * Liefert einen Fraktal‑(Oktaven‑)Noise-Wert an der Stelle (x, y).
     * Dabei werden die Parameter octaves und persistence berücksichtigt.
     *
     * @param x X‑Koordinate.
     * @param y Y‑Koordinate.
     * @return Kombinierter Noise-Wert im Bereich ca. [-1, 1].
     */
    public double getNoise2dFractal(double x, double y) {
        double total = 0;
        double amp = 1;
        double max = 0;
        double freq = frequency;
        for (int i = 0; i < octaves; i++) {
            total += noise.eval(x * freq, y * freq) * amp;
            max += amp;
            amp *= persistence;
            freq *= 2;
        }
        return total / max;
    }

    /**
     * Beispielmethode zur Erzeugung einer Terrainhöhe.
     * Um typische Artefakte entlang der Gitterachsen zu vermeiden, wird hier
     * ein konstanter Y‑Offset genutzt.
     *
     * @param x X‑Koordinate (z. B. horizontale Position in der Welt).
     * @return Terrainhöhe an der Stelle x als int.
     */
    public int getTerrainHeight(double x) {
        // Verwende als Y‑Wert einen festen Offset (z. B. 100), damit nicht exakt entlang einer Achse abgetastet wird.
        double noiseValue = getNoise2dFractal(x, 100.0);
        int baseHeight = 50;
        int heightVariation = 10;
        return baseHeight + (int) (noiseValue * heightVariation);
    }

    // ----- Beispielmain‑Methode zum Testen -----
    public static void main(String[] args) {
        // Beispiel: Erzeuge eine Instanz und setze Parameter ähnlich zu Godot:
        // In Godot: var noise = FastNoiseLite.new(); noise.frequency = 1.0 / 300.0;
        NoiseGenerator ng = new NoiseGenerator(12345L);
        ng.setFrequency(1.0 / 300.0);
        ng.setOctaves(5);
        ng.setPersistence(0.5);

        // Ausgabe: Terrainhöhe für verschiedene x-Werte
        for (int i = 0; i < 100; i += 5) {
            double x = i / 10.0;
            int terrainHeight = ng.getTerrainHeight(x);
            System.out.println("x = " + x + " -> Höhe = " + terrainHeight);
        }
    }
}
