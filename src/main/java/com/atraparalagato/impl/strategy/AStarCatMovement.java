package com.atraparalagato.impl.strategy;
import com.atraparalagato.base.model.GameBoard;
import com.atraparalagato.base.strategy.CatMovementStrategy;
import com.atraparalagato.impl.model.HexGameBoard;
import com.atraparalagato.impl.model.HexPosition;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
/**
 * Implementación esqueleto de estrategia de movimiento usando algoritmo A*.
 * 
 * Los estudiantes deben completar los métodos marcados con TODO.
 * 
 * Conceptos a implementar:
 * - Algoritmos: A* pathfinding
 * - Programación Funcional: Function, Predicate
 * - Estructuras de Datos: PriorityQueue, Map, Set
 */
public class AStarCatMovement extends CatMovementStrategy<HexPosition> {
    public AStarCatMovement(GameBoard<HexPosition> board) {
        super(board);
    }

    // TODO: Implementar selección del mejor movimiento usando A*
    public Optional<HexPosition> getNextMove(HexGameBoard board, HexPosition catPosition) {
        // registra todos los movimientos posibles
        List<HexPosition> possibleMoves = board.getAdjacentPositions(catPosition).stream()
                .filter(pos -> !board.isBlocked(pos))
                .toList();
        // se usa selectBestMove para determinar el mejor movimiento
        return selectBestMove(possibleMoves, catPosition, null);
    }

    @Override
    public List<HexPosition> getPossibleMoves(HexPosition currentPosition) {
        // TODO: Obtener movimientos válidos desde la posición actual
        // entrega los hexagonos adyacentes que no se encuentren bloqueados
        return board.getAdjacentPositions(currentPosition).stream()
                .filter(pos -> !board.isBlocked(pos))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<HexPosition> selectBestMove(List<HexPosition> possibleMoves,
                                                  HexPosition currentPosition,
                                                  HexPosition targetPosition) {
        // TODO: Implementar selección del mejor movimiento usando A*
        if (possibleMoves.isEmpty()) return Optional.empty();
        Function<HexPosition, Double> heuristic = getHeuristicFunction(targetPosition);
        // registra todos los movimientos posibles
        return possibleMoves.stream()
                .min(Comparator.comparing(move ->
                        getMoveCost(currentPosition, move) + heuristic.apply(move)
                ));
        // se usa selectBestMove para determinar el mejor movimiento
    }

    @Override
    protected Function<HexPosition, Double> getHeuristicFunction(HexPosition targetPosition) {
        // TODO: Implementar función heurística
        // calculo de distancia axial o hexagonal heuristica
        return position -> (double) position.distanceTo(targetPosition);
    }

    @Override
    protected Predicate<HexPosition> getGoalPredicate() {
        // TODO: Definir qué posiciones son objetivos válidos
        // lo que determinamos objetivos son las casillas borde
        int size = board.getSize();
        return pos -> Math.abs(pos.getQ()) == size ||
                      Math.abs(pos.getR()) == size ||
                      Math.abs(pos.getS()) == size;
    }

    @Override
    protected double getMoveCost(HexPosition from, HexPosition to) {
        // TODO: Calcular costo de moverse entre dos posiciones
        // costo de movimiento adyacente constante
        return 1.0;
    }

    @Override
    public boolean hasPathToGoal(HexPosition currentPosition) {
        // TODO: Verificar si existe camino desde posición actual hasta cualquier objetivo
        // se usa el algo BFS para buscar el camino hacia un objetivo (hexagono al borde del tablero)
        Set<HexPosition> visited = new HashSet<>();
        Queue<HexPosition> queue = new LinkedList<>();
        Predicate<HexPosition> isGoal = getGoalPredicate();
        queue.add(currentPosition);
        visited.add(currentPosition);
        while (!queue.isEmpty()) {
            HexPosition pos = queue.poll();
            if (isGoal.test(pos)) return true;
            for (HexPosition neighbor : getPossibleMoves(pos)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return false;
    }

    @Override
    public List<HexPosition> getFullPath(HexPosition currentPosition, HexPosition targetPosition) {
        // TODO: Implementar A* completo para obtener el camino completo
        // se usa A* para mostrar el camino completo
        Function<HexPosition, Double> heuristic = getHeuristicFunction(targetPosition);
        Set<HexPosition> closedSet = new HashSet<>();
        Map<HexPosition, Double> gScore = new HashMap<>();
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        gScore.put(currentPosition, 0.0);
        openSet.add(new AStarNode(currentPosition, 0.0, heuristic.apply(currentPosition), null));
        while (!openSet.isEmpty()) {
            AStarNode current = openSet.poll();
            if (current.position.equals(targetPosition)) {
                return reconstructPath(current);
            }
            closedSet.add(current.position);
            for (HexPosition neighbor : getPossibleMoves(current.position)) {
                if (closedSet.contains(neighbor)) continue;
                double tentativeG = current.gScore + getMoveCost(current.position, neighbor);
                if (tentativeG < gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    gScore.put(neighbor, tentativeG);
                    double fScore = tentativeG + heuristic.apply(neighbor);
                    openSet.add(new AStarNode(neighbor, tentativeG, fScore, current));
                }
            }
        }
        return Collections.emptyList();
    }

    // aux
    private static class AStarNode {
        public final HexPosition position;
        public final double gScore;
        public final double fScore; 
        public final AStarNode parent;
        public AStarNode(HexPosition position, double gScore, double fScore, AStarNode parent) {
            this.position = position;
            this.gScore = gScore;
            this.fScore = fScore;
            this.parent = parent;
        }
    }

    // TODO: Reconstruir camino desde nodo objetivo hasta inicio
    private List<HexPosition> reconstructPath(AStarNode goalNode) {
        List<HexPosition> path = new LinkedList<>();
        AStarNode current = goalNode;
        while (current != null) {
            path.add(0, current.position);
            current = current.parent;
        }
        return path;
    }

    // hook methods - los estudiantes pueden override para debugging
    @Override
    protected void beforeMovementCalculation(HexPosition currentPosition) {
        // TODO: Opcional - logging, métricas, etc.
        super.beforeMovementCalculation(currentPosition);
    }

    @Override
    protected void afterMovementCalculation(Optional<HexPosition> selectedMove) {
        // TODO: Opcional - logging, métricas, etc.
        super.afterMovementCalculation(selectedMove);
    }
}
