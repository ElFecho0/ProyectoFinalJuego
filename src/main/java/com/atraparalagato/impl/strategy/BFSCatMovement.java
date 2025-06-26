package com.atraparalagato.impl.strategy;
import com.atraparalagato.base.model.GameBoard;
import com.atraparalagato.base.strategy.CatMovementStrategy;
import com.atraparalagato.impl.model.HexGameBoard;
import com.atraparalagato.impl.model.HexPosition;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
/**
 * Implementación esqueleto de estrategia BFS (Breadth-First Search) para el gato.
 * 
 * Los estudiantes deben completar los métodos marcados con TODO.
 * 
 * Conceptos a implementar:
 * - Algoritmo BFS para pathfinding
 * - Exploración exhaustiva de caminos
 * - Garantía de encontrar el camino más corto
 * - Uso de colas para exploración por niveles
 */
public class BFSCatMovement extends CatMovementStrategy<HexPosition> {
    public BFSCatMovement(GameBoard<HexPosition> board) {
        super(board);
    }

    @Override
    public List<HexPosition> getPossibleMoves(HexPosition currentPosition) {
        // TODO: Obtener todas las posiciones adyacentes válidas
        // entrega los hexagonos que no estan bloqueados o clickeados, adyacentemente
        return board.getAdjacentPositions(currentPosition).stream()
                .filter(pos -> !board.isBlocked(pos))
                .toList();
    }

    
    public Optional<HexPosition> getNextMove(HexGameBoard board, HexPosition catPosition) {
        // busqueda de distintos movimientos (todos) posibles
        List<HexPosition> possibleMoves = board.getAdjacentPositions(catPosition).stream()
                .filter(pos -> !board.isBlocked(pos))
                .toList();
    // usa selectBestMove para elegir el mejor movimiento
        return selectBestMove(possibleMoves, catPosition, null);
    }
    @Override
    public Optional<HexPosition> selectBestMove(List<HexPosition> possibleMoves,
                                            HexPosition currentPosition,
                                            HexPosition targetPosition) {
        // TODO: Usar BFS para encontrar el mejor movimiento
        if (possibleMoves.isEmpty()) return Optional.empty();

        // muy pocas veces usa un movimiento aleatorio para moverse, aseguranod que sea válido (0.3)
        if (Math.random() < 0.6) {
            List<HexPosition> shuffled = new ArrayList<>(possibleMoves);
            Collections.shuffle(shuffled);
            for (HexPosition move : shuffled) {
                if (bfsToGoal(move).isPresent()) {
                    return Optional.of(move);
                }
            }
        }
        // para los otros movimientos, usa lógica e inteligencia
        List<HexPosition> shuffledMoves = new ArrayList<>(possibleMoves);
        Collections.shuffle(shuffledMoves);
        Predicate<HexPosition> isGoal = getGoalPredicate();
        List<HexPosition> bestPath = null;
        for (HexPosition move : shuffledMoves) {
            List<HexPosition> path = bfsToGoal(move).orElse(null);
            if (path != null && !path.isEmpty() && isGoal.test(path.get(path.size() - 1))) {
                if (bestPath == null || path.size() < bestPath.size()) {
                    bestPath = path;
                }
            }
        }
        if (bestPath != null && !bestPath.isEmpty()) {
            return Optional.of(bestPath.get(0));
        }
        return Optional.empty();
    }

    @Override
    protected Function<HexPosition, Double> getHeuristicFunction(HexPosition targetPosition) {
        // TODO: BFS no necesita heurística, pero puede usarse para desempate
        // aunque bfs no sea necesario, se puede usar distancias normales axiales para este vacío
        return position -> (double) position.distanceTo(targetPosition);
    }

    @Override
    protected Predicate<HexPosition> getGoalPredicate() {
        // TODO: Definir condición de objetivo (llegar al borde)
        int size = board.getSize();
        // una casilla objetivo es una casilla borde o que se encuentre en el borde del tablero
        return pos -> Math.abs(pos.getQ()) == size ||
                      Math.abs(pos.getR()) == size ||
                      Math.abs(pos.getS()) == size;
    }

    @Override
    protected double getMoveCost(HexPosition from, HexPosition to) {
        // TODO: BFS usa costo uniforme (1.0 para movimientos adyacentes)
        return 1.0;
    }

    public boolean hasPathToBorder(HexPosition start) {
        return bfsToGoal(start).isPresent();
    }

    @Override
    public boolean hasPathToGoal(HexPosition start) {
        // TODO: Implementar BFS para verificar si existe camino al objetivo
        return bfsToGoal(start).isPresent();
    }

    @Override
    public List<HexPosition> getFullPath(HexPosition currentPosition, HexPosition targetPosition) {
        // TODO: Implementar BFS completo para encontrar camino
        // bfs
        Queue<HexPosition> queue = new LinkedList<>();
        Map<HexPosition, HexPosition> parentMap = new HashMap<>();
        Set<HexPosition> visited = new HashSet<>();
        queue.add(currentPosition);
        visited.add(currentPosition);
        while (!queue.isEmpty()) {
            HexPosition pos = queue.poll();
            if (pos.equals(targetPosition)) {
                return reconstructPath(parentMap, currentPosition, targetPosition);
            }
            for (HexPosition neighbor : getPossibleMoves(pos)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    parentMap.put(neighbor, pos);
                    queue.add(neighbor);
                }
            }
        }
        return Collections.emptyList();
    }

    // Métodos auxiliares que los estudiantes pueden implementar
    
    /**
     * TODO: Ejecutar BFS desde una posición hasta encontrar objetivo.
     */
    //objetivo == borde
    private Optional<List<HexPosition>> bfsToGoal(HexPosition start) {
        Predicate<HexPosition> isGoal = getGoalPredicate();
        Queue<HexPosition> queue = new LinkedList<>();
        Map<HexPosition, HexPosition> parentMap = new HashMap<>();
        Set<HexPosition> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            HexPosition pos = queue.poll();
            if (isGoal.test(pos)) {
                return Optional.of(reconstructPath(parentMap, start, pos));
            }
            for (HexPosition neighbor : getPossibleMoves(pos)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    parentMap.put(neighbor, pos);
                    queue.add(neighbor);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * TODO: Reconstruir camino desde mapa de padres.
     */
    // reconstruye el camino desde el parentMap
    private List<HexPosition> reconstructPath(Map<HexPosition, HexPosition> parentMap,
                                              HexPosition start, HexPosition goal) {
        List<HexPosition> path = new LinkedList<>();
        HexPosition current = goal;
        while (current != null && !current.equals(start)) {
            path.add(0, current);
            current = parentMap.get(current);
        }
        if (current != null) {
            path.add(0, start);
        }
        return path;
    }
}
