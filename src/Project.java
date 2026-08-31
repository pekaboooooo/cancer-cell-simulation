import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;


public class Project {
    private static final int COLUMNS = 100;
    private static final int ROWS = 60;
    private static final int CELL_SIZE = 10;

    private final Cell[][] cells = new Cell[ROWS][COLUMNS];
    private final Random random = new Random();
    private final SimulationPanel simulationPanel = new SimulationPanel();

    public static void main(String[] args) {
        Project program = new Project();
        program.createWindow();
        program.seedCells();
        program.start();
    }

    private void createWindow() {
        JFrame window = new JFrame("Cellular Automata");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        window.setLayout(new BorderLayout());
        window.add(simulationPanel, BorderLayout.CENTER);

        JLabel legend = new JLabel(
                "Green: tissue   Red: cancer   Black: immune   White: dead",
                SwingConstants.CENTER);
        legend.setPreferredSize(new Dimension(COLUMNS * CELL_SIZE, 30));
        window.add(legend, BorderLayout.SOUTH);

        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }

    private void seedCells() {
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                cells[row][column] = new TissueCell(column, row);
            }
        }

        // Seed cancer clusters
        seedCancerCluster(18, 12, 9);
        seedCancerCluster(72, 16, 8);
        seedCancerCluster(42, 43, 7);
        seedCancerCluster(83, 46, 6);

        // Add immune cells
        for (int i = 0; i < 28; i++) {
            int row = random.nextInt(ROWS);
            int column = random.nextInt(COLUMNS);
            cells[row][column] = new ImmuneCell(column, row);
        }
    }

    private void seedCancerCluster(int centerColumn, int centerRow, int radius) {
        for (int row = centerRow - radius; row <= centerRow + radius; row++) {
            for (int column = centerColumn - radius; column <= centerColumn + radius; column++) {
                if (isInside(row, column)) {
                    double distance = Math.hypot(column - centerColumn, row - centerRow);
                    if (distance <= radius && random.nextDouble() > 0.12) {
                        cells[row][column] = new CancerCell(column, row);
                    }
                }
            }
        }
    }

    private void start() {
        Timer timer = new Timer(90, event -> {
            advanceSimulation();
            simulationPanel.repaint();
        });
        timer.start();
    }

    private void advanceSimulation() {
        Cell[][] nextGeneration = new Cell[ROWS][COLUMNS];

        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                ArrayList<Cell> neighbors = getNeighbors(row, column);
                nextGeneration[row][column] = nextCell(cells[row][column], neighbors, column, row);
            }
        }

        for (int row = 0; row < ROWS; row++) {
            System.arraycopy(nextGeneration[row], 0, cells[row], 0, COLUMNS);
        }
    }

    // 细胞演化逻辑参数
    private Cell nextCell(Cell current, ArrayList<Cell> neighbors, int column, int row) {
        int cancerNeighbors = countNeighbors(neighbors, CancerCell.class);
        int immuneNeighbors = countNeighbors(neighbors, ImmuneCell.class);
        int tissueNeighbors = countNeighbors(neighbors, TissueCell.class);

        // 免疫 如果没有癌细胞了，免疫细胞会慢慢消退回普通组织
        if (current instanceof ImmuneCell) {
            if (cancerNeighbors == 0 && random.nextDouble() < 0.08) {
                return new TissueCell(column, row);
            }
            return new ImmuneCell(column, row);
        }

        // 2. 癌细胞 遇到免疫细胞会被消灭（变成死细胞）
        if (current instanceof CancerCell) {
            if (immuneNeighbors > 0 && random.nextDouble() < 0.35) {
                return new DeadCell(column, row);
            }
            return new CancerCell(column, row);
        }

        // 3. 死细胞 快速被健康组织自我修复
        if (current instanceof DeadCell) {
            if (tissueNeighbors >= 1 && random.nextDouble() < 0.50) {
                return new TissueCell(column, row);
            }
            return new DeadCell(column, row);
        }

        // 4. 健康组织 癌细胞侵蚀 vs 免疫细胞扩散招募
        if (current instanceof TissueCell) {
            // 如果周围有免疫细胞，且附近有癌细胞，健康组织有25%概率变为新的免疫细胞
            if (immuneNeighbors > 0 && cancerNeighbors > 0 && random.nextDouble() < 0.25) {
                return new ImmuneCell(column, row);
            }

            // 癌细胞 周围有癌细胞时，50% 概率被感染
            if (cancerNeighbors >= 2 && random.nextDouble() < 0.50) {
                return new CancerCell(column, row);
            }

            return new TissueCell(column, row);
        }

        return new TissueCell(column, row);
    }

    private ArrayList<Cell> getNeighbors(int row, int column) {
        ArrayList<Cell> neighbors = new ArrayList<>();
        for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
            for (int columnOffset = -1; columnOffset <= 1; columnOffset++) {
                if (rowOffset == 0 && columnOffset == 0) {
                    continue;
                }

                int neighborRow = row + rowOffset;
                int neighborColumn = column + columnOffset;
                if (isInside(neighborRow, neighborColumn)) {
                    neighbors.add(cells[neighborRow][neighborColumn]);
                }
            }
        }
        return neighbors;
    }

    private int countNeighbors(ArrayList<Cell> neighbors, Class<?> cellType) {
        int count = 0;
        for (Cell neighbor : neighbors) {
            if (cellType.isInstance(neighbor)) {
                count++;
            }
        }
        return count;
    }

    private boolean isInside(int row, int column) {
        return row >= 0 && row < ROWS && column >= 0 && column < COLUMNS;
    }

    private class SimulationPanel extends JPanel {
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(COLUMNS * CELL_SIZE, ROWS * CELL_SIZE);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            for (int row = 0; row < ROWS; row++) {
                for (int column = 0; column < COLUMNS; column++) {
                    graphics.setColor(colorFor(cells[row][column]));
                    graphics.fillRect(
                            column * CELL_SIZE,
                            row * CELL_SIZE,
                            CELL_SIZE,
                            CELL_SIZE);
                }
            }
        }

        private Color colorFor(Cell cell) {
            if (cell instanceof CancerCell) {
                return Color.RED;
            }
            if (cell instanceof ImmuneCell) {
                return Color.BLACK;
            }
            if (cell instanceof DeadCell) {
                return Color.WHITE;
            }
            return Color.GREEN;
        }
    }
}
