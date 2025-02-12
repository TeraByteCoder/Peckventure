package at.peckventure.world.generator;

import java.util.Random;

public class NoiseGenerator
{

    // Permutationsarray zur Erzeugung deterministischer Gradienten
    private int[] permutation;

    /**
     * Konstruktor: Erzeugt das Permutationsarray basierend auf einem Seed.
     * Dadurch ist der Noise reproduzierbar.
     */
    public NoiseGenerator(long seed)
    {
        Random random = new Random(seed);
        permutation = new int[256];
        // Initialisiere das Array mit Werten 0..255
        for (int i = 0; i < 256; i++)
        {
            permutation[i] = i;
        }
        // Mische das Array
        for (int i = 255; i > 0; i--)
        {
            int j = random.nextInt(i + 1);
            int temp = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = temp;
        }
        // Dupliziere das Array, um Überläufe zu vermeiden
        int[] p = new int[512];
        for (int i = 0; i < 512; i++)
        {
            p[i] = permutation[i % 256];
        }
        permutation = p;
    }


    /**
     * Erzeugt 1D Perlin Noise an der Position x.
     * Der Rückgabewert liegt ungefähr im Bereich -1 bis 1.
     */
    public double noise(double x)
    {
        // Bestimme den ganzzahligen Teil (Gitterpunkt)
        int xi = (int) Math.floor(x) & 255;
        // Bestimme den restlichen Anteil
        double xf = x - Math.floor(x);
        // Anwenden der Fade-Funktion für glatte Übergänge
        double u = fade(xf);

        // Hole zwei Pseudozufallswerte aus dem Permutationsarray
        int a = permutation[xi];
        int b = permutation[xi + 1];

        // Berechne die Gradienten und interpoliere zwischen diesen
        double grad1 = grad(a, xf);
        double grad2 = grad(b, xf - 1);

        return lerp(u, grad1, grad2);
    }

    /**
     * Fade-Funktion (3. Ordnung), wie sie von Ken Perlin vorgeschlagen wurde.
     * Sie sorgt für glatte Übergänge.
     */
    private double fade(double t)
    {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    /**
     * Lineare Interpolation zwischen a und b.
     */
    private double lerp(double t, double a, double b)
    {
        return a + t * (b - a);
    }

    /**
     * Gradient-Funktion für 1D: Liefert entweder den Wert x oder -x,
     * abhängig vom Hash-Wert.
     */
    private double grad(int hash, double x)
    {
        return ((hash & 1) == 0 ? x : -x);
    }

    /**
     * Erzeugt Noise mit mehreren Oktaven, um mehr Detail zu erreichen.
     *
     * @param x           Die Eingabekoordinate
     * @param octaves     Anzahl der Oktaven (mehr Oktaven = mehr Details)
     * @param persistence Bestimmt, wie stark die Amplitude bei jeder Oktave abnimmt (typischerweise 0.5)
     * @return Der normalisierte Noise-Wert im Bereich ca. -1 bis 1.
     */
    public double octaveNoise(double x, int octaves, double persistence)
    {
        double total = 0;
        double frequency = 1;
        double amplitude = 1;
        double maxValue = 0; // Normierungsfaktor

        for (int i = 0; i < octaves; i++)
        {
            total += noise(x * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2;
        }
        return total / maxValue;
    }

    /**
     * Beispiel: Terrain-Generierung in 1D.
     * Hier wird für eine gegebene Breite eine Reihe von Höhenwerten berechnet.
     */
    public static void main(String[] args)
    {
        // Parameter für die Terrain-Generierung
        int width = 200;            // Anzahl der horizontalen Punkte
        double scale = 0.05;        // Skalierungsfaktor, um die Frequenz anzupassen
        int octaves = 5;            // Anzahl der Oktaven
        double persistence = 0.5;   // Abnahme der Amplitude pro Oktave

        // Erzeuge einen Noise-Generator mit einem Seed (hier aktuelle Zeit)
        NoiseGenerator generator = new NoiseGenerator(System.currentTimeMillis());
        double[] terrain = new double[width];

        // Berechne für jeden x-Wert den Noise-Wert
        for (int x = 0; x < width; x++)
        {
            // Mit octaveNoise erhältst du weichere, abwechslungsreiche Werte
            double noiseValue = generator.octaveNoise(x * scale, octaves, persistence);

            // Optional: Passe den Wertebereich an, z.B. auf eine gewünschte Höhe
            // Hier belassen wir den Rohwert (ca. zwischen -1 und 1)
            terrain[x] = noiseValue;
        }

        // Ausgabe der Terrainwerte (kannst du auch graphisch darstellen)
        for (int x = 0; x < width; x++)
        {
            System.out.println("Terrain[" + x + "] = " + terrain[x]);
        }
    }


}
